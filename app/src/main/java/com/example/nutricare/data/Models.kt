package com.example.nutricare.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

object Role {
    const val PATIENT = "PATIENT"
    const val DOCTOR = "DOCTOR"
    const val ADMIN = "ADMIN"
}

object Status {
    const val PENDING = "PENDING"
    const val ACCEPTED = "ACCEPTED"
    const val REJECTED = "REJECTED"
    const val COMPLETED = "COMPLETED"
}

// users/{uid}
data class AppUser(
    @DocumentId val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = Role.PATIENT,
    val active: Boolean = true,
)

// doctors/{uid}  (پزشک یا متخصص تغذیه)
data class Doctor(
    @DocumentId val id: String = "",
    val name: String = "",
    val specialty: String = "",
    val bio: String = "",
    val licenseNumber: String = "",
    val approved: Boolean = false,
)

// requests/{id} ← آیتم کارتابل پزشک
data class ConsultRequest(
    @DocumentId val id: String = "",
    val patientId: String = "",
    val patientName: String = "",
    val doctorId: String = "",
    val doctorName: String = "",
    val description: String = "",
    val status: String = Status.PENDING,
    val rejectReason: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
)

// requests/{id}/messages/{mid}
data class Message(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val createdAt: Timestamp? = null,
)

sealed interface Session {
    data object Loading : Session
    data class SignedOut(val message: String? = null) : Session
    data class SignedIn(val user: AppUser) : Session
}
