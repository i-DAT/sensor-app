import socket
import threading
import time
from pythonosc.dispatcher import Dispatcher
from pythonosc.osc_server import BlockingOSCUDPServer
from pythonosc.udp_client import SimpleUDPClient


def osc_message_handler(address, *args):
    if address != "/rotation":
        print(f"Received OSC message at {address}: {args}")

def receiver(ip, port):
    dispatcher = Dispatcher()
    dispatcher.set_default_handler(osc_message_handler)

    server = BlockingOSCUDPServer((ip, port), dispatcher)
    server.serve_forever()

def sender(ip, port):
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    s.connect(('8.8.8.8', 80))
    this_ip = s.getsockname()[0]
    this_name = socket.gethostname()
    s.close()

    client = SimpleUDPClient(ip, port)
    while True:
        client.send_message("/host", [this_name, this_ip, 8000])
        time.sleep(1)

if __name__ == "__main__":
    receiver_thread = threading.Thread(target=receiver, args=("0.0.0.0", 8000), daemon=True)
    receiver_thread.start()

    sender_thread = threading.Thread(target=sender, args=("239.255.255.250", 4001), daemon=True)
    sender_thread.start()

    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        ...