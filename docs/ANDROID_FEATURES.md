# Android 기능과 ID 10 편의 기능

## 1. 이번 과제에서 Android의 역할

평가표에서 Android는 **ID 7(YOLO가 전송한 이미지를 모바일에서 확인)** 에 쓰인다.
서버 응답 형식(`GET /api_root/Post/`)이 그대로라면 YOLO 게시물도 기존 코드로 표시되므로 Android 코드는 수정할 필요가 없다.

| 기본 동작 | 구현 위치 | 이번 과제에서 |
| --- | --- | --- |
| 게시물 목록 GET + 이미지 다운로드 | `MainActivity` | ID 7 확인에 사용 |
| RecyclerView 목록 표시 | `ImageAdapter` | ID 7 캡처 화면 |
| 게시물 상세 | `DetailActivity` | 필요 시 보조 캡처 |
| 이미지 직접 업로드 (multipart POST) | `UploadActivity` | YOLO 자동 등록과 별개. ID 7 증빙으로 쓰지 않음 |
| 서버 주소 | `ApiConfig.BASE_URL` | 로컬 `http://10.0.2.2:8000` / Cloud `https://hoynola.pythonanywhere.com` |

기본 동작은 ID 10 편의 기능으로 계산하지 않는다.

## 2. 공통평가 01에서 구현된 추가 기능 (현재 코드에 존재)

| 기능 | 구현 |
| --- | --- |
| 검색 | `MainActivity.applyFilterAndSort()` — `title`/`text` 부분 일치 |
| 정렬 | `published_date`(없으면 `created_date`) 기준 최신순/오래된순 |
| 즐겨찾기 | Room `FavoriteEntity`/`FavoriteDao`, 앱 재시작 후 유지 |
| 오프라인 캐시 | Room `PostCacheEntity`/`PostCacheDao`, 네트워크 실패 시 마지막 목록(메타데이터·이미지 URL) 표시 |
| 백그라운드 동기화·알림 | WorkManager `PostSyncWorker`, 15분 주기 + 즉시 1회 실행 버튼, `SharedPreferences`에 알려진 게시물 키 저장 |

이 기능들은 공통평가 01의 평가 항목(14-1~14-5)으로 이미 제출된 것이다.

## 3. 구현상 제약 (회귀 테스트 시 참고)

- API 응답에 `id`가 없어 **`created_date`를 게시물 식별 키**로 쓴다(즐겨찾기·캐시·알림 공통). 서버 응답 필드나 YOLO가 보내는 `created_date`를 바꾸면 이 기능들에 영향이 있다.
- 오프라인 캐시는 이미지 자체가 아니라 URL을 저장한다.
- WorkManager 15분은 최소 주기이며 실제 실행 시점은 시스템이 정한다.
- 신규 게시물 알림은 첫 실행 시 기존 게시물을 기준값으로 저장하고 알리지 않는다.
- Android 13+는 알림 권한(`POST_NOTIFICATIONS`)을 요청한다.

## 4. ID 10 — 기타 기획되지 않은 편의 기능 (최대 2개)

**현재 상태: 미정.** 사용자가 기능을 정하기 전까지 임의로 선정하거나 구현하지 않는다.

정해지면 기능마다 아래를 이 절에 기록한다.

```text
### 기능 N: <기능명>
- 구분: 서버 / 클라이언트(Android) / Edge(YOLOv5)
- 정의:
- 동작 설명:
- 구현 위치(파일):
- 검증 방법:
- 증빙 캡처:
```

- 서버와 클라이언트를 모두 구현해야 하는 기능은 보고서에 **별도 요구사항(행)** 으로 등록한다(보고서 ID 10 원문).
- 기존 GET/POST·YOLO 등록·ID 7 동작이 깨지지 않는지 함께 확인한다.
