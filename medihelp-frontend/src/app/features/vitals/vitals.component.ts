import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { VitalService } from '../../core/services/vital.service';
import { VitalResponse, VitalTrendResponse } from '../../core/models/health.model';

@Component({
  selector: 'app-vitals',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatChipsModule
  ],
  templateUrl: './vitals.component.html',
  styleUrl: './vitals.component.scss'
})
export class VitalsComponent implements OnInit {
  vitalForm: FormGroup;
  loading = false;
  saving = false;

  vitalTypes = [
    { value: 'HEART_RATE', label: 'Heart Rate', unit: 'bpm' },
    { value: 'BLOOD_PRESSURE_SYSTOLIC', label: 'Blood Pressure (Systolic)', unit: 'mmHg' },
    { value: 'BLOOD_PRESSURE_DIASTOLIC', label: 'Blood Pressure (Diastolic)', unit: 'mmHg' },
    { value: 'BLOOD_SUGAR', label: 'Blood Sugar', unit: 'mg/dL' },
    { value: 'TEMPERATURE', label: 'Temperature', unit: '\u00B0F' },
    { value: 'OXYGEN_SATURATION', label: 'Oxygen Saturation', unit: '%' },
    { value: 'WEIGHT', label: 'Weight', unit: 'kg' }
  ];

  // Trends
  selectedTrendType = 'HEART_RATE';
  selectedPeriod = 7;
  trend: VitalTrendResponse | null = null;

  // Table
  vitals: VitalResponse[] = [];
  displayedColumns = ['type', 'value', 'unit', 'notes', 'recordedAt'];
  totalElements = 0;
  pageSize = 10;
  pageIndex = 0;

  constructor(
    private fb: FormBuilder,
    private vitalService: VitalService,
    private snackBar: MatSnackBar
  ) {
    this.vitalForm = this.fb.group({
      type: ['HEART_RATE', Validators.required],
      value: ['', [Validators.required, Validators.min(0)]],
      unit: ['bpm'],
      notes: ['']
    });

    this.vitalForm.get('type')?.valueChanges.subscribe(type => {
      const found = this.vitalTypes.find(t => t.value === type);
      if (found) {
        this.vitalForm.patchValue({ unit: found.unit });
      }
    });
  }

  ngOnInit(): void {
    this.loadVitals();
    this.loadTrends();
  }

  onRecordVital(): void {
    if (this.vitalForm.invalid) return;

    this.saving = true;
    this.vitalService.recordVital(this.vitalForm.value).subscribe({
      next: () => {
        this.saving = false;
        this.snackBar.open('Vital recorded successfully!', 'Close', { duration: 3000 });
        this.vitalForm.patchValue({ value: '', notes: '' });
        this.loadVitals();
        this.loadTrends();
      },
      error: (err) => {
        this.saving = false;
        this.snackBar.open(err.error?.message || 'Failed to record vital.', 'Close', { duration: 5000 });
      }
    });
  }

  loadVitals(): void {
    this.loading = true;
    this.vitalService.getVitals(undefined, this.pageIndex, this.pageSize).subscribe({
      next: page => {
        this.vitals = page.content;
        this.totalElements = page.totalElements;
        this.loading = false;
      },
      error: () => {
        this.vitals = [];
        this.loading = false;
      }
    });
  }

  loadTrends(): void {
    this.vitalService.getVitalTrends(this.selectedTrendType, this.selectedPeriod).subscribe({
      next: trend => this.trend = trend,
      error: () => this.trend = null
    });
  }

  onTrendTypeChange(type: string): void {
    this.selectedTrendType = type;
    this.loadTrends();
  }

  onPeriodChange(days: number): void {
    this.selectedPeriod = days;
    this.loadTrends();
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadVitals();
  }

  getTypeLabel(type: string): string {
    const found = this.vitalTypes.find(t => t.value === type);
    return found ? found.label : type;
  }
}
