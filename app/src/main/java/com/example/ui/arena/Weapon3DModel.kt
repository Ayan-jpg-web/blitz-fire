package com.example.ui.arena

import android.opengl.GLES30
import android.opengl.Matrix
import java.nio.FloatBuffer
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-Polygon Bespoke 3D Firearm & Attachment Geometry Builder.
 * Every weapon ID and attachment ID renders its own signature silhouette,
 * receiver architecture, magazine geometry, stock, barrel shroud, and optics.
 */
internal object Weapon3DModel {

    fun drawWeapon3D(
        meshes: Es3MeshSet,
        shader: Es3ShaderProgram,
        vpMatrix: FloatArray,
        tempModelMatrix: FloatArray,
        tempMvpMatrix: FloatArray,
        weaponId: String,
        weaponColorHex: Long,
        attachmentId: String,
        worldX: Float,
        worldY: Float,
        worldZ: Float,
        rotYDeg: Float,
        scale: Float = 1f,
        isPlayer: Boolean = true,
        isMuzzleFlashing: Boolean = false,
        elapsed: Float = 0f,
        pitchDeg: Float = 0f,
        rollDeg: Float = 0f
    ) {
        val weaponRoot = FloatArray(16)
        Matrix.setIdentityM(weaponRoot, 0)
        Matrix.translateM(weaponRoot, 0, worldX, worldY, worldZ)
        if (rotYDeg != 0f) Matrix.rotateM(weaponRoot, 0, rotYDeg, 0f, 1f, 0f)
        if (pitchDeg != 0f) Matrix.rotateM(weaponRoot, 0, pitchDeg, 0f, 0f, 1f)
        if (rollDeg != 0f) Matrix.rotateM(weaponRoot, 0, rollDeg, 1f, 0f, 0f)
        if (scale != 1f) Matrix.scaleM(weaponRoot, 0, scale, scale, scale)

        fun emitLocal(
            buf: FloatBuffer, verts: Int,
            lx: Float, ly: Float, lz: Float,
            sx: Float, sy: Float, sz: Float,
            rxDeg: Float = 0f, ryDeg: Float = 0f, rzDeg: Float = 0f,
            r: Float, g: Float, b: Float, a: Float = 1f,
            emissive: Float = 0f, mat: Int = Arena3DMeshes.MAT_STANDARD
        ) {
            Matrix.setIdentityM(tempModelMatrix, 0)
            Matrix.translateM(tempModelMatrix, 0, lx, ly, lz)
            if (ryDeg != 0f) Matrix.rotateM(tempModelMatrix, 0, ryDeg, 0f, 1f, 0f)
            if (rxDeg != 0f) Matrix.rotateM(tempModelMatrix, 0, rxDeg, 1f, 0f, 0f)
            if (rzDeg != 0f) Matrix.rotateM(tempModelMatrix, 0, rzDeg, 0f, 0f, 1f)
            Matrix.scaleM(tempModelMatrix, 0, sx, sy, sz)

            val combinedModel = FloatArray(16)
            Matrix.multiplyMM(combinedModel, 0, weaponRoot, 0, tempModelMatrix, 0)
            Matrix.multiplyMM(tempMvpMatrix, 0, vpMatrix, 0, combinedModel, 0)

            GLES30.glUniformMatrix4fv(shader.uMVPMatrixLoc, 1, false, tempMvpMatrix, 0)
            GLES30.glUniformMatrix4fv(shader.uModelMatrixLoc, 1, false, combinedModel, 0)
            GLES30.glUniform4f(shader.uColorLoc, r, g, b, a)
            GLES30.glUniform1f(shader.uEmissiveLoc, emissive)
            GLES30.glUniform1i(shader.uMaterialModeLoc, mat)
            Operative3DModel.bindAndDraw(buf, verts)
        }

        fun emitHex(
            buf: FloatBuffer, verts: Int,
            lx: Float, ly: Float, lz: Float,
            sx: Float, sy: Float, sz: Float,
            hex: Long,
            rxDeg: Float = 0f, ryDeg: Float = 0f, rzDeg: Float = 0f,
            a: Float = 1f, emissive: Float = 0f, mat: Int = Arena3DMeshes.MAT_STANDARD
        ) {
            val r = ((hex shr 16) and 0xFF) / 255f
            val g = ((hex shr 8) and 0xFF) / 255f
            val b = (hex and 0xFF) / 255f
            emitLocal(buf, verts, lx, ly, lz, sx, sy, sz, rxDeg, ryDeg, rzDeg, r, g, b, a, emissive, mat)
        }

        val hasDrumMag = attachmentId == "att_drum_mag"
        val hasCryoSuppressor = attachmentId == "att_cryo_suppressor"
        val hasThermalScope = attachmentId == "att_thermal_4x"
        val laserHex = if (isPlayer) 0xFF10E88AL else 0xFFFF3355L
        val pulse = 0.72f + 0.28f * sin(elapsed * 5.2f)

        var muzzleX = 16.4f

        when (weaponId) {
            // 1. EVO DRACO AKM — Dragon-Winged Mythic AKM with Crimson Ember Core & Bone Horns
            "wpn_draco_akm" -> {
                // Spiked Skeleton Dragon Stock
                emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, -7.8f, -0.5f, 0f, 8.2f, 3.8f, 2.2f, 0xFF1E1B24, mat = Arena3DMeshes.MAT_ARMOR_WEAVE)
                emitHex(meshes.curvedHorn.buffer, meshes.curvedHorn.vertexCount, -10.8f, 0.5f, 0f, 3.2f, 4.6f, 2.2f, weaponColorHex, rzDeg = 42f, emissive = 0.55f, mat = Arena3DMeshes.MAT_METALLIC)
                // Sculpted Dragon Spine Receiver + Glowing Molten Core
                emitHex(meshes.rifleReceiver.buffer, meshes.rifleReceiver.vertexCount, 0f, 0f, 0f, 12.6f, 4.1f, 3.5f, 0xFF1A161F, mat = Arena3DMeshes.MAT_METALLIC)
                emitHex(meshes.sphere, Arena3DMeshes.SPHERE_VERTEX_COUNT, 0.2f, 0.3f, 0f, 3.2f, 2.2f, 3.9f, weaponColorHex, emissive = 0.92f * pulse)
                emitHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, 0.4f, 0.6f, 1.3f, 1.1f, 4.6f, 1.1f, 0xFFF5B400L, rzDeg = 90f, emissive = 0.45f, mat = Arena3DMeshes.MAT_METALLIC)
                // Twin Swept Flaming Dragon Wings on Receiver Sides
                emitHex(meshes.wing, Arena3DMeshes.SWEPT_WING_VERTEX_COUNT, 2.4f, 1.4f, 1.7f, 7.6f, 5.2f, 6.4f, weaponColorHex, rxDeg = -28f, ryDeg = 18f, emissive = 0.72f, mat = Arena3DMeshes.MAT_METALLIC)
                emitHex(meshes.wing, Arena3DMeshes.SWEPT_WING_VERTEX_COUNT, 2.4f, 1.4f, -1.7f, 7.6f, 5.2f, -6.4f, weaponColorHex, rxDeg = 28f, ryDeg = -18f, emissive = 0.72f, mat = Arena3DMeshes.MAT_METALLIC)
                // Grip + Trigger Guard
                emitHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, -2.5f, -2.6f, 0f, 2.0f, 4.6f, 2.2f, 0xFF141218, rzDeg = 22f, mat = Arena3DMeshes.MAT_ARMOR_WEAVE)
                emitHex(meshes.torus, Arena3DMeshes.TORUS_VERTEX_COUNT, -0.6f, -1.6f, 0f, 2.2f, 2.2f, 1.1f, 0xFFF5B400L, mat = Arena3DMeshes.MAT_METALLIC)
                // Curved Dragon-Ribbed AKM Banana Mag (or Beta Drum)
                if (hasDrumMag) {
                    drawTwinDrumMag(meshes, ::emitHex, 2.0f, -2.6f, weaponColorHex)
                } else {
                    emitHex(meshes.curvedMag.buffer, meshes.curvedMag.vertexCount, 2.0f, -3.9f, 0f, 3.6f, 6.8f, 2.5f, weaponColorHex, emissive = 0.35f, mat = Arena3DMeshes.MAT_METALLIC)
                }
                // Vented Shroud + Crimson M-LOK Rings + Dragon Horn Front Sight + Fluted Barrel
                emitHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, 6.2f, 0.1f, 0f, 2.6f, 7.4f, 2.6f, 0xFF1B1922, rzDeg = 90f, mat = Arena3DMeshes.MAT_ARMOR_WEAVE)
                for (i in 0 until 4) {
                    emitHex(meshes.torus, Arena3DMeshes.TORUS_VERTEX_COUNT, 4.0f + i * 1.6f, 0.1f, 0f, 3.0f, 3.0f, 3.0f, weaponColorHex, rxDeg = 90f, rzDeg = 90f, emissive = 0.65f)
                }
                emitHex(meshes.curvedHorn.buffer, meshes.curvedHorn.vertexCount, 12.2f, 1.0f, 0f, 2.4f, 3.8f, 1.8f, weaponColorHex, rzDeg = -18f, emissive = 0.65f)
                emitHex(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, 12.2f, 0.3f, 0f, 1.6f, 11.4f, 1.6f, 0xFF334155, rzDeg = 90f, mat = Arena3DMeshes.MAT_METALLIC)
                emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, 17.5f, 0.3f, 0f, 2.6f, 2.2f, 2.2f, weaponColorHex, emissive = 0.45f, mat = Arena3DMeshes.MAT_METALLIC)
                muzzleX = 18.2f
            }

            // 2. GLACIER M4A1 — AR-15 Split Receiver + Crystalline Ice Shards + Cryo Buffer Stock
            "wpn_m4_frost" -> {
                // AR Buffer Tube + Crane Stock
                emitHex(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, -5.6f, 0.2f, 0f, 1.6f, 6.0f, 1.6f, 0xFF94A3B8, rzDeg = 90f, mat = Arena3DMeshes.MAT_METALLIC)
                emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, -8.2f, -0.4f, 0f, 5.8f, 4.2f, 2.4f, 0xFF1E293B, mat = Arena3DMeshes.MAT_ARMOR_WEAVE)
                // AR-15 Receiver with Ice-Coated Armor Trim
                emitHex(meshes.rifleReceiver.buffer, meshes.rifleReceiver.vertexCount, 0f, 0f, 0f, 11.4f, 3.6f, 3.1f, 0xFF164E63, mat = Arena3DMeshes.MAT_METALLIC)
                emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, 0.2f, 1.7f, 0f, 10.6f, 0.9f, 2.0f, 0xFFE0F2FE, mat = Arena3DMeshes.MAT_GLASS)
                // Grip & STANAG Straight-ish Mag or Twin Drum
                emitHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, -2.3f, -2.4f, 0f, 1.8f, 4.3f, 2.0f, 0xFF0F172A, rzDeg = 20f)
                emitHex(meshes.torus, Arena3DMeshes.TORUS_VERTEX_COUNT, -0.6f, -1.5f, 0f, 1.9f, 1.9f, 0.9f, 0xFF94A3B8, mat = Arena3DMeshes.MAT_METALLIC)
                if (hasDrumMag) {
                    drawTwinDrumMag(meshes, ::emitHex, 1.8f, -2.5f, weaponColorHex)
                } else {
                    emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, 1.8f, -3.5f, 0f, 2.9f, 5.6f, 2.1f, 0xFF0EA5E9, rzDeg = 8f, mat = Arena3DMeshes.MAT_GLASS)
                }
                // Quad-Rail M4 Handguard + 5 Jagged Glowing Cryo Ice Crystals!
                emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, 6.4f, 0.1f, 0f, 7.6f, 2.6f, 2.6f, 0xFF0C4A6E, mat = Arena3DMeshes.MAT_METALLIC)
                val iceOffsets = arrayOf(
                    floatArrayOf(-1.2f, 1.5f, 1.2f, 32f, -24f),
                    floatArrayOf(4.2f, 1.4f, -1.2f, -35f, -38f),
                    floatArrayOf(6.8f, 1.6f, 1.1f, 28f, -45f),
                    floatArrayOf(8.8f, -0.9f, -1.2f, -40f, -125f),
                    floatArrayOf(10.6f, 1.1f, 0f, 0f, -52f)
                )
                iceOffsets.forEach { ice ->
                    emitHex(meshes.hexCrystal.buffer, meshes.hexCrystal.vertexCount, ice[0], ice[1], ice[2], 2.0f, 4.4f, 2.0f, 0xFF7DD3FC, rxDeg = ice[3], rzDeg = ice[4], a = 0.92f, emissive = 0.72f, mat = Arena3DMeshes.MAT_GLASS)
                }
                emitHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, 6.2f, -2.1f, 0f, 1.4f, 3.4f, 1.4f, 0xFF38BDF8, mat = Arena3DMeshes.MAT_GLASS)
                emitHex(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, 11.8f, 0.25f, 0f, 1.35f, 9.6f, 1.35f, 0xFFBAE6FD, rzDeg = 90f, mat = Arena3DMeshes.MAT_METALLIC)
                emitHex(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, 16.4f, 0.25f, 0f, 2.0f, 2.2f, 2.0f, 0xFF00E5FF, rzDeg = 90f, emissive = 0.65f, mat = Arena3DMeshes.MAT_GLASS)
                muzzleX = 17.0f
            }

            // 3. TITAN SCAR-L — Monolithic FN Upper Chassis + Iconic Gold Boot Stock + Heavy Fluted Barrel
            "wpn_scar_titan" -> {
                // Iconic SCAR "Ugg Boot" Adjustable Gold/Polymer Stock
                emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, -7.2f, -0.3f, 0f, 7.2f, 3.4f, 2.4f, 0xFF78350F, mat = Arena3DMeshes.MAT_ARMOR_WEAVE)
                emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, -9.8f, -1.2f, 0f, 2.6f, 5.2f, 2.5f, 0xFFF5B400, mat = Arena3DMeshes.MAT_METALLIC)
                emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, -6.8f, 1.3f, 0f, 5.0f, 1.2f, 2.2f, 0xFFF5B400, mat = Arena3DMeshes.MAT_METALLIC)
                // Monolithic Gold-FDE SCAR Receiver + Full Continuous Picatinny Top Rail
                emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, 2.2f, 0.4f, 0f, 16.2f, 3.6f, 3.2f, weaponColorHex, emissive = 0.24f, mat = Arena3DMeshes.MAT_METALLIC)
                emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, 2.2f, 2.1f, 0f, 15.6f, 0.8f, 2.2f, 0xFF1F2937, mat = Arena3DMeshes.MAT_METALLIC)
                // Side Charging Handle & Gold Trim
                emitHex(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, 3.4f, 0.8f, 1.9f, 0.8f, 1.4f, 0.8f, 0xFFFDE047, rxDeg = 90f, mat = Arena3DMeshes.MAT_METALLIC)
                emitHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, -2.4f, -2.5f, 0f, 1.9f, 4.4f, 2.1f, 0xFF1E293B, rzDeg = 22f)
                if (hasDrumMag) {
                    drawTwinDrumMag(meshes, ::emitHex, 1.8f, -2.6f, weaponColorHex)
                } else {
                    emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, 1.8f, -3.5f, 0f, 3.0f, 5.8f, 2.2f, 0xFFD97706, rzDeg = 10f, mat = Arena3DMeshes.MAT_METALLIC)
                }
                emitHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, 6.2f, -2.2f, 0f, 1.5f, 3.4f, 1.5f, 0xFF0F172A)
                emitHex(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, 13.0f, 0.4f, 0f, 1.5f, 9.2f, 1.5f, 0xFF475569, rzDeg = 90f, mat = Arena3DMeshes.MAT_METALLIC)
                emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, 17.2f, 0.4f, 0f, 2.4f, 2.1f, 2.1f, 0xFFF5B400, emissive = 0.35f, mat = Arena3DMeshes.MAT_METALLIC)
                muzzleX = 17.8f
            }

            // 4. SOLARIS GROZA-X — True Bullpup Silhouette (Magazine Behind Grip!) + Integrated Carry Handle
            "wpn_groza_blaze" -> {
                // Sculpted Bullpup Chassis with Rear Buttstock + Molten Core Vents
                emitHex(meshes.bullpupChassis.buffer, meshes.bullpupChassis.vertexCount, -1.6f, -0.2f, 0f, 14.8f, 5.4f, 3.4f, weaponColorHex, emissive = 0.28f, mat = Arena3DMeshes.MAT_METALLIC)
                // Integrated Upper Carry-Handle Rail Bridge
                emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, 0.8f, 2.4f, 0f, 9.8f, 1.2f, 2.0f, 0xFF1F2937, mat = Arena3DMeshes.MAT_ARMOR_WEAVE)
                emitHex(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, -3.2f, 1.4f, 0f, 1.4f, 2.2f, 1.6f, 0xFF334155, mat = Arena3DMeshes.MAT_METALLIC)
                emitHex(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, 4.8f, 1.4f, 0f, 1.4f, 2.2f, 1.6f, 0xFF334155, mat = Arena3DMeshes.MAT_METALLIC)
                // Molten Solar Cooling Torus Rings along Barrel Shroud
                for (i in 0 until 3) {
                    emitHex(meshes.torus, Arena3DMeshes.TORUS_VERTEX_COUNT, 6.0f + i * 2.0f, 0.2f, 0f, 2.8f, 2.8f, 2.8f, 0xFFFF6B1A, rxDeg = 90f, rzDeg = 90f, emissive = 0.78f * pulse)
                }
                // Forward Pistol Grip (Bullpup Layout: Grip Ahead of Magazine!)
                emitHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, 3.2f, -2.6f, 0f, 1.8f, 4.4f, 2.0f, 0xFF111827, rzDeg = 18f, mat = Arena3DMeshes.MAT_ARMOR_WEAVE)
                emitHex(meshes.torus, Arena3DMeshes.TORUS_VERTEX_COUNT, 4.6f, -1.6f, 0f, 2.0f, 2.0f, 1.0f, 0xFFFFB800, mat = Arena3DMeshes.MAT_METALLIC)
                // Rear Bullpup Curved Magazine Behind the Grip!
                if (hasDrumMag) {
                    drawTwinDrumMag(meshes, ::emitHex, -4.4f, -2.5f, weaponColorHex)
                } else {
                    emitHex(meshes.curvedMag.buffer, meshes.curvedMag.vertexCount, -4.4f, -3.7f, 0f, 3.2f, 6.2f, 2.4f, 0xFF292524, mat = Arena3DMeshes.MAT_METALLIC)
                }
                // Heavy Integrated Groza Suppressor Barrel
                emitHex(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, 10.6f, 0.2f, 0f, 2.5f, 8.2f, 2.5f, 0xFF1F2937, rzDeg = 90f, mat = Arena3DMeshes.MAT_METALLIC)
                emitHex(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, 15.2f, 0.2f, 0f, 1.6f, 2.6f, 1.6f, 0xFFFF6B1A, rzDeg = 90f, emissive = 0.65f)
                muzzleX = 16.4f
            }

            // 5. PREDATOR COBRA MP40 — Flared Cobra-Hood Shroud + Twin Crimson Fangs + Stick Mag
            "wpn_cobra_mp40" -> {
                // Skeletal Wire Underfolder Stock
                emitHex(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, -6.2f, 0.2f, 0.9f, 0.7f, 6.6f, 0.7f, 0xFF334155, rzDeg = 90f, mat = Arena3DMeshes.MAT_METALLIC)
                emitHex(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, -6.2f, 0.2f, -0.9f, 0.7f, 6.6f, 0.7f, 0xFF334155, rzDeg = 90f, mat = Arena3DMeshes.MAT_METALLIC)
                emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, -9.2f, -0.7f, 0f, 1.4f, 4.0f, 2.2f, weaponColorHex, emissive = 0.45f, mat = Arena3DMeshes.MAT_METALLIC)
                // Cylindrical MP40 Receiver + Flared Cobra Hood Wing Plates
                emitHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, 0.8f, 0.2f, 0f, 2.8f, 10.6f, 2.8f, 0xFF1A1625, rzDeg = 90f, mat = Arena3DMeshes.MAT_METALLIC)
                emitHex(meshes.wing, Arena3DMeshes.SWEPT_WING_VERTEX_COUNT, 4.2f, 0.8f, 1.5f, 5.8f, 3.8f, 5.0f, weaponColorHex, rxDeg = -32f, emissive = 0.72f)
                emitHex(meshes.wing, Arena3DMeshes.SWEPT_WING_VERTEX_COUNT, 4.2f, 0.8f, -1.5f, 5.8f, 3.8f, -5.0f, weaponColorHex, rxDeg = 32f, emissive = 0.72f)
                // Twin Glowing Cobra Eye Jewels
                emitHex(meshes.sphere, Arena3DMeshes.SPHERE_VERTEX_COUNT, 3.6f, 1.5f, 1.4f, 1.0f, 0.8f, 0.8f, 0xFFFFEA00, emissive = 0.95f, mat = Arena3DMeshes.MAT_GLASS)
                emitHex(meshes.sphere, Arena3DMeshes.SPHERE_VERTEX_COUNT, 3.6f, 1.5f, -1.4f, 1.0f, 0.8f, 0.8f, 0xFFFFEA00, emissive = 0.95f, mat = Arena3DMeshes.MAT_GLASS)
                // Grip + Forward Stick Mag (or Drum)
                emitHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, -2.4f, -2.2f, 0f, 1.7f, 4.0f, 1.8f, 0xFF111827, rzDeg = 20f)
                if (hasDrumMag) {
                    drawTwinDrumMag(meshes, ::emitHex, 3.2f, -2.4f, weaponColorHex)
                } else {
                    emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, 3.2f, -3.8f, 0f, 1.8f, 6.4f, 1.8f, weaponColorHex, emissive = 0.35f, mat = Arena3DMeshes.MAT_METALLIC)
                }
                // Bare Barrel + Twin Curved Crimson Cobra Fangs at Muzzle!
                emitHex(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, 9.6f, 0.3f, 0f, 1.3f, 8.4f, 1.3f, 0xFF475569, rzDeg = 90f, mat = Arena3DMeshes.MAT_METALLIC)
                emitHex(meshes.curvedHorn.buffer, meshes.curvedHorn.vertexCount, 12.2f, 0.4f, 1.0f, 1.8f, 3.8f, 1.4f, weaponColorHex, rzDeg = -72f, emissive = 0.78f)
                emitHex(meshes.curvedHorn.buffer, meshes.curvedHorn.vertexCount, 12.2f, 0.4f, -1.0f, 1.8f, 3.8f, 1.4f, weaponColorHex, rzDeg = -72f, emissive = 0.78f)
                muzzleX = 14.6f
            }

            // 6. NEON VENOM UMP — Polymer SMG Chassis + Folding Triangle PDW Stock + Glowing Biohazard Toxin Tank
            "wpn_venom_ump" -> {
                // Triangular Side-Folding UMP Stock
                emitHex(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, -6.2f, 0.9f, 0f, 0.8f, 6.0f, 0.8f, 0xFF1F2937, rzDeg = 90f)
                emitHex(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, -6.2f, -0.4f, 0f, 0.8f, 6.0f, 0.8f, 0xFF1F2937, rzDeg = 76f)
                emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, -9.0f, -0.3f, 0f, 1.5f, 3.8f, 2.0f, 0xFF10E88A, emissive = 0.45f)
                // Blocky Contoured UMP Polymer Body + Top Rail
                emitHex(meshes.rifleReceiver.buffer, meshes.rifleReceiver.vertexCount, 1.0f, 0.2f, 0f, 12.0f, 3.9f, 3.3f, 0xFF111827, mat = Arena3DMeshes.MAT_ARMOR_WEAVE)
                // Glowing Emerald Biohazard Toxin Tube Strapped on Side!
                emitHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, 2.6f, 0.3f, 1.9f, 1.5f, 5.4f, 1.5f, 0xFF00E676, rzDeg = 90f, a = 0.92f, emissive = 0.92f * pulse, mat = Arena3DMeshes.MAT_GLASS)
                emitHex(meshes.torus, Arena3DMeshes.TORUS_VERTEX_COUNT, 0.6f, 0.3f, 1.9f, 1.8f, 1.8f, 1.8f, 0xFF334155, rxDeg = 90f, rzDeg = 90f)
                emitHex(meshes.torus, Arena3DMeshes.TORUS_VERTEX_COUNT, 4.6f, 0.3f, 1.9f, 1.8f, 1.8f, 1.8f, 0xFF334155, rxDeg = 90f, rzDeg = 90f)
                // Grip + Curved .45ACP UMP Stick Mag (or Drum)
                emitHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, -2.0f, -2.4f, 0f, 1.8f, 4.2f, 2.0f, 0xFF0F172A, rzDeg = 18f)
                if (hasDrumMag) {
                    drawTwinDrumMag(meshes, ::emitHex, 2.6f, -2.5f, weaponColorHex)
                } else {
                    emitHex(meshes.curvedMag.buffer, meshes.curvedMag.vertexCount, 2.6f, -3.7f, 0f, 2.2f, 6.2f, 2.0f, 0xFF00E676, emissive = 0.38f, mat = Arena3DMeshes.MAT_METALLIC)
                }
                emitHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, 6.0f, -2.0f, 0f, 1.3f, 3.2f, 1.3f, 0xFF1F2937)
                emitHex(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, 9.8f, 0.3f, 0f, 1.7f, 6.6f, 1.7f, 0xFF065F46, rzDeg = 90f, mat = Arena3DMeshes.MAT_METALLIC)
                muzzleX = 13.4f
            }

            // 7. PHARAOH M1887 — Gilded Lever-Action Shotgun with Twin Stacked Tubes & Brass Loop Lever!
            "wpn_gold_m1887" -> {
                // Carved Walnut/Gold Mare's Leg Buttstock
                emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, -5.8f, -1.0f, 0f, 6.8f, 3.2f, 2.3f, 0xFF78350F, rzDeg = -18f)
                // Gilded Engraved M1887 Action Receiver + Ejection Port
                emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, -0.4f, 0.1f, 0f, 6.4f, 3.4f, 2.8f, 0xFFF5B400, emissive = 0.34f, mat = Arena3DMeshes.MAT_METALLIC)
                // Signature Oversized Circular Lever-Action Finger Loop Below Action!
                emitHex(meshes.torus, Arena3DMeshes.TORUS_VERTEX_COUNT, -1.6f, -2.2f, 0f, 3.4f, 3.4f, 1.2f, 0xFFFDE047, emissive = 0.45f, mat = Arena3DMeshes.MAT_METALLIC)
                emitHex(meshes.torus, Arena3DMeshes.TORUS_VERTEX_COUNT, -4.0f, -1.8f, 0f, 2.0f, 2.0f, 1.0f, 0xFFFDE047, mat = Arena3DMeshes.MAT_METALLIC)
                // Stacked Upper Shotgun Barrel + Lower Full-Length Tube Magazine + Brass Barrel Bands
                emitHex(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, 8.8f, 0.9f, 0f, 1.85f, 13.4f, 1.85f, 0xFF334155, rzDeg = 90f, mat = Arena3DMeshes.MAT_METALLIC)
                emitHex(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, 8.4f, -0.9f, 0f, 1.55f, 12.6f, 1.55f, 0xFFB45309, rzDeg = 90f, mat = Arena3DMeshes.MAT_METALLIC)
                // Pharaoh Gold Barrel Clamps & Carved Forend
                emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, 5.2f, -0.4f, 0f, 6.4f, 2.4f, 2.6f, 0xFF92400E)
                emitHex(meshes.torus, Arena3DMeshes.TORUS_VERTEX_COUNT, 6.8f, 0f, 0f, 3.0f, 3.0f, 2.6f, 0xFFF5B400, rxDeg = 90f, rzDeg = 90f, emissive = 0.4f)
                emitHex(meshes.torus, Arena3DMeshes.TORUS_VERTEX_COUNT, 12.2f, 0f, 0f, 3.0f, 3.0f, 2.6f, 0xFFF5B400, rxDeg = 90f, rzDeg = 90f, emissive = 0.4f)
                if (hasDrumMag) {
                    drawTwinDrumMag(meshes, ::emitHex, 2.2f, -2.5f, weaponColorHex)
                }
                muzzleX = 15.8f
            }

            // 8. DUAL VECTOR OVERDRIVE — Iconic KRISS Super-V Angled Magwell PDW + Electric Cyan Arc Coils
            "wpn_vector_cyber" -> {
                // Compact Skeletonized PDW Folding Stock
                emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, -6.8f, 0.2f, 0f, 5.8f, 2.8f, 1.8f, 0xFF0F172A, mat = Arena3DMeshes.MAT_ARMOR_WEAVE)
                // High-Bore Vector Bullpup-Like Angled Polymer Frame
                emitHex(meshes.bullpupChassis.buffer, meshes.bullpupChassis.vertexCount, 0.8f, -0.3f, 0f, 10.6f, 4.6f, 3.0f, 0xFF1E293B, mat = Arena3DMeshes.MAT_METALLIC)
                // Diagonal Super-V Forward Recoil Housing & Slanted Extended Mag!
                emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, 3.6f, -1.8f, 0f, 3.2f, 4.8f, 2.6f, 0xFF0284C7, rzDeg = -28f, mat = Arena3DMeshes.MAT_METALLIC)
                if (hasDrumMag) {
                    drawTwinDrumMag(meshes, ::emitHex, 3.2f, -2.6f, weaponColorHex)
                } else {
                    emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, 4.5f, -4.4f, 0f, 1.8f, 5.6f, 1.8f, 0xFF00E5FF, rzDeg = -28f, emissive = 0.55f, mat = Arena3DMeshes.MAT_GLASS)
                }
                emitHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, -1.6f, -2.2f, 0f, 1.7f, 3.8f, 1.9f, 0xFF0F172A, rzDeg = 22f)
                // Cyan Arc-Coil Shroud Fins + Compact Heavy Shrouded Barrel
                for (i in 0 until 3) {
                    emitHex(meshes.torus, Arena3DMeshes.TORUS_VERTEX_COUNT, 6.4f + i * 1.4f, 0.4f, 0f, 2.7f, 2.7f, 2.7f, 0xFF00E5FF, rxDeg = 90f, rzDeg = 90f, emissive = 0.88f * pulse)
                }
                emitHex(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, 9.6f, 0.4f, 0f, 1.8f, 6.4f, 1.8f, 0xFF334155, rzDeg = 90f, mat = Arena3DMeshes.MAT_METALLIC)
                muzzleX = 13.0f
            }

            // 9. URBAN MP40 — Classic Tubular WWII/Tactical SMG with Underfolder & Vertical Stick Mag
            "wpn_mp40_default" -> {
                emitHex(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, -5.8f, 0.1f, 0f, 0.7f, 5.8f, 0.7f, 0xFF475569, rzDeg = 90f, mat = Arena3DMeshes.MAT_METALLIC)
                emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, -8.5f, -0.6f, 0f, 1.2f, 3.4f, 1.6f, 0xFF334155, mat = Arena3DMeshes.MAT_METALLIC)
                emitHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, 0.8f, 0.2f, 0f, 2.5f, 9.6f, 2.5f, 0xFF334155, rzDeg = 90f, mat = Arena3DMeshes.MAT_METALLIC)
                emitHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, -2.2f, -2.1f, 0f, 1.6f, 3.8f, 1.8f, 0xFF1E293B, rzDeg = 22f)
                if (hasDrumMag) {
                    drawTwinDrumMag(meshes, ::emitHex, 3.0f, -2.2f, weaponColorHex)
                } else {
                    emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, 3.0f, -3.5f, 0f, 1.7f, 5.8f, 1.7f, 0xFF1F2937, mat = Arena3DMeshes.MAT_METALLIC)
                }
                emitHex(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, 8.8f, 0.3f, 0f, 1.2f, 7.8f, 1.2f, 0xFF64748B, rzDeg = 90f, mat = Arena3DMeshes.MAT_METALLIC)
                muzzleX = 12.8f
            }

            // 10. TACTICAL AKM (DEFAULT) — Classic Ribbed Receiver + Wood/Polymer Furniture + Banana Mag
            else -> {
                emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, -7.4f, -0.6f, 0f, 7.6f, 3.8f, 2.2f, 0xFF4B3621, mat = Arena3DMeshes.MAT_STANDARD)
                emitHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, -6.8f, 0.9f, 0f, 1.8f, 5.0f, 2.0f, 0xFF2D2118, rzDeg = 90f)
                emitHex(meshes.rifleReceiver.buffer, meshes.rifleReceiver.vertexCount, 0f, 0f, 0f, 11.8f, 3.8f, 3.2f, weaponColorHex, emissive = 0.20f, mat = Arena3DMeshes.MAT_METALLIC)
                emitHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, 0.4f, 0.5f, 1.1f, 1.1f, 4.4f, 1.1f, 0xFFB8C2CC, rzDeg = 90f, mat = Arena3DMeshes.MAT_METALLIC)
                emitHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, -2.4f, -2.5f, 0f, 1.9f, 4.4f, 2.1f, 0xFF1A1F26, rzDeg = 22f, mat = Arena3DMeshes.MAT_ARMOR_WEAVE)
                emitHex(meshes.torus, Arena3DMeshes.TORUS_VERTEX_COUNT, -0.6f, -1.6f, 0f, 2.0f, 2.0f, 1.0f, 0xFF334155, mat = Arena3DMeshes.MAT_METALLIC)
                if (hasDrumMag) {
                    drawTwinDrumMag(meshes, ::emitHex, 1.8f, -2.5f, 0xFFF5B400L)
                } else {
                    emitHex(meshes.curvedMag.buffer, meshes.curvedMag.vertexCount, 1.8f, -3.6f, 0f, 3.2f, 6.2f, 2.4f, 0xFF1F2937, mat = Arena3DMeshes.MAT_METALLIC)
                }
                emitHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, 5.8f, 0.1f, 0f, 2.4f, 6.8f, 2.4f, 0xFF5C4028, rzDeg = 90f)
                for (ringIdx in 0 until 3) {
                    emitHex(meshes.torus, Arena3DMeshes.TORUS_VERTEX_COUNT, 4.2f + ringIdx * 1.8f, 0.1f, 0f, 2.7f, 2.7f, 2.7f, weaponColorHex, rxDeg = 90f, rzDeg = 90f, emissive = 0.25f, mat = Arena3DMeshes.MAT_METALLIC)
                }
                emitHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, 5.6f, -2.3f, 0f, 1.4f, 3.6f, 1.4f, 0xFF1A1F26)
                emitHex(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, 11.4f, 0.3f, 0f, 1.5f, 10.6f, 1.5f, 0xFF475569, rzDeg = 90f, mat = Arena3DMeshes.MAT_METALLIC)
                emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, 16.4f, 0.3f, 0f, 2.4f, 2.0f, 2.0f, 0xFF334155, mat = Arena3DMeshes.MAT_METALLIC)
                muzzleX = 17.2f
            }
        }

        // Tactical Side PEQ-15 Laser Sight Box (Mounted on Forend Rail)
        emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, 6.0f, 0.6f, 1.5f, 2.6f, 1.3f, 1.3f, 0xFF28303D, mat = Arena3DMeshes.MAT_METALLIC)
        emitHex(meshes.sphere, Arena3DMeshes.SPHERE_VERTEX_COUNT, 7.4f, 0.6f, 1.5f, 0.7f, 0.7f, 0.7f, laserHex, emissive = 0.90f)

        // Coated Multi-Part Optic Scope Mounted on Top Rail
        if (hasThermalScope) {
            emitHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, 0.4f, 2.7f, 0f, 2.5f, 7.8f, 2.5f, 0xFF1E293B, rzDeg = 90f, mat = Arena3DMeshes.MAT_METALLIC)
            emitHex(meshes.torus, Arena3DMeshes.TORUS_VERTEX_COUNT, 2.8f, 2.7f, 0f, 2.9f, 2.9f, 2.4f, 0xFFFF2A55, rxDeg = 90f, rzDeg = 90f, emissive = 0.65f)
            emitHex(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, 0.4f, 4.0f, 0f, 1.3f, 1.2f, 1.3f, 0xFFFF2A55, emissive = 0.65f)
            emitHex(meshes.sphere, Arena3DMeshes.SPHERE_VERTEX_COUNT, 4.2f, 2.7f, 0f, 2.0f, 2.0f, 0.9f, 0xFFFF3355, emissive = 0.85f, mat = Arena3DMeshes.MAT_GLASS)
        } else {
            emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, 0.4f, 2.1f, 0f, 3.2f, 1.2f, 2.2f, 0xFF1F2937, mat = Arena3DMeshes.MAT_METALLIC)
            emitHex(meshes.torus, Arena3DMeshes.TORUS_VERTEX_COUNT, 0.6f, 2.8f, 0f, 2.6f, 2.6f, 2.8f, 0xFF00E5FF, rzDeg = 90f, emissive = 0.58f)
            emitHex(meshes.sphere, Arena3DMeshes.SPHERE_VERTEX_COUNT, 1.3f, 2.8f, 0f, 1.8f, 1.8f, 0.7f, 0xFF00E5FF, emissive = 0.72f, mat = Arena3DMeshes.MAT_GLASS)
        }

        // Fluted Cryo-Core Suppressor Attachment (Mounted onto Barrel Tip)
        if (hasCryoSuppressor) {
            val suppX = muzzleX + 3.0f
            emitHex(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, suppX, 0.3f, 0f, 2.6f, 6.8f, 2.6f, 0xFF1E293B, rzDeg = 90f, mat = Arena3DMeshes.MAT_METALLIC)
            emitHex(meshes.torus, Arena3DMeshes.TORUS_VERTEX_COUNT, suppX - 1.5f, 0.3f, 0f, 2.9f, 2.9f, 2.9f, 0xFF00E5FF, rxDeg = 90f, rzDeg = 90f, emissive = 0.82f, mat = Arena3DMeshes.MAT_GLASS)
            emitHex(meshes.torus, Arena3DMeshes.TORUS_VERTEX_COUNT, suppX + 1.5f, 0.3f, 0f, 2.9f, 2.9f, 2.9f, 0xFF00E5FF, rxDeg = 90f, rzDeg = 90f, emissive = 0.82f, mat = Arena3DMeshes.MAT_GLASS)
            muzzleX += 6.4f
        }

        if (isMuzzleFlashing) {
            emitHex(meshes.sphere, Arena3DMeshes.SPHERE_VERTEX_COUNT, muzzleX + 1.6f, 0.3f, 0f, 8.2f, 8.2f, 8.2f, 0xFFFFCA28, ryDeg = elapsed * 420f, emissive = 1.0f)
            emitHex(meshes.cone, Arena3DMeshes.CONE_VERTEX_COUNT, muzzleX + 1.6f, 0.3f, 0f, 5.2f, 11.0f, 5.2f, 0xFFFF6D00, rzDeg = -90f, emissive = 0.95f)
        }
    }

    private fun drawTwinDrumMag(
        meshes: Es3MeshSet,
        emitHex: (
            FloatBuffer, Int, Float, Float, Float, Float, Float, Float,
            Long, Float, Float, Float, Float, Float, Int
        ) -> Unit,
        cx: Float,
        cy: Float,
        accentHex: Long
    ) {
        // Feed tower
        emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, cx, cy + 0.8f, 0f, 2.4f, 2.8f, 2.0f, 0xFF1F2937, 0f, 0f, 0f, 1f, 0f, Arena3DMeshes.MAT_METALLIC)
        // Left & Right C-Mag Drums
        emitHex(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, cx, cy - 1.4f, -2.0f, 4.2f, 2.4f, 4.2f, 0xFF1E293B, 90f, 0f, 0f, 1f, 0f, Arena3DMeshes.MAT_METALLIC)
        emitHex(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, cx, cy - 1.4f, 2.0f, 4.2f, 2.4f, 4.2f, 0xFF1E293B, 90f, 0f, 0f, 1f, 0f, Arena3DMeshes.MAT_METALLIC)
        // Gold/Accent Rotor Caps
        emitHex(meshes.torus, Arena3DMeshes.TORUS_VERTEX_COUNT, cx, cy - 1.4f, -2.9f, 3.6f, 3.6f, 1.4f, accentHex, 90f, 0f, 0f, 1f, 0.45f, Arena3DMeshes.MAT_METALLIC)
        emitHex(meshes.torus, Arena3DMeshes.TORUS_VERTEX_COUNT, cx, cy - 1.4f, 2.9f, 3.6f, 3.6f, 1.4f, accentHex, 90f, 0f, 0f, 1f, 0.45f, Arena3DMeshes.MAT_METALLIC)
    }
}
