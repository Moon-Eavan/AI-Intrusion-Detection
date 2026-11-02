# AI-Intrusion-Detection — 작업 지침

모바일/웹서비스 **공통평가 02** (Ch08 Django + YOLO 침입자 감시 시스템).
공통평가 01의 Django·Android 코드를 재사용하고 YOLOv5 Edge Client를 새로 연동한다.

- `PhotoBlogServer/`: Django 4.2 + DRF (project `mysite`, app `blog`)
- `PhotoViewer/`: Android Java (`com.example.photoviewer`)
- `YOLOv5/`: Edge Client (upstream YOLOv5 + `changedetection.py`)
- 루트 `.docx`: 수행 결과 보고서 (제출물, 수정은 사용자 몫)

## 현재 상태 (2026-11-02)

- Django·Android 복사 완료(재검증 전), `YOLOv5/` 미생성, 커밋 없음, `origin = Moon-Eavan/AI-Intrusion-Detection`
- `ApiConfig.BASE_URL = https://hoynola.pythonanywhere.com` (로컬 에뮬레이터: 이 한 줄만 `http://10.0.2.2:8000`)
- ID 10 편의 기능: **미정** — 사용자가 정하기 전에 임의로 선정·구현하지 않는다
- 진행 상황은 `docs/IMPLEMENTATION_PLAN.md`의 체크박스로 관리한다

## 문서 (필요한 것만 읽기)

| 목적 | 문서 |
| --- | --- |
| 평가 항목 ID 1~10 | `docs/ASSIGNMENT.md` |
| Phase별 계획·상태 | `docs/IMPLEMENTATION_PLAN.md` |
| YOLO 코드 작성·서버 연동 | `docs/YOLOV5_INTEGRATION.md` (강의 원문 코드는 `docs/ch08.md`) |
| 파일 구조 | `docs/PROJECT_STRUCTURE.md` |
| Android 기능, ID 10 | `docs/ANDROID_FEATURES.md` |
| 캡처·보고서 | `docs/REPORT_CHECKLIST.md` |

## 강의자료와 다른 코드 사실

- `/api_root/Post/`는 인증 없이 POST 가능. `/api-token-auth/`·JWT 없음 → 강의 코드의 토큰 로직을 그대로 쓰면 시작 시 실패
- `author`는 read_only(서버가 지정), 응답에 `id` 없음 → Android는 `created_date`를 게시물 키로 사용
- `USE_TZ=True`, `TIME_ZONE='UTC'` → naive `datetime.now()`를 날짜 필드로 보내지 않는다
- `ALLOWED_HOSTS`에 `localhost` 없음 → 브라우저는 `127.0.0.1:8000`
- `SECRET_KEY`/`DEBUG`는 환경변수(`DJANGO_SECRET_KEY`, `DJANGO_DEBUG`)
- 강의 `detect.py` 줄 번호(49/109/154/162/184)는 2022년 버전 기준 → 코드 위치로 찾는다

## 작업 원칙

1. Phase 단위로 진행하고, 빌드/실행 확인 전에는 다음 Phase로 넘어가지 않는다.
2. 동작하는 기존 코드를 전면 수정하지 않는다. package/project/app 이름을 바꾸지 않는다.
3. ID 3 캡처 전에는 `detect.py`를 수정하지 않는다.
4. 강의자료 코드와 새로 보완한 코드를 구분한다(수정 줄에 짧은 주석).
5. 서버 주소는 YOLO는 `PHOTOBLOG_HOST` 환경변수, Android는 `ApiConfig.BASE_URL` 한 곳에서만 관리한다.
6. 비밀값·토큰·개인 촬영 이미지·`db.sqlite3`·`media/`·`runs/`·`*.pt`를 커밋하지 않는다. 기존 파일·`.git`을 임의로 삭제하지 않는다(`YOLOv5/.git` 처리는 사용자 확인).
7. 실행하지 않은 테스트는 "미실행", 공통평가 01 결과를 이번 과제 완료로 보고하지 않는다.
8. 커밋·push·배포는 사용자가 요청할 때만 한다.

## 종료 보고 형식

```text
완료한 작업 / 수정·생성한 파일 / 테스트 결과(성공·실패·미실행) /
사용자가 직접 확인할 것 / 보고서 캡처(해당 ID) / 다음 단계
```
