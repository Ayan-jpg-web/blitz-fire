package com.example.ui.arena

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.data.model.BattleMapId
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class Arena3DRenderer(
    private val engine: BattleEngine
) : GLSurfaceView.Renderer {

    private val shader = Es3ShaderProgram()
    private lateinit var meshes: Es3MeshSet

    private val projMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private var cachedTerrainMapId: BattleMapId? = null
    private var cachedTerrainMapSize: Float = -1f
    private var terrainMesh: Arena3DMeshes.GlMesh? = null

    private val vpLock = Any()
    private val sharedVpMatrix = FloatArray(16)
    @Volatile
    var viewportWidth: Int = 1
        private set
    @Volatile
    var viewportHeight: Int = 1
        private set

    fun projectWorldToScreen(wx: Float, wy: Float, wz: Float): Pair<Float, Float>? {
        val vp = FloatArray(16)
        synchronized(vpLock) {
            System.arraycopy(sharedVpMatrix, 0, vp, 0, 16)
        }
        val vw = viewportWidth
        val vh = viewportHeight
        if (vw <= 1 || vh <= 1) return null

        val clipX = vp[0] * wx + vp[4] * wy + vp[8] * wz + vp[12]
        val clipY = vp[1] * wx + vp[5] * wy + vp[9] * wz + vp[13]
        val clipW = vp[3] * wx + vp[7] * wy + vp[11] * wz + vp[15]
        if (clipW <= 0.1f) return null

        val ndcX = clipX / clipW
        val ndcY = clipY / clipW
        if (ndcX < -1.15f || ndcX > 1.15f || ndcY < -1.15f || ndcY > 1.15f) return null

        val sx = (ndcX * 0.5f + 0.5f) * vw
        val sy = (0.5f - ndcY * 0.5f) * vh
        return sx to sy
    }

    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        shader.compile()
        meshes = Es3MeshSet()
    }

    override fun onSurfaceChanged(unused: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        viewportWidth = width.coerceAtLeast(1)
        viewportHeight = height.coerceAtLeast(1)
    }

    override fun onDrawFrame(unused: GL10?) {
        val snap = engine.buildRenderSnapshot()

        val (fogR, fogG, fogB) = when (snap.mapId) {
            BattleMapId.ERANGEL -> Triple(0.45f, 0.56f, 0.68f)
            BattleMapId.MIRAMAR -> Triple(0.74f, 0.57f, 0.40f)
            BattleMapId.SANHOK -> Triple(0.24f, 0.44f, 0.42f)
        }
        GLES30.glClearColor(fogR, fogG, fogB, 1.0f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        GLES30.glUseProgram(shader.programId)
        GLES30.glUniform3f(shader.uFogColorLoc, fogR, fogG, fogB)
        GLES30.glUniform1f(shader.uTimeLoc, snap.elapsedSeconds)
        GLES30.glUniform1f(shader.uFogStartLoc, snap.mapSize * 0.14f)
        GLES30.glUniform1f(shader.uFogRangeLoc, snap.mapSize * 0.44f)

        val anchorX = snap.player?.x ?: (snap.mapSize * 0.5f)
        val anchorZ = snap.player?.y ?: (snap.mapSize * 0.65f)
        val stanceHeight = when {
            snap.isDriving -> 24f
            snap.isPlayerCrouched -> 20f
            else -> 28f
        }
        val anchorY = (snap.player?.z ?: 0f) + stanceHeight

        val cosYaw = cos(snap.cameraYaw)
        val sinYaw = sin(snap.cameraYaw)
        val cosPitch = cos(snap.cameraPitch)
        val sinPitch = sin(snap.cameraPitch)

        val rawDist = when {
            snap.isDriving -> 132f
            snap.isScoped -> 28f
            snap.isPlayerIndoors -> 62f
            else -> 88f
        }
        val shoulder = when {
            snap.isDriving -> 0f
            snap.isScoped -> 10f
            else -> 17f
        }
        val extraHeight = when {
            snap.isDriving -> 30f
            snap.isScoped -> 4f
            else -> 14f
        }

        val camDist = computeSafeCameraDistance(anchorX, anchorZ, cosYaw, sinYaw, shoulder, rawDist)
        val camX = anchorX - cosYaw * cosPitch * camDist - sinYaw * shoulder
        val camZ = anchorZ - sinYaw * cosPitch * camDist + cosYaw * shoulder
        val camY = (anchorY - sinPitch * camDist * 0.72f + extraHeight).coerceAtLeast(10f)

        GLES30.glUniform3f(shader.uViewPosLoc, camX, camY, camZ)

        val lookX = anchorX + cosYaw * cosPitch * 260f - sinYaw * (shoulder * 0.55f)
        val lookZ = anchorZ + sinYaw * cosPitch * 260f + cosYaw * (shoulder * 0.55f)
        val lookY = anchorY + sinPitch * 260f

        Matrix.setLookAtM(viewMatrix, 0, camX, camY, camZ, lookX, lookY, lookZ, 0f, 1f, 0f)

        val aspect = viewportWidth.toFloat() / viewportHeight.toFloat().coerceAtLeast(1f)
        val fov = when {
            snap.isScoped && snap.hasThermalScope -> 26f
            snap.isScoped -> 36f
            snap.isDriving -> 66f
            else -> 60f
        }
        val farClip = snap.mapSize * 0.95f
        Matrix.perspectiveM(projMatrix, 0, fov, aspect, 4f, farClip)
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        synchronized(vpLock) {
            System.arraycopy(vpMatrix, 0, sharedVpMatrix, 0, 16)
        }

        // 0. Atmospheric Sky Dome
        GLES30.glDepthMask(false)
        val skyScale = snap.mapSize * 0.62f
        drawMesh(
            buffer = meshes.sphere,
            vertexCount = Arena3DMeshes.SPHERE_VERTEX_COUNT,
            cx = camX, cy = camY, cz = camZ,
            sx = skyScale, sy = skyScale, sz = skyScale,
            rotYDeg = 0f, r = 1f, g = 1f, b = 1f, a = 1f,
            emissive = 0f,
            materialMode = Arena3DMeshes.MAT_SKYDOME
        )
        GLES30.glDepthMask(true)

        // 1. High-Poly 64x64 Procedural 3D Terrain Mesh + Water & Tarmac Road Network
        if (cachedTerrainMapId != snap.mapId || cachedTerrainMapSize != snap.mapSize || terrainMesh == null) {
            cachedTerrainMapId = snap.mapId
            cachedTerrainMapSize = snap.mapSize
            terrainMesh = Arena3DMeshes.createMapTerrainMesh(snap.mapId, snap.mapSize)
        }
        drawEnvironmentForMap(snap.mapId, snap.mapSize, terrainMesh)

        val playerX = snap.player?.x ?: anchorX
        val playerY = snap.player?.y ?: anchorZ
        val buildingMaxDist = snap.mapSize * 0.60f
        val propMaxDist = minOf(2400f, snap.mapSize * 0.48f)

        // 2. Enterable 4-Wall Compounds & Sculpted Landmarks
        engine.obstacles.forEach { obs ->
            val ocx = obs.x + obs.w * 0.5f
            val ocy = obs.y + obs.h * 0.5f
            if (hypot(ocx - playerX, ocy - playerY) <= buildingMaxDist) {
                val playerInsideThis = engine.isPositionInsideBuilding(obs, playerX, playerY)
                drawCompositeBuilding3D(obs, playerInsideThis, snap.elapsedSeconds)
            }
        }

        // 3. Drivable Sculpted Vehicles (Zero Boxes)
        snap.vehicles.forEach { v ->
            if (hypot(v.x - playerX, v.y - playerY) <= propMaxDist * 1.4f) {
                drawVehicle3D(v)
            }
        }

        // 4. Sculpted Foliage (Cones, Capsules, Frond Wings & Rock Boulders)
        engine.trees.forEach { tree ->
            if (hypot(tree.x - playerX, tree.y - playerY) <= propMaxDist) {
                drawFoliage3D(tree)
            }
        }

        // 5. Supply Drops (Always Visible Sky Beacons) & Chamfered Loot Crates
        snap.lootDrops.forEach { drop ->
            if (drop.isSupplyDrop || hypot(drop.x - playerX, drop.y - playerY) <= propMaxDist) {
                drawLoot3D(drop, snap.elapsedSeconds)
            }
        }

        // 6. High-Detail Per-Item Non-Box 3D Operatives
        snap.player?.let { p ->
            Operative3DModel.drawSculptedOperative3D(
                meshes = meshes,
                shader = shader,
                vpMatrix = vpMatrix,
                modelMatrix = modelMatrix,
                mvpMatrix = mvpMatrix,
                c = p,
                outfitColorHex = snap.outfitColorHex,
                hatId = snap.hatId,
                hatColorHex = snap.hatColorHex,
                accessoryId = snap.accessoryId,
                accessoryColorHex = snap.accessoryColorHex,
                weaponColorHex = snap.weaponColorHex,
                hasThermalScope = snap.hasThermalScope,
                elapsed = snap.elapsedSeconds,
                isPlayer = true,
                isSeatedInVehicle = snap.isDriving,
                characterId = snap.characterId,
                outfitId = snap.outfitId,
                weaponId = snap.weaponId,
                attachmentId = snap.attachmentId
            )
        }

        snap.bots.forEach { bot ->
            if (hypot(bot.x - playerX, bot.y - playerY) <= propMaxDist) {
                val botWeaponId = when (bot.id % 4) {
                    0 -> "wpn_akm_default"
                    1 -> "wpn_mp40_default"
                    2 -> "wpn_venom_ump"
                    else -> "wpn_scar_titan"
                }
                Operative3DModel.drawSculptedOperative3D(
                    meshes = meshes,
                    shader = shader,
                    vpMatrix = vpMatrix,
                    modelMatrix = modelMatrix,
                    mvpMatrix = mvpMatrix,
                    c = bot,
                    outfitColorHex = 0xFF3A4336,
                    hatId = if (bot.id % 5 == 0) "hat_crimson_beret" else "hat_none",
                    hatColorHex = 0xFF991B1B,
                    accessoryId = "acc_none",
                    accessoryColorHex = 0xFF334155,
                    weaponColorHex = 0xFF475569,
                    hasThermalScope = false,
                    elapsed = snap.elapsedSeconds,
                    isPlayer = false,
                    isSeatedInVehicle = false,
                    characterId = "char_phantom",
                    outfitId = "outfit_default",
                    weaponId = botWeaponId,
                    attachmentId = "att_holo_sight"
                )
            }
        }

        // 7. Pointed Warhead + Plasma Tracer Rounds
        snap.bullets.forEach { b ->
            drawBullet3D(b)
        }

        // 8. Sculpted Gloo Shields & 1KM Electric Blue Zone Wall
        GLES30.glDepthMask(false)
        snap.glooWalls.forEach { wall ->
            drawGlooWall3D(wall)
        }
        drawElectricZoneWall3D(snap.zoneCenterX, snap.zoneCenterY, snap.zoneRadius)
        GLES30.glDepthMask(true)
    }

    private fun computeSafeCameraDistance(
        ax: Float,
        az: Float,
        cosYaw: Float,
        sinYaw: Float,
        shoulder: Float,
        desiredDist: Float
    ): Float {
        var d = desiredDist
        while (d > 22f) {
            val testX = ax - cosYaw * d - sinYaw * shoulder
            val testZ = az - sinYaw * d + cosYaw * shoulder
            val blocked = engine.obstacles.any { o ->
                engine.collidesWithStructure(o, testX, testZ, 6f)
            }
            if (!blocked) return d
            d -= 10f
        }
        return 22f
    }

    private fun drawEnvironmentForMap(mapId: BattleMapId, mapSize: Float, landscape: Arena3DMeshes.GlMesh?) {
        val half = mapSize * 0.5f

        when (mapId) {
            BattleMapId.ERANGEL -> {
                drawCube(half, -5.0f, half, mapSize + 5200f, 3f, mapSize + 5200f, 0f, 0.06f, 0.27f, 0.42f, 1f, emissive = 0.04f, materialMode = Arena3DMeshes.MAT_WATER)
                drawCube(half, -2.5f, half, mapSize + 240f, 3f, mapSize + 240f, 0f, 0.66f, 0.59f, 0.44f, 1f, materialMode = Arena3DMeshes.MAT_TERRAIN)
                if (landscape != null) {
                    drawMesh(landscape.buffer, landscape.vertexCount, 0f, 0f, 0f, 1f, 1f, 1f, 0f, 0.23f, 0.36f, 0.18f, 1f, 0f, Arena3DMeshes.MAT_TERRAIN)
                } else {
                    drawCube(half, -1.5f, half, mapSize, 3f, mapSize, 0f, 0.23f, 0.35f, 0.18f, 1f, materialMode = Arena3DMeshes.MAT_TERRAIN)
                }
            }
            BattleMapId.MIRAMAR -> {
                drawCube(half, -5.0f, half, mapSize + 5200f, 3f, mapSize + 5200f, 0f, 0.56f, 0.41f, 0.26f, 1f, materialMode = Arena3DMeshes.MAT_TERRAIN)
                if (landscape != null) {
                    drawMesh(landscape.buffer, landscape.vertexCount, 0f, 0f, 0f, 1f, 1f, 1f, 0f, 0.68f, 0.52f, 0.34f, 1f, 0f, Arena3DMeshes.MAT_TERRAIN)
                } else {
                    drawCube(half, -1.5f, half, mapSize, 3f, mapSize, 0f, 0.64f, 0.49f, 0.33f, 1f, materialMode = Arena3DMeshes.MAT_TERRAIN)
                }
            }
            BattleMapId.SANHOK -> {
                drawCube(half, -5.0f, half, mapSize + 5200f, 3f, mapSize + 5200f, 0f, 0.05f, 0.32f, 0.35f, 1f, emissive = 0.06f, materialMode = Arena3DMeshes.MAT_WATER)
                if (landscape != null) {
                    drawMesh(landscape.buffer, landscape.vertexCount, 0f, 0f, 0f, 1f, 1f, 1f, 0f, 0.14f, 0.29f, 0.17f, 1f, 0f, Arena3DMeshes.MAT_TERRAIN)
                } else {
                    drawCube(half, -1.5f, half, mapSize, 3f, mapSize, 0f, 0.13f, 0.27f, 0.16f, 1f, materialMode = Arena3DMeshes.MAT_TERRAIN)
                }
                drawCube(half, -1.8f, mapSize * 0.60f, mapSize - 180f, 1.0f, 145f, 2f, 0.06f, 0.38f, 0.44f, 1f, emissive = 0.06f, materialMode = Arena3DMeshes.MAT_WATER)
            }
        }

        // Weathered Asphalt Main Crossroads + Outer Ring Highway + Curbs
        val roadCol = if (mapId == BattleMapId.MIRAMAR) 0xFF3E3832 else 0xFF23272F
        val curbCol = if (mapId == BattleMapId.MIRAMAR) 0xFF6B5C49 else 0xFF475569
        drawCubeHex(half, 0.2f, half, 96f, 0.4f, mapSize - 120f, 0f, roadCol, materialMode = Arena3DMeshes.MAT_ASPHALT)
        drawCubeHex(half, 0.24f, half, mapSize - 120f, 0.4f, 96f, 0f, roadCol, materialMode = Arena3DMeshes.MAT_ASPHALT)
        drawCubeHex(half - 50f, 0.45f, half, 3.5f, 1.0f, mapSize - 140f, 0f, curbCol)
        drawCubeHex(half + 50f, 0.45f, half, 3.5f, 1.0f, mapSize - 140f, 0f, curbCol)
        drawCubeHex(half, 0.45f, half - 50f, mapSize - 140f, 1.0f, 3.5f, 0f, curbCol)
        drawCubeHex(half, 0.45f, half + 50f, mapSize - 140f, 1.0f, 3.5f, 0f, curbCol)

        val ringMin = mapSize * 0.14f
        val ringMax = mapSize * 0.86f
        val ringLen = ringMax - ringMin + 80f
        drawCubeHex(half, 0.18f, ringMin, ringLen, 0.4f, 76f, 0f, roadCol, materialMode = Arena3DMeshes.MAT_ASPHALT)
        drawCubeHex(half, 0.18f, ringMax, ringLen, 0.4f, 76f, 0f, roadCol, materialMode = Arena3DMeshes.MAT_ASPHALT)
        drawCubeHex(ringMin, 0.18f, half, 76f, 0.4f, ringLen, 0f, roadCol, materialMode = Arena3DMeshes.MAT_ASPHALT)
        drawCubeHex(ringMax, 0.18f, half, 76f, 0.4f, ringLen, 0f, roadCol, materialMode = Arena3DMeshes.MAT_ASPHALT)

        var dash = 180f
        while (dash < mapSize - 180f) {
            drawCube(half, 0.48f, dash, 3f, 0.2f, 30f, 0f, 0.88f, 0.86f, 0.78f, 1f)
            drawCube(dash, 0.48f, half, 30f, 0.2f, 3f, 0f, 0.88f, 0.86f, 0.78f, 1f)
            dash += 140f
        }

        // Circular Central Tactical Plaza Disc + Outer Monument Ring
        val plazaCol = when (mapId) {
            BattleMapId.ERANGEL -> 0xFF39414C
            BattleMapId.MIRAMAR -> 0xFF8B7355
            BattleMapId.SANHOK -> 0xFF3F4A42
        }
        drawHex(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, half, 0.34f, half, 420f, 0.45f, 420f, 0f, plazaCol, materialMode = Arena3DMeshes.MAT_ASPHALT)
        drawHex(meshes.torus, Arena3DMeshes.TORUS_VERTEX_COUNT, half, 0.58f, half, 260f, 1.5f, 260f, 0f, 0xFFF5B400L, emissive = 0.18f)
        drawHex(meshes.torus, Arena3DMeshes.TORUS_VERTEX_COUNT, half, 0.55f, half, 412f, 2.2f, 412f, 0f, 0xFF475569L, materialMode = Arena3DMeshes.MAT_METALLIC)
    }

    private fun drawCompositeBuilding3D(o: ObstacleRect, playerInsideThis: Boolean, elapsed: Float) {
        val cx = o.x + o.w * 0.5f
        val cz = o.y + o.h * 0.5f

        // Soft Oval/Cylinder Contact Drop Shadow
        drawMesh(
            meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT,
            cx - o.height * 0.14f, 0.4f, cz - o.height * 0.08f,
            o.w + 22f, 0.5f, o.h + 22f,
            0f, 0.04f, 0.05f, 0.07f, 0.40f, 0f, Arena3DMeshes.MAT_SHADOW
        )

        when (o.structureType) {
            StructureType.COMPOUND_HOUSE, StructureType.HACIENDA_VILLA -> {
                drawEnterableWallsAndInterior(o, wallBaseY = 0f, wallHeight = o.height)
                if (!playerInsideThis) {
                    drawWedgeRoofHex(cx, o.height, cz, o.w + 12f, 30f, o.h + 12f, 0f, o.roofColorHex)
                    drawHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, cx + o.w * 0.22f, o.height + 18f, cz - o.h * 0.18f, 20f, 14f, 20f, 0f, 0xFF64748B, materialMode = Arena3DMeshes.MAT_METALLIC)
                    drawHex(meshes.torus, Arena3DMeshes.TORUS_VERTEX_COUNT, cx + o.w * 0.22f, o.height + 25f, cz - o.h * 0.18f, 13f, 5f, 13f, (elapsed * 180f) % 360f, 0xFF94A3B8, materialMode = Arena3DMeshes.MAT_METALLIC)
                }
            }

            StructureType.STILT_HUT -> {
                val sx = o.w * 0.38f
                val sz = o.h * 0.38f
                drawHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, cx - sx, 8f, cz - sz, 9f, 18f, 9f, 0f, 0xFF573820)
                drawHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, cx + sx, 8f, cz - sz, 9f, 18f, 9f, 0f, 0xFF573820)
                drawHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, cx - sx, 8f, cz + sz, 9f, 18f, 9f, 0f, 0xFF573820)
                drawHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, cx + sx, 8f, cz + sz, 9f, 18f, 9f, 0f, 0xFF573820)
                drawEnterableWallsAndInterior(o, wallBaseY = 0f, wallHeight = 56f)
                if (!playerInsideThis) {
                    drawWedgeRoofHex(cx, 56f, cz, o.w + 16f, 28f, o.h + 16f, 0f, o.roofColorHex)
                }
            }

            StructureType.CLOCK_TOWER -> {
                drawHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, cx, o.height * 0.34f, cz, o.w, o.height * 0.68f, o.h, 0f, o.wallColorHex)
                drawHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, cx, o.height * 0.80f, cz, o.w * 0.72f, o.height * 0.32f, o.h * 0.72f, 0f, 0xFF475569)
                // Glowing Clock Dial Cylinders on 4 Sides
                drawHex(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, cx, o.height * 0.82f, cz + o.h * 0.35f, 28f, 6f, 28f, 0f, 0xFFF5B400, rxDeg = 90f, emissive = 0.55f)
                drawHex(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, cx, o.height * 0.82f, cz - o.h * 0.35f, 28f, 6f, 28f, 0f, 0xFFF5B400, rxDeg = 90f, emissive = 0.55f)
                drawHex(meshes.cone, Arena3DMeshes.CONE_VERTEX_COUNT, cx, o.height * 0.96f, cz, o.w * 0.80f, 48f, o.h * 0.80f, 0f, o.roofColorHex)
                drawHex(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, cx, o.height + 54f, cz, 4f, 24f, 4f, 0f, 0xFFD1D5DB, materialMode = Arena3DMeshes.MAT_METALLIC)
                drawHex(meshes.sphere, Arena3DMeshes.SPHERE_VERTEX_COUNT, cx, o.height + 68f, cz, 9f, 9f, 9f, 0f, o.accentHex, emissive = 0.85f)
            }

            StructureType.SHIPPING_CONTAINER -> {
                drawHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, cx, o.height * 0.5f, cz, o.w, o.height, o.h, 0f, o.wallColorHex, materialMode = Arena3DMeshes.MAT_METALLIC)
                val ribs = 6
                val step = o.w / (ribs + 1)
                for (i in 1..ribs) {
                    val rx = o.x + i * step
                    drawHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, rx, o.height * 0.5f, cz, 6f, o.height - 2f, o.h + 4f, 0f, o.roofColorHex, materialMode = Arena3DMeshes.MAT_METALLIC)
                }
                drawHex(meshes.torus, Arena3DMeshes.TORUS_VERTEX_COUNT, o.x + 12f, o.height, o.y + 12f, 10f, 10f, 10f, 0f, o.accentHex, materialMode = Arena3DMeshes.MAT_METALLIC)
                drawHex(meshes.torus, Arena3DMeshes.TORUS_VERTEX_COUNT, o.x + o.w - 12f, o.height, o.y + o.h - 12f, 10f, 10f, 10f, 0f, o.accentHex, materialMode = Arena3DMeshes.MAT_METALLIC)
            }

            StructureType.WATCHTOWER -> {
                val sx = o.w * 0.36f
                val sz = o.h * 0.36f
                val stiltH = o.height * 0.62f
                drawHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, cx - sx, stiltH * 0.5f, cz - sz, 7.5f, stiltH, 7.5f, 0f, 0xFF3F4652, materialMode = Arena3DMeshes.MAT_METALLIC)
                drawHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, cx + sx, stiltH * 0.5f, cz - sz, 7.5f, stiltH, 7.5f, 0f, 0xFF3F4652, materialMode = Arena3DMeshes.MAT_METALLIC)
                drawHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, cx - sx, stiltH * 0.5f, cz + sz, 7.5f, stiltH, 7.5f, 0f, 0xFF3F4652, materialMode = Arena3DMeshes.MAT_METALLIC)
                drawHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, cx + sx, stiltH * 0.5f, cz + sz, 7.5f, stiltH, 7.5f, 0f, 0xFF3F4652, materialMode = Arena3DMeshes.MAT_METALLIC)
                drawHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, cx, stiltH + 5f, cz, o.w, 8f, o.h, 0f, 0xFF334155)
                drawHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, cx, stiltH + 17f, cz, o.w - 6f, 16f, o.h - 6f, 0f, o.wallColorHex)
                drawHex(meshes.cone, Arena3DMeshes.CONE_VERTEX_COUNT, cx, o.height - 10f, cz, o.w + 14f, 26f, o.h + 14f, 0f, o.roofColorHex)
                drawHex(meshes.sphere, Arena3DMeshes.SPHERE_VERTEX_COUNT, cx, o.height + 17f, cz, 8f, 8f, 8f, 0f, o.accentHex, emissive = 0.75f)
            }

            StructureType.OIL_DERRICK -> {
                drawHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, cx, 12f, cz, o.w, 24f, o.h, 0f, 0xFF451A03)
                drawHex(meshes.cone, Arena3DMeshes.CONE_VERTEX_COUNT, cx, 22f, cz, o.w * 0.66f, o.height * 0.85f, o.h * 0.66f, 0f, o.wallColorHex, materialMode = Arena3DMeshes.MAT_METALLIC)
                drawHex(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, cx - 32f, 36f, cz + 30f, 32f, 72f, 32f, 0f, 0xFF64748B, materialMode = Arena3DMeshes.MAT_METALLIC)
                drawHex(meshes.sphere, Arena3DMeshes.SPHERE_VERTEX_COUNT, cx - 32f, 72f, cz + 30f, 32f, 16f, 32f, 0f, 0xFF94A3B8, materialMode = Arena3DMeshes.MAT_METALLIC)
                drawHex(meshes.torus, Arena3DMeshes.TORUS_VERTEX_COUNT, cx, o.height * 0.58f, cz, o.w * 0.44f, 10f, o.h * 0.44f, 0f, o.accentHex, emissive = 0.4f, materialMode = Arena3DMeshes.MAT_METALLIC)
            }

            StructureType.TEMPLE_RUIN -> {
                drawHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, cx, 18f, cz, o.w, 36f, o.h, 0f, o.wallColorHex)
                drawHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, cx, 50f, cz, o.w * 0.74f, 30f, o.h * 0.74f, 0f, o.roofColorHex)
                drawHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, cx, 78f, cz, o.w * 0.48f, 28f, o.h * 0.48f, 0f, o.wallColorHex)
                drawPyramidHex(cx, 92f, cz, o.w * 0.46f, 32f, o.h * 0.46f, 0f, o.roofColorHex)
                val pulseRot = (elapsed * 75f) % 360f
                drawHex(meshes.torus, Arena3DMeshes.TORUS_VERTEX_COUNT, cx, 130f, cz, 26f, 12f, 26f, pulseRot, o.accentHex, rxDeg = 35f, emissive = 0.78f)
                drawHex(meshes.sphere, Arena3DMeshes.SPHERE_VERTEX_COUNT, cx, 130f, cz, 12f, 12f, 12f, 0f, o.accentHex, emissive = 0.90f)
            }

            StructureType.SANDBAG_WALL -> {
                // Sculpted stacked rounded capsule sandbags (no plain boxes!)
                val cols = 4
                val step = o.w / cols
                for (i in 0 until cols) {
                    val bx = o.x + (i + 0.5f) * step
                    drawHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, bx, 6.5f, cz, 14f, step * 0.96f, 14f, 0f, o.wallColorHex, rzDeg = 90f)
                    if (i < cols - 1) {
                        drawHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, bx + step * 0.5f, 17f, cz, 13f, step * 0.94f, 13f, 0f, o.roofColorHex, rzDeg = 90f)
                    }
                }
            }
        }
    }

    private fun drawEnterableWallsAndInterior(o: ObstacleRect, wallBaseY: Float, wallHeight: Float) {
        val cx = o.x + o.w * 0.5f
        val cz = o.y + o.h * 0.5f
        val wallT = 9f
        val doorHalfW = 24f
        val doorW = doorHalfW * 2f
        val segW = ((o.w - doorW) * 0.5f).coerceAtLeast(8f)
        val wallCenterY = wallBaseY + wallHeight * 0.5f

        // Tiled Interior Floor + Sculpted Interior Crate & Oil Drum Prop
        drawCube(cx, wallBaseY + 0.9f, cz, o.w - 4f, 1.4f, o.h - 4f, 0f, 0.30f, 0.31f, 0.34f, 1f)
        drawHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, o.x + 24f, wallBaseY + 7.5f, o.y + 24f, 16f, 13f, 16f, 12f, 0xFF78350F)
        drawHex(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, o.x + o.w - 24f, wallBaseY + 8.5f, o.y + 24f, 11f, 16f, 11f, 0f, 0xFFB91C1C, materialMode = Arena3DMeshes.MAT_METALLIC)

        // Left & Right Perimeter Walls
        drawCubeHex(o.x + wallT * 0.5f, wallCenterY, cz, wallT, wallHeight, o.h, 0f, o.wallColorHex)
        drawCubeHex(o.x + o.w - wallT * 0.5f, wallCenterY, cz, wallT, wallHeight, o.h, 0f, o.wallColorHex)

        // Back & Front Split Walls (Centered 48u Open Doorways)
        val leftSegX = o.x + segW * 0.5f
        val rightSegX = o.x + o.w - segW * 0.5f
        val backZ = o.y + wallT * 0.5f
        val frontZ = o.y + o.h - wallT * 0.5f

        drawCubeHex(leftSegX, wallCenterY, backZ, segW, wallHeight, wallT, 0f, o.wallColorHex)
        drawCubeHex(rightSegX, wallCenterY, backZ, segW, wallHeight, wallT, 0f, o.wallColorHex)
        drawCubeHex(leftSegX, wallCenterY, frontZ, segW, wallHeight, wallT, 0f, o.wallColorHex)
        drawCubeHex(rightSegX, wallCenterY, frontZ, segW, wallHeight, wallT, 0f, o.wallColorHex)

        val lintelH = (wallHeight - 36f).coerceAtLeast(6f)
        val lintelY = wallBaseY + 36f + lintelH * 0.5f
        drawCubeHex(cx, lintelY, backZ, doorW, lintelH, wallT, 0f, o.wallColorHex)
        drawCubeHex(cx, lintelY, frontZ, doorW, lintelH, wallT, 0f, o.wallColorHex)

        // Cylindrical Corner Structural Columns + Side Glass Windows + Glowing Doorway Thresholds
        val pxOff = o.w * 0.5f - 4f
        val pzOff = o.h * 0.5f - 4f
        drawHex(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, cx - pxOff, wallCenterY, cz - pzOff, 12f, wallHeight + 2f, 12f, 0f, 0xFF2D333B)
        drawHex(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, cx + pxOff, wallCenterY, cz - pzOff, 12f, wallHeight + 2f, 12f, 0f, 0xFF2D333B)
        drawHex(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, cx - pxOff, wallCenterY, cz + pzOff, 12f, wallHeight + 2f, 12f, 0f, 0xFF2D333B)
        drawHex(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, cx + pxOff, wallCenterY, cz + pzOff, 12f, wallHeight + 2f, 12f, 0f, 0xFF2D333B)
        // Side Ballistic Glass Windows & Metallic Frames
        val winY = wallBaseY + wallHeight * 0.52f
        drawHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, o.x + 2f, winY, cz, 12f, 22f, o.h * 0.34f, 0f, 0xFF38BDF8, alpha = 0.72f, emissive = 0.14f, materialMode = Arena3DMeshes.MAT_GLASS)
        drawHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, o.x + o.w - 2f, winY, cz, 12f, 22f, o.h * 0.34f, 0f, 0xFF38BDF8, alpha = 0.72f, emissive = 0.14f, materialMode = Arena3DMeshes.MAT_GLASS)
        // Roof Trim Fascia & Drum Ribs
        drawHex(meshes.torus, Arena3DMeshes.TORUS_VERTEX_COUNT, o.x + o.w - 24f, wallBaseY + 12.5f, o.y + 24f, 12.5f, 3.2f, 12.5f, 0f, 0xFFF5B400L, materialMode = Arena3DMeshes.MAT_METALLIC)
        drawHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, cx, wallBaseY + 1.4f, frontZ, 6f, doorW, 6f, 0f, o.accentHex, rzDeg = 90f, emissive = 0.25f)
        drawHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, cx, wallBaseY + 1.4f, backZ, 6f, doorW, 6f, 0f, o.accentHex, rzDeg = 90f, emissive = 0.25f)
    }

    private fun drawVehicle3D(v: RenderVehicle) {
        val rad = Math.toRadians(v.rotDeg.toDouble()).toFloat()
        val fx = cos(rad)
        val fz = sin(rad)
        val rx = -sin(rad)
        val rz = cos(rad)

        // Ground Contact Shadow
        drawMesh(
            meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT,
            v.x - 6f, 0.5f, v.y - 4f,
            66f, 0.5f, 36f, -v.rotDeg,
            0.04f, 0.05f, 0.07f, 0.55f, 0f, Arena3DMeshes.MAT_SHADOW
        )

        // 4 Sculpted Tires + Metallic Torus Wheel Rims + Fender Arches
        val wheelSpinDeg = Math.toDegrees(v.wheelSpinRad.toDouble()).toFloat()
        val wheelOffsets = listOf(
            Pair(18f, -15f), Pair(18f, 15f),
            Pair(-18f, -15f), Pair(-18f, 15f)
        )
        wheelOffsets.forEach { (fwd, side) ->
            val wx = v.x + fx * fwd + rx * side
            val wz = v.y + fz * fwd + rz * side
            drawWheelCylinder(wx, 6.8f, wz, 14.0f, 6.4f, -v.rotDeg, wheelSpinDeg, 0.07f, 0.08f, 0.09f)
            drawWheelRimTorus(wx, 6.8f, wz, 11.6f, 6.8f, -v.rotDeg, wheelSpinDeg, 0.70f, 0.74f, 0.80f)
            drawHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, wx, 13.8f, wz, 15f, 3.2f, 6.8f, -v.rotDeg, 0xFF1E293B, materialMode = Arena3DMeshes.MAT_ARMOR_WEAVE)
        }

        val headlightEmissive = if (v.isOccupied) 0.95f else 0.45f
        // Twin Glowing Red Brake Taillights
        drawHex(meshes.sphere, Arena3DMeshes.SPHERE_VERTEX_COUNT, v.x - fx * 26.5f - rx * 9f, 12.2f, v.y - fz * 26.5f - rz * 9f, 3.4f, 2.6f, 3.4f, -v.rotDeg, 0xFFEF4444L, emissive = 0.72f)
        drawHex(meshes.sphere, Arena3DMeshes.SPHERE_VERTEX_COUNT, v.x - fx * 26.5f + rx * 9f, 12.2f, v.y - fz * 26.5f + rz * 9f, 3.4f, 2.6f, 3.4f, -v.rotDeg, 0xFFEF4444L, emissive = 0.72f)

        when (v.type) {
            VehicleType.UAZ_JEEP -> {
                // Sculpted Armored Chassis + Tinted Glass Windshield/Windows + Canopy + Bullbar + Spare Tire + Hood Scoop + Headlights
                drawHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, v.x, 12.2f, v.y, 56f, 11f, 27f, -v.rotDeg, v.bodyColorHex, materialMode = Arena3DMeshes.MAT_METALLIC)
                drawHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, v.x + fx * 15f, 18.2f, v.y + fz * 15f, 14f, 2.4f, 12f, -v.rotDeg, 0xFF1F2937, materialMode = Arena3DMeshes.MAT_ARMOR_WEAVE)
                drawHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, v.x - fx * 5f, 21.8f, v.y - fz * 5f, 34f, 12f, 24f, -v.rotDeg, 0xFF2F3B2C, materialMode = Arena3DMeshes.MAT_METALLIC)
                drawHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, v.x + fx * 10.6f, 21.8f, v.y + fz * 10.6f, 4.5f, 8.2f, 21f, -v.rotDeg, 0xFF38BDF8, alpha = 0.85f, emissive = 0.16f, materialMode = Arena3DMeshes.MAT_GLASS)
                drawHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, v.x + fx * 28.5f, 10.5f, v.y + fz * 28.5f, 3.2f, 24f, 3.2f, -v.rotDeg, rxDeg = 90f, hex = 0xFF1F2937, materialMode = Arena3DMeshes.MAT_METALLIC)
                drawWheelRimTorus(v.x - fx * 27f, 16f, v.y - fz * 27f, 11.5f, 5.2f, -v.rotDeg + 90f, 0f, 0.10f, 0.12f, 0.14f)
                drawHex(meshes.sphere, Arena3DMeshes.SPHERE_VERTEX_COUNT, v.x + fx * 27f - rx * 8f, 13f, v.y + fz * 27f - rz * 8f, 4.4f, 4.4f, 4.4f, -v.rotDeg, 0xFFFDE047, emissive = headlightEmissive, materialMode = Arena3DMeshes.MAT_GLASS)
                drawHex(meshes.sphere, Arena3DMeshes.SPHERE_VERTEX_COUNT, v.x + fx * 27f + rx * 8f, 13f, v.y + fz * 27f + rz * 8f, 4.4f, 4.4f, 4.4f, -v.rotDeg, 0xFFFDE047, emissive = headlightEmissive, materialMode = Arena3DMeshes.MAT_GLASS)
            }
            VehicleType.DESERT_BUGGY -> {
                // Aerodynamic Chamfered Dune Frame + Tubular Roll Cage + Glass Wind Deflector + Twin Spoiler Wings + Exhausts
                drawHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, v.x, 10.5f, v.y, 54f, 8.5f, 25f, -v.rotDeg, v.bodyColorHex, materialMode = Arena3DMeshes.MAT_METALLIC)
                drawHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, v.x + fx * 6f, 16.5f, v.y + fz * 6f, 10f, 5.8f, 17f, -v.rotDeg, 0xFF38BDF8, alpha = 0.85f, emissive = 0.18f, materialMode = Arena3DMeshes.MAT_GLASS)
                drawHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, v.x - fx * 2f, 19.5f, v.y - fz * 2f, 2.8f, 30f, 2.8f, -v.rotDeg, rzDeg = 82f, hex = v.accentHex, materialMode = Arena3DMeshes.MAT_METALLIC)
                drawHex(meshes.wing, Arena3DMeshes.SWEPT_WING_VERTEX_COUNT, v.x - fx * 22f, 19f, v.y - fz * 22f, 12f, 10f, 22f, -v.rotDeg, v.accentHex, materialMode = Arena3DMeshes.MAT_METALLIC)
                drawHex(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, v.x - fx * 24f - rx * 5f, 12f, v.y - fz * 24f - rz * 5f, 3.2f, 12f, 3.2f, -v.rotDeg, rzDeg = 72f, hex = 0xFF94A3B8, materialMode = Arena3DMeshes.MAT_METALLIC)
                drawHex(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, v.x - fx * 24f + rx * 5f, 12f, v.y - fz * 24f + rz * 5f, 3.2f, 12f, 3.2f, -v.rotDeg, rzDeg = 72f, hex = 0xFF94A3B8, materialMode = Arena3DMeshes.MAT_METALLIC)
                drawHex(meshes.sphere, Arena3DMeshes.SPHERE_VERTEX_COUNT, v.x + fx * 25f - rx * 6f, 11.5f, v.y + fz * 25f - rz * 6f, 4.2f, 4.2f, 4.2f, -v.rotDeg, 0xFFFDE047, emissive = headlightEmissive, materialMode = Arena3DMeshes.MAT_GLASS)
                drawHex(meshes.sphere, Arena3DMeshes.SPHERE_VERTEX_COUNT, v.x + fx * 25f + rx * 6f, 11.5f, v.y + fz * 25f + rz * 6f, 4.2f, 4.2f, 4.2f, -v.rotDeg, 0xFFFDE047, emissive = headlightEmissive, materialMode = Arena3DMeshes.MAT_GLASS)
            }
            VehicleType.PATROL_TRUCK -> {
                // Beveled Armored Chassis + Forward Cab + Glass Windshield + Tubular Bed Rails + Mounted Comms Antenna + Lights
                drawHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, v.x, 11.5f, v.y, 60f, 10f, 28f, -v.rotDeg, v.bodyColorHex, materialMode = Arena3DMeshes.MAT_METALLIC)
                drawHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, v.x + fx * 11f, 21f, v.y + fz * 11f, 22f, 12.5f, 25f, -v.rotDeg, v.bodyColorHex, materialMode = Arena3DMeshes.MAT_METALLIC)
                drawHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, v.x + fx * 20.5f, 21.5f, v.y + fz * 20.5f, 4f, 8f, 21f, -v.rotDeg, 0xFF38BDF8, alpha = 0.85f, emissive = 0.16f, materialMode = Arena3DMeshes.MAT_GLASS)
                drawHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, v.x - fx * 14f - rx * 11f, 19f, v.y - fz * 14f - rz * 11f, 2.4f, 24f, 2.4f, -v.rotDeg, rzDeg = 90f, hex = 0xFF94A3B8, materialMode = Arena3DMeshes.MAT_METALLIC)
                drawHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, v.x - fx * 14f + rx * 11f, 19f, v.y - fz * 14f + rz * 11f, 2.4f, 24f, 2.4f, -v.rotDeg, rzDeg = 90f, hex = 0xFF94A3B8, materialMode = Arena3DMeshes.MAT_METALLIC)
                drawHex(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, v.x + fx * 5f - rx * 8f, 31f, v.y + fz * 5f - rz * 8f, 1.0f, 16f, 1.0f, -v.rotDeg, 0xFFCBD5E1, materialMode = Arena3DMeshes.MAT_METALLIC)
                drawHex(meshes.sphere, Arena3DMeshes.SPHERE_VERTEX_COUNT, v.x + fx * 28f - rx * 8f, 12f, v.y + fz * 28f - rz * 8f, 4.5f, 4.5f, 4.5f, -v.rotDeg, 0xFFFDE047, emissive = headlightEmissive, materialMode = Arena3DMeshes.MAT_GLASS)
                drawHex(meshes.sphere, Arena3DMeshes.SPHERE_VERTEX_COUNT, v.x + fx * 28f + rx * 8f, 12f, v.y + fz * 28f + rz * 8f, 4.5f, 4.5f, 4.5f, -v.rotDeg, 0xFFFDE047, emissive = headlightEmissive, materialMode = Arena3DMeshes.MAT_GLASS)
            }
        }
    }

    private fun drawFoliage3D(t: TreeProp) {
        val s = t.scale
        drawMesh(
            meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT,
            t.x - 5f * s, 0.4f, t.y - 4f * s,
            26f * s, 0.5f, 26f * s, 0f,
            0.04f, 0.06f, 0.04f, 0.45f, 0f, Arena3DMeshes.MAT_SHADOW
        )

        when (t.style) {
            FoliageStyle.PINE -> {
                // Sculpted tapered trunk + root flare + 4 stacked high-poly needle foliage cones
                drawMesh(meshes.cone, Arena3DMeshes.CONE_VERTEX_COUNT, t.x, 0f, t.y, 14f * s, 44f * s, 14f * s, 0f, 0.34f, 0.22f, 0.13f, 1f, 0f, Arena3DMeshes.MAT_STANDARD)
                drawMesh(meshes.cone, Arena3DMeshes.CONE_VERTEX_COUNT, t.x, 18f * s, t.y, 48f * s, 30f * s, 48f * s, 0f, 0.11f, 0.34f, 0.16f, 1f, 0f, Arena3DMeshes.MAT_STANDARD)
                drawMesh(meshes.cone, Arena3DMeshes.CONE_VERTEX_COUNT, t.x, 32f * s, t.y, 38f * s, 28f * s, 38f * s, 14f, 0.15f, 0.42f, 0.20f, 1f, 0f, Arena3DMeshes.MAT_STANDARD)
                drawMesh(meshes.cone, Arena3DMeshes.CONE_VERTEX_COUNT, t.x, 45f * s, t.y, 28f * s, 26f * s, 28f * s, 28f, 0.19f, 0.49f, 0.23f, 1f, 0f, Arena3DMeshes.MAT_STANDARD)
                drawMesh(meshes.cone, Arena3DMeshes.CONE_VERTEX_COUNT, t.x, 56f * s, t.y, 17f * s, 22f * s, 17f * s, 42f, 0.24f, 0.56f, 0.26f, 1f, 0f, Arena3DMeshes.MAT_STANDARD)
            }
            FoliageStyle.DESERT_SCRUB -> {
                // Smooth capsule saguaro cactus with curved capsule arms + multi-rock desert boulder cluster
                drawHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, t.x, 22f * s, t.y, 8.5f * s, 44f * s, 8.5f * s, 0f, 0xFF38763A)
                drawHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, t.x - 8f * s, 24f * s, t.y, 5.2f * s, 16f * s, 5.2f * s, 0f, 0xFF38763A, rzDeg = 78f)
                drawHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, t.x - 13f * s, 31f * s, t.y, 5.2f * s, 16f * s, 5.2f * s, 0f, 0xFF38763A)
                drawHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, t.x + 8f * s, 21f * s, t.y, 4.8f * s, 14f * s, 4.8f * s, 0f, 0xFF38763A, rzDeg = -75f)
                drawHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, t.x + 12f * s, 5f * s, t.y + 8f * s, 16f * s, 10f * s, 13f * s, 28f, 0xFF8C7254)
                drawHex(meshes.sphere, Arena3DMeshes.SPHERE_VERTEX_COUNT, t.x - 10f * s, 3.2f * s, t.y - 7f * s, 11f * s, 7f * s, 9f * s, 0f, 0xFFA28562)
            }
            FoliageStyle.JUNGLE_PALM -> {
                // Curved segmented palm trunk + coconut cluster + 8 multi-angle swept palm fronds
                drawHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, t.x, 16f * s, t.y, 8f * s, 32f * s, 8f * s, 0f, 0xFF6B4828, rzDeg = 6f)
                drawHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, t.x - 2.5f * s, 38f * s, t.y, 6.5f * s, 28f * s, 6.5f * s, 0f, 0xFF7A5330, rzDeg = -5f)
                drawMesh(meshes.sphere, Arena3DMeshes.SPHERE_VERTEX_COUNT, t.x - 1.5f * s, 52f * s, t.y, 12f * s, 10f * s, 12f * s, 0f, 0.36f, 0.24f, 0.12f, 1f, 0f, Arena3DMeshes.MAT_STANDARD)
                for (i in 0 until 8) {
                    val aDeg = i * 45f
                    drawMesh(
                        meshes.wing, Arena3DMeshes.SWEPT_WING_VERTEX_COUNT,
                        t.x - 1.5f * s, 53f * s, t.y,
                        28f * s, 12f * s, 35f * s,
                        aDeg, 0.14f, 0.54f, 0.24f, 1f, 0f, Arena3DMeshes.MAT_STANDARD,
                        rxDeg = if (i % 2 == 0) 16f else 26f
                    )
                }
            }
        }
    }

    private fun drawLoot3D(drop: RenderLoot, elapsed: Float) {
        val spin = (elapsed * 90f) % 360f
        if (drop.isSupplyDrop) {
            drawMesh(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, drop.x, 0.5f, drop.y, 28f, 0.6f, 28f, spin, 0.05f, 0.05f, 0.06f, 0.55f, 0f, Arena3DMeshes.MAT_SHADOW)
            drawHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, drop.x, 10f, drop.y, 22f, 18f, 22f, spin, 0xFFDC2626, materialMode = Arena3DMeshes.MAT_METALLIC)
            drawHex(meshes.torus, Arena3DMeshes.TORUS_VERTEX_COUNT, drop.x, 19.5f, drop.y, 26f, 8f, 26f, -spin * 1.4f, 0xFFF5B400, emissive = 0.65f)
            drawMesh(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, drop.x, 160f, drop.y, 5f, 300f, 5f, spin, 1f, 0.2f, 0.35f, 0.26f, 0.9f, Arena3DMeshes.MAT_STANDARD)
        } else {
            val bob = sin(elapsed * 3.5f + drop.x * 0.05f) * 2.5f + 9f
            val hex = when (drop.type) {
                LootType.MEDKIT -> 0xFF10E88AL
                LootType.GLOO_PACK -> 0xFF00E5FFL
                LootType.SHIELD_VEST -> 0xFF3B82F6L
                LootType.AMMO_CRATE -> 0xFFF5B400L
            }
            drawHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, drop.x, bob, drop.y, 13f, 9f, 13f, spin, hex, rxDeg = 14f, emissive = 0.52f)
            drawHex(meshes.torus, Arena3DMeshes.TORUS_VERTEX_COUNT, drop.x, 1.5f, drop.y, 18f, 4f, 18f, spin, hex, emissive = 0.45f)
        }
    }

    private fun drawBullet3D(b: RenderBullet) {
        val yawDeg = -Math.toDegrees(atan2(b.vy.toDouble(), b.vx.toDouble())).toFloat()
        drawHex(
            meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT,
            b.x, b.z, b.y,
            sx = 2.6f,
            sy = if (b.isHeadshot) 16f else 11f,
            sz = 2.6f,
            rotYDeg = yawDeg,
            rzDeg = -90f,
            hex = b.colorHex,
            emissive = 0.95f
        )
    }

    private fun drawGlooWall3D(w: RenderGloo) {
        val rotDeg = -Math.toDegrees(w.angleRad.toDouble()).toFloat()
        val alpha = 0.50f + 0.35f * w.hpRatio
        drawHex(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, w.x, 24f, w.y, 12f, 48f, 70f, rotDeg, w.colorHex, alpha = alpha, emissive = 0.45f)
        drawHex(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, w.x, 47f, w.y, 8f, 70f, 8f, rotDeg, rxDeg = 90f, hex = 0xFFFFFFFFL, alpha = 0.85f, emissive = 0.75f)
    }

    private fun drawElectricZoneWall3D(zx: Float, zy: Float, radius: Float) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, zx, 0f, zy)
        Matrix.scaleM(modelMatrix, 0, radius, 210f, radius)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

        GLES30.glUniformMatrix4fv(shader.uMVPMatrixLoc, 1, false, mvpMatrix, 0)
        GLES30.glUniformMatrix4fv(shader.uModelMatrixLoc, 1, false, modelMatrix, 0)
        GLES30.glUniform4f(shader.uColorLoc, 0.18f, 0.56f, 1.0f, 0.24f)
        GLES30.glUniform1f(shader.uEmissiveLoc, 0.85f)
        GLES30.glUniform1i(shader.uMaterialModeLoc, Arena3DMeshes.MAT_STANDARD)

        Operative3DModel.bindAndDraw(meshes.zoneWall, Arena3DMeshes.ZONE_WALL_VERTEX_COUNT)
    }

    private fun drawWheelCylinder(
        cx: Float, cy: Float, cz: Float,
        diameter: Float, width: Float,
        carRotYDeg: Float, spinDeg: Float,
        r: Float, g: Float, b: Float
    ) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, cx, cy, cz)
        if (carRotYDeg != 0f) Matrix.rotateM(modelMatrix, 0, carRotYDeg, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, 90f, 1f, 0f, 0f)
        if (spinDeg != 0f) Matrix.rotateM(modelMatrix, 0, spinDeg, 0f, 1f, 0f)
        Matrix.scaleM(modelMatrix, 0, diameter, width, diameter)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

        GLES30.glUniformMatrix4fv(shader.uMVPMatrixLoc, 1, false, mvpMatrix, 0)
        GLES30.glUniformMatrix4fv(shader.uModelMatrixLoc, 1, false, modelMatrix, 0)
        GLES30.glUniform4f(shader.uColorLoc, r, g, b, 1f)
        GLES30.glUniform1f(shader.uEmissiveLoc, 0f)
        GLES30.glUniform1i(shader.uMaterialModeLoc, Arena3DMeshes.MAT_STANDARD)

        Operative3DModel.bindAndDraw(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT)
    }

    private fun drawWheelRimTorus(
        cx: Float, cy: Float, cz: Float,
        diameter: Float, width: Float,
        carRotYDeg: Float, spinDeg: Float,
        r: Float, g: Float, b: Float
    ) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, cx, cy, cz)
        if (carRotYDeg != 0f) Matrix.rotateM(modelMatrix, 0, carRotYDeg, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, 90f, 1f, 0f, 0f)
        if (spinDeg != 0f) Matrix.rotateM(modelMatrix, 0, spinDeg, 0f, 1f, 0f)
        Matrix.scaleM(modelMatrix, 0, diameter, width, diameter)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

        GLES30.glUniformMatrix4fv(shader.uMVPMatrixLoc, 1, false, mvpMatrix, 0)
        GLES30.glUniformMatrix4fv(shader.uModelMatrixLoc, 1, false, modelMatrix, 0)
        GLES30.glUniform4f(shader.uColorLoc, r, g, b, 1f)
        GLES30.glUniform1f(shader.uEmissiveLoc, 0.08f)
        GLES30.glUniform1i(shader.uMaterialModeLoc, Arena3DMeshes.MAT_METALLIC)

        Operative3DModel.bindAndDraw(meshes.torus, Arena3DMeshes.TORUS_VERTEX_COUNT)
    }

    private fun drawCubeHex(
        cx: Float, cy: Float, cz: Float,
        sx: Float, sy: Float, sz: Float,
        rotYDeg: Float, hex: Long,
        alpha: Float = 1f, emissive: Float = 0f,
        materialMode: Int = Arena3DMeshes.MAT_STANDARD
    ) {
        val r = ((hex shr 16) and 0xFF) / 255f
        val g = ((hex shr 8) and 0xFF) / 255f
        val b = (hex and 0xFF) / 255f
        drawCube(cx, cy, cz, sx, sy, sz, rotYDeg, r, g, b, alpha, emissive, materialMode)
    }

    private fun drawCube(
        cx: Float, cy: Float, cz: Float,
        sx: Float, sy: Float, sz: Float,
        rotYDeg: Float, r: Float, g: Float, b: Float, a: Float,
        emissive: Float = 0f,
        materialMode: Int = Arena3DMeshes.MAT_STANDARD
    ) = drawMesh(meshes.cube, Arena3DMeshes.CUBE_VERTEX_COUNT, cx, cy, cz, sx, sy, sz, rotYDeg, r, g, b, a, emissive, materialMode)

    private fun drawPyramidHex(
        cx: Float, baseY: Float, cz: Float,
        sx: Float, sy: Float, sz: Float,
        rotYDeg: Float, hex: Long, emissive: Float = 0f,
        materialMode: Int = Arena3DMeshes.MAT_STANDARD
    ) {
        val r = ((hex shr 16) and 0xFF) / 255f
        val g = ((hex shr 8) and 0xFF) / 255f
        val b = (hex and 0xFF) / 255f
        drawMesh(meshes.pyramid, Arena3DMeshes.PYRAMID_VERTEX_COUNT, cx, baseY, cz, sx, sy, sz, rotYDeg, r, g, b, 1f, emissive, materialMode)
    }

    private fun drawWedgeRoofHex(
        cx: Float, baseY: Float, cz: Float,
        sx: Float, sy: Float, sz: Float,
        rotYDeg: Float, hex: Long, emissive: Float = 0f
    ) {
        val r = ((hex shr 16) and 0xFF) / 255f
        val g = ((hex shr 8) and 0xFF) / 255f
        val b = (hex and 0xFF) / 255f
        drawMesh(meshes.wedge, Arena3DMeshes.WEDGE_VERTEX_COUNT, cx, baseY, cz, sx, sy, sz, rotYDeg, r, g, b, 1f, emissive, Arena3DMeshes.MAT_STANDARD)
    }

    private fun drawHex(
        buffer: FloatBuffer, vertexCount: Int,
        cx: Float, cy: Float, cz: Float,
        sx: Float, sy: Float, sz: Float,
        rotYDeg: Float, hex: Long,
        rxDeg: Float = 0f, rzDeg: Float = 0f,
        alpha: Float = 1f, emissive: Float = 0f,
        materialMode: Int = Arena3DMeshes.MAT_STANDARD
    ) {
        val r = ((hex shr 16) and 0xFF) / 255f
        val g = ((hex shr 8) and 0xFF) / 255f
        val b = (hex and 0xFF) / 255f
        drawMesh(buffer, vertexCount, cx, cy, cz, sx, sy, sz, rotYDeg, r, g, b, alpha, emissive, materialMode, rxDeg, rzDeg)
    }

    private fun drawMesh(
        buffer: FloatBuffer, vertexCount: Int,
        cx: Float, cy: Float, cz: Float,
        sx: Float, sy: Float, sz: Float,
        rotYDeg: Float,
        r: Float, g: Float, b: Float, a: Float,
        emissive: Float,
        materialMode: Int,
        rxDeg: Float = 0f,
        rzDeg: Float = 0f
    ) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, cx, cy, cz)
        if (rotYDeg != 0f) Matrix.rotateM(modelMatrix, 0, rotYDeg, 0f, 1f, 0f)
        if (rxDeg != 0f) Matrix.rotateM(modelMatrix, 0, rxDeg, 1f, 0f, 0f)
        if (rzDeg != 0f) Matrix.rotateM(modelMatrix, 0, rzDeg, 0f, 0f, 1f)
        Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

        GLES30.glUniformMatrix4fv(shader.uMVPMatrixLoc, 1, false, mvpMatrix, 0)
        GLES30.glUniformMatrix4fv(shader.uModelMatrixLoc, 1, false, modelMatrix, 0)
        GLES30.glUniform4f(shader.uColorLoc, r, g, b, a)
        GLES30.glUniform1f(shader.uEmissiveLoc, emissive)
        GLES30.glUniform1i(shader.uMaterialModeLoc, materialMode)

        Operative3DModel.bindAndDraw(buffer, vertexCount)
    }
}
