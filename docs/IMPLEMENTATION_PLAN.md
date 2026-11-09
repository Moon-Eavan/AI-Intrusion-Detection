# 공통평가 02 — 구현 계획

Phase 단위로 진행하고, 각 Phase의 확인 항목을 실제로 수행한 뒤에만 `[x]`로 바꾼다.
공통평가 01의 완료 기록·캡처는 이번 과제의 완료로 보지 않는다.

순서 원칙: 강의 기본 흐름(ID 3~4)을 먼저 확보 → ID 10 기능(서버·YOLO 변경 포함)을 붙임 → **ID 5~9 최종 캡처는 기능 구현 후 한 번에** 찍는다.

## 현재 상태 (2026-11-10)

- `PhotoBlogServer/`, `PhotoViewer/`: 공통평가 01 코드 복사, Phase 0 베이스라인 확인 완료.
- `YOLOv5/`: upstream `e128e59` clone, `.git` 제거, venv 설치, 웹캠 기본 실행·ID 3 캡처 완료 (Phase 1). `changedetection.py` 작성·`detect.py` 5곳 수정, ID 4 캡처 완료 (Phase 2). 웹캠 → 로컬 Django 연동 확인 완료 (Phase 3, `--conf-thres 0.5`). 기능 A 구현·웹캠 증빙 완료 (Phase 4). 기능 B 구현·웹캠 증빙 완료 (Phase 5). Phase 6 로컬 최종 게시물 생성·ID 7·회귀 확인 완료. Phase 7 PythonAnywhere 배포·Cloud YOLO 게시물·Android Cloud 확인·ID 8·9 캡처 완료. Phase 8 저장소 점검·보고서 초안·ID 10 증빙 재구성 완료, 보고서 작성·push(사용자, `c408e81`). 남은 것: 보고서 완료 여부·적/부 기입 후 최종 push, URL 제출
- Git: `main` 브랜치, 최신 `c408e81 공통평가02 보고서 작성 완료` (origin과 동일). PA는 `f41162c` 그대로 — 이후 변경은 Edge 코드(`changedetection.py`)·문서·보고서뿐이라 재배포 불필요 (YOLOv5 일반 파일 141개로 추적), `origin = git@github.com:Moon-Eavan/AI-Intrusion-Detection.git`
- ID 10 기능 확정: A 원격 감시 설정(구현됨), B 이벤트 클립 녹화 + 보관 정책(구현됨) — `docs/EXTRA_FEATURES.md`
- `ApiConfig.BASE_URL` = Cloud(`https://hoynola.pythonanywhere.com`)로 복원 (Phase 7). 로컬 테스트 때만 `http://10.0.2.2:8000`
- PythonAnywhere: `/home/hoynola/AI-Intrusion-Detection/PhotoBlogServer`에서 이 저장소 코드로 동작, `DEBUG=False`, 새 `SECRET_KEY`(WSGI 파일 환경변수). 이전 `~/Mobile-Webservice`는 되돌리기용으로 남겨 둠

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

## Phase 2 — 강의 기반 검출기 코드 수정 (ID 4) ✅ (2026-11-03)

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
- [x] VS Code에서 `changedetection.py` 전체와 `detect.py` diff(변경 없는 영역 접기) 캡처 (ID 4) — 사용자 완료, 커밋 `2444fbf`

## Phase 3 — 로컬 기본 연동 확인 (ID 5·6 사전 확인) ✅ (2026-11-04)

- [x] 로컬 `runserver`(127.0.0.1:8000) + 웹캠 `python detect.py --source 0` (2026-11-04, 기본값 `--conf-thres 0.25`)
  - 결과: 약 75초 동안 POST 58건, 모두 `201`. 이미지 320×240·박스 포함 정상, 등장·재등장 시 등록 확인
  - 테스트 게시물 58건과 이미지 파일은 사용자 승인 후 삭제 (기존 16건 유지, 삭제 전 DB는 scratchpad에 백업)
  - **관측된 문제:** 배경 물체 오검출(옷장 문 → `refrigerator 0.30`, `chair`, `tv`)이 임계값 0.25 근처에서 프레임마다 깜빡여(1→0→1) 같은 클래스가 반복 등록됨. 강의 로직(직전 프레임과만 비교)의 한계. 기능 A의 신뢰도 임계값·대상 클래스·재등록 대기시간으로 보완 예정
