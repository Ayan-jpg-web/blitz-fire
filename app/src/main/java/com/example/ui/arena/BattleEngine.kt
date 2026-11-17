package com.example.ui.arena

import com.example.data.model.BattleMapId
import com.example.data.model.CatalogItem
import com.example.data.model.GameCatalog
import com.example.data.model.OwnedItem
import com.example.data.model.SensitivitySettings
import com.example.data.model.UserProfile
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

data class Vec2(val x: Float, val y: Float)

enum class StructureType {
    COMPOUND_HOUSE,
    CLOCK_TOWER,
    SHIPPING_CONTAINER,
    WATCHTOWER,
    SANDBAG_WALL,
    HACIENDA_VILLA,
    OIL_DERRICK,
    TEMPLE_RUIN,
    STILT_HUT
}

enum class VehicleType {
    UAZ_JEEP,
    DESERT_BUGGY,
    PATROL_TRUCK
}

enum class FoliageStyle {
    PINE,
    DESERT_SCRUB,
    JUNGLE_PALM
}

data class ObstacleRect(
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
    val height: Float = 84f,
    val wallColorHex: Long = 0xFF334155,
    val roofColorHex: Long = 0xFF475569,
    val accentHex: Long = 0xFFF5B400,
    val hasSpire: Boolean = false,
    val structureType: StructureType = StructureType.COMPOUND_HOUSE,
    val label: String = ""
)

data class VehicleProp(
    var x: Float,
    var y: Float,
    var rotDeg: Float,
    val type: VehicleType,
    val bodyColorHex: Long,
    val accentHex: Long,
    var speed: Float = 0f,
    var wheelSpinRad: Float = 0f,
    var hp: Float = 260f,
    val maxHp: Float = 260f
)

data class RenderVehicle(
    val x: Float,
    val y: Float,
    val rotDeg: Float,
    val wheelSpinRad: Float,
    val type: VehicleType,
    val bodyColorHex: Long,
    val accentHex: Long,
    val hpRatio: Float,
    val isOccupied: Boolean
)

data class TreeProp(
    val x: Float,
    val y: Float,
    val scale: Float,
    val style: FoliageStyle = FoliageStyle.PINE
)

data class GlooBarrier(
    val id: Int,
    val x: Float,
    val y: Float,
    val angleRad: Float,
    var hp: Float,
    val maxHp: Float,
    val colorHex: Long
)

enum class LootType { MEDKIT, GLOO_PACK, AMMO_CRATE, SHIELD_VEST }

data class LootDrop(
    val id: Int,
    val x: Float,
    val y: Float,
    val type: LootType,
    val isSupplyDrop: Boolean = false
)

data class Bullet(
    val id: Int,
    var x: Float,
    var y: Float,
    var z: Float,
    val vx: Float,
    val vy: Float,
    val vz: Float,
    val ownerId: Int,
    val damage: Int,
    val isHeadshot: Boolean,
    val colorHex: Long,
    var lifeSec: Float = 0.85f
)

data class Combatant(
    val id: Int,
    val name: String,
    var x: Float,
    var y: Float,
    var z: Float = 0f,
    var vz: Float = 0f,
    var angleRad: Float = -1.57f,
    var hp: Float,
    val maxHp: Float,
    var shield: Float,
    val maxShield: Float,
    var alive: Boolean = true,
    var fireCooldown: Float = 0f,
    var aiTargetAngle: Float = 0f,
    var aiWanderTimer: Float = 0f,
    val accentHex: Long = 0xFFFF3D57,
    var kills: Int = 0,
    var walkPhase: Float = 0f,
    var isMoving: Boolean = false,
    var muzzleFlashTimer: Float = 0f
)

data class DamagePopup(
    val id: Int,
    val x: Float,
    val y: Float,
    val z: Float,
    val amount: Int,
    val isHeadshot: Boolean,
    var lifeSec: Float = 0.8f
)

data class KillFeedEntry(
    val id: Int,
    val text: String,
    val isPlayerKill: Boolean,
    var lifeSec: Float = 4.0f
)

data class ArenaMatchOutcome(
    val placement: Int,
    val kills: Int,
    val headshots: Int,
    val damageDealt: Int,
    val survivalSeconds: Int,
    val isBooyah: Boolean
)

data class RenderCombatant(
    val id: Int,
    val name: String,
    val x: Float,
    val y: Float,
    val z: Float,
    val angleRad: Float,
    val hp: Float,
    val maxHp: Float,
    val accentHex: Long,
    val walkPhase: Float,
    val isMoving: Boolean,
    val isMuzzleFlashing: Boolean,
    val isCrouched: Boolean = false
)

data class RenderBullet(
    val x: Float,
    val y: Float,
    val z: Float,
    val vx: Float,
    val vy: Float,
    val vz: Float,
    val colorHex: Long,
    val isHeadshot: Boolean
)

data class RenderGloo(
    val x: Float,
    val y: Float,
    val angleRad: Float,
    val hpRatio: Float,
    val colorHex: Long
)

data class RenderLoot(
    val x: Float,
    val y: Float,
    val type: LootType,
    val isSupplyDrop: Boolean
)

data class Arena3DSnapshot(
    val elapsedSeconds: Float,
    val mapId: BattleMapId,
    val mapSize: Float,
    val cameraYaw: Float,
    val cameraPitch: Float,
    val isScoped: Boolean,
    val isPlayerCrouched: Boolean,
    val isDriving: Boolean,
    val isPlayerIndoors: Boolean,
    val vehicleSpeedKmh: Int,
    val hasThermalScope: Boolean,
    val player: RenderCombatant?,
    val bots: List<RenderCombatant>,
    val bullets: List<RenderBullet>,
    val glooWalls: List<RenderGloo>,
    val lootDrops: List<RenderLoot>,
    val vehicles: List<RenderVehicle>,
    val zoneCenterX: Float,
    val zoneCenterY: Float,
    val zoneRadius: Float,
    val hatId: String,
    val hatColorHex: Long,
    val outfitId: String,
    val outfitColorHex: Long,
    val characterId: String,
    val accessoryId: String,
    val accessoryColorHex: Long,
    val weaponId: String,
    val weaponColorHex: Long,
    val attachmentId: String,
    val activeWeaponIsRifle: Boolean
)

