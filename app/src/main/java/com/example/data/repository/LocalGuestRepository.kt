package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.CatalogItem
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object GuestSessionManager {
    private const val PREFS_NAME = "blitzfire_guest_prefs"
    private const val KEY_GUEST_ACTIVE = "guest_session_active"

    fun isGuestActive(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_GUEST_ACTIVE, false)
    }

    fun setGuestActive(context: Context, active: Boolean) {
        prefs(context).edit().putBoolean(KEY_GUEST_ACTIVE, active).apply()
    }

    private fun prefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}

class LocalGuestRepository(context: Context) : GameDataSource {

    companion object {
        const val GUEST_UID = "guest_local_operative"
        private const val PREFS_NAME = "blitzfire_guest_vault"
        private const val KEY_PROFILE = "guest_profile_json"
        private const val KEY_INVENTORY = "guest_inventory_json"
        private const val KEY_MATCHES = "guest_matches_json"
        private const val KEY_ROYALES = "guest_royales_json"
    }

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val profileFlow = MutableStateFlow<UserProfile?>(null)
    private val inventoryFlow = MutableStateFlow<List<OwnedItem>>(emptyList())
    private val matchesFlow = MutableStateFlow<List<MatchRecord>>(emptyList())
    private val royalesFlow = MutableStateFlow<List<RoyaleSpinRecord>>(emptyList())

    init {
        loadFromPrefs()
    }

    override fun observeProfile(userId: String): Flow<UserProfile?> = profileFlow.asStateFlow()

    override fun observeInventory(userId: String): Flow<List<OwnedItem>> = inventoryFlow.asStateFlow()

    override fun observeMatchHistory(userId: String): Flow<List<MatchRecord>> = matchesFlow.asStateFlow()

    override fun observeRoyaleHistory(userId: String): Flow<List<RoyaleSpinRecord>> = royalesFlow.asStateFlow()

    override suspend fun ensureUserProfile(rawCallsign: String?): UserProfile {
        val current = profileFlow.value
        if (current != null) {
            if (inventoryFlow.value.isEmpty()) {
                val starters = buildStarterInventory()
                inventoryFlow.value = starters
                persistInventory(starters)
            }
            return current
        }

        val cleanName = rawCallsign?.trim()?.takeIf { it.length >= 2 }?.take(24) ?: "Guest_Blaze"
        val created = UserProfile(
            id = GUEST_UID,
            userId = GUEST_UID,
            callsign = cleanName,
            diamonds = GameEconomyEngine.UNLIMITED_BASELINE,
            gold = GameEconomyEngine.UNLIMITED_BASELINE,
            royaleTokens = 25L
        )
        val starters = buildStarterInventory()
        profileFlow.value = created
        inventoryFlow.value = starters
        persistProfile(created)
        persistInventory(starters)
        return created
    }

    override suspend fun refillUnlimitedCurrency(profile: UserProfile): Pair<Long, Long> {
        val nextDiamonds = (profile.diamonds + 5_000_000L).coerceAtMost(GameEconomyEngine.MAX_CURRENCY)
        val nextGold = (profile.gold + 5_000_000L).coerceAtMost(GameEconomyEngine.MAX_CURRENCY)
        val updated = profile.copy(diamonds = nextDiamonds, gold = nextGold)
        profileFlow.value = updated
        persistProfile(updated)
        return nextDiamonds to nextGold
    }

    override suspend fun updateCallsign(newCallsign: String) {
        val sanitized = newCallsign.trim().take(24)
        require(sanitized.length >= 2) { "Callsign must be at least 2 characters." }
        val current = profileFlow.value ?: ensureUserProfile(sanitized)
        val updated = current.copy(callsign = sanitized)
        profileFlow.value = updated
        persistProfile(updated)
    }

    override suspend fun equipItem(item: CatalogItem) {
        if (item.category == ItemCategory.CONSUMABLE) return
        val current = profileFlow.value ?: ensureUserProfile(null)
        val itemsToEquip = if (item.category == ItemCategory.BUNDLE && item.bundleItemIds.isNotEmpty()) {
            GameCatalog.bundleContents(item)
        } else {
            listOf(item)
        }
        var updated = current
        itemsToEquip.forEach { target ->
            updated = when (target.category) {
                ItemCategory.CHARACTER, ItemCategory.BUNDLE -> updated.copy(equippedCharacterId = target.id)
                ItemCategory.GLOO -> updated.copy(equippedGlooSkinId = target.id)
                ItemCategory.ATTACHMENT -> updated.copy(equippedAttachmentId = target.id)
                ItemCategory.HAT -> updated.copy(equippedHatId = target.id)
                ItemCategory.OUTFIT -> updated.copy(equippedOutfitId = target.id)
                ItemCategory.ACCESSORY -> updated.copy(equippedAccessoryId = target.id)
                ItemCategory.CONSUMABLE -> updated
                ItemCategory.WEAPON -> when (target.weaponSlot) {
                    WeaponSlot.SMG -> updated.copy(equippedSmgSkinId = target.id)
                    else -> updated.copy(equippedRifleSkinId = target.id)
                }
            }
        }
        profileFlow.value = updated
        persistProfile(updated)
    }