- [x] 객체 등장 시 `201`, 계속 있을 때 중복 없음, 재등장 시 새 등록 — 3차 실행에서 충족
  - 2차 실행(2026-11-04, `--conf-thres 0.5` 의도): 약 32초에 27건. 그러나 업로드 이미지의 신뢰도가 0.26~0.43 → **옵션이 적용되지 않은 실행으로 판단**. 샘플 이미지로 옵션 동작은 확인(`zidane.jpg`: 0.25 → tie 2개, 0.5 → tie 1개). 터미널 첫 줄 `conf_thres=` 확인 후 재실행 필요
  - **3차 실행(2026-11-04): `python detect.py --source 0 --conf-thres 0.5`** — Claude가 백그라운드로 실행(터미널 카메라 권한 공유), 사용자는 카메라 앞 동작만 수행. 시작 로그 `conf_thres=0.5` 확인
  - 결과: 약 359프레임(약 50초) 동안 POST 4건, 모두 `201`, 전송 실패 0 → 시나리오와 정확히 일치
    `person`(최초 등장) → `person`(퇴장 후 재등장) → `cell phone`(휴대폰 제시, `cell phone 0.72`) → `person`(재등장). 머무는 동안 중복 없음, 옷장 오검출 사라짐
  - **이후 웹캠 실행은 `--conf-thres 0.5`를 표준으로 사용**한다 (YOLOv5 기본 옵션, 코드 변경 없음)
- [x] 서버를 끈 상태에서도 검출 루프가 멈추지 않음 (Phase 2 샘플 이미지 테스트에서 확인: `전송 실패` 출력 후 계속)
- [ ] (선택) 백업용으로 `api_root`, 목록 화면 캡처 — 현재 로컬 DB에 3차 실행 게시물 4건 (최종 캡처는 Phase 6)

## Phase 4 — 기능 A: 원격 감시 설정 (ID 10) ✅ (2026-11-07)

`docs/EXTRA_FEATURES.md` 기능 A (구현 내용·설계 변경·확인 결과 상세).

- [x] Django: `MonitorConfig` 모델·migration `0002`, `GET/PUT/PATCH /api_root/config/`, `POST /api_root/config/edge/`(응답 = 설정), 테스트 12개 추가 → **40개 OK**. 로컬 DB에 migration 적용(백업: scratchpad `db_backup_before_phase4.sqlite3`), 실행 중인 서버로 API 응답 확인
- [x] YOLOv5: `monitorconfig.py`(5초 heartbeat 스레드), `detect.py` 필터 2줄, `changedetection.py` OFF·재등록 대기. 샘플 이미지 7개 시나리오 + 실행 중 설정 변경(5.1초 후 반영) 확인 (임시 서버 복사본, 포트 8765)
- [x] Android: `SettingsActivity`, `MonitorConfigApi`, `MainActivity` Edge 상태 줄. `assembleDebug` 성공, 에뮬레이터에서 로드·저장(PUT)·온라인 표시 확인
- [x] 웹캠 증빙 (2026-11-07, 로컬 서버 + 에뮬레이터, `detect.py --source 0` 임계값 옵션 없음). Claude가 detect.py·앱 조작, 사용자는 카메라 앞 동작
  - 시작 직후(대상 전체, 0.5) 약 30초에 18건 — chair·toilet·cell phone 등 깜빡임 (이 환경에서는 0.5로도 부족 → 대상·대기시간 필요성)
  - 1 대상 `person`만: 5건 모두 `person`, `cell phone` 0건, 사진에 사람 박스만
  - 2 `cell phone` 추가: `cell phone` 등록됨(사진 `cell phone 0.71`). 단 휴대폰을 움직이자 6초에 9건
  - 2-1 재등록 대기 30초(추가 시나리오): 같은 동작에 `cell phone` 1건, 7회 `재등록 대기 중 - cell phone 미등록`
  - 3 감시 OFF: 0건, `감시 OFF - person/cell phone 미등록`, 앱 상단 "감시 OFF"
  - 4 YOLO 종료: 약 20초 후 앱 "Edge 오프라인 · 20초 전"
  - 에뮬레이터 quickboot 스냅샷에 예전 앱이 남아 있어 APK 재설치 필요했음(`-no-snapshot-save`로 설치가 저장 안 됨)
