package com.example.ui.arena

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

internal object Arena3DMeshes {
    const val MAT_STANDARD = 0
    const val MAT_TERRAIN = 1
    const val MAT_ASPHALT = 2
    const val MAT_WATER = 3
    const val MAT_METALLIC = 4
    const val MAT_SHADOW = 5
    const val MAT_SKYDOME = 6
    const val MAT_ARMOR_WEAVE = 7
    const val MAT_GLASS = 8

    data class GlMesh(val buffer: FloatBuffer, val vertexCount: Int)

    const val CUBE_VERTEX_COUNT = 36
    const val PYRAMID_VERTEX_COUNT = 18
    const val WEDGE_VERTEX_COUNT = 24
    const val CYLINDER_SEGMENTS = 36
    const val CYLINDER_VERTEX_COUNT = CYLINDER_SEGMENTS * 12
    const val CONE_SEGMENTS = 32
    const val CONE_VERTEX_COUNT = CONE_SEGMENTS * 6
    const val CAPSULE_SEGMENTS = 28
    const val CAPSULE_RINGS = 7
    const val CAPSULE_VERTEX_COUNT = (CAPSULE_SEGMENTS * 6) + (2 * CAPSULE_RINGS * CAPSULE_SEGMENTS * 6)
    const val TORUS_MAJOR = 40
    const val TORUS_MINOR = 18
    const val TORUS_VERTEX_COUNT = TORUS_MAJOR * TORUS_MINOR * 6
    const val ZONE_WALL_SEGMENTS = 96
    const val ZONE_WALL_VERTEX_COUNT = ZONE_WALL_SEGMENTS * 6
    const val SPHERE_STACKS = 24
    const val SPHERE_SLICES = 36
    const val SPHERE_VERTEX_COUNT = SPHERE_STACKS * SPHERE_SLICES * 6

    // High-polygon sculpted anatomical torso (6 rings x 20 segments = 708 verts)
    const val TORSO_ARMOR_SEGMENTS = 20
    const val TORSO_ARMOR_RINGS = 6
    const val TORSO_ARMOR_VERTEX_COUNT = ((TORSO_ARMOR_RINGS - 1) * TORSO_ARMOR_SEGMENTS * 6) + (2 * (TORSO_ARMOR_SEGMENTS - 2) * 3)

    // High-polygon 4-ring x 12-segment chamfered aerodynamic hull (276 verts)
    const val CHAMFER_HULL_SEGMENTS = 12
    const val CHAMFER_HULL_RINGS = 4
    const val CHAMFER_HULL_VERTEX_COUNT = ((CHAMFER_HULL_RINGS - 1) * CHAMFER_HULL_SEGMENTS * 6) + (2 * (CHAMFER_HULL_SEGMENTS - 2) * 3)

    // Multi-segment cambered airfoil wing/feather blade (64 triangles? Let's compute: top 6 tris + bot 6 tris + side 16 tris = 28 tris = 84 verts)
    const val SWEPT_WING_VERTEX_COUNT = 84

    val VERTEX_SHADER_ES3 = """
        #version 300 es
        uniform mat4 uMVPMatrix;
        uniform mat4 uModelMatrix;
        layout(location = 0) in vec3 aPosition;
        layout(location = 1) in vec3 aNormal;
        out vec3 vWorldNormal;
        out vec3 vWorldPos;
        out float vDepth;
        void main() {
            vec4 worldPos = uModelMatrix * vec4(aPosition, 1.0);
            vec4 clipPos = uMVPMatrix * vec4(aPosition, 1.0);
            gl_Position = clipPos;
            vWorldPos = worldPos.xyz;
            mat3 normalMat = transpose(inverse(mat3(uModelMatrix)));
            vWorldNormal = normalMat * aNormal;
            vDepth = clipPos.w;
        }
    """.trimIndent()

    val FRAGMENT_SHADER_ES3 = """
        #version 300 es
        precision mediump float;
        uniform vec4 uColor;
        uniform float uEmissive;
        uniform vec3 uFogColor;
        uniform vec3 uViewPos;
        uniform float uTime;
        uniform int uMaterialMode;
        uniform float uFogStart;
        uniform float uFogRange;
        in vec3 vWorldNormal;
        in vec3 vWorldPos;
        in float vDepth;
        out vec4 fragColor;

        float hash21(vec2 p) {
            p = fract(p * vec2(123.34, 456.21));
            p += dot(p, p + 45.32);
            return fract(p.x * p.y);
        }

        void main() {
            if (uMaterialMode == 6) {
                vec3 dir = normalize(vWorldPos - uViewPos);
                float h = clamp(dir.y * 1.45, 0.0, 1.0);
                vec3 zenith = uFogColor * vec3(0.42, 0.58, 0.96);
                vec3 sky = mix(uFogColor * 1.08, zenith, pow(h, 0.62));
                vec3 sunDir = normalize(vec3(0.55, 0.48, 0.34));
                float sunAmt = max(dot(dir, sunDir), 0.0);
                sky += vec3(1.0, 0.88, 0.65) * (pow(sunAmt, 36.0) * 0.55 + pow(sunAmt, 320.0) * 0.95);
                fragColor = vec4(sky, 1.0);
                return;
            }

            if (uMaterialMode == 5) {
                float fog = clamp((vDepth - uFogStart) / uFogRange, 0.0, 0.85);
                fragColor = vec4(uColor.rgb, uColor.a * (1.0 - fog));
                return;
            }

            vec3 n = normalize(vWorldNormal);
            vec3 sunDir = normalize(vec3(0.54, 0.78, 0.36));
            vec3 rimFillDir = normalize(vec3(-0.62, 0.45, -0.58));
            vec3 viewDir = normalize(uViewPos - vWorldPos);
            vec3 halfDir = normalize(sunDir + viewDir);
            vec3 halfRim = normalize(rimFillDir + viewDir);

            vec3 baseRgb = uColor.rgb;
            float specPower = 26.0;
            float specStrength = 0.16;
            float rimBoost = 0.18;

            if (uMaterialMode == 1) {
                float n1 = sin(vWorldPos.x * 0.018) * cos(vWorldPos.z * 0.021);
                float n2 = sin(vWorldPos.x * 0.075 + vWorldPos.z * 0.068);
                float micro = hash21(floor(vWorldPos.xz * 0.35)) - 0.5;
                baseRgb *= (1.0 + n1 * 0.095 + n2 * 0.05 + micro * 0.05);
                specStrength = 0.04;
                rimBoost = 0.06;
            } else if (uMaterialMode == 2) {
                float grain = hash21(floor(vWorldPos.xz * 1.0)) - 0.5;
                baseRgb *= (1.0 + grain * 0.14);
                specPower = 28.0;
                specStrength = 0.15;
                rimBoost = 0.08;
            } else if (uMaterialMode == 3) {
                float w1 = sin(vWorldPos.x * 0.06 + uTime * 2.4) * cos(vWorldPos.z * 0.055 - uTime * 1.9);
                float w2 = sin((vWorldPos.x + vWorldPos.z) * 0.10 - uTime * 3.1);
                n = normalize(n + vec3(w1 * 0.18, 0.0, w2 * 0.18));
                baseRgb += vec3(0.03, 0.08, 0.11) * (0.5 + 0.5 * w1);
                specPower = 72.0;
                specStrength = 0.72;
            } else if (uMaterialMode == 4) {
                float brushed = sin(vWorldPos.y * 1.4 + vWorldPos.x * 0.6) * 0.04;
                baseRgb *= (1.0 + brushed);
                specPower = 68.0;
                specStrength = 0.74;
                rimBoost = 0.36;
            } else if (uMaterialMode == 7) {
                // Tactical Carbon Kevlar Micro-Weave
                float weaveX = sin(vWorldPos.x * 1.9 + vWorldPos.y * 1.9);
                float weaveZ = cos(vWorldPos.z * 1.9 - vWorldPos.y * 1.9);
                float pattern = smoothstep(-0.3, 0.8, weaveX * weaveZ);
                baseRgb *= (0.90 + pattern * 0.16);
                specPower = 40.0;
                specStrength = 0.34;
                rimBoost = 0.28;
            } else if (uMaterialMode == 8) {
                // Reflective Ballistic Glass / Visor / Windshield
                specPower = 110.0;
                specStrength = 0.95;
                rimBoost = 0.52;
                baseRgb = mix(baseRgb, vec3(0.55, 0.85, 1.0), 0.22);
            } else {
                float tex = sin(vWorldPos.x * 0.16 + vWorldPos.y * 0.22 + vWorldPos.z * 0.16) * 0.035;
                baseRgb *= (1.0 + tex);
            }

            float ndotl = max(dot(n, sunDir), 0.0);
            float ndotRim = max(dot(n, rimFillDir), 0.0);
            float keySpec = pow(max(dot(n, halfDir), 0.0), specPower) * specStrength;
            float backSpec = pow(max(dot(n, halfRim), 0.0), specPower * 0.6) * (specStrength * 0.45);
            float fresnel = pow(1.0 - max(dot(n, viewDir), 0.0), 2.5) * rimBoost;
            float hemi = 0.40 + 0.22 * clamp(n.y, -1.0, 1.0);
            float ambientOcc = 0.88 + 0.12 * clamp(n.y, 0.0, 1.0);
            float light = min(1.48, (hemi + ndotl * 0.70) * ambientOcc + uEmissive);

            vec3 fillLight = vec3(0.32, 0.62, 0.92) * (ndotRim * 0.22);
            vec3 rimColor = mix(uFogColor, vec3(0.45, 0.82, 1.0), 0.35);
            vec3 lit = baseRgb * light
                + baseRgb * fillLight
                + vec3(1.0, 0.96, 0.88) * keySpec
                + vec3(0.65, 0.88, 1.0) * backSpec
                + rimColor * fresnel;

            float fog = clamp((vDepth - uFogStart) / uFogRange, 0.0, 0.74);
            vec3 finalRgb = mix(lit, uFogColor, fog * (1.0 - clamp(uEmissive, 0.0, 1.0)));
            fragColor = vec4(finalRgb, uColor.a);
        }
    """.trimIndent()

