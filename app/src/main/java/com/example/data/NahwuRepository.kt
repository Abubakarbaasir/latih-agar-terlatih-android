package com.example.data

import android.content.Context
import com.example.R
import com.example.domain.engine.EvaluationResult
import com.example.domain.engine.PlayerTokenInput
import com.example.domain.model.*
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Locale

class NahwuRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    companion object {
        const val BOOTSTRAP_SUPER_ADMIN_EMAIL = "abubakarbaasir8@gmail.com"
    }

    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Pengguna harus masuk dengan Google terlebih dahulu.")

    private fun currentUserName(): String =
        auth.currentUser?.displayName?.takeIf { it.isNotBlank() }
            ?: auth.currentUser?.email?.substringBefore("@")
            ?: "Pengguna"

    // --- USER PROFILE & ADMIN ROLE SYNC ---

    suspend fun ensureUserProfileAndBootstrapAdmin(): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Belum masuk."))
        val uid = user.uid
        val email = user.email.orEmpty()
        val isBootstrapAdmin = email.equals(BOOTSTRAP_SUPER_ADMIN_EMAIL, ignoreCase = true) && user.isEmailVerified
        val userRef = db.collection("users").document(uid)
        val adminRef = db.collection("admins").document(uid)

        return try {
            val snap = userRef.get().await()
            val displayName = user.displayName?.takeIf { it.isNotBlank() }
                ?: email.substringBefore("@").ifBlank { "Pengguna" }

            if (!snap.exists()) {
                val initialRole = if (isBootstrapAdmin) UserRole.SUPER_ADMIN.value else UserRole.PLAYER.value
                val initialPerms = if (isBootstrapAdmin) AdminPermission.defaultsForRole(UserRole.SUPER_ADMIN) else emptyList()
                userRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to displayName.take(120),
                        "email" to email.ifBlank { "$uid@users.local" }.take(200),
                        "role" to initialRole,
                        "permissions" to initialPerms,
                        "active" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                userRef.update(
                    mapOf(
                        "displayName" to displayName.take(120),
                        "email" to email.ifBlank { "$uid@users.local" }.take(200),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            if (isBootstrapAdmin) {
                val adminSnap = adminRef.get().await()
                val allPerms = AdminPermission.defaultsForRole(UserRole.SUPER_ADMIN)
                if (!adminSnap.exists()) {
                    adminRef.set(
                        mapOf(
                            "userId" to uid,
                            "displayName" to displayName.take(120),
                            "email" to email.take(200),
                            "role" to UserRole.SUPER_ADMIN.value,
                            "permissions" to allPerms,
                            "active" to true,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
                seedInitialCurriculumIfEmpty(uid, displayName)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun seedInitialCurriculumIfEmpty(actorUid: String, actorName: String): Result<Boolean> {
        return try {
            val existingCats = db.collection("categories").limit(1).get().await()
            if (!existingCats.isEmpty) {
                return Result.success(false)
            }

            val batch = db.batch()
            val catCounters = mutableMapOf<String, Long>()
            val seedQuestions = CurriculumSeedData.buildInitialQuestions()

            seedQuestions.forEach { q ->
                catCounters[q.categoryId] = (catCounters[q.categoryId] ?: 0L) + 1L
            }

            CurriculumSeedData.initialCategories.forEach { cat ->
                val ref = db.collection("categories").document(cat.id)
                batch.set(
                    ref,
                    mapOf(
                        "code" to cat.code,
                        "name" to cat.name,
                        "description" to cat.description,
                        "sortOrder" to cat.sortOrder,
                        "active" to true,
                        "questionCounter" to (catCounters[cat.id] ?: 0L),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            val diffRef = db.collection("app_config").document("difficulty")
            batch.set(
                diffRef,
                mapOf(
                    "beginnerMaxSeconds" to 120L,
                    "intermediateMaxSeconds" to 300L,
                    "updatedBy" to actorUid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            seedQuestions.forEach { spec ->
                val qRef = db.collection("questions").document(spec.id)
                val versionMap = spec.versionContent.toMap()
                val isPublished = spec.status == QuestionStatus.PUBLISHED
                val qData = mutableMapOf<String, Any>(
                    "code" to spec.code,
                    "categoryId" to spec.categoryId,
                    "categoryCode" to spec.categoryCode,
                    "categoryName" to spec.categoryName,
                    "title" to spec.title,
                    "instruction" to spec.instruction,
                    "difficulty" to spec.difficulty.value,
                    "source" to QuestionSource.MANUAL.value,
                    "status" to spec.status.value,
                    "createdBy" to actorUid,
                    "createdByName" to actorName.take(120),
                    "currentVersionNumber" to 1L,
                    "publishedVersionNumber" to (if (isPublished) 1L else 0L),
                    "latestVersionData" to versionMap,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (isPublished) {
                    qData["publishedVersionData"] = versionMap
                }
                batch.set(qRef, qData)

                val vRef = qRef.collection("versions").document("v1")
                batch.set(
                    vRef,
                    mapOf(
                        "versionNumber" to 1L,
                        "createdBy" to actorUid,
                        "createdByName" to actorName.take(120),
                        "changeSummary" to "Inisialisasi kurikulum awal Nahwu",
                        "versionData" to versionMap,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            val logRef = db.collection("admin_logs").document()
            batch.set(
                logRef,
                mapOf(
                    "actorUid" to actorUid,
                    "actorName" to actorName.take(120),
                    "action" to "SEED_CURRICULUM",
                    "targetType" to "system",
                    "targetId" to "initial_curriculum",
                    "summary" to "Menginisialisasi ${CurriculumSeedData.initialCategories.size} kategori dan ${seedQuestions.size} soal Nahwu.",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )

            batch.commit().await()
            Result.success(true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "seed_curriculum")
            Result.failure(e)
        }
    }

    fun observeMyProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        if (uid != userId) throw IllegalStateException("UID tidak cocok.")
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { it.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) handleFirestoreError(fsErr ?: error, OperationType.GET, path)
                    throw error
                }
        )
    }

    fun observeMyAdminRecord(userId: String): Flow<AdminRecord?> = flow {
        val uid = requireUserId()
        if (uid != userId) throw IllegalStateException("UID tidak cocok.")
        val path = "admins/$uid"
        emitAll(
            db.collection("admins").document(uid)
                .snapshots()
                .map { it.toObject(AdminRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) handleFirestoreError(fsErr ?: error, OperationType.GET, path)
                    throw error
                }
        )
    }

    // --- PLAYER OBSERVATIONS ---

    fun observeActiveCategories(): Flow<List<Category>> = flow {
        val uid = requireUserId()
        val path = "categories"
        emitAll(
            db.collection(path)
                .whereEqualTo("active", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Category::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.sortOrder }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeDifficultyConfig(): Flow<DifficultyConfig> = flow {
        val uid = requireUserId()
        val path = "app_config/difficulty"
        emitAll(
            db.collection("app_config").document("difficulty")
                .snapshots()
                .map { snap ->
                    snap.toObject(DifficultyConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?: DifficultyConfig()
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) handleFirestoreError(fsErr ?: error, OperationType.GET, path)
                    throw error
                }
        )
    }

    suspend fun getPublishedQuestionsForCategory(categoryId: String): Result<List<Question>> {
        val path = "questions"
        return try {
            requireUserId()
            require(categoryId.isNotBlank()) { "Kategori wajib dipilih." }
            val snap = db.collection(path)
                .whereEqualTo("categoryId", categoryId)
                .whereEqualTo("status", QuestionStatus.PUBLISHED.value)
                .get()
                .await()
            val list = snap.toObjects(Question::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedBy { it.code }
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun startPlayerSession(category: Category): Result<PlayerSession> {
        var path = "users/sessions"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("sessions").document()
            path = docRef.path
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "categoryId" to category.id,
                    "categoryName" to category.name.take(100),
                    "status" to "active",
                    "elapsedSeconds" to 0L,
                    "currentDifficulty" to QuestionDifficulty.BEGINNER.value,
                    "questionsAnswered" to 0L,
                    "correctCount" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(
                PlayerSession(
                    id = docRef.id,
                    userId = uid,
                    categoryId = category.id,
                    categoryName = category.name,
                    status = "active"
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun recordPlayerAnswerAndProgress(
        sessionId: String,
        question: Question,
        evaluation: EvaluationResult,
        playerInputs: List<PlayerTokenInput>,
        elapsedSeconds: Long,
        currentDifficulty: QuestionDifficulty,
        questionsAnswered: Long,
        correctCount: Long
    ): Result<Unit> {
        var path = "users/sessions/$sessionId"
        return try {
            val uid = requireUserId()
            val sessRef = db.collection("users").document(uid).collection("sessions").document(sessionId)
            val ansRef = sessRef.collection("answers").document()
            path = ansRef.path
            val summaryTokens = playerInputs.sortedBy { it.position }.map { it.renderArabic().ifBlank { it.baseText } }

            val batch = db.batch()
            batch.set(
                ansRef,
                mapOf(
                    "userId" to uid,
                    "sessionId" to sessionId,
                    "questionId" to question.id,
                    "questionCode" to question.code,
                    "versionNumber" to question.publishedVersionNumber.coerceAtLeast(1L),
                    "matchedPatternId" to evaluation.matchedPattern.patternId,
                    "evaluationStatus" to evaluation.overallStatus.name,
                    "score" to evaluation.score.toLong(),
                    "correctWordCount" to evaluation.correctWordCount.toLong(),
                    "wrongWordCount" to evaluation.wrongWordCount.toLong(),
                    "incompleteWordCount" to evaluation.incompleteWordCount.toLong(),
                    "playerTokensSummary" to summaryTokens,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                sessRef,
                mapOf(
                    "elapsedSeconds" to elapsedSeconds.coerceAtLeast(0L),
                    "currentDifficulty" to currentDifficulty.value,
                    "questionsAnswered" to questionsAnswered.coerceAtLeast(0L),
                    "correctCount" to correctCount.coerceAtLeast(0L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun endPlayerSession(
        sessionId: String,
        elapsedSeconds: Long,
        currentDifficulty: QuestionDifficulty,
        questionsAnswered: Long,
        correctCount: Long
    ): Result<Unit> {
        var path = "users/sessions/$sessionId"
        return try {
            val uid = requireUserId()
            val sessRef = db.collection("users").document(uid).collection("sessions").document(sessionId)
            path = sessRef.path
            sessRef.update(
                mapOf(
                    "status" to "ended",
                    "elapsedSeconds" to elapsedSeconds.coerceAtLeast(0L),
                    "currentDifficulty" to currentDifficulty.value,
                    "questionsAnswered" to questionsAnswered,
                    "correctCount" to correctCount,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- ADMIN OBSERVATIONS ---

    fun observeAllCategoriesAdmin(): Flow<List<Category>> = flow {
        val uid = requireUserId()
        val path = "categories"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Category::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.sortOrder }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeAllQuestionsAdmin(): Flow<List<Question>> = flow {
        val uid = requireUserId()
        val path = "questions"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Question::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeAdminLogs(): Flow<List<AdminLogEntry>> = flow {
        val uid = requireUserId()
        val path = "admin_logs"
        emitAll(
            db.collection(path)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(25)
                .snapshots()
                .map { snap -> snap.toObjects(AdminLogEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeAllAdmins(): Flow<List<AdminRecord>> = flow {
        val uid = requireUserId()
        val path = "admins"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snap -> snap.toObjects(AdminRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeAllUsersForAdmin(): Flow<List<UserProfile>> = flow {
        val uid = requireUserId()
        val path = "users"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snap -> snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    // --- ADMIN MUTATIONS ---

    suspend fun saveCategory(
        categoryId: String?,
        code: String,
        name: String,
        description: String,
        sortOrder: Long,
        active: Boolean
    ): Result<Unit> {
        val cleanCode = code.trim().uppercase(Locale.ROOT).replace(Regex("[^A-Z0-9_\\-]"), "")
        require(cleanCode.length in 2..16) { "Kode kategori harus 2-16 huruf kapital/angka." }
        require(name.trim().length in 2..100) { "Nama kategori minimal 2 karakter." }
        val docRef = if (categoryId.isNullOrBlank()) {
            db.collection("categories").document()
        } else {
            db.collection("categories").document(categoryId)
        }
        return try {
            val uid = requireUserId()
            if (categoryId.isNullOrBlank()) {
                docRef.set(
                    mapOf(
                        "code" to cleanCode,
                        "name" to name.trim().take(100),
                        "description" to description.trim().take(500),
                        "sortOrder" to sortOrder.coerceIn(0L, 10000L),
                        "active" to active,
                        "questionCounter" to 0L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                appendAdminLog(uid, "CREATE_CATEGORY", "category", docRef.id, "Membuat kategori $cleanCode - ${name.trim()}")
            } else {
                docRef.update(
                    mapOf(
                        "code" to cleanCode,
                        "name" to name.trim().take(100),
                        "description" to description.trim().take(500),
                        "sortOrder" to sortOrder.coerceIn(0L, 10000L),
                        "active" to active,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                appendAdminLog(uid, "UPDATE_CATEGORY", "category", docRef.id, "Memperbarui kategori $cleanCode - ${name.trim()}")
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createDraftQuestionTransactional(
        category: Category,
        title: String,
        instruction: String,
        difficulty: QuestionDifficulty,
        source: QuestionSource,
        versionContent: QuestionVersionContent
    ): Result<String> {
        require(versionContent.baseTokens.size == 10) { "Soal wajib memiliki tepat 10 token." }
        require(versionContent.answerPatterns.isNotEmpty()) { "Soal wajib memiliki minimal 1 pola jawaban." }
        var path = "questions"
        return try {
            val uid = requireUserId()
            val actorName = currentUserName()
            val catRef = db.collection("categories").document(category.id)
            val qRef = db.collection("questions").document()
            path = qRef.path

            val generatedCode = db.runTransaction { tx ->
                val catSnap = tx.get(catRef)
                val prevCounter = catSnap.getLong("questionCounter") ?: 0L
                val nextCounter = prevCounter + 1L
                val catCode = catSnap.getString("code") ?: category.code
                val code = "LAT-$catCode-${nextCounter.toString().padStart(3, '0')}"

                val versionMap = versionContent.copy(versionNumber = 1).toMap()

                tx.update(
                    catRef,
                    mapOf(
                        "questionCounter" to nextCounter,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    qRef,
                    mapOf(
                        "code" to code,
                        "categoryId" to category.id,
                        "categoryCode" to catCode,
                        "categoryName" to category.name.take(100),
                        "title" to title.trim().take(200),
                        "instruction" to instruction.trim().take(500),
                        "difficulty" to difficulty.value,
                        "source" to source.value,
                        "status" to QuestionStatus.DRAFT.value,
                        "createdBy" to uid,
                        "createdByName" to actorName.take(120),
                        "currentVersionNumber" to 1L,
                        "publishedVersionNumber" to 0L,
                        "latestVersionData" to versionMap,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                val vRef = qRef.collection("versions").document("v1")
                tx.set(
                    vRef,
                    mapOf(
                        "versionNumber" to 1L,
                        "createdBy" to uid,
                        "createdByName" to actorName.take(120),
                        "changeSummary" to "Pembuatan soal (${source.labelId})",
                        "versionData" to versionMap,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                code
            }.await()

            appendAdminLog(uid, "CREATE_QUESTION", "question", qRef.id, "Membuat draft soal $generatedCode (${source.labelId})")
            Result.success(generatedCode)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateQuestionNewVersion(
        existing: Question,
        category: Category,
        title: String,
        instruction: String,
        difficulty: QuestionDifficulty,
        versionContent: QuestionVersionContent,
        changeSummary: String
    ): Result<Unit> {
        require(versionContent.baseTokens.size == 10) { "Soal wajib memiliki tepat 10 token." }
        val qRef = db.collection("questions").document(existing.id)
        return try {
            val uid = requireUserId()
            val actorName = currentUserName()
            val nextVersion = existing.currentVersionNumber + 1L
            val updatedVersionContent = versionContent.copy(versionNumber = nextVersion.toInt())
            val versionMap = updatedVersionContent.toMap()

            // Notice: If question was published, players continue seeing publishedVersionData
            // while latestVersionData moves to draft until reviewed & republished!
            val batch = db.batch()
            batch.update(
                qRef,
                mapOf(
                    "categoryId" to category.id,
                    "categoryCode" to category.code,
                    "categoryName" to category.name.take(100),
                    "title" to title.trim().take(200),
                    "instruction" to instruction.trim().take(500),
                    "difficulty" to difficulty.value,
                    "status" to QuestionStatus.DRAFT.value,
                    "currentVersionNumber" to nextVersion,
                    "latestVersionData" to versionMap,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            val vRef = qRef.collection("versions").document("v$nextVersion")
            batch.set(
                vRef,
                mapOf(
                    "versionNumber" to nextVersion,
                    "createdBy" to uid,
                    "createdByName" to actorName.take(120),
                    "changeSummary" to changeSummary.ifBlank { "Pembaruan versi $nextVersion" }.take(500),
                    "versionData" to versionMap,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            appendAdminLog(uid, "EDIT_QUESTION", "question", existing.id, "Memperbarui ${existing.code} ke versi v$nextVersion")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, qRef.path)
            Result.failure(e)
        }
    }

    suspend fun submitQuestionForReview(question: Question): Result<Unit> {
        val qRef = db.collection("questions").document(question.id)
        return try {
            val uid = requireUserId()
            val actorName = currentUserName()
            val batch = db.batch()
            batch.update(
                qRef,
                mapOf(
                    "status" to QuestionStatus.REVIEW.value,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                qRef.collection("reviews").document(),
                mapOf(
                    "reviewerUid" to uid,
                    "reviewerName" to actorName.take(120),
                    "decision" to "submitted",
                    "notes" to "Dikirim untuk pemeriksaan Reviewer.",
                    "versionNumber" to question.currentVersionNumber,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            appendAdminLog(uid, "SUBMIT_REVIEW", "question", question.id, "Mengirim ${question.code} untuk pemeriksaan")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, qRef.path)
            Result.failure(e)
        }
    }

    suspend fun reviewQuestionDecision(
        question: Question,
        newStatus: QuestionStatus,
        notes: String
    ): Result<Unit> {
        val qRef = db.collection("questions").document(question.id)
        return try {
            val uid = requireUserId()
            val actorName = currentUserName()
            val decision = when (newStatus) {
                QuestionStatus.APPROVED -> "approved"
                QuestionStatus.NEEDS_REPAIR -> "needs_repair"
                QuestionStatus.REJECTED -> "rejected"
                else -> throw IllegalArgumentException("Status review tidak valid.")
            }
            val batch = db.batch()
            batch.update(
                qRef,
                mapOf(
                    "status" to newStatus.value,
                    "lastReviewNote" to notes.trim().take(1000),
                    "reviewedBy" to actorName.take(120),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                qRef.collection("reviews").document(),
                mapOf(
                    "reviewerUid" to uid,
                    "reviewerName" to actorName.take(120),
                    "decision" to decision,
                    "notes" to notes.trim().take(1000),
                    "versionNumber" to question.currentVersionNumber,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            appendAdminLog(uid, "REVIEW_${decision.uppercase()}", "question", question.id, "Review ${question.code}: ${newStatus.labelId}")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, qRef.path)
            Result.failure(e)
        }
    }

    suspend fun publishQuestion(question: Question): Result<Unit> {
        val qRef = db.collection("questions").document(question.id)
        return try {
            val uid = requireUserId()
            val actorName = currentUserName()
            val batch = db.batch()
            batch.update(
                qRef,
                mapOf(
                    "status" to QuestionStatus.PUBLISHED.value,
                    "publishedVersionNumber" to question.currentVersionNumber,
                    "publishedVersionData" to question.latestVersionData.toMap(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                qRef.collection("reviews").document(),
                mapOf(
                    "reviewerUid" to uid,
                    "reviewerName" to actorName.take(120),
                    "decision" to "published",
                    "notes" to "Soal dipublikasikan ke pemain (versi ${question.currentVersionNumber}).",
                    "versionNumber" to question.currentVersionNumber,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            appendAdminLog(uid, "PUBLISH_QUESTION", "question", question.id, "Mempublikasikan ${question.code} v${question.currentVersionNumber}")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, qRef.path)
            Result.failure(e)
        }
    }

    suspend fun disableQuestion(question: Question): Result<Unit> {
        val qRef = db.collection("questions").document(question.id)
        return try {
            val uid = requireUserId()
            val actorName = currentUserName()
            val batch = db.batch()
            batch.update(
                qRef,
                mapOf(
                    "status" to QuestionStatus.DISABLED.value,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                qRef.collection("reviews").document(),
                mapOf(
                    "reviewerUid" to uid,
                    "reviewerName" to actorName.take(120),
                    "decision" to "disabled",
                    "notes" to "Soal dinonaktifkan dari pemain.",
                    "versionNumber" to question.currentVersionNumber,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            appendAdminLog(uid, "DISABLE_QUESTION", "question", question.id, "Menonaktifkan ${question.code}")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, qRef.path)
            Result.failure(e)
        }
    }

    suspend fun fetchQuestionVersions(questionId: String): Result<List<QuestionVersionRecord>> {
        val path = "questions/$questionId/versions"
        return try {
            requireUserId()
            val snap = db.collection("questions").document(questionId).collection("versions")
                .orderBy("versionNumber", Query.Direction.DESCENDING)
                .get()
                .await()
            Result.success(snap.toObjects(QuestionVersionRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun fetchQuestionReviews(questionId: String): Result<List<QuestionReviewRecord>> {
        val path = "questions/$questionId/reviews"
        return try {
            requireUserId()
            val snap = db.collection("questions").document(questionId).collection("reviews")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            Result.success(snap.toObjects(QuestionReviewRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun saveDifficultyConfig(beginnerMaxSeconds: Long, intermediateMaxSeconds: Long): Result<Unit> {
        require(beginnerMaxSeconds in 30L..3600L) { "Ambang level awal harus 30-3600 detik." }
        require(intermediateMaxSeconds in (beginnerMaxSeconds + 1L)..7200L) { "Ambang level menengah harus lebih besar dari level awal (maks 7200 detik)." }
        val ref = db.collection("app_config").document("difficulty")
        return try {
            val uid = requireUserId()
            ref.set(
                mapOf(
                    "beginnerMaxSeconds" to beginnerMaxSeconds,
                    "intermediateMaxSeconds" to intermediateMaxSeconds,
                    "updatedBy" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            appendAdminLog(uid, "UPDATE_CONFIG", "app_config", "difficulty", "Mengubah ambang kesulitan: Awal <${beginnerMaxSeconds}s, Menengah <${intermediateMaxSeconds}s")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun saveAdminRoleAndPermissions(
        targetUser: UserProfile,
        role: UserRole,
        permissions: List<String>,
        active: Boolean
    ): Result<Unit> {
        val adminRef = db.collection("admins").document(targetUser.userId)
        val userRef = db.collection("users").document(targetUser.userId)
        return try {
            val uid = requireUserId()
            val batch = db.batch()
            if (role == UserRole.PLAYER) {
                batch.delete(adminRef)
                batch.update(
                    userRef,
                    mapOf(
                        "role" to UserRole.PLAYER.value,
                        "permissions" to emptyList<String>(),
                        "active" to active,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                val safePerms = if (role == UserRole.SUPER_ADMIN) {
                    AdminPermission.defaultsForRole(UserRole.SUPER_ADMIN)
                } else {
                    permissions.distinct().take(20)
                }
                val adminPayload = mapOf(
                    "userId" to targetUser.userId,
                    "displayName" to targetUser.displayName.take(120),
                    "email" to targetUser.email.take(200),
                    "role" to role.value,
                    "permissions" to safePerms,
                    "active" to active,
                    "createdAt" to (targetUser.createdAt ?: FieldValue.serverTimestamp()),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(adminRef, adminPayload)
                batch.update(
                    userRef,
                    mapOf(
                        "role" to role.value,
                        "permissions" to safePerms,
                        "active" to active,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            appendAdminLog(uid, "UPDATE_ADMIN_ACCESS", "user", targetUser.userId, "Mengatur akses ${targetUser.email} menjadi ${role.labelId} (aktif=$active)")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, adminRef.path)
            Result.failure(e)
        }
    }

    private suspend fun appendAdminLog(
        actorUid: String,
        action: String,
        targetType: String,
        targetId: String,
        summary: String
    ) {
        try {
            db.collection("admin_logs").document().set(
                mapOf(
                    "actorUid" to actorUid,
                    "actorName" to currentUserName().take(120),
                    "action" to action.take(80),
                    "targetType" to targetType.take(60),
                    "targetId" to targetId.take(128),
                    "summary" to summary.take(500),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (_: Exception) {
            // Non-blocking audit log
        }
    }
}
