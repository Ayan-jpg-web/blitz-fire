package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.CustomHudConfig
import com.example.data.model.HudElementConfig
import com.example.data.model.HudElementId
import com.example.data.model.HudLayoutPreset
import com.example.data.model.SensitivityPreset
import com.example.data.model.SensitivitySettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

class ControlSettingsRepository(context: Context) {

    companion object {
        private const val PREFS_NAME = "blitzfire_hud_sensitivity_prefs"
        private const val KEY_HUD_JSON = "custom_hud_json"
        private const val KEY_SENS_JSON = "sensitivity_json"

        @Volatile
        private var instance: ControlSettingsRepository? = null

        fun get(context: Context): ControlSettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: ControlSettingsRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _hudConfig = MutableStateFlow(loadHudConfig())
    val hudConfig: StateFlow<CustomHudConfig> = _hudConfig.asStateFlow()

    private val _sensitivity = MutableStateFlow(loadSensitivity())
    val sensitivity: StateFlow<SensitivitySettings> = _sensitivity.asStateFlow()

    fun saveHudConfig(config: CustomHudConfig) {
        val sanitized = config.copy(
            elements = config.elements.mapValues { (_, v) -> v.clamped() }
        )
        _hudConfig.value = sanitized
        persistHudConfig(sanitized)
    }

    fun applyHudPreset(preset: HudLayoutPreset) {
        saveHudConfig(
            CustomHudConfig(
                activePreset = preset,
                elements = CustomHudConfig.defaultHudMap(preset)
            )
        )
    }

    fun saveSensitivity(settings: SensitivitySettings) {
        val sanitized = settings.clamped()
        _sensitivity.value = sanitized
        persistSensitivity(sanitized)
    }

    fun applySensitivityPreset(preset: SensitivityPreset) {
        val currentInvertY = _sensitivity.value.invertY
        saveSensitivity(SensitivitySettings.forPreset(preset, currentInvertY))
    }

    fun resetAll() {
        applyHudPreset(HudLayoutPreset.TWO_FINGER)
        saveSensitivity(SensitivitySettings.forPreset(SensitivityPreset.MEDIUM, currentInvertY = false))
    }

    private fun loadHudConfig(): CustomHudConfig {
        val raw = prefs.getString(KEY_HUD_JSON, null) ?: return CustomHudConfig()
        return runCatching {
            val json = JSONObject(raw)
            val presetKey = json.optString("preset", HudLayoutPreset.TWO_FINGER.key)
            val preset = HudLayoutPreset.entries.firstOrNull { it.key == presetKey } ?: HudLayoutPreset.TWO_FINGER
            val defaults = CustomHudConfig.defaultHudMap(preset).toMutableMap()
            val elemObj = json.optJSONObject("elements")
            if (elemObj != null) {
                HudElementId.entries.forEach { id ->
                    val o = elemObj.optJSONObject(id.key)
                    if (o != null) {
                        defaults[id] = HudElementConfig(
                            xFraction = o.optDouble("x", defaults.getValue(id).xFraction.toDouble()).toFloat(),
                            yFraction = o.optDouble("y", defaults.getValue(id).yFraction.toDouble()).toFloat(),
                            scale = o.optDouble("scale", defaults.getValue(id).scale.toDouble()).toFloat(),
                            alpha = o.optDouble("alpha", defaults.getValue(id).alpha.toDouble()).toFloat()
                        ).clamped()
                    }
                }
            }
            CustomHudConfig(activePreset = preset, elements = defaults)
        }.getOrDefault(CustomHudConfig())
    }

    private fun persistHudConfig(config: CustomHudConfig) {
        val elemObj = JSONObject()
        config.elements.forEach { (id, cfg) ->
            elemObj.put(
                id.key,
                JSONObject().apply {
                    put("x", cfg.xFraction.toDouble())
                    put("y", cfg.yFraction.toDouble())
                    put("scale", cfg.scale.toDouble())
                    put("alpha", cfg.alpha.toDouble())
                }
            )
        }
        val root = JSONObject().apply {
            put("preset", config.activePreset.key)
            put("elements", elemObj)
        }
        prefs.edit().putString(KEY_HUD_JSON, root.toString()).apply()
    }

    private fun loadSensitivity(): SensitivitySettings {
        val raw = prefs.getString(KEY_SENS_JSON, null) ?: return SensitivitySettings()
        return runCatching {
            val json = JSONObject(raw)
            val presetKey = json.optString("preset", SensitivityPreset.MEDIUM.key)
            val preset = SensitivityPreset.entries.firstOrNull { it.key == presetKey } ?: SensitivityPreset.MEDIUM
            SensitivitySettings(
                preset = preset,
                generalCameraSens = json.optDouble("general", 1.0).toFloat(),
                redDotAdsSens = json.optDouble("redDot", 0.85).toFloat(),
                thermal4xSens = json.optDouble("thermal4x", 0.65).toFloat(),
                fireDragAimSens = json.optDouble("fireDrag", 0.75).toFloat(),
                invertY = json.optBoolean("invertY", false)
            ).clamped()
        }.getOrDefault(SensitivitySettings())
    }

    private fun persistSensitivity(s: SensitivitySettings) {
        val root = JSONObject().apply {
            put("preset", s.preset.key)
            put("general", s.generalCameraSens.toDouble())
            put("redDot", s.redDotAdsSens.toDouble())
            put("thermal4x", s.thermal4xSens.toDouble())
            put("fireDrag", s.fireDragAimSens.toDouble())
            put("invertY", s.invertY)
        }
        prefs.edit().putString(KEY_SENS_JSON, root.toString()).apply()
    }
}