    fun createCubeBuffer(): FloatBuffer {
        val d = floatArrayOf(
            -0.5f, -0.5f, 0.5f, 0f, 0f, 1f, 0.5f, -0.5f, 0.5f, 0f, 0f, 1f, 0.5f, 0.5f, 0.5f, 0f, 0f, 1f,
            -0.5f, -0.5f, 0.5f, 0f, 0f, 1f, 0.5f, 0.5f, 0.5f, 0f, 0f, 1f, -0.5f, 0.5f, 0.5f, 0f, 0f, 1f,
            0.5f, -0.5f, -0.5f, 0f, 0f, -1f, -0.5f, -0.5f, -0.5f, 0f, 0f, -1f, -0.5f, 0.5f, -0.5f, 0f, 0f, -1f,
            0.5f, -0.5f, -0.5f, 0f, 0f, -1f, -0.5f, 0.5f, -0.5f, 0f, 0f, -1f, 0.5f, 0.5f, -0.5f, 0f, 0f, -1f,
            0.5f, -0.5f, 0.5f, 1f, 0f, 0f, 0.5f, -0.5f, -0.5f, 1f, 0f, 0f, 0.5f, 0.5f, -0.5f, 1f, 0f, 0f,
            0.5f, -0.5f, 0.5f, 1f, 0f, 0f, 0.5f, 0.5f, -0.5f, 1f, 0f, 0f, 0.5f, 0.5f, 0.5f, 1f, 0f, 0f,
            -0.5f, -0.5f, -0.5f, -1f, 0f, 0f, -0.5f, -0.5f, 0.5f, -1f, 0f, 0f, -0.5f, 0.5f, 0.5f, -1f, 0f, 0f,
            -0.5f, -0.5f, -0.5f, -1f, 0f, 0f, -0.5f, 0.5f, 0.5f, -1f, 0f, 0f, -0.5f, 0.5f, -0.5f, -1f, 0f, 0f,
            -0.5f, 0.5f, 0.5f, 0f, 1f, 0f, 0.5f, 0.5f, 0.5f, 0f, 1f, 0f, 0.5f, 0.5f, -0.5f, 0f, 1f, 0f,
            -0.5f, 0.5f, 0.5f, 0f, 1f, 0f, 0.5f, 0.5f, -0.5f, 0f, 1f, 0f, -0.5f, 0.5f, -0.5f, 0f, 1f, 0f,
            -0.5f, -0.5f, -0.5f, 0f, -1f, 0f, 0.5f, -0.5f, -0.5f, 0f, -1f, 0f, 0.5f, -0.5f, 0.5f, 0f, -1f, 0f,
            -0.5f, -0.5f, -0.5f, 0f, -1f, 0f, 0.5f, -0.5f, 0.5f, 0f, -1f, 0f, -0.5f, -0.5f, 0.5f, 0f, -1f, 0f
        )
        return toFloatBuffer(d)
    }

    fun createPyramidBuffer(): FloatBuffer {
        val list = ArrayList<Float>(PYRAMID_VERTEX_COUNT * 6)
        triHelper(list, -0.5f, 0f, 0.5f, 0.5f, 0f, 0.5f, 0f, 1f, 0f)
        triHelper(list, 0.5f, 0f, 0.5f, 0.5f, 0f, -0.5f, 0f, 1f, 0f)
        triHelper(list, 0.5f, 0f, -0.5f, -0.5f, 0f, -0.5f, 0f, 1f, 0f)
        triHelper(list, -0.5f, 0f, -0.5f, -0.5f, 0f, 0.5f, 0f, 1f, 0f)
        triHelper(list, -0.5f, 0f, -0.5f, 0.5f, 0f, -0.5f, 0.5f, 0f, 0.5f)
        triHelper(list, -0.5f, 0f, -0.5f, 0.5f, 0f, 0.5f, -0.5f, 0f, 0.5f)
        return toFloatBuffer(list.toFloatArray())
    }

