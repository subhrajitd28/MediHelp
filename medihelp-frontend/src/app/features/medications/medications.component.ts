import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialogModule } from '@angular/material/dialog';
import { MedicationService } from '../../core/services/medication.service';
import { MedicationResponse, AdherenceResponse } from '../../core/models/prescription.model';

@Component({
  selector: 'app-medications',
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
    MatListModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatSlideToggleModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatChipsModule,
    MatDialogModule
  ],
  templateUrl: './medications.component.html',
  styleUrl: './medications.component.scss'
})
export class MedicationsComponent implements OnInit {
  medications: MedicationResponse[] = [];
  adherence: AdherenceResponse | null = null;
  loading = true;
  showAddForm = false;
  addForm: FormGroup;
  saving = false;

  frequencies = [
    { value: 'ONCE_DAILY', label: 'Once Daily' },
    { value: 'TWICE_DAILY', label: 'Twice Daily' },
    { value: 'THRICE_DAILY', label: 'Three Times Daily' },
    { value: 'AS_NEEDED', label: 'As Needed' }
  ];

  constructor(
    private fb: FormBuilder,
    private medicationService: MedicationService,
    private snackBar: MatSnackBar
  ) {
    this.addForm = this.fb.group({
      drugName: ['', Validators.required],
      dosage: ['', Validators.required],
      frequency: ['ONCE_DAILY', Validators.required],
      startDate: [new Date(), Validators.required],
      withFood: [false],
      instructions: ['']
    });
  }

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading = true;

    this.medicationService.getActiveMedications().subscribe({
      next: meds => {
        this.medications = meds || [];
        this.loading = false;
      },
      error: () => {
        this.medications = [];
        this.loading = false;
      }
    });

    this.medicationService.getAdherence().subscribe({
      next: adh => this.adherence = adh,
      error: () => this.adherence = null
    });
  }

  logMedication(medicationId: string, status: 'TAKEN' | 'SKIPPED'): void {
    this.medicationService.logMedication({ medicationId, status }).subscribe({
      next: () => {
        this.snackBar.open(`Medication marked as ${status.toLowerCase()}.`, 'Close', { duration: 3000 });
        this.loadData();
      },
      error: (err) => {
        this.snackBar.open(err.error?.message || 'Failed to log medication.', 'Close', { duration: 5000 });
      }
    });
  }

  onAddMedication(): void {
    if (this.addForm.invalid) return;

    this.saving = true;
    const formValue = this.addForm.value;
    const payload = {
      ...formValue,
      startDate: new Date(formValue.startDate).toISOString().split('T')[0]
    };

    this.medicationService.addMedication(payload).subscribe({
      next: () => {
        this.saving = false;
        this.showAddForm = false;
        this.addForm.reset({ frequency: 'ONCE_DAILY', startDate: new Date(), withFood: false });
        this.snackBar.open('Medication added!', 'Close', { duration: 3000 });
        this.loadData();
      },
      error: (err) => {
        this.saving = false;
        this.snackBar.open(err.error?.message || 'Failed to add medication.', 'Close', { duration: 5000 });
      }
    });
  }

  getFrequencyLabel(value: string): string {
    const found = this.frequencies.find(f => f.value === value);
    return found ? found.label : value;
  }
}
