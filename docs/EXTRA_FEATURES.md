# ID 10 — 기타 기획되지 않은 편의 기능 (2개)

| 기능 | 이름 | 구현 계층 |
| --- | --- | --- |
| A | 원격 감시 설정 | Django + YOLOv5 + Android |
| B | 이벤트 클립 녹화 + 보관 정책 | YOLOv5 + Django + Android |

- 기능 A: **구현·웹캠 증빙 완료 (Phase 4, 2026-11-07)**. 구현하며 바뀐 점은 "구현" 절, 결과는 "구현 확인 결과"·"증빙 결과"에 적었다.
- 기능 B: **구현·웹캠 증빙 완료 (Phase 5, 2026-11-07)**. 결과는 기능 B "구현 확인 결과" 절.
PythonAnywhere 무료 계정은 서버 → Edge 방향 연결(push, WebSocket)이 안 되므로 **Edge가 서버를 polling**하는 구조를 쓴다.

---

## 기능 A. 원격 감시 설정

### 정의

Android 앱에서 YOLO Edge의 감시 동작(ON/OFF, 감시 대상 클래스, 신뢰도 임계값, 재등록 대기시간)을 원격으로 바꾸고, Edge가 몇 초 안에 반영한다. Edge의 접속 상태도 앱에서 확인한다.

### 설정 항목

| 필드 | 타입 / 기본값 | Android UI | Edge 반영 |
| --- | --- | --- | --- |
| `enabled` | bool / `true` | Switch | `false`면 검출 화면은 유지, 서버 전송(`cd.add`)만 중단 |
| `target_classes` | 클래스명 리스트 / `[]` (= 전체) | 체크 목록 | 목록에 없는 클래스는 `detected`에 1로 표시하지 않음 |
| `conf_threshold` | float / `0.5` (범위 0.25~0.90) | Slider (0.05 단위) | `conf`가 임계값 미만인 박스 무시(박스도 그리지 않음). 기본 0.5는 Phase 3 실측 표준값. `detect.py`는 `--conf-thres` 없이(기본 0.25) 실행해야 앱에서 0.25까지 낮출 수 있다 |
| `cooldown_sec` | int / `0` (0~3600) | 숫자 입력 | 같은 클래스는 마지막 등록 후 이 시간 안에 재출현해도 등록하지 않음 |
| `available_classes` | 리스트 (Edge가 등록) | 클래스 칩 목록 | Edge가 heartbeat 때 모델 `names`(COCO 80개)를 서버에 올림 |
| `edge_last_seen` | datetime (서버가 기록) | — | Edge heartbeat 때 서버가 현재 시각 기록 |
| `edge_online`, `edge_seen_sec` | 응답 전용 (서버 계산) | "Edge 온라인 · n초 전" | 마지막 heartbeat가 15초 이내면 온라인. 서버 시각으로 계산해 휴대폰 시계 차이 영향 없음 |

감시 대상은 새 클래스를 학습하는 것이 아니라 **YOLOv5 기본 모델이 이미 인식하는 80개 COCO 클래스 중에서 고르는 필터**다.

### 구현

**Django** (`blog/models.py`, `serializers.py`, `views.py`, `urls.py`, migration `0002_monitorconfig`)
- `MonitorConfig` 모델(행 1개 pk=1만 사용, `MonitorConfig.load()`가 없으면 기본값으로 생성), admin 등록
- `GET /api_root/config/` — 현재 설정
- `PUT`/`PATCH /api_root/config/` — 설정 변경. **Android는 PUT 사용**(`HttpURLConnection`이 PATCH 미지원)
- `POST /api_root/config/edge/` — Edge heartbeat. `available_classes`(JSON) 등록, `edge_last_seen = now`, **응답으로 현재 설정을 돌려줌** → Edge는 polling 1회에 요청 1번
- 검증: `conf_threshold` 0.25~0.9, `cooldown_sec` 0~3600, `target_classes`는 문자열 리스트이며 `available_classes`에 있는 이름만 허용. `available_classes`·`edge_last_seen`은 PUT/PATCH로 못 바꿈 → 위반 시 400
- 테스트 12개 추가 (`MonitorConfigApiTests`)