    fun createWedgeRoofBuffer(): FloatBuffer {
        val list = ArrayList<Float>(WEDGE_VERTEX_COUNT * 6)
        triHelper(list, -0.5f, 0f, 0.5f, 0.5f, 0f, 0.5f, 0.5f, 1f, 0f)
        triHelper(list, -0.5f, 0f, 0.5f, 0.5f, 1f, 0f, -0.5f, 1f, 0f)
        triHelper(list, 0.5f, 0f, -0.5f, -0.5f, 0f, -0.5f, -0.5f, 1f, 0f)
        triHelper(list, 0.5f, 0f, -0.5f, -0.5f, 1f, 0f, 0.5f, 1f, 0f)
        triHelper(list, -0.5f, 0f, -0.5f, -0.5f, 0f, 0.5f, -0.5f, 1f, 0f)
        triHelper(list, 0.5f, 0f, 0.5f, 0.5f, 0f, -0.5f, 0.5f, 1f, 0f)
        triHelper(list, -0.5f, 0f, -0.5f, 0.5f, 0f, -0.5f, 0.5f, 0f, 0.5f)
        triHelper(list, -0.5f, 0f, -0.5f, 0.5f, 0f, 0.5f, -0.5f, 0f, 0.5f)
        return toFloatBuffer(list.toFloatArray())
    }

    fun createSmoothCylinderBuffer(sides: Int = CYLINDER_SEGMENTS): FloatBuffer {
        val list = ArrayList<Float>(sides * 12 * 6)
        val step = (2.0 * PI) / sides
        for (i in 0 until sides) {
            val a1 = i * step
            val a2 = (i + 1) * step
            val c1 = cos(a1).toFloat(); val s1 = sin(a1).toFloat()
            val c2 = cos(a2).toFloat(); val s2 = sin(a2).toFloat()
            val x1 = 0.5f * c1; val z1 = 0.5f * s1
            val x2 = 0.5f * c2; val z2 = 0.5f * s2

            list.addAll(listOf(x1, -0.5f, z1, c1, 0f, s1, x2, -0.5f, z2, c2, 0f, s2, x2, 0.5f, z2, c2, 0f, s2))
            list.addAll(listOf(x1, -0.5f, z1, c1, 0f, s1, x2, 0.5f, z2, c2, 0f, s2, x1, 0.5f, z1, c1, 0f, s1))
            list.addAll(listOf(0f, 0.5f, 0f, 0f, 1f, 0f, x1, 0.5f, z1, 0f, 1f, 0f, x2, 0.5f, z2, 0f, 1f, 0f))
            list.addAll(listOf(0f, -0.5f, 0f, 0f, -1f, 0f, x2, -0.5f, z2, 0f, -1f, 0f, x1, -0.5f, z1, 0f, -1f, 0f))
        }
        return toFloatBuffer(list.toFloatArray())
    }

    fun createConeBuffer(sides: Int = CONE_SEGMENTS): FloatBuffer {
        val list = ArrayList<Float>(sides * 6 * 6)
        val step = (2.0 * PI) / sides
        for (i in 0 until sides) {
            val a1 = i * step
            val a2 = (i + 1) * step
            val c1 = cos(a1).toFloat(); val s1 = sin(a1).toFloat()
            val c2 = cos(a2).toFloat(); val s2 = sin(a2).toFloat()
            val x1 = 0.5f * c1; val z1 = 0.5f * s1
            val x2 = 0.5f * c2; val z2 = 0.5f * s2
            val cm = cos((a1 + a2) * 0.5).toFloat()
            val sm = sin((a1 + a2) * 0.5).toFloat()
            val ny = 0.4472f
            val nr = 0.8944f

            list.addAll(listOf(x1, 0f, z1, c1 * nr, ny, s1 * nr, x2, 0f, z2, c2 * nr, ny, s2 * nr, 0f, 1f, 0f, cm * nr, ny, sm * nr))
            list.addAll(listOf(0f, 0f, 0f, 0f, -1f, 0f, x2, 0f, z2, 0f, -1f, 0f, x1, 0f, z1, 0f, -1f, 0f))
        }
        return toFloatBuffer(list.toFloatArray())
    }

    fun createCapsuleBuffer(
        sides: Int = CAPSULE_SEGMENTS,
        rings: Int = CAPSULE_RINGS
    ): FloatBuffer {
        val list = ArrayList<Float>(CAPSULE_VERTEX_COUNT * 6)
        val step = (2.0 * PI) / sides
        val halfCyl = 0.22f
        val capRadius = 0.28f

        for (i in 0 until sides) {
            val a1 = i * step
            val a2 = (i + 1) * step
            val c1 = cos(a1).toFloat(); val s1 = sin(a1).toFloat()
            val c2 = cos(a2).toFloat(); val s2 = sin(a2).toFloat()
            val x1 = 0.5f * c1; val z1 = 0.5f * s1
            val x2 = 0.5f * c2; val z2 = 0.5f * s2
            list.addAll(listOf(x1, -halfCyl, z1, c1, 0f, s1, x2, -halfCyl, z2, c2, 0f, s2, x2, halfCyl, z2, c2, 0f, s2))
            list.addAll(listOf(x1, -halfCyl, z1, c1, 0f, s1, x2, halfCyl, z2, c2, 0f, s2, x1, halfCyl, z1, c1, 0f, s1))
        }

        fun addHemisphere(top: Boolean) {
            val sign = if (top) 1f else -1f
            val baseY = halfCyl * sign
            for (r in 0 until rings) {
                val p1 = (PI * 0.5 * r) / rings
                val p2 = (PI * 0.5 * (r + 1)) / rings
                val y1 = sin(p1).toFloat(); val rad1 = cos(p1).toFloat()
                val y2 = sin(p2).toFloat(); val rad2 = cos(p2).toFloat()
                for (i in 0 until sides) {
                    val a1 = i * step
                    val a2 = (i + 1) * step
                    val c1 = cos(a1).toFloat(); val s1 = sin(a1).toFloat()
                    val c2 = cos(a2).toFloat(); val s2 = sin(a2).toFloat()

                    val v11 = floatArrayOf(0.5f * rad1 * c1, baseY + sign * capRadius * y1, 0.5f * rad1 * s1, rad1 * c1, sign * y1, rad1 * s1)
                    val v12 = floatArrayOf(0.5f * rad1 * c2, baseY + sign * capRadius * y1, 0.5f * rad1 * s2, rad1 * c2, sign * y1, rad1 * s2)
                    val v21 = floatArrayOf(0.5f * rad2 * c1, baseY + sign * capRadius * y2, 0.5f * rad2 * s1, rad2 * c1, sign * y2, rad2 * s1)
                    val v22 = floatArrayOf(0.5f * rad2 * c2, baseY + sign * capRadius * y2, 0.5f * rad2 * s2, rad2 * c2, sign * y2, rad2 * s2)

                    if (top) {
                        v11.forEach { list.add(it) }; v12.forEach { list.add(it) }; v22.forEach { list.add(it) }
                        v11.forEach { list.add(it) }; v22.forEach { list.add(it) }; v21.forEach { list.add(it) }
                    } else {
                        v11.forEach { list.add(it) }; v22.forEach { list.add(it) }; v12.forEach { list.add(it) }
                        v11.forEach { list.add(it) }; v21.forEach { list.add(it) }; v22.forEach { list.add(it) }
                    }
                }
            }
        }
        addHemisphere(true)
        addHemisphere(false)
        return toFloatBuffer(list.toFloatArray())
    }

