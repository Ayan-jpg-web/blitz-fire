package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CatalogItem
import com.example.data.model.GameCatalog
import com.example.data.model.ItemCategory
import com.example.data.model.MatchRecord
import com.example.data.model.OwnedItem
import com.example.data.model.UserProfile
import com.example.data.model.rankTitleForPoints
import com.example.ui.components.AppSignatureCard
import com.example.ui.components.AppSignatureInfo
import com.example.ui.components.CatalogItemPreview
import com.example.ui.components.CharacterPreviewStage
import com.example.ui.components.CustomizationPreviewDialog
import com.example.ui.components.DevWatermarkPrefs
import com.example.ui.components.RarityChip
import com.example.ui.components.rarityColor
import com.example.ui.components.readAppSignatureInfo
import com.example.ui.theme.BlazeGold
import com.example.ui.theme.BlazeOrange
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DiamondCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.TacticalGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun VaultScreen(
    profile: UserProfile,
    inventory: Map<String, OwnedItem>,
    onEquipItem: (CatalogItem) -> Unit,
    onNavigateToStore: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<ItemCategory?>(null) }
    var previewItem by remember { mutableStateOf<CatalogItem?>(null) }

    val ownedCatalogItems = GameCatalog.allItems.filter {
        inventory.containsKey(it.id) && (selectedCategory == null || it.category == selectedCategory)
    }
    val equippedIds = setOf(
        profile.equippedCharacterId,
        profile.equippedRifleSkinId,
        profile.equippedSmgSkinId,
        profile.equippedGlooSkinId,
        profile.equippedAttachmentId,
        profile.equippedHatId,
        profile.equippedOutfitId,
        profile.equippedAccessoryId
    )

    val equippedChar = GameCatalog.byId(profile.equippedCharacterId) ?: GameCatalog.byId("char_phantom")!!
    val equippedHat = GameCatalog.byId(profile.equippedHatId)
    val equippedOutfit = GameCatalog.byId(profile.equippedOutfitId)
    val equippedAcc = GameCatalog.byId(profile.equippedAccessoryId)

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // LEFT COLUMN: Live 3D Character & Loadout Summary
        Column(
            modifier = Modifier
                .weight(0.35f)
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(ObsidianCard)
                .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("INVENTORY VAULT", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text("${inventory.size} / ${GameCatalog.allItems.size} Unlocked", style = MaterialTheme.typography.labelSmall, color = DiamondCyan)
                }
                TextButton(onClick = onNavigateToStore) {
                    Text("SHOP", style = MaterialTheme.typography.labelLarge, color = BlazeGold)
                }
            }

            CharacterPreviewStage(
                character = equippedChar,
                hat = equippedHat,
                outfit = equippedOutfit,
                accessory = equippedAcc,
                weapon = GameCatalog.byId(profile.equippedRifleSkinId),
                attachment = GameCatalog.byId(profile.equippedAttachmentId),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(equippedChar.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary, maxLines = 1)
                Text("OUTFIT: ${equippedOutfit?.name ?: "Default"}", style = MaterialTheme.typography.labelSmall, color = BlazeGold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("HAT: ${equippedHat?.name ?: "None"}", style = MaterialTheme.typography.labelSmall, color = DiamondCyan, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("BACK: ${equippedAcc?.name ?: "None"}", style = MaterialTheme.typography.labelSmall, color = TacticalGreen, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        // RIGHT COLUMN: Filter Chips & Owned Items Grid
        Column(
            modifier = Modifier
                .weight(0.65f)
                .fillMaxSize()
        ) {
            val filterCats = listOf(
                ItemCategory.BUNDLE,
                ItemCategory.CHARACTER,
                ItemCategory.OUTFIT,
                ItemCategory.HAT,
                ItemCategory.ACCESSORY,
                ItemCategory.WEAPON,
                ItemCategory.ATTACHMENT,
                ItemCategory.GLOO,
                ItemCategory.CONSUMABLE
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("All") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BlazeOrange,
                            selectedLabelColor = ObsidianBg
                        )
                    )
                }
                items(filterCats.size) { idx ->
                    val cat = filterCats[idx]
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BlazeOrange,
                            selectedLabelColor = ObsidianBg
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 152.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(ownedCatalogItems, key = { it.id }) { item ->
                val isEquipped = item.id in equippedIds
                val ownedCount = inventory[item.id]?.quantity ?: 1
                val rColor = rarityColor(item.rarity)

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(ObsidianCard)
                        .border(
                            width = if (isEquipped) 2.dp else 1.dp,
                            color = if (isEquipped) TacticalGreen else rColor.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(10.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        CatalogItemPreview(
                            item = item,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(104.dp)
                                .clickable { previewItem = item }
                        )
                        Surface(
                            color = ObsidianBg.copy(alpha = 0.78f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .clickable { previewItem = item }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "Preview",
                                tint = DiamondCyan,
                                modifier = Modifier.padding(4.dp).size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RarityChip(rarity = item.rarity)
                        if (ownedCount > 1) {
                            Text(text = "x$ownedCount", style = MaterialTheme.typography.labelSmall, color = BlazeGold)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.perkDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.height(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    when {
                        item.category == ItemCategory.CONSUMABLE -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DiamondCyan.copy(alpha = 0.14f)),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("AUTO-ACTIVE (x$ownedCount)", style = MaterialTheme.typography.labelSmall, color = DiamondCyan)
                            }
                        }
                        isEquipped -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(TacticalGreen.copy(alpha = 0.16f)),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = TacticalGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("EQUIPPED", style = MaterialTheme.typography.labelLarge, color = TacticalGreen)
                            }
                        }
                        else -> {
                            Button(
                                onClick = { onEquipItem(item) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BlazeOrange,
                                    contentColor = ObsidianBg
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("vault_equip_${item.id}")
                            ) {
                                Text("EQUIP", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
    }
    }

    val activePreview = previewItem
    if (activePreview != null) {
        CustomizationPreviewDialog(
            previewItem = activePreview,
            profile = profile,
            isOwned = true,
            isEquipped = activePreview.id in equippedIds,
            onPurchase = {},
            onEquip = { onEquipItem(activePreview) },
            onDismiss = { previewItem = null }
        )
    }
}

@Composable
fun ProfileScreen(
    profile: UserProfile,
    matchHistory: List<MatchRecord>,
    isGuestSession: Boolean = false,
    signatureInfo: AppSignatureInfo? = null,
    devWatermarkLabel: String = DevWatermarkPrefs.DEFAULT_LABEL,
    watermarkEnabled: Boolean = true,
    onToggleWatermark: (Boolean) -> Unit = {},
    onUpdateDevWatermarkLabel: (String) -> Unit = {},
    onLinkGoogleAccount: () -> Unit = {},
    onBack: () -> Unit,
    onUpdateCallsign: (String) -> Unit,
    onOpenHudSettings: () -> Unit = {},
    onRefillCurrency: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val resolvedSignature = remember(signatureInfo, context) {
        signatureInfo ?: readAppSignatureInfo(context)
    }

    var showCallsignDialog by remember { mutableStateOf(false) }
    var draftCallsign by remember(profile.callsign) { mutableStateOf(profile.callsign) }

    val winRate = if (profile.matchesPlayed > 0) (profile.booyahs * 100) / profile.matchesPlayed else 0
    val headshotPct = if (profile.totalKills > 0) (profile.totalHeadshots * 100) / profile.totalKills else 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Lobby", tint = TextPrimary)
                }
                Text(text = "OPERATIVE DOSSIER", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            }
            OutlinedButton(onClick = onSignOut, shape = RoundedCornerShape(10.dp), modifier = Modifier.testTag("sign_out_button")) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = DangerRed, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isGuestSession) "EXIT GUEST" else "SIGN OUT", style = MaterialTheme.typography.labelSmall, color = DangerRed)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ObsidianCard)
                        .border(1.dp, BlazeGold.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = profile.callsign, style = MaterialTheme.typography.displaySmall, color = TextPrimary)
                                if (isGuestSession) {
                                    Surface(
                                        color = DiamondCyan.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.border(1.dp, DiamondCyan.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                    ) {
                                        Text(
                                            text = "GUEST VAULT",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = DiamondCyan,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(text = "${rankTitleForPoints(profile.rankPoints)} • ${profile.rankPoints} Rank Points", style = MaterialTheme.typography.titleMedium, color = BlazeGold)
                        }
                        IconButton(onClick = { showCallsignDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Callsign", tint = DiamondCyan)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        ProfileMetric("MATCHES", "${profile.matchesPlayed}")
                        ProfileMetric("BOOYAH %", "$winRate%")
                        ProfileMetric("TOTAL KILLS", "${profile.totalKills}")
                        ProfileMetric("HEADSHOT %", "$headshotPct%")
                    }
                }
            }

            if (isGuestSession) {
                item {
                    Button(
                        onClick = onLinkGoogleAccount,
                        colors = ButtonDefaults.buttonColors(containerColor = BlazeOrange, contentColor = ObsidianBg),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("guest_link_google_button")
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SIGN IN WITH GOOGLE FOR CLOUD SYNC", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            item {
                Button(
                    onClick = onOpenHudSettings,
                    colors = ButtonDefaults.buttonColors(containerColor = BlazeOrange, contentColor = ObsidianBg),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("profile_hud_settings_button")
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CUSTOMIZE HUD LAYOUT & SENSITIVITY", style = MaterialTheme.typography.labelLarge)
                }
            }

            item {
                Button(
                    onClick = onRefillCurrency,
                    colors = ButtonDefaults.buttonColors(containerColor = DiamondCyan, contentColor = ObsidianBg),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Default.AllInclusive, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("BOOST +5,000,000 DIAMONDS & GOLD", style = MaterialTheme.typography.labelLarge)
                }
            }

            item {
                AppSignatureCard(
                    signatureInfo = resolvedSignature,
                    devLabel = devWatermarkLabel,
                    watermarkEnabled = watermarkEnabled,
                    onToggleWatermark = onToggleWatermark,
                    onUpdateDevLabel = onUpdateDevWatermarkLabel
                )
            }

            item {
                Text("RECENT COMBAT LOGS", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
            }

            if (matchHistory.isEmpty()) {
                item {
                    Text(
                        text = "No matches recorded yet. Drop into Battle Royale to log your first Booyah!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(matchHistory) { record ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ObsidianCard)
                            .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = if (record.booyah) BlazeGold else TextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (record.booyah) "BOOYAH! (#1)" else "Rank #${record.placement}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (record.booyah) BlazeGold else TextPrimary
                                )
                                Text(
                                    text = "${record.mode.replace("_", " ").uppercase()} • ${record.survivalSeconds}s",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "${record.kills} Kills (${record.damageDealt} DMG)", style = MaterialTheme.typography.labelSmall, color = DiamondCyan)
                            Text(
                                text = "${if (record.rankPointsDelta >= 0) "+" else ""}${record.rankPointsDelta} RP",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (record.rankPointsDelta >= 0) TacticalGreen else DangerRed
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCallsignDialog) {
        AlertDialog(
            onDismissRequest = { showCallsignDialog = false },
            title = { Text("Update Survivor Callsign") },
            text = {
                OutlinedTextField(
                    value = draftCallsign,
                    onValueChange = { if (it.length <= 24) draftCallsign = it },
                    singleLine = true,
                    label = { Text("Callsign (2-24 chars)") }
                )
            },
            confirmButton = {
                Button(onClick = { onUpdateCallsign(draftCallsign); showCallsignDialog = false }) { Text("SAVE") }
            },
            dismissButton = {
                TextButton(onClick = { showCallsignDialog = false }) { Text("CANCEL") }
            }
        )
    }
}

@Composable
private fun ProfileMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}
