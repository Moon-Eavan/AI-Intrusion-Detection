# 공통평가 02 — 보고서 / 캡처 체크리스트

보고서: 루트 `모바일웹서비스 프로젝트_공통평가 02_수행 결과 보고서.docx`
모든 항목은 미완료 상태에서 시작한다. 실제로 수행·캡처한 항목만 체크한다.

## 보고서 작성 규칙

- 상단 학번·이름 기재
- 각 행: `완료 여부`, `적/부 판정`(`○` 적합 / `×` 부적합 / `△` 점검제외 / `N/A` 해당없음), `관련증빙`
- 캡처는 Word 본문에 **이미지로 삽입**(링크 금지)
- 로컬/Cloud 구분이 필요한 캡처는 **브라우저 주소창이 보이게** 찍는다
- 비밀값·토큰, 본인 외 인물이 보이지 않게 한다

## ID별 체크

### ID 1. 폴더 구조
- [ ] 루트에 `PhotoBlogServer/`, `PhotoViewer/`, `YOLOv5/`, 보고서 `.docx`
- [ ] Finder 또는 GitHub 루트 화면 캡처

### ID 2. GitHub push
- [ ] `https://github.com/Moon-Eavan/AI-Intrusion-Detection` (공통평가 01 저장소와 다름)
- [ ] GitHub 웹에서 `YOLOv5/` 내용이 실제로 열리는지 확인(gitlink 아님)
- [ ] URL 기재

### ID 3. YOLOv5 설치 및 실행 (p.6)
- [x] **수정 전 원본 `detect.py`** 를 `--source 0`으로 실행 (`cd YOLOv5 && source venv/bin/activate && python detect.py --source 0`, 종료는 `q` 또는 Ctrl+C)
- [x] 바운딩 박스·클래스명이 보이는 인식 화면 + 실행 터미널 캡처 (2026-11-03, 사용자)

### ID 4. 검출기 코드 수정 (p.8–11)
- [x] VS Code에서 `changedetection.py` 캡처 (파일 전체가 길면 `add`·`send` 부분 위주로 2장)
- [x] VS Code에서 `detect.py` 추가 부분(5곳) 캡처 — `added (Ch08 p.11)`로 검색하면 5곳이 바로 보임. VS Code 소스 제어(Git) 탭의 diff 화면도 유용

### ID 5. 로컬 `api_root` (p.12)
- [ ] `http://127.0.0.1:8000/api_root/Post/`에 YOLO 등록 게시물(title = 검출 클래스, image URL) 표시
- [ ] 주소창 포함 캡처

### ID 6. 로컬 목록 및 이미지 (p.12)
- [ ] `http://127.0.0.1:8000/`에서 YOLO 게시물 제목·이미지 표시
- [ ] 주소창 포함 캡처

### ID 7. 모바일 화면
- [x] Android 동기화 후 **YOLO가 전송한 이미지**(ID 5·6 또는 ID 8·9와 같은 게시물) 표시 (2026-11-07, 로컬)
- [x] 에뮬레이터/실기기 캡처 — `../AI-Intrusion-Detection-docs/captures/phase6/ID7_앱_YOLO게시물_최신순.png` (보고서에 넣으면 끝) (수동 업로드 사진으로 대체 금지). 기본 목록은 오래된 순이므로 **최신순**을 눌러 YOLO 게시물이 위에 보이게 한다

### ID 8. PythonAnywhere `api_root`
- [x] `https://hoynola.pythonanywhere.com/api_root/Post/`에 YOLO 게시물 표시 (2026-11-07 07:25~07:27 UTC 6건, 목록 끝부분)
- [ ] 주소창 포함 캡처

### ID 9. PythonAnywhere 목록 및 이미지
- [x] `https://hoynola.pythonanywhere.com/`에서 YOLO 게시물 제목·이미지 표시 (맨 위 6건, 클립 배지)
- [ ] 주소창 포함 캡처

### ID 10. 기타 기획되지 않은 편의 기능 (2개)

정의·시나리오: `docs/EXTRA_FEATURES.md`. 보고서에는 서버/Edge/Android를 별도 행(10-1~10-6)으로 등록.

**기능 A. 원격 감시 설정** — 증빙 파일: `../AI-Intrusion-Detection-docs/captures/phase4/` (2026-11-07 생성, 보고서에 넣으면 체크)
- [x] 10-1 서버: 설정 API 응답 화면 (`http://127.0.0.1:8000/api_root/config/` — DRF 화면, 주소창 포함)
- [ ] 10-2 Edge: 설정 반영 콘솔 로그 (`MonitorConfig: 설정 반영 …`, `ChangeDetection: 감시 OFF - … 미등록`) — `10-2_YOLO_콘솔로그.txt`를 VS Code로 열어 캡처
- [ ] 10-3 Android: 설정 화면(대상 칩·임계값·대기시간), 메인 상단 "● Edge 온라인 · n초 전" / 오프라인 표시 — `10-3_1~5_*.png` (에뮬레이터 화면)
- [ ] 대상 `person`만 → 휴대폰은 등록 안 됨 / 대상 추가 후 등록됨 / 대기 30초 → 1건 / OFF 시 미등록 비교 캡처 (웹 목록 `http://127.0.0.1:8000/`에 2026-11-07 15:20~15:24(KST) 게시물)

**기능 B. 이벤트 클립 녹화 + 보관 정책** — 증빙 파일: `../AI-Intrusion-Detection-docs/captures/phase5/` (2026-11-07 생성, 보고서에 넣으면 체크)
- [ ] 10-4 Edge: 클립 생성·업로드 콘솔 로그 — `10-4_YOLO_클립로그.txt`(`클립 녹화 시작`, `녹화 중인 이벤트에 추가`, `클립 생성 …KB`)
- [ ] 10-5 서버: API의 `clip` URL(`10-5_API_clip응답.json` 또는 `http://127.0.0.1:8000/api_root/Post/` 끝부분), 웹 목록 배지 `http://127.0.0.1:8000/`, 웹 상세 클립 재생 `http://127.0.0.1:8000/post/154/`, "클립 만료됨" `http://127.0.0.1:8000/post/149/` — 브라우저 캡처는 사용자
- [ ] 10-6 Android: 목록 배지, 상세 화면 클립 재생, 만료 화면 — `10-6_1~4_*.png`

## 제출 전 최종 확인

- [ ] 모든 행의 완료 여부·적/부 판정이 실제 상태와 일치, `○` 항목에는 캡처 존재
- [ ] Word 안 이미지가 확대해도 읽힘
- [ ] `git status`에 `db.sqlite3`, `media/`, `venv/`, `YOLOv5/runs/`, `*.pt`, 비밀값 없음
- [ ] 보고서 `.docx` 최종본 커밋·push
- [ ] GitHub URL을 브라우저로 열어 루트 구조 재확인