    fun createTorusBuffer(
        majorSegs: Int = TORUS_MAJOR,
        minorSegs: Int = TORUS_MINOR
    ): FloatBuffer {
        val list = ArrayList<Float>(TORUS_VERTEX_COUNT * 6)
        val majorR = 0.38f
        val minorR = 0.12f
        val dU = (2.0 * PI) / majorSegs
        val dV = (2.0 * PI) / minorSegs

        fun vertex(u: Double, v: Double): FloatArray {
            val cu = cos(u).toFloat(); val su = sin(u).toFloat()
            val cv = cos(v).toFloat(); val sv = sin(v).toFloat()
            val r = majorR + minorR * cv
            return floatArrayOf(r * cu, minorR * sv, r * su, cv * cu, sv, cv * su)
        }

        for (i in 0 until majorSegs) {
            val u1 = i * dU; val u2 = (i + 1) * dU
            for (j in 0 until minorSegs) {
                val v1 = j * dV; val v2 = (j + 1) * dV
                val p11 = vertex(u1, v1)
                val p21 = vertex(u2, v1)
                val p22 = vertex(u2, v2)
                val p12 = vertex(u1, v2)
                p11.forEach { list.add(it) }; p21.forEach { list.add(it) }; p22.forEach { list.add(it) }
                p11.forEach { list.add(it) }; p22.forEach { list.add(it) }; p12.forEach { list.add(it) }
            }
        }
        return toFloatBuffer(list.toFloatArray())
    }

    /**
     * Ultra-detailed 6-Ring x 20-Segment Anatomical V-Taper Chest/Torso (708 vertices) with pecs, abs, lats & smooth normals.
     */
    fun createTorsoArmorBuffer(): FloatBuffer {
        val list = ArrayList<Float>(TORSO_ARMOR_VERTEX_COUNT * 6)
        val segs = TORSO_ARMOR_SEGMENTS
        val step = (2.0 * PI) / segs

        data class RingSpec(val y: Float, val rxFront: Float, val rxBack: Float, val rz: Float, val pecRidge: Float)
        val specs = arrayOf(
            RingSpec(-0.50f, 0.29f, 0.27f, 0.34f, 0.02f), // Pelvis/Waist
            RingSpec(-0.26f, 0.32f, 0.30f, 0.36f, 0.04f), // Lower Abs
            RingSpec(-0.02f, 0.37f, 0.34f, 0.42f, 0.07f), // Serratus / Mid Ribcage
            RingSpec(0.20f, 0.46f, 0.41f, 0.54f, 0.14f),  // Broad Pectorals & Lats
            RingSpec(0.38f, 0.43f, 0.39f, 0.51f, 0.08f),  // Upper Chest & Delts
            RingSpec(0.50f, 0.31f, 0.30f, 0.42f, 0.01f)   // Collar / Traps
        )

        val rings = specs.map { sp ->
            Array(segs) { i ->
                val a = i * step
                val ca = cos(a).toFloat()
                val sa = sin(a).toFloat()
                val dualPec = cos(a * 2.0).toFloat().coerceAtLeast(0f)
                val rx = if (ca >= 0f) sp.rxFront + sp.pecRidge * dualPec else sp.rxBack
                val x = ca * rx
                val z = sa * sp.rz
                val nx = ca / rx.coerceAtLeast(0.1f)
                val nz = sa / sp.rz.coerceAtLeast(0.1f)
                val ny = if (sp.y > 0.32f) 0.30f else if (sp.y < -0.32f) -0.18f else 0.04f
                val nLen = hypot(nx, hypot(ny, nz)).coerceAtLeast(0.001f)
                floatArrayOf(x, sp.y, z, nx / nLen, ny / nLen, nz / nLen)
            }
        }

        fun addVert(v: FloatArray) {
            for (k in 0 until 6) list.add(v[k])
        }

        for (r in 0 until rings.size - 1) {
            val a = rings[r]
            val b = rings[r + 1]
            for (i in 0 until segs) {
                val j = (i + 1) % segs
                addVert(a[i]); addVert(a[j]); addVert(b[j])
                addVert(a[i]); addVert(b[j]); addVert(b[i])
            }
        }

        val top = rings.last()
        val bot = rings.first()
        for (i in 1 until segs - 1) {
            triHelper(list, top[0][0], top[0][1], top[0][2], top[i][0], top[i][1], top[i][2], top[i + 1][0], top[i + 1][1], top[i + 1][2])
            triHelper(list, bot[0][0], bot[0][1], bot[0][2], bot[i + 1][0], bot[i + 1][1], bot[i + 1][2], bot[i][0], bot[i][1], bot[i][2])
        }
        return toFloatBuffer(list.toFloatArray())
    }

    /**
     * High-Polygon 12-Segment 4-Ring Sculpted Chamfered Hull (vehicle fenders, receiver shells, armored cabins, boots).
     */
    fun createChamferedHullBuffer(): FloatBuffer {
        val list = ArrayList<Float>(CHAMFER_HULL_VERTEX_COUNT * 6)
        val segs = CHAMFER_HULL_SEGMENTS
        val step = (2.0 * PI) / segs

        data class HullRing(val y: Float, val rxFront: Float, val rxBack: Float, val rz: Float, val sq: Float)
        val specs = arrayOf(
            HullRing(-0.50f, 0.46f, 0.46f, 0.46f, 0.16f),
            HullRing(-0.18f, 0.52f, 0.50f, 0.51f, 0.20f),
            HullRing(0.22f, 0.48f, 0.48f, 0.48f, 0.18f),
            HullRing(0.50f, 0.36f, 0.42f, 0.39f, 0.14f)
        )

        val rings = specs.map { sp ->
            Array(segs) { i ->
                val a = i * step
                val ca = cos(a).toFloat()
                val sa = sin(a).toFloat()
                val squircle = 1f + sp.sq * kotlin.math.abs(cos(2.0 * a)).toFloat()
                val rx = (if (ca >= 0f) sp.rxFront else sp.rxBack) * squircle
                val rz = sp.rz * squircle
                floatArrayOf(ca * rx, sp.y, sa * rz)
            }
        }

        for (r in 0 until rings.size - 1) {
            val a = rings[r]
            val b = rings[r + 1]
            for (i in 0 until segs) {
                val j = (i + 1) % segs
                triHelper(list, a[i][0], a[i][1], a[i][2], a[j][0], a[j][1], a[j][2], b[j][0], b[j][1], b[j][2])
                triHelper(list, a[i][0], a[i][1], a[i][2], b[j][0], b[j][1], b[j][2], b[i][0], b[i][1], b[i][2])
            }
        }
        val top = rings.last()
        val bot = rings.first()
        for (i in 1 until segs - 1) {
            triHelper(list, top[0][0], top[0][1], top[0][2], top[i][0], top[i][1], top[i][2], top[i + 1][0], top[i + 1][1], top[i + 1][2])
            triHelper(list, bot[0][0], bot[0][1], bot[0][2], bot[i + 1][0], bot[i + 1][1], bot[i + 1][2], bot[i][0], bot[i][1], bot[i][2])
        }
        return toFloatBuffer(list.toFloatArray())
    }

