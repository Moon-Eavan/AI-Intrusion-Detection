# ID 10 기능 A: 원격 감시 설정 (강의자료에 없는 추가 파일)
# 서버의 /api_root/config/edge/ 에 주기적으로 heartbeat를 보내고, 응답으로 받은 설정을 검출 루프에 반영한다.
# PythonAnywhere 무료 계정은 서버 → Edge push가 불가능하므로 Edge가 polling한다.
import threading
import time

import requests


class MonitorConfig:
    POLL_SEC = 5
    KEYS = ['enabled', 'target_classes', 'conf_threshold', 'cooldown_sec']

    def __init__(self, host, names):
        self.url = host + '/api_root/config/edge/'
        # names는 YOLOv5 버전에 따라 list 또는 {0: 'person', ...} dict
        self.available_classes = [names[i] for i in range(len(names))]
        self.lock = threading.Lock()
        self.online = None
        # 서버 연결 전·실패 시 기본값 (Phase 3 표준과 같은 동작)
        self.enabled = True
        self.target_classes = []
        self.conf_threshold = 0.5
        self.cooldown_sec = 0
        self.poll()  # 검출 시작 전에 한 번 받아 둔다
        threading.Thread(target=self._run, daemon=True).start()

    def _run(self):
        while True:
            time.sleep(self.POLL_SEC)
            self.poll()

    def poll(self):
        try:
            res = requests.post(self.url, json={'available_classes': self.available_classes}, timeout=3)
            res.raise_for_status()
            data = res.json()
        except (requests.RequestException, ValueError) as e:
            # 서버가 꺼져 있거나 설정 API가 없는 서버면 마지막 설정을 유지하고 검출은 계속한다
            if self.online is not False:
                print('MonitorConfig: 서버 연결 실패, 마지막 설정 유지 -', e)
            self.online = False
            return
        if self.online is not True:
            print('MonitorConfig: 서버 연결됨', self.url)
        self.online = True
        self.apply(data)

    def apply(self, data):
        changes = []
        with self.lock:
            for key in self.KEYS:
                if key in data and data[key] != getattr(self, key):
                    changes.append('{0}={1}'.format(key, data[key]))
                    setattr(self, key, data[key])
        if changes:
            print('MonitorConfig: 설정 반영', ', '.join(changes))

    def accept(self, name, conf):
        """박스 하나를 감시 대상으로 볼지 판단 (대상 클래스 + 신뢰도 임계값)."""
        with self.lock:
            if self.target_classes and name not in self.target_classes:
                return False
            return conf >= self.conf_threshold
