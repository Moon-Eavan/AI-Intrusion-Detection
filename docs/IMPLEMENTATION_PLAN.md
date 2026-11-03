# 공통평가 02 — 구현 계획

Phase 단위로 진행하고, 각 Phase의 확인 항목을 실제로 수행한 뒤에만 `[x]`로 바꾼다.
공통평가 01의 완료 기록·캡처는 이번 과제의 완료로 보지 않는다.

순서 원칙: 강의 기본 흐름(ID 3~4)을 먼저 확보 → ID 10 기능(서버·YOLO 변경 포함)을 붙임 → **ID 5~9 최종 캡처는 기능 구현 후 한 번에** 찍는다.

## 현재 상태 (2026-11-03)

- `PhotoBlogServer/`, `PhotoViewer/`: 공통평가 01 코드 복사, Phase 0 베이스라인 확인 완료.
- `YOLOv5/`: upstream `e128e59` clone, `.git` 제거, venv 설치, 웹캠 기본 실행·ID 3 캡처 완료 (Phase 1). `changedetection.py` 작성·`detect.py` 5곳 수정 완료 (Phase 2 — ID 4 캡처는 사용자 대기)
- Git: `main` 브랜치, 최신 `fa26e59 feat: set up YOLO and test webcam detection` (YOLOv5 일반 파일 141개로 추적), `origin = git@github.com:Moon-Eavan/AI-Intrusion-Detection.git`
- ID 10 기능 확정: A 원격 감시 설정, B 이벤트 클립 녹화 + 보관 정책 (`docs/EXTRA_FEATURES.md`, 설계 단계)

## Phase 0 — 기존 코드 베이스라인 확인 ✅ (2026-11-02)

- [x] Django: `python manage.py test blog` — 28 tests OK (Python 3.9.5, `PhotoBlogServer/venv`)
- [x] Django: `runserver` 후 `http://127.0.0.1:8000/` 200, `/api_root/`·`/api_root/Post/` 200 (로컬 DB 게시물 16건, 응답 필드 6개, `id` 없음)
  - 공통평가 01 게시물 16건은 **삭제하지 않고 유지**(사용자 결정). 웹 목록이 최신순이라 YOLO 게시물이 위에 표시된다
- [x] `http://localhost:8000/` → 400 `DisallowedHost` 확인 (브라우저는 `127.0.0.1` 사용)
- [x] Android: `./gradlew assembleDebug` BUILD SUCCESSFUL (Android Studio JBR, Gradle 8.13)
- [x] Android 에뮬레이터(`Medium_Phone_API_36.0`, Android 16) 설치·실행, `BASE_URL` = Cloud 상태에서 동기화 → 게시물 9개·이미지 표시 확인
  - 정렬 미선택 시 목록은 서버 응답 순서(오래된 것부터) → ID 7 캡처 때는 **최신순**을 눌러 YOLO 게시물을 위로 올린다
- [x] `.gitignore`: `venv/`, `db.sqlite3`, `media/`, `.coverage`, `htmlcov/`, Android `build/`·`.gradle/`·`local.properties`·`.idea/`, `.DS_Store`, `__pycache__/` 모두 제외됨, 추적 파일에 해당 항목 없음

## Phase 1 — YOLOv5 설치 및 무수정 실행 (ID 3) ✅ (2026-11-03)

```bash
# 저장소 루트에서 (실제 수행한 명령)
git clone --depth 1 https://github.com/ultralytics/yolov5 YOLOv5
rm -rf YOLOv5/.git              # 사용자 승인. 루트 저장소에서 일반 폴더로 추적되게 함
cd YOLOv5
python3.9 -m venv venv          # 강의자료: Python 3.9에서 검증됨
venv/bin/pip install -r requirements.txt -c <(echo "numpy<2")   # Intel Mac: torch 2.2.2는 numpy 2와 비호환
python detect.py --source 0     # ← ID 3 (사용자)
```

