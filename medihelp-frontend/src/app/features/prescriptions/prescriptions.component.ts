import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatDialogModule } from '@angular/material/dialog';
import { MatExpansionModule } from '@angular/material/expansion';
import { PrescriptionService } from '../../core/services/prescription.service';
import { PrescriptionResponse } from '../../core/models/prescription.model';

@Component({
  selector: 'app-prescriptions',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatChipsModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatDialogModule,
    MatExpansionModule
  ],
  templateUrl: './prescriptions.component.html',
  styleUrl: './prescriptions.component.scss'
})
export class PrescriptionsComponent implements OnInit {
  prescriptions: PrescriptionResponse[] = [];
  loading = true;
  showAddForm = false;
  addForm: FormGroup;
  saving = false;

  constructor(
    private fb: FormBuilder,
    private prescriptionService: PrescriptionService,
    private snackBar: MatSnackBar
  ) {
    this.addForm = this.fb.group({
      doctorName: ['', Validators.required],
      hospital: ['', Validators.required],
      prescribedDate: [new Date(), Validators.required],
      notes: ['']
    });
  }

  ngOnInit(): void {
    this.loadPrescriptions();
  }

  loadPrescriptions(): void {
    this.loading = true;
    this.prescriptionService.getPrescriptions().subscribe({
      next: (data) => {
        this.prescriptions = data || [];
        this.loading = false;
      },
      error: () => {
        this.prescriptions = [];
        this.loading = false;
      }
    });
  }

  onAddPrescription(): void {
    if (this.addForm.invalid) return;

    this.saving = true;
    const formValue = this.addForm.value;
    const payload = {
      ...formValue,
      prescribedDate: new Date(formValue.prescribedDate).toISOString().split('T')[0]
    };

    this.prescriptionService.createPrescription(payload).subscribe({
      next: () => {
        this.saving = false;
        this.showAddForm = false;
        this.addForm.reset({ prescribedDate: new Date() });
        this.snackBar.open('Prescription added!', 'Close', { duration: 3000 });
        this.loadPrescriptions();
      },
      error: (err) => {
        this.saving = false;
        this.snackBar.open(err.error?.message || 'Failed to add prescription.', 'Close', { duration: 5000 });
      }
    });
  }

  hasOcrText(prescription: PrescriptionResponse): boolean {
    return !!(prescription as any).ocrText;
  }

  getOcrText(prescription: PrescriptionResponse): string {
    return (prescription as any).ocrText || '';
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }
}
