package com.inkwell.core.model.palette

import com.google.common.truth.Truth.assertThat
import com.inkwell.core.model.Color
import org.junit.jupiter.api.Test

class PaletteManagerTest {

    @Test
    fun `default presets loaded with Essential Studio as active`() {
        val manager = PaletteManager()

        assertThat(manager.palettes).isNotEmpty()
        assertThat(manager.activePalette.name).isEqualTo("Studio Essentials")
        assertThat(manager.activePalette.swatches.size).isAtLeast(10)
    }

    @Test
    fun `create and select new palette`() {
        val manager = PaletteManager()

        val customPalette = manager.createPalette(
            name = "Sunset Glow",
            swatches = listOf(Color.fromRgbInt(255, 100, 50), Color.fromRgbInt(120, 20, 90))
        )

        assertThat(manager.activePaletteId).isEqualTo(customPalette.id)
        assertThat(manager.activePalette.name).isEqualTo("Sunset Glow")
        assertThat(manager.activePalette.swatches).hasSize(2)
    }

    @Test
    fun `add and remove swatches in active palette`() {
        val manager = PaletteManager()
        val palette = manager.createPalette("Test Swatches")

        val newColor = Color.fromRgbInt(0, 200, 255)
        manager.addSwatch(palette.id, newColor)

        assertThat(manager.activePalette.swatches).contains(newColor)

        manager.removeSwatch(palette.id, 0)
        assertThat(manager.activePalette.swatches).doesNotContain(newColor)
    }

    @Test
    fun `rename and delete palette`() {
        val manager = PaletteManager()
        val palette = manager.createPalette("Temp")

        manager.renamePalette(palette.id, "Renamed Palette")
        assertThat(manager.activePalette.name).isEqualTo("Renamed Palette")

        val deleted = manager.deletePalette(palette.id)
        assertThat(deleted).isTrue()
        assertThat(manager.palettes.any { it.id == palette.id }).isFalse()
    }

    @Test
    fun `json persistence survives app kill and restart with 100 percent fidelity`() {
        val originalManager = PaletteManager()

        // Create a custom palette with distinct colors
        val originalCustom = originalManager.createPalette(
            name = "Retro Arcade",
            swatches = listOf(
                Color.fromRgbInt(255, 0, 85, 255),
                Color.fromRgbInt(0, 240, 255, 200),
                Color.fromRgbInt(255, 220, 0, 180)
            )
        )

        // Serialize to JSON
        val json = originalManager.saveToJson()
        assertThat(json).contains("Retro Arcade")

        // New manager instance simulating cold app restart
        val restoredManager = PaletteManager()
        restoredManager.loadFromJson(json)

        assertThat(restoredManager.palettes.size).isEqualTo(originalManager.palettes.size)
        assertThat(restoredManager.activePaletteId).isEqualTo(originalCustom.id)

        val restoredPalette = restoredManager.activePalette
        assertThat(restoredPalette.name).isEqualTo("Retro Arcade")
        assertThat(restoredPalette.swatches).hasSize(3)
        assertThat(restoredPalette.swatches[0].redInt).isEqualTo(255)
        assertThat(restoredPalette.swatches[0].greenInt).isEqualTo(0)
        assertThat(restoredPalette.swatches[0].blueInt).isEqualTo(85)
        assertThat(restoredPalette.swatches[1].greenInt).isEqualTo(240)
    }
}
