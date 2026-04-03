export interface UserProfile {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  dateOfBirth: string;
  gender: string;
  bloodType: string;
  height: number;
  weight: number;
  bio: string;
  phone: string;
  profilePictureUrl?: string;
  createdAt: string;
  updatedAt: string;
}

export interface EmergencyContact {
  id?: string;
  name: string;
  relationship: string;
  phone: string;
  email?: string;
}

export interface Allergy {
  id?: string;
  allergen: string;
  severity: string;
  reaction?: string;
  notes?: string;
}

export interface HealthCondition {
  id?: string;
  condition: string;
  diagnosedDate?: string;
  status: string;
  notes?: string;
}
