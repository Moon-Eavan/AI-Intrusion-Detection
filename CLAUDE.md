# AI-Intrusion-Detection — 작업 지침

모바일/웹서비스 **공통평가 02** (Ch08 Django + YOLO 침입자 감시 시스템).
공통평가 01의 Django·Android 코드를 재사용하고 YOLOv5 Edge Client를 새로 연동한다.

- `PhotoBlogServer/`: Django 4.2 + DRF (project `mysite`, app `blog`)
- `PhotoViewer/`: Android Java (`com.example.photoviewer`)
- `YOLOv5/`: Edge Client (upstream YOLOv5 + `changedetection.py`)
- 루트 `.docx`: 수행 결과 보고서 (제출물, 수정은 사용자 몫)

## 현재 상태 (2026-11-10)

- Phase 0 완료(Django 테스트 28 OK, Android 빌드·에뮬레이터 Cloud 동기화 확인), Phase 1 완료(YOLOv5 `e128e59`, ID 3 캡처), Phase 2 완료(`changedetection.py`, `detect.py` +7줄, ID 4 캡처), Phase 3 완료(웹캠 → 로컬 Django, `--conf-thres 0.5`로 중복 없음 확인), Phase 4 완료(기능 A, 웹캠 증빙), Phase 5 완료(기능 B, Django 테스트 50 OK, 웹캠 증빙), Phase 6 완료(로컬 최종 게시물·ID 7·Android 회귀), Phase 7 완료(PA 배포·Cloud YOLO 게시물·Android Cloud 확인·ID 8·9 캡처), Phase 8 완료(저장소 점검·보고서 초안·ID 10 증빙 재구성, 보고서 작성·push 완료(사용자). 적/부 판정 칸은 교수가 채우므로 비워 제출). 2026-11-10 교수 안내로 ID 10을 기능당 1행(10-1·10-2)으로 합쳐 보고서 수정 완료(점검함) — 커밋·push 후 GitHub URL 제출(사용자). `origin = Moon-Eavan/AI-Intrusion-Detection` (최신 커밋 `8c479ca`, origin과 동일. Word 잠금 파일 `~$*`는 `.gitignore`. PA는 `f41162c` — 이후 변경은 Edge 코드·문서·보고서뿐이라 재배포 불필요)
- `ApiConfig.BASE_URL`: 기본 `https://hoynola.pythonanywhere.com`, 로컬 에뮬레이터는 이 한 줄만 `http://10.0.2.2:8000`. 현재 Cloud 상태
- ID 10 편의 기능 확정: A 원격 감시 설정(구현됨), B 이벤트 클립 녹화 + 보관 정책(구현됨) — `docs/EXTRA_FEATURES.md`. 이 외 기능을 임의로 추가하지 않는다
- 진행 상황은 `docs/IMPLEMENTATION_PLAN.md`의 체크박스로 관리한다

## 문서 (필요한 것만 읽기)

| 목적 | 문서 |
| --- | --- |
| 평가 항목 ID 1~10 | `docs/ASSIGNMENT.md` |
| Phase별 계획·상태 | `docs/IMPLEMENTATION_PLAN.md` |
| YOLO 코드 작성·서버 연동 | `docs/YOLOV5_INTEGRATION.md` (강의 원문 코드는 `docs/ch08.md`) |
| 파일 구조 | `docs/PROJECT_STRUCTURE.md` |
| ID 10 기능 A·B 설계 | `docs/EXTRA_FEATURES.md` |
| Android 기존 기능·변경 범위 | `docs/ANDROID_FEATURES.md` |
| 캡처·보고서 | `docs/REPORT_CHECKLIST.md`, 보고서 문구 초안 `docs/REPORT_DRAFT.md` |

## 강의자료와 다른 코드 사실