    override suspend fun purchaseStoreItem(
        item: CatalogItem,
        profile: UserProfile,
        existingInventory: Map<String, OwnedItem>
    ) {
        if (!GameEconomyEngine.hasSufficientBalance(profile, item)) {
            val currencyName = if (item.currency == com.example.data.model.CurrencyType.DIAMONDS) "Diamonds" else "Gold"
            throw IllegalStateException("Insufficient $currencyName balance for ${item.name}.")
        }
        val (nextDiamonds, nextGold) = GameEconomyEngine.deductWithAutoReplenish(
            diamonds = profile.diamonds,
            gold = profile.gold,
            cost = item.price,
            currency = item.currency
        )
        val updatedProfile = profile.copy(diamonds = nextDiamonds, gold = nextGold)

        val currentInv = inventoryFlow.value.associateBy { it.itemId }.toMutableMap()
        val itemsToUnlock = buildList {
            add(item)
            if (item.category == ItemCategory.BUNDLE && item.bundleItemIds.isNotEmpty()) {
                addAll(GameCatalog.bundleContents(item))
            }
        }
        itemsToUnlock.forEach { target ->
            val existing = currentInv[target.id] ?: existingInventory[target.id]
            currentInv[target.id] = if (existing == null) {
                OwnedItem(
                    id = target.id,
                    itemId = target.id,
                    userId = GUEST_UID,
                    category = target.category.key,
                    name = target.name,
                    rarity = target.rarity.key,
                    acquiredVia = if (item.category == ItemCategory.BUNDLE && target.id != item.id) "bundle" else "store",
                    quantity = 1
                )
            } else if (target.id == item.id || target.category == ItemCategory.CONSUMABLE) {
                existing.copy(
                    quantity = (existing.quantity + 1).coerceAtMost(99999),
                    acquiredVia = "store"
                )
            } else {
                existing
            }
        }

        val nextInvList = currentInv.values.toList()
        profileFlow.value = updatedProfile
        inventoryFlow.value = nextInvList
        persistProfile(updatedProfile)
        persistInventory(nextInvList)
    }

