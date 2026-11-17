package com.example.data.repository

import android.content.Context
import com.example.R
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
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class GameRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : GameDataSource {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    companion object {
        const val UNLIMITED_FLOOR = 1_000_000L
        const val UNLIMITED_BASELINE = 9_999_999L
        const val MAX_CURRENCY = 999_999_999L
    }

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    override fun observeProfile(userId: String): Flow<UserProfile?> {
        val docRef = db.collection("users").document(userId)
        return docRef.snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) null
                else snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                }
                throw error
            }
    }

    override fun observeInventory(userId: String): Flow<List<OwnedItem>> {
        val colRef = db.collection("users").document(userId).collection("inventory")
        return colRef.snapshots()
            .map { snapshot ->
                snapshot.toObjects(OwnedItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, colRef.path)
                }
                throw error
            }
    }

    override fun observeMatchHistory(userId: String): Flow<List<MatchRecord>> {
        val colRef = db.collection("users").document(userId).collection("matches")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(20)
        return colRef.snapshots()
            .map { snapshot ->
                snapshot.toObjects(MatchRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, "users/$userId/matches")
                }
                throw error
            }
    }

    override fun observeRoyaleHistory(userId: String): Flow<List<RoyaleSpinRecord>> {
        val colRef = db.collection("users").document(userId).collection("royale_history")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(15)
        return colRef.snapshots()
            .map { snapshot ->
                snapshot.toObjects(RoyaleSpinRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, "users/$userId/royale_history")
                }
                throw error
            }
    }

    override suspend fun ensureUserProfile(rawCallsign: String?): UserProfile {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        val existing = try {
            userRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, userRef.path)
            throw e
        }

        if (existing.exists()) {
            val profile = existing.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (profile != null) return profile
        }

        val cleanName = rawCallsign?.trim()?.takeIf { it.length >= 2 }?.take(24)
            ?: "Blaze_${uid.take(4)}"

        val payload = mapOf(
            "userId" to uid,
            "callsign" to cleanName,
            "diamonds" to UNLIMITED_BASELINE,
            "gold" to UNLIMITED_BASELINE,
            "royaleTokens" to 25,
            "diamondRoyaleLuck" to 0,
            "weaponRoyaleLuck" to 0,
            "goldRoyaleLuck" to 0,
            "incubatorLuck" to 0,
            "equippedCharacterId" to "char_phantom",
            "equippedRifleSkinId" to "wpn_akm_default",
            "equippedSmgSkinId" to "wpn_mp40_default",
            "equippedGlooSkinId" to "gloo_default",
            "equippedAttachmentId" to "att_holo_sight",
            "equippedHatId" to "hat_none",
            "equippedOutfitId" to "outfit_default",
            "equippedAccessoryId" to "acc_none",
            "matchesPlayed" to 0,
            "booyahs" to 0,
            "totalKills" to 0,
            "totalHeadshots" to 0,
            "rankPoints" to 1000,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        val batch = db.batch()
        batch.set(userRef, payload)

        GameCatalog.starterIds.forEach { starterId ->
            val item = GameCatalog.byId(starterId) ?: return@forEach
            val itemRef = userRef.collection("inventory").document(item.id)
            batch.set(
                itemRef,
                mapOf(
                    "itemId" to item.id,
                    "userId" to uid,
                    "category" to item.category.key,
                    "name" to item.name,
                    "rarity" to item.rarity.key,
                    "acquiredVia" to "starter",
                    "quantity" to 1,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        try {
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, userRef.path)
            throw e
        }

        return UserProfile(
            id = uid,
            userId = uid,
            callsign = cleanName,
            diamonds = UNLIMITED_BASELINE,
            gold = UNLIMITED_BASELINE,
            royaleTokens = 25L
        )
    }

    override suspend fun refillUnlimitedCurrency(profile: UserProfile): Pair<Long, Long> {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        val nextDiamonds = (profile.diamonds + 5_000_000L).coerceAtMost(MAX_CURRENCY)
        val nextGold = (profile.gold + 5_000_000L).coerceAtMost(MAX_CURRENCY)
        val updates = mapOf(
            "diamonds" to nextDiamonds,
            "gold" to nextGold,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            userRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path)
            throw e
        }
        return nextDiamonds to nextGold
    }

    override suspend fun updateCallsign(newCallsign: String) {
        val uid = requireUserId()
        val sanitized = newCallsign.trim().take(24)
        require(sanitized.length >= 2) { "Callsign must be at least 2 characters." }
        val userRef = db.collection("users").document(uid)
        try {
            userRef.update(
                mapOf(
                    "callsign" to sanitized,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path)
            throw e
        }
    }

    override suspend fun equipItem(item: CatalogItem) {
        if (item.category == ItemCategory.CONSUMABLE) return
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        val itemsToEquip = if (item.category == ItemCategory.BUNDLE && item.bundleItemIds.isNotEmpty()) {
            GameCatalog.bundleContents(item)
        } else {
            listOf(item)
        }
        val updates = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
        itemsToEquip.forEach { target ->
            val fieldName = GameEconomyEngine.equipFieldForItem(target)
            if (fieldName != null) {
                updates[fieldName] = target.id
            }
        }
        if (updates.size <= 1) return
        try {
            userRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path)
            throw e
        }
    }

    override suspend fun purchaseStoreItem(
        item: CatalogItem,
        profile: UserProfile,
        existingInventory: Map<String, OwnedItem>
    ) {
        if (!GameEconomyEngine.hasSufficientBalance(profile, item)) {
            val currencyName = if (item.currency == CurrencyType.DIAMONDS) "Diamonds" else "Gold"
            throw IllegalStateException("Insufficient $currencyName balance for ${item.name}.")
        }
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)

        val (nextDiamonds, nextGold) = GameEconomyEngine.deductWithAutoReplenish(
            diamonds = profile.diamonds,
            gold = profile.gold,
            cost = item.price,
            currency = item.currency
        )

        val batch = db.batch()
        batch.update(
            userRef,
            mapOf(
                "diamonds" to nextDiamonds,
                "gold" to nextGold,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        val itemsToUnlock = buildList {
            add(item)
            if (item.category == ItemCategory.BUNDLE && item.bundleItemIds.isNotEmpty()) {
                addAll(GameCatalog.bundleContents(item))
            }
        }

        itemsToUnlock.forEach { target ->
            val invRef = userRef.collection("inventory").document(target.id)
            val owned = existingInventory[target.id]
            if (owned == null) {
                batch.set(
                    invRef,
                    mapOf(
                        "itemId" to target.id,
                        "userId" to uid,
                        "category" to target.category.key,
                        "name" to target.name,
                        "rarity" to target.rarity.key,
                        "acquiredVia" to if (item.category == ItemCategory.BUNDLE && target.id != item.id) "bundle" else "store",
                        "quantity" to 1,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else if (target.id == item.id || target.category == ItemCategory.CONSUMABLE) {
                batch.update(
                    invRef,
                    mapOf(
                        "quantity" to (owned.quantity + 1).coerceAtMost(99999),
                        "acquiredVia" to "store",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
        }

        try {
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userRef.collection("inventory").document(item.id).path)
            throw e
        }
    }

    override suspend fun performLuckRoyaleSpin(
        banner: RoyaleBannerId,
        isMulti: Boolean,
        profile: UserProfile,
        existingInventory: Map<String, OwnedItem>
    ): SpinOutcome {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        val spinCount = if (isMulti) banner.multiCount else 1
        val cost = if (isMulti) banner.multiCost else banner.singleCost

        var nextDiamonds = profile.diamonds
        var nextGold = profile.gold
        when (banner.currency) {
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

        var currentLuck = when (banner) {
            RoyaleBannerId.DIAMOND_ROYALE -> profile.diamondRoyaleLuck
            RoyaleBannerId.WEAPON_ROYALE -> profile.weaponRoyaleLuck
            RoyaleBannerId.GOLD_ROYALE -> profile.goldRoyaleLuck
            RoyaleBannerId.INCUBATOR -> profile.incubatorLuck
        }

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
                val chosen = when {
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
                chosen
            }
            rolledItems.add(dropped)
        }

        val batch = db.batch()
        val newlyUnlocked = mutableSetOf<String>()
        var bonusTokens = 0
        val countPerItem = mutableMapOf<String, Int>()
        rolledItems.forEach { item ->
            countPerItem[item.id] = (countPerItem[item.id] ?: 0) + 1
        }

        countPerItem.forEach { (itemId, count) ->
            val item = GameCatalog.byId(itemId) ?: return@forEach
            val invRef = userRef.collection("inventory").document(itemId)
            val existing = existingInventory[itemId]
            if (existing == null) {
                newlyUnlocked.add(itemId)
                if (count > 1) {
                    bonusTokens += (count - 1) * tokenValueForRarity(item.rarity)
                }
                batch.set(
                    invRef,
                    mapOf(
                        "itemId" to item.id,
                        "userId" to uid,
                        "category" to item.category.key,
                        "name" to item.name,
                        "rarity" to item.rarity.key,
                        "acquiredVia" to "luck_royale",
                        "quantity" to count.coerceAtMost(99999),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                bonusTokens += count * tokenValueForRarity(item.rarity)
                batch.update(
                    invRef,
                    mapOf(
                        "quantity" to (existing.quantity + count).coerceAtMost(99999),
                        "acquiredVia" to "luck_royale",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
        }

        val luckField = when (banner) {
            RoyaleBannerId.DIAMOND_ROYALE -> "diamondRoyaleLuck"
            RoyaleBannerId.WEAPON_ROYALE -> "weaponRoyaleLuck"
            RoyaleBannerId.GOLD_ROYALE -> "goldRoyaleLuck"
            RoyaleBannerId.INCUBATOR -> "incubatorLuck"
        }

        val nextTokens = (profile.royaleTokens + bonusTokens).coerceAtMost(MAX_CURRENCY)
        batch.update(
            userRef,
            mapOf(
                "diamonds" to nextDiamonds,
                "gold" to nextGold,
                "royaleTokens" to nextTokens,
                luckField to currentLuck.coerceIn(0, 100),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        val highestRarity = rolledItems.minByOrNull { rarityOrder(it.rarity) }?.rarity ?: ItemRarity.RARE
        val historyRef = userRef.collection("royale_history").document()
        batch.set(
            historyRef,
            mapOf(
                "userId" to uid,
                "bannerId" to banner.key,
                "spinCount" to spinCount,
                "rewardIds" to rolledItems.map { it.id },
                "highestRarity" to highestRarity.key,
                "createdAt" to FieldValue.serverTimestamp()
            )
        )

        try {
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userRef.path)
            throw e
        }

        return SpinOutcome(
            banner = banner,
            items = rolledItems,
            newlyUnlockedIds = newlyUnlocked,
            bonusTokens = bonusTokens,
            newLuckValue = currentLuck,
            hitPityJackpot = hitJackpot
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
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        val matchRef = userRef.collection("matches").document()
        val isBooyah = placement == 1
        val rpDelta = when {
            isBooyah -> 45 + kills * 4
            placement <= 3 -> 24 + kills * 3
            placement <= 6 -> 10 + kills * 2
            else -> (-8 + kills * 2).coerceAtLeast(-15)
        }.coerceIn(-100, 200)

        val diamondsEarned = (if (isBooyah) 1000L else 250L) + kills * 75L
        val goldEarned = (if (isBooyah) 3500L else 900L) + kills * 250L

        val batch = db.batch()
        batch.set(
            matchRef,
            mapOf(
                "userId" to uid,
                "mode" to mode,
                "placement" to placement.coerceIn(1, 20),
                "kills" to kills.coerceIn(0, 30),
                "headshots" to headshots.coerceIn(0, 30),
                "damageDealt" to damageDealt.coerceIn(0, 50000),
                "survivalSeconds" to survivalSeconds.coerceIn(0, 3600),
                "booyah" to isBooyah,
                "rankPointsDelta" to rpDelta,
                "diamondsEarned" to diamondsEarned,
                "goldEarned" to goldEarned,
                "createdAt" to FieldValue.serverTimestamp()
            )
        )

        val nextDiamonds = (profile.diamonds + diamondsEarned).coerceAtMost(MAX_CURRENCY)
        val nextGold = (profile.gold + goldEarned).coerceAtMost(MAX_CURRENCY)
        val nextRp = (profile.rankPoints + rpDelta).coerceAtLeast(0)

        batch.update(
            userRef,
            mapOf(
                "matchesPlayed" to (profile.matchesPlayed + 1),
                "booyahs" to (profile.booyahs + if (isBooyah) 1 else 0),
                "totalKills" to (profile.totalKills + kills),
                "totalHeadshots" to (profile.totalHeadshots + headshots),
                "rankPoints" to nextRp,
                "diamonds" to nextDiamonds,
                "gold" to nextGold,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        try {
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, matchRef.path)
            throw e
        }

        return MatchRecord(
            id = matchRef.id,
            userId = uid,
            mode = mode,
            placement = placement,
            kills = kills,
            headshots = headshots,
            damageDealt = damageDealt,
            survivalSeconds = survivalSeconds,
            booyah = isBooyah,
            rankPointsDelta = rpDelta,
            diamondsEarned = diamondsEarned,
            goldEarned = goldEarned
        )
    }

    private fun tokenValueForRarity(rarity: ItemRarity): Int = when (rarity) {
        ItemRarity.MYTHIC -> 25
        ItemRarity.LEGENDARY -> 12
        ItemRarity.EPIC -> 6
        ItemRarity.RARE -> 3
    }

    private fun rarityOrder(rarity: ItemRarity): Int = when (rarity) {
        ItemRarity.MYTHIC -> 0
        ItemRarity.LEGENDARY -> 1
        ItemRarity.EPIC -> 2
        ItemRarity.RARE -> 3
    }
}
