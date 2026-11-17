package com.example.ui.arena

import android.opengl.GLES30
import android.opengl.Matrix
import java.nio.FloatBuffer
import kotlin.math.cos
import kotlin.math.sin

internal class Es3MeshSet {
    val cube = Arena3DMeshes.createCubeBuffer()
    val pyramid = Arena3DMeshes.createPyramidBuffer()
    val wedge = Arena3DMeshes.createWedgeRoofBuffer()
    val cylinder = Arena3DMeshes.createSmoothCylinderBuffer()
    val cone = Arena3DMeshes.createConeBuffer()
    val capsule = Arena3DMeshes.createCapsuleBuffer()
    val torus = Arena3DMeshes.createTorusBuffer()
    val sphere = Arena3DMeshes.createSphereBuffer()
    val torso = Arena3DMeshes.createTorsoArmorBuffer()
    val hull = Arena3DMeshes.createChamferedHullBuffer()
    val wing = Arena3DMeshes.createSweptWingBuffer()
    val rifleReceiver = Arena3DMeshes.createRifleReceiverMesh()
    val curvedMag = Arena3DMeshes.createCurvedMagMesh()
    val curvedHorn = Arena3DMeshes.createCurvedHornMesh()
    val sodePlate = Arena3DMeshes.createSodeArmorPlateMesh()
    val katanaScabbard = Arena3DMeshes.createKatanaScabbardMesh()
    val hexCrystal = Arena3DMeshes.createHexCrystalMesh()
    val bullpupChassis = Arena3DMeshes.createBullpupChassisMesh()
    val heavyPauldron = Arena3DMeshes.createHeavyPauldronMesh()
    val zoneWall = Arena3DMeshes.createZoneWallBuffer()
}

internal class Es3ShaderProgram {
    var programId = 0
        private set
    var uMVPMatrixLoc = 0
        private set
    var uModelMatrixLoc = 0
        private set
    var uColorLoc = 0
        private set
    var uEmissiveLoc = 0
        private set
    var uFogColorLoc = 0
        private set
    var uViewPosLoc = 0
        private set
    var uTimeLoc = 0
        private set
    var uMaterialModeLoc = 0
        private set
    var uFogStartLoc = 0
        private set
    var uFogRangeLoc = 0
        private set

    fun compile() {
        val vs = compileShader(GLES30.GL_VERTEX_SHADER, Arena3DMeshes.VERTEX_SHADER_ES3)
        val fs = compileShader(GLES30.GL_FRAGMENT_SHADER, Arena3DMeshes.FRAGMENT_SHADER_ES3)
        programId = GLES30.glCreateProgram().also { prog ->
            GLES30.glAttachShader(prog, vs)
            GLES30.glAttachShader(prog, fs)
            GLES30.glLinkProgram(prog)
        }
        uMVPMatrixLoc = GLES30.glGetUniformLocation(programId, "uMVPMatrix")
        uModelMatrixLoc = GLES30.glGetUniformLocation(programId, "uModelMatrix")
        uColorLoc = GLES30.glGetUniformLocation(programId, "uColor")
        uEmissiveLoc = GLES30.glGetUniformLocation(programId, "uEmissive")
        uFogColorLoc = GLES30.glGetUniformLocation(programId, "uFogColor")
        uViewPosLoc = GLES30.glGetUniformLocation(programId, "uViewPos")
        uTimeLoc = GLES30.glGetUniformLocation(programId, "uTime")
        uMaterialModeLoc = GLES30.glGetUniformLocation(programId, "uMaterialMode")
        uFogStartLoc = GLES30.glGetUniformLocation(programId, "uFogStart")
        uFogRangeLoc = GLES30.glGetUniformLocation(programId, "uFogRange")
    }

    private fun compileShader(type: Int, code: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, code)
        GLES30.glCompileShader(shader)
        return shader
    }
}

internal object Operative3DModel {

    fun drawSculptedOperative3D(
        meshes: Es3MeshSet,
        shader: Es3ShaderProgram,
        vpMatrix: FloatArray,
        modelMatrix: FloatArray,
        mvpMatrix: FloatArray,
        c: RenderCombatant,
        outfitColorHex: Long,
        hatId: String,
        hatColorHex: Long,
        accessoryId: String,
        accessoryColorHex: Long,
        weaponColorHex: Long,
        hasThermalScope: Boolean,
        elapsed: Float,
        isPlayer: Boolean,
        isSeatedInVehicle: Boolean,
        drawShadow: Boolean = true,
        characterId: String = "char_phantom",
        outfitId: String = "outfit_default",
        weaponId: String = "wpn_akm_default",
        attachmentId: String = if (hasThermalScope) "att_thermal_4x" else "att_holo_sight"
    ) {
        val rotDeg = -Math.toDegrees(c.angleRad.toDouble()).toFloat()
        val fx = cos(c.angleRad)
        val fz = sin(c.angleRad)
        val rx = -sin(c.angleRad)
        val rz = cos(c.angleRad)

        val crouchDrop = if (c.isCrouched) -7.5f else 0f
        val stride = if (c.isMoving && !isSeatedInVehicle) sin(c.walkPhase) else 0f
        val legSwing = stride * 4.4f
        val kneeBendDeg = stride * 24f
        val armSwing = stride * 2.2f
        val breathe = sin(elapsed * 2.4f) * 0.42f
        val baseY = if (isSeatedInVehicle) 2.5f else c.z

        val isTitan = isPlayer && characterId == "char_gold_titan"
        val isRonin = isPlayer && characterId == "char_inferno_ronin"
        val isValkyrie = isPlayer && characterId == "char_valkyrie"
        val isViper = isPlayer && characterId == "char_neon_viper"
        val widthMul = if (isTitan) 1.16f else if (isValkyrie) 0.94f else 1.0f

        fun emit(
            buf: FloatBuffer, verts: Int,
            cx: Float, cy: Float, cz: Float,
            sx: Float, sy: Float, sz: Float,
            ry: Float, rxDeg: Float = 0f, rzDeg: Float = 0f,
            r: Float, g: Float, b: Float, a: Float = 1f,
            emissive: Float = 0f, mat: Int = Arena3DMeshes.MAT_STANDARD
        ) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, cx, cy, cz)
            if (ry != 0f) Matrix.rotateM(modelMatrix, 0, ry, 0f, 1f, 0f)
            if (rxDeg != 0f) Matrix.rotateM(modelMatrix, 0, rxDeg, 1f, 0f, 0f)
            if (rzDeg != 0f) Matrix.rotateM(modelMatrix, 0, rzDeg, 0f, 0f, 1f)
            Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
            Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

