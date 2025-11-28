import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.byteBuddy)
    alias(libs.plugins.proguardUuid)
}

val localProperties = Properties()
localProperties.load(FileInputStream(rootProject.file("local.properties")))

android {
    namespace = "io.opentelemetry.android.demo"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.opentelemetry.android.demo"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        all {
            val accessToken = localProperties["rum.access.token"] as String?
            resValue("string", "rum_access_token", accessToken ?: "fakebroken")
        }
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs["debug"]
        }
    }
    buildFeatures {
        compose = true
    }
    val javaVersion = JavaVersion.VERSION_11
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = javaVersion.toString()
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.core.ktx)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.security.crypto)
    implementation(libs.okhttp)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.gson)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.opentelemetry.api.incubator)
    implementation(libs.honeycomb.opentelemetry.android)
    implementation(libs.honeycomb.opentelemetry.android.compose)
    implementation("io.opentelemetry:opentelemetry-extension-kotlin:1.47.0")



    testImplementation(libs.junit)
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.robolectric:robolectric:4.11.1")
//    implementation("io.opentelemetry.android.instrumentation:view-click:0.11.0-alpha")

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.opentelemetry.sdk)
    implementation(libs.otel.kotlin.extension)
    implementation(libs.android.agent)
    implementation(libs.otel.semconv.incubating)
    implementation(libs.opentelemetry.exporter.otlp)

    implementation(libs.okhttp.agent)  // OpenTelemetry OkHttp library
    implementation(libs.okhttp.library)  // OpenTelemetry OkHttp library
    byteBuddy(libs.okhttp.agent)  // OpenTelemetry OkHttp agent
}
