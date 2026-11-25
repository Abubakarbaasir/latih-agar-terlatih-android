const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const PLAYER_UID = "player_alice";
const OTHER_UID = "player_bob";
const SUPER_ADMIN_UID = "super_admin_1";
const MANAGER_UID = "manager_1";
const REVIEWER_UID = "reviewer_1";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function sampleVersionData() {
  const tokens = Array.from({ length: 10 }, (_, idx) => ({
    tokenId: `tok_${idx + 1}`,
    position: idx + 1,
    baseText: "كلمة",
    sharafNotes: "",
    allowedExtraLetters: [],
  }));
  return {
    versionNumber: 1,
    sentenceUnvoweled: "كلمة ".repeat(10).trim(),
    translationNatural: "Terjemahan alami",
    translationGandul: "Terjemahan gandul",
    sentenceMeaningExplanation: "Makna kalimat",
    baseTokens: tokens,
    answerPatterns: [
      {
        patternId: "pat_1",
        primary: true,
        label: "Pola Utama",
        voweledSentence: "كَلِمَةٌ ".repeat(10).trim(),
      },
    ],
  };
}

async function seedAdminsAndCategory() {
  const now = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("admins").doc(SUPER_ADMIN_UID).set({
      userId: SUPER_ADMIN_UID,
      displayName: "Super Admin",
      email: "super@example.com",
      role: "super_admin",
      permissions: ["manage_categories", "create_question", "edit_question", "submit_review", "review_question", "approve_question", "reject_question", "publish_question", "disable_question", "manage_admins", "view_logs"],
      active: true,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("admins").doc(MANAGER_UID).set({
      userId: MANAGER_UID,
      displayName: "Manager",
      email: "manager@example.com",
      role: "question_manager",
      permissions: ["create_question", "edit_question", "submit_review"],
      active: true,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("admins").doc(REVIEWER_UID).set({
      userId: REVIEWER_UID,
      displayName: "Reviewer",
      email: "reviewer@example.com",
      role: "reviewer",
      permissions: ["review_question", "approve_question", "reject_question"],
      active: true,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("categories").doc("cat_fai").set({
      code: "FAI",
      name: "Fa'il dan Naibul Fa'il",
      description: "Latihan i'rab fa'il",
      sortOrder: 1,
      active: true,
      questionCounter: 1,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("categories").doc("cat_hidden").set({
      code: "HID",
      name: "Kategori Nonaktif",
      description: "Tidak aktif",
      sortOrder: 2,
      active: false,
      questionCounter: 0,
      createdAt: now,
      updatedAt: now,
    });
  });
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
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

test("Unauthenticated users cannot read or write any collection", async () => {
  await seedAdminsAndCategory();
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("categories").where("active", "==", true).get());
  await assertFails(unauth.collection("questions").where("status", "==", "published").get());
  await assertFails(unauth.collection("users").doc(PLAYER_UID).get());
});

test("User profile isolation and privilege escalation guard", async () => {
  const now = new Date(Date.now() - 1000);
  const alice = testEnv.authenticatedContext(PLAYER_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bob = testEnv.authenticatedContext(OTHER_UID, { email: "bob@example.com", email_verified: true }).firestore();

  // Player cannot self-assign admin role
  await assertFails(
    alice.collection("users").doc(PLAYER_UID).set({
      userId: PLAYER_UID,
      displayName: "Alice",
      email: "alice@example.com",
      role: "super_admin",
      permissions: ["manage_admins"],
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Player can create own player profile
  await assertSucceeds(
    alice.collection("users").doc(PLAYER_UID).set({
      userId: PLAYER_UID,
      displayName: "Alice",
      email: "alice@example.com",
      role: "player",
      permissions: [],
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob cannot read Alice's PII profile
  await assertFails(bob.collection("users").doc(PLAYER_UID).get());

  // Alice cannot update own role
  await assertFails(
    alice.collection("users").doc(PLAYER_UID).update({
      role: "super_admin",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Bootstrapped super admin email requires email_verified == true
  const spoofed = testEnv.authenticatedContext("spoof_uid", {
    email: "abubakarbaasir8@gmail.com",
    email_verified: false,
  }).firestore();
  await assertFails(
    spoofed.collection("users").doc("spoof_uid").set({
      userId: "spoof_uid",
      displayName: "Spoof",
      email: "abubakarbaasir8@gmail.com",
      role: "super_admin",
      permissions: [],
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Categories: players only query active categories; admins see all", async () => {
  await seedAdminsAndCategory();
  const alice = testEnv.authenticatedContext(PLAYER_UID).firestore();
  const superAdmin = testEnv.authenticatedContext(SUPER_ADMIN_UID).firestore();

  await assertSucceeds(alice.collection("categories").where("active", "==", true).get());
  await assertFails(alice.collection("categories").get());
  await assertFails(alice.collection("categories").doc("cat_hidden").get());
  await assertSucceeds(superAdmin.collection("categories").get());
});

test("Questions workflow: draft -> review -> approved -> published, player isolation", async () => {
  await seedAdminsAndCategory();
  const now = new Date(Date.now() - 2000);
  const manager = testEnv.authenticatedContext(MANAGER_UID).firestore();
  const reviewer = testEnv.authenticatedContext(REVIEWER_UID).firestore();
  const superAdmin = testEnv.authenticatedContext(SUPER_ADMIN_UID).firestore();
  const player = testEnv.authenticatedContext(PLAYER_UID).firestore();

  const qPayload = {
    code: "LAT-FAI-001",
    categoryId: "cat_fai",
    categoryCode: "FAI",
    categoryName: "Fa'il dan Naibul Fa'il",
    title: "Latihan Fa'il 1",
    instruction: "Berilah harakat akhir yang tepat.",
    difficulty: "beginner",
    source: "manual",
    status: "draft",
    createdBy: MANAGER_UID,
    createdByName: "Manager",
    currentVersionNumber: 1,
    publishedVersionNumber: 0,
    latestVersionData: sampleVersionData(),
    createdAt: now,
    updatedAt: now,
  };

  // Manager creates draft question
  await assertSucceeds(manager.collection("questions").doc("q_1").set(qPayload));

  // Player cannot read draft or list unfiltered questions
  await assertFails(player.collection("questions").doc("q_1").get());
  await assertFails(player.collection("questions").get());

  // Manager cannot directly publish
  await assertFails(
    manager.collection("questions").doc("q_1").update({
      status: "published",
      publishedVersionNumber: 1,
      publishedVersionData: sampleVersionData(),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Manager submits for review
  await assertSucceeds(
    manager.collection("questions").doc("q_1").update({
      status: "review",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reviewer approves
  await assertSucceeds(
    reviewer.collection("questions").doc("q_1").update({
      status: "approved",
      lastReviewNote: "Sudah sesuai kaidah",
      reviewedBy: "Reviewer",
      updatedAt: new Date(Date.now() - 400),
    })
  );

  // Reviewer cannot publish without publish_question permission
  await assertFails(
    reviewer.collection("questions").doc("q_1").update({
      status: "published",
      publishedVersionNumber: 1,
      publishedVersionData: sampleVersionData(),
      updatedAt: new Date(Date.now() - 300),
    })
  );

  // Super Admin publishes
  await assertSucceeds(
    superAdmin.collection("questions").doc("q_1").update({
      status: "published",
      publishedVersionNumber: 1,
      publishedVersionData: sampleVersionData(),
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Now player can query published questions in category
  await assertSucceeds(
    player.collection("questions").where("categoryId", "==", "cat_fai").where("status", "==", "published").get()
  );
});

test("Player sessions and answers: strict user ownership", async () => {
  const now = new Date(Date.now() - 2000);
  const alice = testEnv.authenticatedContext(PLAYER_UID).firestore();
  const bob = testEnv.authenticatedContext(OTHER_UID).firestore();

  await assertSucceeds(
    alice.collection("users").doc(PLAYER_UID).collection("sessions").doc("sess_1").set({
      userId: PLAYER_UID,
      categoryId: "cat_fai",
      categoryName: "Fa'il",
      status: "active",
      elapsedSeconds: 0,
      currentDifficulty: "beginner",
      questionsAnswered: 0,
      correctCount: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    alice.collection("users").doc(PLAYER_UID).collection("sessions").doc("sess_1")
      .collection("answers").doc("ans_1").set({
        userId: PLAYER_UID,
        sessionId: "sess_1",
        questionId: "q_1",
        questionCode: "LAT-FAI-001",
        versionNumber: 1,
        matchedPatternId: "pat_1",
        evaluationStatus: "CORRECT",
        score: 100,
        correctWordCount: 10,
        wrongWordCount: 0,
        incompleteWordCount: 0,
        playerTokensSummary: Array.from({ length: 10 }, () => "كَلِمَةٌ"),
        createdAt: now,
      })
  );

  await assertFails(bob.collection("users").doc(PLAYER_UID).collection("sessions").get());
  await assertFails(
    bob.collection("users").doc(PLAYER_UID).collection("sessions").doc("sess_1").collection("answers").get()
  );
});