- `/api_root/Post/`는 인증 없이 POST 가능. `/api-token-auth/`·JWT 없음 → 강의 코드의 토큰 로직을 그대로 쓰면 시작 시 실패
- `author`는 read_only(서버가 지정), 응답에 `id` 없음 → Android는 `created_date`를 게시물 키로 사용
- `USE_TZ=True`, `TIME_ZONE='UTC'` → naive `datetime.now()`를 날짜 필드로 보내지 않는다
- `ALLOWED_HOSTS`에 `localhost` 없음 → 브라우저는 `127.0.0.1:8000`
- `SECRET_KEY`/`DEBUG`: **이 저장소(로컬) 코드**는 환경변수(`DJANGO_SECRET_KEY`, `DJANGO_DEBUG`). PythonAnywhere도 Phase 7부터 같은 코드(`/home/hoynola/AI-Intrusion-Detection/PhotoBlogServer`, 새 키·`DEBUG=False`를 WSGI 파일에서 지정, `/media/`·`/static/`은 Web 탭 매핑). 키 값은 어디에도 옮겨 적지 않는다
- PA 코드 갱신: PA 콘솔에서 `git pull` → 필요하면 `migrate`·`collectstatic` → Web 탭 Reload (사용자). PA static 서버는 Range 미지원 → 클립은 Chrome·Android 재생, Safari 불확실
- 강의 `detect.py` 줄 번호(49/109/154/162/184)는 2022년 버전 기준 → 코드 위치로 찾는다
- 신뢰도 임계값: 기본 0.25는 배경 오검출이 깜빡여 중복 등록됨(Phase 3). 기능 A 이후에는 서버 `conf_threshold`(기본 0.5)가 필터하므로 `detect.py`는 `--conf-thres` 없이 실행한다(넣으면 앱에서 그 값 아래로 못 내림)
- Claude가 `detect.py --source 0`을 백그라운드로 실행할 수 있다(터미널 카메라 권한 공유, 로그 즉시 보려면 `python -u`) — 사용자는 카메라 앞 동작만, 종료는 SIGINT
- Android `HttpURLConnection`은 PATCH 미지원 → 앱의 설정 저장은 PUT
- 에뮬레이터는 quickboot 스냅샷의 예전 앱으로 뜰 수 있다 → 테스트 전 `adb install -r`로 최신 APK 설치
- 클립: YOLO venv에 `imageio`·`imageio-ffmpeg` 필요(`numpy<2` 유지). 클립 때문에 게시물은 감지 후 약 3~4초 뒤 생성. 보관 상한은 `CLIP_STORAGE_LIMIT_MB` 환경변수(기본 200)

## 작업 원칙

1. Phase 단위로 진행하고, 빌드/실행 확인 전에는 다음 Phase로 넘어가지 않는다.
2. 동작하는 기존 코드를 전면 수정하지 않는다. package/project/app 이름을 바꾸지 않는다.
3. ID 3 캡처 전에는 `detect.py`를 수정하지 않는다.
4. 강의자료 코드와 새로 보완한 코드를 구분한다(수정 줄에 짧은 주석).
5. 서버 주소는 YOLO는 `PHOTOBLOG_HOST` 환경변수, Android는 `ApiConfig.BASE_URL` 한 곳에서만 관리한다.
6. 비밀값·토큰·개인 촬영 이미지·`db.sqlite3`·`media/`·`runs/`·`*.pt`를 커밋하지 않는다. 기존 파일·`.git`을 임의로 삭제하지 않는다(`YOLOv5/.git`은 사용자 승인 후 제거함). upstream의 `YOLOv5/CLAUDE.md`·`AGENTS.md`(기여자용 AI 지침)는 사용자 승인 후 삭제함.
7. 실행하지 않은 테스트는 "미실행", 공통평가 01 결과를 이번 과제 완료로 보고하지 않는다.
8. 커밋·push·배포는 사용자가 요청할 때만 한다.
9. **작업이 끝날 때마다 관련 문서를 모두 갱신한다.** 최소한 `IMPLEMENTATION_PLAN.md`(체크박스·결과·현재 상태), `CLAUDE.md` 현재 상태, `README.md` 진행 상태는 갱신한다. 해당되면 `REPORT_CHECKLIST.md`(캡처), `EXTRA_FEATURES.md`(설계 변경), `PROJECT_STRUCTURE.md`(파일 추가)도 갱신한다. 종료 보고에 갱신한 문서를 적는다.

## 종료 보고 형식

```text
완료한 작업 / 수정·생성한 파일 / 테스트 결과(성공·실패·미실행) /
사용자가 직접 확인할 것 / 보고서 캡처(해당 ID) / 다음 단계
```
