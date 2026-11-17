package com.example.ui.arena

import android.opengl.GLSurfaceView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AirlineSeatReclineNormal
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.TacticalVoiceCommsManager
import com.example.ai.VoiceCommsState
import com.example.data.model.BattleMapId
import com.example.data.model.CustomHudConfig
import com.example.data.model.HudElementId
import com.example.data.model.MatchRecord
import com.example.data.model.OwnedItem
import com.example.data.model.UserProfile
import com.example.data.repository.ControlSettingsRepository
import com.example.ui.screens.HudAndSensitivityScreen
import com.example.ui.screens.hudElementBaseSize
import com.example.ui.theme.BlazeGold
import com.example.ui.theme.BlazeOrange
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DiamondCyan
import com.example.ui.theme.MythicCrimson
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.StormBlue
import com.example.ui.theme.TacticalGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun BattleArenaScreen(
    mode: String,
    mapId: BattleMapId,
    profile: UserProfile,
    inventory: Map<String, OwnedItem> = emptyMap(),
    commsManager: TacticalVoiceCommsManager? = null,
    onOpenVoiceComms: () -> Unit = {},
    savedMatchRecord: MatchRecord?,
    onMatchEnded: (ArenaMatchOutcome) -> Unit,
    onExitToLobby: () -> Unit
) {
    BackHandler {
        onExitToLobby()
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val engine = remember(mode, mapId, profile.userId) { BattleEngine(mode, profile, inventory, mapId) }
    val renderer = remember(engine) { Arena3DRenderer(engine) }
    val glSurfaceView = remember(context, renderer) {
        GLSurfaceView(context).apply {
            setEGLContextClientVersion(3)
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
    }

    DisposableEffect(lifecycleOwner, glSurfaceView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> glSurfaceView.onResume()
                Lifecycle.Event.ON_PAUSE -> glSurfaceView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            glSurfaceView.onPause()
        }
    }

    val controlRepo = remember(context) { ControlSettingsRepository.get(context) }
    val hudConfig by controlRepo.hudConfig.collectAsStateWithLifecycle()
    val sensitivity by controlRepo.sensitivity.collectAsStateWithLifecycle()
    var showControlStudio by remember { mutableStateOf(false) }

    LaunchedEffect(engine, sensitivity) {
        engine.updateSensitivity(sensitivity)
    }

    var frameTick by remember { mutableIntStateOf(0) }
    var joyX by remember { mutableFloatStateOf(0f) }
    var joyY by remember { mutableFloatStateOf(0f) }
    var isFiring by remember { mutableStateOf(false) }
    var outcomeReported by remember { mutableStateOf(false) }
    val textMeasurer = rememberTextMeasurer()

    val voiceState by (commsManager?.state?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf<VoiceCommsState>(VoiceCommsState.Idle) })
    val isVoiceLive = voiceState is VoiceCommsState.Live

    LaunchedEffect(engine) {
        var lastNs = 0L
        while (true) {
            withFrameNanos { frameNs ->
                if (lastNs != 0L) {
                    val dt = (frameNs - lastNs) / 1_000_000_000f
                    engine.step(dt, joyX, joyY, isFiring)
                    frameTick++
                    val outcome = engine.matchOutcome
                    if (outcome != null && !outcomeReported) {
                        outcomeReported = true
                        onMatchEnded(outcome)
                    }
                }
                lastNs = frameNs
            }
        }
    }

    val currentTick = frameTick

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        // Real-Time OpenGL ES 2.0 3D Shooter World
        AndroidView(
            factory = { glSurfaceView },
            modifier = Modifier.fillMaxSize()
        )

        // Full-Screen Drag-to-Look Camera Layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(engine) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        engine.rotateCamera(dragAmount.x, dragAmount.y)
                    }
                }
        )

        // 3D Projected World Overlay: Enemy HP Bars, Damage Popups, Dynamic BGMI Crosshair
        Arena3DOverlayCanvas(
            engine = engine,
            renderer = renderer,
            tick = currentTick,
            textMeasurer = textMeasurer,
            modifier = Modifier.fillMaxSize()
        )

        // Hit Vignette Flash
        if (engine.hitFlashTimer > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(8.dp, DangerRed.copy(alpha = (engine.hitFlashTimer * 3f).coerceIn(0f, 0.7f)))
            )
        }

        // TOP STATUS STRIP: Alive/Kills + Squad Voice + HUD Settings + Exit + Kill Feed + Compass
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .displayCutoutPadding()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.TopStart),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HudCounterBadge("ALIVE", "${engine.aliveCount()}", BlazeGold)
                    HudCounterBadge("KILLS", "${engine.player.kills}", MythicCrimson)

                    if (commsManager != null) {
                        IconButton(
                            onClick = onOpenVoiceComms,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(
                                    if (isVoiceLive) TacticalGreen.copy(alpha = 0.25f)
                                    else ObsidianCard.copy(alpha = 0.88f)
                                )
                                .border(
                                    1.dp,
                                    if (isVoiceLive) TacticalGreen else DiamondCyan.copy(alpha = 0.6f),
                                    RoundedCornerShape(9.dp)
                                )
                                .testTag("arena_voice_button")
                        ) {
                            Icon(
                                imageVector = if (isVoiceLive) Icons.Default.GraphicEq else Icons.Default.Mic,
                                contentDescription = "Squad Voice Radio",
                                tint = if (isVoiceLive) TacticalGreen else DiamondCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { showControlStudio = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(ObsidianCard.copy(alpha = 0.88f))
                            .border(1.dp, BlazeGold.copy(alpha = 0.55f), RoundedCornerShape(9.dp))
                            .testTag("arena_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Customize HUD & Sensitivity",
                            tint = BlazeGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onExitToLobby,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(ObsidianCard.copy(alpha = 0.88f))
                            .border(1.dp, ObsidianBorder, RoundedCornerShape(9.dp))
                            .testTag("arena_exit_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Exit Match",
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                engine.killFeed.take(3).forEach { entry ->
                    Surface(
                        color = ObsidianBg.copy(alpha = 0.74f),
                        shape = RoundedCornerShape(5.dp)
                    ) {
                        Text(
                            text = entry.text,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (entry.isPlayerKill) BlazeGold else TextPrimary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                BgmiCompassStrip(
                    bearingDeg = engine.compassBearingDegrees(),
                    zoneRadiusMeters = (engine.zoneRadius / 10f).toInt(),
                    mapName = "${mapId.name} ${if (engine.mapSizeMeters >= 1000) "1KM" else "${engine.mapSizeMeters}M"}"
                )
                if (engine.drivingVehicleIndex != null) {
                    val kmh = engine.activeVehicleSpeedKmh()
                    val vehHp = engine.activeVehicleHpRatio()
                    Row(
                        modifier = Modifier
                            .widthIn(min = 210.dp, max = 270.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ObsidianBg.copy(alpha = 0.86f))
                            .border(1.dp, BlazeGold.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "$kmh KM/H", style = MaterialTheme.typography.labelSmall, color = BlazeGold, fontWeight = FontWeight.Bold)
                        LinearProgressIndicator(
                            progress = { vehHp },
                            color = if (vehHp > 0.35f) TacticalGreen else DangerRed,
                            trackColor = ObsidianCard,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                        Text(text = "VEH ${(vehHp * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = TextPrimary, fontSize = 9.sp)
                    }
                } else if (engine.isPlayerIndoors()) {
                    Surface(
                        color = ObsidianBg.copy(alpha = 0.82f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.border(1.dp, DiamondCyan.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                    ) {
                        Text(
                            text = "INDOOR COVER • LOOT INSIDE",
                            style = MaterialTheme.typography.labelSmall,
                            color = DiamondCyan,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // FULLY CUSTOMIZABLE 12-ELEMENT HUD LAYER (Positions, Scale & Alpha from ControlSettingsRepository)
        val density = LocalDensity.current
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .displayCutoutPadding()
        ) {
            val maxWPx = with(density) { maxWidth.toPx().coerceAtLeast(1f) }
            val maxHPx = with(density) { maxHeight.toPx().coerceAtLeast(1f) }

            // 1. Minimap
            PositionedHudElement(HudElementId.MINIMAP, hudConfig, maxWPx, maxHPx, density) {
                Box(modifier = Modifier.fillMaxSize()) {
                    MinimapCanvas(
                        engine = engine,
                        tick = currentTick,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ObsidianBg.copy(alpha = 0.84f))
                            .border(1.5.dp, Color(mapId.accentHex).copy(alpha = 0.72f), RoundedCornerShape(10.dp))
                    )
                    Surface(
                        color = ObsidianBg.copy(alpha = 0.82f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                    ) {
                        Text(
                            text = mapId.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(mapId.accentHex),
                            fontSize = 8.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            // 2. Virtual Joystick
            PositionedHudElement(HudElementId.JOYSTICK, hudConfig, maxWPx, maxHPx, density) {
                VirtualJoystick(
                    onMove = { dx, dy ->
                        joyX = dx
                        joyY = dy
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 3. Weapon Slots & HP/Vest Cluster
            PositionedHudElement(HudElementId.WEAPON_SLOTS, hudConfig, maxWPx, maxHPx, density) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Bottom),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val rifleReloading = engine.reloadTimer > 0f && engine.activeWeaponIsRifle
                        val smgReloading = engine.reloadTimer > 0f && !engine.activeWeaponIsRifle
                        BgmiWeaponSlotCard(
                            slotNumber = "1",
                            weaponName = engine.equippedRifle.name,
                            ammoLabel = if (rifleReloading) "RELOAD" else "${engine.rifleAmmo}/${engine.rifleMaxAmmo}",
                            selected = engine.activeWeaponIsRifle,
                            accent = Color(engine.equippedRifle.accentHex),
                            onClick = { if (!engine.activeWeaponIsRifle) engine.switchWeapon() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag(if (engine.activeWeaponIsRifle) "arena_weapon_slot_1" else "arena_weapon_switch")
                        )
                        BgmiWeaponSlotCard(
                            slotNumber = "2",
                            weaponName = engine.equippedSmg.name,
                            ammoLabel = if (smgReloading) "RELOAD" else "${engine.smgAmmo}/${engine.smgMaxAmmo}",
                            selected = !engine.activeWeaponIsRifle,
                            accent = Color(engine.equippedSmg.accentHex),
                            onClick = { if (engine.activeWeaponIsRifle) engine.switchWeapon() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag(if (!engine.activeWeaponIsRifle) "arena_weapon_slot_2" else "arena_weapon_switch")
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ObsidianBg.copy(alpha = 0.84f))
                            .border(1.dp, ObsidianBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "HP ${engine.player.hp.toInt()}/${engine.player.maxHp.toInt()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TacticalGreen,
                                fontSize = 9.sp
                            )
                            Text(
                                text = "VEST ${engine.player.shield.toInt()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = DiamondCyan,
                                fontSize = 9.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        LinearProgressIndicator(
                            progress = { (engine.player.hp / engine.player.maxHp).coerceIn(0f, 1f) },
                            color = if (engine.player.hp > engine.player.maxHp * 0.35f) Color.White else DangerRed,
                            trackColor = ObsidianCard,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        LinearProgressIndicator(
                            progress = { (engine.player.shield / engine.player.maxShield).coerceIn(0f, 1f) },
                            color = DiamondCyan,
                            trackColor = ObsidianCard,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }
                }
            }

            // 4. Medkit
            PositionedHudElement(HudElementId.MEDKIT_BUTTON, hudConfig, maxWPx, maxHPx, density) {
                TacticalAbilityButton(
                    icon = Icons.Default.LocalHospital,
                    label = "MED (${engine.medkits})",
                    accent = TacticalGreen,
                    enabled = engine.medkits > 0 && engine.healTimer <= 0f,
                    onClick = { engine.useMedkit() },
                    testTag = "arena_medkit_button"
                )
            }

            // 5. Reload
            PositionedHudElement(HudElementId.RELOAD_BUTTON, hudConfig, maxWPx, maxHPx, density) {
                TacticalAbilityButton(
                    icon = Icons.Default.Refresh,
                    label = "RELOAD",
                    accent = TextSecondary,
                    enabled = engine.reloadTimer <= 0f,
                    onClick = { engine.triggerReload() },
                    testTag = "arena_reload_button"
                )
            }

            // 6. Gloo Wall
            PositionedHudElement(HudElementId.GLOO_BUTTON, hudConfig, maxWPx, maxHPx, density) {
                TacticalAbilityButton(
                    icon = Icons.Default.Shield,
                    label = "GLOO (${engine.glooCharges})",
                    accent = Color(engine.equippedGloo.accentHex),
                    enabled = engine.glooCharges > 0,
                    onClick = { engine.deployGlooWall() },
                    testTag = "arena_gloo_button"
                )
            }

            // 7. Tactical Dash
            PositionedHudElement(HudElementId.DASH_BUTTON, hudConfig, maxWPx, maxHPx, density) {
                TacticalAbilityButton(
                    icon = Icons.Default.Bolt,
                    label = if (engine.dashCooldown > 0f) "${engine.dashCooldown.toInt()}s" else "DASH",
                    accent = DiamondCyan,
                    enabled = engine.dashCooldown <= 0f,
                    onClick = { engine.dash() },
                    testTag = "arena_dash_button"
                )
            }

            // 8. Scope / ADS
            PositionedHudElement(HudElementId.SCOPE_BUTTON, hudConfig, maxWPx, maxHPx, density) {
                TacticalAbilityButton(
                    icon = Icons.Default.CenterFocusStrong,
                    label = if (engine.isScoped) "ADS" else "SCOPE",
                    accent = if (engine.isScoped) MythicCrimson else BlazeGold,
                    enabled = true,
                    onClick = { engine.toggleScope() },
                    testTag = "arena_scope_button"
                )
            }

            // 9. Crouch / Stand
            PositionedHudElement(HudElementId.CROUCH_BUTTON, hudConfig, maxWPx, maxHPx, density) {
                TacticalAbilityButton(
                    icon = Icons.Default.AirlineSeatReclineNormal,
                    label = if (engine.isCrouched) "STAND" else "CROUCH",
                    accent = if (engine.isCrouched) BlazeGold else TextPrimary,
                    enabled = true,
                    onClick = { engine.toggleCrouch() },
                    testTag = "arena_crouch_button"
                )
            }

            // 10. Drive / Exit Vehicle (shown near vehicle or while driving)
            if (engine.canEnterOrExitVehicle()) {
                val isDriving = engine.drivingVehicleIndex != null
                PositionedHudElement(HudElementId.DRIVE_BUTTON, hudConfig, maxWPx, maxHPx, density) {
                    TacticalAbilityButton(
                        icon = Icons.Default.DirectionsCar,
                        label = if (isDriving) "EXIT" else "DRIVE",
                        accent = if (isDriving) MythicCrimson else BlazeGold,
                        enabled = true,
                        onClick = { engine.toggleDriveVehicle() },
                        testTag = "arena_drive_button"
                    )
                }
            }

            // 11. Jump
            PositionedHudElement(HudElementId.JUMP_BUTTON, hudConfig, maxWPx, maxHPx, density) {
                TacticalAbilityButton(
                    icon = Icons.Default.ArrowUpward,
                    label = "JUMP",
                    accent = BlazeOrange,
                    enabled = engine.player.z <= 0.2f && engine.drivingVehicleIndex == null,
                    onClick = { engine.jump() },
                    testTag = "arena_jump_button"
                )
            }

            // 12. Primary Hold-and-Aim Fire Trigger (Honors SensitivitySettings.fireDragAimSens!)
            PositionedHudElement(HudElementId.FIRE_BUTTON, hudConfig, maxWPx, maxHPx, density) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(if (isFiring) MythicCrimson else BlazeOrange)
                        .border(3.dp, BlazeGold, CircleShape)
                        .pointerInput(engine, sensitivity) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                isFiring = true
                                try {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull() ?: break
                                        if (!change.pressed) break
                                        val delta = change.position - change.previousPosition
                                        if (delta != Offset.Zero) {
                                            val mult = sensitivity.fireDragAimSens
                                            engine.rotateCamera(delta.x * mult, delta.y * mult)
                                        }
                                        change.consume()
                                    }
                                } finally {
                                    isFiring = false
                                }
                            }
                        }
                        .testTag("arena_fire_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isFiring) "FIRING" else "FIRE",
                        style = MaterialTheme.typography.titleSmall,
                        color = ObsidianBg,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // In-Match Custom HUD & Sensitivity Studio Overlay
        if (showControlStudio) {
            HudAndSensitivityScreen(
                onClose = { showControlStudio = false },
                modifier = Modifier.fillMaxSize()
            )
        }

        // End-of-Match Winner Winner Chicken Dinner / Eliminated Summary Overlay
        val outcome = engine.matchOutcome
        if (outcome != null) {
            MatchResultOverlay(
                outcome = outcome,
                mapTitle = mapId.title,
                savedRecord = savedMatchRecord,
                onBackToLobby = onExitToLobby
            )
        }
    }
}

@Composable
private fun PositionedHudElement(
    id: HudElementId,
    hudConfig: CustomHudConfig,
    maxWPx: Float,
    maxHPx: Float,
    density: Density,
    content: @Composable () -> Unit
) {
    val cfg = hudConfig.configFor(id)
    val (baseW, baseH) = hudElementBaseSize(id)
    val baseWPx = with(density) { baseW.toPx() }
    val baseHPx = with(density) { baseH.toPx() }
    val topLeftX = (cfg.xFraction * maxWPx - baseWPx * 0.5f).coerceIn(0f, (maxWPx - baseWPx).coerceAtLeast(0f))
    val topLeftY = (cfg.yFraction * maxHPx - baseHPx * 0.5f).coerceIn(0f, (maxHPx - baseHPx).coerceAtLeast(0f))

    Box(
        modifier = Modifier
            .offset { IntOffset(topLeftX.roundToInt(), topLeftY.roundToInt()) }
            .size(baseW, baseH)
            .graphicsLayer {
                scaleX = cfg.scale
                scaleY = cfg.scale
                alpha = cfg.alpha
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun BgmiCompassStrip(
    bearingDeg: Int,
    zoneRadiusMeters: Int,
    mapName: String
) {
    val cardinal = when (((bearingDeg + 22) % 360) / 45) {
        0 -> "N"
        1 -> "NE"
        2 -> "E"
        3 -> "SE"
        4 -> "S"
        5 -> "SW"
        6 -> "W"
        else -> "NW"
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(ObsidianBg.copy(alpha = 0.72f))
            .border(1.dp, ObsidianBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("W", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
            Text("NW", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
            Surface(color = BlazeGold.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                Text(
                    text = "$cardinal  $bearingDeg°",
                    style = MaterialTheme.typography.labelLarge,
                    color = BlazeGold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                )
            }
            Text("NE", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
            Text("E", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
        }
        Text(
            text = "$mapName • SAFE ZONE ${zoneRadiusMeters}m",
            style = MaterialTheme.typography.labelSmall,
            color = DiamondCyan,
            fontSize = 9.sp
        )
    }
}

@Composable
private fun BgmiWeaponSlotCard(
    slotNumber: String,
    weaponName: String,
    ammoLabel: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) ObsidianCard.copy(alpha = 0.94f) else ObsidianBg.copy(alpha = 0.72f))
            .border(if (selected) 1.6.dp else 1.dp, if (selected) accent else ObsidianBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "[$slotNumber] $weaponName",
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) accent else TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 9.sp
            )
            Text(
                text = ammoLabel,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) TextPrimary else TextSecondary
            )
        }
    }
}

@Composable
private fun Arena3DOverlayCanvas(
    engine: BattleEngine,
    renderer: Arena3DRenderer,
    tick: Int,
    textMeasurer: TextMeasurer,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        @Suppress("UNUSED_EXPRESSION")
        tick

        val cx = size.width * 0.5f
        val cy = size.height * 0.5f
        val hasLock = engine.lockedEnemyId != null
        val reticleColor = if (hasLock) MythicCrimson else Color.White.copy(alpha = 0.88f)
        val gap = if (engine.isScoped) 7f else 13f
        val arm = if (engine.isScoped) 16f else 11f

        drawCircle(color = reticleColor, radius = 2.5f, center = Offset(cx, cy))
        drawLine(reticleColor, Offset(cx - gap - arm, cy), Offset(cx - gap, cy), strokeWidth = 2.5f)
        drawLine(reticleColor, Offset(cx + gap, cy), Offset(cx + gap + arm, cy), strokeWidth = 2.5f)
        drawLine(reticleColor, Offset(cx, cy - gap - arm), Offset(cx, cy - gap), strokeWidth = 2.5f)
        drawLine(reticleColor, Offset(cx, cy + gap), Offset(cx, cy + gap + arm), strokeWidth = 2.5f)
        if (engine.isScoped) {
            drawCircle(
                color = reticleColor.copy(alpha = 0.65f),
                radius = 54f,
                center = Offset(cx, cy),
                style = Stroke(width = 2f)
            )
        }

        val snap = engine.buildRenderSnapshot()
        val px = snap.player?.x ?: 0f
        val py = snap.player?.y ?: 0f
        snap.bots.forEach { bot ->
            val dist = hypot(bot.x - px, bot.y - py)
            if (dist < 1050f) {
                val projected = renderer.projectWorldToScreen(bot.x, bot.z + 48f, bot.y)
                if (projected != null) {
                    val (sx, sy) = projected
                    val barW = 46f
                    val pct = (bot.hp / bot.maxHp).coerceIn(0f, 1f)
                    drawRect(
                        color = ObsidianBg.copy(alpha = 0.85f),
                        topLeft = Offset(sx - barW * 0.5f, sy),
                        size = Size(barW, 6f)
                    )
                    drawRect(
                        color = if (bot.id == engine.lockedEnemyId) BlazeGold else DangerRed,
                        topLeft = Offset(sx - barW * 0.5f, sy),
                        size = Size(barW * pct, 6f)
                    )
                }
            }
        }

        val popupsCopy = synchronized(engine.stateLock) { engine.popups.toList() }
        popupsCopy.forEach { pop ->
            val rise = (0.8f - pop.lifeSec) * 28f
            val projected = renderer.projectWorldToScreen(pop.x, pop.z + rise, pop.y)
            if (projected != null) {
                val (sx, sy) = projected
                val label = if (pop.isHeadshot) "${pop.amount}!" else "${pop.amount}"
                drawText(
                    textMeasurer = textMeasurer,
                    text = label,
                    topLeft = Offset(sx - 12f, sy - 14f),
                    style = TextStyle(
                        color = if (pop.isHeadshot) BlazeGold else Color.White,
                        fontSize = if (pop.isHeadshot) 17.sp else 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
private fun MinimapCanvas(
    engine: BattleEngine,
    tick: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        @Suppress("UNUSED_EXPRESSION")
        tick
        val scale = size.width / engine.mapSize

        engine.obstacles.forEach { o ->
            drawRect(
                color = Color(0xFF47566B),
                topLeft = Offset(o.x * scale, o.y * scale),
                size = Size(o.w * scale, o.h * scale)
            )
        }

        // Draw vehicles on minimap
        engine.vehicles.forEach { v ->
            drawCircle(
                color = BlazeGold,
                radius = 2.4f,
                center = Offset(v.x * scale, v.y * scale)
            )
        }

        drawCircle(
            color = StormBlue,
            radius = engine.zoneRadius * scale,
            center = Offset(engine.zoneCenterX * scale, engine.zoneCenterY * scale),
            style = Stroke(width = 2f)
        )

        val snap = engine.buildRenderSnapshot()
        snap.bots.forEach { bot ->
            drawCircle(
                color = DangerRed,
                radius = 2.5f,
                center = Offset(bot.x * scale, bot.y * scale)
            )
        }

        val p = snap.player
        if (p != null) {
            val px = p.x * scale
            val py = p.y * scale
            val coneLen = 20f
            val leftA = snap.cameraYaw - 0.42f
            val rightA = snap.cameraYaw + 0.42f
            val conePath = Path().apply {
                moveTo(px, py)
                lineTo(px + cos(leftA) * coneLen, py + sin(leftA) * coneLen)
                lineTo(px + cos(rightA) * coneLen, py + sin(rightA) * coneLen)
                close()
            }
            drawPath(conePath, color = DiamondCyan.copy(alpha = 0.28f))
            drawCircle(color = TacticalGreen, radius = 3.8f, center = Offset(px, py))
        }
    }
}

@Composable
private fun VirtualJoystick(
    onMove: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    val maxRadius = 44f

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        knobOffset = Offset.Zero
                        onMove(0f, 0f)
                    },
                    onDragCancel = {
                        knobOffset = Offset.Zero
                        onMove(0f, 0f)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val next = knobOffset + dragAmount
                        val dist = hypot(next.x, next.y)
                        knobOffset = if (dist > maxRadius) {
                            Offset(next.x / dist * maxRadius, next.y / dist * maxRadius)
                        } else {
                            next
                        }
                        onMove(knobOffset.x / maxRadius, knobOffset.y / maxRadius)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width * 0.5f, size.height * 0.5f)
            drawCircle(
                color = ObsidianBg.copy(alpha = 0.55f),
                radius = size.minDimension * 0.44f,
                center = center
            )
            drawCircle(
                color = TextSecondary.copy(alpha = 0.45f),
                radius = size.minDimension * 0.44f,
                center = center,
                style = Stroke(width = 2.5f)
            )
            drawCircle(
                color = BlazeOrange.copy(alpha = 0.9f),
                radius = 20.dp.toPx(),
                center = center + knobOffset
            )
        }
    }
}

@Composable
private fun HudCounterBadge(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(ObsidianBg.copy(alpha = 0.85f))
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Spacer(modifier = Modifier.width(5.dp))
        Text(text = value, style = MaterialTheme.typography.titleSmall, color = color)
    }
}

@Composable
private fun TacticalAbilityButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianBg.copy(alpha = if (enabled) 0.88f else 0.45f))
            .border(1.5.dp, if (enabled) accent else ObsidianBorder, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = label, tint = if (enabled) accent else TextSecondary, modifier = Modifier.size(18.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextPrimary, fontSize = 7.5.sp, maxLines = 1)
    }
}

@Composable
private fun MatchResultOverlay(
    outcome: ArenaMatchOutcome,
    mapTitle: String,
    savedRecord: MatchRecord?,
    onBackToLobby: () -> Unit
) {
    val accent = if (outcome.isBooyah) BlazeGold else MythicCrimson
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg.copy(alpha = 0.88f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = ObsidianCard,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .border(2.dp, accent, RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    text = if (outcome.isBooyah) "WINNER WINNER CHICKEN DINNER!" else "ELIMINATED • RANK #${outcome.placement}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = accent
                )
                Text(
                    text = "$mapTitle • Survival: ${outcome.survivalSeconds}s",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ResultStat("ELIMS", "${outcome.kills}", MythicCrimson)
                    ResultStat("HEADSHOTS", "${outcome.headshots}", BlazeGold)
                    ResultStat("DAMAGE", "${outcome.damageDealt}", DiamondCyan)
                }

                if (savedRecord != null) {
                    Surface(
                        color = ObsidianBg,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Rewards Synced: +${savedRecord.diamondsEarned} Diamonds • +${savedRecord.goldEarned} Gold • ${if (savedRecord.rankPointsDelta >= 0) "+" else ""}${savedRecord.rankPointsDelta} RP",
                            style = MaterialTheme.typography.labelSmall,
                            color = TacticalGreen,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Button(
                    onClick = onBackToLobby,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BlazeOrange,
                        contentColor = ObsidianBg
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("match_return_lobby_button")
                ) {
                    Text("RETURN TO LOBBY", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun ResultStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}
