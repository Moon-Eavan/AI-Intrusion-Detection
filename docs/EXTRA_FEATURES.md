# ID 10 — 기타 기획되지 않은 편의 기능 (2개)

| 기능 | 이름 | 구현 계층 |
| --- | --- | --- |
| A | 원격 감시 설정 | Django + YOLOv5 + Android |
| B | 이벤트 클립 녹화 + 보관 정책 | YOLOv5 + Django + Android |

두 기능 모두 **구현 전(설계 단계)** 이다. 아래 값(주기, 길이, 상한 등)은 기본안이며 구현 중 조정할 수 있다.
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
| `conf_threshold` | float / `0.25` (범위 0.25~0.90) | SeekBar | `conf`가 임계값 미만인 박스 무시. `detect.py` 기본 `--conf-thres 0.25`보다 낮추는 것은 무의미 |
| `cooldown_sec` | int / `0` | 숫자 입력 | 같은 클래스는 마지막 등록 후 이 시간 안에 재출현해도 등록하지 않음 |
| `available_classes` | 리스트 (Edge가 등록) | 체크 목록의 항목 | Edge 시작 시 모델 `names`(COCO 80개)를 서버에 올림 |
| `edge_last_seen` | datetime (서버가 기록) | "Edge 온라인 / n초 전" 표시 | Edge가 polling할 때 서버가 현재 시각 기록 |

감시 대상은 새 클래스를 학습하는 것이 아니라 **YOLOv5 기본 모델이 이미 인식하는 80개 COCO 클래스 중에서 고르는 필터**다.

### 구현

**Django**
- `MonitorConfig` 모델(행 1개만 사용, 없으면 기본값으로 생성), serializer
- `GET /api_root/config/` — 현재 설정 (Android, Edge)
- `PATCH /api_root/config/` — 설정 변경 (Android)
- `POST /api_root/config/edge/` — Edge 등록·heartbeat (`available_classes` 전송, 서버가 `edge_last_seen = now`)
- `edge_last_seen`, `available_classes`는 PATCH로 바꿀 수 없게 read-only

**YOLOv5**
- 백그라운드 스레드가 5초마다 `POST /config/edge/` → `GET /config/`, 받은 설정을 공유 객체에 저장(lock)
- 서버 연결 실패 시 마지막 설정 유지, 검출 계속
- `detect.py`의 `detected[int(cls)] = 1` 앞에 클래스·신뢰도 필터, `cd.add` 앞에 `enabled` 확인
- `ChangeDetection`에 클래스별 마지막 등록 시각을 두고 `cooldown_sec` 적용
- 설정이 바뀌면 콘솔에 변경 내용 출력 (증빙용)

**Android**
- `SettingsActivity`: Switch, 클래스 체크 목록(`available_classes` 기반), SeekBar, 쿨다운 입력, 저장 → PATCH
- `MainActivity`: 설정 화면 진입 버튼, Edge 상태 표시 (`edge_last_seen`이 15초 이내면 온라인)
- 서버 주소는 기존 `ApiConfig.BASE_URL` 사용

### 한계 (보고서에 명시)

- API 인증이 없어 URL을 알면 누구나 설정을 바꿀 수 있다.
- 반영까지 최대 polling 주기(약 5초) 지연.
- Edge 1대를 가정한다(설정 1개).

### 증빙 시나리오

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
- 녹화 중 새로 나타난 클래스는 진행 중인 이벤트의 `text`에 합친다(별도 이벤트로 만들지 않음)
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

### 한계 (보고서에 명시)

- 녹화 중 발생한 출현은 진행 중 클립에 합쳐진다.
- 이벤트 후 약 3초 + 인코딩·업로드 시간이 지나야 게시물이 생긴다.
- 클립은 저해상도(320×240)이며, 오래된 클립은 상한 초과 시 삭제된다.

### 증빙 시나리오

1. 출현 이벤트 후 API에 `clip` URL이 포함된 게시물
2. 웹 상세에서 클립 재생
3. Android 상세에서 클립 재생
4. 상한을 낮춰(예: 1MB) 클립을 여러 개 올린 뒤, 오래된 게시물이 "클립 만료됨"이 되고 이미지·제목은 남아 있는 화면

---

## 보고서 행 구성 (안)

보고서 원문: "서버/클라이언트 각각 구현이 필요한 경우 별도 요구사항으로 등록".

| 행 | 내용 |
| --- | --- |
| 10-1 | 기능 A — 서버: 감시 설정 API, Edge heartbeat |
| 10-2 | 기능 A — Edge: 설정 polling 및 필터·쿨다운 적용 |
| 10-3 | 기능 A — Android: 감시 설정 화면, Edge 상태 표시 |
| 10-4 | 기능 B — Edge: 전후 클립 녹화·인코딩·업로드 |
| 10-5 | 기능 B — 서버: 클립 저장, 파일 정리, 용량 기준 보관 정책 |
| 10-6 | 기능 B — Android: 클립 재생 |

## 회귀 확인

- 기존 Android 동기화, 직접 업로드, 검색·정렬·즐겨찾기·오프라인 캐시·백그라운드 알림
- 강의 기본 동작(0→1 감지 시 게시물 등록), Django 기존 테스트 28개 + 신규 기능 테스트
- Android 게시물 키(`created_date`) 체계 유지