**YOLOv5** (`monitorconfig.py` 신규, `changedetection.py`·`detect.py` 수정)
- `MonitorConfig`(Edge 쪽 클래스): 시작 시 1회 + 백그라운드 daemon 스레드가 5초마다 heartbeat, 응답 설정을 lock으로 보관
- 서버 연결 실패·설정 API 없는 서버(404)면 마지막 설정(처음엔 기본값: 전체 클래스, 0.5, 대기 0) 유지, 검출 계속. 실패/복구는 상태가 바뀔 때만 출력
- `detect.py` 박스 루프 맨 앞에 2줄: `cd.config.accept(클래스명, conf)`가 False면 `continue` → `detected`에 반영하지 않고 **박스도 그리지 않음**(서버로 가는 사진에 대상 외 박스가 없음)
- `changedetection.py`: 0→1 판정 줄에 `self.allowed(names, i)` 조건 추가. 감시 OFF면 미등록, 클래스별 마지막 등록 시각으로 `cooldown_sec` 적용. OFF 동안에도 `result_prev`(검출 상태)는 갱신하므로 다시 켤 때 이미 화면에 있던 객체가 한꺼번에 등록되지 않음
- 콘솔 로그(증빙용): `MonitorConfig: 서버 연결됨`, `MonitorConfig: 설정 반영 target_classes=['person']`, `ChangeDetection: 감시 OFF - person 미등록`, `ChangeDetection: 재등록 대기 중 - bus 미등록`

**Android** (`SettingsActivity`·`MonitorConfigApi` 신규, `MainActivity` 수정)
- `SettingsActivity`: Edge 상태 박스(5초 갱신), 감시 ON/OFF `MaterialSwitch`, 신뢰도 `Slider`(내부 정수 25~90, 화면 0.xx), 재등록 대기시간 입력, 감시 대상 칩 80개(`available_classes`, 선택 없음 = 전체), 저장 → PUT
- `MainActivity`: 상단에 "● Edge 온라인 · n초 전 · 감시 ON" 줄(화면이 보이는 동안 5초마다 갱신)과 "감시 설정" 버튼
- `MonitorConfigApi`: GET/PUT 공용 코드, 400이면 서버 검증 메시지 표시. 서버 주소는 기존 `ApiConfig.BASE_URL` 사용

### 구현 확인 결과 (2026-11-06)

- Django: `python manage.py test blog` 40개 OK (기존 28 + 신규 12). 작성 중 음수 `cooldown_sec`가 400이 아닌 DB 오류(500)가 되는 문제를 테스트로 발견 → 모델에 `MinValueValidator(0)` 추가
- Edge (샘플 이미지 + 임시 서버 복사본): 기본 → `bus` 등록 / 대상 `person`만 → `person`만 등록, 사진에 bus 박스 없음 / OFF → 0건 + `감시 OFF` 로그 / 대기 60초 → bus→zidane→bus에서 두 번째 bus `재등록 대기 중` / 임계값 0.9 → 0건 / 서버 없음 → `서버 연결 실패, 마지막 설정 유지` 후 검출 계속 / 실행 중 PATCH → 5.1초 뒤 반영
- Android (에뮬레이터 + 로컬 서버): 메인 상단 Edge 온라인/오프라인 표시, 설정 화면 로드, 대상 person·임계값 0.6·대기 10초 저장 → 서버 값 일치. 첫 구현의 소수 Slider(stepSize 0.05)가 0.57처럼 단계에 맞지 않는 문제 → 정수 % Slider로 수정

### 증빙 결과 (웹캠, 2026-11-07)

| 시나리오 | 설정 | 결과 |
| --- | --- | --- |
| 시작 직후 | 대상 전체, 0.5, 대기 0 | 약 30초에 18건 (chair·toilet·cell phone 깜빡임) |
| 1 | 대상 `person` | `person` 5건, `cell phone` 0건, 사진에 사람 박스만 |
| 2 | + `cell phone` | `cell phone` 등록. 휴대폰을 움직이자 6초에 9건 |
| 2-1 | + 재등록 대기 30초 | 같은 동작에 `cell phone` 1건, 7회 `재등록 대기 중` 로그 |
| 3 | 감시 OFF | 0건, `감시 OFF - … 미등록` 로그, 앱 "감시 OFF" |
| 4 | YOLO 종료 | 약 20초 후 앱 "Edge 오프라인 · 20초 전" |

신뢰도 0.5만으로도 이동하는 물체·배경 물체는 깜빡여 중복 등록되며, 대상 클래스와 재등록 대기시간을 함께 써야 한다는 것을 실측으로 확인했다.
증빙 파일: `../AI-Intrusion-Detection-docs/captures/phase4/`

### 도입 근거 (Phase 3 관측, 2026-11-04)

강의 기본 로직 + 기본 임계값 0.25로 웹캠을 75초 실행하자 게시물이 58건 생겼다. 대부분 배경 물체의 저신뢰도 오검출(옷장 문 → `refrigerator 0.30` 등)이 프레임마다 깜빡여 반복 등록된 것이다. 신뢰도 임계값·감시 대상 클래스·재등록 대기시간을 원격으로 조정하는 이 기능의 직접적인 필요성이다(보고서 기능 설명에 활용).

### 한계 (보고서에 명시)

