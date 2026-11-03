# YOLOv5 ↔ Django ↔ Android 연동 가이드

강의자료 원문 코드는 `docs/ch08.md`에 있다. 이 문서는 그 코드를 **현재 저장소의 Django API에 맞게 적용하는 방법**을 정리한 설계 노트다.
`YOLOv5/`는 upstream `e128e59`(2026-11-01)로 설치됐고(Phase 1), 아래 연동 코드는 아직 구현 전이다.

## 1. 데이터 흐름

```text
카메라(--source 0)
  → detect.py: 프레임마다 추론, 클래스별 detected[0/1] 계산
  → changedetection.py: 이전 상태와 비교, 0→1 변화가 있을 때만
       └ runs/detect/expN/detected/YYYY/M/D/*.jpg 저장 (320×240)
       └ POST {HOST}/api_root/Post/  (multipart: title, text, image)
  → Django: Post 저장 (media/blog_image/YYYY/MM/DD/)
  → 브라우저 / api_root, Android PhotoViewer(GET /api_root/Post/)
```

## 2. 현재 Django API (코드 확인 결과)

| 항목 | 현재 코드 | 근거 |
| --- | --- | --- |
| 엔드포인트 | `GET/POST /api_root/Post/` (`DefaultRouter`, `BlogImages(ModelViewSet)`) | `blog/urls.py`, `blog/views.py` |
| 응답 필드 | `author, title, text, created_date, published_date, image` (**`id` 없음**) | `blog/serializers.py` |
| 필수 입력 | `title`, `text` / `image`는 선택(`blank=True`) | `blog/models.py` |
| `author` | `read_only`. 로그인 사용자 또는 서버가 고른 기본 작성자(superuser → 첫 사용자 → `mobile_uploader` 자동 생성) | `BlogImages.perform_create()` |
| 인증 | **없음** — DRF 기본 권한(AllowAny). `/api-token-auth/`, JWT 설정 없음 | `mysite/urls.py`, `settings.py`에 `REST_FRAMEWORK` 없음 |
| 이미지 경로 | `MEDIA_ROOT/blog_image/%Y/%m/%d/` (강의자료 화면의 `intruder_image/`와 다름, 바꿀 필요 없음) | `blog/models.py` |
| 시간대 | `USE_TZ = True`, `TIME_ZONE = 'UTC'` | `mysite/settings.py` |
| 호스트 | `ALLOWED_HOSTS = ['127.0.0.1', '10.0.2.2', '.pythonanywhere.com']` — **`localhost` 없음** | `mysite/settings.py` |

## 3. 강의자료 코드 → 현재 프로젝트 적용 시 차이

| 강의자료 | 문제 | 적용 방법 |
| --- | --- | --- |
| `__init__`에서 `/api-token-auth/` 호출, `Authorization: JWT ...` 헤더 | 해당 URL이 없어 404 → `raise_for_status()`에서 **detect.py 시작 자체가 실패** | 현행 API는 인증 없이 POST 가능하므로 토큰 로직을 제외한다. 인증을 추가하려면 서버(DRF 인증·권한)와 Android 업로드까지 함께 바꿔야 하므로 사용자와 먼저 결정 |
| `username='admin'`, `password='password'` 하드코딩 | 자격 증명 커밋 위험 | 토큰을 쓰지 않으면 불필요. 쓴다면 환경변수로 |
| `HOST = 'http://127.0.0.1:8000'` 고정 | 로컬/Cloud 전환 시 코드 수정 필요 | `HOST = os.environ.get('PHOTOBLOG_HOST', 'http://127.0.0.1:8000')` 한 곳에서 관리 |
| `created_date`, `published_date`에 `datetime.now()`(naive) 전송 | `USE_TZ=True`라 naive 로컬 시각(KST)이 UTC로 해석되어 **9시간 어긋남** + 경고 | `created_date`는 보내지 않고 서버 기본값 사용. `published_date`는 `datetime.now().astimezone().isoformat()`처럼 시간대 포함 값으로 전송(또는 생략) |
| `cv2.imwrite(full_path, dst)` (`Path` 객체) | OpenCV 버전에 따라 `Path`를 받지 못할 수 있음 | `cv2.imwrite(str(full_path), dst)` |
| `open(full_path, 'rb')`를 닫지 않음, timeout 없음 | 파일 핸들 누수, 서버 무응답 시 검출 루프 정지 | `with open(...)`, `timeout=` 지정, `requests.RequestException`은 출력만 하고 검출은 계속 |
| `os.getcwd() / save_dir` | `save_dir`가 `Path`라서 동작은 함 | 그대로 두거나 `Path.cwd() / save_dir`로 명확히 |
| `detect.py` 줄 번호 49/109/154/162/184 | 2022-06 버전 기준. 현재 upstream은 `@torch.no_grad()` 대신 `@smart_inference_mode()` 등 구조가 다름 | 줄 번호가 아니라 **코드 위치**(§4)로 찾아 삽입 |

변경하지 않는 것: 0→1 변화 판정 로직(`add`), 320×240 축소, `detected/연/월/일/` 저장 구조, `title`/`text` 구성.
강의자료 원본과 수정한 부분이 구분되도록 수정 줄에 짧은 주석을 남기면 ID 4 캡처 설명이 쉬워진다.

