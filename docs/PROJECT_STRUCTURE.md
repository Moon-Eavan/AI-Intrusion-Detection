# 프로젝트 구조

현재 저장소 기준이다 (ID 10 기능 A·B 구현 포함).

## 1. 루트

```text
AI-Intrusion-Detection/
├── PhotoBlogServer/        # Django Server (공통평가 01 재사용)
├── PhotoViewer/            # Android Client (공통평가 01 재사용)
├── YOLOv5/                 # Edge Client (upstream YOLOv5 e128e59, .git 제거)
├── docs/                   # 과제 문서
├── tools/                  # 공통평가 01의 Raw Socket 요청 확인 도구 (이번 평가 항목 아님)
├── CLAUDE.md
├── README.md
├── .gitignore
└── 모바일웹서비스 프로젝트_공통평가 02_수행 결과 보고서.docx
```

## 2. PhotoBlogServer (Django 4.2 + DRF 3.16)

```text
PhotoBlogServer/
├── manage.py
├── requirements.txt          # django 4.2.30, djangorestframework 3.16.1, pillow, coverage
├── .coveragerc
├── mysite/
│   ├── settings.py           # SECRET_KEY·DEBUG 환경변수, ALLOWED_HOSTS, MEDIA/STATIC
│   ├── urls.py               # admin/ + blog.urls (+ DEBUG 시 media 서빙)
│   ├── wsgi.py / asgi.py
└── blog/
    ├── models.py             # Post(author, title, text, created_date, published_date, image), MonitorConfig(ID 10 A)
    ├── serializers.py        # PostSerializer (author read_only, id 없음), MonitorConfigSerializer
    ├── views.py              # BlogImages(ModelViewSet) + 웹 목록/상세/작성/수정, MonitorConfigView, EdgeHeartbeatView
    ├── urls.py               # '', post/<pk>/, post/new/, post/<pk>/edit/, api_root/config/, api_root/config/edge/, api_root/
    ├── clip_storage.py       # ID 10 B: 클립 총용량 상한(CLIP_STORAGE_LIMIT_MB) FIFO 정리
    ├── signals.py            # ID 10 B: 게시물 삭제 시 이미지·클립 파일 삭제 (apps.ready에서 등록)
    ├── forms.py / admin.py / apps.py
    ├── tests.py              # 50개 테스트 (API·웹·모델 28 + 감시 설정 12 + 클립 10)
    ├── migrations/           # 0001_initial, 0002_monitorconfig, 0003_post_clip_post_clip_expired
    ├── templates/blog/       # base, post_list(클립 배지), post_detail(<video>), post_edit
    └── static/blog/css/
```

로컬 전용(Git 제외): `venv/`, `db.sqlite3`, `media/`, `.coverage`, `htmlcov/`, `staticfiles/`

환경변수 (이 저장소 코드 기준):

- `DJANGO_SECRET_KEY`: 없으면 개발용 키 사용, 단 `DEBUG=False`면 실행 중단
- `DJANGO_DEBUG`: 기본 `True`, `'False'`면 꺼짐

> **PythonAnywhere:** `/home/hoynola/AI-Intrusion-Detection/PhotoBlogServer`(이 저장소 clone)에서 동작. WSGI 설정 파일에서 `DJANGO_SECRET_KEY`(새 키)와 `DJANGO_DEBUG='False'`를 지정하고,
> Web 탭 Static files로 `/static/` → `staticfiles`, `/media/` → `media`를 서빙한다 (Phase 7, 2026-11-07).

## 3. PhotoViewer (Android, Java 11)

`com.example.photoviewer`, minSdk 24 / targetSdk 36

```text
PhotoViewer/app/src/main/
├── AndroidManifest.xml
├── java/com/example/photoviewer/
│   ├── ApiConfig.java           # BASE_URL 단일 관리
│   ├── MainActivity.java        # GET 동기화, RecyclerView, 검색·정렬·즐겨찾기·캐시, Worker 등록, Edge 상태 줄(ID 10 A)
│   ├── UploadActivity.java      # Photo Picker + multipart POST
│   ├── DetailActivity.java      # 게시물 상세
│   ├── SettingsActivity.java    # ID 10 A: 원격 감시 설정 화면 (PUT /api_root/config/)
│   ├── MonitorConfigApi.java    # ID 10 A: /api_root/config/ GET·PUT 공용, Edge 상태 문구
│   ├── ImageAdapter.java        # RecyclerView 어댑터
│   ├── ZoomableImageView.java   # 이미지 확대
│   ├── model/PostItem.java      # 게시물 모델, 식별키 = created_date
│   ├── database/                # Room: AppDatabase, Favorite*, PostCache*
│   └── worker/PostSyncWorker.java  # WorkManager 신규 게시물 확인 + 알림
└── res/
    ├── layout/                  # activity_main, activity_detail, activity_upload, item_image, activity_settings, item_target_chip
    └── xml/network_security_config.xml  # 10.0.2.2, 127.0.0.1만 cleartext 허용
```

