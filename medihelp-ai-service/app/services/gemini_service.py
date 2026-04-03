import os
import logging
from typing import Optional

import google.generativeai as genai

logger = logging.getLogger(__name__)

GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")
if GEMINI_API_KEY:
    genai.configure(api_key=GEMINI_API_KEY)

MODEL_NAME = "gemini-1.5-flash"

SYMPTOM_TRIAGE_PROMPT = """You are a medical triage assistant. Based on the symptoms described below, provide:
1. Urgency level: LOW, MEDIUM, HIGH, or EMERGENCY
2. Up to 3 possible conditions (as a layperson would understand them)
3. Recommended next steps

IMPORTANT: You are NOT diagnosing. You are helping the user decide their next steps.

Patient info:
- Age: {age}
- Gender: {gender}
- Existing conditions: {conditions}
- Current medications: {medications}

Symptoms described: {symptoms}

Respond in this exact JSON format:
{{
  "urgency": "LOW|MEDIUM|HIGH|EMERGENCY",
  "possible_conditions": ["condition1", "condition2"],
  "recommended_actions": ["action1", "action2", "action3"]
}}
"""

DIET_PROMPT = """You are a nutrition advisor. Based on the following health profile, suggest a personalized diet plan.

Conditions: {conditions}
Allergies: {allergies}
Dietary restrictions: {restrictions}
Goal: {goal}

Provide 5-7 specific, actionable dietary recommendations as a JSON array:
[
  {{"category": "Breakfast", "suggestion": "...", "reason": "..."}},
  ...
]
"""

EXERCISE_PROMPT = """You are a fitness advisor for people with health conditions. Suggest safe exercises.

Conditions: {conditions}
Fitness level: {fitness_level}
Restrictions: {restrictions}

Provide 5-7 specific exercise recommendations as a JSON array:
[
  {{"exercise": "...", "duration": "...", "frequency": "...", "benefit": "...", "precaution": "..."}},
  ...
]
"""


async def triage_symptoms(
    symptoms: str,
    age: Optional[int] = None,
    gender: Optional[str] = None,
    existing_conditions: Optional[list[str]] = None,
    current_medications: Optional[list[str]] = None,
) -> dict:
    if not GEMINI_API_KEY:
        return _mock_triage_response(symptoms)

    prompt = SYMPTOM_TRIAGE_PROMPT.format(
        symptoms=symptoms,
        age=age or "Not provided",
        gender=gender or "Not provided",
        conditions=", ".join(existing_conditions) if existing_conditions else "None reported",
        medications=", ".join(current_medications) if current_medications else "None reported",
    )

    try:
        model = genai.GenerativeModel(MODEL_NAME)
        response = model.generate_content(prompt)
        import json

        text = response.text.strip()
        # Strip markdown code block if present
        if text.startswith("```"):
            text = text.split("\n", 1)[1].rsplit("```", 1)[0].strip()
        return json.loads(text)
    except Exception as e:
        logger.error(f"Gemini API error: {e}")
        return _mock_triage_response(symptoms)


async def get_diet_recommendations(
    conditions: list[str],
    allergies: Optional[list[str]] = None,
    restrictions: Optional[list[str]] = None,
    goal: Optional[str] = None,
) -> list[dict]:
    if not GEMINI_API_KEY:
        return _mock_diet_response()

    prompt = DIET_PROMPT.format(
        conditions=", ".join(conditions),
        allergies=", ".join(allergies) if allergies else "None",
        restrictions=", ".join(restrictions) if restrictions else "None",
        goal=goal or "General health improvement",
    )

    try:
        model = genai.GenerativeModel(MODEL_NAME)
        response = model.generate_content(prompt)
        import json

        text = response.text.strip()
        if text.startswith("```"):
            text = text.split("\n", 1)[1].rsplit("```", 1)[0].strip()
        return json.loads(text)
    except Exception as e:
        logger.error(f"Gemini API error: {e}")
        return _mock_diet_response()


async def get_exercise_recommendations(
    conditions: list[str],
    fitness_level: Optional[str] = None,
    restrictions: Optional[list[str]] = None,
) -> list[dict]:
    if not GEMINI_API_KEY:
        return _mock_exercise_response()

    prompt = EXERCISE_PROMPT.format(
        conditions=", ".join(conditions),
        fitness_level=fitness_level or "BEGINNER",
        restrictions=", ".join(restrictions) if restrictions else "None",
    )

    try:
        model = genai.GenerativeModel(MODEL_NAME)
        response = model.generate_content(prompt)
        import json

        text = response.text.strip()
        if text.startswith("```"):
            text = text.split("\n", 1)[1].rsplit("```", 1)[0].strip()
        return json.loads(text)
    except Exception as e:
        logger.error(f"Gemini API error: {e}")
        return _mock_exercise_response()


def _mock_triage_response(symptoms: str) -> dict:
    return {
        "urgency": "MEDIUM",
        "possible_conditions": [
            "Common cold or viral infection",
            "Seasonal allergies",
        ],
        "recommended_actions": [
            "Monitor symptoms for 24-48 hours",
            "Stay hydrated and rest",
            "Consult a doctor if symptoms worsen or persist beyond 3 days",
        ],
    }


def _mock_diet_response() -> list[dict]:
    return [
        {"category": "Breakfast", "suggestion": "Oatmeal with fresh berries", "reason": "High fiber, antioxidants"},
        {"category": "Lunch", "suggestion": "Grilled chicken salad", "reason": "Lean protein, vegetables"},
        {"category": "Snack", "suggestion": "Mixed nuts and fruits", "reason": "Healthy fats, vitamins"},
    ]


def _mock_exercise_response() -> list[dict]:
    return [
        {"exercise": "Walking", "duration": "30 minutes", "frequency": "Daily", "benefit": "Cardiovascular health", "precaution": "Wear comfortable shoes"},
        {"exercise": "Stretching", "duration": "15 minutes", "frequency": "Daily", "benefit": "Flexibility", "precaution": "Don't overstretch"},
    ]
