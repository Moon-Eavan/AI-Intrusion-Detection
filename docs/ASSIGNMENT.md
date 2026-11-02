# 공통평가 02 — 과제 요구사항

기준 자료 (충돌 시 보고서 양식 우선):

1. `모바일웹서비스 프로젝트_공통평가 02_수행 결과 보고서.docx` (루트, 평가표 ID 1~10)
2. `ch08. Django와 YOLO를 이용한 침입자 감시 시스템_v1.8.pdf` — 정리본 `docs/ch08.md`

공통평가 01의 요구사항(ID 1~14, Socket 검증, Coverage, 추가 기능 최대 5개)은 이번 평가 대상이 아니다.

## 1. 제출 방식

- 평가 항목을 구현·테스트하고, 캡처를 제공된 **MS Word 보고서**에 정리한 뒤 **GitHub URL**을 제출한다.
- **공통평가 01과 다른 레포지토리**로 제출한다 → `https://github.com/Moon-Eavan/AI-Intrusion-Detection`
- 첨부 그림은 모두 **문서에 포함**(링크 아님)하는 형식으로 저장한다.
- 보고서 상단에 학번·이름을 쓰고, 각 행의 `완료 여부`, `적/부 판정`, `관련증빙`을 채운다.
  - 적/부 판정 범례: `○` 적합, `×` 부적합, `△` 점검제외, `N/A` 해당없음

필수 루트 구조:

```text
ROOT/
├── PhotoBlogServer/    # Django Server
├── PhotoViewer/        # Android Client
├── YOLOv5/             # Edge Client
└── 모바일웹서비스 프로젝트_공통평가 02_수행 결과 보고서.docx
```

`README.md`, `CLAUDE.md`, `docs/`, `tools/`, `.gitignore` 등은 추가로 있어도 된다.

## 2. 평가표 (보고서 원문 기준)

| ID | 구분 | 요구사항명 | 제출 증빙 | 강의자료 |
| --- | --- | --- | --- | --- |
| 1 | 제출문 | 폴더 구조 | 탐색기·Finder 또는 Git 레포지토리 폴더 구조 캡처 (공통평가 01과 다른 레포지토리) | p.13 |
| 2 | 제출문 | GitHub 레포지토리 push | URL | p.13 |
| 3 | 강의 자료 | YOLOv5 설치 및 실행 | ① **변경 없는 기본 상태의 `detect.py`** 실행 ② 화면 객체 인식 완료 화면 캡처 | p.6 |
| 4 | 강의 자료 | 검출기 코드 수정 | VS Code에서 추가된 소스코드 화면 캡처 | p.8–10 (+ p.11 `detect.py`) |
| 5 | 강의 자료 | 블로그 등록 결과 | Server 브라우저 출력 화면 **(localhost) `api_root`** | p.12 |
| 6 | 강의 자료 | 블로그 등록 결과 | Server 브라우저 출력 화면 **(localhost) 목록 및 이미지** | p.12 |
| 7 | 제출문 | 모바일 화면 확인 | **YOLO에서 검출되어 서버로 전송된 이미지**를 모바일에서 확인하는 화면 | — |
| 8 | 제출문 | PythonAnywhere 클라우드상 동작 | Server 브라우저 출력 화면 **(PythonAnywhere) `api_root`** | p.12 |
| 9 | 제출문 | PythonAnywhere 클라우드상 동작 | Server 브라우저 출력 화면 **(PythonAnywhere) 목록 및 이미지** | p.12 |
| 10 | 제출문 | 기타 기획되지 않은 편의 기능 **[최대 2개]** | 기능 정의, 설명 및 동작 증빙 화면 캡처. 서버/클라이언트 각각 구현이 필요하면 **별도 요구사항(행)으로 등록** | p.13 |

## 3. 해석 시 주의할 점

- **ID 3**: `detect.py`를 수정하기 **전에** 실행·캡처해야 한다. 수정 후 화면은 "변경 없는 기본 상태" 증빙이 아니다.
- **ID 5·6·7**: 수동 업로드 사진이 아니라 **YOLO가 검출해 자동 전송한 게시물**이 보여야 한다.
- **ID 5·6**은 localhost, **ID 8·9**는 PythonAnywhere 화면으로 구분한다. 강의자료 p.12를 근거로 하므로
  Cloud 화면에도 YOLO가 등록한 게시물이 보이도록 하는 것이 안전하다.
- **ID 10**: 최대 2개. 공통평가 01의 추가 기능(14-1~14-5) 번호·목록을 그대로 옮기지 않는다.
  현재 선정된 기능 없음 → `docs/ANDROID_FEATURES.md` §4.

## 4. 강의자료 실습 동작 요약

1. YOLOv5 clone → 가상환경 → `pip install -r requirements.txt` → `python detect.py --source 0` (Python 3.9에서 검증됨)
2. `changedetection.py`에 `ChangeDetection` 클래스 작성: 클래스별 이전 검출 상태(0/1)를 저장하고 **0→1 변화가 있을 때만**
   이미지(320×240 축소 저장)와 `title`(새로 나타난 클래스명)·`text`(새로 나타난 클래스 목록)를 REST API로 전송
3. `detect.py`에 5곳(import, 생성, 프레임별 초기화, 클래스 표시, `cd.add`) 추가
4. 브라우저에서 `/api_root/Post/`와 블로그 목록으로 결과 확인

강의자료 코드와 현재 저장소 API의 차이(토큰 인증 URL 부재 등)는 `docs/YOLOV5_INTEGRATION.md` 참고.
