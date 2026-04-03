from fastapi import APIRouter, File, UploadFile, HTTPException

from app.models.schemas import OcrResponse
from app.services.ocr_service import process_prescription

router = APIRouter()

ALLOWED_TYPES = {"image/jpeg", "image/png", "image/webp", "application/pdf"}
MAX_FILE_SIZE = 5 * 1024 * 1024  # 5MB


@router.post("/process", response_model=OcrResponse)
async def process_prescription_image(file: UploadFile = File(...)):
    """Process a prescription image using OCR and extract medication information."""
    if file.content_type not in ALLOWED_TYPES:
        raise HTTPException(
            status_code=400,
            detail=f"File type {file.content_type} not allowed. Use JPEG, PNG, or WebP.",
        )

    contents = await file.read()

    if len(contents) > MAX_FILE_SIZE:
        raise HTTPException(status_code=400, detail="File size exceeds 5MB limit.")

    result = process_prescription(contents)

    return OcrResponse(
        extracted_text=result["extracted_text"],
        medications=result["medications"],
        confidence=result["confidence"],
        needs_confirmation=result["needs_confirmation"],
    )
