plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.openapi.generator)
    alias(libs.plugins.protobuf)
}

android {
    namespace = "com.stoganet.tv"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.stoganet.tv"
        minSdk = 28
        targetSdk = 37
        versionCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.useJUnitPlatform()
                it.jvmArgs("-Xmx1g")
            }
        }
    }

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "/META-INF/LICENSE*")
    }
}

kotlin {
    jvmToolchain(25)
    // Keep JDK 25 toolchain but target JVM 21 to match compileOptions and stay within
    // Robolectric's ASM version support ceiling (class file major version 65).
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
}

openApiGenerate {
    generatorName.set("kotlin")
    library.set("jvm-retrofit2")
    inputSpec.set("${rootDir}/openapi/openapi.yaml")
    outputDir.set(layout.buildDirectory.dir("generated/openapi").get().asFile.absolutePath)
    apiPackage.set("com.stoganet.tv.api")
    modelPackage.set("com.stoganet.tv.api.model")
    packageName.set("com.stoganet.tv.api")
    configOptions.set(
        mapOf(
            "useCoroutines" to "true",
            "serializationLibrary" to "kotlinx_serialization",
            "enumPropertyNaming" to "UPPERCASE",
            "omitGradleWrapper" to "true",
        ),
    )
}

android.sourceSets["main"].java.srcDirs(
    "${layout.buildDirectory.get().asFile}/generated/java/generateDebugProto/java",
)

android.sourceSets["main"].kotlin.srcDirs(
    "${layout.buildDirectory.get().asFile}/generated/openapi/src/main/kotlin",
)

val openApiOutDir = layout.buildDirectory.dir("generated/openapi")

tasks.named("openApiGenerate") {
    val unusedSupportingFiles = listOf(
        "src/main/kotlin/com/stoganet/tv/api/infrastructure/ApiClient.kt",
        "src/main/kotlin/com/stoganet/tv/api/auth",
        "src/main/kotlin/com/stoganet/tv/api/DefaultApi.kt",
        "src/main/kotlin/com/stoganet/tv/api/infrastructure/ResponseExt.kt",
    )
    val outDirProvider = openApiOutDir
    doLast {
        val out = outDirProvider.get().asFile
        unusedSupportingFiles.forEach { rel ->
            val target = File(out, rel)
            if (target.exists()) {
                target.deleteRecursively()
            } else {
                logger.warn("openApiGenerate cleanup: expected path not found (generator output may have changed): $target")
            }
        }
    }
}

tasks.named("preBuild") {
    dependsOn("openApiGenerate", "generateDebugProto")
}

tasks.register("generateSources") {
    dependsOn("openApiGenerate", "generateDebugProto")
    group = "build setup"
    description = "Regenerate all generated sources after clean"
}

tasks.matching { it.name.startsWith("detekt") }.configureEach {
    dependsOn("openApiGenerate", "generateDebugProto")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.asProvider().get()}"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") { option("lite") }
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.tv.material)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.auth)
    implementation(libs.ktor.client.logging)
    implementation(libs.serialization.json)
    implementation(libs.collections.immutable)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.ktor3)

    implementation(libs.datastore)
    implementation(libs.protobuf.javalite)
    implementation(libs.tink.android)

    implementation(libs.timber)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.media3.session)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.vintage.engine)
    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.test.core)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.mockk)

    debugImplementation(libs.compose.ui.test.manifest)
}
