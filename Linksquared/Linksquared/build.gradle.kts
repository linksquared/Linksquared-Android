plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("kotlin-parcelize")
    id("maven-publish")
}

android {
    namespace = "io.linksquared"
    compileSdk = 34

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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

    productFlavors {
        val BOOLEAN = "boolean"
        val STRING = "String"
        val TRUE = "true"
        val FALSE = "false"
        val SERVER_URL = "SERVER_URL"

        val SERVER_URL_DEVELOPMENT = "\"https://sdk.sqd.link/api/v1/sdk/\""
        val SERVER_URL_PRODUCTION = "\"https://sdk.sqd.link/api/v1/sdk/\""

        create("envDevelopment") {
            buildConfigField(STRING, SERVER_URL, SERVER_URL_DEVELOPMENT)
            dimension = "default"
        }

        create("envProd") {
            buildConfigField(STRING, SERVER_URL, SERVER_URL_PRODUCTION)
            dimension = "default"
        }
    }

    flavorDimensions.add("default")

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    implementation(libs.retrofit)
    implementation(libs.gson)
    implementation(libs.okhttp)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)

    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.livedata.core.ktx)

    implementation(libs.androidx.constraintlayout)

    //noinspection GradleDynamicVersion
    implementation("com.android.installreferrer:installreferrer:2.+")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

}

private val libraryGroupId = "io.linksquared"
private val libraryArtifactId = "Linksquared"
private val libraryVersion = "1.0.0"

project.afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                groupId = libraryGroupId
                artifactId = libraryArtifactId
                version = libraryVersion
                artifact(tasks["bundleEnvProdReleaseAar"])
                artifact(tasks["androidSourcesJar"])

                pom.withXml {
                    val dependenciesNode = asNode().appendNode("dependencies")
                    configurations.implementation.get().allDependencies.forEach { dependency ->
                        val dependencyNode = dependenciesNode.appendNode("dependency")
                        dependencyNode.appendNode("groupId", dependency.group)
                        dependencyNode.appendNode("artifactId", dependency.name)
                        dependencyNode.appendNode("version", dependency.version)
                    }
                }
            }
        }
    }
}

tasks.register("androidSourcesJar", Jar::class) {
    archiveClassifier.set("sources")

    if (project.plugins.hasPlugin("com.android.library")) {
        // For Android libraries
        from(android.sourceSets["main"].java.srcDirs)
    } else {
        // For pure Kotlin libraries
        from(sourceSets["main"].java.srcDirs)
        from(kotlin.sourceSets["main"].kotlin.srcDirs)
    }
}