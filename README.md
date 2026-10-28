# RoadMate

Pleos Connect 차량용 앱 공모전 출품작. 차가 드라이브를 자동으로 기록하고, 도착하면 한 페이지 추억과 공유 카드로 남겨 주는 앱.

## 개발 환경
- Android Studio, Android SDK API 34 이상, minSdk 28, Kotlin
- Pleos Connect Emulator (AVD: `Pleos_Connect_v2.1.0`)
  - system image URL은 Pleos Playground에서 확인

## 처음 받았을 때
1. `git clone` 후 Android Studio로 폴더 열기
2. Sync Project with Gradle Files
3. 실행 기기를 **Pleos Connect** 에뮬레이터로 선택 (휴대폰 에뮬레이터 X)
4. ▶ Run → 화면에 "✅ 연결 성공"이 뜨면 정상

## Pleos SDK
| SDK | 버전 | 비고 |
|---|---|---|
| Vehicle | 2.3.2 | `app/build.gradle.kts` (2.3.1은 저장소에 없음) |
| NaviHelper | 3.0.5 | 추가 예정 |
| SpeechToText / TextToSpeech | 3.3.1 | Playground에서 "준비 중" |
| FusedLocation | 1.1.3 | Playground에서 "준비 중" |

Maven 저장소는 `settings.gradle.kts`에 등록되어 있음. 퍼미션은 `app/src/main/AndroidManifest.xml`.

## 주의
- 클라이언트 ID/시크릿은 **절대 커밋하지 않기**. `local.properties`(git 제외됨)에 저장.
- 의존성은 최상위 `build.gradle.kts`가 아니라 `app/build.gradle.kts`에 추가.
