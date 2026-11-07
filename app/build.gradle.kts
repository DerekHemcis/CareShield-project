plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.careshield.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.careshield.app"
        minSdk = 26            // 无障碍服务稳定 & AGSL(RenderEffect 需 31+，shader 在 31 才生效)
        targetSdk = 34
        versionCode = 60
        versionName = "1.6.0"
        vectorDrawables { useSupportLibrary = true }
        resourceConfigurations += listOf("zh-rTW", "en")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.appcompat:appcompat:1.7.0")

    // Compose + Material3
    val composeBom = platform("androidx.compose:compose-bom:2024.09.02")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // DataStore (设置持久化)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Shizuku
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")

    debugImplementation("androidx.compose.ui:ui-tooling")

    // JVM 单测（规则匹配逻辑）
    testImplementation("junit:junit:4.13.2")
}
