"""
Phase 6 검증용 최소 Socket Server.

Django 서버를 대신해 Android Client가 실제로 보내는 HTTP 요청(Request Line,
Header)을 raw 소켓으로 수신하고, 받은 그대로를 화면에 출력 + 파일로 저장한다.
Django API 로직과는 무관한 별도의 검증용 스크립트이다.

사용법:
    python tools/socket_server.py [port]   (기본 port=9000)

Android 쪽에서는 ApiConfig.BASE_URL을 "http://10.0.2.2:<port>"로
임시 변경한 뒤 "동기화" 버튼을 눌러 요청을 1회 보내면 된다.
"""
import socket
import sys
from datetime import datetime
from pathlib import Path

HOST = "0.0.0.0"
PORT = int(sys.argv[1]) if len(sys.argv) > 1 else 9000

OUTPUT_DIR = Path(__file__).resolve().parent / "captured_requests"
OUTPUT_DIR.mkdir(exist_ok=True)

# Client가 응답을 기다리다 타임아웃/에러로 멈추지 않도록 최소한의 200 응답을 돌려준다.
RESPONSE_BODY = b"[]"
RESPONSE = (
    b"HTTP/1.1 200 OK\r\n"
    b"Content-Type: application/json\r\n"
    b"Content-Length: " + str(len(RESPONSE_BODY)).encode() + b"\r\n"
    b"Connection: close\r\n"
    b"\r\n" + RESPONSE_BODY
)


def main():
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as server:
        server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        server.bind((HOST, PORT))
        server.listen(1)
        print(f"Socket Server listening on {HOST}:{PORT} (Ctrl+C로 종료)")

        while True:
            conn, addr = server.accept()
            with conn:
                data = conn.recv(65536)
                request_text = data.decode("utf-8", errors="replace")

                print("=" * 60)
                print(f"[{datetime.now().isoformat()}] Request from {addr}")
                print(request_text)
                print("=" * 60)

                timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
                out_file = OUTPUT_DIR / f"request_{timestamp}.txt"
                out_file.write_text(request_text, encoding="utf-8")
                print(f"saved: {out_file}")

                conn.sendall(RESPONSE)


if __name__ == "__main__":
    main()
