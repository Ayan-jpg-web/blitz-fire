package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ai.TacticalVoiceCommsManager
import com.example.data.model.BattleMapId
import com.example.data.model.GameCatalog
import com.example.data.model.MatchRecord
import com.example.data.model.UserProfile
import com.example.ui.components.CharacterPreviewStage
import com.example.ui.components.RarityChip
import com.example.ui.components.SquadVoicePill
import com.example.ui.components.rarityColor
import com.example.ui.theme.BlazeGold
import com.example.ui.theme.BlazeOrange
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

@Composable
fun LobbyScreen(
    profile: UserProfile,
    selectedMode: String,
    selectedMap: BattleMapId,
    matchHistory: List<MatchRecord>,
    commsManager: TacticalVoiceCommsManager,
    onSelectMode: (String) -> Unit,
    onSelectMap: (BattleMapId) -> Unit,
    onStartMatch: () -> Unit,
    onOpenVoiceComms: () -> Unit,
    onNavigateToRoyale: () -> Unit,
    onNavigateToStore: () -> Unit,
    onNavigateToVault: () -> Unit,
    onOpenHudSettings: () -> Unit = {},
    onRefillCurrency: () -> Unit,
    modifier: Modifier = Modifier
) {
    val equippedChar = GameCatalog.byId(profile.equippedCharacterId) ?: GameCatalog.byId("char_phantom")!!
    val equippedRifle = GameCatalog.byId(profile.equippedRifleSkinId) ?: GameCatalog.byId("wpn_akm_default")!!
    val equippedSmg = GameCatalog.byId(profile.equippedSmgSkinId) ?: GameCatalog.byId("wpn_mp40_default")!!
    val equippedGloo = GameCatalog.byId(profile.equippedGlooSkinId) ?: GameCatalog.byId("gloo_default")!!
    val equippedHat = GameCatalog.byId(profile.equippedHatId)
    val equippedOutfit = GameCatalog.byId(profile.equippedOutfitId)
    val equippedAcc = GameCatalog.byId(profile.equippedAccessoryId)

    var showMapModePicker by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // LEFT COMMAND COLUMN: BGMI Map/Mode Selector + START Button + Squad Voice + Quick Access
        Column(
            modifier = Modifier
                .weight(0.38f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // BGMI Map & Mode Selector Card with Aerial Battleground Thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ObsidianCard)
                    .border(1.5.dp, Color(selectedMap.accentHex).copy(alpha = 0.78f), RoundedCornerShape(14.dp))
                    .clickable { showMapModePicker = true }
                    .minimumInteractiveComponentSize()
                    .testTag("map_mode_selector_card")
            ) {
                Image(
                    painter = painterResource(id = mapThumbnailDrawable(selectedMap)),
                    contentDescription = selectedMap.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    ObsidianBg.copy(alpha = 0.92f),
                                    ObsidianBg.copy(alpha = 0.68f),
                                    ObsidianBg.copy(alpha = 0.45f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = "Select Map",
                                tint = Color(selectedMap.accentHex),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = selectedMap.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            color = ObsidianBg.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.border(0.75.dp, BlazeGold.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = BlazeGold, modifier = Modifier.size(11.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("CHANGE", style = MaterialTheme.typography.labelSmall, color = BlazeGold)
                            }
                        }
                    }
                    Text(
                        text = "${if (selectedMode == "battle_royale") "CLASSIC BR 3D • 16P" else "ARENA BLITZ 3D • 10P"} • ${selectedMap.sizeBadge}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(selectedMap.accentHex),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = selectedMap.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary.copy(alpha = 0.86f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Primary BGMI Mustard-Gold START CTA
            Button(
                onClick = onStartMatch,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BlazeOrange,
                    contentColor = ObsidianBg
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("start_match_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "START MATCH",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "DEPLOY ${selectedMap.name} • TPP 3D",
                        style = MaterialTheme.typography.labelSmall,
                        color = ObsidianBg.copy(alpha = 0.85f)
                    )
                }
            }

            // Real-Time Squad Tactical Voice Radio
            SquadVoicePill(
                commsManager = commsManager,
                onClick = onOpenVoiceComms,
                modifier = Modifier.fillMaxWidth()
            )

            // Quick Armory & Royale Tiles with Bespoke Artwork Thumbnails
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LobbyCompactTile(
                    title = "ROYALE",
                    subtitle = "10+1 Draws",
                    thumbResId = R.drawable.img_royale_banner,
                    icon = Icons.Default.AutoAwesome,
                    accent = MythicCrimson,
                    onClick = onNavigateToRoyale,
                    testTag = "lobby_royale_shortcut",
                    modifier = Modifier.weight(1f)
                )
                LobbyCompactTile(
                    title = "ARMORY",
                    subtitle = "Skins & Gear",
                    thumbResId = R.drawable.img_thumb_weapon,
                    icon = Icons.Default.Storefront,
                    accent = DiamondCyan,
                    onClick = onNavigateToStore,
                    testTag = "lobby_store_shortcut",
                    modifier = Modifier.weight(1f)
                )
            }

            // Unlimited Vault Boost + Custom HUD & Sensitivity Shortcuts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1.1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(DiamondCyan.copy(alpha = 0.14f), BlazeGold.copy(alpha = 0.14f))
                            )
                        )
                        .border(1.dp, BlazeGold.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                        .clickable(onClick = onRefillCurrency)
                        .minimumInteractiveComponentSize()
                        .padding(horizontal = 9.dp, vertical = 7.dp)
                        .testTag("unlimited_vault_banner"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.AllInclusive, contentDescription = "Unlimited Vault", tint = DiamondCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "VAULT",
                            style = MaterialTheme.typography.labelSmall,
                            color = BlazeGold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text("+5M", style = MaterialTheme.typography.labelLarge, color = DiamondCyan)
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ObsidianCard)
                        .border(1.dp, BlazeOrange.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenHudSettings)
                        .minimumInteractiveComponentSize()
                        .padding(horizontal = 9.dp, vertical = 7.dp)
                        .testTag("lobby_hud_settings_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Tune, contentDescription = "Custom HUD & Sensitivity", tint = BlazeOrange, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "HUD & SENS",
                        style = MaterialTheme.typography.labelSmall,
                        color = BlazeGold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // RIGHT/CENTER STAGE: Full-Height 3D PUBG/BGMI Operative Showcase + Telemetry Bar
        Box(
            modifier = Modifier
                .weight(0.62f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(18.dp))
                .border(1.5.dp, rarityColor(equippedChar.rarity).copy(alpha = 0.65f), RoundedCornerShape(18.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_lobby),
                contentDescription = "BGMI Dropzone Backdrop",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                ObsidianBg.copy(alpha = 0.90f),
                                ObsidianBg.copy(alpha = 0.42f),
                                ObsidianBg.copy(alpha = 0.72f)
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Operative Dossier & Loadout Summary
                Column(
                    modifier = Modifier
                        .weight(0.46f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        RarityChip(rarity = equippedChar.rarity)
                        Text(
                            text = equippedChar.name,
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = equippedChar.perkDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = DiamondCyan,
                            maxLines = 2
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Surface(
                            color = ObsidianCard.copy(alpha = 0.90f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onNavigateToVault)
                                .border(1.dp, ObsidianBorder, RoundedCornerShape(10.dp))
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                                Text(
                                    text = "AR: ${equippedRifle.name} (+${equippedRifle.damageBonus} DMG)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BlazeGold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "SMG: ${equippedSmg.name}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "GLOO: ${equippedGloo.name} • TAP TO EDIT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DiamondCyan,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Bottom Telemetry Ribbon inside Lobby Stage
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(ObsidianCard.copy(alpha = 0.88f))
                                .border(1.dp, ObsidianBorder, RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            StatCounter(label = "WINS", value = "${profile.booyahs}", color = BlazeGold)
                            StatCounter(label = "MATCH", value = "${profile.matchesPlayed}", color = TextPrimary)
                            StatCounter(label = "KILLS", value = "${profile.totalKills}", color = MythicCrimson)
                            StatCounter(label = "HS", value = "${profile.totalHeadshots}", color = TacticalGreen)
                        }

                        if (matchHistory.isNotEmpty()) {
                            val latest = matchHistory.first()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ObsidianElevated.copy(alpha = 0.90f))
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = if (latest.booyah) BlazeGold else TextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (latest.booyah) "LAST: WINNER #1" else "LAST: #${latest.placement}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (latest.booyah) BlazeGold else TextPrimary
                                    )
                                }
                                Text(
                                    text = "${latest.kills}K • ${if (latest.rankPointsDelta >= 0) "+" else ""}${latest.rankPointsDelta} RP",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DiamondCyan
                                )
                            }
                        }
                    }
                }

                // 360 Interactive 3D PUBG/BGMI Soldier Viewport
                Box(
                    modifier = Modifier
                        .weight(0.54f)
                        .fillMaxHeight()
                        .clickable(onClick = onNavigateToVault)
                ) {
                    CharacterPreviewStage(
                        character = equippedChar,
                        hat = equippedHat,
                        outfit = equippedOutfit,
                        accessory = equippedAcc,
                        weapon = equippedRifle,
                        attachment = GameCatalog.byId(profile.equippedAttachmentId),
                        modifier = Modifier.fillMaxSize()
                    )
                    Surface(
                        color = ObsidianBg.copy(alpha = 0.82f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                            .border(1.dp, BlazeGold.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = "TAP TO CUSTOMIZE LOADOUT",
                            style = MaterialTheme.typography.labelSmall,
                            color = BlazeGold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }

    if (showMapModePicker) {
        MapAndModePickerDialog(
            selectedMap = selectedMap,
            selectedMode = selectedMode,
            onSelectMap = onSelectMap,
            onSelectMode = onSelectMode,
            onDismiss = { showMapModePicker = false }
        )
    }
}

@Composable
private fun MapAndModePickerDialog(
    selectedMap: BattleMapId,
    selectedMode: String,
    onSelectMap: (BattleMapId) -> Unit,
    onSelectMode: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = ObsidianSurface,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxWidth(0.92f)
                .heightIn(max = 360.dp)
                .border(1.5.dp, BlazeGold, RoundedCornerShape(18.dp))
                .testTag("map_mode_picker_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(14.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SELECT BATTLEGROUND & MODE",
                        style = MaterialTheme.typography.titleLarge,
                        color = BlazeGold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close Map Selector", tint = TextSecondary)
                    }
                }

                // 3 Maps Horizontal Row with Aerial Battleground Thumbnails
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BattleMapId.entries.forEach { map ->
                        val active = map == selectedMap
                        val accent = Color(map.accentHex)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (active) accent.copy(alpha = 0.16f) else ObsidianCard)
                                .border(if (active) 2.dp else 1.dp, if (active) accent else ObsidianBorder, RoundedCornerShape(12.dp))
                                .clickable { onSelectMap(map) }
                                .minimumInteractiveComponentSize()
                                .testTag("map_card_${map.key}")
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(82.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = mapThumbnailDrawable(map)),
                                    contentDescription = map.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color.Transparent, ObsidianBg.copy(alpha = 0.80f))
                                            )
                                        )
                                )
                                Surface(
                                    color = ObsidianBg.copy(alpha = 0.80f),
                                    shape = RoundedCornerShape(5.dp),
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(6.dp)
                                ) {
                                    Text(
                                        text = map.sizeBadge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = accent,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                                if (active) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = accent,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(18.dp)
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(map.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, maxLines = 1)
                                Text(map.subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }

                // Combat Mode Switch + Confirm Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ModeSelectChip(
                        title = "CLASSIC BR 3D",
                        details = "1KM × 1KM • 16P",
                        selected = selectedMode == "battle_royale",
                        accent = BlazeOrange,
                        onClick = { onSelectMode("battle_royale") },
                        testTag = "mode_battle_royale",
                        modifier = Modifier.weight(1f)
                    )
                    ModeSelectChip(
                        title = "CLASH BLITZ 3D",
                        details = "500M × 500M • 10P",
                        selected = selectedMode == "clash_blitz",
                        accent = DiamondCyan,
                        onClick = { onSelectMode("clash_blitz") },
                        testTag = "mode_clash_blitz",
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = BlazeOrange, contentColor = ObsidianBg),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("confirm_map_mode_button")
                    ) {
                        Text("CONFIRM", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeSelectChip(
    title: String,
    details: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) accent.copy(alpha = 0.18f) else ObsidianCard)
            .border(if (selected) 1.8.dp else 1.dp, if (selected) accent else ObsidianBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.labelLarge, color = if (selected) TextPrimary else TextSecondary)
            Text(details, style = MaterialTheme.typography.labelSmall, color = if (selected) accent else TextSecondary)
        }
        if (selected) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun LobbyCompactTile(
    title: String,
    subtitle: String,
    thumbResId: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianCard)
            .border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .testTag(testTag)
    ) {
        Image(
            painter = painterResource(id = thumbResId),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(ObsidianBg.copy(alpha = 0.90f), ObsidianBg.copy(alpha = 0.62f))
                    )
                )
        )
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(17.dp))
            }
            Spacer(modifier = Modifier.width(7.dp))
            Column {
                Text(title, style = MaterialTheme.typography.labelLarge, color = TextPrimary, maxLines = 1)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = accent, maxLines = 1)
            }
        }
    }
}

private fun mapThumbnailDrawable(map: BattleMapId): Int = when (map) {
    BattleMapId.ERANGEL -> R.drawable.img_map_erangel
    BattleMapId.MIRAMAR -> R.drawable.img_map_miramar
    BattleMapId.SANHOK -> R.drawable.img_map_sanhok
}

@Composable
private fun StatCounter(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleMedium, color = color)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}