- [x] 증빙 파일 모음: `../AI-Intrusion-Detection-docs/captures/공통평가02/` (앱 화면 5장 10-3, YOLO 콘솔 로그 10-2). 10-1 브라우저 캡처는 사용자
- 증빙 후 서버 설정을 기본값(ON, 전체, 0.5, 대기 0)으로 복원
- 시작 직후 오검출 게시물 18건 삭제(사용자 요청, 이미지 파일 포함, 백업 scratchpad `db_backup_before_delete_phase4.sqlite3`) → 로컬 DB 36건 (기존 16 + Phase 3 4건 + Phase 4 시나리오 16건)
- [x] 10-1 `/api_root/config/` 브라우저 캡처 (사용자, 상단 설정 필드 부분)

## Phase 5 — 기능 B: 이벤트 클립 녹화 + 보관 정책 (ID 10) ✅ (2026-11-07)

`docs/EXTRA_FEATURES.md` 기능 B (구현 내용·확인 결과 상세).

- [x] Django: `clip`, `clip_expired` 필드·migration `0003`, serializer, 파일 정리 시그널(`signals.py`), 용량 기준 FIFO(`clip_storage.py`, `CLIP_STORAGE_LIMIT_MB` 기본 200), 웹 목록 배지·상세 `<video>`, 테스트 10개 추가 → **50개 OK**. 로컬 DB migration 적용(백업: scratchpad `db_backup_before_phase5.sqlite3`)
- [x] YOLOv5: `pip install imageio imageio-ffmpeg`(2.37.2 / 0.6.0, numpy 1.26.4 유지), `cliprecorder.py`(3초 버퍼 + 감지 후 3초), `changedetection.py` 백그라운드 인코딩·업로드. `detect.py` 수정 없음
  - 테스트 영상(임시 서버 8765): 클립 2개(약 6초, 6fps, H.264 yuv420p 320×240, faststart), 녹화 중 `tie` 병합, 클립 첫 3초 = 감지 전 장면 확인
- [x] Android: `PostItem.setClip`, 목록 "▶ 클립"/"클립 만료" 배지, `DetailActivity` `VideoView` 반복 재생·만료 문구. 빌드·에뮬레이터 재생(00:06) 확인
- [x] 파일 정리: 테스트 게시물 삭제 시 이미지·클립 파일 삭제 확인
- [x] 웹캠 증빙 (서버 `CLIP_STORAGE_LIMIT_MB=0.3`으로 임시 실행, 대상 person·cell phone, 대기 10초): 이벤트 6건 모두 클립 업로드(22~127KB), 녹화 중 `cell phone` 병합 2회, 4번째 클립부터 상한 초과 → 오래된 3건 `clip_expired`(게시물·사진 유지), 남은 클립 3개 232KB. 앱 목록 배지·상세 재생·만료 화면 확인
  - 시작 직후 첫 이벤트는 버퍼가 비어 감지 전 구간 없이 3.1초 클립
  - 앱 동기화가 한 번 `Failed to connect`(일시적)로 오프라인 캐시 표시 → 재시도 시 정상
- [x] 증빙 후 서버를 기본 상한(200MB)으로 재시작, 감시 설정 기본값 복원
- [x] 증빙 파일: `../AI-Intrusion-Detection-docs/captures/공통평가02/` (YOLO 로그 10-4, API 응답 10-5, 앱 화면 4장 10-6). 웹 캡처 10-5는 사용자
- [x] (Phase 5 커밋 후, 사용자 요청) 게시물 `text` 끝 쉼표 제거: `"person, "` → `"person"`, 녹화 중 병합은 `", "`로 연결. 테스트 영상으로 `'person, bus, tie'`, `'person, bus'` 확인
- [x] (2026-11-10, 사용자 요청) 녹화 중 병합 시 이미 이벤트에 있는 클래스는 다시 붙이지 않음: 웹캠 ON/OFF 증빙 중 사람이 녹화 도중 깜빡여 `"person, person"`이 저장된 것을 발견 → `changedetection.py` 수정. 카메라 없이 시뮬레이션(person 깜빡임·cell phone 깜빡임)으로 `'person, cell phone'` 확인. 기존 로컬 게시물 1건(`person, person`)은 그대로

## Phase 6 — 로컬 최종 캡처 (ID 5·6·7) ✅ (2026-11-07)

