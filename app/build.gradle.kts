import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}
// 릴리스 서명 정보는 저장소에 올리지 않는 keystore.properties 에서 읽는다(없으면 서명 없이 빌드).
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.example.test"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.test"
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (keystoreProps.getProperty("storeFile") != null) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        release {
            if (keystoreProps.getProperty("storeFile") != null) signingConfig = signingConfigs.getByName("release")
            // 코드 최적화(R8)를 켜면 시작 시 kotlin 클래스 접근 오류로 앱이 죽어서 끈다.
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    // 단위 테스트에서 실제 JSON 저장 경로(수행 기록)를 확인하기 위한 org.json (안드로이드 단위 테스트의 기본 org.json 은 빈 껍데기)
    testImplementation("org.json:json:20240303")
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    // Pleos SDK (저장소는 settings.gradle.kts 에 등록됨)
    implementation("ai.pleos.playground:Vehicle:2.3.2")
    // Playground에서 "준비 중" 표시지만 라이브러리는 배포됨. 동작 여부 시험용
    implementation("ai.pleos.playground:FusedLocation:1.1.3")
    implementation("ai.pleos.playground:SpeechToText:3.3.1")
    implementation("ai.pleos.playground:TextToSpeech:3.3.1")
    // 내비 연동: 목적지 경로 요청, 도착 이벤트
    implementation("ai.pleos.playground:NaviHelper:3.0.5")
}