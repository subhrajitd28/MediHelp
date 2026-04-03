from pydantic import BaseModel, Field
from typing import Optional
from datetime import datetime


class SymptomTriageRequest(BaseModel):
    symptoms: str = Field(..., description="Description of symptoms in plain language")
    age: Optional[int] = None
    gender: Optional[str] = None
    existing_conditions: Optional[list[str]] = None
    current_medications: Optional[list[str]] = None


class SymptomTriageResponse(BaseModel):
    urgency: str = Field(..., description="LOW, MEDIUM, HIGH, or EMERGENCY")
    possible_conditions: list[str]
    recommended_actions: list[str]
    disclaimer: str = (
        "This is NOT a medical diagnosis. Always consult a qualified healthcare "
        "professional for medical advice. If you are experiencing a medical emergency, "
        "call emergency services immediately."
    )
    timestamp: datetime = Field(default_factory=datetime.utcnow)


class OcrRequest(BaseModel):
    pass  # File uploaded via multipart form


class OcrResponse(BaseModel):
    extracted_text: str
    medications: list[dict]
    confidence: float
    needs_confirmation: bool = True


class DietRecommendationRequest(BaseModel):
    conditions: list[str] = Field(..., description="Health conditions")
    allergies: Optional[list[str]] = None
    dietary_restrictions: Optional[list[str]] = None
    goal: Optional[str] = None


class ExerciseRecommendationRequest(BaseModel):
    conditions: list[str] = Field(..., description="Health conditions")
    fitness_level: Optional[str] = Field(None, description="BEGINNER, INTERMEDIATE, ADVANCED")
    restrictions: Optional[list[str]] = None


class RecommendationResponse(BaseModel):
    recommendations: list[dict]
    disclaimer: str = (
        "These recommendations are for informational purposes only. "
        "Always follow your doctor's specific advice."
    )
    timestamp: datetime = Field(default_factory=datetime.utcnow)
