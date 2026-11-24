import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("keystore.properties")
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
}

// Реквизиты SMTP читаются из mail.properties (не коммитится), чтобы пароль
// почтового ящика не попадал в репозиторий и в APK.
val mailProperties = Properties()
val mailPropertiesFile = rootProject.file("mail.properties")
if (mailPropertiesFile.exists()) {
    mailPropertiesFile.inputStream().use { mailProperties.load(it) }
}

fun quoted(value: String): String = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "com.startline.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.startline.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 104
        versionName = "1.0.4"
        resourceConfigurations += listOf("ru")

        buildConfigField("String", "MAIL_HOST", quoted(mailProperties.getProperty("host", "")))
        buildConfigField("int", "MAIL_PORT", (mailProperties.getProperty("port", "587").toIntOrNull() ?: 587).toString())
        buildConfigField("String", "MAIL_USER", quoted(mailProperties.getProperty("user", "")))
        buildConfigField("String", "MAIL_PASSWORD", quoted(mailProperties.getProperty("password", "")))
        buildConfigField("String", "MAIL_FROM", quoted(mailProperties.getProperty("from", "")))
        buildConfigField("boolean", "MAIL_STARTTLS", (mailProperties.getProperty("starttls", "true").toBoolean()).toString())
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf("-Xskip-prerelease-check")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")

    implementation("androidx.compose.ui:ui:1.7.3")
    implementation("androidx.compose.ui:ui-graphics:1.7.3")
    implementation("androidx.compose.ui:ui-tooling-preview:1.7.3")
    implementation("androidx.compose.foundation:foundation:1.7.3")
    implementation("androidx.compose.material3:material3:1.3.0")
    implementation("androidx.compose.material:material-icons-extended:1.7.3")

    debugImplementation("androidx.compose.ui:ui-tooling:1.7.3")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20231013")
}