            GLES30.glUniformMatrix4fv(shader.uMVPMatrixLoc, 1, false, mvpMatrix, 0)
            GLES30.glUniformMatrix4fv(shader.uModelMatrixLoc, 1, false, modelMatrix, 0)
            GLES30.glUniform4f(shader.uColorLoc, r, g, b, a)
            GLES30.glUniform1f(shader.uEmissiveLoc, emissive)
            GLES30.glUniform1i(shader.uMaterialModeLoc, mat)
            bindAndDraw(buf, verts)
        }

        fun emitHex(
            buf: FloatBuffer, verts: Int,
            cx: Float, cy: Float, cz: Float,
            sx: Float, sy: Float, sz: Float,
            ry: Float, hex: Long,
            rxDeg: Float = 0f, rzDeg: Float = 0f,
            a: Float = 1f, emissive: Float = 0f, mat: Int = Arena3DMeshes.MAT_STANDARD
        ) {
            val r = ((hex shr 16) and 0xFF) / 255f
            val g = ((hex shr 8) and 0xFF) / 255f
            val b = (hex and 0xFF) / 255f
            emit(buf, verts, cx, cy, cz, sx, sy, sz, ry, rxDeg, rzDeg, r, g, b, a, emissive, mat)
        }

        if (drawShadow && !isSeatedInVehicle) {
            emit(
                meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT,
                c.x - 2.5f, 0.42f, c.y - 1.5f,
                21f * widthMul, 0.45f, 21f * widthMul, rotDeg,
                r = 0.03f, g = 0.04f, b = 0.06f, a = 0.54f,
                mat = Arena3DMeshes.MAT_SHADOW
            )
        }

        // 1. ARTICULATED LOWER BODY + CHARACTER-SPECIFIC LEG ARMOR & BOOTS
        if (!isSeatedInVehicle) {
            val legH = if (c.isCrouched) 10.6f else 15.8f
            val legSpread = 4.6f * widthMul
            val leftX = c.x - rx * legSpread
            val leftZ = c.y - rz * legSpread
            val rightX = c.x + rx * legSpread
            val rightZ = c.y + rz * legSpread
            val thighY = baseY + legH * 0.80f
            val shinY = baseY + legH * 0.38f

            // Thighs + Shins + Knee Pads
            emitHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, leftX + fx * (legSwing * 0.7f), thighY, leftZ + fz * (legSwing * 0.7f), 6.4f * widthMul, legH * 0.54f, 6.4f * widthMul, rotDeg, 0xFF1E242E, rzDeg = -kneeBendDeg * 0.55f)
            emitHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, leftX + fx * (legSwing * 1.05f), shinY, leftZ + fz * (legSwing * 1.05f), 5.6f * widthMul, legH * 0.54f, 5.6f * widthMul, rotDeg, 0xFF151921, rzDeg = kneeBendDeg * 0.35f, mat = Arena3DMeshes.MAT_ARMOR_WEAVE)
            emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, leftX + fx * (legSwing * 0.9f + 2.8f), baseY + legH * 0.58f, leftZ + fz * (legSwing * 0.9f + 2.8f), 4.0f, 4.4f, 4.8f, rotDeg, if (isTitan) 0xFFF5B400 else 0xFF283242, mat = Arena3DMeshes.MAT_METALLIC)
            emitHex(meshes.torus, Arena3DMeshes.TORUS_VERTEX_COUNT, leftX + fx * (legSwing * 0.85f), baseY + legH * 0.57f, leftZ + fz * (legSwing * 0.85f), 6.2f, 2.2f, 6.2f, rotDeg, 0xFF0F172A)

            emitHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, rightX - fx * (legSwing * 0.7f), thighY, rightZ - fz * (legSwing * 0.7f), 6.4f * widthMul, legH * 0.54f, 6.4f * widthMul, rotDeg, 0xFF1E242E, rzDeg = kneeBendDeg * 0.55f)
            emitHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, rightX - fx * (legSwing * 1.05f), shinY, rightZ - fz * (legSwing * 1.05f), 5.6f * widthMul, legH * 0.54f, 5.6f * widthMul, rotDeg, 0xFF151921, rzDeg = -kneeBendDeg * 0.35f, mat = Arena3DMeshes.MAT_ARMOR_WEAVE)
            emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, rightX + fx * (-legSwing * 0.9f + 2.8f), baseY + legH * 0.58f, rightZ + fz * (-legSwing * 0.9f + 2.8f), 4.0f, 4.4f, 4.8f, rotDeg, if (isTitan) 0xFFF5B400 else 0xFF283242, mat = Arena3DMeshes.MAT_METALLIC)
            emitHex(meshes.torus, Arena3DMeshes.TORUS_VERTEX_COUNT, rightX - fx * (legSwing * 0.85f), baseY + legH * 0.57f, rightZ - fz * (legSwing * 0.85f), 6.2f, 2.2f, 6.2f, rotDeg, 0xFF0F172A)

            // Character-Specific Thigh Gear
            if (isViper) {
                emitHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, rightX + rx * 3.0f, thighY, rightZ + rz * 3.0f, 2.6f, 6.4f, 2.6f, rotDeg, 0xFF00E676, emissive = 0.88f, mat = Arena3DMeshes.MAT_GLASS)
                emitHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, leftX - rx * 3.0f, thighY, leftZ - rz * 3.0f, 2.6f, 6.4f, 2.6f, rotDeg, 0xFF00E676, emissive = 0.88f, mat = Arena3DMeshes.MAT_GLASS)
            } else if (isRonin) {
                emitHex(meshes.sodePlate.buffer, meshes.sodePlate.vertexCount, leftX - rx * 1.8f, thighY + 1.0f, leftZ - rz * 1.8f, 10.5f, 8.8f, 10.5f, rotDeg + 95f, 0xFF7F1D1D, mat = Arena3DMeshes.MAT_METALLIC)
                emitHex(meshes.sodePlate.buffer, meshes.sodePlate.vertexCount, rightX + rx * 1.8f, thighY + 1.0f, rightZ + rz * 1.8f, 10.5f, 8.8f, 10.5f, rotDeg - 95f, 0xFF7F1D1D, mat = Arena3DMeshes.MAT_METALLIC)
            } else if (isValkyrie) {
                emitHex(meshes.wing, Arena3DMeshes.SWEPT_WING_VERTEX_COUNT, leftX - rx * 3.2f, shinY + 1f, leftZ - rz * 3.2f, 6.4f, 6.8f, 3.2f, rotDeg + 15f, 0xFF00E5FF, emissive = 0.72f)
                emitHex(meshes.wing, Arena3DMeshes.SWEPT_WING_VERTEX_COUNT, rightX + rx * 3.2f, shinY + 1f, rightZ + rz * 3.2f, 6.4f, 6.8f, -3.2f, rotDeg - 15f, 0xFF00E5FF, emissive = 0.72f)
            } else {
                emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, rightX + rx * 2.8f - fx * (legSwing * 0.7f), thighY - 0.5f, rightZ + rz * 2.8f - fz * (legSwing * 0.7f), 4.6f, 5.2f, 2.8f, rotDeg, 0xFF111827, mat = Arena3DMeshes.MAT_ARMOR_WEAVE)
            }

            // 2-Part Sculpted Combat Boots
            emit(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, leftX + fx * (legSwing + 1.4f), baseY + 2.7f, leftZ + fz * (legSwing + 1.4f), 8.2f, 3.8f, 5.8f, rotDeg, r = 0.11f, g = 0.13f, b = 0.16f)
            emit(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, leftX + fx * (legSwing + 1.5f), baseY + 0.8f, leftZ + fz * (legSwing + 1.5f), 8.8f, 1.5f, 6.4f, rotDeg, r = 0.05f, g = 0.06f, b = 0.07f)
            emitHex(meshes.torus, Arena3DMeshes.TORUS_VERTEX_COUNT, leftX + fx * (legSwing + 0.4f), baseY + 4.8f, leftZ + fz * (legSwing + 0.4f), 6.2f, 2.4f, 6.2f, rotDeg, 0xFF1E293B)

            emit(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, rightX + fx * (-legSwing + 1.4f), baseY + 2.7f, rightZ + fz * (-legSwing + 1.4f), 8.2f, 3.8f, 5.8f, rotDeg, r = 0.11f, g = 0.13f, b = 0.16f)
            emit(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, rightX + fx * (-legSwing + 1.5f), baseY + 0.8f, rightZ + fz * (-legSwing + 1.5f), 8.8f, 1.5f, 6.4f, rotDeg, r = 0.05f, g = 0.06f, b = 0.07f)
            emitHex(meshes.torus, Arena3DMeshes.TORUS_VERTEX_COUNT, rightX + fx * (-legSwing + 0.4f), baseY + 4.8f, rightZ + fz * (-legSwing + 0.4f), 6.2f, 2.4f, 6.2f, rotDeg, 0xFF1E293B)

            // Utility Belt + Gold Buckle + Frag Grenade
            val beltY = baseY + 16.4f + crouchDrop * 0.5f
            emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, c.x, beltY, c.y, 11.4f * widthMul, 3.6f, 14.2f * widthMul, rotDeg, 0xFF1A1D24, mat = Arena3DMeshes.MAT_ARMOR_WEAVE)
            emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, c.x - rx * 6.8f * widthMul, beltY, c.y - rz * 6.8f * widthMul, 4.2f, 3.8f, 3.2f, rotDeg, 0xFF28303B)
            emitHex(meshes.sphere, Arena3DMeshes.SPHERE_VERTEX_COUNT, c.x + rx * 6.8f * widthMul, beltY - 0.3f, c.y + rz * 6.8f * widthMul, 3.2f, 3.8f, 3.2f, rotDeg, 0xFF3F6212, mat = Arena3DMeshes.MAT_METALLIC)
            emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, c.x + fx * 5.7f, beltY, c.y + fz * 5.7f, 1.8f, 2.6f, 3.4f, rotDeg, 0xFFF5B400L, emissive = 0.25f, mat = Arena3DMeshes.MAT_METALLIC)
        }

        // 2. ANATOMICAL TORSO + OUTFIT LAYER + CHARACTER-SPECIFIC SIGNATURE CHEST & PAULDRONS
        val torsoY = baseY + 23.6f + crouchDrop + breathe
        emitHex(meshes.torso, Arena3DMeshes.TORSO_ARMOR_VERTEX_COUNT, c.x, torsoY, c.y, 11.8f * widthMul, 15.2f, 15.6f * widthMul, rotDeg, outfitColorHex)
        emitHex(meshes.torso, Arena3DMeshes.TORSO_ARMOR_VERTEX_COUNT, c.x + fx * 0.5f, torsoY + 0.6f, c.y + fz * 0.5f, 12.9f * widthMul, 11.4f, 16.6f * widthMul, rotDeg, 0xFF1B2330, mat = Arena3DMeshes.MAT_ARMOR_WEAVE)
        emitHex(meshes.torus, Arena3DMeshes.TORUS_VERTEX_COUNT, c.x, torsoY + 6.8f, c.y, 9.4f * widthMul, 3.6f, 9.8f * widthMul, rotDeg, 0xFF222D3C, mat = Arena3DMeshes.MAT_ARMOR_WEAVE)

        // Character & Outfit Specific Chest Plates / Trench Tails / Cyber Conduits
        if (outfitId == "outfit_amber_jacket" && !isSeatedInVehicle) {
            // Flowing Trench Coat Coattails & Cross-Body Shell Bandolier
            emitHex(meshes.sodePlate.buffer, meshes.sodePlate.vertexCount, c.x - fx * 3.0f, torsoY - 10.5f, c.y - fz * 3.0f, 18.5f, 12.5f, 18.0f, rotDeg + 180f, 0xFFD97706, mat = Arena3DMeshes.MAT_ARMOR_WEAVE)
            emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, c.x + fx * 3.8f, torsoY + 7.2f, c.y + fz * 3.8f, 3.6f, 4.4f, 12.6f, rotDeg, 0xFF92400E)
        } else if (outfitId == "outfit_neon_stealth") {
            // Glowing Cyber Shadow Exo-Spine & Chest Ribs
            for (r in 0..2) {
                emitHex(meshes.torus, Arena3DMeshes.TORUS_VERTEX_COUNT, c.x + fx * 0.8f, torsoY - 1.5f + r * 3.0f, c.y + fz * 0.8f, 15.6f, 1.4f, 17.6f, rotDeg, 0xFF00E5FF, emissive = 0.85f)
            }
        }

        when {
            isRonin -> {
                // Layered Samurai Dou Breastplate + Crossed Twin Katanas + 3-Tier Sode Shoulder Plates!
                emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, c.x + fx * 6.0f, torsoY + 1.2f, c.y + fz * 6.0f, 3.2f, 7.8f, 11.4f, rotDeg, 0xFF991B1B, mat = Arena3DMeshes.MAT_METALLIC)
                emitHex(meshes.sphere, Arena3DMeshes.SPHERE_VERTEX_COUNT, c.x + fx * 7.2f, torsoY + 2.2f, c.y + fz * 7.2f, 3.4f, 3.4f, 2.2f, rotDeg, 0xFFFF6B1A, emissive = 0.92f)
                // Crossed Dual Katana Scabbards Behind Shoulders
                emitHex(meshes.katanaScabbard.buffer, meshes.katanaScabbard.vertexCount, c.x - fx * 6.2f, torsoY + 3.0f, c.y - fz * 6.2f, 18f, 28f, 18f, rotDeg, 0xFF1F2937, rxDeg = 34f, mat = Arena3DMeshes.MAT_METALLIC)
                emitHex(meshes.katanaScabbard.buffer, meshes.katanaScabbard.vertexCount, c.x - fx * 6.2f, torsoY + 3.0f, c.y - fz * 6.2f, 18f, 28f, 18f, rotDeg, 0xFF7F1D1D, rxDeg = -34f, mat = Arena3DMeshes.MAT_METALLIC)
                // 3-Tier Sode Samurai Pauldrons
                for (tier in 0..2) {
                    emitHex(meshes.sodePlate.buffer, meshes.sodePlate.vertexCount, c.x - rx * (7.8f + tier * 0.6f), torsoY + 5.8f - tier * 1.8f, c.y - rz * (7.8f + tier * 0.6f), 8.2f, 3.6f, 8.2f, rotDeg + 90f, if (tier == 1) 0xFFF5B400 else 0xFF991B1B, mat = Arena3DMeshes.MAT_METALLIC)
                    emitHex(meshes.sodePlate.buffer, meshes.sodePlate.vertexCount, c.x + rx * (7.8f + tier * 0.6f), torsoY + 5.8f - tier * 1.8f, c.y + rz * (7.8f + tier * 0.6f), 8.2f, 3.6f, 8.2f, rotDeg - 90f, if (tier == 1) 0xFFF5B400 else 0xFF991B1B, mat = Arena3DMeshes.MAT_METALLIC)
                }
            }
            isTitan -> {
                // Heavy Juggernaut Exo-Frame + Gold Blast Shield + Huge Ribbed Pauldrons
                emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, c.x + fx * 6.8f, torsoY + 1.2f, c.y + fz * 6.8f, 4.2f, 8.6f, 13.0f, rotDeg, 0xFFF5B400, emissive = 0.25f, mat = Arena3DMeshes.MAT_METALLIC)
                emitHex(meshes.heavyPauldron.buffer, meshes.heavyPauldron.vertexCount, c.x - rx * 9.4f, torsoY + 5.8f, c.y - rz * 9.4f, 12.2f, 9.6f, 11.4f, rotDeg + 90f, 0xFFD97706, mat = Arena3DMeshes.MAT_METALLIC)
                emitHex(meshes.heavyPauldron.buffer, meshes.heavyPauldron.vertexCount, c.x + rx * 9.4f, torsoY + 5.8f, c.y + rz * 9.4f, 12.2f, 9.6f, 11.4f, rotDeg - 90f, 0xFFD97706, mat = Arena3DMeshes.MAT_METALLIC)
            }
            isValkyrie -> {
                // Valkyrie Cyan Arc Reactor + Aero Flight Pauldrons
                emitHex(meshes.torus, Arena3DMeshes.TORUS_VERTEX_COUNT, c.x + fx * 6.4f, torsoY + 2.2f, c.y + fz * 6.4f, 4.8f, 4.8f, 2.0f, rotDeg + 90f, 0xFF00E5FF, emissive = 0.92f)
                emitHex(meshes.sphere, Arena3DMeshes.SPHERE_VERTEX_COUNT, c.x + fx * 6.8f, torsoY + 2.2f, c.y + fz * 6.8f, 2.6f, 2.6f, 1.8f, rotDeg, 0xFFE0F2FE, emissive = 0.96f, mat = Arena3DMeshes.MAT_GLASS)
                emitHex(meshes.wing, Arena3DMeshes.SWEPT_WING_VERTEX_COUNT, c.x - rx * 7.5f, torsoY + 5.8f, c.y - rz * 7.5f, 9.0f, 6.2f, 7.8f, rotDeg + 18f, 0xFF00E5FF, emissive = 0.55f, mat = Arena3DMeshes.MAT_METALLIC)
                emitHex(meshes.wing, Arena3DMeshes.SWEPT_WING_VERTEX_COUNT, c.x + rx * 7.5f, torsoY + 5.8f, c.y + rz * 7.5f, 9.0f, 6.2f, -7.8f, rotDeg - 18f, 0xFF00E5FF, emissive = 0.55f, mat = Arena3DMeshes.MAT_METALLIC)
            }
            isViper -> {
                // Cyber Viper Bio-Toxin Chest Valves + Spiked Snake Spine + Mantis Forearm Blades
                emitHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, c.x + fx * 6.2f - rx * 2.6f, torsoY + 1.8f, c.y + fz * 6.2f - rz * 2.6f, 2.2f, 5.4f, 2.2f, rotDeg, 0xFF00E676, emissive = 0.88f, mat = Arena3DMeshes.MAT_GLASS)
                emitHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, c.x + fx * 6.2f + rx * 2.6f, torsoY + 1.8f, c.y + fz * 6.2f + rz * 2.6f, 2.2f, 5.4f, 2.2f, rotDeg, 0xFF00E676, emissive = 0.88f, mat = Arena3DMeshes.MAT_GLASS)
                emitHex(meshes.heavyPauldron.buffer, meshes.heavyPauldron.vertexCount, c.x - rx * 8.2f, torsoY + 5.4f, c.y - rz * 8.2f, 8.6f, 6.4f, 8.4f, rotDeg + 90f, 0xFF065F46, mat = Arena3DMeshes.MAT_METALLIC)
                emitHex(meshes.heavyPauldron.buffer, meshes.heavyPauldron.vertexCount, c.x + rx * 8.2f, torsoY + 5.4f, c.y + rz * 8.2f, 8.6f, 6.4f, 8.4f, rotDeg - 90f, 0xFF065F46, mat = Arena3DMeshes.MAT_METALLIC)
                emitHex(meshes.curvedHorn.buffer, meshes.curvedHorn.vertexCount, c.x - rx * 8.4f, torsoY + 6.4f, c.y - rz * 8.4f, 2.4f, 5.2f, 2.4f, rotDeg, 0xFF00E676, rzDeg = 22f, emissive = 0.65f)
                emitHex(meshes.curvedHorn.buffer, meshes.curvedHorn.vertexCount, c.x + rx * 8.4f, torsoY + 6.4f, c.y + rz * 8.4f, 2.4f, 5.2f, 2.4f, rotDeg, 0xFF00E676, rzDeg = -22f, emissive = 0.65f)
            }
            else -> {
                // Phantom Striker / Tactical PMC: Armored Plate + Combat Knife Sheath + Dual Pauldrons
                emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, c.x + fx * 5.8f, torsoY + 1.0f, c.y + fz * 5.8f, 2.8f, 6.2f, 10.6f, rotDeg, c.accentHex, emissive = 0.24f, mat = Arena3DMeshes.MAT_METALLIC)
                emitHex(meshes.heavyPauldron.buffer, meshes.heavyPauldron.vertexCount, c.x - rx * 8.0f, torsoY + 5.6f, c.y - rz * 8.0f, 8.4f, 6.8f, 8.4f, rotDeg + 90f, 0xFF283242, mat = Arena3DMeshes.MAT_METALLIC)
                emitHex(meshes.heavyPauldron.buffer, meshes.heavyPauldron.vertexCount, c.x + rx * 8.0f, torsoY + 5.6f, c.y + rz * 8.0f, 8.4f, 6.8f, 8.4f, rotDeg - 90f, 0xFF283242, mat = Arena3DMeshes.MAT_METALLIC)
                emitHex(meshes.torus, Arena3DMeshes.TORUS_VERTEX_COUNT, c.x - rx * 8.1f, torsoY + 5.2f, c.y - rz * 8.1f, 6.8f, 2.2f, 6.8f, rotDeg, c.accentHex, emissive = 0.22f)
                emitHex(meshes.torus, Arena3DMeshes.TORUS_VERTEX_COUNT, c.x + rx * 8.1f, torsoY + 5.2f, c.y + rz * 8.1f, 6.8f, 2.2f, 6.8f, rotDeg, c.accentHex, emissive = 0.22f)
            }
        }

        // Front Triple Mag Pouches & Shoulder Tactical Radio Whip
        for (p in -1..1) {
            val offsetSide = p * 3.1f
            emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, c.x + fx * 6.4f + rx * offsetSide, torsoY - 3.6f, c.y + fz * 6.4f + rz * offsetSide, 2.4f, 4.2f, 2.5f, rotDeg, 0xFF141820, mat = Arena3DMeshes.MAT_ARMOR_WEAVE)
        }
        emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, c.x - rx * 6.2f + fx * 2.0f, torsoY + 7.2f, c.y - rz * 6.2f + fz * 2.0f, 2.6f, 3.8f, 2.4f, rotDeg, 0xFF0F172A, mat = Arena3DMeshes.MAT_METALLIC)
        emitHex(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, c.x - rx * 6.2f + fx * 1.4f, torsoY + 12.6f, c.y - rz * 6.2f + fz * 1.4f, 0.6f, 8.8f, 0.6f, rotDeg, 0xFF94A3B8, mat = Arena3DMeshes.MAT_METALLIC)

        // 3. ULTRA-DETAILED ACCESSORIES (14-Feather Phoenix Wings / Triple-Gyro Quantum Halo / L3 Pack + Tactical Pan)
        drawAccessoryOnBack(meshes, ::emit, ::emitHex, accessoryId, accessoryColorHex, c.x, torsoY, c.y, fx, fz, rx, rz, rotDeg, elapsed)

        // 4. HEAD + CHARACTER FACE MASK + HEADSET + HIGH-POLY HELMETS
        val headY = baseY + 36.2f + crouchDrop + breathe
        emit(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, c.x, headY - 5.2f, c.y, 4.6f, 5.2f, 4.6f, rotDeg, r = 0.76f, g = 0.60f, b = 0.50f)
        emit(meshes.sphere, Arena3DMeshes.SPHERE_VERTEX_COUNT, c.x, headY, c.y, 9.8f, 10.6f, 9.6f, rotDeg, r = 0.82f, g = 0.68f, b = 0.58f)
        // Jaw Mask / Oni Mask / Viper Gas Mask
        when {
            isRonin -> {
                emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, c.x + fx * 2.6f, headY - 2.3f, c.y + fz * 2.6f, 8.2f, 4.8f, 9.4f, rotDeg, 0xFF7F1D1D, mat = Arena3DMeshes.MAT_METALLIC)
                emitHex(meshes.curvedHorn.buffer, meshes.curvedHorn.vertexCount, c.x + fx * 5.2f - rx * 2.2f, headY - 2.4f, c.y + fz * 5.2f - rz * 2.2f, 1.8f, 2.8f, 1.8f, rotDeg, 0xFFFDE047, rzDeg = -25f, emissive = 0.4f)
                emitHex(meshes.curvedHorn.buffer, meshes.curvedHorn.vertexCount, c.x + fx * 5.2f + rx * 2.2f, headY - 2.4f, c.y + fz * 5.2f + rz * 2.2f, 1.8f, 2.8f, 1.8f, rotDeg, 0xFFFDE047, rzDeg = -25f, emissive = 0.4f)
            }
            isViper -> {
                emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, c.x + fx * 2.5f, headY - 2.4f, c.y + fz * 2.5f, 8.0f, 4.8f, 9.0f, rotDeg, 0xFF0F172A, mat = Arena3DMeshes.MAT_ARMOR_WEAVE)
                emitHex(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, c.x + fx * 5.0f - rx * 2.8f, headY - 2.6f, c.y + fz * 5.0f - rz * 2.8f, 2.6f, 2.2f, 2.6f, rotDeg, 0xFF00E676, rzDeg = 90f, emissive = 0.85f)
                emitHex(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, c.x + fx * 5.0f + rx * 2.8f, headY - 2.6f, c.y + fz * 5.0f + rz * 2.8f, 2.6f, 2.2f, 2.6f, rotDeg, 0xFF00E676, rzDeg = 90f, emissive = 0.85f)
            }
            else -> {
                emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, c.x + fx * 2.2f, headY - 2.5f, c.y + fz * 2.2f, 7.6f, 4.4f, 8.8f, rotDeg, 0xFF141821, mat = Arena3DMeshes.MAT_ARMOR_WEAVE)
            }
        }
        // Tactical Headset & Boom Mic (Valkyrie gets Winged Sensor Ear-Fins)
        emitHex(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, c.x - rx * 5.0f, headY + 0.2f, c.y - rz * 5.0f, 3.6f, 1.8f, 3.6f, rotDeg, 0xFF1E293B, rxDeg = 90f, mat = Arena3DMeshes.MAT_METALLIC)
        emitHex(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, c.x + rx * 5.0f, headY + 0.2f, c.y + rz * 5.0f, 3.6f, 1.8f, 3.6f, rotDeg, 0xFF1E293B, rxDeg = 90f, mat = Arena3DMeshes.MAT_METALLIC)
        emitHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, c.x - rx * 4.5f + fx * 3.0f, headY - 1.8f, c.y - rz * 4.5f + fz * 3.0f, 0.8f, 4.4f, 0.8f, rotDeg, rzDeg = 76f, hex = 0xFF475569, mat = Arena3DMeshes.MAT_METALLIC)
        if (isValkyrie) {
            emitHex(meshes.wing, Arena3DMeshes.SWEPT_WING_VERTEX_COUNT, c.x - rx * 5.6f, headY + 2.2f, c.y - rz * 5.6f, 7.5f, 6.2f, 3.0f, rotDeg + 20f, 0xFF00E5FF, emissive = 0.85f)
            emitHex(meshes.wing, Arena3DMeshes.SWEPT_WING_VERTEX_COUNT, c.x + rx * 5.6f, headY + 2.2f, c.y + rz * 5.6f, 7.5f, 6.2f, -3.0f, rotDeg - 20f, 0xFF00E5FF, emissive = 0.85f)
        }

        drawHatOnHead(meshes, ::emit, ::emitHex, hatId, hatColorHex, c.x, headY, c.y, fx, fz, rx, rz, rotDeg, isPlayer, isTitan)

        // 5. ARTICULATED ARMS + ELBOW GUARDS + GLOVED HANDS
        val armY = torsoY + 2.2f
        val armSpread = 7.8f * widthMul
        emitHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, c.x - rx * armSpread + fx * (1.8f - armSwing * 0.4f), armY - 1.2f, c.y - rz * armSpread + fz * (1.8f - armSwing * 0.4f), 4.3f * widthMul, 8.8f, 4.3f * widthMul, rotDeg, rxDeg = 28f, rzDeg = -22f, hex = outfitColorHex)
        emitHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, c.x + rx * armSpread + fx * (2.6f + armSwing * 0.4f), armY - 1.0f, c.y + rz * armSpread + fz * (2.6f + armSwing * 0.4f), 4.3f * widthMul, 8.8f, 4.3f * widthMul, rotDeg, rxDeg = -22f, rzDeg = -34f, hex = outfitColorHex)
        emitHex(meshes.sphere, Arena3DMeshes.SPHERE_VERTEX_COUNT, c.x - rx * (armSpread - 0.5f) + fx * 4.2f, armY - 3.6f, c.y - rz * (armSpread - 0.5f) + fz * 4.2f, 3.6f, 3.6f, 3.6f, rotDeg, 0xFF1F2937, mat = Arena3DMeshes.MAT_METALLIC)
        emitHex(meshes.sphere, Arena3DMeshes.SPHERE_VERTEX_COUNT, c.x + rx * (armSpread - 0.3f) + fx * 4.8f, armY - 3.2f, c.y + rz * (armSpread - 0.3f) + fz * 4.8f, 3.6f, 3.6f, 3.6f, rotDeg, 0xFF1F2937, mat = Arena3DMeshes.MAT_METALLIC)
        emit(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, c.x + fx * 8.2f - rx * 1.4f, armY - 2.6f, c.y + fz * 8.2f - rz * 1.4f, 3.6f, 9.2f, 3.6f, rotDeg, rzDeg = 72f, r = 0.14f, g = 0.17f, b = 0.21f, mat = Arena3DMeshes.MAT_ARMOR_WEAVE)
        emit(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, c.x + fx * 7.4f + rx * 6.0f, armY - 2.2f, c.y + fz * 7.4f + rz * 6.0f, 3.6f, 8.6f, 3.6f, rotDeg, rzDeg = 68f, r = 0.14f, g = 0.17f, b = 0.21f, mat = Arena3DMeshes.MAT_ARMOR_WEAVE)
        emitHex(meshes.sphere, Arena3DMeshes.SPHERE_VERTEX_COUNT, c.x + fx * 10.6f + rx * 5.2f, armY - 2.0f, c.y + fz * 10.6f + rz * 5.2f, 2.8f, 2.4f, 2.6f, rotDeg, 0xFF0F172A)
        emitHex(meshes.sphere, Arena3DMeshes.SPHERE_VERTEX_COUNT, c.x + fx * 15.2f + rx * 5.2f, armY - 1.8f, c.y + fz * 15.2f + rz * 5.2f, 2.8f, 2.4f, 2.6f, rotDeg, 0xFF0F172A)

        // 6. BESPOKE PER-ID WEAPON + ATTACHMENT MODEL IN HANDS
        val gunX = c.x + rx * 5.4f + fx * 11.6f
        val gunZ = c.y + rz * 5.4f + fz * 11.6f
        val gunY = armY - 0.5f
        Weapon3DModel.drawWeapon3D(
            meshes = meshes,
            shader = shader,
            vpMatrix = vpMatrix,
            tempModelMatrix = modelMatrix,
            tempMvpMatrix = mvpMatrix,
            weaponId = weaponId,
            weaponColorHex = weaponColorHex,
            attachmentId = attachmentId,
            worldX = gunX,
            worldY = gunY,
            worldZ = gunZ,
            rotYDeg = rotDeg,
            scale = 1.0f,
            isPlayer = isPlayer,
            isMuzzleFlashing = c.isMuzzleFlashing,
            elapsed = elapsed
        )
    }

    fun drawAccessoryOnBack(
        meshes: Es3MeshSet,
        emit: (FloatBuffer, Int, Float, Float, Float, Float, Float, Float, Float, Float, Float, Float, Float, Float, Float, Float, Int) -> Unit,
        emitHex: (FloatBuffer, Int, Float, Float, Float, Float, Float, Float, Float, Long, Float, Float, Float, Float, Int) -> Unit,
        accessoryId: String,
        accessoryColorHex: Long,
        cx: Float, torsoY: Float, cz: Float,
        fx: Float, fz: Float, rx: Float, rz: Float,
        rotDeg: Float,
        elapsed: Float,
        scaleMul: Float = 1f
    ) {
        when (accessoryId) {
            "acc_wings_blaze" -> {
                val flap = sin(elapsed * 6f) * 8f
                emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, cx - fx * 6.4f * scaleMul, torsoY + 1.8f * scaleMul, cz - fz * 6.4f * scaleMul, 4.8f * scaleMul, 11.2f * scaleMul, 6.8f * scaleMul, rotDeg, 0xFF1E293B, 0f, 0f, 1f, 0f, Arena3DMeshes.MAT_METALLIC)
                emitHex(meshes.sphere, Arena3DMeshes.SPHERE_VERTEX_COUNT, cx - fx * 8.4f * scaleMul, torsoY + 2.2f * scaleMul, cz - fz * 8.4f * scaleMul, 4.6f * scaleMul, 5.6f * scaleMul, 4.6f * scaleMul, rotDeg, accessoryColorHex, 0f, 0f, 1f, 0.94f, Arena3DMeshes.MAT_STANDARD)
                val wingCount = 7
                for (w in 0 until wingCount) {
                    val fanDeg = -24f + w * 11f
                    val scaleFrac = (1.12f - w * 0.11f) * scaleMul
                    val yOff = torsoY + (5.8f - w * 1.6f) * scaleMul
                    emitHex(
                        meshes.wing, Arena3DMeshes.SWEPT_WING_VERTEX_COUNT,
                        cx - fx * 6.8f * scaleMul - rx * 4.8f * scaleMul, yOff, cz - fz * 6.8f * scaleMul - rz * 4.8f * scaleMul,
                        20f * scaleFrac, 24f * scaleFrac, 24f * scaleFrac,
                        rotDeg + 22f + flap + fanDeg * 0.45f,
                        if (w % 2 == 0) accessoryColorHex else 0xFFFFB800L, fanDeg * 0.38f, 0f,
                        0.95f, 0.72f - w * 0.05f, Arena3DMeshes.MAT_METALLIC
                    )
                    emitHex(
                        meshes.wing, Arena3DMeshes.SWEPT_WING_VERTEX_COUNT,
                        cx - fx * 6.8f * scaleMul + rx * 4.8f * scaleMul, yOff, cz - fz * 6.8f * scaleMul + rz * 4.8f * scaleMul,
                        20f * scaleFrac, 24f * scaleFrac, -24f * scaleFrac,
                        rotDeg - 22f - flap - fanDeg * 0.45f,
                        if (w % 2 == 0) accessoryColorHex else 0xFFFFB800L, -fanDeg * 0.38f, 0f,
                        0.95f, 0.72f - w * 0.05f, Arena3DMeshes.MAT_METALLIC
                    )
                }
            }
            "acc_neon_aura" -> {
                val spin = (elapsed * 140f) % 360f
                val spinRev = -(elapsed * 95f) % 360f
                val spinMid = (elapsed * 190f) % 360f
                emitHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, cx - fx * 6.2f * scaleMul, torsoY + 2f * scaleMul, cz - fz * 6.2f * scaleMul, 4.6f * scaleMul, 9.0f * scaleMul, 4.6f * scaleMul, rotDeg, 0xFF0F172A, 0f, 0f, 1f, 0f, Arena3DMeshes.MAT_METALLIC)
                emitHex(meshes.sphere, Arena3DMeshes.SPHERE_VERTEX_COUNT, cx - fx * 7.6f * scaleMul, torsoY + 2f * scaleMul, cz - fz * 7.6f * scaleMul, 3.8f * scaleMul, 3.8f * scaleMul, 3.8f * scaleMul, rotDeg, accessoryColorHex, 0f, 0f, 1f, 0.96f, Arena3DMeshes.MAT_STANDARD)
                emitHex(meshes.torus, Arena3DMeshes.TORUS_VERTEX_COUNT, cx - fx * 5.8f * scaleMul, torsoY + 7.5f * scaleMul, cz - fz * 5.8f * scaleMul, 24f * scaleMul, 19f * scaleMul, 24f * scaleMul, spin, accessoryColorHex, 72f, 0f, 0.92f, 0.88f, Arena3DMeshes.MAT_STANDARD)
                emitHex(meshes.torus, Arena3DMeshes.TORUS_VERTEX_COUNT, cx - fx * 5.8f * scaleMul, torsoY + 7.5f * scaleMul, cz - fz * 5.8f * scaleMul, 17f * scaleMul, 15f * scaleMul, 17f * scaleMul, spinRev, 0xFFF5B400L, 55f, 35f, 0.90f, 0.82f, Arena3DMeshes.MAT_STANDARD)
                emitHex(meshes.torus, Arena3DMeshes.TORUS_VERTEX_COUNT, cx - fx * 5.8f * scaleMul, torsoY + 7.5f * scaleMul, cz - fz * 5.8f * scaleMul, 11f * scaleMul, 10f * scaleMul, 11f * scaleMul, spinMid, 0xFF38BDF8L, -42f, 60f, 0.92f, 0.90f, Arena3DMeshes.MAT_STANDARD)
                for (orb in 0 until 6) {
                    val oa = elapsed * 2.8f + orb * 1.0472f
                    val ox = cx - fx * 6f * scaleMul + cos(oa) * 11.2f * scaleMul
                    val oz = cz - fz * 6f * scaleMul + sin(oa) * 11.2f * scaleMul
                    val oy = torsoY + 7.5f * scaleMul + sin(oa * 2f) * 3.8f * scaleMul
                    emitHex(meshes.hexCrystal.buffer, meshes.hexCrystal.vertexCount, ox, oy, oz, 2.2f * scaleMul, 3.6f * scaleMul, 2.2f * scaleMul, elapsed * 120f + orb * 60f, accessoryColorHex, 0f, 0f, 1f, 0.96f, Arena3DMeshes.MAT_GLASS)
                }
            }
            else -> {
                // Authentic PUBG Level-3 Tactical Rucksack + Iconic Cast-Iron Pan!
                emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, cx - fx * 7.5f * scaleMul, torsoY + 0.2f * scaleMul, cz - fz * 7.5f * scaleMul, 7.6f * scaleMul, 12.6f * scaleMul, 11.4f * scaleMul, rotDeg, 0xFF2B3429, 0f, 0f, 1f, 0f, Arena3DMeshes.MAT_ARMOR_WEAVE)
                emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, cx - fx * 8.4f * scaleMul, torsoY + 4.4f * scaleMul, cz - fz * 8.4f * scaleMul, 6.2f * scaleMul, 4.0f * scaleMul, 10.4f * scaleMul, rotDeg, 0xFF394436, 0f, 0f, 1f, 0f, Arena3DMeshes.MAT_STANDARD)
                emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, cx - fx * 10.2f * scaleMul, torsoY - 2.2f * scaleMul, cz - fz * 10.2f * scaleMul, 3.8f * scaleMul, 6.2f * scaleMul, 8.2f * scaleMul, rotDeg, 0xFF1F271E, 0f, 0f, 1f, 0f, Arena3DMeshes.MAT_STANDARD)
                emit(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, cx - fx * 7.8f * scaleMul, torsoY + 7.6f * scaleMul, cz - fz * 7.8f * scaleMul, 4.4f * scaleMul, 11.4f * scaleMul, 4.4f * scaleMul, rotDeg, 90f, 0f, 0.36f, 0.32f, 0.24f, 1f, 0f, Arena3DMeshes.MAT_STANDARD)
                emitHex(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, cx - fx * 8.2f * scaleMul - rx * 6.2f * scaleMul, torsoY + 0.5f * scaleMul, cz - fz * 8.2f * scaleMul - rz * 6.2f * scaleMul, 2.8f * scaleMul, 5.4f * scaleMul, 2.8f * scaleMul, rotDeg, 0xFF374151, 0f, 0f, 1f, 0f, Arena3DMeshes.MAT_METALLIC)
                // Strapped Cast-Iron Pan on Lower Pack
                emitHex(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, cx - fx * 11.6f * scaleMul + rx * 2.2f * scaleMul, torsoY - 2.6f * scaleMul, cz - fz * 11.6f * scaleMul + rz * 2.2f * scaleMul, 6.4f * scaleMul, 1.4f * scaleMul, 6.4f * scaleMul, rotDeg, 0xFF111827, 0f, 90f, 1f, 0f, Arena3DMeshes.MAT_METALLIC)
                emitHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, cx - fx * 11.6f * scaleMul + rx * 4.4f * scaleMul, torsoY - 5.6f * scaleMul, cz - fz * 11.6f * scaleMul + rz * 4.4f * scaleMul, 1.1f * scaleMul, 4.8f * scaleMul, 1.1f * scaleMul, rotDeg, 0xFF1F2937, 22f, 0f, 1f, 0f, Arena3DMeshes.MAT_METALLIC)
            }
        }
    }

    fun drawHatOnHead(
        meshes: Es3MeshSet,
        emit: (FloatBuffer, Int, Float, Float, Float, Float, Float, Float, Float, Float, Float, Float, Float, Float, Float, Float, Int) -> Unit,
        emitHex: (FloatBuffer, Int, Float, Float, Float, Float, Float, Float, Float, Long, Float, Float, Float, Float, Int) -> Unit,
        hatId: String,
        hatColorHex: Long,
        cx: Float, headY: Float, cz: Float,
        fx: Float, fz: Float, rx: Float, rz: Float,
        rotDeg: Float,
        isPlayer: Boolean,
        isTitan: Boolean = false
    ) {
        when (hatId) {
            "hat_samurai_kabuto" -> {
                emitHex(meshes.sphere, Arena3DMeshes.SPHERE_VERTEX_COUNT, cx, headY + 2.4f, cz, 12.2f, 10.6f, 12.2f, rotDeg, 0xFF7F1D1DL, 0f, 0f, 1f, 0f, Arena3DMeshes.MAT_METALLIC)
                // 3-Tier Curved Shikoro Neck Guard
                emitHex(meshes.sodePlate.buffer, meshes.sodePlate.vertexCount, cx - fx * 1.4f, headY + 1.2f, cz - fz * 1.4f, 14.5f, 4.2f, 14.5f, rotDeg + 180f, 0xFF450A0AL, 0f, 0f, 1f, 0f, Arena3DMeshes.MAT_METALLIC)
                emitHex(meshes.sodePlate.buffer, meshes.sodePlate.vertexCount, cx - fx * 1.8f, headY - 1.4f, cz - fz * 1.8f, 16.0f, 4.0f, 16.0f, rotDeg + 180f, 0xFF7F1D1DL, 0f, 0f, 1f, 0f, Arena3DMeshes.MAT_METALLIC)
                // Giant Swept Curved Kuwa-gata Samurai Demon Horns + Maedate Sun Crest
                emitHex(meshes.torus, Arena3DMeshes.TORUS_VERTEX_COUNT, cx + fx * 5.6f, headY + 4.8f, cz + fz * 5.6f, 7.6f, 7.6f, 2.4f, rotDeg + 90f, hatColorHex, 18f, 0f, 1f, 0.50f, Arena3DMeshes.MAT_METALLIC)
                emitHex(meshes.curvedHorn.buffer, meshes.curvedHorn.vertexCount, cx + fx * 3.8f - rx * 3.6f, headY + 4.6f, cz + fz * 3.8f - rz * 3.6f, 4.2f, 11.4f, 4.2f, rotDeg + 45f, hatColorHex, 0f, -18f, 1f, 0.45f, Arena3DMeshes.MAT_METALLIC)
                emitHex(meshes.curvedHorn.buffer, meshes.curvedHorn.vertexCount, cx + fx * 3.8f + rx * 3.6f, headY + 4.6f, cz + fz * 3.8f + rz * 3.6f, 4.2f, 11.4f, 4.2f, rotDeg - 45f, hatColorHex, 0f, -18f, 1f, 0.45f, Arena3DMeshes.MAT_METALLIC)
            }
            "hat_cyber_visor" -> {
                emitHex(meshes.sphere, Arena3DMeshes.SPHERE_VERTEX_COUNT, cx, headY + 1.8f, cz, 12.0f, 10.5f, 11.8f, rotDeg, 0xFF0F172AL, 0f, 0f, 1f, 0f, Arena3DMeshes.MAT_ARMOR_WEAVE)
                emitHex(meshes.sphere, Arena3DMeshes.SPHERE_VERTEX_COUNT, cx + fx * 2.8f, headY + 0.8f, cz + fz * 2.8f, 9.4f, 4.8f, 10.8f, rotDeg, hatColorHex, 0f, 0f, 0.92f, 0.68f, Arena3DMeshes.MAT_GLASS)
                emitHex(meshes.torus, Arena3DMeshes.TORUS_VERTEX_COUNT, cx + fx * 2.2f, headY + 1.0f, cz + fz * 2.2f, 11.8f, 5.0f, 11.8f, rotDeg, hatColorHex, 0f, 0f, 1f, 0.88f, Arena3DMeshes.MAT_STANDARD)
                emitHex(meshes.wing, Arena3DMeshes.SWEPT_WING_VERTEX_COUNT, cx - rx * 4.8f, headY + 3.2f, cz - rz * 4.8f, 6.2f, 5.4f, 2.2f, rotDeg, hatColorHex, 0f, 0f, 1f, 0.72f, Arena3DMeshes.MAT_METALLIC)
                emitHex(meshes.wing, Arena3DMeshes.SWEPT_WING_VERTEX_COUNT, cx + rx * 4.8f, headY + 3.2f, cz + rz * 4.8f, 6.2f, 5.4f, -2.2f, rotDeg, hatColorHex, 0f, 0f, 1f, 0.72f, Arena3DMeshes.MAT_METALLIC)
            }
            "hat_crimson_beret" -> {
                emitHex(meshes.sphere, Arena3DMeshes.SPHERE_VERTEX_COUNT, cx + rx * 1.4f, headY + 4.6f, cz + rz * 1.4f, 12.2f, 4.8f, 12.2f, rotDeg + 12f, hatColorHex, 0f, 0f, 1f, 0f, Arena3DMeshes.MAT_STANDARD)
                emitHex(meshes.sphere, Arena3DMeshes.SPHERE_VERTEX_COUNT, cx + fx * 4.5f - rx * 1.2f, headY + 4.8f, cz + fz * 4.5f - rz * 1.2f, 2.4f, 2.4f, 1.4f, rotDeg, 0xFFF5B400L, 0f, 0f, 1f, 0.45f, Arena3DMeshes.MAT_METALLIC)
                emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, cx + fx * 4.6f, headY + 1.4f, cz + fz * 4.6f, 2.4f, 3.0f, 8.8f, rotDeg, 0xFF111827, 0f, 0f, 1f, 0.15f, Arena3DMeshes.MAT_GLASS)
                emitHex(meshes.torus, Arena3DMeshes.TORUS_VERTEX_COUNT, cx, headY + 1.4f, cz, 10.4f, 2.4f, 10.4f, rotDeg, 0xFF1F2937, 0f, 0f, 1f, 0f, Arena3DMeshes.MAT_STANDARD)
            }
            else -> {
                // Iconic PUBG Level-3 Spetsnaz Titanium Helmet + Dual NVG Mount + Mandible Shield
                val shellR = if (isTitan) 0.78f else 0.22f
                val shellG = if (isTitan) 0.56f else 0.25f
                val shellB = if (isTitan) 0.10f else 0.30f
                emit(meshes.sphere, Arena3DMeshes.SPHERE_VERTEX_COUNT, cx, headY + 2.2f, cz, 12.0f, 10.6f, 11.8f, rotDeg, 0f, 0f, shellR, shellG, shellB, 1f, 0f, Arena3DMeshes.MAT_METALLIC)
                emit(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, cx + fx * 4.4f, headY - 0.2f, cz + fz * 4.4f, 3.8f, 8.6f, 10.4f, rotDeg, 0f, 0f, 0.16f, 0.19f, 0.23f, 1f, 0f, Arena3DMeshes.MAT_METALLIC)
                emitHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, cx + fx * 4.2f, headY + 5.8f, cz + fz * 4.2f, 3.2f, 2.0f, 3.4f, rotDeg, 0xFF0F172A, 0f, 0f, 1f, 0f, Arena3DMeshes.MAT_METALLIC)
                if (isPlayer) {
                    emitHex(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, cx + fx * 5.6f - rx * 1.4f, headY + 5.6f, cz + fz * 5.6f - rz * 1.4f, 1.5f, 3.2f, 1.5f, rotDeg, 0xFF1E293B, 0f, 90f, 1f, 0f, Arena3DMeshes.MAT_METALLIC)
                    emitHex(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, cx + fx * 5.6f + rx * 1.4f, headY + 5.6f, cz + fz * 5.6f + rz * 1.4f, 1.5f, 3.2f, 1.5f, rotDeg, 0xFF1E293B, 0f, 90f, 1f, 0f, Arena3DMeshes.MAT_METALLIC)
                }
                val slitHex = if (isPlayer) 0xFF00E5FFL else 0xFFFF3355L
                emitHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, cx + fx * 5.8f, headY + 0.8f, cz + fz * 5.8f, 1.6f, 7.6f, 1.6f, rotDeg, slitHex, 90f, 0f, 1f, 0.85f, Arena3DMeshes.MAT_GLASS)
            }
        }
    }

    fun bindAndDraw(buffer: FloatBuffer, vertexCount: Int) {
        val strideBytes = 6 * 4
        buffer.position(0)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, strideBytes, buffer)

        buffer.position(3)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, strideBytes, buffer)

        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, vertexCount)
    }
}
