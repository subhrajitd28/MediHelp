import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatListModule } from '@angular/material/list';
import { MatChipsModule } from '@angular/material/chips';
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
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatListModule,
    MatChipsModule
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

  constructor(
    private healthScoreService: HealthScoreService,
    private vitalService: VitalService,
    private appointmentService: AppointmentService
  ) {}

  ngOnInit(): void {
    this.loadDashboardData();
  }

  loadDashboardData(): void {
    this.loading = true;

    this.healthScoreService.getLatestScore().subscribe({
      next: score => this.healthScore = score,
      error: () => this.healthScore = null
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
