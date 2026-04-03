export interface PrescriptionResponse {
  id: string;
  userId: string;
  doctorName: string;
  hospital: string;
  prescribedDate: string;
  notes: string;
  medications: MedicationResponse[];
}

export interface MedicationResponse {
  id: string;
  drugName: string;
  dosage: string;
  frequency: string;
  startDate: string;
  endDate?: string;
  withFood: boolean;
  active: boolean;
  instructions?: string;
  schedules?: MedicationScheduleResponse[];
}

export interface MedicationScheduleResponse {
  id: string;
  scheduledTime: string;
  taken: boolean;
  takenAt?: string;
}

export interface MedicationLogRequest {
  medicationId: string;
  status: 'TAKEN' | 'MISSED' | 'SKIPPED';
}

export interface AdherenceResponse {
  totalScheduled: number;
  totalTaken: number;
  totalMissed: number;
  totalSkipped: number;
  adherencePercentage: number;
}

export interface AppointmentRequest {
  doctorName: string;
  hospital: string;
  specialization: string;
  purpose: string;
  scheduledAt: string;
  notes?: string;
}

export interface AppointmentResponse {
  id: string;
  userId: string;
  doctorName: string;
  hospital: string;
  specialization: string;
  purpose: string;
  scheduledAt: string;
  status: string;
  notes: string;
  createdAt: string;
}

export interface NotificationResponse {
  id: string;
  userId: string;
  title: string;
  message: string;
  category: string;
  read: boolean;
  createdAt: string;
}
