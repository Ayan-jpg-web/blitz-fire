package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CatalogItem
import com.example.data.model.CurrencyType
import com.example.data.model.GameCatalog
import com.example.data.model.ItemCategory
import com.example.data.model.OwnedItem
import com.example.data.model.UserProfile
import com.example.data.repository.GameEconomyEngine
import com.example.ui.components.CatalogItemPreview
import com.example.ui.components.CustomizationPreviewDialog
import com.example.ui.components.RarityChip
import com.example.ui.components.StatMiniBar
import com.example.ui.components.formatNumber
import com.example.ui.components.rarityColor
import com.example.ui.theme.BlazeGold
import com.example.ui.theme.BlazeOrange
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DiamondCyan
import com.example.ui.theme.MythicCrimson
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.TacticalGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private enum class StoreQuickFilter(val label: String) {
    ALL("All"),
    AFFORDABLE("Affordable"),
    DIAMONDS("Diamonds"),
    GOLD("Gold"),
    OWNED("Owned")
}

@Composable
fun StoreScreen(
    profile: UserProfile,
    inventory: Map<String, OwnedItem>,
    selectedCategory: ItemCategory?,
    searchQuery: String,
    isBusy: Boolean,
    onSelectCategory: (ItemCategory?) -> Unit,
    onSearchChange: (String) -> Unit,
    onPurchaseItem: (CatalogItem) -> Unit,
    onEquipItem: (CatalogItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var previewItem by remember { mutableStateOf<CatalogItem?>(null) }
    var quickFilter by remember { mutableStateOf(StoreQuickFilter.ALL) }

    val equippedIds = remember(profile) {
        setOf(
            profile.equippedCharacterId,
            profile.equippedRifleSkinId,
            profile.equippedSmgSkinId,
            profile.equippedGlooSkinId,
            profile.equippedAttachmentId,
            profile.equippedHatId,
            profile.equippedOutfitId,
            profile.equippedAccessoryId
        )
    }

    val allStoreItems = remember { GameCatalog.storeItems() }
    val storeItems = remember(allStoreItems, selectedCategory, searchQuery, quickFilter, profile, inventory) {
        allStoreItems.filter { item ->
            val matchesCategory = selectedCategory == null || item.category == selectedCategory
            val matchesQuery = searchQuery.isBlank() ||
                item.name.contains(searchQuery, ignoreCase = true) ||
                item.subtitle.contains(searchQuery, ignoreCase = true) ||
                item.perkDescription.contains(searchQuery, ignoreCase = true)
            val matchesQuick = when (quickFilter) {
                StoreQuickFilter.ALL -> true
                StoreQuickFilter.AFFORDABLE -> GameEconomyEngine.hasSufficientBalance(profile, item)
                StoreQuickFilter.DIAMONDS -> item.currency == CurrencyType.DIAMONDS
                StoreQuickFilter.GOLD -> item.currency == CurrencyType.GOLD
                StoreQuickFilter.OWNED -> inventory.containsKey(item.id)
            }
            matchesCategory && matchesQuery && matchesQuick
        }
    }

    val defaultSpotlight = remember(allStoreItems) {
        allStoreItems.firstOrNull { it.category == ItemCategory.BUNDLE }
            ?: allStoreItems.firstOrNull { it.category == ItemCategory.WEAPON }
            ?: allStoreItems.first()
    }
    var inspectedItem by remember { mutableStateOf<CatalogItem?>(null) }
    val spotlightItem = remember(inspectedItem, storeItems, defaultSpotlight) {
        val current = inspectedItem
        when {
            current != null && (current in storeItems || storeItems.isEmpty()) -> current
            storeItems.isNotEmpty() -> storeItems.first()
            else -> defaultSpotlight
        }
    }

    fun isItemEquipped(item: CatalogItem): Boolean {
        if (item.category == ItemCategory.BUNDLE && item.bundleItemIds.isNotEmpty()) {
            val parts = GameCatalog.bundleContents(item).filter { it.category != ItemCategory.CONSUMABLE }
            return parts.isNotEmpty() && parts.all { it.id in equippedIds }
        }
        return item.id in equippedIds
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // LEFT INSPECTOR PANE: Featured / Selected Weapon or Bundle + Balance Affordability Breakdown
        StoreSpotlightPane(
            item = spotlightItem,
            profile = profile,
            inventory = inventory,
            isEquipped = isItemEquipped(spotlightItem),
            isBusy = isBusy,
            onPreview3D = { previewItem = spotlightItem },
            onPurchase = { onPurchaseItem(spotlightItem) },
            onEquip = { onEquipItem(spotlightItem) },
            modifier = Modifier
                .weight(0.36f)
                .fillMaxHeight()
        )

        // RIGHT BROWSE PANE: Search, Category Tabs, Balance Filters & Store Grid
        Column(
            modifier = Modifier
                .weight(0.64f)
                .fillMaxHeight()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(0.95f)) {
                    Text(
                        text = "WEAPON & BUNDLE STORE",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BalanceInlineChip(
                            icon = Icons.Default.Diamond,
                            tint = DiamondCyan,
                            amount = formatNumber(profile.diamonds)
                        )
                        BalanceInlineChip(
                            icon = Icons.Default.MonetizationOn,
                            tint = BlazeGold,
                            amount = formatNumber(profile.gold)
                        )
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search bundles, rifles, SMGs...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search Store", tint = TextSecondary)
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BlazeOrange,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedContainerColor = ObsidianCard,
                        unfocusedContainerColor = ObsidianCard,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1.05f)
                        .testTag("store_search_field")
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            val categories = listOf(
                ItemCategory.BUNDLE,
                ItemCategory.WEAPON,
                ItemCategory.CHARACTER,
                ItemCategory.OUTFIT,
                ItemCategory.HAT,
                ItemCategory.ACCESSORY,
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
                        onClick = { onSelectCategory(null) },
                        label = { Text("All (${allStoreItems.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BlazeOrange,
                            selectedLabelColor = ObsidianBg
                        ),
                        modifier = Modifier.testTag("store_cat_all")
                    )
                }
                items(categories.size) { index ->
                    val cat = categories[index]
                    val count = allStoreItems.count { it.category == cat }
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { onSelectCategory(cat) },
                        label = {
                            Text(
                                text = if (cat == ItemCategory.BUNDLE) "Bundles ($count)" else "${cat.label} ($count)"
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (cat == ItemCategory.BUNDLE) MythicCrimson else BlazeOrange,
                            selectedLabelColor = if (cat == ItemCategory.BUNDLE) TextPrimary else ObsidianBg
                        ),
                        modifier = Modifier.testTag("store_cat_${cat.key}")
                    )
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(StoreQuickFilter.entries.size) { index ->
                    val filter = StoreQuickFilter.entries[index]
                    FilterChip(
                        selected = quickFilter == filter,
                        onClick = { quickFilter = filter },
                        label = { Text(filter.label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DiamondCyan,
                            selectedLabelColor = ObsidianBg
                        ),
                        modifier = Modifier.testTag("store_quick_${filter.name.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (storeItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ObsidianCard)
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "No store items match your filter.",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        OutlinedButton(onClick = {
                            onSelectCategory(null)
                            onSearchChange("")
                            quickFilter = StoreQuickFilter.ALL
                        }) {
                            Text("RESET FILTERS")
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(storeItems, key = { it.id }) { item ->
                        val owned = inventory[item.id]
                        val canAfford = GameEconomyEngine.hasSufficientBalance(profile, item)
                        StoreItemCard(
                            item = item,
                            ownedQuantity = owned?.quantity ?: 0,
                            isEquipped = isItemEquipped(item),
                            isSelected = item.id == spotlightItem.id,
                            canAfford = canAfford,
                            isBusy = isBusy,
                            onSelect = { inspectedItem = item },
                            onPreview = { previewItem = item },
                            onPurchase = { onPurchaseItem(item) },
                            onEquip = { onEquipItem(item) }
                        )
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
            isOwned = inventory.containsKey(activePreview.id),
            isEquipped = isItemEquipped(activePreview),
            onPurchase = { onPurchaseItem(activePreview) },
            onEquip = { onEquipItem(activePreview) },
            onDismiss = { previewItem = null }
        )
    }
}

@Composable
private fun BalanceInlineChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    amount: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(13.dp))
        Spacer(modifier = Modifier.width(3.dp))
        Text(amount, style = MaterialTheme.typography.labelSmall, color = tint, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StoreSpotlightPane(
    item: CatalogItem,
    profile: UserProfile,
    inventory: Map<String, OwnedItem>,
    isEquipped: Boolean,
    isBusy: Boolean,
    onPreview3D: () -> Unit,
    onPurchase: () -> Unit,
    onEquip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rColor = rarityColor(item.rarity)
    val isOwned = inventory.containsKey(item.id)
    val canAfford = GameEconomyEngine.hasSufficientBalance(profile, item)
    val balance = if (item.currency == CurrencyType.DIAMONDS) profile.diamonds else profile.gold
    val remaining = balance - item.price
    val bundleContents = remember(item.id) {
        if (item.category == ItemCategory.BUNDLE) GameCatalog.bundleContents(item) else emptyList()
    }
    val discountPct = if (item.originalPrice > item.price && item.originalPrice > 0L) {
        (((item.originalPrice - item.price) * 100L) / item.originalPrice).toInt()
    } else 0

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(ObsidianCard)
            .border(1.5.dp, rColor.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
            .padding(10.dp)
            .testTag("store_spotlight_pane")
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                CatalogItemPreview(
                    item = item,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(108.dp)
                        .clickable(onClick = onPreview3D)
                )
                if (discountPct > 0) {
                    Surface(
                        color = MythicCrimson,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = "-$discountPct% BUNDLE",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Surface(
                    color = ObsidianBg.copy(alpha = 0.86f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .border(1.dp, DiamondCyan.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                        .clickable(onClick = onPreview3D)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = "3D Preview", tint = DiamondCyan, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("3D VIEW", style = MaterialTheme.typography.labelSmall, color = DiamondCyan)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RarityChip(rarity = item.rarity)
                Text(
                    text = item.category.label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = DiamondCyan
                )
            }

            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.perkDescription,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            // Weapon / Tactical Stats
            when {
                item.damageBonus > 0 || item.fireRateBonus > 0 -> {
                    StatMiniBar("DAMAGE +${item.damageBonus}  •  FIRE RATE +${item.fireRateBonus}", ((item.damageBonus + item.fireRateBonus) / 13f), rColor)
                }
                item.maxHpBonus > 0 -> {
                    StatMiniBar("ARMOR / HP +${item.maxHpBonus}", (item.maxHpBonus / 170f), rColor)
                }
                item.rangeBonus > 0 -> {
                    StatMiniBar("LOCK RANGE +${item.rangeBonus}", (item.rangeBonus / 120f), rColor)
                }
            }

            // Included Bundle Breakdown
            if (bundleContents.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ObsidianElevated)
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(10.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Inventory2, contentDescription = null, tint = BlazeGold, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "PACK CONTENTS (${bundleContents.size} ITEMS)",
                            style = MaterialTheme.typography.labelSmall,
                            color = BlazeGold
                        )
                    }
                    bundleContents.forEach { part ->
                        val partOwned = inventory.containsKey(part.id)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "• ${part.name}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = if (partOwned) "OWNED" else part.category.label.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (partOwned) TacticalGreen else TextSecondary,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }

            // Balance Breakdown Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(ObsidianBg.copy(alpha = 0.72f))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("YOUR BALANCE", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
                    Text(
                        text = "${formatNumber(balance)} ${if (item.currency == CurrencyType.DIAMONDS) "DIA" else "GOLD"}",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (item.currency == CurrencyType.DIAMONDS) DiamondCyan else BlazeGold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (canAfford) "AFTER BUY" else "SHORTFALL",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (canAfford) TextSecondary else DangerRed,
                        fontSize = 9.sp
                    )
                    Text(
                        text = if (canAfford) formatNumber(remaining.coerceAtLeast(0L)) else "-${formatNumber(item.price - balance)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (canAfford) TacticalGreen else DangerRed
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when {
            isEquipped -> {
                OutlinedButton(
                    onClick = {},
                    enabled = false,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("EQUIPPED IN LOADOUT", style = MaterialTheme.typography.labelLarge, color = TacticalGreen)
                }
            }
            isOwned && item.category != ItemCategory.CONSUMABLE -> {
                Button(
                    onClick = onEquip,
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalGreen, contentColor = ObsidianBg),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = if (item.category == ItemCategory.BUNDLE) "EQUIP FULL BUNDLE" else "EQUIP NOW",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            else -> {
                val cColor = if (item.currency == CurrencyType.DIAMONDS) DiamondCyan else BlazeGold
                Button(
                    onClick = onPurchase,
                    enabled = !isBusy && canAfford,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canAfford) cColor else ObsidianElevated,
                        contentColor = if (canAfford) ObsidianBg else DangerRed
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = if (item.currency == CurrencyType.DIAMONDS) Icons.Default.Diamond else Icons.Default.MonetizationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (canAfford) {
                            if (item.category == ItemCategory.BUNDLE) "BUY BUNDLE • ${formatNumber(item.price)}"
                            else "BUY • ${formatNumber(item.price)}"
                        } else {
                            "INSUFFICIENT BALANCE"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun StoreItemCard(
    item: CatalogItem,
    ownedQuantity: Int,
    isEquipped: Boolean,
    isSelected: Boolean,
    canAfford: Boolean,
    isBusy: Boolean,
    onSelect: () -> Unit,
    onPreview: () -> Unit,
    onPurchase: () -> Unit,
    onEquip: () -> Unit
) {
    val isOwned = ownedQuantity > 0
    val rColor = rarityColor(item.rarity)
    val borderColor = when {
        isEquipped -> TacticalGreen
        isSelected -> BlazeGold
        else -> rColor.copy(alpha = 0.50f)
    }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(ObsidianCard)
            .border(if (isSelected || isEquipped) 2.dp else 1.2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onSelect)
            .padding(10.dp)
            .testTag("store_item_${item.id}")
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            CatalogItemPreview(
                item = item,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clickable(onClick = onSelect)
            )
            if (item.category == ItemCategory.BUNDLE && item.originalPrice > item.price) {
                val pct = (((item.originalPrice - item.price) * 100L) / item.originalPrice).toInt()
                Surface(
                    color = MythicCrimson,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(5.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocalOffer, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(10.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("-$pct%", style = MaterialTheme.typography.labelSmall, color = TextPrimary, fontSize = 9.sp)
                    }
                }
            }
            Surface(
                color = ObsidianBg.copy(alpha = 0.80f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .clickable(onClick = onPreview)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = "Preview", tint = DiamondCyan, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("3D", style = MaterialTheme.typography.labelSmall, color = DiamondCyan, fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RarityChip(rarity = item.rarity)
            if (isEquipped) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Equipped", tint = TacticalGreen, modifier = Modifier.size(17.dp))
            } else if (item.category == ItemCategory.CONSUMABLE && isOwned) {
                Text("x$ownedQuantity", style = MaterialTheme.typography.labelSmall, color = BlazeGold)
            } else if (item.originalPrice > item.price) {
                Text(
                    text = formatNumber(item.originalPrice),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    textDecoration = TextDecoration.LineThrough
                )
            }
        }

        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = item.name,
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = item.perkDescription,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.height(34.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))
        when {
            item.damageBonus > 0 -> StatMiniBar("DMG +${item.damageBonus}  •  ROF +${item.fireRateBonus}", (item.damageBonus / 10f), rColor)
            item.maxHpBonus > 0 -> StatMiniBar("BONUS ARMOR +${item.maxHpBonus}", (item.maxHpBonus / 170f), rColor)
            item.rangeBonus > 0 -> StatMiniBar("LOCK RANGE +${item.rangeBonus}", (item.rangeBonus / 120f), rColor)
            else -> Spacer(modifier = Modifier.height(19.dp))
        }

        Spacer(modifier = Modifier.height(6.dp))

        when {
            isEquipped -> {
                OutlinedButton(
                    onClick = {},
                    enabled = false,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("EQUIPPED", style = MaterialTheme.typography.labelLarge, color = TacticalGreen)
                }
            }
            isOwned && item.category != ItemCategory.CONSUMABLE -> {
                Button(
                    onClick = onEquip,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TacticalGreen,
                        contentColor = ObsidianBg
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("equip_store_${item.id}")
                ) {
                    Text(
                        text = if (item.category == ItemCategory.BUNDLE) "EQUIP ALL" else "EQUIP NOW",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            else -> {
                val currencyColor = if (item.currency == CurrencyType.DIAMONDS) DiamondCyan else BlazeGold
                Button(
                    onClick = onPurchase,
                    enabled = !isBusy && canAfford,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canAfford) currencyColor else ObsidianElevated,
                        contentColor = if (canAfford) ObsidianBg else DangerRed
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("buy_store_${item.id}")
                ) {
                    Icon(
                        imageVector = if (item.currency == CurrencyType.DIAMONDS) Icons.Default.Diamond else Icons.Default.MonetizationOn,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (canAfford) formatNumber(item.price) else "LOW ${if (item.currency == CurrencyType.DIAMONDS) "DIA" else "GOLD"}",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
