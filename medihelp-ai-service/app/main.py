from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.routers import symptoms, ocr, recommendations


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup: register with Eureka if configured
    yield
    # Shutdown: deregister from Eureka


app = FastAPI(
    title="MediHelp AI Service",
    description="AI-powered symptom triage, OCR processing, and health recommendations",
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(symptoms.router, prefix="/api/v1/ai/symptoms", tags=["Symptoms"])
app.include_router(ocr.router, prefix="/api/v1/ai/ocr", tags=["OCR"])
app.include_router(recommendations.router, prefix="/api/v1/ai", tags=["Recommendations"])


@app.get("/actuator/health")
async def health():
    return {"status": "UP"}
