package com.example.data.repository

import com.example.data.model.CatalogItem
import com.example.data.model.CurrencyType
import com.example.data.model.GameCatalog
import com.example.data.model.ItemCategory
import com.example.data.model.ItemRarity
import com.example.data.model.MatchRecord
import com.example.data.model.OwnedItem
import com.example.data.model.RoyaleBannerId
import com.example.data.model.RoyaleSpinRecord
import com.example.data.model.SpinOutcome
import com.example.data.model.UserProfile
import com.example.data.model.WeaponSlot
import kotlinx.coroutines.flow.Flow
import kotlin.random.Random

interface GameDataSource {
    fun observeProfile(userId: String): Flow<UserProfile?>
    fun observeInventory(userId: String): Flow<List<OwnedItem>>
    fun observeMatchHistory(userId: String): Flow<List<MatchRecord>>
    fun observeRoyaleHistory(userId: String): Flow<List<RoyaleSpinRecord>>

    suspend fun ensureUserProfile(rawCallsign: String?): UserProfile
    suspend fun refillUnlimitedCurrency(profile: UserProfile): Pair<Long, Long>
    suspend fun updateCallsign(newCallsign: String)
    suspend fun equipItem(item: CatalogItem)
    suspend fun purchaseStoreItem(
        item: CatalogItem,
        profile: UserProfile,
        existingInventory: Map<String, OwnedItem>
    )

    suspend fun performLuckRoyaleSpin(
        banner: RoyaleBannerId,
        isMulti: Boolean,
        profile: UserProfile,
        existingInventory: Map<String, OwnedItem>
    ): SpinOutcome

    suspend fun recordMatchResult(
        mode: String,
        placement: Int,
        kills: Int,
        headshots: Int,
        damageDealt: Int,
        survivalSeconds: Int,
        profile: UserProfile
    ): MatchRecord
}

object GameEconomyEngine {
    const val UNLIMITED_FLOOR = 1_000_000L
    const val UNLIMITED_BASELINE = 9_999_999L
    const val MAX_CURRENCY = 999_999_999L

    fun hasSufficientBalance(profile: UserProfile, item: CatalogItem): Boolean = when (item.currency) {
        CurrencyType.DIAMONDS -> profile.diamonds >= item.price
        CurrencyType.GOLD -> profile.gold >= item.price
    }

    fun deductWithAutoReplenish(
        diamonds: Long,
        gold: Long,
        cost: Long,
        currency: CurrencyType
    ): Pair<Long, Long> {
        var nextDiamonds = diamonds
        var nextGold = gold
        when (currency) {
            CurrencyType.DIAMONDS -> {
                nextDiamonds = (nextDiamonds - cost).let {
                    if (it < UNLIMITED_FLOOR) UNLIMITED_BASELINE else it
                }
            }
            CurrencyType.GOLD -> {
                nextGold = (nextGold - cost).let {
                    if (it < UNLIMITED_FLOOR) UNLIMITED_BASELINE else it
                }
            }
        }
        return nextDiamonds to nextGold
    }

    fun equipFieldForItem(item: CatalogItem): String? = when (item.category) {
        ItemCategory.CHARACTER, ItemCategory.BUNDLE -> "equippedCharacterId"
        ItemCategory.GLOO -> "equippedGlooSkinId"
        ItemCategory.ATTACHMENT -> "equippedAttachmentId"
        ItemCategory.HAT -> "equippedHatId"
        ItemCategory.OUTFIT -> "equippedOutfitId"
        ItemCategory.ACCESSORY -> "equippedAccessoryId"
        ItemCategory.CONSUMABLE -> null
        ItemCategory.WEAPON -> when (item.weaponSlot) {
            WeaponSlot.SMG -> "equippedSmgSkinId"
            else -> "equippedRifleSkinId"
        }
    }

    data class RoyaleRollResult(
        val rolledItems: List<CatalogItem>,
        val newLuckValue: Int,
        val hitJackpot: Boolean
    )

    fun rollLuckRoyale(
        banner: RoyaleBannerId,
        spinCount: Int,
        startingLuck: Int
    ): RoyaleRollResult {
        var currentLuck = startingLuck
        val pool = GameCatalog.poolForBanner(banner)
        val grandPrize = GameCatalog.byId(banner.grandPrizeId) ?: pool.first()
        val rolledItems = mutableListOf<CatalogItem>()
        var hitJackpot = false

        repeat(spinCount) {
            val nextLuck = currentLuck + 10
            val dropped = if (nextLuck >= 100) {
                hitJackpot = true
                currentLuck = 0
                grandPrize
            } else {
                val roll = Random.nextInt(100)
                when {
                    roll < 12 -> {
                        if (grandPrize.rarity == ItemRarity.MYTHIC) {
                            hitJackpot = true
                            currentLuck = 0
                        } else {
                            currentLuck = nextLuck
                        }
                        grandPrize
                    }
                    roll < 38 -> {
                        currentLuck = nextLuck
                        pool.filter { it.rarity == ItemRarity.LEGENDARY }.randomOrNull() ?: pool.first()
                    }
                    roll < 72 -> {
                        currentLuck = nextLuck
                        pool.filter { it.rarity == ItemRarity.EPIC }.randomOrNull() ?: pool.last()
                    }
                    else -> {
                        currentLuck = nextLuck
                        pool.random()
                    }
                }
            }
            rolledItems.add(dropped)
        }

        return RoyaleRollResult(
            rolledItems = rolledItems,
            newLuckValue = currentLuck.coerceIn(0, 100),
            hitJackpot = hitJackpot
        )
    }

    fun tokenValueForRarity(rarity: ItemRarity): Int = when (rarity) {
        ItemRarity.MYTHIC -> 25
        ItemRarity.LEGENDARY -> 12
        ItemRarity.EPIC -> 6
        ItemRarity.RARE -> 3
    }

    fun rarityOrder(rarity: ItemRarity): Int = when (rarity) {
        ItemRarity.MYTHIC -> 0
        ItemRarity.LEGENDARY -> 1
        ItemRarity.EPIC -> 2
        ItemRarity.RARE -> 3
    }
}
