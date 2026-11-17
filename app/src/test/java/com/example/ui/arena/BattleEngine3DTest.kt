package com.example.ui.arena

import com.example.data.model.BattleMapId
import com.example.data.model.OwnedItem
import com.example.data.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BattleEngine3DTest {

    @Test
    fun battleEngine3D_handlesCameraJumpScopeFiringAndSnapshot() {
        val profile = UserProfile(
            userId = "test_uid",
            callsign = "Ghost3D",
            equippedCharacterId = "char_valkyrie",
            equippedHatId = "hat_samurai_kabuto",
            equippedOutfitId = "outfit_amber_jacket",
            equippedAccessoryId = "acc_wings_blaze",
            equippedAttachmentId = "att_thermal_4x",
            equippedRifleSkinId = "wpn_draco_akm"
        )
        val inventory = mapOf(
            "cons_medkit_pack" to OwnedItem(itemId = "cons_medkit_pack", quantity = 1),
            "cons_gloo_supply" to OwnedItem(itemId = "cons_gloo_supply", quantity = 1),
            "cons_kevlar_kit" to OwnedItem(itemId = "cons_kevlar_kit", quantity = 1)
        )

        val engine = BattleEngine("battle_royale", profile, inventory)

        // Consumables & attachment bonuses applied
        assertEquals(5, engine.medkits)
        assertEquals(6, engine.glooCharges)
        assertEquals(100f, engine.player.shield, 0.01f)

        // Camera rotation & pitch clamping
        val initialYaw = engine.cameraYaw
        engine.rotateCamera(100f, -50f)
        assertTrue(engine.cameraYaw > initialYaw)
        engine.rotateCamera(0f, -10000f)
        assertTrue(engine.cameraPitch <= 0.42f)

        // Scope ADS & Crouch toggles
        assertFalse(engine.isScoped)
        engine.toggleScope()
        assertTrue(engine.isScoped)

        assertFalse(engine.isCrouched)
        engine.toggleCrouch()
        assertTrue(engine.isCrouched)

        // Jump clears crouch & triggers vertical velocity
        engine.jump()
        assertFalse(engine.isCrouched)
        engine.step(dt = 0.04f, joyX = 0f, joyY = -1f, isFiring = true)
        assertTrue(engine.player.z > 0f)
        assertTrue(engine.player.isMoving)

        val snapshot = engine.buildRenderSnapshot()
        assertNotNull(snapshot.player)
        assertTrue(snapshot.hasThermalScope)
        assertEquals("hat_samurai_kabuto", snapshot.hatId)
        assertEquals("acc_wings_blaze", snapshot.accessoryId)
        assertTrue(snapshot.bullets.isNotEmpty())
        assertTrue(snapshot.bullets.first().z > 0f)
        assertTrue(engine.compassBearingDegrees() in 0..359)
    }

    @Test
    fun battleEngine3D_supportsDistinctMapsAndVehicles() {
        val profile = UserProfile(userId = "map_tester", callsign = "Nomad")
        val erangel = BattleEngine("battle_royale", profile, mapId = BattleMapId.ERANGEL)
        val miramar = BattleEngine("battle_royale", profile, mapId = BattleMapId.MIRAMAR)
        val sanhok = BattleEngine("clash_blitz", profile, mapId = BattleMapId.SANHOK)

        assertEquals(BattleMapId.ERANGEL, erangel.buildRenderSnapshot().mapId)
        assertEquals(BattleMapId.MIRAMAR, miramar.buildRenderSnapshot().mapId)
        assertEquals(BattleMapId.SANHOK, sanhok.buildRenderSnapshot().mapId)

        assertEquals(1000, erangel.mapSizeMeters)
        assertEquals(10000f, erangel.mapSize, 0.1f)
        assertEquals(500, sanhok.mapSizeMeters)
        assertEquals(5000f, sanhok.mapSize, 0.1f)

        assertTrue(erangel.obstacles.size >= 20)
        assertTrue(erangel.obstacles.any { it.structureType == StructureType.CLOCK_TOWER })
        assertTrue(miramar.obstacles.any { it.structureType == StructureType.OIL_DERRICK })
        assertTrue(sanhok.obstacles.any { it.structureType == StructureType.TEMPLE_RUIN })

        assertTrue(erangel.vehicles.size >= 12)
        assertTrue(miramar.vehicles.any { it.type == VehicleType.DESERT_BUGGY })
        assertNotEquals(erangel.vehicles.first().type, miramar.vehicles.first().type)
    }

    @Test
    fun battleEngine3D_drivesAndExitsVehiclesSmoothly() {
        val profile = UserProfile(userId = "driver_uid", callsign = "Driver")
        val engine = BattleEngine("battle_royale", profile, mapId = BattleMapId.ERANGEL)

        val veh = engine.vehicles.first()
        val startVehX = veh.x
        val startVehY = veh.y

        // Walk next to the vehicle and mount it
        engine.player.x = veh.x - 25f
        engine.player.y = veh.y
        assertTrue(engine.canEnterOrExitVehicle())
        assertTrue(engine.toggleDriveVehicle())
        assertEquals(0, engine.drivingVehicleIndex)

        // Accelerate forward and steer slightly
        repeat(6) {
            engine.step(dt = 0.04f, joyX = 0.25f, joyY = -1f, isFiring = false)
        }

        val snap = engine.buildRenderSnapshot()
        assertTrue(snap.isDriving)
        assertTrue(snap.vehicleSpeedKmh > 0)
        assertTrue(veh.x != startVehX || veh.y != startVehY)

        // Exit vehicle cleanly
        assertTrue(engine.toggleDriveVehicle())
        assertEquals(null, engine.drivingVehicleIndex)
        assertFalse(engine.buildRenderSnapshot().isDriving)
    }

    @Test
    fun battleEngine3D_allowsWalkingThroughHouseDoorwayAndBlocksWalls() {
        val profile = UserProfile(userId = "breacher_uid", callsign = "Breacher")
        val engine = BattleEngine("battle_royale", profile, mapId = BattleMapId.ERANGEL)

        val house = engine.obstacles.first { it.structureType == StructureType.COMPOUND_HOUSE }
        val doorMidX = house.x + house.w * 0.5f
        val doorFrontY = house.y + house.h

        // Doorway center should allow unobstructed passage inside
        assertFalse(engine.collidesWithStructure(house, doorMidX, doorFrontY, 18f))

        // Left wall corner should block passage
        assertTrue(engine.collidesWithStructure(house, house.x + 12f, doorFrontY, 18f))

        // Standing inside house reports indoor status & interior loot exists
        engine.player.x = doorMidX
        engine.player.y = house.y + house.h * 0.5f
        assertTrue(engine.isPlayerIndoors())
        assertTrue(engine.buildRenderSnapshot().isPlayerIndoors)
        assertTrue(
            engine.lootDrops.any { drop ->
                engine.isPositionInsideBuilding(house, drop.x, drop.y)
            }
        )
    }

    @Test
    fun bespokePerItemMeshesAndSnapshotIds_areGeneratedAndSwitchedCleanly() {
        val meshes = listOf(
            Arena3DMeshes.createCurvedHornMesh(),
            Arena3DMeshes.createSodeArmorPlateMesh(),
            Arena3DMeshes.createKatanaScabbardMesh(),
            Arena3DMeshes.createHexCrystalMesh(),
            Arena3DMeshes.createBullpupChassisMesh(),
            Arena3DMeshes.createHeavyPauldronMesh(),
            Arena3DMeshes.createRifleReceiverMesh(),
            Arena3DMeshes.createCurvedMagMesh()
        )
        meshes.forEach { mesh ->
            assertTrue("Expected high-poly vertex count >= 140, got ${mesh.vertexCount}", mesh.vertexCount >= 140)
            assertEquals(mesh.vertexCount * 6, mesh.buffer.capacity())
        }

        val terrain = Arena3DMeshes.createMapTerrainMesh(BattleMapId.MIRAMAR, 10000f)
        assertEquals(64 * 64 * 6, terrain.vertexCount)

        val profile = UserProfile(
            userId = "mesh_uid",
            callsign = "MeshMaster",
            equippedCharacterId = "char_inferno_ronin",
            equippedOutfitId = "outfit_amber_jacket",
            equippedHatId = "hat_samurai_kabuto",
            equippedAccessoryId = "acc_wings_blaze",
            equippedRifleSkinId = "wpn_groza_blaze",
            equippedSmgSkinId = "wpn_vector_cyber",
            equippedAttachmentId = "att_drum_mag"
        )
        val engine = BattleEngine("battle_royale", profile, emptyMap(), BattleMapId.SANHOK)
        val rifleSnap = engine.buildRenderSnapshot()
        assertEquals("char_inferno_ronin", rifleSnap.characterId)
        assertEquals("outfit_amber_jacket", rifleSnap.outfitId)
        assertEquals("hat_samurai_kabuto", rifleSnap.hatId)
        assertEquals("acc_wings_blaze", rifleSnap.accessoryId)
        assertEquals("wpn_groza_blaze", rifleSnap.weaponId)
        assertEquals("att_drum_mag", rifleSnap.attachmentId)

        engine.switchWeapon()
        val smgSnap = engine.buildRenderSnapshot()
        assertEquals("wpn_vector_cyber", smgSnap.weaponId)
        assertFalse(smgSnap.activeWeaponIsRifle)
    }
}