`ApiConfig.BASE_URL` 하나만 바꾸면 `MainActivity`, `UploadActivity`, `PostSyncWorker`, `MonitorConfigApi`가 모두 따라간다.

| 대상 | BASE_URL |
| --- | --- |
| 에뮬레이터 → 로컬 Django | `http://10.0.2.2:8000` |
| PythonAnywhere (기본값) | `https://hoynola.pythonanywhere.com` |

> 현재 값은 Cloud다 (Phase 4~6 로컬 테스트 후 Phase 7에서 복원).

## 4. YOLOv5

upstream `ultralytics/yolov5` `e128e595e13c39faecaf2d5ccd3a14daeb2ff40a` (2026-11-01)를 clone한 뒤 `.git`과 upstream 기여자용 AI 지침(`CLAUDE.md`, `AGENTS.md`)을 제거했다. 그 외 upstream 파일은 그대로다.

```text
YOLOv5/
├── detect.py                # upstream + ChangeDetection 연동 5곳(`# added (Ch08 p.11)`) + 감시 필터 2줄(`# added (ID 10 기능 A)`)
├── changedetection.py       # 0→1 변화 감지 + 이미지 저장(320×240) + POST /api_root/Post/, 감시 OFF·재등록 대기(ID 10 A), 클립 녹화·백그라운드 업로드(ID 10 B)
├── monitorconfig.py         # ID 10 A: 5초 heartbeat 스레드, 서버 설정 보관, 클래스·신뢰도 필터
├── cliprecorder.py          # ID 10 B: 최근 3초 버퍼 + 감지 후 3초 녹화, imageio(H.264) 인코딩
├── .vscode/launch.json      # 강의자료 p.7 디버깅 설정 (upstream `.gitignore`의 `*.json`으로 Git 제외, 로컬 전용)
├── models/ utils/ data/     # upstream
├── requirements.txt         # upstream
├── .gitignore               # upstream: venv/, *.pt, runs/*, *.jpg, *.json 등 제외
├── venv/                    # 로컬 전용 (Git 제외)
├── yolov5s.pt               # 첫 실행 시 자동 다운로드, 14MB (Git 제외)
└── runs/detect/expN/        # 실행 결과 + detected/YYYY/M/D/*.jpg 전송 이미지 (Git 제외)
```

- Python 3.9.5, torch 2.2.2, numpy 1.26.4, opencv-python 4.11.0.86 (Intel Mac은 torch 2.2.2가 마지막 → `numpy<2` 필요)
- 카메라 촬영 이미지는 `runs/` 아래에 저장되어 커밋되지 않는다. 그래도 본인 외 인물이 찍히지 않도록 한다.

## 5. 문서

| 파일 | 내용 |
| --- | --- |
| `README.md` | 저장소 소개, 구조, 실행법, 제출 항목 |
| `CLAUDE.md` | Claude Code 작업 지침 |
| `docs/ASSIGNMENT.md` | 평가표 ID 1~10 |
| `docs/IMPLEMENTATION_PLAN.md` | Phase별 계획과 진행 상태 |
| `docs/YOLOV5_INTEGRATION.md` | 강의 코드를 현재 API에 맞추는 방법 |
| `docs/EXTRA_FEATURES.md` | ID 10 기능 A(원격 감시 설정)·B(이벤트 클립 녹화) 설계·구현·확인 결과 |
| `docs/ANDROID_FEATURES.md` | Android 기존 기능, ID 10 Android 변경 범위 |
| `docs/REPORT_CHECKLIST.md` | 평가 ID별 캡처·보고서 체크리스트 |
| `docs/ch08.md` | Ch08 강의자료 정리본 |
