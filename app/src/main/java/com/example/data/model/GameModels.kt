package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val callsign: String = "Survivor",
    val diamonds: Long = 9_999_999L,
    val gold: Long = 9_999_999L,
    val royaleTokens: Long = 0L,
    val diamondRoyaleLuck: Int = 0,
    val weaponRoyaleLuck: Int = 0,
    val goldRoyaleLuck: Int = 0,
    val incubatorLuck: Int = 0,
    val equippedCharacterId: String = "char_phantom",
    val equippedRifleSkinId: String = "wpn_akm_default",
    val equippedSmgSkinId: String = "wpn_mp40_default",
    val equippedGlooSkinId: String = "gloo_default",
    val equippedAttachmentId: String = "att_holo_sight",
    val equippedHatId: String = "hat_none",
    val equippedOutfitId: String = "outfit_default",
    val equippedAccessoryId: String = "acc_none",
    val matchesPlayed: Int = 0,
    val booyahs: Int = 0,
    val totalKills: Int = 0,
    val totalHeadshots: Int = 0,
    val rankPoints: Int = 1000,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class OwnedItem(
    @DocumentId val id: String = "",
    val itemId: String = "",
    val userId: String = "",
    val category: String = "weapon",
    val name: String = "",
    val rarity: String = "rare",
    val acquiredVia: String = "starter",
    val quantity: Int = 1,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MatchRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val mode: String = "battle_royale",
    val placement: Int = 1,
    val kills: Int = 0,
    val headshots: Int = 0,
    val damageDealt: Int = 0,
    val survivalSeconds: Int = 0,
    val booyah: Boolean = false,
    val rankPointsDelta: Int = 0,
    val diamondsEarned: Long = 0L,
    val goldEarned: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class RoyaleSpinRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val bannerId: String = "diamond_royale",
    val spinCount: Int = 1,
    val rewardIds: List<String> = emptyList(),
    val highestRarity: String = "rare",
    @ServerTimestamp val createdAt: Timestamp? = null
)

enum class ItemRarity(val key: String, val label: String) {
    MYTHIC("mythic", "MYTHIC"),
    LEGENDARY("legendary", "LEGENDARY"),
    EPIC("epic", "EPIC"),
    RARE("rare", "RARE");

    companion object {
        fun fromKey(key: String): ItemRarity = entries.firstOrNull { it.key == key } ?: RARE
    }
}

enum class ItemCategory(val key: String, val label: String) {
    CHARACTER("character", "Character"),
    WEAPON("weapon", "Weapon Skin"),
    ATTACHMENT("attachment", "Attachment"),
    OUTFIT("outfit", "Outfit"),
    HAT("hat", "Hat"),
    ACCESSORY("accessory", "Accessory"),
    CONSUMABLE("consumable", "Consumable"),
    GLOO("gloo", "Gloo Wall"),
    BUNDLE("bundle", "Bundle");

    companion object {
        fun fromKey(key: String): ItemCategory = entries.firstOrNull { it.key == key } ?: WEAPON
    }
}

enum class CurrencyType {
    DIAMONDS,
    GOLD
}

enum class WeaponSlot {
    RIFLE,
    SMG
}

enum class BattleMapId(
    val key: String,
    val title: String,
    val subtitle: String,
    val sizeBadge: String,
    val accentHex: Long
) {
    ERANGEL(
        key = "erangel",
        title = "ERANGEL SECTOR",
        subtitle = "Pochinki Compounds • School Tower • Georgopol Port & Sosnovka UAZ",
        sizeBadge = "TEMPERATE 1KM / 500M",
        accentHex = 0xFF10E88A
    ),
    MIRAMAR(
        key = "miramar",
        title = "MIRAMAR CANYON",
        subtitle = "Hacienda Villa • Twin Refineries • Pecado & Los Leones Buggy",
        sizeBadge = "DESERT 1KM / 500M",
        accentHex = 0xFFF5B400
    ),
    SANHOK(
        key = "sanhok",
        title = "SANHOK JUNGLE",
        subtitle = "Ancient Temple • Paradise Resort • Bootcamp Stilt Huts & Patrol Rig",
        sizeBadge = "JUNGLE 1KM / 500M",
        accentHex = 0xFF00E5FF
    );

    companion object {
        fun fromKey(key: String): BattleMapId = entries.firstOrNull { it.key == key } ?: ERANGEL
    }
}