    /**
     * Multi-segment Cambered Swept Wing / Feather / Frond Blade (84 vertices).
     */
    fun createSweptWingBuffer(): FloatBuffer {
        val list = ArrayList<Float>(SWEPT_WING_VERTEX_COUNT * 6)
        val p0 = floatArrayOf(0f, 0f, 0f)
        val p1 = floatArrayOf(-0.14f, 0.34f, 0.24f)
        val p2 = floatArrayOf(-0.32f, 0.50f, 0.44f)
        val p3 = floatArrayOf(-0.56f, 0.32f, 0.54f)
        val p4 = floatArrayOf(-0.48f, 0.04f, 0.48f)
        val p5 = floatArrayOf(-0.34f, -0.34f, 0.34f)
        val p6 = floatArrayOf(-0.18f, -0.46f, 0.16f)
        val p7 = floatArrayOf(-0.06f, -0.26f, 0.04f)
        val thick = 0.055f

        fun face(a: FloatArray, b: FloatArray, c: FloatArray, zOff: Float) {
            triHelper(list, a[0] + zOff, a[1], a[2], b[0] + zOff, b[1], b[2], c[0] + zOff, c[1], c[2])
        }
        val fan = arrayOf(p1, p2, p3, p4, p5, p6, p7)
        for (i in 0 until fan.size - 1) {
            face(p0, fan[i], fan[i + 1], thick)
            face(p0, fan[i + 1], fan[i], -thick)
        }
        val pts = arrayOf(p0, p1, p2, p3, p4, p5, p6, p7)
        for (i in pts.indices) {
            val a = pts[i]; val b = pts[(i + 1) % pts.size]
            triHelper(list, a[0] + thick, a[1], a[2], a[0] - thick, a[1], a[2], b[0] - thick, b[1], b[2])
            triHelper(list, a[0] + thick, a[1], a[2], b[0] - thick, b[1], b[2], b[0] + thick, b[1], b[2])
        }
        return toFloatBuffer(list.toFloatArray())
    }

    /**
     * Sculpted Multi-Ring Assault Rifle / SMG Receiver Body with Upper Picatinny Spine & Bevel Lower Frame (~420+ vertices).
     */
    fun createRifleReceiverMesh(): GlMesh {
        val list = ArrayList<Float>(3000)
        // Extrude contoured octagonal cross-sections along local X (-0.5 .. +0.5)
        data class Sec(val x: Float, val yBot: Float, val yTop: Float, val halfZ: Float, val pinch: Float)
        val sections = arrayOf(
            Sec(-0.50f, -0.28f, 0.34f, 0.32f, 0.08f),
            Sec(-0.30f, -0.44f, 0.42f, 0.38f, 0.06f), // Pistol grip / rear lug transition
            Sec(-0.08f, -0.48f, 0.46f, 0.44f, 0.04f), // Ejection port & magwell housing
            Sec(0.14f, -0.50f, 0.46f, 0.44f, 0.05f),  // Front magwell chamfer
            Sec(0.28f, -0.32f, 0.44f, 0.36f, 0.07f),  // Handguard collar step
            Sec(0.50f, -0.26f, 0.40f, 0.30f, 0.08f)   // Front gas block shoulder
        )
        val rings = sections.map { s ->
            val my = (s.yBot + s.yTop) * 0.5f
            arrayOf(
                floatArrayOf(s.x, s.yTop, s.halfZ * 0.65f),
                floatArrayOf(s.x, my + (s.yTop - my) * 0.45f, s.halfZ),
                floatArrayOf(s.x, my - (my - s.yBot) * 0.45f, s.halfZ * (1f - s.pinch)),
                floatArrayOf(s.x, s.yBot, s.halfZ * 0.60f),
                floatArrayOf(s.x, s.yBot, -s.halfZ * 0.60f),
                floatArrayOf(s.x, my - (my - s.yBot) * 0.45f, -s.halfZ * (1f - s.pinch)),
                floatArrayOf(s.x, my + (s.yTop - my) * 0.45f, -s.halfZ),
                floatArrayOf(s.x, s.yTop, -s.halfZ * 0.65f)
            )
        }
        for (r in 0 until rings.size - 1) {
            val a = rings[r]; val b = rings[r + 1]
            for (i in 0 until 8) {
                val j = (i + 1) % 8
                triHelper(list, a[i][0], a[i][1], a[i][2], a[j][0], a[j][1], a[j][2], b[j][0], b[j][1], b[j][2])
                triHelper(list, a[i][0], a[i][1], a[i][2], b[j][0], b[j][1], b[j][2], b[i][0], b[i][1], b[i][2])
            }
        }
        val front = rings.last(); val back = rings.first()
        for (i in 1 until 7) {
            triHelper(list, front[0][0], front[0][1], front[0][2], front[i][0], front[i][1], front[i][2], front[i + 1][0], front[i + 1][1], front[i + 1][2])
            triHelper(list, back[0][0], back[0][1], back[0][2], back[i + 1][0], back[i + 1][1], back[i + 1][2], back[i][0], back[i][1], back[i][2])
        }
        return GlMesh(toFloatBuffer(list.toFloatArray()), list.size / 6)
    }

    /**
     * Sculpted 12-Segment Curved AKM / Tactical Magazine Mesh (~1,500+ vertices).
     */
    fun createCurvedMagMesh(): GlMesh {
        val list = ArrayList<Float>(12000)
        val steps = 12
        val segs = 12
        val angleStep = (2.0 * PI) / segs
        val rings = Array(steps + 1) { t ->
            val frac = t.toFloat() / steps
            val cx = frac * frac * 0.42f // Smooth forward sweep!
            val cy = 0.5f - frac * 1.0f
            val rx = 0.34f * (1f - frac * 0.14f)
            val rz = 0.22f * (1f - frac * 0.10f)
            Array(segs) { i ->
                val a = i * angleStep
                val ca = cos(a).toFloat(); val sa = sin(a).toFloat()
                floatArrayOf(cx + ca * rx, cy, sa * rz)
            }
        }
        for (r in 0 until rings.size - 1) {
            val a = rings[r]; val b = rings[r + 1]
            for (i in 0 until segs) {
                val j = (i + 1) % segs
                triHelper(list, a[i][0], a[i][1], a[i][2], a[j][0], a[j][1], a[j][2], b[j][0], b[j][1], b[j][2])
                triHelper(list, a[i][0], a[i][1], a[i][2], b[j][0], b[j][1], b[j][2], b[i][0], b[i][1], b[i][2])
            }
        }
        val top = rings.first(); val bot = rings.last()
        for (i in 1 until segs - 1) {
            triHelper(list, top[0][0], top[0][1], top[0][2], top[i][0], top[i][1], top[i][2], top[i + 1][0], top[i + 1][1], top[i + 1][2])
            triHelper(list, bot[0][0], bot[0][1], bot[0][2], bot[i + 1][0], bot[i + 1][1], bot[i + 1][2], bot[i][0], bot[i][1], bot[i][2])
        }
        return GlMesh(toFloatBuffer(list.toFloatArray()), list.size / 6)
    }

