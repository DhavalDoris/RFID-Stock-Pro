package com.example.rfidstockpro.aws.models



import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import java.util.*

data class UserModel(
    var createdDate: String = "",
    var updatedDate: String = "",
    var id: String = "",
    var userName: String = "",
    var companyName: String = "",
    var email: String = "",
    var mobile: Long = 0,
    var password: String = "",
    var otp: Int? = null,
    var permissions: List<Permission> = emptyList(),
    var role: Int = 1, // 0 = Admin, 1 = Owner, 2 = Manager, 3 = Staff
    var status: String = "pending" // "pending", "active", "inactive"
) {
    companion object {
        fun fromMap(item: Map<String, AttributeValue>): UserModel {
            return UserModel(
                createdDate = item["createdDate"]?.s() ?: "",
                updatedDate = item["updatedDate"]?.s() ?: "",
                id = item["id"]?.s() ?: "",
                userName = item["userName"]?.s() ?: "",
                companyName = item["companyName"]?.s() ?: "",
                email = item["email"]?.s() ?: "",
                mobile = item["mobile"]?.n()?.toLong() ?: 0,
                password = item["password"]?.s() ?: "",
                otp = item["otp"]?.n()?.toInt(),
                permissions = item["permissions"]?.l()?.mapNotNull { attr ->
                    attr.m()?.let { Permission.fromMap(it) }
                } ?: emptyList(),
                role = item["role"]?.n()?.toInt() ?: 1,
                status = item["status"]?.s() ?: "pending"
            )
        }

        fun toMap(user: UserModel): Map<String, AttributeValue?> {
            return mapOf(
                "createdDate" to AttributeValue.builder().s(user.createdDate).build(),
                "updatedDate" to AttributeValue.builder().s(user.updatedDate).build(),
                "id" to AttributeValue.builder().s(user.id).build(),
                "userName" to AttributeValue.builder().s(user.userName).build(),
                "companyName" to AttributeValue.builder().s(user.companyName).build(),
                "email" to AttributeValue.builder().s(user.email).build(),
                "mobile" to AttributeValue.builder().n(user.mobile.toString()).build(),
                "password" to AttributeValue.builder().s(user.password).build(),
                "otp" to user.otp?.let { AttributeValue.builder().n(it.toString()).build() },
                "permissions" to AttributeValue.builder()
                    .l(user.permissions.map { AttributeValue.builder().m(Permission.toMap(it)).build() })
                    .build(),
                "role" to AttributeValue.builder().n(user.role.toString()).build(),
                "status" to AttributeValue.builder().s(user.status).build()
            ).filterValues { it != null } // Remove null values
        }
    }
}

data class Permission(
    var title: String = ""
) {
    companion object {
        fun fromMap(item: Map<String, AttributeValue>): Permission {
            return Permission(title = item["title"]?.s() ?: "")
        }

        fun toMap(permission: Permission): Map<String, AttributeValue> {
            return mapOf("title" to AttributeValue.builder().s(permission.title).build())
        }
    }
}
