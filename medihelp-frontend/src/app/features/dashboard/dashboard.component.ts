import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatListModule } from '@angular/material/list';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { HealthScoreService } from '../../core/services/health-score.service';
import { VitalService } from '../../core/services/vital.service';
import { AppointmentService } from '../../core/services/appointment.service';
import { HealthScoreResponse, StreakResponse, VitalResponse } from '../../core/models/health.model';
import { AppointmentResponse } from '../../core/models/prescription.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ReactiveFormsModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatListModule,
    MatChipsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSnackBarModule
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  healthScore: HealthScoreResponse | null = null;
  latestVitals: { [key: string]: VitalResponse } = {};
  streaks: StreakResponse[] = [];
  upcomingAppointments: AppointmentResponse[] = [];
  loading = true;

  vitalIcons: { [key: string]: string } = {
    HEART_RATE: 'favorite',
    BLOOD_PRESSURE_SYSTOLIC: 'speed',
    BLOOD_PRESSURE_DIASTOLIC: 'speed',
    BLOOD_SUGAR: 'water_drop',
    TEMPERATURE: 'thermostat',
    OXYGEN_SATURATION: 'air',
    WEIGHT: 'monitor_weight'
  };

  vitalLabels: { [key: string]: string } = {
    HEART_RATE: 'Heart Rate',
    BLOOD_PRESSURE_SYSTOLIC: 'BP (Systolic)',
    BLOOD_PRESSURE_DIASTOLIC: 'BP (Diastolic)',
    BLOOD_SUGAR: 'Blood Sugar',
    TEMPERATURE: 'Temperature',
    OXYGEN_SATURATION: 'SpO2',
    WEIGHT: 'Weight'
  };

  // Quick-log widget state — lets users record a vital from the dashboard
  // without navigating to /vitals. Default unit auto-syncs to the selected
  // type (same UX as the dedicated /vitals page so the widgets feel
  // consistent).
  readonly quickLogTypes = [
    { value: 'HEART_RATE',                label: 'Heart Rate',          unit: 'bpm'   },
    { value: 'BLOOD_PRESSURE_SYSTOLIC',   label: 'BP (Systolic)',       unit: 'mmHg'  },
    { value: 'BLOOD_PRESSURE_DIASTOLIC',  label: 'BP (Diastolic)',      unit: 'mmHg'  },
    { value: 'BLOOD_SUGAR',               label: 'Blood Sugar',         unit: 'mg/dL' },
    { value: 'TEMPERATURE',               label: 'Temperature',         unit: '°F'    },
    { value: 'OXYGEN_SATURATION',         label: 'SpO₂',           unit: '%'     },
    { value: 'WEIGHT',                    label: 'Weight',              unit: 'kg'    }
  ];
  quickLogForm!: FormGroup;
  quickSaving = false;

  constructor(
    private healthScoreService: HealthScoreService,
    private vitalService: VitalService,
    private appointmentService: AppointmentService,
    private fb: FormBuilder,
    private snackBar: MatSnackBar
  ) {
    this.quickLogForm = this.fb.group({
      type:  ['HEART_RATE', Validators.required],
      value: ['', [Validators.required, Validators.min(0)]],
      unit:  ['bpm']
    });
    this.quickLogForm.get('type')?.valueChanges.subscribe(t => {
      const found = this.quickLogTypes.find(x => x.value === t);
      if (found) this.quickLogForm.patchValue({ unit: found.unit });
    });
  }

  onQuickLog(): void {
    if (this.quickLogForm.invalid || this.quickSaving) return;
    this.quickSaving = true;
    this.vitalService.recordVital(this.quickLogForm.value).subscribe({
      next: () => {
        this.quickSaving = false;
        this.quickLogForm.patchValue({ value: '' });
        this.snackBar.open('Vital recorded.', 'Close', { duration: 2000 });
        // Refresh the latest-vitals tiles so the dashboard reflects the new value
        this.vitalService.getLatestVitals().subscribe({
          next: v => this.latestVitals = v || {},
          error: () => {}
        });
        // Re-score: each new vital is worth 5 points (cap 20), so the
        // dashboard's Health Score should jump immediately.
        this.healthScoreService.calculateScore().subscribe({
          next: score => this.healthScore = score,
          error: () => {}
        });
      },
      error: (err) => {
        this.quickSaving = false;
        this.snackBar.open(err?.error?.message || 'Could not record vital.', 'Close', { duration: 4000 });
      }
    });
  }

  ngOnInit(): void {
    this.loadDashboardData();
  }

  loadDashboardData(): void {
    this.loading = true;

    // Recalculate first, then read latest. Calculation is idempotent (saves a
    // new snapshot row) and the only thing that turns the placeholder default
    // of {totalScore: 0} into an actual score derived from today's logs.
    this.healthScoreService.calculateScore().subscribe({
      next: score => this.healthScore = score,
      error: () => {
        // Calculation failed — fall back to whatever was last saved.
        this.healthScoreService.getLatestScore().subscribe({
          next: score => this.healthScore = score,
          error: () => this.healthScore = null
        });
      }
    });

    this.vitalService.getLatestVitals().subscribe({
      next: vitals => this.latestVitals = vitals || {},
      error: () => this.latestVitals = {}
    });

    this.healthScoreService.getStreaks().subscribe({
      next: streaks => this.streaks = streaks || [],
      error: () => this.streaks = []
    });

    this.appointmentService.getUpcoming().subscribe({
      next: appointments => {
        this.upcomingAppointments = (appointments || []).slice(0, 3);
        this.loading = false;
      },
      error: () => {
        this.upcomingAppointments = [];
        this.loading = false;
      }
    });
  }

  getVitalKeys(): string[] {
    return Object.keys(this.latestVitals);
  }

  getStreakIcon(type: string): string {
    const icons: { [key: string]: string } = {
      VITAL_LOGGING: 'monitor_heart',
      MEDICATION_ADHERENCE: 'medication',
      MOOD_LOGGING: 'mood',
      LOGIN: 'login'
    };
    return icons[type] || 'local_fire_department';
  }
}