- API 인증이 없어 URL을 알면 누구나 설정을 바꿀 수 있다.
- 반영까지 최대 polling 주기(약 5초) 지연.
- Edge 1대를 가정한다(설정 1개).

### 증빙 시나리오

웹캠 실행: `PHOTOBLOG_HOST=http://127.0.0.1:8000 venv/bin/python -u detect.py --source 0` (`--conf-thres` 없이, 임계값은 앱에서)

1. Android 설정 화면에서 대상 = `person`만 선택
2. Edge 콘솔의 설정 반영 로그
3. 사람 + 휴대폰이 함께 보여도 `person` 게시물만 등록된 API/웹 화면
4. 대상에 `cell phone` 추가 후 휴대폰 게시물도 등록되는 화면
5. 감시 OFF 상태에서 객체가 나타나도 게시물이 생기지 않는 화면
6. Edge 실행/종료에 따라 앱의 온라인/오프라인 표시 변화

---

## 기능 B. 이벤트 클립 녹화 + 보관 정책

### 정의

객체 출현(0→1) 시 **감지 전 3초 + 감지 후 3초** 영상을 짧은 H.264 mp4 클립으로 만들어 게시물과 함께 업로드한다. Android·웹에서 재생할 수 있고, 서버는 클립 총용량이 상한을 넘으면 오래된 클립부터 자동 삭제한다.

### Edge (YOLOv5)

- 별도 녹화 프로그램 없이 `detect.py`가 처리하는 프레임 `im0`(박스가 그려진 프레임)를 사용
- `collections.deque(maxlen=N)` ring buffer에 최근 3초 프레임 보관 (320×240로 축소 후 `copy()`)
- 0→1 감지 시 "녹화 중" 상태로 전환, 이후 3초 프레임 추가 수집
- 녹화 중 새로 나타난 클래스는 진행 중인 이벤트의 `text`에 `", "`로 이어 붙인다(별도 이벤트로 만들지 않음, 예: `person, bus, tie`)
- 수집이 끝나면 **백그라운드 스레드**에서 인코딩 + 업로드 (검출 루프가 멈추지 않게)
- 인코딩: `imageio` + `imageio-ffmpeg` (`codec="libx264"`, `pixelformat="yuv420p"`), BGR→RGB 변환
  - `cv2.VideoWriter`의 `mp4v`는 브라우저에서 재생되지 않아 사용하지 않음
  - 추가 설치: `pip install imageio imageio-ffmpeg` (YOLOv5 venv)
- FPS: 프레임 타임스탬프로 `프레임 수 ÷ 경과 시간`을 계산해 사용 (CPU 추론은 웹캠 30fps보다 느림)
- 업로드: 기존과 같은 `POST /api_root/Post/` 한 번에 `title`, `text`, `image`, `clip` 전송 → 게시물 생성이 이벤트 후 수 초 늦어진다
- 기능 A의 `enabled`가 `false`면 녹화도 하지 않음

### Django

- `Post`에 필드 추가 (migration 필요, 둘 다 선택 필드라 기존 Android 업로드·웹 작성에 영향 없음)
  - `clip = FileField(upload_to='blog_clip/%Y/%m/%d/', blank=True)`
  - `clip_expired = BooleanField(default=False)` — 보관 정책으로 삭제된 경우 표시용
- `PostSerializer` fields에 `clip`, `clip_expired` 추가 (`clip_expired`는 read-only)
- **파일 정리 시그널**: `post_delete`에서 `image`, `clip` 파일 삭제 (Django는 게시물을 지워도 파일을 자동 삭제하지 않음)
- **보관 정책 (용량 기준 FIFO)**
  - 상한 `CLIP_STORAGE_LIMIT_MB` (settings, 기본 200MB, 환경변수로 변경 가능)
  - 클립이 포함된 게시물 생성 후, **저장된 클립 전체 합계**가 상한을 넘으면 가장 오래된 클립부터 삭제
  - 클립 파일만 지우고(`clip` 비움, `clip_expired = True`) 게시물·썸네일 이미지는 유지
  - 파일당 크기 제한은 두지 않는다
- 웹 템플릿: 상세에 `<video controls>`, 만료 시 "클립 만료됨"; 목록에 클립 표시

### Android

- `PostItem`에 `clipUrl`, `clipExpired` 추가 (JSON 파싱)
- 목록 아이템에 클립 유무 표시, `ImageAdapter` → `DetailActivity` Intent extra로 클립 URL 전달
- `DetailActivity`: 클립이 있으면 `VideoView` + `MediaController`로 스트리밍 재생, 만료면 "클립 만료됨"
- **Room 오프라인 캐시(`PostCacheEntity`, DB v2)는 변경하지 않는다** → 오프라인 목록에서는 클립 없음. 스키마 변경에 따른 migration 위험을 피하기 위함

