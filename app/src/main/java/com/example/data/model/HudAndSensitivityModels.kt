package com.example.data.model

enum class HudElementId(
    val key: String,
    val label: String,
    val shortLabel: String
) {
    JOYSTICK("joystick", "Move Joystick", "MOVE"),
    MINIMAP("minimap", "Tactical Minimap", "MAP"),
    WEAPON_SLOTS("weapons", "Weapons & HP", "WEAPONS"),
    FIRE_BUTTON("fire", "Primary Fire", "FIRE"),
    SCOPE_BUTTON("scope", "Scope / ADS", "SCOPE"),
    CROUCH_BUTTON("crouch", "Crouch / Stand", "CROUCH"),
    JUMP_BUTTON("jump", "Jump", "JUMP"),
    DASH_BUTTON("dash", "Tactical Dash", "DASH"),
    GLOO_BUTTON("gloo", "Gloo Wall", "GLOO"),
    MEDKIT_BUTTON("medkit", "Medkit", "MED"),
    RELOAD_BUTTON("reload", "Reload", "RELOAD"),
    DRIVE_BUTTON("drive", "Vehicle Drive", "DRIVE");

    companion object {
        fun fromKey(key: String): HudElementId? = entries.firstOrNull { it.key == key }
    }
}

data class HudElementConfig(
    val xFraction: Float,
    val yFraction: Float,
    val scale: Float = 1.0f,
    val alpha: Float = 0.92f
) {
    fun clamped(): HudElementConfig = copy(
        xFraction = xFraction.coerceIn(0.06f, 0.94f),
        yFraction = yFraction.coerceIn(0.12f, 0.92f),
        scale = scale.coerceIn(0.65f, 1.55f),
        alpha = alpha.coerceIn(0.35f, 1.0f)
    )
}

enum class HudLayoutPreset(val key: String, val label: String, val description: String) {
    TWO_FINGER("2_finger", "2-Finger Classic", "Balanced BGMI thumb layout"),
    THREE_FINGER("3_finger", "3-Finger Claw", "Top-left index Fire + thumb aim"),
    FOUR_FINGER("4_finger", "4-Finger Pro", "Top Fire & Scope + agile thumb cluster")
}

data class CustomHudConfig(
    val activePreset: HudLayoutPreset = HudLayoutPreset.TWO_FINGER,
    val elements: Map<HudElementId, HudElementConfig> = defaultHudMap(HudLayoutPreset.TWO_FINGER)
) {
    fun configFor(id: HudElementId): HudElementConfig =
        elements[id] ?: defaultHudMap(HudLayoutPreset.TWO_FINGER).getValue(id)

    companion object {
        fun defaultHudMap(preset: HudLayoutPreset): Map<HudElementId, HudElementConfig> = when (preset) {
            HudLayoutPreset.TWO_FINGER -> mapOf(
                HudElementId.JOYSTICK to HudElementConfig(0.11f, 0.78f, 1.0f, 0.92f),
                HudElementId.MINIMAP to HudElementConfig(0.90f, 0.17f, 1.0f, 0.92f),
                HudElementId.WEAPON_SLOTS to HudElementConfig(0.41f, 0.85f, 1.0f, 0.94f),
                HudElementId.MEDKIT_BUTTON to HudElementConfig(0.66f, 0.72f, 1.0f, 0.92f),
                HudElementId.RELOAD_BUTTON to HudElementConfig(0.66f, 0.88f, 1.0f, 0.92f),
                HudElementId.GLOO_BUTTON to HudElementConfig(0.74f, 0.72f, 1.0f, 0.92f),
                HudElementId.DASH_BUTTON to HudElementConfig(0.74f, 0.88f, 1.0f, 0.92f),
                HudElementId.SCOPE_BUTTON to HudElementConfig(0.82f, 0.72f, 1.0f, 0.92f),
                HudElementId.CROUCH_BUTTON to HudElementConfig(0.82f, 0.88f, 1.0f, 0.92f),
                HudElementId.DRIVE_BUTTON to HudElementConfig(0.91f, 0.49f, 1.0f, 0.92f),
                HudElementId.JUMP_BUTTON to HudElementConfig(0.91f, 0.64f, 1.0f, 0.92f),
                HudElementId.FIRE_BUTTON to HudElementConfig(0.91f, 0.83f, 1.05f, 0.96f)
            )
            HudLayoutPreset.THREE_FINGER -> mapOf(
                HudElementId.JOYSTICK to HudElementConfig(0.12f, 0.78f, 1.0f, 0.92f),
                HudElementId.FIRE_BUTTON to HudElementConfig(0.13f, 0.34f, 1.12f, 0.95f),
                HudElementId.MINIMAP to HudElementConfig(0.90f, 0.17f, 0.95f, 0.90f),
                HudElementId.WEAPON_SLOTS to HudElementConfig(0.44f, 0.85f, 0.95f, 0.94f),
                HudElementId.MEDKIT_BUTTON to HudElementConfig(0.68f, 0.72f, 0.95f, 0.90f),
                HudElementId.RELOAD_BUTTON to HudElementConfig(0.68f, 0.88f, 0.95f, 0.90f),
                HudElementId.GLOO_BUTTON to HudElementConfig(0.76f, 0.72f, 1.0f, 0.92f),
                HudElementId.DASH_BUTTON to HudElementConfig(0.76f, 0.88f, 1.0f, 0.92f),
                HudElementId.SCOPE_BUTTON to HudElementConfig(0.85f, 0.64f, 1.08f, 0.95f),
                HudElementId.CROUCH_BUTTON to HudElementConfig(0.85f, 0.85f, 1.0f, 0.92f),
                HudElementId.DRIVE_BUTTON to HudElementConfig(0.92f, 0.49f, 0.95f, 0.90f),
                HudElementId.JUMP_BUTTON to HudElementConfig(0.92f, 0.74f, 1.05f, 0.94f)
            )
            HudLayoutPreset.FOUR_FINGER -> mapOf(
                HudElementId.JOYSTICK to HudElementConfig(0.12f, 0.78f, 0.96f, 0.90f),
                HudElementId.FIRE_BUTTON to HudElementConfig(0.13f, 0.33f, 1.15f, 0.96f),
                HudElementId.SCOPE_BUTTON to HudElementConfig(0.78f, 0.31f, 1.10f, 0.96f),
                HudElementId.MINIMAP to HudElementConfig(0.91f, 0.18f, 0.90f, 0.88f),
                HudElementId.WEAPON_SLOTS to HudElementConfig(0.44f, 0.85f, 0.94f, 0.94f),
                HudElementId.MEDKIT_BUTTON to HudElementConfig(0.68f, 0.73f, 0.95f, 0.90f),
                HudElementId.RELOAD_BUTTON to HudElementConfig(0.68f, 0.88f, 0.95f, 0.90f),
                HudElementId.GLOO_BUTTON to HudElementConfig(0.76f, 0.73f, 1.0f, 0.92f),
                HudElementId.DASH_BUTTON to HudElementConfig(0.76f, 0.88f, 1.0f, 0.92f),
                HudElementId.CROUCH_BUTTON to HudElementConfig(0.85f, 0.84f, 1.05f, 0.94f),
                HudElementId.DRIVE_BUTTON to HudElementConfig(0.91f, 0.50f, 0.95f, 0.90f),
                HudElementId.JUMP_BUTTON to HudElementConfig(0.91f, 0.70f, 1.08f, 0.94f)
            )
        }
    }
}