- [x] YOLO 실행해 새 검출 게시물 생성 (웹캠, 대상 person·cell phone, 대기 10초): 8건(`person` 7, `cell phone` 1), 모두 클립 포함, `text` 끝 쉼표 없는 새 형식. 로컬 DB 50건. 이후 감시 설정 기본값 복원
- [x] `http://127.0.0.1:8000/api_root/Post/` 캡처 (ID 5) — 사용자 완료 (목록 끝부분에 2026-11-07 06:58~06:59 UTC 게시물)
- [x] `http://127.0.0.1:8000/` 목록·이미지 캡처 (ID 6) — 사용자 완료 (맨 위 8건)
- [x] Android(`BASE_URL = http://10.0.2.2:8000`, Phase 5 빌드) 동기화 → **최신순** → YOLO 게시물·클립 배지 캡처 (ID 7) — `../AI-Intrusion-Detection-docs/captures/공통평가02/`
- [x] 기존 Android 기능 회귀 확인 (에뮬레이터, adb 조작)
  - 동기화 50건 / 검색 "cell" → 14건 / 오래된순(2026-10-08 글)·최신순(오늘 YOLO 글) / 즐겨찾기 등록 → 필터 1개 → 해제
  - 업로드: Photo Picker로 사진 선택 + 제목·내용 → 서버에 이미지 포함 등록, 자동 재동기화 51건 → 테스트 게시물 삭제(파일도 삭제)
  - 백그라운드 테스트 버튼 → 알림 "새 게시물 8개가 있습니다."
  - 오프라인 캐시: Phase 5에서 일시 연결 실패 시 캐시 37건 표시 확인
  - 조작 중 키보드가 닫힌 상태에서 BACK을 보내 앱이 한 번 종료됨(테스트 조작 실수, 앱 문제 아님)

## Phase 7 — PythonAnywhere (ID 8·9) ✅ (2026-11-07)

배포 전 Cloud는 공통평가 01 코드(`~/Mobile-Webservice`, SECRET_KEY 하드코딩·`DEBUG = True`, 게시물 9건, `/api_root/config/` 404)였다.
배포 방식: 새 저장소를 PA에 clone하고 웹앱 경로를 옮김 (기존 폴더는 되돌리기용으로 유지).

```bash
# PA Bash 콘솔 (사용자 실행)
cd ~ && git clone https://github.com/Moon-Eavan/AI-Intrusion-Detection.git
cp -a ~/Mobile-Webservice/PhotoBlogServer/db.sqlite3 ~/AI-Intrusion-Detection/PhotoBlogServer/
cp -a ~/Mobile-Webservice/PhotoBlogServer/media ~/AI-Intrusion-Detection/PhotoBlogServer/
workon photoblog-venv          # Python 3.10, 패키지 버전 requirements.txt와 동일
cd ~/AI-Intrusion-Detection/PhotoBlogServer
pip install -r requirements.txt && python manage.py migrate && python manage.py collectstatic --noinput
# 이후 코드 갱신: cd ~/AI-Intrusion-Detection && git pull → (필요 시 migrate·collectstatic) → Web 탭 Reload
```

- [x] 새 코드 clone, 기존 `db.sqlite3`·`media/` 복사, `migrate`(0002·0003 OK), `collectstatic`(162개), 게시물 9건 유지
- [x] 새 `SECRET_KEY` 생성(PA 콘솔) → WSGI 파일 `os.environ['DJANGO_SECRET_KEY']`, `os.environ['DJANGO_DEBUG'] = 'False'` (키 값은 어디에도 기록하지 않음). 이전 키는 폐기
- [x] WSGI `path`, Web 탭 Source code·Working directory, Static files `/static/`·`/media/` → `/home/hoynola/AI-Intrusion-Detection/PhotoBlogServer/...`, virtualenv `photoblog-venv` 유지, Reload
- [x] 외부 확인: `/`, `/api_root/`, `/api_root/Post/`(필드에 `clip`, `clip_expired`), `/api_root/config/`, `/admin/login/` 200, static CSS 200, 기존 이미지 8개 200, 없는 주소 404(디버그 화면 아님 → DEBUG 꺼짐)
- [x] `PHOTOBLOG_HOST=https://hoynola.pythonanywhere.com`으로 YOLO 실행(Cloud 설정 person·cell phone, 대기 10초) → 6건(`person` 4, `cell phone` 2) 클립 포함 등록, Cloud 게시물 15건. 종료 직전 녹화 중이던 1건은 업로드 전 종료로 유실
  - Cloud 클립 `video/mp4` 200. PA static 서버는 Range 요청에 206이 아닌 200 전체 응답 → faststart로 Chrome·Android 재생 가능, Safari는 재생 안 될 수 있음
