# FitBuddy

카메라로 운동 자세를 실시간 분석하고, 체중을 기록하고, 주변 운동 시설을 찾을 수 있는 Android 앱입니다.

자세 분석·챗봇·시설 검색은 모두 별도의 백엔드 서버(FastAPI 계열, 이 저장소에 포함되지 않음)를 호출합니다.

## 주요 기능

| 기능 | 설명 | 관련 화면 |
|---|---|---|
| 회원가입 / 로그인 | 이메일·비밀번호 기반 인증 후 신체 정보 입력 | `SignupActivity`, `LoginActivity`, `UserInfoActivity` |
| 체중 트래커 | 최근 7일 체중 추이를 MPAndroidChart 라인 차트로 표시 | `WeightTrackerActivity` |
| 운동 목록 | 상체·하체·복부·전신 카테고리별 운동 선택 | `ExerciseCategoryActivity`, `ExerciseListActivity` |
| 실시간 자세 분석 | CameraX 프리뷰 프레임을 서버로 보내 관절 좌표와 피드백을 받아 화면에 오버레이 | `ExerciseExecutionActivity`, `PoseOverlayView` |
| AI 챗봇 | 운동 관련 질문을 주고받는 대화형 화면 | `ChatActivity` |
| 주변 운동 시설 | 현재 위치 기준 반경 검색, Leaflet 지도와 목록으로 결과 표시 | `NearbyFacilityActivity` |

## 화면 흐름

```
SignupActivity (런처)
  ├─ 가입 성공 → UserInfoActivity ─→ WeightTrackerActivity
  └─ "로그인" → LoginActivity ─────→ WeightTrackerActivity

WeightTrackerActivity (홈)
  ├─ 운동 카드   → ExerciseCategoryActivity → ExerciseListActivity → ExerciseExecutionActivity
  ├─ 챗봇 카드   → ChatActivity
  └─ 시설 카드   → NearbyFacilityActivity
```

## 기술 스택

- **언어 / 빌드** — Kotlin 1.9.0, AGP 8.2.1, Gradle 8.5, JVM target 17
- **SDK** — `minSdk 24`, `compileSdk` / `targetSdk 34`
- **UI** — View Binding, Material Components, ConstraintLayout, RecyclerView, CardView
- **카메라** — CameraX 1.3.1 (`camera-core` / `camera2` / `lifecycle` / `view`)
- **네트워크** — Retrofit 2.9.0 + Gson 컨버터, OkHttp 4.10.0 + logging interceptor
- **비동기** — Kotlin Coroutines 1.7.3
- **차트** — MPAndroidChart v3.1.0 (JitPack)
- **위치** — Google Play Services Location 21.0.1 (`FusedLocationProviderClient`)
- **지도** — WebView에 Leaflet 1.7.1 + OpenStreetMap 타일을 인라인 HTML로 로드 (API 키 불필요)

## 프로젝트 구조

```
app/src/main/java/com/fitbuddy/app/
├── SignupActivity.kt / LoginActivity.kt / UserInfoActivity.kt   인증 및 신체 정보 입력
├── WeightTrackerActivity.kt                                     홈 · 체중 차트
├── ExerciseCategoryActivity.kt / ExerciseListActivity.kt        운동 선택
├── ExerciseExecutionActivity.kt                                 카메라 · 타이머 · 자세 분석
├── PoseOverlayView.kt                                           관절 좌표를 프리뷰 위에 그리는 커스텀 View
├── ChatActivity.kt                                              챗봇
├── NearbyFacilityActivity.kt                                    주변 시설 검색 · 지도
├── adapters/     ChatAdapter, ExerciseAdapter, FacilityAdapter
├── models/       Exercise, Message                              화면 전용 모델
└── network/
    ├── ApiClient.kt / ApiService.kt          메인 API (인증 · 자세 · 시설)
    ├── ChatApiClient.kt / ChatApiService.kt  챗봇 API
    ├── PoseApiModels.kt                      자세 분석 요청/응답 DTO
    ├── FacilityApiModels.kt                  시설 검색 요청/응답 DTO
    └── ChatApiModels.kt                      챗봇 요청/응답 DTO
```

API DTO는 모두 `com.fitbuddy.app.network` 패키지에 모여 있습니다. 새 엔드포인트를 추가할 때도 같은 자리에 두세요.

## 백엔드 API

Base URL은 [`ApiClient.kt`](app/src/main/java/com/fitbuddy/app/network/ApiClient.kt)와 [`ChatApiClient.kt`](app/src/main/java/com/fitbuddy/app/network/ChatApiClient.kt)에 각각 상수로 박혀 있습니다.

```
http://54.206.28.172:8000
```

| 메서드 | 경로 | 용도 |
|---|---|---|
| `GET`  | `/` | 서버 헬스 체크 |
| `POST` | `/signup` | 회원가입 |
| `POST` | `/login` | 로그인 |
| `POST` | `/user/info` | 키·몸무게·성별·운동 목표 저장 |
| `POST` | `/pose/analyze` | Base64 프레임 → 관절 좌표·각도·피드백 |
| `POST` | `/facility/nearby` | 위경도·반경 → 주변 시설 목록 |
| `POST` | `/api/chat` | 챗봇 메시지 |

평문 HTTP를 쓰기 때문에 매니페스트에 `android:usesCleartextTraffic="true"`가 설정되어 있습니다.

### 로컬 백엔드로 붙이기

에뮬레이터에서 PC의 로컬 서버를 쓰려면 두 파일의 `BASE_URL`을 `http://10.0.2.2:8000` 으로 바꿉니다. **커밋에 섞여 올라가지 않도록 주의하세요.** 실기기라면 PC의 LAN IP를 씁니다.

## 권한

| 권한 | 사용처 |
|---|---|
| `CAMERA` | 자세 분석용 프리뷰 |
| `INTERNET` | 모든 API 호출 |
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` | 주변 시설 검색 시 현재 위치 |

카메라와 위치 권한은 런타임에 요청합니다.

## 빌드 및 실행

Android Studio(Hedgehog 이상 권장)에서 프로젝트를 열고 `app` 구성을 실행하면 됩니다. JDK 17이 필요합니다.

> **주의** — 이 저장소에는 Gradle 래퍼가 완전하지 않습니다. `gradle/wrapper/gradle-wrapper.properties`만 있고 `gradlew` 스크립트와 `gradle-wrapper.jar`가 빠져 있어서 커맨드라인 `./gradlew` 빌드는 바로 동작하지 않습니다. Android Studio는 자체 Gradle로 빌드하므로 문제가 없습니다. CLI 빌드가 필요하면 Android Studio에서 한 번 실행해 래퍼를 생성하거나, 로컬에 설치된 Gradle 8.5로 `gradle assembleDebug` 를 쓰세요.

자세 분석 기능은 에뮬레이터 가상 카메라로도 화면 확인은 되지만, 실제 관절 인식 결과를 보려면 실기기를 권장합니다.

## 알려진 제약

- 운동 목록([`ExerciseListActivity`](app/src/main/java/com/fitbuddy/app/ExerciseListActivity.kt))은 하드코딩된 12개 항목입니다.
- 체중 기록에 로컬 저장소가 없습니다. 최근 7일 그래프의 과거 데이터는 매 실행마다 임의 값으로 생성되며, 앱을 종료하면 입력한 값이 사라집니다.
- 로그인 상태를 유지하지 않아 실행할 때마다 다시 인증해야 합니다.
- 백엔드 서버가 떠 있지 않으면 자세 분석·챗봇·시설 검색이 모두 실패합니다.
