import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { HealthRecordService, HealthRecordResponse } from '../../core/services/health-record.service';

@Component({
  selector: 'app-health-records',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatCardModule, MatButtonModule,
    MatIconModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatDatepickerModule, MatNativeDateModule, MatChipsModule,
    MatSnackBarModule, MatProgressSpinnerModule
  ],
  templateUrl: './health-records.component.html',
  styleUrl: './health-records.component.scss'
})
export class HealthRecordsComponent implements OnInit {
  records: HealthRecordResponse[] = [];
  showForm = false;
  loading = false;
  uploading = false;
  form: FormGroup;
  selectedFile: File | null = null;
  categories = ['TEST_REPORT', 'DOCTOR_NOTE', 'PRESCRIPTION', 'IMAGING', 'OTHER'];
  filterCategory = '';

  constructor(
    private recordService: HealthRecordService,
    private fb: FormBuilder,
    private snackBar: MatSnackBar
  ) {
    this.form = this.fb.group({
      title: ['', Validators.required],
      category: ['OTHER'],
      description: [''],
      doctorName: [''],
      hospital: [''],
      recordDate: [null]
    });
  }

  ngOnInit() { this.loadRecords(); }

  loadRecords() {
    this.loading = true;
    const cat = this.filterCategory || undefined;
    this.recordService.getRecords(cat).subscribe({
      next: (data) => { this.records = data; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      this.selectedFile = input.files[0];
    }
  }

  onSubmit() {
    if (this.form.invalid) return;
    this.uploading = true;

    const formData: any = { ...this.form.value };
    if (formData.recordDate) {
      formData.recordDate = new Date(formData.recordDate).toISOString().split('T')[0];
    }

    if (this.selectedFile) {
      const reader = new FileReader();
      reader.onload = () => {
        formData.fileContentBase64 = (reader.result as string).split(',')[1];
        formData.fileName = this.selectedFile!.name;
        formData.fileType = this.selectedFile!.type;
        formData.fileSize = this.selectedFile!.size;
        this.createRecord(formData);
      };
      reader.readAsDataURL(this.selectedFile);
    } else {
      this.createRecord(formData);
    }
  }

  private createRecord(data: any) {
    this.recordService.createRecord(data).subscribe({
      next: () => {
        this.snackBar.open('Record uploaded!', 'Close', { duration: 3000 });
        this.showForm = false;
        this.form.reset({ category: 'OTHER' });
        this.selectedFile = null;
        this.uploading = false;
        this.loadRecords();
      },
      error: () => {
        this.snackBar.open('Upload failed', 'Close', { duration: 3000 });
        this.uploading = false;
      }
    });
  }

  deleteRecord(id: string) {
    this.recordService.deleteRecord(id).subscribe({
      next: () => {
        this.snackBar.open('Record deleted', 'Close', { duration: 3000 });
        this.loadRecords();
      }
    });
  }

  getCategoryIcon(cat: string): string {
    const icons: Record<string, string> = {
      TEST_REPORT: 'science', DOCTOR_NOTE: 'note', PRESCRIPTION: 'medication',
      IMAGING: 'image', OTHER: 'folder'
    };
    return icons[cat] || 'folder';
  }

  formatSize(bytes: number): string {
    if (!bytes) return '';
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / 1048576).toFixed(1) + ' MB';
  }
}
