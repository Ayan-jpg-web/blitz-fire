package com.example.ui.components

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.CatalogItem
import com.example.ui.arena.Arena3DMeshes
import com.example.ui.arena.Es3MeshSet
import com.example.ui.arena.Es3ShaderProgram
import com.example.ui.arena.Operative3DModel
import com.example.ui.arena.RenderCombatant
import com.example.ui.arena.Weapon3DModel
import com.example.ui.theme.BlazeGold
import com.example.ui.theme.ObsidianBg
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class PreviewFocusMode {
    OPERATIVE,
    WEAPON,
    ACCESSORY,
    HAT
}

private data class CharacterPreviewState(
    val userYaw: Float,
    val isDragging: Boolean,
    val focusMode: PreviewFocusMode,
    val characterId: String,
    val charAccentHex: Long,
    val outfitId: String,
    val outfitColorHex: Long,
    val hatId: String,
    val hatColorHex: Long,
    val accessoryId: String,
    val accessoryColorHex: Long,
    val weaponId: String,
    val weaponColorHex: Long,
    val attachmentId: String
)

private class CharacterPreviewEs3Renderer(
    initialState: CharacterPreviewState
) : GLSurfaceView.Renderer {

    @Volatile
    var state: CharacterPreviewState = initialState

    private val shader = Es3ShaderProgram()
    private lateinit var meshes: Es3MeshSet

    private val projMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private var viewportW = 1
    private var viewportH = 1
    private var lastNs = 0L
    private var elapsed = 0f
    private var autoSpinYaw = 0f

    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        shader.compile()
        meshes = Es3MeshSet()
    }

    override fun onSurfaceChanged(unused: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        viewportW = width.coerceAtLeast(1)
        viewportH = height.coerceAtLeast(1)
    }

    override fun onDrawFrame(unused: GL10?) {
        val now = System.nanoTime()
        if (lastNs != 0L) {
            val dt = ((now - lastNs) / 1_000_000_000f).coerceIn(0f, 0.05f)
            elapsed += dt
            if (!state.isDragging) {
                autoSpinYaw += dt * 0.60f
            }
        }
        lastNs = now

        val cur = state
        val tintHex = when (cur.focusMode) {
            PreviewFocusMode.WEAPON -> cur.weaponColorHex
            PreviewFocusMode.ACCESSORY -> cur.accessoryColorHex
            PreviewFocusMode.HAT -> cur.hatColorHex
            PreviewFocusMode.OPERATIVE -> cur.charAccentHex
        }
        val rAcc = ((tintHex shr 16) and 0xFF) / 255f
        val gAcc = ((tintHex shr 8) and 0xFF) / 255f
        val bAcc = (tintHex and 0xFF) / 255f

        val bgR = 0.05f + rAcc * 0.06f
        val bgG = 0.08f + gAcc * 0.06f
        val bgB = 0.13f + bAcc * 0.08f
        GLES30.glClearColor(bgR, bgG, bgB, 1.0f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        GLES30.glUseProgram(shader.programId)
        GLES30.glUniform3f(shader.uFogColorLoc, bgR, bgG, bgB)
        GLES30.glUniform1f(shader.uTimeLoc, elapsed)
        GLES30.glUniform1f(shader.uFogStartLoc, 600f)
        GLES30.glUniform1f(shader.uFogRangeLoc, 1200f)

        val (camY, camZ, lookY) = when (cur.focusMode) {
            PreviewFocusMode.WEAPON -> Triple(15f, 44f, 12f)
            PreviewFocusMode.HAT -> Triple(16f, 38f, 13f)
            PreviewFocusMode.ACCESSORY -> Triple(18f, 52f, 15f)
            PreviewFocusMode.OPERATIVE -> Triple(25f, 66f, 20.5f)
        }
        val camX = 0f
        GLES30.glUniform3f(shader.uViewPosLoc, camX, camY, camZ)

        Matrix.setLookAtM(viewMatrix, 0, camX, camY, camZ, 0f, lookY, 0f, 0f, 1f, 0f)
        val aspect = viewportW.toFloat() / viewportH.toFloat().coerceAtLeast(1f)
        Matrix.perspectiveM(projMatrix, 0, 38f, aspect, 2f, 400f)
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        // Multi-Tier Holographic Cyber-Armory Display Plinth
        drawPart(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, 0f, -4.4f, 0f, 46f, 2.2f, 46f, 0f, 0xFF0F172A, mat = Arena3DMeshes.MAT_ARMOR_WEAVE)
        drawPart(meshes.hull, Arena3DMeshes.CHAMFER_HULL_VERTEX_COUNT, 0f, -2.5f, 0f, 38f, 2.8f, 38f, 15f, 0xFF1E293B, mat = Arena3DMeshes.MAT_METALLIC)
        drawPart(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, 0f, -0.8f, 0f, 31f, 1.4f, 31f, 0f, 0xFF283548, mat = Arena3DMeshes.MAT_METALLIC)
        drawPart(meshes.torus, Arena3DMeshes.TORUS_VERTEX_COUNT, 0f, 0.05f, 0f, 33f, 2.0f, 33f, (elapsed * 45f) % 360f, 0xFFF5B400, emissive = 0.78f)
        drawPart(meshes.torus, Arena3DMeshes.TORUS_VERTEX_COUNT, 0f, -0.45f, 0f, 42f, 1.8f, 42f, -(elapsed * 65f) % 360f, 0xFF00E5FF, emissive = 0.68f)
        drawPart(meshes.torus, Arena3DMeshes.TORUS_VERTEX_COUNT, 0f, -2.0f, 0f, 49f, 1.4f, 49f, (elapsed * 22f) % 360f, tintHex, emissive = 0.45f)

        // 6 Perimeter Holographic Emitter Pylons + Glass Crystals
        for (i in 0 until 6) {
            val angleDeg = i * 60f
            val angleRad = Math.toRadians(angleDeg.toDouble()).toFloat()
            val px = cos(angleRad) * 21.5f
            val pz = sin(angleRad) * 21.5f
            drawPart(meshes.cylinder, Arena3DMeshes.CYLINDER_VERTEX_COUNT, px, 0.4f, pz, 2.0f, 4.6f, 2.0f, -angleDeg, 0xFF334155, mat = Arena3DMeshes.MAT_METALLIC)
            drawPart(meshes.sphere, Arena3DMeshes.SPHERE_VERTEX_COUNT, px, 3.0f, pz, 1.8f, 2.2f, 1.8f, 0f, 0xFF00E5FF, emissive = 0.85f, mat = Arena3DMeshes.MAT_GLASS)
        }

        val totalYaw = cur.userYaw + autoSpinYaw
        val bob = sin(elapsed * 2.3f) * 1.1f

        when (cur.focusMode) {
            PreviewFocusMode.WEAPON -> {
                val rotDeg = Math.toDegrees(totalYaw.toDouble()).toFloat()
                Weapon3DModel.drawWeapon3D(
                    meshes = meshes,
                    shader = shader,
                    vpMatrix = vpMatrix,
                    tempModelMatrix = modelMatrix,
                    tempMvpMatrix = mvpMatrix,
                    weaponId = cur.weaponId,
                    weaponColorHex = cur.weaponColorHex,
                    attachmentId = cur.attachmentId,
                    worldX = 0f,
                    worldY = 12.6f + bob,
                    worldZ = 0f,
                    rotYDeg = rotDeg,
                    scale = 1.48f,
                    isPlayer = true,
                    isMuzzleFlashing = false,
                    elapsed = elapsed,
                    pitchDeg = 8f,
                    rollDeg = 6f
                )
            }
            PreviewFocusMode.ACCESSORY -> {
                val angleRad = (PI * 0.5).toFloat() - totalYaw
                val rotDeg = -Math.toDegrees(angleRad.toDouble()).toFloat()
                val fx = cos(angleRad); val fz = sin(angleRad)
                val rx = -sin(angleRad); val rz = cos(angleRad)
                Operative3DModel.drawAccessoryOnBack(
                    meshes = meshes,
                    emit = ::emitRaw,
                    emitHex = ::emitHexRaw,
                    accessoryId = cur.accessoryId,
                    accessoryColorHex = cur.accessoryColorHex,
                    cx = fx * 5.0f,
                    torsoY = 14.5f + bob,
                    cz = fz * 5.0f,
                    fx = fx, fz = fz, rx = rx, rz = rz,
                    rotDeg = rotDeg,
                    elapsed = elapsed,
                    scaleMul = 1.32f
                )
            }
            PreviewFocusMode.HAT -> {
                val angleRad = (PI * 0.5).toFloat() - totalYaw
                val rotDeg = -Math.toDegrees(angleRad.toDouble()).toFloat()
                val fx = cos(angleRad); val fz = sin(angleRad)
                val rx = -sin(angleRad); val rz = cos(angleRad)
                // Translucent Holographic Mannequin Head Bust
                drawPart(meshes.capsule, Arena3DMeshes.CAPSULE_VERTEX_COUNT, 0f, 5.5f + bob, 0f, 5.4f, 8.0f, 5.4f, rotDeg, 0xFF1E293B, mat = Arena3DMeshes.MAT_METALLIC)
                drawPart(meshes.sphere, Arena3DMeshes.SPHERE_VERTEX_COUNT, 0f, 13.0f + bob, 0f, 10.6f, 11.4f, 10.6f, rotDeg, 0xFF00E5FF, emissive = 0.25f, mat = Arena3DMeshes.MAT_GLASS)
                Operative3DModel.drawHatOnHead(
                    meshes = meshes,
                    emit = ::emitRaw,
                    emitHex = ::emitHexRaw,
                    hatId = cur.hatId,
                    hatColorHex = cur.hatColorHex,
                    cx = 0f,
                    headY = 13.0f + bob,
                    cz = 0f,
                    fx = fx, fz = fz, rx = rx, rz = rz,
                    rotDeg = rotDeg,
                    isPlayer = true,
                    isTitan = cur.characterId == "char_gold_titan"
                )
            }
            PreviewFocusMode.OPERATIVE -> {
                val dummyCombatant = RenderCombatant(
                    id = 0,
                    name = "Operative",
                    x = 0f,
                    y = 0f,
                    z = 0f,
                    angleRad = (PI * 0.5).toFloat() - totalYaw,
                    hp = 200f,
                    maxHp = 200f,
                    accentHex = cur.charAccentHex,
                    walkPhase = 0f,
                    isMoving = false,
                    isMuzzleFlashing = false,
                    isCrouched = false
                )

                Operative3DModel.drawSculptedOperative3D(
                    meshes = meshes,
                    shader = shader,
                    vpMatrix = vpMatrix,
                    modelMatrix = modelMatrix,
                    mvpMatrix = mvpMatrix,
                    c = dummyCombatant,
                    outfitColorHex = cur.outfitColorHex,
                    hatId = cur.hatId,
                    hatColorHex = cur.hatColorHex,
                    accessoryId = cur.accessoryId,
                    accessoryColorHex = cur.accessoryColorHex,
                    weaponColorHex = cur.weaponColorHex,
                    hasThermalScope = cur.attachmentId == "att_thermal_4x",
                    elapsed = elapsed,
                    isPlayer = true,
                    isSeatedInVehicle = false,
                    drawShadow = true,
                    characterId = cur.characterId,
                    outfitId = cur.outfitId,
                    weaponId = cur.weaponId,
                    attachmentId = cur.attachmentId
                )
            }
        }
    }

    private fun emitRaw(
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
        Operative3DModel.bindAndDraw(buf, verts)
    }

    private fun emitHexRaw(
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
        emitRaw(buf, verts, cx, cy, cz, sx, sy, sz, ry, rxDeg, rzDeg, r, g, b, a, emissive, mat)
    }

    private fun drawPart(
        buffer: FloatBuffer,
        vertexCount: Int,
        cx: Float, cy: Float, cz: Float,
        sx: Float, sy: Float, sz: Float,
        rotYDeg: Float,
        hex: Long,
        emissive: Float = 0f,
        mat: Int = Arena3DMeshes.MAT_STANDARD
    ) {
        emitHexRaw(buffer, vertexCount, cx, cy, cz, sx, sy, sz, rotYDeg, hex, 0f, 0f, 1f, emissive, mat)
    }
}