data class CatalogItem(
    val id: String,
    val name: String,
    val subtitle: String,
    val category: ItemCategory,
    val rarity: ItemRarity,
    val price: Long,
    val currency: CurrencyType,
    val weaponSlot: WeaponSlot? = null,
    val damageBonus: Int = 0,
    val fireRateBonus: Int = 0,
    val speedBonusPct: Int = 0,
    val maxHpBonus: Int = 0,
    val rangeBonus: Int = 0,
    val magazineBonus: Int = 0,
    val headshotBonusPct: Int = 0,
    val accentHex: Long = 0xFFFF6B1A,
    val perkDescription: String = "",
    val inStore: Boolean = true,
    val imageKey: String = "",
    val bundleItemIds: List<String> = emptyList(),
    val originalPrice: Long = 0L
)

enum class RoyaleBannerId(
    val key: String,
    val title: String,
    val subtitle: String,
    val singleCost: Long,
    val multiCost: Long,
    val multiLabel: String,
    val multiCount: Int,
    val currency: CurrencyType,
    val grandPrizeId: String
) {
    DIAMOND_ROYALE(
        key = "diamond_royale",
        title = "DIAMOND ROYALE",
        subtitle = "Mythic Cyber-Samurai & Tactical Operatives",
        singleCost = 60L,
        multiCost = 600L,
        multiLabel = "10+1 SPINS",
        multiCount = 11,
        currency = CurrencyType.DIAMONDS,
        grandPrizeId = "char_inferno_ronin"
    ),
    WEAPON_ROYALE(
        key = "weapon_royale",
        title = "WEAPON ROYALE",
        subtitle = "Evo Draco AKM & Custom Tracer Firearms",
        singleCost = 40L,
        multiCost = 400L,
        multiLabel = "10+1 SPINS",
        multiCount = 11,
        currency = CurrencyType.DIAMONDS,
        grandPrizeId = "wpn_draco_akm"
    ),
    GOLD_ROYALE(
        key = "gold_royale",
        title = "GOLD ROYALE",
        subtitle = "Tactical Gear, Gloo Shields & Recon Skins",
        singleCost = 300L,
        multiCost = 3000L,
        multiLabel = "10+1 SPINS",
        multiCount = 11,
        currency = CurrencyType.GOLD,
        grandPrizeId = "gloo_cyber_aegis"
    ),
    INCUBATOR(
        key = "incubator",
        title = "EVO INCUBATOR",
        subtitle = "Ultra-Rare Mythic Vault & High-Damage Skins",
        singleCost = 80L,
        multiCost = 400L,
        multiLabel = "5 SPINS",
        multiCount = 5,
        currency = CurrencyType.DIAMONDS,
        grandPrizeId = "wpn_cobra_mp40"
    )
}

data class SpinOutcome(
    val banner: RoyaleBannerId,
    val items: List<CatalogItem>,
    val newlyUnlockedIds: Set<String>,
    val bonusTokens: Int,
    val newLuckValue: Int,
    val hitPityJackpot: Boolean
)

