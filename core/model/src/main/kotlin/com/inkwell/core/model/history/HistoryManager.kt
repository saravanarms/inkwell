package com.inkwell.core.model.history

import java.util.ArrayDeque

/**
 * Transactional history manager with dual bounded deques, memory ceiling enforcement,
 * and sub-second undo/redo execution.
 */
class HistoryManager(
    val maxUndoSteps: Int = DEFAULT_MAX_STEPS,
    val maxMemoryCeilingBytes: Long = DEFAULT_MAX_MEMORY_BYTES
) {
    private val undoStack = ArrayDeque<HistoryCommand>()
    private val redoStack = ArrayDeque<HistoryCommand>()

    var totalMemoryBytes: Long = 0L
        private set

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
    val undoCount: Int get() = undoStack.size
    val redoCount: Int get() = redoStack.size

    /**
     * Records a newly executed action into the undo history.
     * Clears the redo stack immediately and enforces the memory ceiling.
     */
    fun recordCommand(command: HistoryCommand) {
        // 1. Clear redo stack on new action
        while (redoStack.isNotEmpty()) {
            val popped = redoStack.removeLast()
            totalMemoryBytes -= popped.memoryCostBytes
        }

        // 2. Push to undo stack
        undoStack.addLast(command)
        totalMemoryBytes += command.memoryCostBytes

        // 3. Enforce step count limit
        while (undoStack.size > maxUndoSteps) {
            val evicted = undoStack.removeFirst()
            totalMemoryBytes -= evicted.memoryCostBytes
        }

        // 4. Enforce memory ceiling
        while (totalMemoryBytes > maxMemoryCeilingBytes && undoStack.size > 1) {
            val evicted = undoStack.removeFirst()
            totalMemoryBytes -= evicted.memoryCostBytes
        }
    }

    /**
     * Executes an undo operation: pops the latest command from the undo stack,
     * calls its undo logic, and transfers it to the redo stack.
     */
    fun undo(): HistoryCommand? {
        if (undoStack.isEmpty()) return null

        val command = undoStack.removeLast()
        command.undo()
        redoStack.addLast(command)
        return command
    }

    /**
     * Executes a redo operation: pops the latest command from the redo stack,
     * calls its redo logic, and transfers it back to the undo stack.
     */
    fun redo(): HistoryCommand? {
        if (redoStack.isEmpty()) return null

        val command = redoStack.removeLast()
        command.redo()
        undoStack.addLast(command)
        return command
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
        totalMemoryBytes = 0L
    }

    companion object {
        const val DEFAULT_MAX_STEPS = 50
        const val DEFAULT_MAX_MEMORY_BYTES = 50L * 1024L * 1024L // 50 MB
    }
}
