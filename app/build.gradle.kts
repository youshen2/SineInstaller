plugins {
    id("com.android.application")
}

android {
    namespace = "moye.installer"
    compileSdk = 33
    
    defaultConfig {
        applicationId = "moye.installer.sine"
        minSdk = 17
        //noinspection ExpiredTargetSdkVersion
        targetSdk = 19
        versionCode = 20240710
        versionName = "V1.5"
        resConfigs("zh")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.material:material:1.5.0")
}