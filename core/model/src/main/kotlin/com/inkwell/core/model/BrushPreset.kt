package com.inkwell.core.model

/**
 * Brush tip geometry profile.
 */
enum class BrushTip(val id: Int) {
    ROUND_SOFT(0),
    ROUND_HARD(1),
    CHISEL(2),
    FLAT_BRISTLE(3),
    STIPPLE(4)
}

/**
 * Paper grain surface texture.
 */
enum class BrushGrain(val id: Int) {
    NONE(0),
    PAPER_CANVAS(1),
    COLD_PRESS(2),
    ROUGH(3)
}

/**
 * Brush category taxonomy.
 */
enum class BrushCategory(val displayName: String) {
    PENCIL("Pencils & Sketching"),
    INK("Inking & Pens"),
    PAINT("Painting & Acrylics"),
    AIRBRUSH("Airbrushing"),
    EFFECTS("Blenders & Erasers")
}

/**
 * Production brush specification controlling dab dynamics, spacing, smoothing,
 * tip geometry, paper grain, and tool mode.
 */
data class BrushPreset(
    val id: String,
    val name: String,
    val category: BrushCategory,
    val toolMode: ToolMode = ToolMode.PAINT,
    val tip: BrushTip = BrushTip.ROUND_SOFT,
    val grain: BrushGrain = BrushGrain.NONE,
    val minSize: Float = 2.0f,
    val maxSize: Float = 40.0f,
    val baseOpacity: Float = 1.0f,
    val spacing: Float = 0.08f, // 8% spacing
    val hardness: Float = 0.8f,
    val streamline: Float = 0.5f, // 50% smoothing
    val tiltSensitivity: Float = 0.0f, // 0.0 to 1.0
    val sizeJitter: Float = 0.0f,
    val opacityJitter: Float = 0.0f
) {
    init {
        require(minSize > 0f && maxSize >= minSize) { "Invalid brush size range [$minSize, $maxSize]" }
        require(baseOpacity in 0.01f..1.0f) { "Opacity $baseOpacity must be in [0.01, 1.0]" }
        require(spacing in 0.01f..1.0f) { "Spacing $spacing must be in [0.01, 1.0]" }
        require(hardness in 0.0f..1.0f) { "Hardness $hardness must be in [0.0, 1.0]" }
        require(streamline in 0.0f..0.95f) { "Streamline $streamline must be in [0.0, 0.95]" }
    }

    companion object {
        val PENCIL_2B = BrushPreset(
            id = "pencil_2b",
            name = "2B Graphite Pencil",
            category = BrushCategory.PENCIL,
            toolMode = ToolMode.PAINT,
            tip = BrushTip.ROUND_HARD,
            grain = BrushGrain.PAPER_CANVAS,
            minSize = 1.0f,
            maxSize = 14.0f,
            baseOpacity = 0.85f,
            spacing = 0.05f,
            hardness = 0.45f,
            streamline = 0.25f,
            tiltSensitivity = 0.70f
        )

        val TECHNICAL_PEN = BrushPreset(
            id = "tech_pen",
            name = "Technical Inking Pen",
            category = BrushCategory.INK,
            toolMode = ToolMode.PAINT,
            tip = BrushTip.ROUND_HARD,
            grain = BrushGrain.NONE,
            minSize = 1.5f,
            maxSize = 8.0f,
            baseOpacity = 1.0f,
            spacing = 0.03f,
            hardness = 0.98f,
            streamline = 0.65f
        )

        val STUDIO_BRUSH_PEN = BrushPreset(
            id = "studio_brush_pen",
            name = "Studio Brush Pen",
            category = BrushCategory.INK,
            toolMode = ToolMode.PAINT,
            tip = BrushTip.ROUND_SOFT,
            grain = BrushGrain.NONE,
            minSize = 2.0f,
            maxSize = 36.0f,
            baseOpacity = 1.0f,
            spacing = 0.04f,
            hardness = 0.85f,
            streamline = 0.75f,
            tiltSensitivity = 0.40f
        )

        val CALLIGRAPHY_NIB = BrushPreset(
            id = "calligraphy_nib",
            name = "Fine Calligraphy Nib",
            category = BrushCategory.INK,
            toolMode = ToolMode.PAINT,
            tip = BrushTip.CHISEL,
            grain = BrushGrain.NONE,
            minSize = 2.0f,
            maxSize = 28.0f,
            baseOpacity = 1.0f,
            spacing = 0.03f,
            hardness = 0.90f,
            streamline = 0.60f,
            tiltSensitivity = 0.80f
        )

        val CLASSIC_OIL = BrushPreset(
            id = "classic_oil",
            name = "Classic Oil Paint",
            category = BrushCategory.PAINT,
            toolMode = ToolMode.PAINT,
            tip = BrushTip.FLAT_BRISTLE,
            grain = BrushGrain.COLD_PRESS,
            minSize = 8.0f,
            maxSize = 75.0f,
            baseOpacity = 0.92f,
            spacing = 0.06f,
            hardness = 0.70f,
            streamline = 0.40f
        )

        val WET_ACRYLIC = BrushPreset(
            id = "wet_acrylic",
            name = "Wet Acrylic Filbert",
            category = BrushCategory.PAINT,
            toolMode = ToolMode.PAINT,
            tip = BrushTip.ROUND_SOFT,
            grain = BrushGrain.NONE,
            minSize = 6.0f,
            maxSize = 60.0f,
            baseOpacity = 0.90f,
            spacing = 0.05f,
            hardness = 0.75f,
            streamline = 0.45f
        )

        val BRISTLE_FAN = BrushPreset(
            id = "bristle_fan",
            name = "Bristle Fan Brush",
            category = BrushCategory.PAINT,
            toolMode = ToolMode.PAINT,
            tip = BrushTip.STIPPLE,
            grain = BrushGrain.ROUGH,
            minSize = 10.0f,
            maxSize = 90.0f,
            baseOpacity = 0.70f,
            spacing = 0.08f,
            hardness = 0.50f,
            streamline = 0.30f
        )

        val SOFT_AIRBRUSH = BrushPreset(
            id = "soft_airbrush",
            name = "Soft Studio Airbrush",
            category = BrushCategory.AIRBRUSH,
            toolMode = ToolMode.PAINT,
            tip = BrushTip.ROUND_SOFT,
            grain = BrushGrain.NONE,
            minSize = 15.0f,
            maxSize = 180.0f,
            baseOpacity = 0.40f,
            spacing = 0.04f,
            hardness = 0.15f,
            streamline = 0.20f
        )

        val HARD_AIRBRUSH = BrushPreset(
            id = "hard_airbrush",
            name = "Hard Edge Airbrush",
            category = BrushCategory.AIRBRUSH,
            toolMode = ToolMode.PAINT,
            tip = BrushTip.ROUND_HARD,
            grain = BrushGrain.NONE,
            minSize = 10.0f,
            maxSize = 120.0f,
            baseOpacity = 0.65f,
            spacing = 0.04f,
            hardness = 0.90f,
            streamline = 0.35f
        )

        val WATERCOLOR_GLAZE = BrushPreset(
            id = "watercolor_glaze",
            name = "Watercolor Glaze",
            category = BrushCategory.PAINT,
            toolMode = ToolMode.PAINT,
            tip = BrushTip.ROUND_SOFT,
            grain = BrushGrain.COLD_PRESS,
            minSize = 12.0f,
            maxSize = 100.0f,
            baseOpacity = 0.35f,
            spacing = 0.07f,
            hardness = 0.30f,
            streamline = 0.50f
        )

        val SMUDGE_BLENDER = BrushPreset(
            id = "smudge_blender",
            name = "Finger Smudge Blender",
            category = BrushCategory.EFFECTS,
            toolMode = ToolMode.SMUDGE,
            tip = BrushTip.ROUND_SOFT,
            grain = BrushGrain.NONE,
            minSize = 10.0f,
            maxSize = 70.0f,
            baseOpacity = 0.80f,
            spacing = 0.05f,
            hardness = 0.60f,
            streamline = 0.55f
        )

        val KNEADED_ERASER = BrushPreset(
            id = "kneaded_eraser",
            name = "Clean Kneaded Eraser",
            category = BrushCategory.EFFECTS,
            toolMode = ToolMode.ERASER,
            tip = BrushTip.ROUND_SOFT,
            grain = BrushGrain.NONE,
            minSize = 5.0f,
            maxSize = 65.0f,
            baseOpacity = 1.0f,
            spacing = 0.05f,
            hardness = 0.85f,
            streamline = 0.30f
        )

        val DEFAULT_PRESETS: List<BrushPreset> = listOf(
            PENCIL_2B,
            TECHNICAL_PEN,
            STUDIO_BRUSH_PEN,
            CALLIGRAPHY_NIB,
            CLASSIC_OIL,
            WET_ACRYLIC,
            BRISTLE_FAN,
            SOFT_AIRBRUSH,
            HARD_AIRBRUSH,
            WATERCOLOR_GLAZE,
            SMUDGE_BLENDER,
            KNEADED_ERASER
        )
    }
}
