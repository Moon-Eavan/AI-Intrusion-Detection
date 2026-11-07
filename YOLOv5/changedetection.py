import os
import cv2
import pathlib
import time
import requests
from datetime import datetime
from monitorconfig import MonitorConfig  # 추가(ID 10 기능 A)


class ChangeDetection:
    result_prev = []
    # 수정: 서버 주소를 환경변수로 관리 (로컬/PythonAnywhere 전환 시 코드 수정 불필요)
    HOST = os.environ.get('PHOTOBLOG_HOST', 'http://127.0.0.1:8000')
    # 수정: 현재 서버에는 /api-token-auth/와 JWT 인증이 없어 username/password/token을 사용하지 않는다
    title = ''
    text = ''

    def __init__(self, names):
        self.result_prev = [0 for i in range(len(names))]
        # 수정: 토큰 발급 요청 제거 (없는 URL이라 시작 시 404로 실패함)
        print('ChangeDetection: 서버', self.HOST)
        # 추가(ID 10 기능 A): 원격 감시 설정, 클래스별 마지막 등록 시각
        self.config = MonitorConfig(self.HOST, names)
        self.last_sent = [0.0 for i in range(len(names))]

    def allowed(self, names, i):
        # 추가(ID 10 기능 A): 감시 OFF이거나 재등록 대기시간 안이면 등록하지 않는다 (검출 상태는 계속 갱신)
        if not self.config.enabled:
            print('ChangeDetection: 감시 OFF -', names[i], '미등록')
            return False
        now = time.time()
        if now - self.last_sent[i] < self.config.cooldown_sec:
            print('ChangeDetection: 재등록 대기 중 -', names[i], '미등록')
            return False
        self.last_sent[i] = now
        return True

    def add(self, names, detected_current, save_dir, image):
        self.title = ''
        self.text = ''
        change_flag = 0  # 변화 감지 플래그
        i = 0
        while i < len(self.result_prev):
            if self.result_prev[i] == 0 and detected_current[i] == 1 and self.allowed(names, i):  # 수정(ID 10 기능 A)
                change_flag = 1
                self.title = names[i]
                self.text += names[i] + ", "
            i += 1
        self.result_prev = detected_current[:]  # 객체 검출 상태 저장
        if change_flag == 1:
            self.send(save_dir, image)

    def send(self, save_dir, image):
        # 수정: 서버가 USE_TZ=True이므로 시간대가 포함된 현재 시각을 사용
        now = datetime.now().astimezone()
        today = datetime.now()
        save_path = os.getcwd() / save_dir / 'detected' / str(today.year) / str(today.month) / str(today.day)
        pathlib.Path(save_path).mkdir(parents=True, exist_ok=True)
        full_path = save_path / '{0}-{1}-{2}-{3}.jpg'.format(today.hour, today.minute, today.second, today.microsecond)
        dst = cv2.resize(image, dsize=(320, 240), interpolation=cv2.INTER_AREA)
        cv2.imwrite(str(full_path), dst)  # 수정: OpenCV 버전에 따라 Path를 받지 못하므로 str로 전달

        # 수정: 인증 헤더 제거
        headers = {'Accept': 'application/json'}

        # Post Create
        data = {
            'title': self.title,
            'text': self.text,
            # 수정: created_date는 서버 기본값 사용 (Android가 게시물 키로 사용)
            'published_date': now.isoformat(),
        }
        # 수정: 파일 핸들 닫기, timeout 지정, 서버 오류가 나도 검출 루프는 계속 진행
        try:
            with open(full_path, 'rb') as f:
                file = {'image': f}
                res = requests.post(self.HOST + '/api_root/Post/', data=data, files=file, headers=headers, timeout=5)
            print(res)
        except requests.RequestException as e:
            print('ChangeDetection: 전송 실패', e)
