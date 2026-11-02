# 프로젝트 구조

`YOLOv5/`는 아직 없다(Phase 1에서 생성). 그 외 트리는 현재 저장소 기준이다.

## 1. 루트

```text
AI-Intrusion-Detection/
├── PhotoBlogServer/        # Django Server (공통평가 01 재사용)
├── PhotoViewer/            # Android Client (공통평가 01 재사용)
├── YOLOv5/                 # Edge Client (생성 예정)
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
    ├── models.py             # Post(author, title, text, created_date, published_date, image)
    ├── serializers.py        # PostSerializer (author read_only, id 없음)
    ├── views.py              # BlogImages(ModelViewSet) + 웹 목록/상세/작성/수정
    ├── urls.py               # '', post/<pk>/, post/new/, post/<pk>/edit/, api_root/
    ├── forms.py / admin.py / apps.py
    ├── tests.py              # 28개 테스트 (API·웹·모델)
    ├── migrations/
    ├── templates/blog/       # base, post_list, post_detail, post_edit
    └── static/blog/css/
```

로컬 전용(Git 제외): `venv/`, `db.sqlite3`, `media/`, `.coverage`, `htmlcov/`, `staticfiles/`

환경변수:

- `DJANGO_SECRET_KEY`: 없으면 개발용 키 사용, 단 `DEBUG=False`면 실행 중단
- `DJANGO_DEBUG`: 기본 `True`, `'False'`면 꺼짐

## 3. PhotoViewer (Android, Java 11)

`com.example.photoviewer`, minSdk 24 / targetSdk 36

```text
PhotoViewer/app/src/main/
├── AndroidManifest.xml
├── java/com/example/photoviewer/
│   ├── ApiConfig.java           # BASE_URL 단일 관리
│   ├── MainActivity.java        # GET 동기화, RecyclerView, 검색·정렬·즐겨찾기·캐시, Worker 등록
│   ├── UploadActivity.java      # Photo Picker + multipart POST
│   ├── DetailActivity.java      # 게시물 상세
│   ├── ImageAdapter.java        # RecyclerView 어댑터
│   ├── ZoomableImageView.java   # 이미지 확대
│   ├── model/PostItem.java      # 게시물 모델, 식별키 = created_date
│   ├── database/                # Room: AppDatabase, Favorite*, PostCache*
│   └── worker/PostSyncWorker.java  # WorkManager 신규 게시물 확인 + 알림
└── res/
    ├── layout/                  # activity_main, activity_detail, activity_upload, item_image
    └── xml/network_security_config.xml  # 10.0.2.2, 127.0.0.1만 cleartext 허용
```

`ApiConfig.BASE_URL` 하나만 바꾸면 `MainActivity`, `UploadActivity`, `PostSyncWorker`가 모두 따라간다.

| 대상 | BASE_URL |
| --- | --- |
| 에뮬레이터 → 로컬 Django | `http://10.0.2.2:8000` |
| PythonAnywhere (현재 값) | `https://hoynola.pythonanywhere.com` |

## 4. YOLOv5 (생성 예정)

```text
YOLOv5/                      # git clone https://github.com/ultralytics/yolov5 YOLOv5
├── detect.py                # upstream + ChangeDetection 연동 5곳 추가
├── changedetection.py       # 신규: 0→1 변화 감지 + 이미지 저장 + POST
├── .vscode/launch.json      # (선택) 강의자료 p.7
├── models/ utils/ data/     # upstream
├── requirements.txt         # upstream
└── runs/detect/expN/detected/YYYY/M/D/*.jpg   # 실행 시 생성 (Git 제외)
```

Git 관련 주의:

- clone하면 `YOLOv5/.git`이 생긴다. 그대로 두고 루트에서 `git add`하면 일반 폴더가 아니라 gitlink로 기록되어 GitHub에서 내용이 보이지 않는다. 처리 방식은 사용자 확인 후 결정한다.
- 루트 `.gitignore`에 `YOLOv5/runs/`(카메라 촬영 이미지), `*.pt`(자동 다운로드되는 `yolov5s.pt` 등) 추가를 검토한다. `venv/`는 이미 제외되어 있다.
- 카메라 촬영 이미지에 본인 외 인물이 찍히지 않도록 한다.

## 5. 문서

| 파일 | 내용 |
| --- | --- |
| `README.md` | 저장소 소개, 구조, 실행법, 제출 항목 |
| `CLAUDE.md` | Claude Code 작업 지침 |
| `docs/ASSIGNMENT.md` | 평가표 ID 1~10 |
| `docs/IMPLEMENTATION_PLAN.md` | Phase별 계획과 진행 상태 |
| `docs/YOLOV5_INTEGRATION.md` | 강의 코드를 현재 API에 맞추는 방법 |
| `docs/ANDROID_FEATURES.md` | Android 기존 기능, ID 10 편의 기능 |
| `docs/REPORT_CHECKLIST.md` | 평가 ID별 캡처·보고서 체크리스트 |
| `docs/ch08.md` | Ch08 강의자료 정리본 |
