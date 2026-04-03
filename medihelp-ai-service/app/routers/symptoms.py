from fastapi import APIRouter

from app.models.schemas import SymptomTriageRequest, SymptomTriageResponse
from app.services.gemini_service import triage_symptoms

router = APIRouter()


@router.post("/triage", response_model=SymptomTriageResponse)
async def symptom_triage(request: SymptomTriageRequest):
    """Analyze symptoms and provide urgency classification with recommended actions."""
    result = await triage_symptoms(
        symptoms=request.symptoms,
        age=request.age,
        gender=request.gender,
        existing_conditions=request.existing_conditions,
        current_medications=request.current_medications,
    )

    return SymptomTriageResponse(
        urgency=result.get("urgency", "MEDIUM"),
        possible_conditions=result.get("possible_conditions", []),
        recommended_actions=result.get("recommended_actions", []),
    )
