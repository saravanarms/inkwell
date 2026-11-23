package com.inkwell.core.model.history

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class HistoryManagerTest {

    private class TestCommand(
        val id: Int,
        override val memoryCostBytes: Long = 1024L
    ) : HistoryCommand {
        override val description: String = "Command $id"
        var isUndone = false

        override fun undo() {
            isUndone = true
        }

        override fun redo() {
            isUndone = false
        }
    }

    @Test
    fun `supports 50 or more consecutive undo steps`() {
        val manager = HistoryManager(maxUndoSteps = 60)

        for (i in 1..60) {
            manager.recordCommand(TestCommand(i))
        }

        assertThat(manager.undoCount).isEqualTo(60)
        assertThat(manager.canUndo).isTrue()
        assertThat(manager.canRedo).isFalse()

        // Execute 50 consecutive undos
        for (i in 60 downTo 11) {
            val undone = manager.undo() as TestCommand
            assertThat(undone.id).isEqualTo(i)
            assertThat(undone.isUndone).isTrue()
        }

        assertThat(manager.undoCount).isEqualTo(10)
        assertThat(manager.redoCount).isEqualTo(50)
    }

    @Test
    fun `redo stack clears immediately when new action is recorded`() {
        val manager = HistoryManager()

        manager.recordCommand(TestCommand(1))
        manager.recordCommand(TestCommand(2))

        manager.undo() // Redo stack now has Command 2
        assertThat(manager.canRedo).isTrue()

        // Record new action
        manager.recordCommand(TestCommand(3))

        // Redo stack must be completely cleared
        assertThat(manager.canRedo).isFalse()
        assertThat(manager.redoCount).isEqualTo(0)
    }

    @Test
    fun `memory ceiling strictly evicts oldest commands when limit exceeded`() {
        // 5 KB memory ceiling
        val manager = HistoryManager(
            maxUndoSteps = 50,
            maxMemoryCeilingBytes = 5 * 1024L
        )

        // Add 10 commands of 1 KB each (total 10 KB > 5 KB limit)
        for (i in 1..10) {
            manager.recordCommand(TestCommand(i, memoryCostBytes = 1024L))
        }

        // Must have evicted oldest commands so total stays within 5 KB
        assertThat(manager.totalMemoryBytes).isAtMost(5 * 1024L)
        assertThat(manager.undoCount).isAtMost(5)
    }
}