    override suspend fun performLuckRoyaleSpin(
        banner: RoyaleBannerId,
        isMulti: Boolean,
        profile: UserProfile,
        existingInventory: Map<String, OwnedItem>
    ): SpinOutcome {
        val spinCount = if (isMulti) banner.multiCount else 1
        val cost = if (isMulti) banner.multiCost else banner.singleCost

        val (nextDiamonds, nextGold) = GameEconomyEngine.deductWithAutoReplenish(
            diamonds = profile.diamonds,
            gold = profile.gold,
            cost = cost,
            currency = banner.currency
        )

        val currentLuck = when (banner) {
            RoyaleBannerId.DIAMOND_ROYALE -> profile.diamondRoyaleLuck
            RoyaleBannerId.WEAPON_ROYALE -> profile.weaponRoyaleLuck
            RoyaleBannerId.GOLD_ROYALE -> profile.goldRoyaleLuck
            RoyaleBannerId.INCUBATOR -> profile.incubatorLuck
        }

        val rollResult = GameEconomyEngine.rollLuckRoyale(
            banner = banner,
            spinCount = spinCount,
            startingLuck = currentLuck
        )

        val newlyUnlocked = mutableSetOf<String>()
        var bonusTokens = 0
        val countPerItem = mutableMapOf<String, Int>()
        rollResult.rolledItems.forEach { item ->
            countPerItem[item.id] = (countPerItem[item.id] ?: 0) + 1
        }

        val currentInv = inventoryFlow.value.associateBy { it.itemId }.toMutableMap()
        countPerItem.forEach { (itemId, count) ->
            val item = GameCatalog.byId(itemId) ?: return@forEach
            val existing = currentInv[itemId] ?: existingInventory[itemId]
            if (existing == null) {
                newlyUnlocked.add(itemId)
                if (count > 1) {
                    bonusTokens += (count - 1) * GameEconomyEngine.tokenValueForRarity(item.rarity)
                }
                currentInv[itemId] = OwnedItem(
                    id = item.id,
                    itemId = item.id,
                    userId = GUEST_UID,
                    category = item.category.key,
                    name = item.name,
                    rarity = item.rarity.key,
                    acquiredVia = "luck_royale",
                    quantity = count.coerceAtMost(99999)
                )
            } else {
                bonusTokens += count * GameEconomyEngine.tokenValueForRarity(item.rarity)
                currentInv[itemId] = existing.copy(
                    quantity = (existing.quantity + count).coerceAtMost(99999),
                    acquiredVia = "luck_royale"
                )
            }
        }

        val nextTokens = (profile.royaleTokens + bonusTokens).coerceAtMost(GameEconomyEngine.MAX_CURRENCY)
        val updatedProfile = when (banner) {
            RoyaleBannerId.DIAMOND_ROYALE -> profile.copy(
                diamonds = nextDiamonds, gold = nextGold, royaleTokens = nextTokens,
                diamondRoyaleLuck = rollResult.newLuckValue
            )
            RoyaleBannerId.WEAPON_ROYALE -> profile.copy(
                diamonds = nextDiamonds, gold = nextGold, royaleTokens = nextTokens,
                weaponRoyaleLuck = rollResult.newLuckValue
            )
            RoyaleBannerId.GOLD_ROYALE -> profile.copy(
                diamonds = nextDiamonds, gold = nextGold, royaleTokens = nextTokens,
                goldRoyaleLuck = rollResult.newLuckValue
            )
            RoyaleBannerId.INCUBATOR -> profile.copy(
                diamonds = nextDiamonds, gold = nextGold, royaleTokens = nextTokens,
                incubatorLuck = rollResult.newLuckValue
            )
        }

        val highestRarity = rollResult.rolledItems
            .minByOrNull { GameEconomyEngine.rarityOrder(it.rarity) }
            ?.rarity ?: ItemRarity.RARE
        val spinRecord = RoyaleSpinRecord(
            id = UUID.randomUUID().toString(),
            userId = GUEST_UID,
            bannerId = banner.key,
            spinCount = spinCount,
            rewardIds = rollResult.rolledItems.map { it.id },
            highestRarity = highestRarity.key
        )

        val nextInvList = currentInv.values.toList()
        val nextRoyales = (listOf(spinRecord) + royalesFlow.value).take(15)

        profileFlow.value = updatedProfile
        inventoryFlow.value = nextInvList
        royalesFlow.value = nextRoyales
        persistProfile(updatedProfile)
        persistInventory(nextInvList)
        persistRoyales(nextRoyales)

        return SpinOutcome(
            banner = banner,
            items = rollResult.rolledItems,
            newlyUnlockedIds = newlyUnlocked,
            bonusTokens = bonusTokens,
            newLuckValue = rollResult.newLuckValue,
            hitPityJackpot = rollResult.hitJackpot
        )
    }

    override suspend fun recordMatchResult(
        mode: String,
        placement: Int,
        kills: Int,
        headshots: Int,
        damageDealt: Int,
        survivalSeconds: Int,
        profile: UserProfile
    ): MatchRecord {
        val isBooyah = placement == 1
        val rpDelta = when {
            isBooyah -> 45 + kills * 4
            placement <= 3 -> 24 + kills * 3
            placement <= 6 -> 10 + kills * 2
            else -> (-8 + kills * 2).coerceAtLeast(-15)
        }.coerceIn(-100, 200)

        val diamondsEarned = (if (isBooyah) 1000L else 250L) + kills * 75L
        val goldEarned = (if (isBooyah) 3500L else 900L) + kills * 250L

        val record = MatchRecord(
            id = UUID.randomUUID().toString(),
            userId = GUEST_UID,
            mode = mode,
            placement = placement.coerceIn(1, 20),
            kills = kills.coerceIn(0, 30),
            headshots = headshots.coerceIn(0, 30),
            damageDealt = damageDealt.coerceIn(0, 50000),
            survivalSeconds = survivalSeconds.coerceIn(0, 3600),
            booyah = isBooyah,
            rankPointsDelta = rpDelta,
            diamondsEarned = diamondsEarned,
            goldEarned = goldEarned
        )

        val updatedProfile = profile.copy(
            matchesPlayed = profile.matchesPlayed + 1,
            booyahs = profile.booyahs + if (isBooyah) 1 else 0,
            totalKills = profile.totalKills + kills,
            totalHeadshots = profile.totalHeadshots + headshots,
            rankPoints = (profile.rankPoints + rpDelta).coerceAtLeast(0),
            diamonds = (profile.diamonds + diamondsEarned).coerceAtMost(GameEconomyEngine.MAX_CURRENCY),
            gold = (profile.gold + goldEarned).coerceAtMost(GameEconomyEngine.MAX_CURRENCY)
        )

        val nextMatches = (listOf(record) + matchesFlow.value).take(20)
        profileFlow.value = updatedProfile
        matchesFlow.value = nextMatches
        persistProfile(updatedProfile)
        persistMatches(nextMatches)
        return record
    }

