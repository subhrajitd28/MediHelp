import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatStepperModule } from '@angular/material/stepper';
import { MatTableModule } from '@angular/material/table';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { PrescriptionScanService, OcrResponse, OcrMedication } from '../../core/services/prescription-scan.service';
import { PrescriptionService } from '../../core/services/prescription.service';
import { MedicationService } from '../../core/services/medication.service';

@Component({
  selector: 'app-prescription-scan',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatStepperModule,
    MatTableModule,
    MatProgressBarModule
  ],
  templateUrl: './prescription-scan.component.html',
  styleUrl: './prescription-scan.component.scss'
})
export class PrescriptionScanComponent {
  // Step tracking
  currentStep = 1;

  // Step 1: Upload
  selectedFile: File | null = null;
  imagePreviewUrl: string | null = null;
  scanning = false;
  dragOver = false;

  // Step 2: Review
  ocrResult: OcrResponse | null = null;
  extractedText = '';
  medications: OcrMedication[] = [];
  displayedColumns = ['name', 'dosage', 'frequency', 'actions'];

  // Step 3: Confirm
  saving = false;
  savedPrescriptionId: string | null = null;
  saveComplete = false;

  // Prescription metadata
  doctorName = '';
  hospital = '';

  acceptedTypes = 'image/jpeg,image/png,image/webp,application/pdf';

  constructor(
    private prescriptionScanService: PrescriptionScanService,
    private prescriptionService: PrescriptionService,
    private medicationService: MedicationService,
    private snackBar: MatSnackBar,
    private router: Router
  ) {}

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.setFile(input.files[0]);
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.dragOver = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.dragOver = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.dragOver = false;

    if (event.dataTransfer?.files && event.dataTransfer.files.length > 0) {
      this.setFile(event.dataTransfer.files[0]);
    }
  }

  private setFile(file: File): void {
    const allowedTypes = ['image/jpeg', 'image/png', 'image/webp', 'application/pdf'];
    if (!allowedTypes.includes(file.type)) {
      this.snackBar.open('Unsupported file type. Use JPEG, PNG, WebP, or PDF.', 'Close', { duration: 5000 });
      return;
    }

    this.selectedFile = file;

    if (file.type.startsWith('image/')) {
      const reader = new FileReader();
      reader.onload = (e) => {
        this.imagePreviewUrl = e.target?.result as string;
      };
      reader.readAsDataURL(file);
    } else {
      this.imagePreviewUrl = null;
    }
  }

  scanPrescription(): void {
    if (!this.selectedFile) return;

    this.scanning = true;
    this.prescriptionScanService.scanPrescription(this.selectedFile).subscribe({
      next: (res) => {
        this.ocrResult = res;
        this.extractedText = res.extracted_text;
        this.medications = res.medications.map(m => ({ ...m }));
        this.scanning = false;
        this.currentStep = 2;
      },
      error: (err) => {
        this.scanning = false;
        const msg = err.error?.detail || err.error?.message || 'Failed to scan prescription. Please try again.';
        this.snackBar.open(msg, 'Close', { duration: 5000 });
      }
    });
  }

  removeMedication(index: number): void {
    this.medications.splice(index, 1);
    this.medications = [...this.medications];
  }

  addMedication(): void {
    this.medications = [...this.medications, { name: '', dosage: '', frequency: '' }];
  }

  confirmAndCreate(): void {
    if (this.medications.length === 0) {
      this.snackBar.open('Please add at least one medication.', 'Close', { duration: 3000 });
      return;
    }

    this.saving = true;

    // Step 1: Create the prescription
    const prescriptionData = {
      doctorName: this.doctorName || 'Unknown Doctor',
      hospital: this.hospital || 'Unknown Hospital',
      prescribedDate: new Date().toISOString().split('T')[0],
      notes: 'Created from prescription scan',
      ocrText: this.extractedText
    };

    this.prescriptionService.createPrescription(prescriptionData).subscribe({
      next: (prescription) => {
        this.savedPrescriptionId = prescription.id;
        // Step 2: Create medications
        this.createMedications(prescription.id);
      },
      error: (err) => {
        this.saving = false;
        this.snackBar.open(err.error?.message || 'Failed to create prescription.', 'Close', { duration: 5000 });
      }
    });
  }

  private createMedications(prescriptionId: string): void {
    let completed = 0;
    let failed = 0;
    const total = this.medications.length;

    if (total === 0) {
      this.onSaveComplete();
      return;
    }

    for (const med of this.medications) {
      const payload = {
        drugName: med.name,
        dosage: med.dosage,
        frequency: this.mapFrequency(med.frequency),
        prescriptionId,
        startDate: new Date().toISOString().split('T')[0]
      };

      this.medicationService.addMedication(payload).subscribe({
        next: () => {
          completed++;
          if (completed + failed === total) {
            this.onSaveComplete(failed);
          }
        },
        error: () => {
          failed++;
          if (completed + failed === total) {
            this.onSaveComplete(failed);
          }
        }
      });
    }
  }

  private mapFrequency(freq: string): string {
    const lower = freq.toLowerCase();
    if (lower.includes('once') || lower.includes('1') || lower.includes('daily')) return 'ONCE_DAILY';
    if (lower.includes('twice') || lower.includes('2') || lower.includes('bid')) return 'TWICE_DAILY';
    if (lower.includes('three') || lower.includes('3') || lower.includes('tid') || lower.includes('thrice')) return 'THRICE_DAILY';
    if (lower.includes('needed') || lower.includes('prn')) return 'AS_NEEDED';
    return 'ONCE_DAILY';
  }

  private onSaveComplete(failedCount = 0): void {
    this.saving = false;
    this.saveComplete = true;
    this.currentStep = 3;

    if (failedCount > 0) {
      this.snackBar.open(
        `Prescription saved. ${failedCount} medication(s) failed to save.`,
        'Close',
        { duration: 5000 }
      );
    } else {
      this.snackBar.open('Prescription and medications saved successfully!', 'Close', { duration: 3000 });
    }
  }

  goToMedications(): void {
    this.router.navigate(['/medications']);
  }

  goToPrescriptions(): void {
    this.router.navigate(['/prescriptions']);
  }

  reset(): void {
    this.currentStep = 1;
    this.selectedFile = null;
    this.imagePreviewUrl = null;
    this.scanning = false;
    this.ocrResult = null;
    this.extractedText = '';
    this.medications = [];
    this.saving = false;
    this.savedPrescriptionId = null;
    this.saveComplete = false;
    this.doctorName = '';
    this.hospital = '';
  }
}
