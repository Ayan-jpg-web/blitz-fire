package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.BattleMapId
import com.example.data.model.CustomHudConfig
import com.example.data.model.GameCatalog
import com.example.data.model.HudElementConfig
import com.example.data.model.HudElementId
import com.example.data.model.HudLayoutPreset
import com.example.data.model.ItemCategory
import com.example.data.model.SensitivityPreset
import com.example.data.model.SensitivitySettings
import com.example.data.model.UserProfile
import com.example.data.repository.ControlSettingsRepository
import com.example.data.repository.GameEconomyEngine
import com.example.data.repository.LocalGuestRepository
import com.example.ui.arena.BattleEngine
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
class StoreAndControlsTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("blitzfire_guest_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("blitzfire_guest_vault", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("blitzfire_hud_sensitivity_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun weaponAndBundleCatalog_isValidWithSavingsAndContents() {
        val bundles = GameCatalog.allItems.filter { it.category == ItemCategory.BUNDLE }
        assertTrue(bundles.size >= 4)
        bundles.forEach { bundle ->
            val contents = GameCatalog.bundleContents(bundle)
            assertTrue("Bundle ${bundle.id} must have >= 2 items", contents.size >= 2)
            assertEquals(bundle.bundleItemIds.size, contents.size)
            assertTrue("Bundle ${bundle.id} should show originalPrice > price", bundle.originalPrice > bundle.price)
        }
        assertNotNull(GameCatalog.byId("wpn_groza_blaze"))
        assertNotNull(GameCatalog.byId("wpn_vector_cyber"))
    }

    @Test
    fun bundlePurchase_unlocksAndEquipsAllConstituents_andRejectsLowBalance() = runBlocking {
        val repo = LocalGuestRepository(context)
        val initial = repo.ensureUserProfile(null)
        val bundle = GameCatalog.byId("bundle_inferno_shogun")!!
        val invMap = repo.observeInventory(initial.userId).first().associateBy { it.itemId }

        assertTrue(GameEconomyEngine.hasSufficientBalance(initial, bundle))
        repo.purchaseStoreItem(bundle, initial, invMap)
        repo.equipItem(bundle)

        val updatedInv = repo.observeInventory(initial.userId).first().associateBy { it.itemId }
        assertTrue(updatedInv.containsKey(bundle.id))
        bundle.bundleItemIds.forEach { childId ->
            assertTrue("Expected bundle child $childId unlocked", updatedInv.containsKey(childId))
        }

        val updatedProfile = repo.observeProfile(initial.userId).first()!!
        assertEquals("char_inferno_ronin", updatedProfile.equippedCharacterId)
        assertEquals("wpn_draco_akm", updatedProfile.equippedRifleSkinId)
        assertEquals("hat_samurai_kabuto", updatedProfile.equippedHatId)
        assertEquals("gloo_dragon_scale", updatedProfile.equippedGlooSkinId)

        // Low currency balance rejection check
        val brokeProfile = UserProfile(userId = initial.userId, diamonds = 10L, gold = 50L)
        val cyberBundle = GameCatalog.byId("bundle_cyber_valkyrie")!!
        assertFalse(GameEconomyEngine.hasSufficientBalance(brokeProfile, cyberBundle))

        var rejected = false
        try {
            repo.purchaseStoreItem(cyberBundle, brokeProfile, updatedInv)
        } catch (_: IllegalStateException) {
            rejected = true
        }
        assertTrue("Insufficient currency balance must reject purchase", rejected)
    }

    @Test
    fun customHudAndSensitivity_persistAndDriveBattleEngine() {
        val repo = ControlSettingsRepository(context)
        assertEquals(HudLayoutPreset.TWO_FINGER, repo.hudConfig.value.activePreset)

        // Update preset and individual HUD button layout
        val customFire = HudElementConfig(xFraction = 0.22f, yFraction = 0.28f, scale = 1.40f, alpha = 0.72f)
        val customElements = CustomHudConfig.defaultHudMap(HudLayoutPreset.THREE_FINGER).toMutableMap()
        customElements[HudElementId.FIRE_BUTTON] = customFire
        val savedConfig = CustomHudConfig(activePreset = HudLayoutPreset.THREE_FINGER, elements = customElements)
        repo.saveHudConfig(savedConfig)

        val sens = SensitivitySettings(
            preset = SensitivityPreset.PRO_CLAW,
            generalCameraSens = 2.0f,
            redDotAdsSens = 0.5f,
            thermal4xSens = 0.4f,
            fireDragAimSens = 1.4f,
            invertY = true
        )
        repo.saveSensitivity(sens)

        // Reload in fresh repository instance to verify persistence
        val reloaded = ControlSettingsRepository(context)
        assertEquals(HudLayoutPreset.THREE_FINGER, reloaded.hudConfig.value.activePreset)
        val fireCfg = reloaded.hudConfig.value.configFor(HudElementId.FIRE_BUTTON)
        assertEquals(0.22f, fireCfg.xFraction, 0.001f)
        assertEquals(0.28f, fireCfg.yFraction, 0.001f)
        assertEquals(1.40f, fireCfg.scale, 0.001f)
        assertEquals(0.72f, fireCfg.alpha, 0.001f)

        val loadedSens = reloaded.sensitivity.value
        assertEquals(SensitivityPreset.PRO_CLAW, loadedSens.preset)
        assertEquals(2.0f, loadedSens.generalCameraSens, 0.001f)
        assertEquals(1.4f, loadedSens.fireDragAimSens, 0.001f)
        assertTrue(loadedSens.invertY)

        // Verify BattleEngine honors general vs ADS vs invertY sensitivity
        val profile = UserProfile(equippedAttachmentId = "att_thermal_4x")
        val engine = BattleEngine("battle_royale", profile, emptyMap(), BattleMapId.ERANGEL)
        engine.updateSensitivity(loadedSens)

        val startYaw = engine.cameraYaw
        val startPitch = engine.cameraPitch
        engine.rotateCamera(deltaScreenX = 100f, deltaScreenY = 20f)
        val hipYawDelta = engine.cameraYaw - startYaw
        val hipPitchDelta = engine.cameraPitch - startPitch
        assertTrue("High general sens should rotate yaw significantly", hipYawDelta > 0.6f)
        assertTrue("InvertY=true should flip pitch up on positive screenY", hipPitchDelta > 0f)

        // Scoped thermal sensitivity (0.4x multiplier) should be slower than hip-fire
        engine.toggleScope()
        val scopedStartYaw = engine.cameraYaw
        engine.rotateCamera(deltaScreenX = 100f, deltaScreenY = 0f)
        val scopedYawDelta = engine.cameraYaw - scopedStartYaw
        assertTrue("Scoped thermal aim ($scopedYawDelta) must be finer than hip-fire ($hipYawDelta)", scopedYawDelta < hipYawDelta * 0.3f)
    }
}