    private fun buildStarterInventory(): List<OwnedItem> {
        return GameCatalog.starterIds.mapNotNull { starterId ->
            val item = GameCatalog.byId(starterId) ?: return@mapNotNull null
            OwnedItem(
                id = item.id,
                itemId = item.id,
                userId = GUEST_UID,
                category = item.category.key,
                name = item.name,
                rarity = item.rarity.key,
                acquiredVia = "starter",
                quantity = 1
            )
        }
    }

    private fun loadFromPrefs() {
        prefs.getString(KEY_PROFILE, null)?.let { raw ->
            runCatching { decodeProfile(JSONObject(raw)) }.getOrNull()?.let { profileFlow.value = it }
        }
        prefs.getString(KEY_INVENTORY, null)?.let { raw ->
            runCatching { decodeInventory(JSONArray(raw)) }.getOrNull()?.let { inventoryFlow.value = it }
        }
        prefs.getString(KEY_MATCHES, null)?.let { raw ->
            runCatching { decodeMatches(JSONArray(raw)) }.getOrNull()?.let { matchesFlow.value = it }
        }
        prefs.getString(KEY_ROYALES, null)?.let { raw ->
            runCatching { decodeRoyales(JSONArray(raw)) }.getOrNull()?.let { royalesFlow.value = it }
        }
    }

    private fun persistProfile(p: UserProfile) {
        val json = JSONObject().apply {
            put("callsign", p.callsign)
            put("diamonds", p.diamonds)
            put("gold", p.gold)
            put("royaleTokens", p.royaleTokens)
            put("diamondRoyaleLuck", p.diamondRoyaleLuck)
            put("weaponRoyaleLuck", p.weaponRoyaleLuck)
            put("goldRoyaleLuck", p.goldRoyaleLuck)
            put("incubatorLuck", p.incubatorLuck)
            put("equippedCharacterId", p.equippedCharacterId)
            put("equippedRifleSkinId", p.equippedRifleSkinId)
            put("equippedSmgSkinId", p.equippedSmgSkinId)
            put("equippedGlooSkinId", p.equippedGlooSkinId)
            put("equippedAttachmentId", p.equippedAttachmentId)
            put("equippedHatId", p.equippedHatId)
            put("equippedOutfitId", p.equippedOutfitId)
            put("equippedAccessoryId", p.equippedAccessoryId)
            put("matchesPlayed", p.matchesPlayed)
            put("booyahs", p.booyahs)
            put("totalKills", p.totalKills)
            put("totalHeadshots", p.totalHeadshots)
            put("rankPoints", p.rankPoints)
        }
        prefs.edit().putString(KEY_PROFILE, json.toString()).apply()
    }

    private fun decodeProfile(j: JSONObject): UserProfile = UserProfile(
        id = GUEST_UID,
        userId = GUEST_UID,
        callsign = j.optString("callsign", "Guest_Blaze"),
        diamonds = j.optLong("diamonds", GameEconomyEngine.UNLIMITED_BASELINE),
        gold = j.optLong("gold", GameEconomyEngine.UNLIMITED_BASELINE),
        royaleTokens = j.optLong("royaleTokens", 25L),
        diamondRoyaleLuck = j.optInt("diamondRoyaleLuck", 0),
        weaponRoyaleLuck = j.optInt("weaponRoyaleLuck", 0),
        goldRoyaleLuck = j.optInt("goldRoyaleLuck", 0),
        incubatorLuck = j.optInt("incubatorLuck", 0),
        equippedCharacterId = j.optString("equippedCharacterId", "char_phantom"),
        equippedRifleSkinId = j.optString("equippedRifleSkinId", "wpn_akm_default"),
        equippedSmgSkinId = j.optString("equippedSmgSkinId", "wpn_mp40_default"),
        equippedGlooSkinId = j.optString("equippedGlooSkinId", "gloo_default"),
        equippedAttachmentId = j.optString("equippedAttachmentId", "att_holo_sight"),
        equippedHatId = j.optString("equippedHatId", "hat_none"),
        equippedOutfitId = j.optString("equippedOutfitId", "outfit_default"),
        equippedAccessoryId = j.optString("equippedAccessoryId", "acc_none"),
        matchesPlayed = j.optInt("matchesPlayed", 0),
        booyahs = j.optInt("booyahs", 0),
        totalKills = j.optInt("totalKills", 0),
        totalHeadshots = j.optInt("totalHeadshots", 0),
        rankPoints = j.optInt("rankPoints", 1000)
    )

