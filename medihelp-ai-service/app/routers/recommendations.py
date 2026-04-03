from fastapi import APIRouter

from app.models.schemas import (
    DietRecommendationRequest,
    ExerciseRecommendationRequest,
    RecommendationResponse,
)
from app.services.gemini_service import get_diet_recommendations, get_exercise_recommendations

router = APIRouter()


@router.post("/diet/recommendations", response_model=RecommendationResponse)
async def diet_recommendations(request: DietRecommendationRequest):
    """Get personalized diet recommendations based on health conditions."""
    result = await get_diet_recommendations(
        conditions=request.conditions,
        allergies=request.allergies,
        restrictions=request.dietary_restrictions,
        goal=request.goal,
    )

    return RecommendationResponse(recommendations=result)


@router.post("/exercise/recommendations", response_model=RecommendationResponse)
async def exercise_recommendations(request: ExerciseRecommendationRequest):
    """Get personalized exercise recommendations based on health conditions."""
    result = await get_exercise_recommendations(
        conditions=request.conditions,
        fitness_level=request.fitness_level,
        restrictions=request.restrictions,
    )

    return RecommendationResponse(recommendations=result)
