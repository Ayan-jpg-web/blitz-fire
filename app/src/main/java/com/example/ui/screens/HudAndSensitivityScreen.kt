package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AirlineSeatReclineNormal
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CustomHudConfig
import com.example.data.model.HudElementConfig
import com.example.data.model.HudElementId
import com.example.data.model.HudLayoutPreset
import com.example.data.model.SensitivityPreset
import com.example.data.model.SensitivitySettings
import com.example.data.repository.ControlSettingsRepository
import com.example.ui.theme.BlazeGold
import com.example.ui.theme.BlazeOrange
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DiamondCyan
import com.example.ui.theme.MythicCrimson
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TacticalGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

enum class ControlStudioTab(val label: String) {
    CUSTOM_HUD("CUSTOM HUD"),
    SENSITIVITY("SENSITIVITY")
}

@Composable
fun HudAndSensitivityScreen(
    initialTab: ControlStudioTab = ControlStudioTab.CUSTOM_HUD,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onClose() }

    val context = LocalContext.current
    val repo = remember(context) { ControlSettingsRepository.get(context) }
    val savedHud by repo.hudConfig.collectAsStateWithLifecycle()
    val savedSens by repo.sensitivity.collectAsStateWithLifecycle()

    var draftHud by remember(savedHud) { mutableStateOf(savedHud) }
    var draftSens by remember(savedSens) { mutableStateOf(savedSens) }
    var activeTab by remember { mutableStateOf(initialTab) }
    var selectedElement by remember { mutableStateOf(HudElementId.FIRE_BUTTON) }
    var showGrid by remember { mutableStateOf(true) }
    var inspectorExpanded by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .displayCutoutPadding()
    ) {
        // Top Command Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ObsidianSurface)
                .border(1.dp, ObsidianBorder)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("hud_studio_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Text(
                    text = "CONTROLS STUDIO",
                    style = MaterialTheme.typography.titleMedium,
                    color = BlazeGold,
                    fontWeight = FontWeight.Bold
                )

                FilterChip(
                    selected = activeTab == ControlStudioTab.CUSTOM_HUD,
                    onClick = { activeTab = ControlStudioTab.CUSTOM_HUD },
                    leadingIcon = { Icon(Icons.Default.Gamepad, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text(ControlStudioTab.CUSTOM_HUD.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BlazeOrange,
                        selectedLabelColor = ObsidianBg,
                        selectedLeadingIconColor = ObsidianBg
                    ),
                    modifier = Modifier.testTag("tab_custom_hud")
                )
                FilterChip(
                    selected = activeTab == ControlStudioTab.SENSITIVITY,
                    onClick = { activeTab = ControlStudioTab.SENSITIVITY },
                    leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text(ControlStudioTab.SENSITIVITY.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DiamondCyan,
                        selectedLabelColor = ObsidianBg,
                        selectedLeadingIconColor = ObsidianBg
                    ),
                    modifier = Modifier.testTag("tab_sensitivity")
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        if (activeTab == ControlStudioTab.CUSTOM_HUD) {
                            val preset = draftHud.activePreset
                            draftHud = CustomHudConfig(
                                activePreset = preset,
                                elements = CustomHudConfig.defaultHudMap(preset)
                            )
                        } else {
                            draftSens = SensitivitySettings.forPreset(SensitivityPreset.MEDIUM, currentInvertY = false)
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("hud_studio_reset_button")
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, tint = DangerRed, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("RESET", style = MaterialTheme.typography.labelLarge, color = DangerRed)
                }

                Button(
                    onClick = {
                        repo.saveHudConfig(draftHud)
                        repo.saveSensitivity(draftSens)
                        onClose()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TacticalGreen,
                        contentColor = ObsidianBg
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("hud_studio_save_button")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("SAVE & APPLY", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
        }

        when (activeTab) {
            ControlStudioTab.CUSTOM_HUD -> {
                CustomHudEditorCanvas(
                    hudConfig = draftHud,
                    selectedElement = selectedElement,
                    showGrid = showGrid,
                    inspectorExpanded = inspectorExpanded,
                    onToggleGrid = { showGrid = !showGrid },
                    onToggleInspector = { inspectorExpanded = !inspectorExpanded },
                    onSelectElement = { selectedElement = it },
                    onSelectPreset = { preset ->
                        draftHud = CustomHudConfig(
                            activePreset = preset,
                            elements = CustomHudConfig.defaultHudMap(preset)
                        )
                    },
                    onUpdateElement = { id, updated ->
                        val nextMap = draftHud.elements.toMutableMap()
                        nextMap[id] = updated.clamped()
                        draftHud = draftHud.copy(elements = nextMap)
                    },
                    onResetElement = { id ->
                        val def = CustomHudConfig.defaultHudMap(draftHud.activePreset).getValue(id)
                        val nextMap = draftHud.elements.toMutableMap()
                        nextMap[id] = def
                        draftHud = draftHud.copy(elements = nextMap)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            ControlStudioTab.SENSITIVITY -> {
                SensitivityStudioPane(
                    settings = draftSens,
                    onUpdate = { draftSens = it.clamped() },
                    onSelectPreset = { preset ->
                        draftSens = SensitivitySettings.forPreset(preset, draftSens.invertY)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CustomHudEditorCanvas(
    hudConfig: CustomHudConfig,
    selectedElement: HudElementId,
    showGrid: Boolean,
    inspectorExpanded: Boolean,
    onToggleGrid: () -> Unit,
    onToggleInspector: () -> Unit,
    onSelectElement: (HudElementId) -> Unit,
    onSelectPreset: (HudLayoutPreset) -> Unit,
    onUpdateElement: (HudElementId, HudElementConfig) -> Unit,
    onResetElement: (HudElementId) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        val widthPx = with(density) { maxWidth.toPx().coerceAtLeast(1f) }
        val heightPx = with(density) { maxHeight.toPx().coerceAtLeast(1f) }

        // Tactical Grid & Reference Horizon
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (showGrid) {
                val cols = 16
                val rows = 9
                val stepX = size.width / cols
                val stepY = size.height / rows
                for (c in 1 until cols) {
                    val x = c * stepX
                    drawLine(
                        color = if (c == cols / 2) DiamondCyan.copy(alpha = 0.24f) else ObsidianBorder.copy(alpha = 0.35f),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = if (c == cols / 2) 2f else 1f
                    )
                }
                for (r in 1 until rows) {
                    val y = r * stepY
                    drawLine(
                        color = if (r == rows / 2) DiamondCyan.copy(alpha = 0.24f) else ObsidianBorder.copy(alpha = 0.35f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = if (r == rows / 2) 2f else 1f
                    )
                }
            }
            // Center Reticle Reference
            val cx = size.width * 0.5f
            val cy = size.height * 0.5f
            drawCircle(color = BlazeGold.copy(alpha = 0.35f), radius = 14f, center = Offset(cx, cy))
        }

        // Fixed Top-Left & Top-Center Safe Reference Placeholders
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .alpha(0.55f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(color = ObsidianCard, shape = RoundedCornerShape(6.dp)) {
                Text("ALIVE 16", style = MaterialTheme.typography.labelSmall, color = BlazeGold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
            Surface(color = ObsidianCard, shape = RoundedCornerShape(6.dp)) {
                Text("KILLS 0", style = MaterialTheme.typography.labelSmall, color = MythicCrimson, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }

        // Draggable HUD Elements
        HudElementId.entries.forEach { elemId ->
            val cfg = hudConfig.configFor(elemId)
            val isSelected = elemId == selectedElement
            val (baseW, baseH) = hudElementBaseSize(elemId)
            val baseWPx = with(density) { baseW.toPx() }
            val baseHPx = with(density) { baseH.toPx() }

            val centerPxX = cfg.xFraction * widthPx
            val centerPxY = cfg.yFraction * heightPx
            val topLeftX = (centerPxX - baseWPx * 0.5f).coerceIn(0f, (widthPx - baseWPx).coerceAtLeast(0f))
            val topLeftY = (centerPxY - baseHPx * 0.5f).coerceIn(0f, (heightPx - baseHPx).coerceAtLeast(0f))

            Box(
                modifier = Modifier
                    .offset { IntOffset(topLeftX.roundToInt(), topLeftY.roundToInt()) }
                    .size(baseW, baseH)
                    .graphicsLayer {
                        scaleX = cfg.scale
                        scaleY = cfg.scale
                        alpha = cfg.alpha
                    }
                    .pointerInput(elemId, widthPx, heightPx, cfg) {
                        detectDragGestures(
                            onDragStart = { onSelectElement(elemId) },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val currentCfg = hudConfig.configFor(elemId)
                                val nextX = currentCfg.xFraction + (dragAmount.x / widthPx)
                                val nextY = currentCfg.yFraction + (dragAmount.y / heightPx)
                                onUpdateElement(elemId, currentCfg.copy(xFraction = nextX, yFraction = nextY))
                            }
                        )
                    }
                    .clickable { onSelectElement(elemId) }
                    .border(
                        width = if (isSelected) 2.5.dp else 1.dp,
                        color = if (isSelected) BlazeGold else DiamondCyan.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .testTag("hud_editor_item_${elemId.key}"),
                contentAlignment = Alignment.Center
            ) {
                HudEditorElementPreview(elemId = elemId, isSelected = isSelected)
            }
        }

        // Floating Inspector Panel at Top-Center
        val selCfg = hudConfig.configFor(selectedElement)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(ObsidianSurface.copy(alpha = 0.94f))
                .border(1.5.dp, BlazeGold.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "DRAG BUTTONS TO MOVE • EDITING: ${selectedElement.label.uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = BlazeGold,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onToggleGrid,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GridOn,
                        contentDescription = "Toggle Grid",
                        tint = if (showGrid) DiamondCyan else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Surface(
                    color = ObsidianCard,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.clickable(onClick = onToggleInspector)
                ) {
                    Text(
                        text = if (inspectorExpanded) "HIDE PANEL" else "SHOW PANEL",
                        style = MaterialTheme.typography.labelSmall,
                        color = DiamondCyan,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            if (inspectorExpanded) {
                // Layout Presets Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HudLayoutPreset.entries.forEach { preset ->
                        val active = hudConfig.activePreset == preset
                        Surface(
                            color = if (active) BlazeOrange else ObsidianCard,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .border(1.dp, if (active) BlazeGold else ObsidianBorder, RoundedCornerShape(8.dp))
                                .clickable { onSelectPreset(preset) }
                                .testTag("hud_preset_${preset.key}")
                        ) {
                            Text(
                                text = preset.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (active) ObsidianBg else TextPrimary,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Surface(
                        color = ObsidianElevated,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .border(1.dp, DangerRed.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .clickable { onResetElement(selectedElement) }
                    ) {
                        Text(
                            text = "RESET BUTTON",
                            style = MaterialTheme.typography.labelSmall,
                            color = DangerRed,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }

                // Scale & Alpha Sliders
                Row(
                    modifier = Modifier.width(460.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SIZE: ${(selCfg.scale * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary
                        )
                        Slider(
                            value = selCfg.scale,
                            onValueChange = { next ->
                                onUpdateElement(selectedElement, selCfg.copy(scale = next))
                            },
                            valueRange = 0.65f..1.55f,
                            colors = SliderDefaults.colors(
                                thumbColor = BlazeGold,
                                activeTrackColor = BlazeOrange
                            ),
                            modifier = Modifier
                                .height(28.dp)
                                .testTag("hud_scale_slider")
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "OPACITY: ${(selCfg.alpha * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary
                        )
                        Slider(
                            value = selCfg.alpha,
                            onValueChange = { next ->
                                onUpdateElement(selectedElement, selCfg.copy(alpha = next))
                            },
                            valueRange = 0.35f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = DiamondCyan,
                                activeTrackColor = DiamondCyan
                            ),
                            modifier = Modifier
                                .height(28.dp)
                                .testTag("hud_alpha_slider")
                        )
                    }
                }
            }
        }
    }
}

fun hudElementBaseSize(id: HudElementId): Pair<Dp, Dp> = when (id) {
    HudElementId.JOYSTICK -> 120.dp to 120.dp
    HudElementId.MINIMAP -> 104.dp to 104.dp
    HudElementId.WEAPON_SLOTS -> 260.dp to 96.dp
    HudElementId.FIRE_BUTTON -> 76.dp to 76.dp
    else -> 50.dp to 50.dp
}

@Composable
private fun HudEditorElementPreview(elemId: HudElementId, isSelected: Boolean) {
    val accent = if (isSelected) BlazeGold else TextPrimary
    when (elemId) {
        HudElementId.JOYSTICK -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(ObsidianCard.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(BlazeOrange)
                )
                Text("MOVE", style = MaterialTheme.typography.labelSmall, color = ObsidianBg, fontWeight = FontWeight.Bold)
            }
        }
        HudElementId.MINIMAP -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ObsidianCard.copy(alpha = 0.8f)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Map, contentDescription = null, tint = DiamondCyan, modifier = Modifier.size(28.dp))
                Text("MINIMAP", style = MaterialTheme.typography.labelSmall, color = DiamondCyan)
            }
        }
        HudElementId.WEAPON_SLOTS -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ObsidianCard.copy(alpha = 0.85f))
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(color = ObsidianElevated, shape = RoundedCornerShape(6.dp), modifier = Modifier.weight(1f)) {
                        Text("[1] AKM 30/30", style = MaterialTheme.typography.labelSmall, color = BlazeGold, modifier = Modifier.padding(6.dp), maxLines = 1)
                    }
                    Surface(color = ObsidianElevated, shape = RoundedCornerShape(6.dp), modifier = Modifier.weight(1f)) {
                        Text("[2] MP40 38/38", style = MaterialTheme.typography.labelSmall, color = TextPrimary, modifier = Modifier.padding(6.dp), maxLines = 1)
                    }
                }
                Surface(color = TacticalGreen.copy(alpha = 0.28f), shape = RoundedCornerShape(4.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("HP 200 • VEST 100", style = MaterialTheme.typography.labelSmall, color = TacticalGreen, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        }
        HudElementId.FIRE_BUTTON -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(BlazeOrange),
                contentAlignment = Alignment.Center
            ) {
                Text("FIRE", style = MaterialTheme.typography.titleSmall, color = ObsidianBg, fontWeight = FontWeight.ExtraBold)
            }
        }
        else -> {
            val icon = when (elemId) {
                HudElementId.SCOPE_BUTTON -> Icons.Default.CenterFocusStrong
                HudElementId.CROUCH_BUTTON -> Icons.Default.AirlineSeatReclineNormal
                HudElementId.JUMP_BUTTON -> Icons.Default.ArrowUpward
                HudElementId.DASH_BUTTON -> Icons.Default.Bolt
                HudElementId.GLOO_BUTTON -> Icons.Default.Shield
                HudElementId.MEDKIT_BUTTON -> Icons.Default.LocalHospital
                HudElementId.RELOAD_BUTTON -> Icons.Default.Refresh
                HudElementId.DRIVE_BUTTON -> Icons.Default.DirectionsCar
                else -> Icons.Default.Gamepad
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ObsidianCard.copy(alpha = 0.90f)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(icon, contentDescription = elemId.label, tint = accent, modifier = Modifier.size(18.dp))
                Text(elemId.shortLabel, style = MaterialTheme.typography.labelSmall, color = TextPrimary, fontSize = 8.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun SensitivityStudioPane(
    settings: SensitivitySettings,
    onUpdate: (SensitivitySettings) -> Unit,
    onSelectPreset: (SensitivityPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Left Column: Sensitivity Presets & Sliders
        Column(
            modifier = Modifier
                .weight(0.58f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp))
                .background(ObsidianCard)
                .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
                .padding(12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "CAMERA & SCOPE SENSITIVITY",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SensitivityPreset.entries.forEach { preset ->
                    val active = settings.preset == preset
                    Surface(
                        color = if (active) DiamondCyan else ObsidianElevated,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, if (active) DiamondCyan else ObsidianBorder, RoundedCornerShape(8.dp))
                            .clickable { onSelectPreset(preset) }
                            .testTag("sens_preset_${preset.key}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (active) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = ObsidianBg, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                            }
                            Text(
                                text = preset.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (active) ObsidianBg else TextPrimary,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            SensitivitySliderRow(
                title = "General / Free-Look Camera",
                subtitle = "Hip-fire rotation & 360° terrain scanning",
                value = settings.generalCameraSens,
                accent = BlazeOrange,
                testTag = "sens_slider_general",
                onValueChange = { onUpdate(settings.copy(generalCameraSens = it)) }
            )

            SensitivitySliderRow(
                title = "Red Dot / Holo ADS Scope",
                subtitle = "Precision tracking when ADS is active",
                value = settings.redDotAdsSens,
                accent = DiamondCyan,
                testTag = "sens_slider_red_dot",
                onValueChange = { onUpdate(settings.copy(redDotAdsSens = it)) }
            )

            SensitivitySliderRow(
                title = "Thermal 4X Scope ADS",
                subtitle = "Long-range magnified sniper / DMR aim",
                value = settings.thermal4xSens,
                accent = MythicCrimson,
                testTag = "sens_slider_thermal_4x",
                onValueChange = { onUpdate(settings.copy(thermal4xSens = it)) }
            )

            SensitivitySliderRow(
                title = "Fire-Button Drag Recoil Control",
                subtitle = "Aim pull sensitivity while holding Fire",
                value = settings.fireDragAimSens,
                accent = TacticalGreen,
                testTag = "sens_slider_fire_drag",
                onValueChange = { onUpdate(settings.copy(fireDragAimSens = it)) }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ObsidianElevated)
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
                    .clickable { onUpdate(settings.copy(invertY = !settings.invertY)) }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("sens_invert_y_row"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Invert Vertical Look (Y-Axis)", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Text("Flip vertical pitch drag direction", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Switch(
                    checked = settings.invertY,
                    onCheckedChange = { onUpdate(settings.copy(invertY = it)) },
                    colors = SwitchDefaults.colors(checkedThumbColor = ObsidianBg, checkedTrackColor = DiamondCyan)
                )
            }
        }

        // Right Column: Live Interactive Sensitivity Aim Test Pad
        SensitivityLiveTestPad(
            settings = settings,
            modifier = Modifier
                .weight(0.42f)
                .fillMaxHeight()
        )
    }
}

@Composable
private fun SensitivitySliderRow(
    title: String,
    subtitle: String,
    value: Float,
    accent: Color,
    testTag: String,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianElevated)
            .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Surface(
                color = accent.copy(alpha = 0.2f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            ) {
                Text(
                    text = "${(value * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0.25f..2.50f,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun SensitivityLiveTestPad(
    settings: SensitivitySettings,
    modifier: Modifier = Modifier
) {
    var testMode by remember { mutableStateOf(0) } // 0=HIP, 1=1X, 2=4X
    var reticleX by remember { mutableFloatStateOf(0.5f) }
    var reticleY by remember { mutableFloatStateOf(0.5f) }

    val activeMultiplier = when (testMode) {
        1 -> settings.redDotAdsSens
        2 -> settings.thermal4xSens
        else -> settings.generalCameraSens
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(ObsidianCard)
            .border(1.5.dp, DiamondCyan.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("LIVE AIM TEST PAD", style = MaterialTheme.typography.titleMedium, color = DiamondCyan)
                Text("Drag inside pad to feel sensitivity", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            OutlinedButton(
                onClick = { reticleX = 0.5f; reticleY = 0.5f },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text("CENTER", style = MaterialTheme.typography.labelSmall, color = BlazeGold)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("HIP-FIRE", "1X SCOPE", "4X SCOPE").forEachIndexed { idx, label ->
                val selected = testMode == idx
                Surface(
                    color = if (selected) BlazeOrange else ObsidianElevated,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { testMode = idx }
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) ObsidianBg else TextPrimary,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(vertical = 7.dp, horizontal = 6.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(ObsidianBg)
                .border(1.dp, ObsidianBorder, RoundedCornerShape(14.dp))
                .pointerInput(activeMultiplier, settings.invertY) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        val h = size.height.toFloat().coerceAtLeast(1f)
                        val ySign = if (settings.invertY) -1f else 1f
                        reticleX = (reticleX + (dragAmount.x / w) * activeMultiplier).coerceIn(0.06f, 0.94f)
                        reticleY = (reticleY + (dragAmount.y / h) * activeMultiplier * ySign).coerceIn(0.08f, 0.92f)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Static practice bullseyes
                val targets = listOf(
                    Offset(size.width * 0.24f, size.height * 0.35f),
                    Offset(size.width * 0.50f, size.height * 0.50f),
                    Offset(size.width * 0.78f, size.height * 0.32f),
                    Offset(size.width * 0.68f, size.height * 0.74f)
                )
                targets.forEach { t ->
                    drawCircle(color = MythicCrimson.copy(alpha = 0.25f), radius = 20.dp.toPx(), center = t)
                    drawCircle(color = MythicCrimson, radius = 6.dp.toPx(), center = t)
                }

                // Live Crosshair
                val rx = size.width * reticleX
                val ry = size.height * reticleY
                val gap = if (testMode == 0) 10.dp.toPx() else 6.dp.toPx()
                val arm = if (testMode == 2) 20.dp.toPx() else 12.dp.toPx()
                drawCircle(color = TacticalGreen, radius = 3.dp.toPx(), center = Offset(rx, ry))
                drawLine(TacticalGreen, Offset(rx - gap - arm, ry), Offset(rx - gap, ry), strokeWidth = 2.5f)
                drawLine(TacticalGreen, Offset(rx + gap, ry), Offset(rx + gap + arm, ry), strokeWidth = 2.5f)
                drawLine(TacticalGreen, Offset(rx, ry - gap - arm), Offset(rx, ry - gap), strokeWidth = 2.5f)
                drawLine(TacticalGreen, Offset(rx, ry + gap), Offset(rx, ry + gap + arm), strokeWidth = 2.5f)
            }

            Text(
                text = "ACTIVE MULTIPLIER: ${(activeMultiplier * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = BlazeGold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp)
            )
        }
    }
}
