package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.CatalogItem
import com.example.data.model.CurrencyType
import com.example.data.model.GameCatalog
import com.example.data.model.ItemCategory
import com.example.data.model.ItemRarity
import com.example.data.model.SpinOutcome
import com.example.data.model.UserProfile
import com.example.data.model.WeaponSlot
import com.example.data.model.rankTitleForPoints
import com.example.data.repository.LocalGuestRepository
import com.example.ui.theme.BlazeGold
import com.example.ui.theme.BlazeOrange
import com.example.ui.theme.DiamondCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.RarityEpic
import com.example.ui.theme.RarityLegendary
import com.example.ui.theme.RarityMythic
import com.example.ui.theme.RarityRare
import com.example.ui.theme.TacticalGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

fun rarityColor(rarity: ItemRarity): Color = when (rarity) {
    ItemRarity.MYTHIC -> RarityMythic
    ItemRarity.LEGENDARY -> RarityLegendary
    ItemRarity.EPIC -> RarityEpic
    ItemRarity.RARE -> RarityRare
}

fun formatNumber(value: Long): String {
    return NumberFormat.getNumberInstance(Locale.US).format(value)
}

@Composable
fun TopCurrencyBar(
    profile: UserProfile,
    onRefillClicked: () -> Unit,
    onProfileClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(ObsidianCard)
                .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
                .clickable(onClick = onProfileClicked)
                .minimumInteractiveComponentSize()
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag("profile_badge_button"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(BlazeOrange.copy(alpha = 0.2f))
                    .border(1.5.dp, BlazeGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Player Profile",
                    tint = BlazeGold,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = profile.callsign,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (profile.userId == LocalGuestRepository.GUEST_UID) {
                        Surface(
                            color = DiamondCyan.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.border(0.75.dp, DiamondCyan.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        ) {
                            Text(
                                text = "GUEST",
                                style = MaterialTheme.typography.labelSmall,
                                color = DiamondCyan,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "${rankTitleForPoints(profile.rankPoints)} • ${profile.rankPoints} RP",
                    style = MaterialTheme.typography.labelSmall,
                    color = BlazeGold
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CurrencyPill(
                icon = Icons.Default.Diamond,
                tint = DiamondCyan,
                amount = formatNumber(profile.diamonds),
                onClick = onRefillClicked,
                testTag = "diamonds_currency_pill"
            )
            CurrencyPill(
                icon = Icons.Default.MonetizationOn,
                tint = BlazeGold,
                amount = formatNumber(profile.gold),
                onClick = onRefillClicked,
                testTag = "gold_currency_pill"
            )
        }
    }
}

@Composable
fun CurrencyPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    amount: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(ObsidianCard)
            .border(1.dp, tint.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .padding(horizontal = 8.dp, vertical = 5.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = amount, style = MaterialTheme.typography.labelSmall, color = TextPrimary)
        Spacer(modifier = Modifier.width(3.dp))
        Icon(imageVector = Icons.Default.AllInclusive, contentDescription = "Unlimited", tint = tint, modifier = Modifier.size(13.dp))
        Spacer(modifier = Modifier.width(2.dp))
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Boost Unlimited Currency", tint = tint, modifier = Modifier.size(12.dp))
        }
    }
}

@Composable
fun RarityChip(rarity: ItemRarity) {
    val color = rarityColor(rarity)
    Surface(
        color = color.copy(alpha = 0.18f),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.border(1.dp, color.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
    ) {
        Text(
            text = rarity.label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun CharacterPreviewStage(
    character: CatalogItem,
    hat: CatalogItem?,
    outfit: CatalogItem?,
    accessory: CatalogItem?,
    weapon: CatalogItem? = null,
    attachment: CatalogItem? = null,
    focusMode: PreviewFocusMode = PreviewFocusMode.OPERATIVE,
    isDialogOverlay: Boolean = false,
    modifier: Modifier = Modifier
) {
    Interactive3DCharacterStage(
        character = character,
        hat = hat,
        outfit = outfit,
        accessory = accessory,
        weapon = weapon,
        attachment = attachment,
        focusMode = focusMode,
        isDialogOverlay = isDialogOverlay,
        modifier = modifier
    )
}

@Composable
fun CatalogItemPreview(
    item: CatalogItem,
    modifier: Modifier = Modifier
) {
    val rColor = rarityColor(item.rarity)
    val accent = Color(item.accentHex)

    val drawableId = when (item.category) {
        ItemCategory.CHARACTER, ItemCategory.BUNDLE -> when {
            item.imageKey == "valkyrie" -> R.drawable.img_character_valkyrie
            item.id == "char_inferno_ronin" -> R.drawable.img_royale_banner
            else -> R.drawable.img_character_phantom
        }
        ItemCategory.WEAPON -> if (item.weaponSlot == WeaponSlot.SMG) {
            R.drawable.img_thumb_smg
        } else {
            R.drawable.img_thumb_weapon
        }
        ItemCategory.ATTACHMENT -> R.drawable.img_thumb_smg
        ItemCategory.HAT, ItemCategory.OUTFIT, ItemCategory.ACCESSORY -> R.drawable.img_thumb_gear
        ItemCategory.GLOO, ItemCategory.CONSUMABLE -> R.drawable.img_thumb_supply
    }

    val categoryBadge = when (item.category) {
        ItemCategory.BUNDLE -> "BUNDLE • ${item.bundleItemIds.size}X"
        ItemCategory.CHARACTER -> "OPERATIVE"
        ItemCategory.WEAPON -> if (item.weaponSlot == WeaponSlot.SMG) "SMG" else "RIFLE"
        ItemCategory.ATTACHMENT -> "MOD"
        ItemCategory.HAT -> "HELM"
        ItemCategory.OUTFIT -> "SUIT"
        ItemCategory.ACCESSORY -> "BACK"
        ItemCategory.GLOO -> "SHIELD"
        ItemCategory.CONSUMABLE -> "SUPPLY"
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianBg)
            .border(1.dp, rColor.copy(alpha = 0.55f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = drawableId),
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.16f),
                            Color.Transparent,
                            ObsidianBg.copy(alpha = 0.72f)
                        )
                    )
                )
        )
        Surface(
            color = ObsidianBg.copy(alpha = 0.80f),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp)
                .border(0.75.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
        ) {
            Text(
                text = categoryBadge,
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun StatMiniBar(label: String, progress: Float, color: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Spacer(modifier = Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            color = color,
            trackColor = ObsidianElevated,
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(4.dp))
        )
    }
}

@Composable
fun CustomizationPreviewDialog(
    previewItem: CatalogItem,
    profile: UserProfile,
    isOwned: Boolean,
    isEquipped: Boolean,
    onPurchase: () -> Unit,
    onEquip: () -> Unit,
    onDismiss: () -> Unit
) {
    val bundleParts = remember(previewItem.id) {
        if (previewItem.category == ItemCategory.BUNDLE) GameCatalog.bundleContents(previewItem) else emptyList()
    }
    val previewChar = bundleParts.firstOrNull { it.category == ItemCategory.CHARACTER }
        ?: if (previewItem.category == ItemCategory.CHARACTER) previewItem
        else GameCatalog.byId(profile.equippedCharacterId) ?: GameCatalog.byId("char_phantom")!!
    val previewHat = bundleParts.firstOrNull { it.category == ItemCategory.HAT }
        ?: if (previewItem.category == ItemCategory.HAT) previewItem
        else GameCatalog.byId(profile.equippedHatId)
    val previewOutfit = bundleParts.firstOrNull { it.category == ItemCategory.OUTFIT }
        ?: if (previewItem.category == ItemCategory.OUTFIT) previewItem
        else GameCatalog.byId(profile.equippedOutfitId)
    val previewAcc = bundleParts.firstOrNull { it.category == ItemCategory.ACCESSORY }
        ?: if (previewItem.category == ItemCategory.ACCESSORY) previewItem
        else GameCatalog.byId(profile.equippedAccessoryId)

    val previewWeapon = bundleParts.firstOrNull { it.category == ItemCategory.WEAPON }
        ?: if (previewItem.category == ItemCategory.WEAPON) previewItem
        else GameCatalog.byId(profile.equippedRifleSkinId)

    val previewAttachment = bundleParts.firstOrNull { it.category == ItemCategory.ATTACHMENT }
        ?: if (previewItem.category == ItemCategory.ATTACHMENT) previewItem
        else GameCatalog.byId(profile.equippedAttachmentId)

    val defaultFocusMode = when (previewItem.category) {
        ItemCategory.WEAPON, ItemCategory.ATTACHMENT -> PreviewFocusMode.WEAPON
        ItemCategory.ACCESSORY -> PreviewFocusMode.ACCESSORY
        ItemCategory.HAT -> PreviewFocusMode.HAT
        else -> PreviewFocusMode.OPERATIVE
    }
    var focusMode by remember(previewItem.id) { mutableStateOf(defaultFocusMode) }
    val supportsCloseupToggle = defaultFocusMode != PreviewFocusMode.OPERATIVE

    val rColor = rarityColor(previewItem.rarity)
    val canAfford = when (previewItem.currency) {
        CurrencyType.DIAMONDS -> profile.diamonds >= previewItem.price
        CurrencyType.GOLD -> profile.gold >= previewItem.price
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = ObsidianSurface,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth(0.90f)
                .heightIn(max = 365.dp)
                .border(2.dp, rColor, RoundedCornerShape(18.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (previewItem.category in setOf(
                        ItemCategory.CHARACTER,
                        ItemCategory.HAT,
                        ItemCategory.OUTFIT,
                        ItemCategory.ACCESSORY,
                        ItemCategory.WEAPON,
                        ItemCategory.ATTACHMENT,
                        ItemCategory.BUNDLE
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .width(205.dp)
                            .height(280.dp)
                    ) {
                        CharacterPreviewStage(
                            character = previewChar,
                            hat = previewHat,
                            outfit = previewOutfit,
                            accessory = previewAcc,
                            weapon = previewWeapon,
                            attachment = previewAttachment,
                            focusMode = focusMode,
                            isDialogOverlay = true,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (supportsCloseupToggle) {
                            Surface(
                                color = ObsidianBg.copy(alpha = 0.86f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(8.dp)
                                    .border(1.dp, DiamondCyan.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                                    .clickable {
                                        focusMode = if (focusMode == PreviewFocusMode.OPERATIVE) {
                                            defaultFocusMode
                                        } else {
                                            PreviewFocusMode.OPERATIVE
                                        }
                                    }
                            ) {
                                Text(
                                    text = if (focusMode == PreviewFocusMode.OPERATIVE) "ZOOM ITEM 3D" else "SHOW ON OPERATIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DiamondCyan,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                } else {
                    CatalogItemPreview(
                        item = previewItem,
                        modifier = Modifier
                            .width(190.dp)
                            .height(210.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (previewItem.category == ItemCategory.BUNDLE) "LIVE 3D BUNDLE SHOWCASE" else "LIVE 3D CUSTOMIZATION PREVIEW",
                        style = MaterialTheme.typography.labelSmall,
                        color = DiamondCyan
                    )
                    Text(
                        text = previewItem.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary
                    )
                    RarityChip(rarity = previewItem.rarity)
                    Text(
                        text = previewItem.perkDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    if (bundleParts.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(ObsidianCard)
                                .border(1.dp, ObsidianBorder, RoundedCornerShape(10.dp))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "INCLUDED IN BUNDLE (${bundleParts.size})",
                                style = MaterialTheme.typography.labelSmall,
                                color = BlazeGold
                            )
                            bundleParts.forEach { part ->
                                Text(
                                    text = "• ${part.name} (${part.category.label})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("CLOSE", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                        }

                        when {
                            isEquipped -> Unit
                            isOwned && previewItem.category != ItemCategory.CONSUMABLE -> {
                                Button(
                                    onClick = {
                                        onEquip()
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = TacticalGreen,
                                        contentColor = ObsidianBg
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Text(
                                        text = if (previewItem.category == ItemCategory.BUNDLE) "EQUIP ALL" else "EQUIP",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                            else -> {
                                val cColor = if (previewItem.currency == CurrencyType.DIAMONDS) DiamondCyan else BlazeGold
                                Button(
                                    onClick = {
                                        onPurchase()
                                        onDismiss()
                                    },
                                    enabled = canAfford,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = cColor,
                                        contentColor = ObsidianBg
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Text(
                                        text = if (canAfford) "BUY ${formatNumber(previewItem.price)}" else "LOW BALANCE",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpinRewardDialog(
    outcome: SpinOutcome,
    onEquipItem: (CatalogItem) -> Unit,
    onDismiss: () -> Unit
) {
    val bestItem = outcome.items.minByOrNull {
        when (it.rarity) {
            ItemRarity.MYTHIC -> 0
            ItemRarity.LEGENDARY -> 1
            ItemRarity.EPIC -> 2
            ItemRarity.RARE -> 3
        }
    } ?: outcome.items.first()

    val infinite = rememberInfiniteTransition(label = "burst")
    val rayAngle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Restart),
        label = "rayAngle"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = ObsidianSurface,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .widthIn(max = 620.dp)
                .fillMaxWidth(0.90f)
                .heightIn(max = 360.dp)
                .border(2.dp, rarityColor(bestItem.rarity), RoundedCornerShape(20.dp))
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Canvas(
                    modifier = Modifier
                        .matchParentSize()
                        .rotate(rayAngle)
                        .alpha(0.16f)
                ) {
                    val center = Offset(size.width * 0.5f, size.height * 0.35f)
                    val rayColor = rarityColor(bestItem.rarity)
                    for (i in 0 until 12) {
                        val a = Math.toRadians(i * 30.0)
                        drawLine(
                            color = rayColor,
                            start = center,
                            end = Offset(
                                center.x + (cos(a) * size.maxDimension).toFloat(),
                                center.y + (sin(a) * size.maxDimension).toFloat()
                            ),
                            strokeWidth = 18f
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (outcome.hitPityJackpot) Icons.Default.WorkspacePremium else Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = BlazeGold,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (outcome.hitPityJackpot) "JACKPOT GRAND PRIZE!" else "${outcome.banner.title} REWARDS",
                            style = MaterialTheme.typography.titleLarge,
                            color = BlazeGold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(if (outcome.items.size == 1) 1 else 4),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (outcome.items.size == 1) 138.dp else 184.dp)
                    ) {
                        itemsIndexed(outcome.items) { index, reward ->
                            val scaleAnim = remember { Animatable(0.45f) }
                            val alphaAnim = remember { Animatable(0f) }
                            LaunchedEffect(outcome) {
                                delay(index * 55L)
                                alphaAnim.animateTo(1f, tween(180))
                                scaleAnim.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                            }

                            val isNew = reward.id in outcome.newlyUnlockedIds
                            val rColor = rarityColor(reward.rarity)
                            Column(
                                modifier = Modifier
                                    .scale(scaleAnim.value)
                                    .alpha(alphaAnim.value)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ObsidianCard)
                                    .border(1.5.dp, rColor, RoundedCornerShape(10.dp))
                                    .padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    CatalogItemPreview(
                                        item = reward,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(if (outcome.items.size == 1) 92.dp else 54.dp)
                                    )
                                    if (isNew) {
                                        Surface(
                                            color = TacticalGreen,
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(3.dp)
                                        ) {
                                            Text(
                                                text = "NEW",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ObsidianBg,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = reward.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (outcome.bonusTokens > 0) {
                            "Duplicates -> +${outcome.bonusTokens} Royale Tokens • Luck: ${outcome.newLuckValue}/100"
                        } else {
                            "Vault Unlocks Saved! • Luck Meter: ${outcome.newLuckValue}/100"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (outcome.bonusTokens > 0) DiamondCyan else TacticalGreen
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("royale_collect_button")
                        ) {
                            Text("COLLECT", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                        }
                        Button(
                            onClick = {
                                onEquipItem(bestItem)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BlazeOrange,
                                contentColor = ObsidianBg
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("royale_equip_best_button")
                        ) {
                            Text("EQUIP BEST", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}