    /**
     * Tapered 12-Ring Swept Curved Horn / Fang / Claw Mesh (~1,152 vertices).
     * Local origin at base (0,0,0), sweeps upward along +Y and curves outward along +X/+Z.
     */
    fun createCurvedHornMesh(): GlMesh {
        val list = ArrayList<Float>(10000)
        val steps = 12
        val segs = 12
        val angleStep = (2.0 * PI) / segs
        val rings = Array(steps + 1) { t ->
            val f = t.toFloat() / steps
            val cy = f * 1.0f
            val cx = (f * f) * 0.48f
            val rad = (1f - f * 0.88f) * 0.24f
            Array(segs) { i ->
                val a = i * angleStep
                floatArrayOf(cx + cos(a).toFloat() * rad, cy, sin(a).toFloat() * rad)
            }
        }
        for (r in 0 until rings.size - 1) {
            val a = rings[r]; val b = rings[r + 1]
            for (i in 0 until segs) {
                val j = (i + 1) % segs
                triHelper(list, a[i][0], a[i][1], a[i][2], a[j][0], a[j][1], a[j][2], b[j][0], b[j][1], b[j][2])
                triHelper(list, a[i][0], a[i][1], a[i][2], b[j][0], b[j][1], b[j][2], b[i][0], b[i][1], b[i][2])
            }
        }
        val tip = rings.last()
        val tipCenter = floatArrayOf(0.50f, 1.04f, 0f)
        for (i in 0 until segs) {
            val j = (i + 1) % segs
            triHelper(list, tip[i][0], tip[i][1], tip[i][2], tip[j][0], tip[j][1], tip[j][2], tipCenter[0], tipCenter[1], tipCenter[2])
        }
        return GlMesh(toFloatBuffer(list.toFloatArray()), list.size / 6)
    }

    /**
     * Curved Lamellar Sode / Skirt Samurai Armor Plate (~380 vertices).
     */
    fun createSodeArmorPlateMesh(): GlMesh {
        val list = ArrayList<Float>(3000)
        val arcSegs = 8
        val vSegs = 4
        val innerR = 0.36f
        val outerR = 0.46f
        fun pt(u: Int, v: Int, r: Float): FloatArray {
            val a = (-0.42f + (u.toFloat() / arcSegs) * 0.84f) * PI.toFloat()
            val y = 0.5f - (v.toFloat() / vSegs) * 1.0f
            val flare = 1f + (v.toFloat() / vSegs) * 0.25f
            return floatArrayOf(cos(a) * r * flare, y, sin(a) * r * flare)
        }
        for (v in 0 until vSegs) {
            for (u in 0 until arcSegs) {
                val o00 = pt(u, v, outerR); val o10 = pt(u + 1, v, outerR)
                val o01 = pt(u, v + 1, outerR); val o11 = pt(u + 1, v + 1, outerR)
                triHelper(list, o00[0], o00[1], o00[2], o10[0], o10[1], o10[2], o11[0], o11[1], o11[2])
                triHelper(list, o00[0], o00[1], o00[2], o11[0], o11[1], o11[2], o01[0], o01[1], o01[2])

                val i00 = pt(u, v, innerR); val i10 = pt(u + 1, v, innerR)
                val i01 = pt(u, v + 1, innerR); val i11 = pt(u + 1, v + 1, innerR)
                triHelper(list, i00[0], i00[1], i00[2], i11[0], i11[1], i11[2], i10[0], i10[1], i10[2])
                triHelper(list, i00[0], i00[1], i00[2], i01[0], i01[1], i01[2], i11[0], i11[1], i11[2])
            }
        }
        return GlMesh(toFloatBuffer(list.toFloatArray()), list.size / 6)
    }

    /**
     * Sculpted 10-Segment Curved Katana Scabbard & Saya Mesh (~720 vertices).
     */
    fun createKatanaScabbardMesh(): GlMesh {
        val list = ArrayList<Float>(5500)
        val steps = 10
        val segs = 8
        val aStep = (2.0 * PI) / segs
        val rings = Array(steps + 1) { t ->
            val f = t.toFloat() / steps
            val y = -0.5f + f * 1.0f
            val curveX = (f - 0.5f) * (f - 0.5f) * 0.22f
            val rx = 0.085f * (1f - f * 0.22f)
            val rz = 0.042f * (1f - f * 0.22f)
            Array(segs) { i ->
                val a = i * aStep
                floatArrayOf(curveX + cos(a).toFloat() * rx, y, sin(a).toFloat() * rz)
            }
        }
        for (r in 0 until rings.size - 1) {
            val a = rings[r]; val b = rings[r + 1]
            for (i in 0 until segs) {
                val j = (i + 1) % segs
                triHelper(list, a[i][0], a[i][1], a[i][2], a[j][0], a[j][1], a[j][2], b[j][0], b[j][1], b[j][2])
                triHelper(list, a[i][0], a[i][1], a[i][2], b[j][0], b[j][1], b[j][2], b[i][0], b[i][1], b[i][2])
            }
        }
        return GlMesh(toFloatBuffer(list.toFloatArray()), list.size / 6)
    }

