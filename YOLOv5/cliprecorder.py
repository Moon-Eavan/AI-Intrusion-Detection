# ID 10 기능 B: 이벤트 전후 클립 녹화 (강의자료에 없는 추가 파일)
# detect.py가 처리한 프레임(바운딩 박스 포함)을 받아 감지 전 3초 + 감지 후 3초를 H.264 mp4로 만든다.
import collections
import time

import cv2
import imageio


class ClipRecorder:
    PRE_SEC = 3
    POST_SEC = 3
    SIZE = (320, 240)  # 서버로 보내는 사진과 같은 크기

    def __init__(self):
        self.buffer = collections.deque()  # 최근 PRE_SEC초의 (시각, 프레임)
        self.event = None  # 녹화 중인 이벤트

    @property
    def recording(self):
        return self.event is not None

    def push(self, image):
        """매 프레임 호출한다. 녹화가 끝난 프레임이 있으면 그 이벤트를 돌려준다."""
        now = time.time()
        frame = cv2.resize(image, dsize=self.SIZE, interpolation=cv2.INTER_AREA)
        self.buffer.append((now, frame))
        while now - self.buffer[0][0] > self.PRE_SEC:
            self.buffer.popleft()

        if self.event is None:
            return None
        self.event['frames'].append((now, frame))
        if now < self.event['end_time']:
            return None
        done, self.event = self.event, None
        return done

    def start(self, **info):
        """감지 순간 호출한다. 버퍼의 감지 전 프레임으로 시작해 POST_SEC초 동안 더 모은다."""
        self.event = dict(info, frames=list(self.buffer), end_time=time.time() + self.POST_SEC)

    @staticmethod
    def encode(frames, path):
        """(시각, BGR 프레임) 목록을 브라우저·Android에서 재생되는 H.264 mp4로 저장한다.
        CPU 추론은 웹캠 30fps보다 느리므로 실제 처리 속도(프레임 수 ÷ 경과 시간)를 fps로 쓴다."""
        duration = frames[-1][0] - frames[0][0]
        fps = (len(frames) - 1) / duration if duration > 0 else 5
        writer = imageio.get_writer(str(path), fps=fps, codec='libx264', pixelformat='yuv420p',
                                    ffmpeg_log_level='error',
                                    output_params=['-movflags', '+faststart'])  # 스트리밍 재생용
        try:
            for _, frame in frames:
                writer.append_data(cv2.cvtColor(frame, cv2.COLOR_BGR2RGB))
        finally:
            writer.close()
        return fps, duration
