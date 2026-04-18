package com.example.workoutapp.data.remote

import com.example.workoutapp.data.local.entity.Exercise
import com.example.workoutapp.data.local.entity.RestDay
import com.example.workoutapp.data.local.entity.SessionExercise
import com.example.workoutapp.data.local.entity.Settings
import com.example.workoutapp.data.local.entity.UserMetrics
import com.example.workoutapp.data.local.entity.WorkoutSession
import com.example.workoutapp.data.local.entity.WorkoutStats
import com.example.workoutapp.data.remote.model.CloudMigrationMeta
import com.example.workoutapp.data.remote.model.CloudSettings
import com.example.workoutapp.data.remote.model.toCloud
import com.example.workoutapp.data.remote.model.toLocal
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    private fun userRoot(uid: String) = firestore.collection("users").document(uid)

    fun observeUserMetrics(uid: String): Flow<UserMetrics?> = callbackFlow {
        var listener: com.google.firebase.firestore.ListenerRegistration? = null
        try {
            listener = userRoot(uid)
                .collection("profiles")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(null)
                        return@addSnapshotListener
                    }

                    val profiles = snapshot?.documents
                        ?.mapNotNull { it.toObject<com.example.workoutapp.data.remote.model.CloudUserMetrics>() }
                        ?: emptyList()

                    val model = profiles.firstOrNull { it.isActive }
                        ?: profiles.minByOrNull { it.id }

                    trySend(model?.toLocal())
                }
        } catch (e: Exception) {
            listener?.remove()
            trySend(null)
        }

        awaitClose { listener?.remove() }
    }.conflate()

    fun observeAllUserMetrics(uid: String): Flow<List<UserMetrics>> = callbackFlow {
        var listener: com.google.firebase.firestore.ListenerRegistration? = null
        try {
            listener = userRoot(uid)
                .collection("profiles")
                .orderBy("id")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }

                    val profiles = snapshot?.documents
                        ?.mapNotNull { it.toObject<com.example.workoutapp.data.remote.model.CloudUserMetrics>()?.toLocal() }
                        ?: emptyList()

                    trySend(profiles)
                }
        } catch (e: Exception) {
            listener?.remove()
            trySend(emptyList())
        }

        awaitClose { listener?.remove() }
    }.conflate()

    suspend fun saveUserMetrics(uid: String, metrics: UserMetrics) {
        val root = userRoot(uid)
        val allProfiles = root.collection("profiles").get().await()
        val batch = firestore.batch()
        val resolvedId = if (metrics.id > 0) metrics.id else nextNumericId(uid, "profiles")

        allProfiles.documents.forEach { doc ->
            batch.set(doc.reference, mapOf("isActive" to false), SetOptions.merge())
        }

        val targetDoc = root.collection("profiles").document(resolvedId.toString())
        batch.set(targetDoc, metrics.copy(id = resolvedId, isActive = true).toCloud())
        batch.commit().await()
    }

    suspend fun addUserMetrics(uid: String, metrics: UserMetrics) {
        val resolvedId = if (metrics.id > 0) metrics.id else nextNumericId(uid, "profiles")
        userRoot(uid)
            .collection("profiles")
            .document(resolvedId.toString())
            .set(metrics.copy(id = resolvedId).toCloud())
            .await()
    }

    suspend fun updateUserMetrics(uid: String, metrics: UserMetrics) {
        val resolvedId = if (metrics.id > 0) metrics.id else nextNumericId(uid, "profiles")
        userRoot(uid)
            .collection("profiles")
            .document(resolvedId.toString())
            .set(metrics.copy(id = resolvedId).toCloud())
            .await()
    }

    suspend fun setActiveProfile(uid: String, profileId: Int) {
        val root = userRoot(uid)
        val allProfiles = root.collection("profiles").get().await()
        if (allProfiles.documents.isEmpty()) return

        val batch = firestore.batch()

        allProfiles.documents.forEach { doc ->
            batch.set(doc.reference, mapOf("isActive" to (doc.id == profileId.toString())), SetOptions.merge())
        }

        batch.commit().await()
    }

    suspend fun deleteUserMetrics(uid: String, profileId: Int) {
        userRoot(uid).collection("profiles").document(profileId.toString()).delete().await()
    }

    fun observeExercises(uid: String): Flow<List<Exercise>> = callbackFlow {
        var listener: com.google.firebase.firestore.ListenerRegistration? = null
        try {
            listener = userRoot(uid)
                .collection("exercises")
                .orderBy("id")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }

                    val exercises = snapshot?.documents
                        ?.mapNotNull { it.toObject<com.example.workoutapp.data.remote.model.CloudExercise>()?.toLocal() }
                        ?: emptyList()

                    trySend(exercises.filterNot { it.isDeleted })
                }
        } catch (e: Exception) {
            listener?.remove()
            trySend(emptyList())
        }

        awaitClose { listener?.remove() }
    }.conflate()

    suspend fun upsertExercise(uid: String, exercise: Exercise) {
        val resolvedId = if (exercise.id > 0) exercise.id else nextNumericId(uid, "exercises")
        userRoot(uid)
            .collection("exercises")
            .document(resolvedId.toString())
            .set(exercise.copy(id = resolvedId).toCloud())
            .await()
    }

    suspend fun markExerciseDeleted(uid: String, exerciseId: Int) {
        userRoot(uid)
            .collection("exercises")
            .document(exerciseId.toString())
            .set(
                mapOf(
                    "deleted" to true,
                    "isDeleted" to true
                ),
                SetOptions.merge()
            )
            .await()
    }

    fun observeSessions(uid: String): Flow<List<WorkoutSession>> = callbackFlow {
        var listener: com.google.firebase.firestore.ListenerRegistration? = null
        try {
            listener = userRoot(uid)
                .collection("sessions")
                .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }

                    val sessions = snapshot?.documents
                        ?.mapNotNull { it.toObject<com.example.workoutapp.data.remote.model.CloudWorkoutSession>()?.toLocal() }
                        ?: emptyList()

                    trySend(sessions)
                }
        } catch (e: Exception) {
            listener?.remove()
            trySend(emptyList())
        }

        awaitClose { listener?.remove() }
    }.conflate()

    suspend fun saveSession(uid: String, session: WorkoutSession): Long {
        val id = if (session.id > 0) session.id else nextSessionId(uid)
        userRoot(uid).collection("sessions").document(id.toString()).set(session.copy(id = id).toCloud()).await()
        return id.toLong()
    }

    suspend fun getSession(uid: String, sessionId: Int): WorkoutSession? {
        val snapshot = userRoot(uid)
            .collection("sessions")
            .document(sessionId.toString())
            .get()
            .await()

        return snapshot.toObject<com.example.workoutapp.data.remote.model.CloudWorkoutSession>()?.toLocal()
    }

    suspend fun deleteSession(uid: String, sessionId: Int) {
        val root = userRoot(uid)
        root.collection("sessions").document(sessionId.toString()).delete().await()

        val sessionExercises = root.collection("sessionExercises")
            .whereEqualTo("sessionId", sessionId)
            .get()
            .await()

        if (sessionExercises.documents.isNotEmpty()) {
            val batch = firestore.batch()
            sessionExercises.documents.forEach { batch.delete(it.reference) }
            batch.commit().await()
        }
    }

    fun observeSettings(uid: String): Flow<Settings?> = callbackFlow {
        var listener: com.google.firebase.firestore.ListenerRegistration? = null
        try {
            listener = userRoot(uid)
                .collection("settings")
                .document("default")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(null)
                        return@addSnapshotListener
                    }

                    val settings = snapshot?.toObject(CloudSettings::class.java)?.toLocal()
                    trySend(settings)
                }
        } catch (e: Exception) {
            listener?.remove()
            trySend(null)
        }

        awaitClose { listener?.remove() }
    }.conflate()

    suspend fun saveSettings(uid: String, settings: Settings) {
        userRoot(uid).collection("settings").document("default").set(settings.toCloud()).await()
    }

    fun observeRestDays(uid: String): Flow<List<RestDay>> = callbackFlow {
        var listener: com.google.firebase.firestore.ListenerRegistration? = null
        try {
            listener = userRoot(uid)
                .collection("restDays")
                .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }

                    val restDays = snapshot?.documents
                        ?.mapNotNull { it.toObject<com.example.workoutapp.data.remote.model.CloudRestDay>()?.toLocal() }
                        ?: emptyList()

                    trySend(restDays)
                }
        } catch (e: Exception) {
            listener?.remove()
            trySend(emptyList())
        }

        awaitClose { listener?.remove() }
    }.conflate()

    suspend fun addRestDay(uid: String, restDay: RestDay) {
        val resolvedId = if (restDay.id > 0) restDay.id else nextNumericId(uid, "restDays")
        userRoot(uid)
            .collection("restDays")
            .document(resolvedId.toString())
            .set(restDay.copy(id = resolvedId).toCloud())
            .await()
    }

    suspend fun deleteRestDay(uid: String, restDayId: Int) {
        userRoot(uid).collection("restDays").document(restDayId.toString()).delete().await()
    }

    suspend fun getRestDayByDate(uid: String, date: Long): RestDay? {
        val snapshot = userRoot(uid)
            .collection("restDays")
            .whereEqualTo("date", date)
            .limit(1)
            .get()
            .await()

        return snapshot.documents.firstOrNull()
            ?.toObject<com.example.workoutapp.data.remote.model.CloudRestDay>()
            ?.toLocal()
    }

    suspend fun saveSessionExercises(uid: String, exercises: List<SessionExercise>) {
        if (exercises.isEmpty()) return
        val root = userRoot(uid)
        var nextId = nextNumericId(uid, "sessionExercises")
        val writes = mutableListOf<Pair<com.google.firebase.firestore.DocumentReference, SessionExercise>>()

        exercises.forEach { exercise ->
            val resolvedId = if (exercise.id > 0) exercise.id else nextId++
            val doc = root.collection("sessionExercises").document(resolvedId.toString())
            writes.add(doc to exercise.copy(id = resolvedId))
        }

        writes.chunked(400).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { (docRef, exercise) ->
                batch.set(docRef, exercise.toCloud())
            }
            batch.commit().await()
        }
    }

    fun observeSessionExercises(uid: String, sessionId: Int): Flow<List<SessionExercise>> = callbackFlow {
        var listener: com.google.firebase.firestore.ListenerRegistration? = null
        try {
            listener = userRoot(uid)
                .collection("sessionExercises")
                .whereEqualTo("sessionId", sessionId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }

                    val items = snapshot?.documents
                        ?.mapNotNull { it.toObject<com.example.workoutapp.data.remote.model.CloudSessionExercise>()?.toLocal() }
                        ?.sortedBy { it.sortOrder }
                        ?: emptyList()

                    trySend(items)
                }
        } catch (e: Exception) {
            listener?.remove()
            trySend(emptyList())
        }

        awaitClose { listener?.remove() }
    }.conflate()

    fun observeExerciseHistory(uid: String, exerciseName: String): Flow<List<SessionExercise>> = callbackFlow {
        var listener: com.google.firebase.firestore.ListenerRegistration? = null
        try {
            listener = userRoot(uid)
                .collection("sessionExercises")
                .whereEqualTo("exerciseName", exerciseName)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }

                    val items = snapshot?.documents
                        ?.mapNotNull { it.toObject<com.example.workoutapp.data.remote.model.CloudSessionExercise>()?.toLocal() }
                        ?.sortedByDescending { it.sessionId }
                        ?: emptyList()

                    trySend(items)
                }
        } catch (e: Exception) {
            listener?.remove()
            trySend(emptyList())
        }

        awaitClose { listener?.remove() }
    }.conflate()

    fun observeAllExerciseNames(uid: String): Flow<List<String>> = callbackFlow {
        var listener: com.google.firebase.firestore.ListenerRegistration? = null
        try {
            listener = userRoot(uid)
                .collection("sessionExercises")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }

                    val names = snapshot?.documents
                        ?.mapNotNull { it.getString("exerciseName") }
                        ?.distinct()
                        ?.sorted()
                        ?: emptyList()

                    trySend(names)
                }
        } catch (e: Exception) {
            listener?.remove()
            trySend(emptyList())
        }

        awaitClose { listener?.remove() }
    }.conflate()

    fun observeAllSessionExercises(uid: String): Flow<List<SessionExercise>> = callbackFlow {
        var listener: com.google.firebase.firestore.ListenerRegistration? = null
        try {
            listener = userRoot(uid)
                .collection("sessionExercises")
                .orderBy("sessionId", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }

                    val items = snapshot?.documents
                        ?.mapNotNull { it.toObject<com.example.workoutapp.data.remote.model.CloudSessionExercise>()?.toLocal() }
                        ?: emptyList()

                    trySend(items)
                }
        } catch (e: Exception) {
            listener?.remove()
            trySend(emptyList())
        }

        awaitClose { listener?.remove() }
    }.conflate()

    fun observeWorkoutStats(uid: String): Flow<WorkoutStats?> = callbackFlow {
        var listener: com.google.firebase.firestore.ListenerRegistration? = null
        try {
            listener = userRoot(uid)
                .collection("sessions")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(null)
                        return@addSnapshotListener
                    }

                    val sessions = snapshot?.documents
                        ?.mapNotNull { it.toObject<com.example.workoutapp.data.remote.model.CloudWorkoutSession>()?.toLocal() }
                        ?: emptyList()

                    val totalWorkouts = sessions.size
                    val totalWeight = sessions.sumOf { it.totalWeightLifted.toDouble() }.toFloat()
                    val totalDuration = sessions.sumOf { it.durationSeconds }

                    trySend(
                        WorkoutStats(
                            totalWorkouts = totalWorkouts,
                            totalWeightLifted = totalWeight,
                            totalDurationSeconds = totalDuration
                        )
                    )
                }
        } catch (e: Exception) {
            listener?.remove()
            trySend(null)
        }

        awaitClose { listener?.remove() }
    }.conflate()

    suspend fun getMigrationMeta(uid: String): CloudMigrationMeta? {
        val snapshot = userRoot(uid).collection("meta").document("migration").get().await()
        return snapshot.toObject()
    }

    suspend fun setMigrationMeta(uid: String, meta: CloudMigrationMeta) {
        userRoot(uid).collection("meta").document("migration").set(meta).await()
    }

    suspend fun performInitialMigration(
        uid: String,
        userMetrics: List<UserMetrics>,
        exercises: List<Exercise>,
        sessions: List<WorkoutSession>,
        sessionExercises: List<SessionExercise>,
        restDays: List<RestDay>,
        settings: Settings?
    ) {
        if (getMigrationMeta(uid)?.migrationComplete == true) return

        val root = userRoot(uid)
        val writes = mutableListOf<Pair<com.google.firebase.firestore.DocumentReference, Any>>()

        userMetrics.forEach { metric ->
            writes.add(root.collection("profiles").document(metric.id.toString()) to metric.toCloud())
        }

        exercises.forEach { exercise ->
            writes.add(root.collection("exercises").document(exercise.id.toString()) to exercise.toCloud())
        }

        sessions.forEach { session ->
            writes.add(root.collection("sessions").document(session.id.toString()) to session.toCloud())
        }

        sessionExercises.forEach { sessionExercise ->
            writes.add(root.collection("sessionExercises").document(sessionExercise.id.toString()) to sessionExercise.toCloud())
        }

        restDays.forEach { restDay ->
            writes.add(root.collection("restDays").document(restDay.id.toString()) to restDay.toCloud())
        }

        if (settings != null) {
            writes.add(root.collection("settings").document("default") to settings.toCloud())
        }

        writes.chunked(400).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { (docRef, value) ->
                batch.set(docRef, value)
            }
            batch.commit().await()
        }

        val remoteProfiles = root.collection("profiles").get().await().size()
        val remoteExercises = root.collection("exercises").get().await().size()
        val remoteSessions = root.collection("sessions").get().await().size()
        val remoteSessionExercises = root.collection("sessionExercises").get().await().size()
        val remoteRestDays = root.collection("restDays").get().await().size()

        val countsMatch =
            remoteProfiles == userMetrics.size &&
                remoteExercises == exercises.size &&
                remoteSessions == sessions.size &&
                remoteSessionExercises == sessionExercises.size &&
                remoteRestDays == restDays.size

        if (!countsMatch) {
            throw IllegalStateException("Cloud migration verification failed. Local data remains intact.")
        }

        setMigrationMeta(
            uid,
            CloudMigrationMeta(
                migrationComplete = true,
                migratedAt = System.currentTimeMillis(),
                userMetricsCount = userMetrics.size,
                exercisesCount = exercises.size,
                sessionsCount = sessions.size,
                sessionExercisesCount = sessionExercises.size,
                restDaysCount = restDays.size,
                schemaVersion = 1
            )
        )

        if (userMetrics.isNotEmpty() && userMetrics.none { it.isActive }) {
            val fallbackProfileId = userMetrics.minByOrNull { it.id }?.id
            if (fallbackProfileId != null) {
                setActiveProfile(uid, fallbackProfileId)
            }
        }
    }

    private suspend fun nextSessionId(uid: String): Int {
        return nextNumericId(uid, "sessions")
    }

    private suspend fun nextNumericId(uid: String, collection: String): Int {
        val snapshot = userRoot(uid)
            .collection(collection)
            .orderBy("id", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()

        val currentMax = snapshot.documents.firstOrNull()?.getLong("id")?.toInt() ?: 0
        return currentMax + 1
    }
}
