package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CurrencyType
import com.example.data.model.GameCatalog
import com.example.data.model.ItemRarity
import com.example.data.model.OwnedItem
import com.example.data.model.RoyaleBannerId
import com.example.data.model.RoyaleSpinRecord
import com.example.data.model.UserProfile
import com.example.ui.components.CatalogItemPreview
import com.example.ui.components.RarityChip
import com.example.ui.components.formatNumber
import com.example.ui.components.rarityColor
import com.example.ui.theme.BlazeGold
import com.example.ui.theme.BlazeOrange
import com.example.ui.theme.DiamondCyan
import com.example.ui.theme.MythicCrimson
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.TacticalGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun LuckRoyaleScreen(
    profile: UserProfile,
    inventory: Map<String, OwnedItem>,
    selectedBanner: RoyaleBannerId,
    isSpinning: Boolean,
    royaleHistory: List<RoyaleSpinRecord>,
    onSelectBanner: (RoyaleBannerId) -> Unit,
    onSpinSingle: () -> Unit,
    onSpinMulti: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentLuck = when (selectedBanner) {
        RoyaleBannerId.DIAMOND_ROYALE -> profile.diamondRoyaleLuck
        RoyaleBannerId.WEAPON_ROYALE -> profile.weaponRoyaleLuck
        RoyaleBannerId.GOLD_ROYALE -> profile.goldRoyaleLuck
        RoyaleBannerId.INCUBATOR -> profile.incubatorLuck
    }
    val pool = GameCatalog.poolForBanner(selectedBanner)
    val grandPrize = GameCatalog.byId(selectedBanner.grandPrizeId) ?: pool.first()
    val bannerColor = when (selectedBanner) {
        RoyaleBannerId.DIAMOND_ROYALE -> DiamondCyan
        RoyaleBannerId.WEAPON_ROYALE -> BlazeOrange
        RoyaleBannerId.GOLD_ROYALE -> BlazeGold
        RoyaleBannerId.INCUBATOR -> MythicCrimson
    }

    var useLootCrateMode by remember { mutableStateOf(false) }

    val wheelRotation = remember { Animatable(0f) }
    LaunchedEffect(isSpinning) {
        if (isSpinning) {
            wheelRotation.animateTo(
                targetValue = wheelRotation.value + 1440f + 225f,
                animationSpec = tween(durationMillis = 880, easing = FastOutSlowInEasing)
            )
        }
    }

    val currencyIcon = if (selectedBanner.currency == CurrencyType.DIAMONDS) Icons.Default.Diamond else Icons.Default.MonetizationOn
    val singleActionLabel = if (useLootCrateMode) "OPEN 1 CRATE" else "1 SPIN"
    val multiActionLabel = if (useLootCrateMode) "OPEN ${selectedBanner.multiCount}" else selectedBanner.multiLabel

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Top Banner Tabs + Wheel/Crate Switcher
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(RoyaleBannerId.entries) { banner ->
                    val active = banner == selectedBanner
                    val tabColor = when (banner) {
                        RoyaleBannerId.DIAMOND_ROYALE -> DiamondCyan
                        RoyaleBannerId.WEAPON_ROYALE -> BlazeOrange
                        RoyaleBannerId.GOLD_ROYALE -> BlazeGold
                        RoyaleBannerId.INCUBATOR -> MythicCrimson
                    }
                    Surface(
                        color = if (active) tabColor else ObsidianCard,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .border(1.dp, if (active) tabColor else ObsidianBorder, RoundedCornerShape(10.dp))
                            .clickable { onSelectBanner(banner) }
                            .minimumInteractiveComponentSize()
                            .testTag("banner_tab_${banner.key}")
                    ) {
                        Text(
                            text = banner.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (active) ObsidianBg else TextSecondary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    color = if (!useLootCrateMode) bannerColor.copy(alpha = 0.22f) else ObsidianCard,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .border(1.dp, if (!useLootCrateMode) bannerColor else ObsidianBorder, RoundedCornerShape(10.dp))
                        .clickable { useLootCrateMode = false }
                        .minimumInteractiveComponentSize()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Casino, contentDescription = null, tint = bannerColor, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WHEEL", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                    }
                }
                Surface(
                    color = if (useLootCrateMode) bannerColor.copy(alpha = 0.22f) else ObsidianCard,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .border(1.dp, if (useLootCrateMode) bannerColor else ObsidianBorder, RoundedCornerShape(10.dp))
                        .clickable { useLootCrateMode = true }
                        .minimumInteractiveComponentSize()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Inventory2, contentDescription = null, tint = BlazeGold, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("CRATE", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                    }
                }
            }
        }

        // Main Landscape 2-Column Stage
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // LEFT PANE: Spinning Wheel / Loot Crate Stage + Spin CTAs
            Column(
                modifier = Modifier
                    .weight(0.45f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.5.dp, bannerColor.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_royale_banner),
                        contentDescription = selectedBanner.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        ObsidianBg.copy(alpha = 0.40f),
                                        ObsidianBg.copy(alpha = 0.85f),
                                        ObsidianBg.copy(alpha = 0.96f)
                                    )
                                )
                            )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = BlazeGold, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("GRAND PRIZE", style = MaterialTheme.typography.labelSmall, color = BlazeGold)
                            }
                            Text(
                                text = grandPrize.name,
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = selectedBanner.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 2
                            )
                            RarityChip(rarity = grandPrize.rarity)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        if (useLootCrateMode) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(106.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(bannerColor.copy(alpha = 0.16f))
                                        .border(2.dp, BlazeGold, RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CatalogItemPreview(item = grandPrize, modifier = Modifier.size(88.dp))
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = if (isSpinning) "OPENING..." else "MYTHIC CRATE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BlazeGold
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier.size(132.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val center = Offset(size.width * 0.5f, size.height * 0.5f)
                                    val radius = size.minDimension * 0.45f
                                    val wedgeColors = listOf(
                                        rarityColor(ItemRarity.MYTHIC),
                                        rarityColor(ItemRarity.RARE),
                                        rarityColor(ItemRarity.EPIC),
                                        rarityColor(ItemRarity.LEGENDARY),
                                        rarityColor(ItemRarity.RARE),
                                        rarityColor(ItemRarity.EPIC),
                                        rarityColor(ItemRarity.LEGENDARY),
                                        rarityColor(ItemRarity.RARE)
                                    )
                                    rotate(degrees = wheelRotation.value, pivot = center) {
                                        val sweep = 360f / wedgeColors.size
                                        wedgeColors.forEachIndexed { idx, wColor ->
                                            drawArc(
                                                color = wColor.copy(alpha = 0.38f),
                                                startAngle = idx * sweep,
                                                sweepAngle = sweep,
                                                useCenter = true,
                                                topLeft = Offset(center.x - radius, center.y - radius),
                                                size = Size(radius * 2, radius * 2)
                                            )
                                            drawArc(
                                                color = wColor,
                                                startAngle = idx * sweep,
                                                sweepAngle = sweep,
                                                useCenter = true,
                                                topLeft = Offset(center.x - radius, center.y - radius),
                                                size = Size(radius * 2, radius * 2),
                                                style = Stroke(width = 2f)
                                            )
                                        }
                                    }
                                    drawCircle(color = BlazeGold, radius = radius, center = center, style = Stroke(width = 3f))
                                    val pointer = Path().apply {
                                        moveTo(center.x, center.y - radius + 7f)
                                        lineTo(center.x - 9f, center.y - radius - 7f)
                                        lineTo(center.x + 9f, center.y - radius - 7f)
                                        close()
                                    }
                                    drawPath(pointer, color = BlazeGold)
                                }

                                CatalogItemPreview(
                                    item = grandPrize,
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, BlazeGold, CircleShape)
                                )
                            }
                        }
                    }
                }

                // Spin CTAs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onSpinSingle,
                        enabled = !isSpinning,
                        colors = ButtonDefaults.buttonColors(containerColor = ObsidianElevated, contentColor = TextPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .border(1.5.dp, bannerColor, RoundedCornerShape(12.dp))
                            .testTag("royale_spin_single")
                    ) {
                        if (isSpinning) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = bannerColor)
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(singleActionLabel, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(currencyIcon, contentDescription = null, tint = bannerColor, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(formatNumber(selectedBanner.singleCost), style = MaterialTheme.typography.labelSmall, color = bannerColor)
                                }
                            }
                        }
                    }

                    Button(
                        onClick = onSpinMulti,
                        enabled = !isSpinning,
                        colors = ButtonDefaults.buttonColors(containerColor = bannerColor, contentColor = ObsidianBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.15f)
                            .height(54.dp)
                            .testTag("royale_spin_multi")
                    ) {
                        if (isSpinning) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.5.dp, color = ObsidianBg)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (useLootCrateMode) "OPENING..." else "SPINNING...", style = MaterialTheme.typography.labelLarge)
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(multiActionLabel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(currencyIcon, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(formatNumber(selectedBanner.multiCost), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            // RIGHT PANE: Odds + Pity Progress + Prize Pool + Recent History
            Column(
                modifier = Modifier
                    .weight(0.55f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ObsidianCard)
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("LUCK METER: $currentLuck / 100", style = MaterialTheme.typography.titleSmall, color = BlazeGold)
                        Text("Tokens: ${profile.royaleTokens}", style = MaterialTheme.typography.labelSmall, color = DiamondCyan)
                    }
                    LinearProgressIndicator(
                        progress = { (currentLuck / 100f).coerceIn(0f, 1f) },
                        color = bannerColor,
                        trackColor = ObsidianElevated,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        GameCatalog.tierOdds.forEach { (rarity, pct) ->
                            val rColor = rarityColor(rarity)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(rColor.copy(alpha = 0.12f))
                                    .border(1.dp, rColor.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                                    .padding(vertical = 3.dp)
                            ) {
                                Text(rarity.label, style = MaterialTheme.typography.labelSmall, color = rColor)
                                Text("$pct%", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            }
                        }
                    }
                }

                Text("VAULT PRIZE POOL", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(pool, key = { it.id }) { item ->
                        val owned = inventory.containsKey(item.id)
                        Column(
                            modifier = Modifier
                                .width(114.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ObsidianCard)
                                .border(1.dp, rarityColor(item.rarity).copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                .padding(6.dp)
                        ) {
                            CatalogItemPreview(
                                item = item,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(66.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.labelLarge,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (owned) "OWNED" else item.rarity.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (owned) TacticalGreen else rarityColor(item.rarity)
                            )
                        }
                    }
                }

                if (royaleHistory.isNotEmpty()) {
                    Text("RECENT DRAWS", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        royaleHistory.take(3).forEach { record ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ObsidianCard)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = BlazeGold, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${record.bannerId.replace("_", " ").uppercase()} • ${record.spinCount}x",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextPrimary
                                    )
                                }
                                Text("BEST: ${record.highestRarity.uppercase()}", style = MaterialTheme.typography.labelSmall, color = BlazeGold)
                            }
                        }
                    }
                }
            }
        }
    }
}