    private fun persistInventory(items: List<OwnedItem>) {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("itemId", item.itemId)
                    put("category", item.category)
                    put("name", item.name)
                    put("rarity", item.rarity)
                    put("acquiredVia", item.acquiredVia)
                    put("quantity", item.quantity)
                }
            )
        }
        prefs.edit().putString(KEY_INVENTORY, arr.toString()).apply()
    }

    private fun decodeInventory(arr: JSONArray): List<OwnedItem> {
        val list = mutableListOf<OwnedItem>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val id = o.optString("itemId")
            if (id.isNotBlank()) {
                list.add(
                    OwnedItem(
                        id = id,
                        itemId = id,
                        userId = GUEST_UID,
                        category = o.optString("category", "weapon"),
                        name = o.optString("name", ""),
                        rarity = o.optString("rarity", "rare"),
                        acquiredVia = o.optString("acquiredVia", "starter"),
                        quantity = o.optInt("quantity", 1)
                    )
                )
            }
        }
        return list
    }

    private fun persistMatches(matches: List<MatchRecord>) {
        val arr = JSONArray()
        matches.forEach { m ->
            arr.put(
                JSONObject().apply {
                    put("id", m.id)
                    put("mode", m.mode)
                    put("placement", m.placement)
                    put("kills", m.kills)
                    put("headshots", m.headshots)
                    put("damageDealt", m.damageDealt)
                    put("survivalSeconds", m.survivalSeconds)
                    put("booyah", m.booyah)
                    put("rankPointsDelta", m.rankPointsDelta)
                    put("diamondsEarned", m.diamondsEarned)
                    put("goldEarned", m.goldEarned)
                }
            )
        }
        prefs.edit().putString(KEY_MATCHES, arr.toString()).apply()
    }

    private fun decodeMatches(arr: JSONArray): List<MatchRecord> {
        val list = mutableListOf<MatchRecord>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            list.add(
                MatchRecord(
                    id = o.optString("id", UUID.randomUUID().toString()),
                    userId = GUEST_UID,
                    mode = o.optString("mode", "battle_royale"),
                    placement = o.optInt("placement", 1),
                    kills = o.optInt("kills", 0),
                    headshots = o.optInt("headshots", 0),
                    damageDealt = o.optInt("damageDealt", 0),
                    survivalSeconds = o.optInt("survivalSeconds", 0),
                    booyah = o.optBoolean("booyah", false),
                    rankPointsDelta = o.optInt("rankPointsDelta", 0),
                    diamondsEarned = o.optLong("diamondsEarned", 0L),
                    goldEarned = o.optLong("goldEarned", 0L)
                )
            )
        }
        return list
    }

    private fun persistRoyales(records: List<RoyaleSpinRecord>) {
        val arr = JSONArray()
        records.forEach { r ->
            arr.put(
                JSONObject().apply {
                    put("id", r.id)
                    put("bannerId", r.bannerId)
                    put("spinCount", r.spinCount)
                    put("highestRarity", r.highestRarity)
                    put("rewardIds", JSONArray(r.rewardIds))
                }
            )
        }
        prefs.edit().putString(KEY_ROYALES, arr.toString()).apply()
    }

    private fun decodeRoyales(arr: JSONArray): List<RoyaleSpinRecord> {
        val list = mutableListOf<RoyaleSpinRecord>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val rewardArr = o.optJSONArray("rewardIds") ?: JSONArray()
            val rewards = buildList {
                for (j in 0 until rewardArr.length()) {
                    add(rewardArr.optString(j))
                }
            }
            list.add(
                RoyaleSpinRecord(
                    id = o.optString("id", UUID.randomUUID().toString()),
                    userId = GUEST_UID,
                    bannerId = o.optString("bannerId", "diamond_royale"),
                    spinCount = o.optInt("spinCount", 1),
                    rewardIds = rewards,
                    highestRarity = o.optString("highestRarity", "rare")
                )
            )
        }
        return list
    }
}