- [x] clone: upstream `e128e595e13c39faecaf2d5ccd3a14daeb2ff40a` (2026-11-01). `YOLOv5/.git` 제거 → 루트 Git에서 일반 파일 143개(2.2MB)로 인식
- [x] venv: Python 3.9.5 (Intel Mac x86_64), torch 2.2.2 / torchvision 0.17.2 / numpy 1.26.4 / opencv-python 4.11.0.86 / ultralytics 8.4.199
- [x] 설치 검증: 원본 `detect.py --source data/images/bus.jpg` → `4 persons, 1 bus`, CPU 추론 약 177ms/프레임(약 5 FPS), `yolov5s.pt` 자동 다운로드
- [x] Git 제외 확인: upstream `YOLOv5/.gitignore`가 `venv/`, `*.pt`, `runs/*`, `*.jpg`(샘플 제외)를 이미 제외 → 루트 `.gitignore` 추가 불필요
- [x] `detect.py` 연동 위치 확인 (`docs/YOLOV5_INTEGRATION.md` §4에 이 버전 줄 번호 기록)
- [x] macOS 카메라 권한(터미널) 허용
  - 2026-11-03 첫 실행: `OpenCV: not authorized to capture video (status 0)` → `AssertionError: Failed to open 0`. 권한 미허용 상태의 정상 반응
  - 해결: 시스템 설정 → 개인정보 보호 및 보안 → 카메라에서 실행한 터미널 앱 켜기 → **터미널 앱 완전 종료 후 재실행** → 다시 `python detect.py --source 0`
- [x] **수정 전 원본 `detect.py`** 를 `--source 0`으로 실행, 바운딩 박스·클래스명 표시 확인 (upstream `e128e59`의 `detect.py`와 바이트 단위 동일함을 비교 확인)
- [x] 실행 명령과 인식 화면 캡처 (ID 3) — 사용자 완료
  - 웹캠 실행 결과 영상 `runs/detect/exp3·exp4/0.mp4`(얼굴 포함)는 `runs/*`로 Git 제외 확인
- [x] upstream `YOLOv5/CLAUDE.md`(→ `AGENTS.md` 링크)·`AGENTS.md` 삭제 (사용자 결정, 과제 지침과 충돌하는 Ultralytics 기여자용 지침)

## Phase 2 — 강의 기반 검출기 코드 수정 (ID 4) (2026-11-03, 코드 완료 / 캡처 대기)

`docs/YOLOV5_INTEGRATION.md` §3·§4 기준. 이 단계는 **강의자료 범위만** 구현했다.

- [x] `YOLOv5/changedetection.py` 작성 — 강의 p.8–10 구조·변수명 유지, 바꾼 줄은 `# 수정:` 주석
  - 토큰 인증 제거, `HOST = PHOTOBLOG_HOST` 환경변수(기본 `http://127.0.0.1:8000`), `created_date` 미전송·`published_date`는 시간대 포함, `cv2.imwrite(str(...))`, `with open`, `timeout=5`, 전송 실패 시 출력 후 계속
- [x] `YOLOv5/detect.py`에 5곳 추가 (p.11, 각 줄 `# added (Ch08 p.11)`) — `git diff YOLOv5/detect.py`로 확인 가능 (+7줄)
- [x] `YOLOv5/.vscode/launch.json` 작성 (p.7, `"type": "debugpy"`). upstream `.gitignore`의 `*.json` 규칙으로 **Git 제외(로컬 전용)**
- [x] 테스트 (실제 로컬 DB를 건드리지 않도록 `PhotoBlogServer` 복사본을 scratchpad에서 8765 포트로 실행)
  - 변화 판정 7단계(없음 / 등장 / 유지 / 추가 / 사라짐 / 재등장 / 2개 동시 등장) 모두 기대대로, 전송마다 `201`
  - 원본 샘플 `detect.py --source data/images`: `bus.jpg` → `title: bus, text: person, bus,` / `zidane.jpg` → `person` 유지라 `tie`만 등록. 업로드 이미지 320×240, 박스 포함, 이미지 URL 200
  - 서버 꺼짐: `ChangeDetection: 전송 실패 ...` 출력 후 검출 계속, 정상 종료
  - `published_date` 시간대 정상 (KST 18:29 → UTC 09:29)
- [ ] VS Code에서 `changedetection.py`와 `detect.py` 추가 코드 캡처 (ID 4) — 사용자

## Phase 3 — 로컬 기본 연동 확인 (ID 5·6 사전 확인)

- [ ] 로컬 `runserver` → `PHOTOBLOG_HOST=http://127.0.0.1:8000 python detect.py --source 0`
- [ ] 객체 등장 시 `201`, 계속 있을 때 중복 없음, 재등장 시 새 등록
- [ ] 서버를 끈 상태에서도 검출 루프가 멈추지 않음
- [ ] 백업용으로 `api_root`, 목록 화면 캡처 (최종 캡처는 Phase 6)

