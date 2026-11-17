package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.GameCatalog
import com.example.data.model.RoyaleBannerId
import com.example.data.repository.GameEconomyEngine
import com.example.data.repository.GuestSessionManager
import com.example.data.repository.LocalGuestRepository
import com.example.ui.components.DevWatermarkPrefs
import com.example.ui.components.readAppSignatureInfo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GuestAndSignatureTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("blitzfire_guest_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("blitzfire_guest_vault", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("blitzfire_dev_watermark", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun guestMode_purchasesSpinsEquipsAndPersistsLocally() = runBlocking {
        assertFalse(GuestSessionManager.isGuestActive(context))
        GuestSessionManager.setGuestActive(context, true)
        assertTrue(GuestSessionManager.isGuestActive(context))

        val repo = LocalGuestRepository(context)
        val initial = repo.ensureUserProfile(null)
        assertEquals(LocalGuestRepository.GUEST_UID, initial.userId)
        assertEquals(GameEconomyEngine.UNLIMITED_BASELINE, initial.diamonds)
        assertEquals(GameEconomyEngine.UNLIMITED_BASELINE, initial.gold)

        val item = GameCatalog.byId("char_valkyrie")!!
        val invMap = repo.observeInventory(initial.userId).first().associateBy { it.itemId }
        repo.purchaseStoreItem(item, initial, invMap)
        repo.equipItem(item)

        val afterPurchase = repo.observeProfile(initial.userId).first()!!
        assertEquals("char_valkyrie", afterPurchase.equippedCharacterId)
        assertTrue(repo.observeInventory(initial.userId).first().any { it.itemId == "char_valkyrie" })

        val outcome = repo.performLuckRoyaleSpin(
            banner = RoyaleBannerId.DIAMOND_ROYALE,
            isMulti = true,
            profile = afterPurchase,
            existingInventory = repo.observeInventory(initial.userId).first().associateBy { it.itemId }
        )
        assertEquals(RoyaleBannerId.DIAMOND_ROYALE.multiCount, outcome.items.size)
        assertTrue(outcome.hitPityJackpot || outcome.newLuckValue > 0)

        val match = repo.recordMatchResult(
            mode = "battle_royale",
            placement = 1,
            kills = 5,
            headshots = 3,
            damageDealt = 920,
            survivalSeconds = 185,
            profile = repo.observeProfile(initial.userId).first()!!
        )
        assertTrue(match.booyah)

        // Verify persistence across a new repository instance
        val reloadedRepo = LocalGuestRepository(context)
        val reloadedProfile = reloadedRepo.observeProfile(LocalGuestRepository.GUEST_UID).first()
        assertNotNull(reloadedProfile)
        assertEquals(1, reloadedProfile!!.booyahs)
        assertEquals("char_valkyrie", reloadedProfile.equippedCharacterId)
    }

    @Test
    fun appSignatureAndDevWatermarkPrefsWork() {
        val sig = readAppSignatureInfo(context)
        assertEquals("BLITZ FIRE", sig.appName)
        assertEquals(BuildConfig.APPLICATION_ID, sig.packageId)
        assertTrue(sig.sha256Full.isNotBlank())
        assertTrue(sig.sha256Short.isNotBlank())

        assertTrue(DevWatermarkPrefs.isEnabled(context))
        DevWatermarkPrefs.setEnabled(context, false)
        assertFalse(DevWatermarkPrefs.isEnabled(context))

        DevWatermarkPrefs.setLabel(context, "SHAHID DEV")
        assertEquals("SHAHID DEV", DevWatermarkPrefs.getLabel(context))
    }
}