## 4. `detect.py` 삽입 위치

`e128e59` 버전 원본 `detect.py`에서 확인한 줄 번호 (수정 전 기준, 추가하면 뒤 줄 번호는 밀린다):

| 강의자료 | 현재 버전 줄 | 찾을 코드 | 추가 |
| --- | --- | --- | --- |
| 49 | 49 부근 | `from models.common import DetectMultiBackend` 등 import 블록 | `from changedetection import ChangeDetection` |
| 109 | 173 → 202 사이 | `stride, names, pt = model.stride, model.names, model.pt` 이후, `model.warmup(...)` 이전 | `cd = ChangeDetection(names)` |
| 154 | 231 → 246 사이 | `for i, det in enumerate(pred):` 안, `if len(det):` 이전 | `detected = [0 for i in range(len(names))]` |
| 162 | 256 | `for *xyxy, conf, cls in reversed(det):` 안 | `detected[int(cls)] = 1` |
| 184 | 284 → 285 사이 | `im0 = annotator.result()` 이후, `if view_img:` 이전 | `cd.add(names, detected, save_dir, im0)` |

- 데코레이터는 강의자료의 `@torch.no_grad()`가 아니라 `@smart_inference_mode()`(70줄)다.
- `detect.py`가 실행 위치(ROOT)를 `sys.path`에 추가하므로 같은 폴더의 `changedetection.py`를 바로 import할 수 있다.
- `im0`는 이 시점에 바운딩 박스가 그려진 이미지이므로 서버에 박스 포함 사진이 올라간다(강의자료 p.12와 동일).
- `names`는 버전에 따라 list 또는 `{0: 'person', ...}` dict다. `len(names)`, `names[i]` 모두 동작한다.
- 웹캠 1대 기준으로 설계되어 있다. 여러 소스를 동시에 쓰면 `result_prev`가 소스 간에 섞인다.

## 5. 동작 특성 (보고서 설명용)

- 같은 클래스 객체가 여러 개여도 상태는 0/1 하나다 → 개수·개별 추적·신원 식별은 하지 않는다.
- 객체가 계속 화면에 있으면 다시 보내지 않고, 사라졌다(0) 다시 나타나면(1) 새 게시물을 만든다.
- 검출이 프레임마다 깜빡이면 짧은 간격으로 중복 등록될 수 있다. 강의자료 기본 로직을 먼저 구현하고, 보완은 별도 판단한다.
- `title`은 이번 프레임에 새로 나타난 클래스 중 **마지막** 것, `text`는 새로 나타난 클래스 전체(`"person, cell phone, "`)다.

## 6. 로컬 / Cloud 전환

| 대상 | YOLOv5 (`PHOTOBLOG_HOST`) | Android (`ApiConfig.BASE_URL`) | 브라우저 |
| --- | --- | --- | --- |
| 로컬 (ID 5·6·7) | `http://127.0.0.1:8000` | `http://10.0.2.2:8000` (에뮬레이터) | `http://127.0.0.1:8000/` |
| Cloud (ID 8·9) | `https://hoynola.pythonanywhere.com` | `https://hoynola.pythonanywhere.com` | 같은 주소 |

- 브라우저는 `localhost` 대신 **`127.0.0.1`**로 접속한다(`localhost`는 `ALLOWED_HOSTS`에 없어 400). 캡처에 꼭 `localhost`가 필요하면 `ALLOWED_HOSTS`에 추가한다.
- Android 실기기로 PC 로컬 서버에 붙으려면 PC의 LAN IP를 `ALLOWED_HOSTS`와 `res/xml/network_security_config.xml`(cleartext 허용 도메인)에 추가해야 한다.
- Cloud 서버 코드는 공통평가 01 배포본이며, `settings.py`도 이전 버전(SECRET_KEY 하드코딩, `DEBUG = True`)이다. API 형식은 같으므로 기본 POST는 재배포 없이 가능하다. 단 ID 10 기능(모델·API 추가)은 배포가 필요하고, 그때 settings 전환도 함께 한다 (`IMPLEMENTATION_PLAN.md` Phase 7).

## 7. 검증 시나리오

| 시나리오 | 기대 결과 | 확인 위치 |
| --- | --- | --- |
| 화면에 검출 대상 없음 | 게시물 생성 없음 | YOLO 콘솔, API |
| 사람이 처음 나타남 | `title: person` 게시물 1건 + 이미지 | API, 웹 목록 |
| 사람이 계속 있음 | 추가 게시물 없음 | API |
| 사람이 나갔다 다시 들어옴 | 새 게시물 1건 | API |
| 새 클래스(예: `cell phone`)가 함께 등장 | 해당 클래스로 게시물 1건 | API |
| Django 서버 꺼짐 | 오류 출력 후 검출은 계속 진행 | YOLO 콘솔 |
| Android 동기화 | 같은 YOLO 이미지가 목록에 표시 | 에뮬레이터 |

Android는 서버 응답 형식이 그대로이면 수정 없이 YOLO 게시물을 표시한다. 게시물 식별 키는 `created_date`이므로
YOLO 쪽에서 `created_date`를 임의 값으로 보내 중복을 만들지 않는다.
