plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
}

android {
    namespace = "com.example.rfidstockpro"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.rfidstockpro"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
    packagingOptions {
        excludes.add("META-INF/DEPENDENCIES")
        excludes.add("META-INF/LICENSE")
        excludes.add("META-INF/NOTICE")
        excludes.add("META-INF/INDEX.LIST")
        excludes.add("META-INF/io.netty.versions.properties")
    }

}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.gridlayout)
    implementation(libs.androidx.ui.text.android)
    implementation(libs.androidx.lifecycle.viewmodel.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation ("com.intuit.sdp:sdp-android:1.1.0")
    implementation ("com.intuit.ssp:ssp-android:1.1.0")
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    kapt ("com.github.bumptech.glide:compiler:4.16.0")
    implementation ("androidx.viewpager2:viewpager2:1.0.0")
    implementation ("com.google.android.material:material:1.12.0")

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.fragment:fragment-ktx:1.6.2")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    implementation ("com.github.PhilJay:MPAndroidChart:v3.1.0")

    implementation(files("libs/DeviceAPI_ver20250209_release.aar"))
    implementation("no.nordicsemi.android:dfu:2.4.1")

    // AWS Core SDK (Required for all AWS services)
//    implementation("com.amazonaws:aws-android-sdk-core:2.79.0")

    // AWS DynamoDB (For CRUD Operations)
    implementation("com.amazonaws:aws-android-sdk-ddb-mapper:2.79.0")
    implementation("com.amazonaws:aws-android-sdk-ddb:2.79.0") // Ensure DynamoDB SDK is included
    implementation("com.amazonaws:aws-android-sdk-s3:2.79.0") // Optional for S3 storage

    implementation("software.amazon.awssdk:dynamodb:2.25.26")
    implementation ("software.amazon.awssdk:core:2.20.40")

//    implementation("com.amazonaws:aws-java-sdk-core:1.12.782")

    // AWS Cognito (For Authentication & User Management)
//    implementation("com.amazonaws:aws-android-sdk-cognitoidentityprovider:2.46.0")
//    implementation("com.amazonaws:aws-android-sdk-auth-userpools:2.46.0")

    // AWS DynamoDB (For CRUD Operations)
//    implementation("com.amazonaws:aws-android-sdk-dynamodb:2.46.0")
    // https://mvnrepository.com/artifact/com.amazonaws/aws-android-sdk-ddb
//    implementation("com.amazonaws:aws-android-sdk-ddb:2.79.0")
    // https://mvnrepository.com/artifact/com.amazonaws/aws-java-sdk-dynamodb
//    implementation("com.amazonaws:aws-java-sdk-dynamodb:1.12.782")

//    AWS S3 (For Uploading Images/Videos)
//    implementation("com.amazonaws:aws-android-sdk-s3:2.46.0")

//    implementation ("com.amplifyframework:core:2.0.0")
//    implementation ("com.amplifyframework:aws-api:2.0.0")
//    implementation ("com.amplifyframework:aws-datastore:2.0.0")

//    implementation ("com.amazonaws:aws-android-sdk-core:2.73.0")
//    implementation ("com.amazonaws:aws-android-sdk-ddb:2.73.0")

}