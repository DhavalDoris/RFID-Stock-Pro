package com.example.rfidstockpro.aws.models

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.*
import com.example.rfidstockpro.aws.AwsManager.TABLE_NAME

@DynamoDBTable(tableName = TABLE_NAME) // Replace with your DynamoDB table name
data class UserModel(
  /*  @DynamoDBHashKey(attributeName = "id") // Primary key
    var id: String = "",*/

    @DynamoDBHashKey(attributeName = "email")
    var email: String = "",

    @DynamoDBAttribute(attributeName = "password")
    var password: String = "" // Store only if necessary
)
