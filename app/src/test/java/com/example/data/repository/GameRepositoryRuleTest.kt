package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.GameCatalog
import com.example.data.model.RoyaleBannerId
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class GameRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureUserProfile_andStorePurchase_andLuckRoyaleSpin_succeedForOwner() = runBlocking {
        val uid = signInTestUser("alice_${UUID.randomUUID()}@test.com")
        val repository = GameRepository(firestore, auth)

        val profile = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureUserProfile("BlazeAlice")
        }
        assertEquals(uid, profile.userId)
        assertEquals(GameRepository.UNLIMITED_BASELINE, profile.diamonds)

        val inventory = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeInventory(uid).first { it.size >= GameCatalog.starterIds.size }
        }
        assertTrue(inventory.any { it.itemId == "char_phantom" })

        val storeItem = GameCatalog.byId("wpn_draco_akm")!!
        val invMap = inventory.associateBy { it.itemId }
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.purchaseStoreItem(storeItem, profile, invMap)
        }

        val updatedInv = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeInventory(uid).first { list -> list.any { it.itemId == "wpn_draco_akm" } }
        }
        assertTrue(updatedInv.any { it.itemId == "wpn_draco_akm" })

        val latestProfile = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeProfile(uid).first { it != null }!!
        }
        val spinOutcome = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.performLuckRoyaleSpin(
                banner = RoyaleBannerId.DIAMOND_ROYALE,
                isMulti = true,
                profile = latestProfile,
                existingInventory = updatedInv.associateBy { it.itemId }
            )
        }
        assertEquals(11, spinOutcome.items.size)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.equipItem(storeItem)
            val thermalScope = GameCatalog.byId("att_thermal_4x")!!
            val kabutoHat = GameCatalog.byId("hat_samurai_kabuto")!!
            val amberOutfit = GameCatalog.byId("outfit_amber_jacket")!!
            val flameWings = GameCatalog.byId("acc_wings_blaze")!!
            val medkitCrate = GameCatalog.byId("cons_medkit_pack")!!

            val invNow = repository.observeInventory(uid).first().associateBy { it.itemId }
            repository.purchaseStoreItem(thermalScope, latestProfile, invNow)
            repository.equipItem(thermalScope)
            repository.purchaseStoreItem(kabutoHat, latestProfile, invNow)
            repository.equipItem(kabutoHat)
            repository.purchaseStoreItem(amberOutfit, latestProfile, invNow)
            repository.equipItem(amberOutfit)
            repository.purchaseStoreItem(flameWings, latestProfile, invNow)
            repository.equipItem(flameWings)
            repository.purchaseStoreItem(medkitCrate, latestProfile, invNow)

            repository.recordMatchResult(
                mode = "battle_royale",
                placement = 1,
                kills = 7,
                headshots = 3,
                damageDealt = 1480,
                survivalSeconds = 160,
                profile = latestProfile
            )
        }

        val finalProfile = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeProfile(uid).first {
                it != null && it.equippedHatId == "hat_samurai_kabuto" &&
                    it.equippedOutfitId == "outfit_amber_jacket" &&
                    it.equippedAccessoryId == "acc_wings_blaze" &&
                    it.equippedAttachmentId == "att_thermal_4x"
            }!!
        }
        assertEquals("hat_samurai_kabuto", finalProfile.equippedHatId)

        val matches = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeMatchHistory(uid).first { it.isNotEmpty() }
        }
        assertTrue(matches.first().booyah)
    }

    @Test
    fun crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser("alice_iso_${UUID.randomUUID()}@test.com")
        val aliceRepo = GameRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.ensureUserProfile("AliceIso")
        }

        signInTestUser("bob_iso_${UUID.randomUUID()}@test.com")
        val bobRepo = GameRepository(firestore, auth)
        var denied = false
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeProfile(aliceUid).first()
            }
        } catch (e: Exception) {
            val fsError = e.unwrapFirestoreException()
            if (fsError?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                denied = true
            }
        }
        assertTrue("Expected PERMISSION_DENIED on cross-user profile read", denied)
    }

    @Test
    fun unauthenticatedAccess_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = GameRepository(firestore, auth)
        var denied = false
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeInventory("some_uid").first()
            }
        } catch (e: Exception) {
            val fsError = e.unwrapFirestoreException()
            if (fsError?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                denied = true
            }
        }
        assertTrue("Expected PERMISSION_DENIED on unauthenticated inventory read", denied)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 8000L
        const val FLOW_TIMEOUT_MS = 5000L
    }
}
