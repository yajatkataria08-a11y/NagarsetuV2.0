package com.nagarsetu.auth.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TrustedContact(
    val name: String = "",
    val phone: String = "",
    val relation: String = ""
)

@Serializable
data class MedicalInfo(
    val bloodGroup: String = "",
    val allergies: String = "",
    val medications: String = "",
    val chronicConditions: String = "",
    val emergencyInstructions: String = ""
)