@Composable
fun Interactive3DCharacterStage(
    character: CatalogItem,
    hat: CatalogItem?,
    outfit: CatalogItem?,
    accessory: CatalogItem?,
    weapon: CatalogItem? = null,
    attachment: CatalogItem? = null,
    focusMode: PreviewFocusMode = PreviewFocusMode.OPERATIVE,
    isDialogOverlay: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var userYaw by remember(focusMode) { mutableFloatStateOf(0.28f) }
    var isDragging by remember { mutableStateOf(false) }

    val outfitColorHex = outfit?.takeIf { it.id != "outfit_default" }?.accentHex ?: character.accentHex
    val borderColor = Color(
        when (focusMode) {
            PreviewFocusMode.WEAPON -> weapon?.accentHex ?: outfitColorHex
            PreviewFocusMode.ACCESSORY -> accessory?.accentHex ?: outfitColorHex
            PreviewFocusMode.HAT -> hat?.accentHex ?: outfitColorHex
            PreviewFocusMode.OPERATIVE -> outfitColorHex
        }
    )

    val previewState = CharacterPreviewState(
        userYaw = userYaw,
        isDragging = isDragging,
        focusMode = focusMode,
        characterId = character.id,
        charAccentHex = character.accentHex,
        outfitId = outfit?.id ?: "outfit_default",
        outfitColorHex = outfitColorHex,
        hatId = hat?.id ?: "hat_none",
        hatColorHex = hat?.accentHex ?: 0xFFF5B400L,
        accessoryId = accessory?.id ?: "acc_none",
        accessoryColorHex = accessory?.accentHex ?: 0xFF00E5FFL,
        weaponId = weapon?.id ?: "wpn_akm_default",
        weaponColorHex = weapon?.accentHex ?: 0xFF9AA6C0L,
        attachmentId = attachment?.id ?: "att_holo_sight"
    )

    val renderer = remember(isDialogOverlay) { CharacterPreviewEs3Renderer(previewState) }
    SideEffect {
        renderer.state = previewState
    }

    val glSurfaceView = remember(context, renderer, isDialogOverlay) {
        GLSurfaceView(context).apply {
            setEGLContextClientVersion(3)
            if (isDialogOverlay) {
                setZOrderMediaOverlay(true)
            }
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
    }

    DisposableEffect(lifecycleOwner, glSurfaceView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> glSurfaceView.onResume()
                Lifecycle.Event.ON_PAUSE -> glSurfaceView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            glSurfaceView.onPause()
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(ObsidianBg)
            .border(1.5.dp, borderColor.copy(alpha = 0.72f), RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        userYaw += dragAmount * 0.015f
                    }
                )
            }
    ) {
        AndroidView(
            factory = { glSurfaceView },
            modifier = Modifier.fillMaxSize()
        )

        val badgeLabel = when (focusMode) {
            PreviewFocusMode.WEAPON -> "3D WEAPON • DRAG 360°"
            PreviewFocusMode.ACCESSORY -> "3D ACCESSORY • DRAG 360°"
            PreviewFocusMode.HAT -> "3D HEADGEAR • DRAG 360°"
            PreviewFocusMode.OPERATIVE -> "OPENGL 3.0 • DRAG 360°"
        }

        Surface(
            color = ObsidianBg.copy(alpha = 0.78f),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .border(1.dp, BlazeGold.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
        ) {
            Text(
                text = badgeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = BlazeGold,
                fontSize = 9.sp,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}
