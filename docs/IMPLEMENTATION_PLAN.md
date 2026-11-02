# 공통평가 02 — 구현 계획

Phase 단위로 진행하고, 각 Phase의 확인 항목을 실제로 수행한 뒤에만 `[x]`로 바꾼다.
공통평가 01의 완료 기록·캡처는 이번 과제의 완료로 보지 않는다.

## 현재 상태 (2026-11-02)

- `PhotoBlogServer/`, `PhotoViewer/`: 공통평가 01 코드 복사 완료. 이 저장소 기준 재검증 전.
- `YOLOv5/`: 미생성.
- Git: `main` 브랜치, 커밋 없음, `origin = git@github.com:Moon-Eavan/AI-Intrusion-Detection.git`
- 문서: 공통평가 02 기준으로 재작성 완료.

## Phase 0 — 기존 코드 베이스라인 확인

- [ ] Django: `cd PhotoBlogServer && source venv/bin/activate && python manage.py test blog` 통과
- [ ] Django: `runserver` 후 `http://127.0.0.1:8000/`, `/api_root/Post/` 응답
- [ ] Android: Android Studio에서 `PhotoViewer/` 빌드 성공
- [ ] `.gitignore`가 `venv/`, `db.sqlite3`, `media/`, `.coverage`, `htmlcov/`, 빌드 산출물을 제외하는지 확인

## Phase 1 — YOLOv5 설치 및 무수정 실행 (ID 3)

```bash
# 저장소 루트에서
git clone https://github.com/ultralytics/yolov5 YOLOv5
cd YOLOv5
python3.9 -m venv venv          # 강의자료: Python 3.9에서 검증됨
source venv/bin/activate
pip install -r requirements.txt
python detect.py --source 0
```

- [ ] macOS 카메라 권한(터미널/VS Code) 허용
- [ ] **수정 전 원본 `detect.py`** 실행, 바운딩 박스·클래스명 표시 확인
- [ ] 실행 명령과 인식 화면 캡처 (ID 3)
- [ ] 사용한 YOLOv5 커밋(`git -C YOLOv5 rev-parse --short HEAD`)과 Python 버전 기록
- [ ] `YOLOv5/.git` 처리 결정 (그대로 두면 루트 저장소에서 서브모듈/gitlink로 잡혀 파일이 올라가지 않음) — 사용자 확인 후 처리
- [ ] 루트 `.gitignore`에 `YOLOv5/runs/`, `*.pt` 추가 검토 (`venv/`는 이미 제외됨)

## Phase 2 — 검출기 코드 수정 (ID 4)

`docs/YOLOV5_INTEGRATION.md` §3·§4 기준.

- [ ] `YOLOv5/changedetection.py` 작성 (강의자료 p.8–10 + 현재 API에 맞춘 수정)
- [ ] `YOLOv5/detect.py`에 5곳 추가 (p.11)
- [ ] (선택) `YOLOv5/.vscode/launch.json` 작성 (p.7)
- [ ] 서버 없이 실행해도 검출 루프가 멈추지 않는지 확인
- [ ] VS Code에서 `changedetection.py`와 `detect.py` 추가 코드 캡처 (ID 4)

## Phase 3 — 로컬 Django 등록 (ID 5·6)

- [ ] 로컬 `runserver` 실행 → `PHOTOBLOG_HOST=http://127.0.0.1:8000 python detect.py --source 0`
- [ ] 객체 등장 시 `201` 응답, 계속 있을 때 중복 등록 없음, 재등장 시 새 등록 확인
- [ ] `http://127.0.0.1:8000/api_root/Post/` 캡처 (ID 5)
- [ ] `http://127.0.0.1:8000/` 목록·이미지 캡처 (ID 6)

## Phase 4 — Android에서 확인 (ID 7)

- [ ] 로컬 서버 대상이면 `ApiConfig.BASE_URL = "http://10.0.2.2:8000"` (Cloud 대상이면 그대로)
- [ ] 동기화 후 YOLO가 등록한 게시물·이미지 표시 캡처 (ID 7)
- [ ] 기존 업로드·상세 보기 등 동작에 이상 없는지 확인
- [ ] 테스트 후 `BASE_URL`을 어떤 값으로 둘지 정리 (커밋 시 Cloud 주소 권장)

## Phase 5 — PythonAnywhere (ID 8·9)

- [ ] `https://hoynola.pythonanywhere.com/api_root/Post/` 접속 확인 (Django 코드를 바꿨다면 먼저 재배포 + Reload)
- [ ] `PHOTOBLOG_HOST=https://hoynola.pythonanywhere.com`으로 YOLO 실행해 Cloud에 검출 게시물 등록
- [ ] Cloud `api_root` 캡처 (ID 8)
- [ ] Cloud 웹 목록·이미지 캡처 (ID 9)

## Phase 6 — 편의 기능 (ID 10)

- [ ] **기능 미정.** 사용자가 기능(최대 2개)을 정하면 `docs/ANDROID_FEATURES.md` §4에 정의를 적고 이 Phase를 세분화
- [ ] 기능 정의·설명·동작 캡처 (서버/클라이언트 양쪽 구현이면 보고서에 별도 행)

## Phase 7 — 제출 (ID 1·2)

- [ ] 루트에 `PhotoBlogServer/`, `PhotoViewer/`, `YOLOv5/`, 보고서 `.docx` 확인 → 폴더 구조 캡처 (ID 1)
- [ ] 비밀값·개인 사진·`db.sqlite3`·`media/`·`runs/`·모델 가중치가 추적되지 않는지 `git status`로 확인
- [ ] 커밋 및 `origin`에 push, GitHub 웹에서 구조 확인 (ID 2)
- [ ] Word 보고서 작성(학번·이름, 완료 여부, 적/부, 캡처 내장) 후 다시 커밋·push
- [ ] GitHub URL 제출
