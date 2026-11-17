const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfilePayload(uid) {
  return {
    userId: uid,
    callsign: "Phantom01",
    diamonds: 9999999,
    gold: 9999999,
    royaleTokens: 10,
    diamondRoyaleLuck: 0,
    weaponRoyaleLuck: 0,
    goldRoyaleLuck: 0,
    incubatorLuck: 0,
    equippedCharacterId: "char_phantom",
    equippedRifleSkinId: "wpn_akm_default",
    equippedSmgSkinId: "wpn_mp40_default",
    equippedGlooSkinId: "gloo_default",
    equippedAttachmentId: "att_holo_sight",
    equippedHatId: "hat_none",
    equippedOutfitId: "outfit_default",
    equippedAccessoryId: "acc_none",
    matchesPlayed: 0,
    booyahs: 0,
    totalKills: 0,
    totalHeadshots: 0,
    rankPoints: 1000,
    createdAt: new Date(),
    updatedAt: new Date(),
  };
}

function validInventoryItem(uid, itemId) {
  return {
    itemId,
    userId: uid,
    category: "weapon",
    name: "Draco AKM",
    rarity: "mythic",
    acquiredVia: "luck_royale",
    quantity: 1,
    createdAt: new Date(),
    updatedAt: new Date(),
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write user profiles or inventory", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID)));
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("inventory").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("matches").get());
});

test("Authenticated user can create, read, and update own profile with valid actions", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);

  await assertSucceeds(profileRef.set(validProfilePayload(ALICE_UID)));
  await assertSucceeds(profileRef.get());

  // Action 1: Currency & luck update
  await assertSucceeds(
    profileRef.update({
      diamonds: 9999399,
      diamondRoyaleLuck: 10,
      royaleTokens: 15,
      updatedAt: new Date(),
    })
  );

  // Action 2: Loadout & customization update
  await assertSucceeds(
    profileRef.update({
      equippedCharacterId: "char_valkyrie",
      equippedAttachmentId: "att_thermal_4x",
      equippedHatId: "hat_samurai_kabuto",
      equippedOutfitId: "outfit_amber_jacket",
      equippedAccessoryId: "acc_wings_blaze",
      updatedAt: new Date(),
    })
  );

  // Action 3: Match stats update
  await assertSucceeds(
    profileRef.update({
      matchesPlayed: 1,
      booyahs: 1,
      totalKills: 7,
      totalHeadshots: 3,
      rankPoints: 1045,
      updatedAt: new Date(),
    })
  );
});

test("Shadow update & identity spoofing rejected on user profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID)));

  // Bob cannot read or overwrite Alice's profile
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ callsign: "Hacked", updatedAt: new Date() }));

  // Alice cannot inject ghost field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(),
    })
  );

  // Alice cannot create profile with Bob's UID inside payload
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      ...validProfilePayload(BOB_UID),
    })
  );
});

test("Inventory, match history, and Luck Royale spin records enforce ownership and immutability", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const itemRef = aliceDb.collection("users").doc(ALICE_UID).collection("inventory").doc("wpn_draco_akm");
  await assertSucceeds(itemRef.set(validInventoryItem(ALICE_UID, "wpn_draco_akm")));
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("inventory").doc("hat_samurai_kabuto").set({
      ...validInventoryItem(ALICE_UID, "hat_samurai_kabuto"),
      category: "hat",
      name: "Ember Shogun Kabuto",
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("inventory").doc("cons_medkit_pack").set({
      ...validInventoryItem(ALICE_UID, "cons_medkit_pack"),
      category: "consumable",
      name: "Tactical Medkit Crate",
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("inventory").get());

  // Update quantity is allowed
  await assertSucceeds(itemRef.update({ quantity: 2, updatedAt: new Date() }));

  // Category mutation is rejected
  await assertFails(itemRef.update({ category: "bundle", updatedAt: new Date() }));

  // Bob cannot list or read Alice's inventory
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("inventory").get());

  // Match record is create-only (immutable)
  const matchRef = aliceDb.collection("users").doc(ALICE_UID).collection("matches").doc("match_1");
  await assertSucceeds(
    matchRef.set({
      userId: ALICE_UID,
      mode: "battle_royale",
      placement: 1,
      kills: 8,
      headshots: 4,
      damageDealt: 1640,
      survivalSeconds: 185,
      booyah: true,
      rankPointsDelta: 45,
      diamondsEarned: 500,
      goldEarned: 1500,
      createdAt: new Date(),
    })
  );
  await assertFails(matchRef.update({ kills: 99 }));

  // Luck Royale spin record is create-only (immutable)
  const spinRef = aliceDb.collection("users").doc(ALICE_UID).collection("royale_history").doc("spin_1");
  await assertSucceeds(
    spinRef.set({
      userId: ALICE_UID,
      bannerId: "diamond_royale",
      spinCount: 10,
      rewardIds: ["char_phantom", "wpn_draco_akm"],
      highestRarity: "mythic",
      createdAt: new Date(),
    })
  );
  await assertFails(spinRef.update({ spinCount: 1 }));
});
