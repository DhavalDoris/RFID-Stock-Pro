package com.example.rfidstockpro.aws.models

import software.amazon.awssdk.services.dynamodb.model.AttributeValue

data class UserModel(
    var email: String = "",
    var password: String = ""
) {
    companion object {
        fun fromMap(item: Map<String, AttributeValue>): UserModel {
            return UserModel(
                email = item["email"]?.s() ?: "",
                password = item["password"]?.s() ?: ""
            )
        }

        fun toMap(user: UserModel): Map<String, AttributeValue> {
            return mapOf(
                "email" to AttributeValue.builder().s(user.email).build(),
                "password" to AttributeValue.builder().s(user.password).build()
            )
        }
    }
}