object GameCatalog {
    val allItems: List<CatalogItem> = listOf(
        // Characters / Bundles
        CatalogItem(
            id = "char_phantom",
            name = "Phantom Striker",
            subtitle = "Crimson Visor Operative",
            category = ItemCategory.CHARACTER,
            rarity = ItemRarity.EPIC,
            price = 0L,
            currency = CurrencyType.DIAMONDS,
            speedBonusPct = 5,
            maxHpBonus = 10,
            accentHex = 0xFFFF6B1A,
            perkDescription = "Dropzone Fury: +5% Sprint Speed, +10 Max HP",
            inStore = false,
            imageKey = "phantom"
        ),
        CatalogItem(
            id = "char_valkyrie",
            name = "Valkyrie Recon",
            subtitle = "Electric Cyan Tactical Specialist",
            category = ItemCategory.CHARACTER,
            rarity = ItemRarity.LEGENDARY,
            price = 1200L,
            currency = CurrencyType.DIAMONDS,
            speedBonusPct = 14,
            maxHpBonus = 15,
            accentHex = 0xFF00E5FF,
            perkDescription = "Jet Dash: +14% Sprint Speed, +15 Max HP",
            inStore = true,
            imageKey = "valkyrie"
        ),
        CatalogItem(
            id = "char_inferno_ronin",
            name = "Inferno Ronin",
            subtitle = "Mythic Ember Shogun Bundle",
            category = ItemCategory.CHARACTER,
            rarity = ItemRarity.MYTHIC,
            price = 2499L,
            currency = CurrencyType.DIAMONDS,
            speedBonusPct = 15,
            maxHpBonus = 35,
            accentHex = 0xFFFF2A55,
            perkDescription = "Shogun Armor: +35 Max HP, +15% Sprint Speed",
            inStore = true,
            imageKey = "phantom"
        ),
        CatalogItem(
            id = "char_neon_viper",
            name = "Cyber Viper",
            subtitle = "Venom Stealth Assassin",
            category = ItemCategory.CHARACTER,
            rarity = ItemRarity.LEGENDARY,
            price = 15000L,
            currency = CurrencyType.GOLD,
            speedBonusPct = 10,
            maxHpBonus = 20,
            accentHex = 0xFF00E676,
            perkDescription = "Toxic Reflex: +10% Move Speed, +20 Max HP",
            inStore = true,
            imageKey = "valkyrie"
        ),
        CatalogItem(
            id = "char_gold_titan",
            name = "Apex Titan",
            subtitle = "Heavy Gold Ballistic Juggernaut",
            category = ItemCategory.CHARACTER,
            rarity = ItemRarity.EPIC,
            price = 8500L,
            currency = CurrencyType.GOLD,
            speedBonusPct = 4,
            maxHpBonus = 30,
            accentHex = 0xFFFFB800,
            perkDescription = "Iron Plating: +30 Max HP",
            inStore = true,
            imageKey = "phantom"
        ),

        // Rifle / Primary Weapon Skins
        CatalogItem(
            id = "wpn_akm_default",
            name = "Tactical AKM",
            subtitle = "Standard Issue 7.62mm Rifle",
            category = ItemCategory.WEAPON,
            rarity = ItemRarity.RARE,
            price = 0L,
            currency = CurrencyType.GOLD,
            weaponSlot = WeaponSlot.RIFLE,
            damageBonus = 0,
            fireRateBonus = 0,
            accentHex = 0xFF9AA6C0,
            perkDescription = "Balanced mid-range precision rifle",
            inStore = false
        ),
        CatalogItem(
            id = "wpn_draco_akm",
            name = "Evo Draco AKM",
            subtitle = "Mythic Flame Wing Assault Rifle",
            category = ItemCategory.WEAPON,
            rarity = ItemRarity.MYTHIC,
            price = 1999L,
            currency = CurrencyType.DIAMONDS,
            weaponSlot = WeaponSlot.RIFLE,
            damageBonus = 9,
            fireRateBonus = 2,
            accentHex = 0xFFFF2A55,
            perkDescription = "+9 Damage per shot, +2 Fire Rate, Crimson Flame Tracers",
            inStore = true
        ),
        CatalogItem(
            id = "wpn_scar_titan",
            name = "Titan SCAR-L",
            subtitle = "Cybernetic Gold Carbine",
            category = ItemCategory.WEAPON,
            rarity = ItemRarity.LEGENDARY,
            price = 1100L,
            currency = CurrencyType.DIAMONDS,
            weaponSlot = WeaponSlot.RIFLE,
            damageBonus = 6,
            fireRateBonus = 2,
            accentHex = 0xFFFFB800,
            perkDescription = "+6 Damage, +2 Fire Rate, Gold Ballistic Tracers",
            inStore = true
        ),
        CatalogItem(
            id = "wpn_m4_frost",
            name = "Glacier M4A1",
            subtitle = "Cryo-Core Tactical Rifle",
            category = ItemCategory.WEAPON,
            rarity = ItemRarity.EPIC,
            price = 12000L,
            currency = CurrencyType.GOLD,
            weaponSlot = WeaponSlot.RIFLE,
            damageBonus = 4,
            fireRateBonus = 1,
            accentHex = 0xFF00E5FF,
            perkDescription = "+4 Damage, +1 Fire Rate, Ice Blue Tracers",
            inStore = true
        ),

        // SMG / Close-Quarters Weapon Skins
        CatalogItem(
            id = "wpn_mp40_default",
            name = "Urban MP40",
            subtitle = "Standard Issue Submachine Gun",
            category = ItemCategory.WEAPON,
            rarity = ItemRarity.RARE,
            price = 0L,
            currency = CurrencyType.GOLD,
            weaponSlot = WeaponSlot.SMG,
            damageBonus = 0,
            fireRateBonus = 0,
            accentHex = 0xFF9AA6C0,
            perkDescription = "High-cadence close-range shredder",
            inStore = false
        ),
        CatalogItem(
            id = "wpn_cobra_mp40",
            name = "Predator Cobra MP40",
            subtitle = "Mythic Venomfang SMG",
            category = ItemCategory.WEAPON,
            rarity = ItemRarity.MYTHIC,
            price = 2199L,
            currency = CurrencyType.DIAMONDS,
            weaponSlot = WeaponSlot.SMG,
            damageBonus = 7,
            fireRateBonus = 3,
            accentHex = 0xFFFF3B5C,
            perkDescription = "+7 Damage, +3 Rapid Fire, Crimson Fang Tracers",
            inStore = true
        ),
        CatalogItem(
            id = "wpn_venom_ump",
            name = "Neon Venom UMP",
            subtitle = "Biohazard Pulse SMG",
            category = ItemCategory.WEAPON,
            rarity = ItemRarity.LEGENDARY,
            price = 899L,
            currency = CurrencyType.DIAMONDS,
            weaponSlot = WeaponSlot.SMG,
            damageBonus = 5,
            fireRateBonus = 2,
            accentHex = 0xFF00E676,
            perkDescription = "+5 Damage, +2 Fire Rate, Emerald Pulse Tracers",
            inStore = true
        ),
        CatalogItem(
            id = "wpn_gold_m1887",
            name = "Pharaoh M1887",
            subtitle = "Gilded Rapid Shotgun SMG Hybrid",
            category = ItemCategory.WEAPON,
            rarity = ItemRarity.EPIC,
            price = 9500L,
            currency = CurrencyType.GOLD,
            weaponSlot = WeaponSlot.SMG,
            damageBonus = 6,
            fireRateBonus = 1,
            accentHex = 0xFFFFB800,
            perkDescription = "+6 Impact Damage, +1 Fire Rate",
            inStore = true
        ),

        // Gloo Wall Skins
        CatalogItem(
            id = "gloo_default",
            name = "Standard Gloo Wall",
            subtitle = "Cryo-Polymer Deployable Cover",
            category = ItemCategory.GLOO,
            rarity = ItemRarity.RARE,
            price = 0L,
            currency = CurrencyType.GOLD,
            maxHpBonus = 0,
            accentHex = 0xFF448AFF,
            perkDescription = "Blocks incoming enemy fire in the arena",
            inStore = false
        ),
        CatalogItem(
            id = "gloo_cyber_aegis",
            name = "Cyber Aegis Wall",
            subtitle = "Reinforced Hex-Shield Barrier",
            category = ItemCategory.GLOO,
            rarity = ItemRarity.LEGENDARY,
            price = 799L,
            currency = CurrencyType.DIAMONDS,
            maxHpBonus = 60,
            accentHex = 0xFF00E5FF,
            perkDescription = "+60 Wall Durability, Neon Cyan Hex Grid",
            inStore = true
        ),
        CatalogItem(
            id = "gloo_dragon_scale",
            name = "Crimson Dragon Wall",
            subtitle = "Mythic Flame Bastion Shield",
            category = ItemCategory.GLOO,
            rarity = ItemRarity.MYTHIC,
            price = 1499L,
            currency = CurrencyType.DIAMONDS,
            maxHpBonus = 110,
            accentHex = 0xFFFF2A55,
            perkDescription = "+110 Wall Durability, Molten Shield Aura",
            inStore = true
        ),
        CatalogItem(
            id = "gloo_obsidian_fort",
            name = "Obsidian Gold Wall",
            subtitle = "Heavy Armored Street Barricade",
            category = ItemCategory.GLOO,
            rarity = ItemRarity.EPIC,
            price = 6000L,
            currency = CurrencyType.GOLD,
            maxHpBonus = 40,
            accentHex = 0xFFFFB800,
            perkDescription = "+40 Wall Durability, Gold Trim",
            inStore = true
        ),

        // Weapon Attachments
        CatalogItem(
            id = "att_holo_sight",
            name = "Tactical Holo Sight",
            subtitle = "Standard Issue Optical Reflex",
            category = ItemCategory.ATTACHMENT,
            rarity = ItemRarity.RARE,
            price = 0L,
            currency = CurrencyType.GOLD,
            rangeBonus = 20,
            headshotBonusPct = 2,
            accentHex = 0xFF29B6F6,
            perkDescription = "+20 Lock Range, +2% Headshot Chance",
            inStore = false
        ),
        CatalogItem(
            id = "att_thermal_4x",
            name = "Mythic Thermal 4X Scope",
            subtitle = "Infrared Target Acquisition Optic",
            category = ItemCategory.ATTACHMENT,
            rarity = ItemRarity.MYTHIC,
            price = 1350L,
            currency = CurrencyType.DIAMONDS,
            rangeBonus = 110,
            headshotBonusPct = 14,
            accentHex = 0xFFFF2A55,
            perkDescription = "+110 Lock Range, +14% Headshot Crit Rate",
            inStore = true
        ),
        CatalogItem(
            id = "att_drum_mag",
            name = "Titanium Drum Mag III",
            subtitle = "High-Capacity Dual-Feed Magazine",
            category = ItemCategory.ATTACHMENT,
            rarity = ItemRarity.LEGENDARY,
            price = 750L,
            currency = CurrencyType.DIAMONDS,
            magazineBonus = 15,
            fireRateBonus = 1,
            accentHex = 0xFFFFB800,
            perkDescription = "+15 Magazine Rounds, +1 Fire Rate",
            inStore = true
        ),
        CatalogItem(
            id = "att_cryo_suppressor",
            name = "Cryo-Core Suppressor",
            subtitle = "Zero-Recoil Muzzle Stabilizer",
            category = ItemCategory.ATTACHMENT,
            rarity = ItemRarity.EPIC,
            price = 5400L,
            currency = CurrencyType.GOLD,
            damageBonus = 3,
            headshotBonusPct = 8,
            accentHex = 0xFF00E5FF,
            perkDescription = "+3 Bullet Damage, +8% Headshot Crit Rate",
            inStore = true
        ),

        // Character Hats / Headgear
        CatalogItem(
            id = "hat_none",
            name = "Tactical Bare Head",
            subtitle = "Standard Operative Visor",
            category = ItemCategory.HAT,
            rarity = ItemRarity.RARE,
            price = 0L,
            currency = CurrencyType.GOLD,
            accentHex = 0xFF9AA6C0,
            perkDescription = "Default unequipped headgear slot",
            inStore = false
        ),
        CatalogItem(
            id = "hat_crimson_beret",
            name = "Crimson Commander Beret",
            subtitle = "Elite Strike Squad Headgear",
            category = ItemCategory.HAT,
            rarity = ItemRarity.EPIC,
            price = 4200L,
            currency = CurrencyType.GOLD,
            maxHpBonus = 10,
            accentHex = 0xFFFF2A55,
            perkDescription = "+10 Max HP • Crimson Commander Crest",
            inStore = true
        ),
        CatalogItem(
            id = "hat_samurai_kabuto",
            name = "Ember Shogun Kabuto",
            subtitle = "Mythic Horned Battle Helm",
            category = ItemCategory.HAT,
            rarity = ItemRarity.MYTHIC,
            price = 980L,
            currency = CurrencyType.DIAMONDS,
            maxHpBonus = 25,
            headshotBonusPct = 5,
            accentHex = 0xFFFFB800,
            perkDescription = "+25 Max HP, +5% Headshot Crit • Gold Horns",
            inStore = true
        ),
        CatalogItem(
            id = "hat_cyber_visor",
            name = "Neon Recon Helmet",
            subtitle = "Holographic AR HUD Visor",
            category = ItemCategory.HAT,
            rarity = ItemRarity.LEGENDARY,
            price = 640L,
            currency = CurrencyType.DIAMONDS,
            rangeBonus = 45,
            accentHex = 0xFF00E5FF,
            perkDescription = "+45 Target Lock Range • Cyan AR Visor",
            inStore = true
        ),

        // Character Outfits
        CatalogItem(
            id = "outfit_default",
            name = "Standard Drop Suit",
            subtitle = "Obsidian Combat Jacket",
            category = ItemCategory.OUTFIT,
            rarity = ItemRarity.RARE,
            price = 0L,
            currency = CurrencyType.GOLD,
            accentHex = 0xFF667391,
            perkDescription = "Standard issue dropzone combat attire",
            inStore = false
        ),
        CatalogItem(
            id = "outfit_amber_jacket",
            name = "Blaze Street Trench",
            subtitle = "Molten Amber Armored Coat",
            category = ItemCategory.OUTFIT,
            rarity = ItemRarity.LEGENDARY,
            price = 880L,
            currency = CurrencyType.DIAMONDS,
            speedBonusPct = 6,
            maxHpBonus = 15,
            accentHex = 0xFFFF6B1A,
            perkDescription = "+6% Sprint Speed, +15 Max HP",
            inStore = true
        ),
        CatalogItem(
            id = "outfit_neon_stealth",
            name = "Cyber Shadow Suit",
            subtitle = "Electric Cyan Exoskeleton Vest",
            category = ItemCategory.OUTFIT,
            rarity = ItemRarity.EPIC,
            price = 7800L,
            currency = CurrencyType.GOLD,
            speedBonusPct = 8,
            accentHex = 0xFF00E5FF,
            perkDescription = "+8% Sprint Speed • Lightweight Nano-Weave",
            inStore = true
        ),

        // Character Accessories
        CatalogItem(
            id = "acc_none",
            name = "No Back Accessory",
            subtitle = "Clean Silhouette",
            category = ItemCategory.ACCESSORY,
            rarity = ItemRarity.RARE,
            price = 0L,
            currency = CurrencyType.GOLD,
            accentHex = 0xFF9AA6C0,
            perkDescription = "Default unequipped accessory slot",
            inStore = false
        ),
        CatalogItem(
            id = "acc_wings_blaze",
            name = "Phoenix Flame Wings",
            subtitle = "Mythic Emissive Back Pack",
            category = ItemCategory.ACCESSORY,
            rarity = ItemRarity.MYTHIC,
            price = 1599L,
            currency = CurrencyType.DIAMONDS,
            speedBonusPct = 10,
            maxHpBonus = 20,
            accentHex = 0xFFFF6B1A,
            perkDescription = "+10% Speed, +20 Max HP • Blazing Wings",
            inStore = true
        ),
        CatalogItem(
            id = "acc_neon_aura",
            name = "Quantum Holo Halo",
            subtitle = "Spinning Cybernetic Energy Ring",
            category = ItemCategory.ACCESSORY,
            rarity = ItemRarity.LEGENDARY,
            price = 720L,
            currency = CurrencyType.DIAMONDS,
            damageBonus = 2,
            accentHex = 0xFF00E5FF,
            perkDescription = "+2 Bonus Weapon Damage • Cyan Aura",
            inStore = true
        ),

        // Functional Consumables
        CatalogItem(
            id = "cons_medkit_pack",
            name = "Tactical Medkit Crate",
            subtitle = "+2 Extra Starting Medkits per Match",
            category = ItemCategory.CONSUMABLE,
            rarity = ItemRarity.EPIC,
            price = 1200L,
            currency = CurrencyType.GOLD,
            accentHex = 0xFF00E676,
            perkDescription = "Starts your next Battle Royale with extra Medkits",
            inStore = true
        ),
        CatalogItem(
            id = "cons_gloo_supply",
            name = "Gloo Wall Supply Drop",
            subtitle = "+2 Extra Starting Gloo Walls",
            category = ItemCategory.CONSUMABLE,
            rarity = ItemRarity.EPIC,
            price = 1500L,
            currency = CurrencyType.GOLD,
            accentHex = 0xFF448AFF,
            perkDescription = "Deploys extra Gloo Wall charges in the arena",
            inStore = true
        ),
        CatalogItem(
            id = "cons_kevlar_kit",
            name = "Titanium Armor Booster",
            subtitle = "+40 Starting Tactical Shield",
            category = ItemCategory.CONSUMABLE,
            rarity = ItemRarity.LEGENDARY,
            price = 180L,
            currency = CurrencyType.DIAMONDS,
            accentHex = 0xFFFFB800,
            perkDescription = "Reinforces your starting Kevlar Shield in battle",
            inStore = true
        ),

        // Additional High-Tier Weapons
        CatalogItem(
            id = "wpn_groza_blaze",
            name = "Solaris Groza-X",
            subtitle = "Bullpup Molten Core Rifle",
            category = ItemCategory.WEAPON,
            rarity = ItemRarity.LEGENDARY,
            price = 1390L,
            currency = CurrencyType.DIAMONDS,
            weaponSlot = WeaponSlot.RIFLE,
            damageBonus = 7,
            fireRateBonus = 2,
            accentHex = 0xFFFF6B1A,
            perkDescription = "+7 Damage, +2 Fire Rate, Solar Flare Tracers",
            inStore = true
        ),
        CatalogItem(
            id = "wpn_vector_cyber",
            name = "Dual Vector Overdrive",
            subtitle = "Ultra-Rapid CQC Submachine Gun",
            category = ItemCategory.WEAPON,
            rarity = ItemRarity.EPIC,
            price = 11200L,
            currency = CurrencyType.GOLD,
            weaponSlot = WeaponSlot.SMG,
            damageBonus = 4,
            fireRateBonus = 3,
            accentHex = 0xFF00E5FF,
            perkDescription = "+4 Damage, +3 Extreme Fire Rate, Cyan Arc Tracers",
            inStore = true
        ),

        // Weapon & Gear Bundles
        CatalogItem(
            id = "bundle_inferno_shogun",
            name = "Inferno Shogun Arsenal",
            subtitle = "Mythic Ronin + Evo Draco AKM + Kabuto + Dragon Wall",
            category = ItemCategory.BUNDLE,
            rarity = ItemRarity.MYTHIC,
            price = 4200L,
            originalPrice = 6977L,
            currency = CurrencyType.DIAMONDS,
            damageBonus = 9,
            fireRateBonus = 2,
            speedBonusPct = 15,
            maxHpBonus = 170,
            headshotBonusPct = 5,
            accentHex = 0xFFFF2A55,
            perkDescription = "4-Item Mythic Pack: Ronin Operative, Evo Draco AKM, Shogun Kabuto & Dragon Gloo Wall",
            inStore = true,
            imageKey = "phantom",
            bundleItemIds = listOf(
                "char_inferno_ronin",
                "wpn_draco_akm",
                "hat_samurai_kabuto",
                "gloo_dragon_scale"
            )
        ),
        CatalogItem(
            id = "bundle_cyber_valkyrie",
            name = "Cyber Valkyrie Overdrive",
            subtitle = "Valkyrie + Venom UMP + Recon Visor + Holo Halo",
            category = ItemCategory.BUNDLE,
            rarity = ItemRarity.LEGENDARY,
            price = 2350L,
            originalPrice = 3459L,
            currency = CurrencyType.DIAMONDS,
            damageBonus = 7,
            fireRateBonus = 2,
            speedBonusPct = 14,
            maxHpBonus = 15,
            rangeBonus = 45,
            accentHex = 0xFF00E5FF,
            perkDescription = "4-Item Recon Pack: Valkyrie, Neon Venom UMP, Cyber Visor & Quantum Halo",
            inStore = true,
            imageKey = "valkyrie",
            bundleItemIds = listOf(
                "char_valkyrie",
                "wpn_venom_ump",
                "hat_cyber_visor",
                "acc_neon_aura"
            )
        ),
        CatalogItem(
            id = "bundle_cryo_assault",
            name = "Cryo Vanguard Bundle",
            subtitle = "Glacier M4A1 + Cyber Suit + Suppressor + Viper",
            category = ItemCategory.BUNDLE,
            rarity = ItemRarity.LEGENDARY,
            price = 28500L,
            originalPrice = 40200L,
            currency = CurrencyType.GOLD,
            damageBonus = 7,
            fireRateBonus = 1,
            speedBonusPct = 18,
            maxHpBonus = 20,
            headshotBonusPct = 8,
            accentHex = 0xFF00E676,
            perkDescription = "4-Item Gold Bundle: Cyber Viper, Glacier M4A1, Shadow Suit & Cryo Suppressor",
            inStore = true,
            imageKey = "valkyrie",
            bundleItemIds = listOf(
                "char_neon_viper",
                "wpn_m4_frost",
                "outfit_neon_stealth",
                "att_cryo_suppressor"
            )
        ),
        CatalogItem(
            id = "bundle_titan_strike",
            name = "Pharaoh Titan Strike Kit",
            subtitle = "Apex Titan + Pharaoh M1887 + Beret + Obsidian Wall",
            category = ItemCategory.BUNDLE,
            rarity = ItemRarity.EPIC,
            price = 19500L,
            originalPrice = 28200L,
            currency = CurrencyType.GOLD,
            damageBonus = 6,
            fireRateBonus = 1,
            speedBonusPct = 4,
            maxHpBonus = 80,
            accentHex = 0xFFFFB800,
            perkDescription = "4-Item Heavy Pack: Apex Titan, Pharaoh M1887, Commander Beret & Obsidian Wall",
            inStore = true,
            imageKey = "phantom",
            bundleItemIds = listOf(
                "char_gold_titan",
                "wpn_gold_m1887",
                "hat_crimson_beret",
                "gloo_obsidian_fort"
            )
        )
    )

