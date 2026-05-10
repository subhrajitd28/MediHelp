"""
Register this Flask service with the medihelp-gateway's Eureka server so the
gateway can route /api/v1/chatbot/** here via lb://chatbot-service.

The gateway looks up service-name via Eureka and load-balances. Without this
registration, the gateway returns 503 (we hit this exact bug with the old
medihelp-ai-service). Hence: register on startup, deregister on shutdown.
"""
import atexit
import logging
import os
import socket

import py_eureka_client.eureka_client as eureka_client

log = logging.getLogger(__name__)

EUREKA_URL = os.getenv("EUREKA_URL", "http://localhost:8761/eureka/")
APP_NAME = os.getenv("EUREKA_APP_NAME", "chatbot-service")
INSTANCE_PORT = int(os.getenv("CHATBOT_PORT", "8086"))


def _local_ip() -> str:
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 53))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return "127.0.0.1"


def register() -> None:
    try:
        eureka_client.init(
            eureka_server=EUREKA_URL,
            app_name=APP_NAME,
            instance_port=INSTANCE_PORT,
            instance_ip=_local_ip(),
            instance_host=_local_ip(),
            health_check_url=f"http://{_local_ip()}:{INSTANCE_PORT}/actuator/health",
            status_page_url=f"http://{_local_ip()}:{INSTANCE_PORT}/actuator/info",
        )
        atexit.register(stop)
        print(f"Registered with Eureka at {EUREKA_URL} as '{APP_NAME}' on port {INSTANCE_PORT}")
    except Exception as e:
        log.warning("Eureka registration failed: %s -- service will run but gateway routing won't work", e)


def stop() -> None:
    try:
        eureka_client.stop()
    except Exception:
        pass