## Phase 4 — 기능 A: 원격 감시 설정 (ID 10)

`docs/EXTRA_FEATURES.md` 기능 A.

- [ ] Django: `MonitorConfig` 모델·migration, `GET/PATCH /api_root/config/`, `POST /api_root/config/edge/`, 테스트
- [ ] YOLOv5: 설정 polling 스레드, 클래스·신뢰도 필터, `enabled`, 쿨다운
- [ ] Android: `SettingsActivity`, Edge 상태 표시
- [ ] 증빙 시나리오 1~6 확인·캡처

## Phase 5 — 기능 B: 이벤트 클립 녹화 + 보관 정책 (ID 10)

`docs/EXTRA_FEATURES.md` 기능 B.

- [ ] Django: `clip`, `clip_expired` 필드·migration, serializer, 파일 정리 시그널, 용량 기준 FIFO, 웹 `<video>`, 테스트
- [ ] YOLOv5: `pip install imageio imageio-ffmpeg`, ring buffer, 전후 3초 수집, 백그라운드 인코딩·업로드
- [ ] Android: `PostItem` 파싱, 목록 클립 표시, `DetailActivity` 재생 (Room 캐시 스키마는 유지)
- [ ] 증빙 시나리오 1~4 확인·캡처

## Phase 6 — 로컬 최종 캡처 (ID 5·6·7)

- [ ] YOLO 실행해 새 검출 게시물 생성
- [ ] `http://127.0.0.1:8000/api_root/Post/` 캡처 (ID 5)
- [ ] `http://127.0.0.1:8000/` 목록·이미지 캡처 (ID 6)
- [ ] Android(`BASE_URL = http://10.0.2.2:8000`으로 바꿔 재빌드) 동기화 → **최신순** 선택 후 YOLO 게시물 캡처 (ID 7)
- [ ] 기존 Android 기능 회귀 확인 (업로드, 검색·정렬·즐겨찾기·캐시·알림)

## Phase 7 — PythonAnywhere (ID 8·9)

현재 Cloud는 이전 `settings.py`(SECRET_KEY 하드코딩, `DEBUG = True`)로 동작 중이다. 새 코드를 올리면 settings도 환경변수 방식으로 바뀌므로 아래를 함께 처리한다.

- [ ] PythonAnywhere WSGI 설정 파일에 `os.environ['DJANGO_SECRET_KEY']` 지정. 이전 키가 공통평가 01 저장소 기록에 남아 있다면 **새 키로 교체** 권장 (키 값은 Git·문서·로그에 남기지 않음)
- [ ] `DJANGO_DEBUG` 결정. `False`로 끄면 Django가 `/media/`를 서빙하지 않으므로(`mysite/urls.py`의 DEBUG 조건) **Web 탭 Static files에 `/media/` → `.../PhotoBlogServer/media` 매핑**이 있어야 이미지·클립이 보인다 (ID 9 영향)
- [ ] 변경된 Django 코드 배포 → `python manage.py migrate` → Web 탭 Reload
- [ ] 배포 후 기존 게시물 이미지가 여전히 열리는지 먼저 확인
- [ ] `PHOTOBLOG_HOST=https://hoynola.pythonanywhere.com`으로 YOLO 실행해 Cloud에 검출 게시물(클립 포함) 등록
- [ ] Cloud `api_root` 캡처 (ID 8), 웹 목록·이미지 캡처 (ID 9)
- [ ] Android `BASE_URL`을 Cloud로 두고 동기화·클립 재생·설정 변경 확인
- [ ] Cloud `media/` 사용량 확인

## Phase 8 — 제출 (ID 1·2)

- [ ] 루트에 `PhotoBlogServer/`, `PhotoViewer/`, `YOLOv5/`, 보고서 `.docx` 확인 → 폴더 구조 캡처 (ID 1)
- [ ] 비밀값·개인 사진·`db.sqlite3`·`media/`·`runs/`·모델 가중치가 추적되지 않는지 `git status`로 확인
- [ ] 커밋 및 `origin`에 push, GitHub 웹에서 구조 확인 (ID 2)
- [ ] Word 보고서 작성(학번·이름, 완료 여부, 적/부, 캡처 내장, ID 10 행 구성) 후 다시 커밋·push
- [ ] GitHub URL 제출
