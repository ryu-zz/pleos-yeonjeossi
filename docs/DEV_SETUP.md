# 연저씨 (너의 연수 아저씨)
Pleos Connect 차량용 앱 공모전 출품작. 초보 운전자가 차량 조작을 퀘스트로 익히고, 주행 중에는 연저씨가 짧게 안내해 주는 연수 앱.

## 개발 환경
- Android Studio, Android SDK API 34 이상, minSdk 28, Kotlin
- Pleos Connect Emulator (AVD: `Pleos_Connect_v2.1.0`, system image URL은 Pleos Playground에서 확인)
- 앱은 에뮬레이터의 `운전자` 사용자 계정에서 실행됨 (`adb shell run-as com.example.test --user 10 ...`)

## 처음 받았을 때
1. 작업 브랜치 받기: `git clone https://github.com/ryu-zz/roadmate.git` 후 `git checkout feature/yeonsu-first-ride`
2. Android Studio로 폴더 열기 → Sync Project with Gradle Files (`local.properties`는 자동 생성됨)
3. 실행 기기를 **Pleos Connect** 에뮬레이터로 선택 (휴대폰 에뮬레이터 X)
4. ▶ Run → 시작 화면(로딩 → 연저씨 시작 화면)이 뜨면 정상

## 화면 구조 (`app/src/main/java/com/example/test/yeonsu/`)
- `FadeActivity`: 모든 화면의 부모. 전환은 `goTo(X::class.java)`로 하고 페이드인이 자동 적용됨
- `LoadingActivity` → `StartActivity` → `HomeActivity`(일반 주행 / 퀘스트 / 마이페이지)
- 일반 주행: `DriveLoadingActivity` → `DriveActivity`(안내 문구 `DriveCoach.kt`) + 내비 앱 실행
- 퀘스트: `QuestTypesActivity` → `QuestListActivity` → `QuestDetailActivity`, 데이터는 `QuestData.kt`
- 마이페이지: `RecordsActivity`
- 화면 틀: `ScreenActivity` (헤더 + 창 폭에 따라 compact). 2/3 폭 화면은 연저씨 마크를 맨 왼쪽, 홈·설정·도움말을 맨 오른쪽에 둠
- 차량 퀘스트 50개와 팁은 `상세 튜토리얼.xlsx`를 옮긴 것. 사진은 `res/drawable-nodpi/quest_img_{카테고리}_{번호}.jpg`
- 퀘스트 완료 버튼("동작을 마쳤다고 치기")은 미리보기용이며 실제 차량 신호 판정이 아님

## Pleos SDK
| SDK | 버전 | Playground 상태 | 에뮬레이터 |
|---|---|---|---|
| Vehicle | 2.3.2 | 사용 가능 | 동작 (2.3.1은 저장소에 없음) |
| FusedLocation | 1.1.3 | 준비 중 | 위치 수신 |
| SpeechToText | 3.3.1 | 준비 중 | 초기화 (ON_DEVICE) |
| TextToSpeech | 3.3.1 | 준비 중 | 초기화 (ON_DEVICE), 남자 목소리 `EUNWOO` |
| NaviHelper | 3.0.5 | 사용 가능 | 아직 연동 안 함 |
"준비 중" SDK는 에뮬레이터에서 동작하지만 실차·심사 허용 여부는 미확인.
Maven 저장소는 `settings.gradle.kts`에 등록되어 있음. 퍼미션은 `app/src/main/AndroidManifest.xml`.

## 알려진 제약
- 앱이 스스로 분할 화면을 만들 수 없음. 일반 주행은 내비를 켠 뒤 손잡이를 왼쪽으로 끌어 오른쪽 1/3에 연저씨를 놓는 방식
- 에뮬레이터에서 `PERF_VEHICLE_SPEED`는 없음. Vehicle 읽기 104개 중 54개만 읽힘

## 주의
- 클라이언트 ID/시크릿은 **절대 커밋하지 않기**. `local.properties`(git 제외됨)에 저장.
- 의존성은 최상위 `build.gradle.kts`가 아니라 `app/build.gradle.kts`에 추가.
- `main`에 바로 올리지 말고 브랜치를 만들어 작업.