enum class SensitivityPreset(val key: String, val label: String) {
    LOW("low", "Low Precision"),
    MEDIUM("medium", "Balanced"),
    HIGH("high", "High Reflex"),
    PRO_CLAW("pro_claw", "Pro Flick")
}

data class SensitivitySettings(
    val preset: SensitivityPreset = SensitivityPreset.MEDIUM,
    val generalCameraSens: Float = 1.00f,
    val redDotAdsSens: Float = 0.85f,
    val thermal4xSens: Float = 0.65f,
    val fireDragAimSens: Float = 0.75f,
    val invertY: Boolean = false
) {
    fun clamped(): SensitivitySettings = copy(
        generalCameraSens = generalCameraSens.coerceIn(0.25f, 2.50f),
        redDotAdsSens = redDotAdsSens.coerceIn(0.25f, 2.50f),
        thermal4xSens = thermal4xSens.coerceIn(0.25f, 2.50f),
        fireDragAimSens = fireDragAimSens.coerceIn(0.25f, 2.50f)
    )

    companion object {
        fun forPreset(preset: SensitivityPreset, currentInvertY: Boolean = false): SensitivitySettings = when (preset) {
            SensitivityPreset.LOW -> SensitivitySettings(
                preset = preset,
                generalCameraSens = 0.65f,
                redDotAdsSens = 0.55f,
                thermal4xSens = 0.42f,
                fireDragAimSens = 0.50f,
                invertY = currentInvertY
            )
            SensitivityPreset.MEDIUM -> SensitivitySettings(
                preset = preset,
                generalCameraSens = 1.00f,
                redDotAdsSens = 0.85f,
                thermal4xSens = 0.65f,
                fireDragAimSens = 0.75f,
                invertY = currentInvertY
            )
            SensitivityPreset.HIGH -> SensitivitySettings(
                preset = preset,
                generalCameraSens = 1.50f,
                redDotAdsSens = 1.25f,
                thermal4xSens = 0.95f,
                fireDragAimSens = 1.15f,
                invertY = currentInvertY
            )
            SensitivityPreset.PRO_CLAW -> SensitivitySettings(
                preset = preset,
                generalCameraSens = 1.95f,
                redDotAdsSens = 1.55f,
                thermal4xSens = 1.15f,
                fireDragAimSens = 1.45f,
                invertY = currentInvertY
            )
        }
    }
}
