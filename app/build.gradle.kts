plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.sporen.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sporen.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
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
        compose = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all { it.jvmArgs("-Xmx2g") }
        }
    }

    // Required to prevent duplicate META-INF files from Apache POI
    packaging {
        resources {
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/MANIFEST.MF"
            excludes += "META-INF/*.kotlin_module"
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
            excludes += "META-INF/services/javax.xml.parsers.SAXParserFactory"
            excludes += "META-INF/services/javax.xml.parsers.DocumentBuilderFactory"
            excludes += "META-INF/services/org.apache.xmlbeans.SchemaTypeSystem"
            excludes += "META-INF/services/org.apache.poi.ss.usermodel.WorkbookProvider"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons)
    debugImplementation(libs.androidx.ui.tooling)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Apache POI — xlsx parsing
    implementation(libs.apache.poi.ooxml) {
        // Exclude heavy/incompatible transitive deps; xmlbeans added separately below
        exclude(group = "org.apache.xmlbeans", module = "xmlbeans")
        exclude(group = "org.apache.commons", module = "commons-compress")
        exclude(group = "com.github.virtuald", module = "curvesapi")
        exclude(group = "org.apache.logging.log4j", module = "log4j-core")
        exclude(group = "commons-logging")
    }
    // xmlbeans required by POI OOXML for xlsx — xml-apis conflicts with Android, exclude it
    implementation("org.apache.xmlbeans:xmlbeans:5.2.0") {
        exclude(group = "xml-apis", module = "xml-apis")
    }
    // log4j-api required by POI at runtime (log4j-core excluded — too heavy for Android)
    implementation("org.apache.logging.log4j:log4j-api:2.20.0")
    // Required stax + XML streaming for OOXML on Android
    implementation("javax.xml.stream:stax-api:1.0-2")
    implementation("com.fasterxml:aalto-xml:1.3.2")
    implementation("org.apache.commons:commons-compress:1.26.2")

    // Google Sign-In
    implementation(libs.google.play.services.auth)

    // Testing
    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.androidx.room.testing)
}