    val starterIds = setOf(
        "char_phantom", "wpn_akm_default", "wpn_mp40_default",
        "gloo_default", "att_holo_sight", "hat_none", "outfit_default", "acc_none"
    )

    val tierOdds: List<Pair<ItemRarity, Int>> = listOf(
        ItemRarity.MYTHIC to 12,
        ItemRarity.LEGENDARY to 26,
        ItemRarity.EPIC to 34,
        ItemRarity.RARE to 28
    )

    fun byId(id: String): CatalogItem? = allItems.firstOrNull { it.id == id }

    fun storeItems(): List<CatalogItem> = allItems.filter { it.inStore }

    fun bundleContents(bundle: CatalogItem): List<CatalogItem> =
        bundle.bundleItemIds.mapNotNull { byId(it) }

    fun poolForBanner(banner: RoyaleBannerId): List<CatalogItem> = when (banner) {
        RoyaleBannerId.DIAMOND_ROYALE -> listOfNotNull(
            byId("char_inferno_ronin"),
            byId("char_valkyrie"),
            byId("outfit_amber_jacket"),
            byId("hat_samurai_kabuto"),
            byId("acc_wings_blaze"),
            byId("att_thermal_4x"),
            byId("char_neon_viper")
        )
        RoyaleBannerId.WEAPON_ROYALE -> listOfNotNull(
            byId("wpn_draco_akm"),
            byId("wpn_scar_titan"),
            byId("att_drum_mag"),
            byId("att_cryo_suppressor"),
            byId("wpn_venom_ump"),
            byId("wpn_m4_frost"),
            byId("wpn_gold_m1887")
        )
        RoyaleBannerId.GOLD_ROYALE -> listOfNotNull(
            byId("gloo_cyber_aegis"),
            byId("hat_crimson_beret"),
            byId("outfit_neon_stealth"),
            byId("char_neon_viper"),
            byId("char_gold_titan"),
            byId("att_cryo_suppressor"),
            byId("gloo_obsidian_fort")
        )
        RoyaleBannerId.INCUBATOR -> listOfNotNull(
            byId("wpn_cobra_mp40"),
            byId("acc_wings_blaze"),
            byId("hat_cyber_visor"),
            byId("acc_neon_aura"),
            byId("char_inferno_ronin"),
            byId("att_thermal_4x"),
            byId("gloo_dragon_scale")
        )
    }
}

fun rankTitleForPoints(points: Int): String = when {
    points >= 3200 -> "Grandmaster"
    points >= 2600 -> "Heroic"
    points >= 2100 -> "Diamond"
    points >= 1700 -> "Platinum"
    points >= 1350 -> "Gold"
    points >= 1100 -> "Silver"
    else -> "Bronze"
}
