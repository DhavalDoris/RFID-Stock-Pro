package com.example.rfidstockpro.data.signup

data class UserModel(
    val id: String,
    val userName: String,
    val companyName: String,
    val email: String,
    val mobile: Long,
    val password: String,
    val otp: Int? = null,
    val permissions: List<Permission>,
    val role: Int, // 0: admin, 1: owner, 2: manager, 3: staff
    val status: String, // "pending", "active", "inactive"
    val createdDate: String,
    val updatedDate: String
)

data class Permission(
    val title: String
)