class BattleEngine(
    val mode: String,
    profile: UserProfile,
    inventory: Map<String, OwnedItem> = emptyMap(),
    val mapId: BattleMapId = BattleMapId.ERANGEL
) {
    val stateLock = Any()

    val mapSizeMeters: Int = if (mode == "clash_blitz") 500 else 1000
    val mapSize: Float = mapSizeMeters * 10f
    private val totalSurvivors = if (mode == "clash_blitz") 10 else 16

    val equippedChar: CatalogItem = GameCatalog.byId(profile.equippedCharacterId)
        ?: GameCatalog.byId("char_phantom")!!
    val equippedRifle: CatalogItem = GameCatalog.byId(profile.equippedRifleSkinId)
        ?: GameCatalog.byId("wpn_akm_default")!!
    val equippedSmg: CatalogItem = GameCatalog.byId(profile.equippedSmgSkinId)
        ?: GameCatalog.byId("wpn_mp40_default")!!
    val equippedGloo: CatalogItem = GameCatalog.byId(profile.equippedGlooSkinId)
        ?: GameCatalog.byId("gloo_default")!!
    val equippedAttachment: CatalogItem = GameCatalog.byId(profile.equippedAttachmentId)
        ?: GameCatalog.byId("att_holo_sight")!!
    val equippedHat: CatalogItem = GameCatalog.byId(profile.equippedHatId)
        ?: GameCatalog.byId("hat_none")!!
    val equippedOutfit: CatalogItem = GameCatalog.byId(profile.equippedOutfitId)
        ?: GameCatalog.byId("outfit_default")!!
    val equippedAccessory: CatalogItem = GameCatalog.byId(profile.equippedAccessoryId)
        ?: GameCatalog.byId("acc_none")!!

    private val totalMaxHpBonus =
        equippedChar.maxHpBonus + equippedHat.maxHpBonus + equippedOutfit.maxHpBonus + equippedAccessory.maxHpBonus
    private val totalSpeedBonusPct =
        equippedChar.speedBonusPct + equippedOutfit.speedBonusPct + equippedAccessory.speedBonusPct
    private val totalRangeBonus = equippedAttachment.rangeBonus + equippedHat.rangeBonus
    private val totalHeadshotBonusPct = equippedAttachment.headshotBonusPct + equippedHat.headshotBonusPct
    private val totalDamageBonus = equippedAttachment.damageBonus + equippedAccessory.damageBonus
    private val totalFireRateBonus = equippedAttachment.fireRateBonus

    val obstacles: List<ObstacleRect> = buildObstaclesForMap(mapId, mapSize)
    val vehicles: MutableList<VehicleProp> = buildVehiclesForMap(mapId, mapSize).toMutableList()
    val trees: List<TreeProp> = buildTreesForMap(mapId, mapSize)

    val player: Combatant
    val bots: MutableList<Combatant> = mutableListOf()
    val bullets: MutableList<Bullet> = mutableListOf()
    val glooWalls: MutableList<GlooBarrier> = mutableListOf()
    val lootDrops: MutableList<LootDrop> = mutableListOf()
    val popups: MutableList<DamagePopup> = mutableListOf()
    val killFeed: MutableList<KillFeedEntry> = mutableListOf()

    var cameraYaw: Float = (-PI / 2.0).toFloat()
    var cameraPitch: Float = -0.06f
    var isScoped: Boolean = false
    var isCrouched: Boolean = false
    var drivingVehicleIndex: Int? = null
    var lockedEnemyId: Int? = null

    @Volatile
    var sensitivitySettings: SensitivitySettings = SensitivitySettings()
        private set

    fun updateSensitivity(settings: SensitivitySettings) {
        sensitivitySettings = settings.clamped()
    }

    var activeWeaponIsRifle = true
    val rifleMaxAmmo = 30 + equippedAttachment.magazineBonus
    var rifleAmmo = rifleMaxAmmo
    val smgMaxAmmo = 38 + equippedAttachment.magazineBonus
    var smgAmmo = smgMaxAmmo
    var reloadTimer = 0f
    var medkits = 3 + if ((inventory["cons_medkit_pack"]?.quantity ?: 0) > 0) 2 else 0
    var glooCharges = 4 + if ((inventory["cons_gloo_supply"]?.quantity ?: 0) > 0) 2 else 0
    var dashCooldown = 0f
    var healTimer = 0f
    var hitFlashTimer = 0f

    var zoneCenterX = mapSize * 0.5f
    var zoneCenterY = mapSize * 0.5f
    var zoneRadius = mapSize * 0.48f
    val minZoneRadius = 260f

    var elapsedSeconds = 0f
    var playerHeadshots = 0
    var playerDamageDealt = 0
    var matchOutcome: ArenaMatchOutcome? = null

    private var nextEntityId = 100
    private val botNames = listOf(
        "Mortal_Pro", "ScoutX", "Jonathan99", "BlazeKing", "NovaAWM",
        "ApexWolf", "GhostAce", "RiotSquad", "Titan_07", "PyroFang",
        "ErangelPro", "Reaper", "ZeroCool", "Crimson", "Valiant"
    )

    init {
        val playerMaxHp = 200f + totalMaxHpBonus.toFloat()
        val hasKevlarBooster = (inventory["cons_kevlar_kit"]?.quantity ?: 0) > 0
        val startShield = 60f + if (hasKevlarBooster) 40f else 0f
        val capShield = if (hasKevlarBooster) 120f else 100f
        player = Combatant(
            id = 0,
            name = profile.callsign,
            x = mapSize * 0.5f,
            y = mapSize * 0.65f,
            angleRad = cameraYaw,
            hp = playerMaxHp,
            maxHp = playerMaxHp,
            shield = startShield,
            maxShield = capShield,
            accentHex = equippedOutfit.takeIf { it.id != "outfit_default" }?.accentHex ?: equippedChar.accentHex
        )

        repeat(totalSurvivors - 1) { index ->
            // Place early engagements within reachable 32m-120m radius and distribute the rest across the 500m-1km arena
            val spawn = if (index < 4) {
                findPositionNearPlayer(minDist = 340f, maxDist = 1150f)
            } else {
                findFreePosition(minDistFromPlayer = 750f)
            }
            bots.add(
                Combatant(
                    id = index + 1,
                    name = botNames[index % botNames.size],
                    x = spawn.x,
                    y = spawn.y,
                    angleRad = Random.nextFloat() * 6.28f,
                    hp = if (mode == "clash_blitz") 130f else 155f,
                    maxHp = if (mode == "clash_blitz") 130f else 155f,
                    shield = 30f,
                    maxShield = 80f,
                    aiTargetAngle = Random.nextFloat() * 6.28f,
                    aiWanderTimer = Random.nextFloat() * 2.5f
                )
            )
        }

        repeat(36) { i ->
            val pos = findFreePosition(minDistFromPlayer = 110f)
            val type = when (i % 4) {
                0 -> LootType.MEDKIT
                1 -> LootType.GLOO_PACK
                2 -> LootType.SHIELD_VEST
                else -> LootType.AMMO_CRATE
            }
            lootDrops.add(LootDrop(nextEntityId++, pos.x, pos.y, type, isSupplyDrop = i < 6))
        }

        // Spawn tactical interior floor loot inside every enterable compound house, villa & stilt hut
        obstacles.filter { isEnterableStructure(it.structureType) }.forEachIndexed { idx, obs ->
            val cx = obs.x + obs.w * 0.5f
            val cy = obs.y + obs.h * 0.5f
            val primaryType = when (idx % 4) {
                0 -> LootType.SHIELD_VEST
                1 -> LootType.MEDKIT
                2 -> LootType.AMMO_CRATE
                else -> LootType.GLOO_PACK
            }
            lootDrops.add(LootDrop(nextEntityId++, cx - obs.w * 0.18f, cy, primaryType, isSupplyDrop = false))
            lootDrops.add(LootDrop(nextEntityId++, cx + obs.w * 0.18f, cy, LootType.AMMO_CRATE, isSupplyDrop = false))
        }
    }

    fun aliveCount(): Int = synchronized(stateLock) {
        (if (player.alive) 1 else 0) + bots.count { it.alive }
    }

    fun compassBearingDegrees(): Int = synchronized(stateLock) {
        // -PI/2 corresponds to North (0 deg)
        val deg = Math.toDegrees((cameraYaw + PI / 2.0)).toInt()
        ((deg % 360) + 360) % 360
    }

    fun activeWeapon(): CatalogItem = if (activeWeaponIsRifle) equippedRifle else equippedSmg

    fun rotateCamera(deltaScreenX: Float, deltaScreenY: Float) = synchronized(stateLock) {
        val s = sensitivitySettings
        val sens = when {
            isScoped && equippedAttachment.id == "att_thermal_4x" -> 0.0020f * s.thermal4xSens
            isScoped -> 0.0028f * s.redDotAdsSens
            else -> 0.0048f * s.generalCameraSens
        }
        val pitchSign = if (s.invertY) 1f else -1f
        cameraYaw += deltaScreenX * sens
        cameraPitch = (cameraPitch + pitchSign * deltaScreenY * sens * 0.75f).coerceIn(-0.48f, 0.42f)
    }

    fun toggleScope() = synchronized(stateLock) {
        isScoped = !isScoped
    }

    fun toggleCrouch() = synchronized(stateLock) {
        isCrouched = !isCrouched
    }

    fun jump() = synchronized(stateLock) {
        if (player.alive && player.z <= 0.2f) {
            isCrouched = false
            player.vz = 175f
        }
    }

    fun triggerReload() = synchronized(stateLock) {
        if (reloadTimer <= 0f) {
            reloadTimer = 0.85f
        }
    }

    fun switchWeapon() = synchronized(stateLock) {
        activeWeaponIsRifle = !activeWeaponIsRifle
        reloadTimer = 0f
    }

    fun useMedkit() = synchronized(stateLock) {
        if (medkits > 0 && healTimer <= 0f && player.hp < player.maxHp) {
            medkits--
            healTimer = 1.0f
        }
    }

    fun deployGlooWall() = synchronized(stateLock) {
        if (glooCharges <= 0 || !player.alive) return@synchronized
        glooCharges--
        val dist = 56f
        val facing = cameraYaw
        val gx = (player.x + cos(facing) * dist).coerceIn(40f, mapSize - 40f)
        val gy = (player.y + sin(facing) * dist).coerceIn(40f, mapSize - 40f)
        val maxHp = 180f + equippedGloo.maxHpBonus.toFloat()
        glooWalls.add(
            GlooBarrier(
                id = nextEntityId++,
                x = gx,
                y = gy,
                angleRad = facing,
                hp = maxHp,
                maxHp = maxHp,
                colorHex = equippedGloo.accentHex
            )
        )
    }

    fun nearbyVehicle(): VehicleProp? = synchronized(stateLock) {
        nearbyVehicleIndexUnlocked()?.let { vehicles[it] }
    }

    fun canEnterOrExitVehicle(): Boolean = synchronized(stateLock) {
        player.alive && (drivingVehicleIndex != null || nearbyVehicleIndexUnlocked() != null)
    }

    fun activeVehicleSpeedKmh(): Int = synchronized(stateLock) {
        drivingVehicleIndex?.let { abs(vehicles[it].speed * 0.22f).toInt() } ?: 0
    }

    fun activeVehicleHpRatio(): Float = synchronized(stateLock) {
        drivingVehicleIndex?.let { (vehicles[it].hp / vehicles[it].maxHp).coerceIn(0f, 1f) } ?: 0f
    }

    fun isPlayerIndoors(): Boolean = synchronized(stateLock) {
        isPlayerIndoorsUnlocked()
    }

    private fun isPlayerIndoorsUnlocked(): Boolean = obstacles.any { isPositionInsideBuilding(it, player.x, player.y) }

    private fun nearbyVehicleIndexUnlocked(): Int? {
        var bestIdx: Int? = null
        var bestDist = 76f
        vehicles.forEachIndexed { idx, v ->
            if (v.hp > 0f) {
                val d = hypot(v.x - player.x, v.y - player.y)
                if (d < bestDist) {
                    bestDist = d
                    bestIdx = idx
                }
            }
        }
        return bestIdx
    }

    fun toggleDriveVehicle(): Boolean = synchronized(stateLock) {
        if (!player.alive) return@synchronized false
        val currentIdx = drivingVehicleIndex
        if (currentIdx != null) {
            val v = vehicles[currentIdx]
            val rad = Math.toRadians(v.rotDeg.toDouble()).toFloat()
            val rx = -sin(rad)
            val ry = cos(rad)
            val fx = cos(rad)
            val fy = sin(rad)
            val candidates = listOf(
                Vec2(v.x - rx * 42f, v.y - ry * 42f),
                Vec2(v.x + rx * 42f, v.y + ry * 42f),
                Vec2(v.x - fx * 48f, v.y - fy * 48f),
                Vec2(v.x + fx * 48f, v.y + fy * 48f)
            )
            val exitPos = candidates.firstOrNull { pos ->
                pos.x in 24f..(mapSize - 24f) &&
                    pos.y in 24f..(mapSize - 24f) &&
                    !collidesWithObstacle(pos.x, pos.y, 18f, ignoreVehicleIdx = currentIdx)
            } ?: Vec2(v.x, (v.y + 42f).coerceIn(24f, mapSize - 24f))
            player.x = exitPos.x
            player.y = exitPos.y
            player.z = 0f
            player.vz = 0f
            v.speed *= 0.25f
            drivingVehicleIndex = null
            return@synchronized true
        }

        val targetIdx = nearbyVehicleIndexUnlocked() ?: return@synchronized false
        drivingVehicleIndex = targetIdx
        isScoped = false
        isCrouched = false
        player.z = 0f
        player.vz = 0f
        cameraYaw = Math.toRadians(vehicles[targetIdx].rotDeg.toDouble()).toFloat()
        return@synchronized true
    }

    fun dash() = synchronized(stateLock) {
        if (dashCooldown > 0f || !player.alive || drivingVehicleIndex != null) return@synchronized
        isCrouched = false
        dashCooldown = 3.2f
        moveCombatant(player, cos(cameraYaw) * 165f, sin(cameraYaw) * 165f)
    }

    fun step(
        dt: Float,
        joyX: Float,
        joyY: Float,
        isFiring: Boolean
    ) = synchronized(stateLock) {
        if (matchOutcome != null) return@synchronized
        val clampedDt = dt.coerceIn(0.001f, 0.05f)
        elapsedSeconds += clampedDt

        val shrinkRate = if (mode == "clash_blitz") 22f else 28f
        if (zoneRadius > minZoneRadius) {
            zoneRadius = (zoneRadius - shrinkRate * clampedDt).coerceAtLeast(minZoneRadius)
        }

        if (player.fireCooldown > 0f) player.fireCooldown = (player.fireCooldown - clampedDt).coerceAtLeast(0f)
        if (player.muzzleFlashTimer > 0f) player.muzzleFlashTimer = (player.muzzleFlashTimer - clampedDt).coerceAtLeast(0f)
        if (dashCooldown > 0f) dashCooldown = (dashCooldown - clampedDt).coerceAtLeast(0f)
        if (hitFlashTimer > 0f) hitFlashTimer = (hitFlashTimer - clampedDt).coerceAtLeast(0f)

        if (reloadTimer > 0f) {
            reloadTimer -= clampedDt
            if (reloadTimer <= 0f) {
                reloadTimer = 0f
                if (activeWeaponIsRifle) rifleAmmo = rifleMaxAmmo else smgAmmo = smgMaxAmmo
            }
        }

        if (healTimer > 0f) {
            healTimer -= clampedDt
            if (healTimer <= 0f) {
                healTimer = 0f
                player.hp = (player.hp + 55f).coerceAtMost(player.maxHp)
            }
        }

        if (player.alive) {
            val activeVehIdx = drivingVehicleIndex
            if (activeVehIdx != null) {
                stepDrivingVehicle(activeVehIdx, clampedDt, joyX, joyY, isFiring)
            } else {
                if (player.z > 0f || player.vz != 0f) {
                    player.z += player.vz * clampedDt
                    player.vz -= 460f * clampedDt
                    if (player.z <= 0f) {
                        player.z = 0f
                        player.vz = 0f
                    }
                }

                val speedMult = 1f + (totalSpeedBonusPct / 100f)
                val scopeSlow = if (isScoped) 0.72f else 1f
                val crouchSlow = if (isCrouched) 0.62f else 1f
                val baseSpeed = 285f * speedMult * scopeSlow * crouchSlow * (if (healTimer > 0f) 0.55f else 1f)

                val forwardInput = -joyY
                val strafeInput = joyX
                val mag = hypot(strafeInput, forwardInput)

                if (mag > 0.08f) {
                    val nForward = forwardInput / mag.coerceAtLeast(1f)
                    val nStrafe = strafeInput / mag.coerceAtLeast(1f)
                    val moveWorldX = cos(cameraYaw) * nForward - sin(cameraYaw) * nStrafe
                    val moveWorldY = sin(cameraYaw) * nForward + cos(cameraYaw) * nStrafe
                    moveCombatant(player, moveWorldX * baseSpeed * clampedDt, moveWorldY * baseSpeed * clampedDt)
                    player.isMoving = true
                    player.walkPhase += clampedDt * (if (isCrouched) 7.5f else 12f)
                    player.angleRad = if (isFiring || isScoped) cameraYaw else atan2(moveWorldY, moveWorldX)
                } else {
                    player.isMoving = false
                    player.angleRad = cameraYaw
                }

                val lockRange = (if (activeWeaponIsRifle) 820f else 580f) + totalRangeBonus.toFloat() + (if (isScoped) 240f else 0f)
                val targetInSights = findBestCrosshairTarget(lockRange)
                lockedEnemyId = targetInSights?.id

                if (isFiring && reloadTimer <= 0f && player.fireCooldown <= 0f) {
                    player.angleRad = cameraYaw
                    firePlayerBullet(targetInSights)
                }
            }

            val iterator = lootDrops.iterator()
            while (iterator.hasNext()) {
                val drop = iterator.next()
                if (hypot(drop.x - player.x, drop.y - player.y) < 42f) {
                    when (drop.type) {
                        LootType.MEDKIT -> medkits = (medkits + 1).coerceAtMost(10)
                        LootType.GLOO_PACK -> glooCharges = (glooCharges + 2).coerceAtMost(12)
                        LootType.SHIELD_VEST -> player.shield = (player.shield + 45f).coerceAtMost(player.maxShield)
                        LootType.AMMO_CRATE -> {
                            rifleAmmo = rifleMaxAmmo
                            smgAmmo = smgMaxAmmo
                        }
                    }
                    iterator.remove()
                }
            }

            if (hypot(player.x - zoneCenterX, player.y - zoneCenterY) > zoneRadius) {
                applyDamage(player, 6.5f * clampedDt, attackerId = -1, isHeadshot = false)
            }
        } else {
            lockedEnemyId = null
            drivingVehicleIndex = null
        }

        updateBots(clampedDt)
        updateBullets(clampedDt)

        popups.removeAll {
            it.lifeSec -= clampedDt
            it.lifeSec <= 0f
        }
        killFeed.removeAll {
            it.lifeSec -= clampedDt
            it.lifeSec <= 0f
        }

        if (!player.alive && matchOutcome == null) {
            val placement = (aliveCountUnlocked() + 1).coerceIn(2, totalSurvivors)
            matchOutcome = ArenaMatchOutcome(
                placement = placement,
                kills = player.kills,
                headshots = playerHeadshots,
                damageDealt = playerDamageDealt,
                survivalSeconds = elapsedSeconds.toInt(),
                isBooyah = false
            )
        } else if (player.alive && bots.none { it.alive } && matchOutcome == null) {
            matchOutcome = ArenaMatchOutcome(
                placement = 1,
                kills = player.kills,
                headshots = playerHeadshots,
                damageDealt = playerDamageDealt,
                survivalSeconds = elapsedSeconds.toInt(),
                isBooyah = true
            )
        }
    }

    private fun stepDrivingVehicle(
        vehIdx: Int,
        dt: Float,
        joyX: Float,
        joyY: Float,
        isFiring: Boolean
    ) {
        val v = vehicles[vehIdx]
        val maxFwd = when (v.type) {
            VehicleType.DESERT_BUGGY -> 760f
            VehicleType.UAZ_JEEP -> 660f
            VehicleType.PATROL_TRUCK -> 610f
        }
        val throttle = -joyY
        if (abs(throttle) > 0.08f) {
            val accel = if (throttle > 0f) 620f else 420f
            v.speed = (v.speed + throttle * accel * dt).coerceIn(-240f, maxFwd)
        } else {
            // Smooth rolling drag
            v.speed *= (1f - (2.4f * dt)).coerceIn(0f, 1f)
            if (abs(v.speed) < 4f) v.speed = 0f
        }

        if (abs(joyX) > 0.08f) {
            val steerSign = if (v.speed < -12f) -1f else 1f
            val steerRateRad = if (abs(v.speed) > 20f) 2.25f else 1.45f
            val deltaRad = joyX * steerSign * steerRateRad * dt
            v.rotDeg = ((v.rotDeg + Math.toDegrees(deltaRad.toDouble()).toFloat()) % 360f)
            cameraYaw += deltaRad * 0.72f
        }

        val rad = Math.toRadians(v.rotDeg.toDouble()).toFloat()
        val dx = cos(rad) * v.speed * dt
        val dy = sin(rad) * v.speed * dt
        val vehRadius = 26f
        val tryX = (v.x + dx).coerceIn(vehRadius, mapSize - vehRadius)
        if (!collidesWithObstacle(tryX, v.y, vehRadius, ignoreVehicleIdx = vehIdx)) {
            v.x = tryX
        } else {
            v.speed *= -0.25f
        }
        val tryY = (v.y + dy).coerceIn(vehRadius, mapSize - vehRadius)
        if (!collidesWithObstacle(v.x, tryY, vehRadius, ignoreVehicleIdx = vehIdx)) {
            v.y = tryY
        } else {
            v.speed *= -0.25f
        }

        v.wheelSpinRad += (v.speed / 9f) * dt
        player.x = v.x
        player.y = v.y
        player.z = 7f
        player.angleRad = rad
        player.isMoving = abs(v.speed) > 12f

        // Vehicle ramming collision against enemy bots
        if (abs(v.speed) > 110f) {
            for (bot in bots) {
                if (!bot.alive) continue
                val d = hypot(bot.x - v.x, bot.y - v.y)
                if (d < 36f) {
                    val impactDmg = (abs(v.speed) * 0.22f).coerceIn(45f, 105f)
                    applyDamage(bot, impactDmg, attackerId = 0, isHeadshot = false)
                    playerDamageDealt += impactDmg.toInt()
                    popups.add(
                        DamagePopup(
                            id = nextEntityId++,
                            x = bot.x,
                            y = bot.y,
                            z = bot.z + 44f,
                            amount = impactDmg.toInt(),
                            isHeadshot = false
                        )
                    )
                    val pushA = atan2(bot.y - v.y, bot.x - v.x)
                    moveCombatant(bot, cos(pushA) * 42f, sin(pushA) * 42f)
                    v.speed *= 0.72f
                }
            }
        }

        // Drive-by shooting along aim direction
        val lockRange = 720f + totalRangeBonus.toFloat()
        val targetInSights = findBestCrosshairTarget(lockRange)
        lockedEnemyId = targetInSights?.id
        if (isFiring && reloadTimer <= 0f && player.fireCooldown <= 0f) {
            firePlayerBullet(targetInSights)
        }
    }

    fun buildRenderSnapshot(): Arena3DSnapshot = synchronized(stateLock) {
        Arena3DSnapshot(
            elapsedSeconds = elapsedSeconds,
            mapId = mapId,
            mapSize = mapSize,
            cameraYaw = cameraYaw,
            cameraPitch = cameraPitch,
            isScoped = isScoped,
            isPlayerCrouched = isCrouched,
            isDriving = drivingVehicleIndex != null,
            isPlayerIndoors = isPlayerIndoorsUnlocked(),
            vehicleSpeedKmh = drivingVehicleIndex?.let { abs(vehicles[it].speed * 0.22f).toInt() } ?: 0,
            hasThermalScope = equippedAttachment.id == "att_thermal_4x",
            player = if (player.alive) player.toRender(isPlayerCrouched = isCrouched) else null,
            bots = bots.filter { it.alive }.map { it.toRender(isPlayerCrouched = false) },
            bullets = bullets.map { RenderBullet(it.x, it.y, it.z, it.vx, it.vy, it.vz, it.colorHex, it.isHeadshot) },
            glooWalls = glooWalls.map { RenderGloo(it.x, it.y, it.angleRad, (it.hp / it.maxHp).coerceIn(0f, 1f), it.colorHex) },
            lootDrops = lootDrops.map { RenderLoot(it.x, it.y, it.type, it.isSupplyDrop) },
            vehicles = vehicles.mapIndexed { idx, v ->
                RenderVehicle(
                    x = v.x,
                    y = v.y,
                    rotDeg = v.rotDeg,
                    wheelSpinRad = v.wheelSpinRad,
                    type = v.type,
                    bodyColorHex = v.bodyColorHex,
                    accentHex = v.accentHex,
                    hpRatio = (v.hp / v.maxHp).coerceIn(0f, 1f),
                    isOccupied = drivingVehicleIndex == idx
                )
            },
            zoneCenterX = zoneCenterX,
            zoneCenterY = zoneCenterY,
            zoneRadius = zoneRadius,
            hatId = equippedHat.id,
            hatColorHex = equippedHat.accentHex,
            outfitId = equippedOutfit.id,
            outfitColorHex = equippedOutfit.takeIf { it.id != "outfit_default" }?.accentHex ?: equippedChar.accentHex,
            characterId = equippedChar.id,
            accessoryId = equippedAccessory.id,
            accessoryColorHex = equippedAccessory.accentHex,
            weaponId = activeWeapon().id,
            weaponColorHex = activeWeapon().accentHex,
            attachmentId = equippedAttachment.id,
            activeWeaponIsRifle = activeWeaponIsRifle
        )
    }

    private fun Combatant.toRender(isPlayerCrouched: Boolean) = RenderCombatant(
        id = id,
        name = name,
        x = x,
        y = y,
        z = z,
        angleRad = angleRad,
        hp = hp,
        maxHp = maxHp,
        accentHex = accentHex,
        walkPhase = walkPhase,
        isMoving = isMoving,
        isMuzzleFlashing = muzzleFlashTimer > 0f,
        isCrouched = isPlayerCrouched
    )

    private fun aliveCountUnlocked(): Int = (if (player.alive) 1 else 0) + bots.count { it.alive }

    private fun findBestCrosshairTarget(maxRange: Float): Combatant? {
        var best: Combatant? = null
        var bestScore = Float.MAX_VALUE
        val coneCos = if (isScoped) 0.96f else 0.90f
        val fx = cos(cameraYaw)
        val fy = sin(cameraYaw)

        for (bot in bots) {
            if (!bot.alive) continue
            val dx = bot.x - player.x
            val dy = bot.y - player.y
            val dist = hypot(dx, dy)
            if (dist > maxRange || dist < 1f) continue
            val dot = (dx * fx + dy * fy) / dist
            if (dot >= coneCos) {
                val anglePenalty = (1f - dot) * 900f
                val score = dist + anglePenalty
                if (score < bestScore) {
                    bestScore = score
                    best = bot
                }
            }
        }
        return best
    }

    private fun firePlayerBullet(lockedTarget: Combatant?) {
        val weapon = activeWeapon()
        if (activeWeaponIsRifle) {
            if (rifleAmmo <= 0) {
                reloadTimer = 0.85f
                return
            }
            rifleAmmo--
            if (rifleAmmo == 0) reloadTimer = 0.85f
        } else {
            if (smgAmmo <= 0) {
                reloadTimer = 0.85f
                return
            }
            smgAmmo--
            if (smgAmmo == 0) reloadTimer = 0.85f
        }

        val baseDelay = if (activeWeaponIsRifle) 0.18f else 0.105f
        val rofReduction = ((weapon.fireRateBonus + totalFireRateBonus) * 0.015f)
        player.fireCooldown = (baseDelay - rofReduction).coerceAtLeast(0.055f)
        player.muzzleFlashTimer = 0.07f

        val baseDmg = if (activeWeaponIsRifle) 28 else 19
        val scopeCritBonus = if (isScoped) 0.12f else 0f
        val crouchCritBonus = if (isCrouched) 0.08f else 0f
        val hsChance = (0.24f + scopeCritBonus + crouchCritBonus + totalHeadshotBonusPct / 100f).coerceAtMost(0.82f)
        val isHeadshot = Random.nextFloat() < hsChance
        val dmg = ((baseDmg + weapon.damageBonus + totalDamageBonus) * (if (isHeadshot) 1.65f else 1f)).toInt()

        val spreadBase = if (activeWeaponIsRifle) 0.045f else 0.095f
        val spreadFactor = (if (isScoped) 0.35f else 1f) * (if (isCrouched) 0.65f else 1f)
        val spread = (Random.nextFloat() - 0.5f) * spreadBase * spreadFactor

        val muzzleX = player.x + cos(cameraYaw) * 18f
        val muzzleY = player.y + sin(cameraYaw) * 18f
        val muzzleZ = player.z + (if (isCrouched) 18f else 25f)

        val speed = if (activeWeaponIsRifle) 1320f else 1140f
        val shotYaw: Float
        val vz: Float

        if (lockedTarget != null) {
            val dx = lockedTarget.x - muzzleX
            val dy = lockedTarget.y - muzzleY
            val horizDist = hypot(dx, dy).coerceAtLeast(1f)
            shotYaw = atan2(dy, dx) + spread * 0.4f
            val targetZ = lockedTarget.z + (if (isHeadshot) 30f else 22f)
            vz = ((targetZ - muzzleZ) / horizDist) * speed
            player.angleRad = shotYaw
        } else {
            shotYaw = cameraYaw + spread
            vz = sin(cameraPitch) * speed
        }

        val cosPitch = cos(cameraPitch).coerceAtLeast(0.3f)
        bullets.add(
            Bullet(
                id = nextEntityId++,
                x = muzzleX,
                y = muzzleY,
                z = muzzleZ,
                vx = cos(shotYaw) * speed * (if (lockedTarget != null) 1f else cosPitch),
                vy = sin(shotYaw) * speed * (if (lockedTarget != null) 1f else cosPitch),
                vz = vz,
                ownerId = 0,
                damage = dmg,
                isHeadshot = isHeadshot,
                colorHex = weapon.accentHex,
                lifeSec = 1.45f
            )
        )
    }

    private fun updateBots(dt: Float) {
        for (bot in bots) {
            if (!bot.alive) continue
            if (bot.fireCooldown > 0f) bot.fireCooldown = (bot.fireCooldown - dt).coerceAtLeast(0f)
            if (bot.muzzleFlashTimer > 0f) bot.muzzleFlashTimer = (bot.muzzleFlashTimer - dt).coerceAtLeast(0f)

            val distFromCenter = hypot(bot.x - zoneCenterX, bot.y - zoneCenterY)
            if (distFromCenter > zoneRadius * 0.88f) {
                bot.aiTargetAngle = atan2(zoneCenterY - bot.y, zoneCenterX - bot.x)
                applyDamage(bot, if (distFromCenter > zoneRadius) 7f * dt else 0f, attackerId = -1, isHeadshot = false)
            } else {
                bot.aiWanderTimer -= dt
                if (bot.aiWanderTimer <= 0f) {
                    bot.aiWanderTimer = 1.4f + Random.nextFloat() * 1.8f
                    bot.aiTargetAngle += (Random.nextFloat() - 0.5f) * 2.2f
                }
            }

            val target = findNearestBotTarget(bot)
            if (target != null) {
                val dist = hypot(target.x - bot.x, target.y - bot.y)
                bot.angleRad = atan2(target.y - bot.y, target.x - bot.x)
                bot.isMoving = true
                bot.walkPhase += dt * 9f
                if (dist > 220f) {
                    moveCombatant(bot, cos(bot.angleRad) * 165f * dt, sin(bot.angleRad) * 165f * dt)
                } else {
                    val strafeAngle = bot.angleRad + 1.57f
                    moveCombatant(bot, cos(strafeAngle) * 115f * dt, sin(strafeAngle) * 115f * dt)
                }
                if (dist < 560f && bot.fireCooldown <= 0f) {
                    bot.fireCooldown = 0.58f + Random.nextFloat() * 0.45f
                    bot.muzzleFlashTimer = 0.08f
                    val spread = (Random.nextFloat() - 0.5f) * 0.22f
                    val a = bot.angleRad + spread
                    val speed = 840f
                    val vz = ((target.z + 22f - (bot.z + 24f)) / dist.coerceAtLeast(1f)) * speed
                    bullets.add(
                        Bullet(
                            id = nextEntityId++,
                            x = bot.x + cos(a) * 20f,
                            y = bot.y + sin(a) * 20f,
                            z = bot.z + 24f,
                            vx = cos(a) * speed,
                            vy = sin(a) * speed,
                            vz = vz,
                            ownerId = bot.id,
                            damage = 11,
                            isHeadshot = false,
                            colorHex = 0xFFFF8A65,
                            lifeSec = 1.25f
                        )
                    )
                }
            } else {
                bot.angleRad = bot.aiTargetAngle
                bot.isMoving = true
                bot.walkPhase += dt * 8f
                moveCombatant(bot, cos(bot.aiTargetAngle) * 140f * dt, sin(bot.aiTargetAngle) * 140f * dt)
            }
        }
    }

    private fun updateBullets(dt: Float) {
        val iter = bullets.iterator()
        while (iter.hasNext()) {
            val b = iter.next()
            b.x += b.vx * dt
            b.y += b.vy * dt
            b.z += b.vz * dt
            b.lifeSec -= dt

            if (b.lifeSec <= 0f || b.z < 0f || b.z > 240f || b.x < 0f || b.y < 0f || b.x > mapSize || b.y > mapSize) {
                iter.remove()
                continue
            }

            if (obstacles.any { bulletHitsStructure(it, b.x, b.y, b.z) }) {
                iter.remove()
                continue
            }

            val hitVehIdx = vehicles.indexOfFirst { v ->
                v.hp > 0f && hypot(b.x - v.x, b.y - v.y) < 34f && b.z <= 28f
            }
            if (hitVehIdx != -1) {
                val hitVeh = vehicles[hitVehIdx]
                if (!(b.ownerId == 0 && drivingVehicleIndex == hitVehIdx)) {
                    hitVeh.hp = (hitVeh.hp - b.damage.toFloat() * 0.45f).coerceAtLeast(0f)
                    if (hitVeh.hp <= 0f && drivingVehicleIndex == hitVehIdx) {
                        toggleDriveVehicle()
                    }
                    iter.remove()
                    continue
                }
            }

            val hitWall = glooWalls.firstOrNull { hypot(b.x - it.x, b.y - it.y) < 34f && b.z <= 48f }
            if (hitWall != null && b.ownerId != 0) {
                hitWall.hp -= b.damage.toFloat()
                if (hitWall.hp <= 0f) glooWalls.remove(hitWall)
                iter.remove()
                continue
            }

            if (b.ownerId != 0 && player.alive &&
                hypot(b.x - player.x, b.y - player.y) < 20f &&
                abs(b.z - (player.z + 20f)) < 24f
            ) {
                applyDamage(player, b.damage.toFloat(), b.ownerId, b.isHeadshot)
                hitFlashTimer = 0.22f
                iter.remove()
                continue
            }

            val hitBot = bots.firstOrNull {
                it.alive && it.id != b.ownerId &&
                    hypot(b.x - it.x, b.y - it.y) < 22f &&
                    abs(b.z - (it.z + 20f)) < 24f
            }
            if (hitBot != null) {
                applyDamage(hitBot, b.damage.toFloat(), b.ownerId, b.isHeadshot)
                if (b.ownerId == 0) {
                    playerDamageDealt += b.damage
                    popups.add(
                        DamagePopup(
                            id = nextEntityId++,
                            x = hitBot.x,
                            y = hitBot.y,
                            z = hitBot.z + 44f,
                            amount = b.damage,
                            isHeadshot = b.isHeadshot
                        )
                    )
                }
                iter.remove()
            }
        }
    }

    private fun applyDamage(
        target: Combatant,
        amount: Float,
        attackerId: Int,
        isHeadshot: Boolean
    ) {
        if (!target.alive || amount <= 0f) return
        var remaining = amount
        if (target.shield > 0f) {
            val absorbed = minOf(target.shield, remaining * 0.65f)
            target.shield -= absorbed
            remaining -= absorbed
        }
        target.hp -= remaining
        if (target.hp <= 0f) {
            target.hp = 0f
            target.alive = false
            lootDrops.add(LootDrop(nextEntityId++, target.x, target.y, LootType.MEDKIT))

            val killerName = when (attackerId) {
                0 -> {
                    player.kills++
                    if (isHeadshot) playerHeadshots++
                    player.name
                }
                -1 -> "Blue Zone"
                else -> {
                    val killerBot = bots.firstOrNull { it.id == attackerId }
                    killerBot?.kills = (killerBot?.kills ?: 0) + 1
                    killerBot?.name ?: "Survivor"
                }
            }
            val hsTag = if (isHeadshot) " [HEADSHOT]" else ""
            killFeed.add(
                0,
                KillFeedEntry(
                    id = nextEntityId++,
                    text = "$killerName eliminated ${target.name}$hsTag",
                    isPlayerKill = attackerId == 0
                )
            )
            if (killFeed.size > 4) killFeed.removeAt(killFeed.lastIndex)
        }
    }

    private fun findNearestBotTarget(bot: Combatant): Combatant? {
        var best: Combatant? = null
        var bestDist = 680f
        if (player.alive) {
            val d = hypot(player.x - bot.x, player.y - bot.y)
            if (d < bestDist) {
                bestDist = d
                best = player
            }
        }
        for (other in bots) {
            if (!other.alive || other.id == bot.id) continue
            val d = hypot(other.x - bot.x, other.y - bot.y)
            if (d < bestDist) {
                bestDist = d
                best = other
            }
        }
        return best
    }

    private fun moveCombatant(c: Combatant, dx: Float, dy: Float) {
        val radius = 18f
        val ignoreVeh = if (c.id == 0) drivingVehicleIndex ?: -1 else -1
        val tryX = (c.x + dx).coerceIn(radius, mapSize - radius)
        if (!collidesWithObstacle(tryX, c.y, radius, ignoreVehicleIdx = ignoreVeh)) {
            c.x = tryX
        }
        val tryY = (c.y + dy).coerceIn(radius, mapSize - radius)
        if (!collidesWithObstacle(c.x, tryY, radius, ignoreVehicleIdx = ignoreVeh)) {
            c.y = tryY
        }
    }

    fun isEnterableStructure(type: StructureType): Boolean =
        type == StructureType.COMPOUND_HOUSE ||
            type == StructureType.HACIENDA_VILLA ||
            type == StructureType.STILT_HUT

    fun isPositionInsideBuilding(o: ObstacleRect, px: Float, py: Float): Boolean {
        if (!isEnterableStructure(o.structureType)) return false
        return px in (o.x + 2f)..(o.x + o.w - 2f) && py in (o.y + 2f)..(o.y + o.h - 2f)
    }

    fun collidesWithStructure(o: ObstacleRect, cx: Float, cy: Float, r: Float): Boolean {
        if (cx + r <= o.x || cx - r >= o.x + o.w || cy + r <= o.y || cy - r >= o.y + o.h) {
            return false
        }
        if (!isEnterableStructure(o.structureType)) {
            return true
        }
        val wallT = 9f
        val doorHalfW = 24f
        val midX = o.x + o.w * 0.5f
        val doorLeft = midX - doorHalfW
        val doorRight = midX + doorHalfW

        fun hitsBox(bx1: Float, by1: Float, bx2: Float, by2: Float): Boolean =
            cx + r > bx1 && cx - r < bx2 && cy + r > by1 && cy - r < by2

        return hitsBox(o.x, o.y, o.x + wallT, o.y + o.h) || // Left wall
            hitsBox(o.x + o.w - wallT, o.y, o.x + o.w, o.y + o.h) || // Right wall
            hitsBox(o.x, o.y, doorLeft, o.y + wallT) || // Back-left wall
            hitsBox(doorRight, o.y, o.x + o.w, o.y + wallT) || // Back-right wall
            hitsBox(o.x, o.y + o.h - wallT, doorLeft, o.y + o.h) || // Front-left wall
            hitsBox(doorRight, o.y + o.h - wallT, o.x + o.w, o.y + o.h) // Front-right wall
    }

    private fun bulletHitsStructure(o: ObstacleRect, bx: Float, by: Float, bz: Float): Boolean {
        if (bx !in o.x..(o.x + o.w) || by !in o.y..(o.y + o.h)) return false
        if (!isEnterableStructure(o.structureType)) return bz <= o.height
        if (bz > o.height && bz <= o.height + 24f) return true
        if (bz > o.height) return false

        val wallT = 9f
        val doorHalfW = 24f
        val midX = o.x + o.w * 0.5f
        val doorLeft = midX - doorHalfW
        val doorRight = midX + doorHalfW

        val onSideWalls = bx <= o.x + wallT || bx >= o.x + o.w - wallT
        val onFrontBackWall = by <= o.y + wallT || by >= o.y + o.h - wallT
        if (onSideWalls) return true
        if (onFrontBackWall) {
            return (bx < doorLeft || bx > doorRight) || bz > 36f
        }
        return false
    }

    private fun collidesWithObstacle(
        x: Float,
        y: Float,
        r: Float,
        ignoreVehicleIdx: Int = -1
    ): Boolean {
        val hitBuilding = obstacles.any { o -> collidesWithStructure(o, x, y, r) }
        if (hitBuilding) return true
        return vehicles.indices.any { idx ->
            if (idx == ignoreVehicleIdx) return@any false
            val v = vehicles[idx]
            hypot(x - v.x, y - v.y) < r + 24f
        }
    }

    private fun findPositionNearPlayer(minDist: Float, maxDist: Float): Vec2 {
        val px = mapSize * 0.5f
        val py = mapSize * 0.65f
        repeat(40) {
            val angle = Random.nextFloat() * 6.283f
            val dist = minDist + Random.nextFloat() * (maxDist - minDist)
            val x = (px + cos(angle) * dist).coerceIn(140f, mapSize - 140f)
            val y = (py + sin(angle) * dist).coerceIn(140f, mapSize - 140f)
            val insideOuterBox = obstacles.any { o ->
                x + 28f > o.x && x - 28f < o.x + o.w && y + 28f > o.y && y - 28f < o.y + o.h
            }
            if (!insideOuterBox && !collidesWithObstacle(x, y, 28f)) {
                return Vec2(x, y)
            }
        }
        return findFreePosition(minDist)
    }

    private fun findFreePosition(minDistFromPlayer: Float): Vec2 {
        repeat(50) {
            val x = Random.nextFloat() * (mapSize - 320f) + 160f
            val y = Random.nextFloat() * (mapSize - 320f) + 160f
            val insideOuterBox = obstacles.any { o ->
                x + 28f > o.x && x - 28f < o.x + o.w && y + 28f > o.y && y - 28f < o.y + o.h
            }
            if (!insideOuterBox &&
                !collidesWithObstacle(x, y, 28f) &&
                hypot(x - mapSize * 0.5f, y - mapSize * 0.65f) >= minDistFromPlayer
            ) {
                return Vec2(x, y)
            }
        }
        return Vec2(mapSize * 0.2f, mapSize * 0.2f)
    }

    private fun buildObstaclesForMap(mapId: BattleMapId, s: Float): List<ObstacleRect> {
        fun rect(
            fx: Float, fy: Float,
            w: Float, h: Float,
            height: Float, wallColorHex: Long, roofColorHex: Long,
            accentHex: Long, structureType: StructureType,
            hasSpire: Boolean = false, label: String = ""
        ) = ObstacleRect(
            x = s * fx,
            y = s * fy,
            w = w,
            h = h,
            height = height,
            wallColorHex = wallColorHex,
            roofColorHex = roofColorHex,
            accentHex = accentHex,
            hasSpire = hasSpire,
            structureType = structureType,
            label = label
        )

        return when (mapId) {
            BattleMapId.ERANGEL -> listOf(
                // Central Pochinki & Roadside Compounds
                rect(0.45f, 0.43f, 175f, 145f, 92f, 0xFF78716C, 0xFF9A3412, 0xFFF5B400, StructureType.COMPOUND_HOUSE, label = "POCHINKI"),
                rect(0.56f, 0.56f, 155f, 130f, 84f, 0xFF64748B, 0xFF7C2D12, 0xFF10E88A, StructureType.COMPOUND_HOUSE, label = "POCHINKI SOUTH"),
                rect(0.41f, 0.58f, 115f, 34f, 30f, 0xFF927E5B, 0xFFA8946E, 0xFFF5B400, StructureType.SANDBAG_WALL),
                rect(0.59f, 0.40f, 75f, 75f, 118f, 0xFF475569, 0xFF334155, 0xFF00E5FF, StructureType.WATCHTOWER),
                // NW School Sector
                rect(0.16f, 0.16f, 165f, 145f, 148f, 0xFF6B7280, 0xFF991B1B, 0xFFF5B400, StructureType.CLOCK_TOWER, hasSpire = true, label = "SCHOOL TOWER"),
                rect(0.26f, 0.21f, 165f, 135f, 88f, 0xFF64748B, 0xFF9A3412, 0xFFF5B400, StructureType.COMPOUND_HOUSE, label = "ROZHOK"),
                rect(0.14f, 0.34f, 75f, 75f, 112f, 0xFF475569, 0xFF334155, 0xFF10E88A, StructureType.WATCHTOWER),
                rect(0.34f, 0.14f, 120f, 36f, 30f, 0xFF927E5B, 0xFFA8946E, 0xFFF5B400, StructureType.SANDBAG_WALL),
                // NE Yasnaya Compounds
                rect(0.74f, 0.17f, 195f, 135f, 94f, 0xFF64748B, 0xFF9A3412, 0xFFF5B400, StructureType.COMPOUND_HOUSE, label = "YASNAYA"),
                rect(0.84f, 0.28f, 155f, 130f, 82f, 0xFF57534E, 0xFF7C2D12, 0xFF10E88A, StructureType.COMPOUND_HOUSE),
                rect(0.67f, 0.29f, 75f, 75f, 116f, 0xFF475569, 0xFF334155, 0xFF00E5FF, StructureType.WATCHTOWER),
                rect(0.80f, 0.12f, 185f, 90f, 58f, 0xFF1D4ED8, 0xFF1E3A8A, 0xFF38BDF8, StructureType.SHIPPING_CONTAINER),
                // SW Georgopol Docks
                rect(0.16f, 0.72f, 195f, 92f, 58f, 0xFF1D4ED8, 0xFF1E3A8A, 0xFF38BDF8, StructureType.SHIPPING_CONTAINER, label = "GEORGOPOL"),
                rect(0.27f, 0.80f, 195f, 92f, 58f, 0xFF0F766E, 0xFF115E59, 0xFF10E88A, StructureType.SHIPPING_CONTAINER),
                rect(0.14f, 0.59f, 165f, 130f, 84f, 0xFF78716C, 0xFF9A3412, 0xFFF5B400, StructureType.COMPOUND_HOUSE, label = "GATKA"),
                rect(0.32f, 0.67f, 115f, 34f, 28f, 0xFF927E5B, 0xFFA8946E, 0xFFF5B400, StructureType.SANDBAG_WALL),
                // SE Sosnovka Military Base
                rect(0.73f, 0.73f, 195f, 92f, 58f, 0xFFB91C1C, 0xFF7F1D1D, 0xFFF5B400, StructureType.SHIPPING_CONTAINER, label = "SOSNOVKA"),
                rect(0.82f, 0.82f, 175f, 140f, 92f, 0xFF6B7280, 0xFF991B1B, 0xFFF5B400, StructureType.COMPOUND_HOUSE, label = "MILITARY HQ"),
                rect(0.68f, 0.83f, 75f, 75f, 118f, 0xFF475569, 0xFF334155, 0xFF10E88A, StructureType.WATCHTOWER),
                rect(0.84f, 0.63f, 120f, 36f, 30f, 0xFF927E5B, 0xFFA8946E, 0xFFF5B400, StructureType.SANDBAG_WALL)
            )

            BattleMapId.MIRAMAR -> listOf(
                // Central Hacienda
                rect(0.45f, 0.43f, 200f, 155f, 94f, 0xFFD6B588, 0xFFB45309, 0xFFF5B400, StructureType.HACIENDA_VILLA, label = "HACIENDA"),
                rect(0.57f, 0.56f, 165f, 130f, 80f, 0xFFC29B6B, 0xFF9A3412, 0xFFF5B400, StructureType.COMPOUND_HOUSE, label = "SAN MARTIN"),
                rect(0.40f, 0.57f, 115f, 38f, 32f, 0xFFB08968, 0xFFDDB892, 0xFFF5B400, StructureType.SANDBAG_WALL),
                rect(0.59f, 0.39f, 80f, 80f, 108f, 0xFF854D0E, 0xFF422006, 0xFFF5B400, StructureType.WATCHTOWER),
                // NW Refinery
                rect(0.16f, 0.16f, 120f, 120f, 152f, 0xFF78350F, 0xFF451A03, 0xFFFF6B1A, StructureType.OIL_DERRICK, hasSpire = true, label = "REFINERY"),
                rect(0.28f, 0.19f, 170f, 130f, 82f, 0xFFD4A373, 0xFF9A3412, 0xFFF5B400, StructureType.HACIENDA_VILLA, label = "EL POZO"),
                rect(0.14f, 0.34f, 80f, 80f, 108f, 0xFF854D0E, 0xFF422006, 0xFFF5B400, StructureType.WATCHTOWER),
                rect(0.35f, 0.31f, 115f, 38f, 32f, 0xFFB08968, 0xFFDDB892, 0xFFF5B400, StructureType.SANDBAG_WALL),
                // NE Pecado Arena
                rect(0.74f, 0.17f, 195f, 145f, 88f, 0xFFC29B6B, 0xFF9A3412, 0xFFF5B400, StructureType.HACIENDA_VILLA, label = "PECADO"),
                rect(0.84f, 0.30f, 155f, 125f, 78f, 0xFFD6B588, 0xFFB45309, 0xFF00E5FF, StructureType.COMPOUND_HOUSE),
                rect(0.68f, 0.25f, 120f, 120f, 146f, 0xFF7C2D12, 0xFF431407, 0xFFFF6B1A, StructureType.OIL_DERRICK, hasSpire = true),
                rect(0.79f, 0.11f, 185f, 96f, 62f, 0xFFA16207, 0xFF713F12, 0xFFF5B400, StructureType.SHIPPING_CONTAINER),
                // SW Los Leones
                rect(0.16f, 0.72f, 190f, 100f, 64f, 0xFFA16207, 0xFF713F12, 0xFFF5B400, StructureType.SHIPPING_CONTAINER, label = "LOS LEONES"),
                rect(0.27f, 0.81f, 175f, 135f, 86f, 0xFFD4A373, 0xFFB45309, 0xFFF5B400, StructureType.HACIENDA_VILLA),
                rect(0.14f, 0.59f, 155f, 120f, 76f, 0xFFC29B6B, 0xFF9A3412, 0xFFF5B400, StructureType.COMPOUND_HOUSE),
                rect(0.33f, 0.69f, 115f, 38f, 32f, 0xFFB08968, 0xFFDDB892, 0xFFF5B400, StructureType.SANDBAG_WALL),
                // SE Valle del Mar Oil Fields
                rect(0.75f, 0.75f, 125f, 125f, 148f, 0xFF7C2D12, 0xFF431407, 0xFFF5B400, StructureType.OIL_DERRICK, hasSpire = true, label = "VALLE RIG"),
                rect(0.83f, 0.63f, 170f, 135f, 84f, 0xFFD6B588, 0xFFB45309, 0xFFF5B400, StructureType.HACIENDA_VILLA),
                rect(0.67f, 0.83f, 80f, 80f, 108f, 0xFF854D0E, 0xFF422006, 0xFFF5B400, StructureType.WATCHTOWER),
                rect(0.69f, 0.67f, 115f, 38f, 32f, 0xFFB08968, 0xFFDDB892, 0xFFF5B400, StructureType.SANDBAG_WALL)
            )

            BattleMapId.SANHOK -> listOf(
                // Central Ancient Temple Ruins
                rect(0.45f, 0.43f, 180f, 180f, 116f, 0xFF57534E, 0xFF44403C, 0xFF00E5FF, StructureType.TEMPLE_RUIN, hasSpire = true, label = "RUINS"),
                rect(0.57f, 0.56f, 145f, 120f, 80f, 0xFF78350F, 0xFFA16207, 0xFF10E88A, StructureType.STILT_HUT, label = "PAI NAN"),
                rect(0.40f, 0.58f, 110f, 36f, 30f, 0xFF57534E, 0xFF78716C, 0xFF00E5FF, StructureType.SANDBAG_WALL),
                rect(0.60f, 0.40f, 75f, 75f, 110f, 0xFF78350F, 0xFF92400E, 0xFF10E88A, StructureType.WATCHTOWER),
                // NW Paradise Resort
                rect(0.16f, 0.16f, 155f, 125f, 82f, 0xFF78350F, 0xFFA16207, 0xFF10E88A, StructureType.STILT_HUT, label = "PARADISE"),
                rect(0.28f, 0.22f, 165f, 165f, 108f, 0xFF57534E, 0xFF292524, 0xFF00E5FF, StructureType.TEMPLE_RUIN, hasSpire = true),
                rect(0.14f, 0.34f, 75f, 75f, 110f, 0xFF78350F, 0xFF92400E, 0xFF10E88A, StructureType.WATCHTOWER),
                rect(0.35f, 0.14f, 140f, 115f, 78f, 0xFF713F12, 0xFF854D0E, 0xFF10E88A, StructureType.STILT_HUT),
                // NE Bootcamp
                rect(0.73f, 0.17f, 165f, 130f, 84f, 0xFF713F12, 0xFF854D0E, 0xFF00E5FF, StructureType.STILT_HUT, label = "BOOTCAMP"),
                rect(0.84f, 0.28f, 150f, 120f, 80f, 0xFF78350F, 0xFFA16207, 0xFF10E88A, StructureType.STILT_HUT),
                rect(0.67f, 0.29f, 75f, 75f, 112f, 0xFF78350F, 0xFF92400E, 0xFF00E5FF, StructureType.WATCHTOWER),
                rect(0.80f, 0.11f, 175f, 92f, 58f, 0xFF0F766E, 0xFF115E59, 0xFF10E88A, StructureType.SHIPPING_CONTAINER),
                // SW Docks
                rect(0.16f, 0.72f, 185f, 92f, 58f, 0xFF0F766E, 0xFF115E59, 0xFF10E88A, StructureType.SHIPPING_CONTAINER, label = "DOCKS"),
                rect(0.28f, 0.81f, 155f, 125f, 82f, 0xFF78350F, 0xFFA16207, 0xFF10E88A, StructureType.STILT_HUT),
                rect(0.14f, 0.59f, 145f, 120f, 80f, 0xFF713F12, 0xFF854D0E, 0xFF00E5FF, StructureType.STILT_HUT),
                rect(0.32f, 0.67f, 110f, 36f, 30f, 0xFF57534E, 0xFF78716C, 0xFF00E5FF, StructureType.SANDBAG_WALL),
                // SE Quarry & Cave Temple
                rect(0.75f, 0.74f, 165f, 165f, 102f, 0xFF57534E, 0xFF292524, 0xFF00E5FF, StructureType.TEMPLE_RUIN, hasSpire = true, label = "CAVE TEMPLE"),
                rect(0.84f, 0.62f, 150f, 120f, 82f, 0xFF78350F, 0xFFA16207, 0xFF10E88A, StructureType.STILT_HUT),
                rect(0.67f, 0.83f, 75f, 75f, 110f, 0xFF78350F, 0xFF92400E, 0xFF10E88A, StructureType.WATCHTOWER),
                rect(0.69f, 0.67f, 110f, 36f, 30f, 0xFF57534E, 0xFF78716C, 0xFF00E5FF, StructureType.SANDBAG_WALL)
            )
        }
    }

    private fun buildVehiclesForMap(mapId: BattleMapId, s: Float): List<VehicleProp> {
        fun v(fx: Float, fy: Float, rot: Float, type: VehicleType, bodyHex: Long, accentHex: Long) =
            VehicleProp(s * fx, s * fy, rot, type, bodyHex, accentHex)

        // Vehicle #0 spawns near player starting road position (0.50, 0.65) for instant vehicle combat!
        val spawnNearX = 0.5f - 78f / s
        val spawnNearY = 0.65f - 48f / s

        return when (mapId) {
            BattleMapId.ERANGEL -> listOf(
                v(spawnNearX, spawnNearY, 18f, VehicleType.UAZ_JEEP, 0xFF3F4E3A, 0xFFF5B400),
                v(0.50f, 0.34f, -90f, VehicleType.UAZ_JEEP, 0xFF334155, 0xFF00E5FF),
                v(0.32f, 0.50f, 0f, VehicleType.PATROL_TRUCK, 0xFF475569, 0xFFF5B400),
                v(0.69f, 0.50f, 180f, VehicleType.UAZ_JEEP, 0xFF4D5E46, 0xFFF5B400),
                v(0.22f, 0.25f, 45f, VehicleType.UAZ_JEEP, 0xFF3F4E3A, 0xFF10E88A),
                v(0.78f, 0.24f, 135f, VehicleType.PATROL_TRUCK, 0xFF334155, 0xFFF5B400),
                v(0.24f, 0.77f, -35f, VehicleType.PATROL_TRUCK, 0xFF1E3A8A, 0xFF38BDF8),
                v(0.77f, 0.78f, -145f, VehicleType.UAZ_JEEP, 0xFF7F1D1D, 0xFFF5B400),
                v(0.50f, 0.14f, 90f, VehicleType.DESERT_BUGGY, 0xFFD97706, 0xFFF5B400),
                v(0.50f, 0.86f, -90f, VehicleType.UAZ_JEEP, 0xFF3F4E3A, 0xFFF5B400),
                v(0.14f, 0.50f, 0f, VehicleType.DESERT_BUGGY, 0xFF0F766E, 0xFF00E5FF),
                v(0.86f, 0.50f, 180f, VehicleType.PATROL_TRUCK, 0xFF475569, 0xFFF5B400)
            )
            BattleMapId.MIRAMAR -> listOf(
                v(spawnNearX, spawnNearY, 15f, VehicleType.DESERT_BUGGY, 0xFFD97706, 0xFFF5B400),
                v(0.50f, 0.34f, -85f, VehicleType.PATROL_TRUCK, 0xFF92400E, 0xFFF5B400),
                v(0.32f, 0.50f, 5f, VehicleType.DESERT_BUGGY, 0xFFB45309, 0xFF00E5FF),
                v(0.69f, 0.50f, 175f, VehicleType.UAZ_JEEP, 0xFFA16207, 0xFFF5B400),
                v(0.22f, 0.25f, 40f, VehicleType.DESERT_BUGGY, 0xFFD97706, 0xFFFF6B1A),
                v(0.78f, 0.24f, 140f, VehicleType.DESERT_BUGGY, 0xFFB45309, 0xFFF5B400),
                v(0.24f, 0.77f, -40f, VehicleType.PATROL_TRUCK, 0xFF78350F, 0xFFF5B400),
                v(0.77f, 0.78f, -135f, VehicleType.UAZ_JEEP, 0xFF92400E, 0xFFFF6B1A),
                v(0.50f, 0.14f, 90f, VehicleType.DESERT_BUGGY, 0xFFF59E0B, 0xFFFFFFFF),
                v(0.50f, 0.86f, -90f, VehicleType.PATROL_TRUCK, 0xFF78350F, 0xFFF5B400),
                v(0.14f, 0.50f, 0f, VehicleType.DESERT_BUGGY, 0xFFD97706, 0xFF00E5FF),
                v(0.86f, 0.50f, 180f, VehicleType.UAZ_JEEP, 0xFFA16207, 0xFFF5B400)
            )
            BattleMapId.SANHOK -> listOf(
                v(spawnNearX, spawnNearY, 24f, VehicleType.PATROL_TRUCK, 0xFF065F46, 0xFF10E88A),
                v(0.50f, 0.34f, -88f, VehicleType.DESERT_BUGGY, 0xFF0F766E, 0xFF00E5FF),
                v(0.32f, 0.50f, 0f, VehicleType.UAZ_JEEP, 0xFF1E3A2F, 0xFF10E88A),
                v(0.69f, 0.50f, 180f, VehicleType.PATROL_TRUCK, 0xFF115E59, 0xFFF5B400),
                v(0.22f, 0.25f, 35f, VehicleType.DESERT_BUGGY, 0xFF065F46, 0xFF00E5FF),
                v(0.78f, 0.24f, 130f, VehicleType.UAZ_JEEP, 0xFF1E3A2F, 0xFF10E88A),
                v(0.24f, 0.77f, -45f, VehicleType.PATROL_TRUCK, 0xFF0F766E, 0xFFF5B400),
                v(0.77f, 0.78f, -130f, VehicleType.DESERT_BUGGY, 0xFF047857, 0xFF00E5FF),
                v(0.50f, 0.14f, 90f, VehicleType.UAZ_JEEP, 0xFF1E3A2F, 0xFF10E88A),
                v(0.50f, 0.86f, -90f, VehicleType.PATROL_TRUCK, 0xFF065F46, 0xFFF5B400),
                v(0.14f, 0.50f, 0f, VehicleType.DESERT_BUGGY, 0xFF0F766E, 0xFF10E88A),
                v(0.86f, 0.50f, 180f, VehicleType.UAZ_JEEP, 0xFF115E59, 0xFF00E5FF)
            )
        }
    }

    private fun buildTreesForMap(mapId: BattleMapId, s: Float): List<TreeProp> {
        val style = when (mapId) {
            BattleMapId.ERANGEL -> FoliageStyle.PINE
            BattleMapId.MIRAMAR -> FoliageStyle.DESERT_SCRUB
            BattleMapId.SANHOK -> FoliageStyle.JUNGLE_PALM
        }
        val list = ArrayList<TreeProp>(64)
        val rng = Random(mapId.ordinal * 9973 + s.toInt())
        val half = s * 0.5f
        var attempts = 0
        while (list.size < 64 && attempts < 500) {
            attempts++
            val x = rng.nextFloat() * (s - 400f) + 200f
            val y = rng.nextFloat() * (s - 400f) + 200f
            // Keep clear of central plaza & tarmac crossroads
            if (abs(x - half) < 120f || abs(y - half) < 120f) continue
            if (abs(x - half) < 280f && abs(y - half) < 280f) continue
            // Keep clear of player spawn & vehicles & structures
            if (hypot(x - s * 0.5f, y - s * 0.65f) < 120f) continue
            val hitsStructure = obstacles.any { o ->
                x in (o.x - 45f)..(o.x + o.w + 45f) && y in (o.y - 45f)..(o.y + o.h + 45f)
            }
            if (hitsStructure) continue
            val scale = 0.85f + rng.nextFloat() * 0.55f
            list.add(TreeProp(x, y, scale, style))
        }
        return list
    }
}