### 구현 파일

- YOLOv5: `cliprecorder.py`(신규: 버퍼·녹화·인코딩), `changedetection.py`(`send()`는 사진 저장 후 녹화 시작, `upload()`가 스레드에서 인코딩 + `image`·`clip` 함께 POST, 인코딩 실패 시 사진만 전송). `detect.py`는 이미 매 프레임 `cd.add(…, im0)`를 호출하므로 수정 없음
- 게시물 시각: `published_date` = 감지 순간(Edge), `created_date` = 업로드 시각(약 3~4초 뒤)
- 클립은 사진과 같은 폴더에 같은 이름의 `.mp4`로 저장(`runs/…/detected/`, Git 제외)
- Django: `blog/clip_storage.py`(`enforce_clip_storage_limit()`, `perform_create`에서 클립이 있을 때 호출), `blog/signals.py`(`apps.ready`에서 등록), mp4 확장자만 허용(`FileExtensionValidator`)
- 웹: 목록 썸네일 "▶ 클립"/"클립 만료" 배지, 상세 `<video controls>`(poster = 사진)
- Android: `PostItem.setClip()`(Room 캐시용 생성자는 그대로), 목록 배지, `DetailActivity` `VideoView` + `MediaController`, 준비되면 반복 재생

### 구현 확인 결과 (2026-11-07)

- Django 테스트 50개 OK (신규 10: 클립 업로드 URL, 클립 없는 업로드, mp4 외 400, `clip_expired` read-only, FIFO 만료 순서, 만료 파일 삭제, 상한 이하 유지, 게시물 삭제 시 파일 삭제, 웹 상세 video/만료 문구, 웹 목록 배지)
- 테스트 영상: 클립 약 6초·6fps·H.264 yuv420p·faststart, 첫 3초가 감지 전 장면, 녹화 중 `tie` 병합
- 웹캠 (상한 0.3MB로 임시 실행): 이벤트 6건 모두 클립(22~127KB), 녹화 중 `cell phone` 병합, 오래된 3건 만료(게시물·사진 유지), 앱 목록 배지·상세 재생·만료 화면 확인
- 증빙 파일: `../AI-Intrusion-Detection-docs/captures/phase5/`

### 한계 (보고서에 명시)

- Edge 시작 직후의 이벤트는 버퍼가 비어 감지 전 구간이 짧다.
- 녹화 중(감지 후 3초 안)에 Edge를 종료하면 그 이벤트는 업로드되지 않는다 (Phase 7에서 1건 확인).
- PythonAnywhere static 서버는 Range 요청을 지원하지 않아 클립을 통째로 받아 재생한다(Chrome·Android 확인, Safari는 재생 안 될 수 있음).
- 녹화 중 발생한 출현은 진행 중 클립에 합쳐진다.
- 이벤트 후 약 3초 + 인코딩·업로드 시간이 지나야 게시물이 생긴다.
- 클립은 저해상도(320×240)이며, 오래된 클립은 상한 초과 시 삭제된다.

### 증빙 시나리오

1. 출현 이벤트 후 API에 `clip` URL이 포함된 게시물
2. 웹 상세에서 클립 재생
3. Android 상세에서 클립 재생
4. 상한을 낮춰(예: 1MB) 클립을 여러 개 올린 뒤, 오래된 게시물이 "클립 만료됨"이 되고 이미지·제목은 남아 있는 화면

---

## 보고서 행 구성

보고서 원문: "서버/클라이언트 각각 구현이 필요한 경우 별도 요구사항으로 등록". 양식의 ID 10 아래 빈 행이 4개라 **기능 × 서버/클라이언트 4행**으로 쓴다 (2026-11-07 결정, 문구 초안: `docs/REPORT_DRAFT.md`).

| 보고서 행 | 내용 | 증빙 파일명(작업 당시 번호) |
| --- | --- | --- |
| 10-1 | 기능 A — 서버: 감시 설정 API, Edge heartbeat | 10-1 |
| 10-2 | 기능 A — 클라이언트: Edge 설정 반영·필터·대기시간, Android 설정 화면·Edge 상태 | 10-2, 10-3 |
| 10-3 | 기능 B — 서버: 클립 저장, 파일 정리, 용량 기준 보관 정책, 웹 재생 | 10-5 |
| 10-4 | 기능 B — 클라이언트: Edge 전후 클립 녹화·인코딩·업로드, Android 재생 | 10-4, 10-6 |

## 회귀 확인

- 기존 Android 동기화, 직접 업로드, 검색·정렬·즐겨찾기·오프라인 캐시·백그라운드 알림
- 강의 기본 동작(0→1 감지 시 게시물 등록), Django 기존 테스트 28개 + 신규 기능 테스트
- Android 게시물 키(`created_date`) 체계 유지
