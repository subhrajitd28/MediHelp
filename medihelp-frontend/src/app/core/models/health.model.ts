export interface VitalRequest {
  type: string;
  value: number;
  unit: string;
  source?: string;
  notes?: string;
}

export interface VitalResponse {
  id: string;
  userId: string;
  type: string;
  value: number;
  unit: string;
  source: string;
  notes: string;
  recordedAt: string;
}

export interface VitalTrendResponse {
  type: string;
  period: string;
  averageValue: number;
  minValue: number;
  maxValue: number;
  dataPoints: VitalDataPoint[];
}

export interface VitalDataPoint {
  value: number;
  recordedAt: string;
}

export interface HealthScoreResponse {
  totalScore: number;
  vitalScore: number;
  activityScore: number;
  nutritionScore: number;
  sleepScore: number;
  mentalHealthScore: number;
  calculatedAt: string;
}

export interface StreakResponse {
  id: string;
  streakType: string;
  currentStreak: number;
  longestStreak: number;
  lastActivityDate: string;
}

export interface BadgeResponse {
  id: string;
  badgeType: string;
  name: string;
  description: string;
  earnedAt: string;
}

export interface MoodEntryRequest {
  mood: number;
  journalText?: string;
  tags?: string[];
  sleepHours?: number;
  exerciseMinutes?: number;
}

export interface MoodEntryResponse {
  id: string;
  userId: string;
  mood: number;
  journalText: string;
  tags: string[];
  sleepHours: number;
  exerciseMinutes: number;
  recordedAt: string;
}
