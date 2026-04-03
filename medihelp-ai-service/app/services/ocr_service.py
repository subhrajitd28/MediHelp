import io
import logging
import re

import cv2
import numpy as np
import pytesseract
from PIL import Image

logger = logging.getLogger(__name__)


def preprocess_image(image_bytes: bytes) -> np.ndarray:
    """Preprocess prescription image for better OCR accuracy."""
    nparr = np.frombuffer(image_bytes, np.uint8)
    img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)

    # Convert to grayscale
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

    # Adaptive thresholding for better text extraction
    thresh = cv2.adaptiveThreshold(
        gray, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY, 11, 2
    )

    # Denoise
    denoised = cv2.fastNlMeansDenoising(thresh, None, 10, 7, 21)

    return denoised


def extract_text(image_bytes: bytes) -> str:
    """Extract text from prescription image using Tesseract OCR."""
    try:
        processed = preprocess_image(image_bytes)
        pil_image = Image.fromarray(processed)

        text = pytesseract.image_to_string(pil_image, config="--psm 6")
        return text.strip()
    except Exception as e:
        logger.error(f"OCR extraction failed: {e}")
        # Fallback: try without preprocessing
        try:
            image = Image.open(io.BytesIO(image_bytes))
            return pytesseract.image_to_string(image).strip()
        except Exception as fallback_error:
            logger.error(f"OCR fallback also failed: {fallback_error}")
            return ""


def extract_medications(text: str) -> list[dict]:
    """Parse extracted text to identify medications, dosages, and frequencies."""
    medications = []

    # Common medication patterns (simplified)
    # Look for patterns like: "Drug Name 500mg twice daily"
    lines = text.split("\n")

    dosage_pattern = re.compile(
        r"(\d+\s*(?:mg|ml|mcg|g|iu|units?))", re.IGNORECASE
    )
    frequency_pattern = re.compile(
        r"(once|twice|thrice|[1-4]\s*(?:times?|x)\s*(?:daily|a\s*day)|"
        r"every\s*\d+\s*hours?|"
        r"morning|evening|night|bedtime|"
        r"(?:before|after)\s*(?:meals?|food|breakfast|lunch|dinner))",
        re.IGNORECASE,
    )

    for line in lines:
        line = line.strip()
        if not line or len(line) < 3:
            continue

        dosage_match = dosage_pattern.search(line)
        frequency_match = frequency_pattern.search(line)

        if dosage_match or frequency_match:
            med = {
                "raw_text": line,
                "dosage": dosage_match.group(1) if dosage_match else None,
                "frequency": frequency_match.group(1) if frequency_match else None,
            }
            medications.append(med)

    return medications


def process_prescription(image_bytes: bytes) -> dict:
    """Full OCR pipeline: preprocess -> extract -> parse."""
    text = extract_text(image_bytes)
    medications = extract_medications(text)

    return {
        "extracted_text": text,
        "medications": medications,
        "confidence": 0.80 if text else 0.0,
        "needs_confirmation": True,
    }