- [x] Android `BASE_URL` Cloud로 복원·재빌드 → 동기화 15건, 클립 배지, 상세 클립 재생(00:05/00:06), 감시 설정 화면(대상 2개·대기 10 로드), Edge 상태 표시 확인. 이후 Cloud 감시 설정 기본값 복원
- [x] 증빙 파일: `../AI-Intrusion-Detection-docs/captures/공통평가02/` (Cloud 앱 목록·클립 재생·감시 설정)
- [x] Cloud `api_root` 캡처 (ID 8), 웹 목록·이미지 캡처 (ID 9) — 사용자 완료
- [x] 커밋 `f41162c` push 후 PA에서 `git pull` + Reload (사용자) → 외부 재확인: 페이지·API·설정 API·CSS 200, 클립 `video/mp4` 200, 게시물 15건, 404 정상
- [ ] (선택) Cloud `media/` 사용량: PA 콘솔에서 `du -sh ~/AI-Intrusion-Detection/PhotoBlogServer/media` (이번 클립 6개 약 0.5MB, 상한 200MB)

## Phase 8 — 제출 (ID 1·2)

- [x] 루트에 `PhotoBlogServer/`, `PhotoViewer/`, `YOLOv5/`, 보고서 `.docx` 확인 (2026-11-07, 그 외 `docs/`, `tools/`, `README.md`, `CLAUDE.md`, `.gitignore`)
- [x] 폴더 구조 캡처 (ID 1) — 사용자 (Finder)
- [x] 캡처 정리: 사용자 캡처 12장 내용 확인 후 Claude 저장분과 함께 `../AI-Intrusion-Detection-docs/captures/공통평가02/`에 보고서 행 번호 이름(`ID05_…`, `ID10-3_2_…`)으로 이동·이름 변경, 보조 자료는 `참고/`, 빈 `phase4~7/` 폴더 삭제. `ID06`과 `ID10-3_1`은 같은 화면(웹 목록·클립 배지)
- [x] 추적 파일·전체 히스토리 점검: `db.sqlite3`·`media/`·`runs/`·`*.pt`·`venv/`·`.env`·`*.mp4`·`local.properties` 없음, 실제 비밀키 없음(히스토리의 `password = 'password'`는 `docs/ch08.md` 강의 예제 원문, 테스트용 `unused-pw`, upstream YOLOv5 CI의 `secrets.*` 참조뿐). 추적 이미지는 upstream 샘플 `bus.jpg`·`zidane.jpg`만. 259개 파일
- [x] GitHub 확인 (API): 루트 구조 일치, `YOLOv5/`는 일반 폴더(tree, gitlink 아님)이고 `detect.py`·`changedetection.py`·`monitorconfig.py`·`cliprecorder.py` 포함
- [x] 보고서 양식 확인: 표는 ID 1~10 + 빈 행 4개, 아직 미작성 → ID 10을 기능 × 서버/클라이언트 4행으로 쓰기로 정리, 초안 `docs/REPORT_DRAFT.md` 작성
- [x] ID 10 증빙 재구성 (2026-11-09~10, 사용자 요청): 10-2 사진 1을 웹캠 감시 ON→OFF→ON 로그(터미널 캡처)로 교체, 10-3을 서버 테스트 결과·API 만료 화면·웹 재생 3장으로 교체, 10-4 앱 목록을 클립·만료 한 화면으로 합침. 각 행 3장(10-1은 1장), 이전 사진은 `참고/`
- [x] Word 보고서 작성(학번·이름, 캡처 내장, ID 10 4행 정의·설명·관련증빙) — 사용자. Claude 점검(2026-11-10): 행별 사진 19장 순서·내용 일치, 원본 해상도로 재삽입 확인
- [x] 보고서 커밋·push — 사용자 (`99230ef`, `c408e81`)
- [ ] 보고서 마무리 — 보고서 저장본(11-10 00:41, `c408e81`)은 완료 여부·적/부 칸, ID 10 상위 행 증빙, 10-1 사진 설명의 "Edge 접속 상태" 수정이 아직 반영 안 됨 → 사용자가 Word에서 고쳐 저장 후 다시 커밋·push
- [ ] 최종본 push 후 GitHub에서 재확인
- [ ] GitHub URL 제출
