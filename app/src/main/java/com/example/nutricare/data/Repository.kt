package com.example.nutricare.data

import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

private fun Timestamp?.millis(): Long = this?.toDate()?.time ?: Long.MAX_VALUE

private inline fun <reified T : Any> Query.listFlow(
    crossinline sort: (List<T>) -> List<T> = { it },
): Flow<List<T>> =
    snapshots()
        .map { sort(it.toObjects(T::class.java)) }
        .catch { emit(emptyList()) }

class Repository {
    private val auth = Firebase.auth
    private val db = Firebase.firestore

    val uid: String get() = auth.currentUser?.uid ?: ""

    // ───────── احراز هویت ─────────
    suspend fun loadSession(): Session {
        val current = auth.currentUser ?: return Session.SignedOut()
        return try {
            val user = db.collection("users").document(current.uid).get().await()
                .toObject(AppUser::class.java)
            when {
                user == null -> { auth.signOut(); Session.SignedOut("حساب کاربری یافت نشد") }
                !user.active -> { auth.signOut(); Session.SignedOut("حساب شما مسدود شده است") }
                else -> Session.SignedIn(user)
            }
        } catch (e: Exception) {
            Session.SignedOut("خطا در ارتباط با سرور")
        }
    }

    suspend fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    suspend fun register(name: String, email: String, password: String) {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val id = result.user!!.uid
        db.collection("users").document(id).set(
            mapOf(
                "name" to name,
                "email" to email,
                "role" to Role.PATIENT,
                "active" to true,
            )
        ).await()
    }

    /** خروج؛ توکن اعلان پاک می‌شود تا کاربر قبلی روی این دستگاه اعلان نگیرد */
    suspend fun signOut() {
        val id = auth.currentUser?.uid
        if (id != null) {
            withTimeoutOrNull(3000) {
                runCatching { db.collection("users").document(id).update("fcmToken", "").await() }
            }
        }
        auth.signOut()
    }

    /** ذخیره‌ی توکن FCM کاربر فعلی (برای دریافت اعلان) */
    suspend fun saveFcmToken(token: String? = null) {
        val id = auth.currentUser?.uid ?: return
        val t = token ?: FirebaseMessaging.getInstance().token.await()
        db.collection("users").document(id).update("fcmToken", t).await()
    }

    // ───────── بیمار ─────────
    fun approvedDoctors(): Flow<List<Doctor>> =
        db.collection("doctors").whereEqualTo("approved", true).listFlow<Doctor>()

    suspend fun sendRequest(me: AppUser, doctor: Doctor, description: String) {
        db.collection("requests").add(
            mapOf(
                "patientId" to uid,
                "patientName" to me.name,
                "doctorId" to doctor.id,
                "doctorName" to doctor.name,
                "description" to description,
                "status" to Status.PENDING,
                "rejectReason" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
        ).await()
    }

    fun myRequests(): Flow<List<ConsultRequest>> =
        db.collection("requests").whereEqualTo("patientId", uid)
            .listFlow<ConsultRequest> { l -> l.sortedByDescending { it.createdAt.millis() } }

    // ───────── پزشک (کارتابل) ─────────
    fun doctorInbox(): Flow<List<ConsultRequest>> =
        db.collection("requests").whereEqualTo("doctorId", uid)
            .listFlow<ConsultRequest> { l -> l.sortedByDescending { it.createdAt.millis() } }

    suspend fun updateStatus(requestId: String, status: String, rejectReason: String = "") {
        val update = mutableMapOf<String, Any>(
            "status" to status,
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        if (status == Status.REJECTED) update["rejectReason"] = rejectReason
        db.collection("requests").document(requestId).update(update).await()
    }

    // ───────── چت ─────────
    fun messages(requestId: String): Flow<List<Message>> =
        db.collection("requests").document(requestId).collection("messages")
            .listFlow<Message> { l -> l.sortedBy { it.createdAt.millis() } }

    suspend fun sendMessage(requestId: String, text: String) {
        db.collection("requests").document(requestId).collection("messages").add(
            mapOf(
                "senderId" to uid,
                "text" to text,
                "createdAt" to FieldValue.serverTimestamp(),
            )
        ).await()
    }

    // ───────── مدیر ─────────
    fun patients(): Flow<List<AppUser>> =
        db.collection("users").whereEqualTo("role", Role.PATIENT).listFlow<AppUser>()

    fun allDoctors(): Flow<List<Doctor>> =
        db.collection("doctors").listFlow<Doctor>()

    fun allRequests(): Flow<List<ConsultRequest>> =
        db.collection("requests")
            .listFlow<ConsultRequest> { l -> l.sortedByDescending { it.createdAt.millis() } }

    suspend fun promoteToDoctor(user: AppUser, specialty: String, license: String) {
        val batch = db.batch()
        batch.update(db.collection("users").document(user.id), "role", Role.DOCTOR)
        batch.set(
            db.collection("doctors").document(user.id),
            mapOf(
                "name" to user.name,
                "specialty" to specialty,
                "licenseNumber" to license,
                "bio" to "",
                "approved" to true,
            )
        )
        batch.commit().await()
    }

    suspend fun setDoctorApproved(doctorId: String, approved: Boolean) {
        db.collection("doctors").document(doctorId).update("approved", approved).await()
    }

    suspend fun setUserActive(userId: String, active: Boolean) {
        db.collection("users").document(userId).update("active", active).await()
    }
}
