package com.inkwell.core.model.palette

import com.inkwell.core.model.Color
import java.util.UUID

/**
 * Manages the collection of user and default color palettes,
 * providing modification operations and JSON persistence.
 */
class PaletteManager(
    initialPalettes: List<Palette> = Palette.DEFAULT_PRESETS,
    initialActiveId: String = Palette.ESSENTIALS.id
) {
    private val _palettes = mutableListOf<Palette>().apply {
        addAll(initialPalettes)
    }

    var activePaletteId: String = initialActiveId
        private set

    val palettes: List<Palette> get() = _palettes.toList()

    val activePalette: Palette
        get() = _palettes.find { it.id == activePaletteId } ?: _palettes.firstOrNull() ?: Palette.ESSENTIALS

    fun selectPalette(id: String): Boolean {
        if (_palettes.any { it.id == id }) {
            activePaletteId = id
            return true
        }
        return false
    }

    fun createPalette(name: String, swatches: List<Color> = emptyList()): Palette {
        val newPalette = Palette(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "New Palette" },
            swatches = swatches,
            isDefault = false
        )
        _palettes.add(newPalette)
        activePaletteId = newPalette.id
        return newPalette
    }

    fun deletePalette(id: String): Boolean {
        val target = _palettes.find { it.id == id } ?: return false
        if (_palettes.size <= 1) return false // Prevent deleting the sole remaining palette

        _palettes.remove(target)
        if (activePaletteId == id) {
            activePaletteId = _palettes.first().id
        }
        return true
    }

    fun renamePalette(id: String, newName: String): Boolean {
        val index = _palettes.indexOfFirst { it.id == id }
        if (index == -1 || newName.isBlank()) return false

        val current = _palettes[index]
        _palettes[index] = current.copy(name = newName.trim())
        return true
    }

    fun addSwatch(paletteId: String, color: Color): Boolean {
        val index = _palettes.indexOfFirst { it.id == paletteId }
        if (index == -1) return false

        val current = _palettes[index]
        _palettes[index] = current.copy(swatches = current.swatches + color)
        return true
    }

    fun removeSwatch(paletteId: String, swatchIndex: Int): Boolean {
        val index = _palettes.indexOfFirst { it.id == paletteId }
        if (index == -1) return false

        val current = _palettes[index]
        if (swatchIndex !in current.swatches.indices) return false

        val newSwatches = current.swatches.toMutableList().apply { removeAt(swatchIndex) }
        _palettes[index] = current.copy(swatches = newSwatches)
        return true
    }

    /**
     * Serializes all palettes and active state into a persistent JSON document.
     */
    fun saveToJson(): String {
        val palettesJson = _palettes.joinToString(separator = ",") { it.toJson() }
        return """{"activePaletteId":"$activePaletteId","palettes":[$palettesJson]}"""
    }

    /**
     * Restores palettes and active selection from JSON.
     */
    fun loadFromJson(json: String) {
        val activeMatch = """"activePaletteId"\s*:\s*"([^"]+)"""".toRegex().find(json)
        val loadedActiveId = activeMatch?.groupValues?.get(1)

        val palettesList = mutableListOf<Palette>()
        val paletteRegex = """\{"id"[^}]+\}""".toRegex()
        val matches = paletteRegex.findAll(json)

        for (m in matches) {
            try {
                palettesList.add(Palette.fromJson(m.value))
            } catch (ignored: Exception) {
            }
        }

        if (palettesList.isNotEmpty()) {
            _palettes.clear()
            _palettes.addAll(palettesList)
            if (loadedActiveId != null && _palettes.any { it.id == loadedActiveId }) {
                activePaletteId = loadedActiveId
            } else {
                activePaletteId = _palettes.first().id
            }
        }
    }
}