    /**
     * Multi-Tier 12-Facet Beveled Crystalline Ice / Energy Prism Mesh (288 vertices).
     */
    fun createHexCrystalMesh(): GlMesh {
        val list = ArrayList<Float>(2400)
        val sides = 12
        val step = (2.0 * PI) / sides
        val lowPts = Array(sides) { i ->
            val a = i * step
            val r = if (i % 2 == 0) 0.22f else 0.18f
            floatArrayOf(cos(a).toFloat() * r, -0.50f, sin(a).toFloat() * r)
        }
        val waistPts = Array(sides) { i ->
            val a = i * step
            val r = if (i % 2 == 0) 0.34f else 0.28f
            floatArrayOf(cos(a).toFloat() * r, -0.05f, sin(a).toFloat() * r)
        }
        val crownPts = Array(sides) { i ->
            val a = i * step
            val r = if (i % 2 == 0) 0.30f else 0.24f
            floatArrayOf(cos(a).toFloat() * r, 0.28f, sin(a).toFloat() * r)
        }
        for (i in 0 until sides) {
            val j = (i + 1) % sides
            triHelper(list, lowPts[i][0], lowPts[i][1], lowPts[i][2], lowPts[j][0], lowPts[j][1], lowPts[j][2], waistPts[j][0], waistPts[j][1], waistPts[j][2])
            triHelper(list, lowPts[i][0], lowPts[i][1], lowPts[i][2], waistPts[j][0], waistPts[j][1], waistPts[j][2], waistPts[i][0], waistPts[i][1], waistPts[i][2])

            triHelper(list, waistPts[i][0], waistPts[i][1], waistPts[i][2], waistPts[j][0], waistPts[j][1], waistPts[j][2], crownPts[j][0], crownPts[j][1], crownPts[j][2])
            triHelper(list, waistPts[i][0], waistPts[i][1], waistPts[i][2], crownPts[j][0], crownPts[j][1], crownPts[j][2], crownPts[i][0], crownPts[i][1], crownPts[i][2])

            triHelper(list, crownPts[i][0], crownPts[i][1], crownPts[i][2], crownPts[j][0], crownPts[j][1], crownPts[j][2], 0f, 0.66f, 0f)
            triHelper(list, lowPts[j][0], lowPts[j][1], lowPts[j][2], lowPts[i][0], lowPts[i][1], lowPts[i][2], 0f, -0.50f, 0f)
        }
        return GlMesh(toFloatBuffer(list.toFloatArray()), list.size / 6)
    }

    /**
     * Angular Contoured Bullpup / PDW Polymer Chassis Mesh (~580 vertices).
     */
    fun createBullpupChassisMesh(): GlMesh {
        val list = ArrayList<Float>(4000)
        data class Sec(val x: Float, val yBot: Float, val yTop: Float, val zTop: Float, val zMid: Float)
        val sections = arrayOf(
            Sec(-0.52f, -0.46f, 0.34f, 0.28f, 0.36f), // Integrated rear buttpad
            Sec(-0.28f, -0.50f, 0.42f, 0.30f, 0.42f), // Rear bullpup magwell housing
            Sec(-0.04f, -0.34f, 0.46f, 0.26f, 0.38f), // Cheek rest / upper bridge
            Sec(0.22f, -0.42f, 0.48f, 0.24f, 0.36f),  // Trigger guard / Super-V drop angle
            Sec(0.46f, -0.24f, 0.40f, 0.22f, 0.30f)   // Front shroud taper
        )
        val rings = sections.map { s ->
            val ym = (s.yBot + s.yTop) * 0.5f
            arrayOf(
                floatArrayOf(s.x, s.yTop, s.zTop),
                floatArrayOf(s.x, ym, s.zMid),
                floatArrayOf(s.x, s.yBot, s.zTop * 0.8f),
                floatArrayOf(s.x, s.yBot, -s.zTop * 0.8f),
                floatArrayOf(s.x, ym, -s.zMid),
                floatArrayOf(s.x, s.yTop, -s.zTop)
            )
        }
        for (r in 0 until rings.size - 1) {
            val a = rings[r]; val b = rings[r + 1]
            for (i in 0 until 6) {
                val j = (i + 1) % 6
                triHelper(list, a[i][0], a[i][1], a[i][2], a[j][0], a[j][1], a[j][2], b[j][0], b[j][1], b[j][2])
                triHelper(list, a[i][0], a[i][1], a[i][2], b[j][0], b[j][1], b[j][2], b[i][0], b[i][1], b[i][2])
            }
        }
        val front = rings.last(); val back = rings.first()
        for (i in 1 until 5) {
            triHelper(list, front[0][0], front[0][1], front[0][2], front[i][0], front[i][1], front[i][2], front[i + 1][0], front[i + 1][1], front[i + 1][2])
            triHelper(list, back[0][0], back[0][1], back[0][2], back[i + 1][0], back[i + 1][1], back[i + 1][2], back[i][0], back[i][1], back[i][2])
        }
        return GlMesh(toFloatBuffer(list.toFloatArray()), list.size / 6)
    }

    /**
     * Multi-Ribbed Armored Juggernaut Pauldron Dome (~480 vertices).
     */
    fun createHeavyPauldronMesh(): GlMesh {
        val list = ArrayList<Float>(3500)
        val uSegs = 10
        val vSegs = 6
        fun surf(u: Int, v: Int): FloatArray {
            val a = (-0.5f + u.toFloat() / uSegs) * PI.toFloat() * 1.15f
            val p = (v.toFloat() / vSegs) * (PI.toFloat() * 0.52f)
            val rad = 0.52f
            val x = cos(p) * cos(a) * rad
            val y = sin(p) * rad - 0.15f
            val z = cos(p) * sin(a) * rad * 1.15f
            return floatArrayOf(x, y, z)
        }
        for (v in 0 until vSegs) {
            for (u in 0 until uSegs) {
                val p00 = surf(u, v); val p10 = surf(u + 1, v)
                val p01 = surf(u, v + 1); val p11 = surf(u + 1, v + 1)
                triHelper(list, p00[0], p00[1], p00[2], p10[0], p10[1], p10[2], p11[0], p11[1], p11[2])
                triHelper(list, p00[0], p00[1], p00[2], p11[0], p11[1], p11[2], p01[0], p01[1], p01[2])
            }
        }
        return GlMesh(toFloatBuffer(list.toFloatArray()), list.size / 6)
    }

    /**
     * High-Polygon 64x64 Displaced 3D Map Landscape Terrain Mesh (8,192 triangles = 24,576 vertices!)
     * Keeps road crossroads, perimeter ring road, and building plazas level while sculpting rolling island ridges,
     * foothills, coastal cliffs, desert dunes, and alpine border mountains around 500M / 1KM maps.
     */
    fun createMapTerrainMesh(mapId: com.example.data.model.BattleMapId, mapSize: Float): GlMesh {
        val grid = 64
        val list = ArrayList<Float>(grid * grid * 6 * 6)
        val margin = mapSize * 0.24f
        val minCoord = -margin
        val totalSpan = mapSize + margin * 2f
        val cell = totalSpan / grid
        val half = mapSize * 0.5f
        val ringMin = mapSize * 0.14f
        val ringMax = mapSize * 0.86f

        fun heightAt(x: Float, z: Float): Float {
            // Distance outside playable island margin creates coastal slopes and distant mountain peaks
            val outX = maxOf(0f, -x, x - mapSize)
            val outZ = maxOf(0f, -z, z - mapSize)
            val outsideDist = hypot(outX, outZ)

            // Damp elevation near crossroads & outer highway so roads/compounds always sit flush at y=0
            val dCross = minOf(kotlin.math.abs(x - half), kotlin.math.abs(z - half))
            val dRing = minOf(
                kotlin.math.abs(x - ringMin),
                kotlin.math.abs(x - ringMax),
                kotlin.math.abs(z - ringMin),
                kotlin.math.abs(z - ringMax)
            )
            val roadFlatness = (minOf(dCross, dRing) / 140f).coerceIn(0f, 1f)
            val edgeBoost = (outsideDist / 260f).coerceIn(0f, 3.2f)

            val wave1 = sin(x * 0.0022f) * cos(z * 0.0024f)
            val wave2 = sin(x * 0.0053f + z * 0.0041f) * 0.55f
            val ridge = kotlin.math.abs(sin(x * 0.0014f - z * 0.0016f))

            return when (mapId) {
                com.example.data.model.BattleMapId.ERANGEL -> {
                    if (outsideDist > 40f && outsideDist < 260f) {
                        // Gentle sandy coastal drop near ocean rim, rising to distant horizon peaks beyond
                        -2.2f + (wave1 * 1.8f)
                    } else if (outsideDist >= 260f) {
                        // Distant island bluffs beyond sea strait
                        14f + ridge * 58f + wave2 * 22f
                    } else {
                        // Subtle playable rolling grass turf undulations away from roads
                        (wave1 * 0.85f + wave2 * 0.45f) * roadFlatness
                    }
                }
                com.example.data.model.BattleMapId.MIRAMAR -> {
                    val dune = sin(x * 0.0031f + z * 0.0018f) * 0.8f + ridge
                    if (outsideDist > 20f) {
                        edgeBoost * (36f + dune * 34f)
                    } else {
                        (wave1 * 1.1f + wave2 * 0.55f) * roadFlatness
                    }
                }
                com.example.data.model.BattleMapId.SANHOK -> {
                    val riverCenter = mapSize * 0.60f + sin(x * 0.0018f) * 35f
                    val distRiver = kotlin.math.abs(z - riverCenter)
                    if (distRiver < 92f && x > 120f && x < mapSize - 120f) {
                        // Carved sunken jungle riverbed
                        -3.8f + (distRiver / 92f) * 3.5f
                    } else if (outsideDist > 45f) {
                        edgeBoost * (28f + ridge * 40f)
                    } else {
                        (wave1 * 0.9f + wave2 * 0.45f) * roadFlatness
                    }
                }
            }
        }

        fun normalAt(x: Float, z: Float): FloatArray {
            val eps = cell * 0.35f
            val hL = heightAt(x - eps, z)
            val hR = heightAt(x + eps, z)
            val hD = heightAt(x, z - eps)
            val hU = heightAt(x, z + eps)
            val nx = hL - hR
            val ny = 2f * eps
            val nz = hD - hU
            val len = hypot(nx, hypot(ny, nz)).coerceAtLeast(0.001f)
            return floatArrayOf(nx / len, ny / len, nz / len)
        }

        val verts = Array(grid + 1) { gx ->
            Array(grid + 1) { gz ->
                val x = minCoord + gx * cell
                val z = minCoord + gz * cell
                val y = heightAt(x, z)
                val n = normalAt(x, z)
                floatArrayOf(x, y, z, n[0], n[1], n[2])
            }
        }

        fun push(v: FloatArray) {
            for (k in 0 until 6) list.add(v[k])
        }

        for (gx in 0 until grid) {
            for (gz in 0 until grid) {
                val p00 = verts[gx][gz]
                val p10 = verts[gx + 1][gz]
                val p01 = verts[gx][gz + 1]
                val p11 = verts[gx + 1][gz + 1]
                push(p00); push(p01); push(p11)
                push(p00); push(p11); push(p10)
            }
        }
        return GlMesh(toFloatBuffer(list.toFloatArray()), list.size / 6)
    }

    fun createSphereBuffer(
        stacks: Int = SPHERE_STACKS,
        slices: Int = SPHERE_SLICES
    ): FloatBuffer {
        val list = ArrayList<Float>(stacks * slices * 6 * 6)
        for (i in 0 until stacks) {
            val phi1 = (PI * i) / stacks - PI * 0.5
            val phi2 = (PI * (i + 1)) / stacks - PI * 0.5
            val y1 = sin(phi1).toFloat(); val r1 = cos(phi1).toFloat()
            val y2 = sin(phi2).toFloat(); val r2 = cos(phi2).toFloat()

            for (j in 0 until slices) {
                val th1 = (2.0 * PI * j) / slices
                val th2 = (2.0 * PI * (j + 1)) / slices
                val c1 = cos(th1).toFloat(); val s1 = sin(th1).toFloat()
                val c2 = cos(th2).toFloat(); val s2 = sin(th2).toFloat()

                val nx11 = r1 * c1; val ny11 = y1; val nz11 = r1 * s1
                val nx12 = r1 * c2; val ny12 = y1; val nz12 = r1 * s2
                val nx21 = r2 * c1; val ny21 = y2; val nz21 = r2 * s1
                val nx22 = r2 * c2; val ny22 = y2; val nz22 = r2 * s2

                fun addV(nx: Float, ny: Float, nz: Float) {
                    list.add(nx * 0.5f); list.add(ny * 0.5f); list.add(nz * 0.5f)
                    list.add(nx); list.add(ny); list.add(nz)
                }

                addV(nx11, ny11, nz11); addV(nx21, ny21, nz21); addV(nx22, ny22, nz22)
                addV(nx11, ny11, nz11); addV(nx22, ny22, nz22); addV(nx12, ny12, nz12)
            }
        }
        return toFloatBuffer(list.toFloatArray())
    }

    fun createZoneWallBuffer(segments: Int = ZONE_WALL_SEGMENTS): FloatBuffer {
        val list = ArrayList<Float>(segments * 6 * 6)
        val step = (2.0 * PI) / segments
        for (i in 0 until segments) {
            val a1 = i * step
            val a2 = (i + 1) * step
            val x1 = cos(a1).toFloat(); val z1 = sin(a1).toFloat()
            val x2 = cos(a2).toFloat(); val z2 = sin(a2).toFloat()
            list.addAll(listOf(x1, 0f, z1, -x1, 0f, -z1, x2, 0f, z2, -x2, 0f, -z2, x2, 1f, z2, -x2, 0f, -z2))
            list.addAll(listOf(x1, 0f, z1, -x1, 0f, -z1, x2, 1f, z2, -x2, 0f, -z2, x1, 1f, z1, -x1, 0f, -z1))
        }
        return toFloatBuffer(list.toFloatArray())
    }

    private fun triHelper(
        list: MutableList<Float>,
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float
    ) {
        val ux = x2 - x1; val uy = y2 - y1; val uz = z2 - z1
        val vx = x3 - x1; val vy = y3 - y1; val vz = z3 - z1
        var nx = uy * vz - uz * vy; var ny = uz * vx - ux * vz; var nz = ux * vy - uy * vx
        val len = hypot(nx, hypot(ny, nz)).coerceAtLeast(0.001f)
        nx /= len; ny /= len; nz /= len
        list.addAll(listOf(x1, y1, z1, nx, ny, nz, x2, y2, z2, nx, ny, nz, x3, y3, z3, nx, ny, nz))
    }

    private fun toFloatBuffer(data: FloatArray): FloatBuffer {
        return ByteBuffer.allocateDirect(data.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(data)
                position(0)
            }
    }
}
