import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { AppointmentService } from '../../core/services/appointment.service';
import { AppointmentResponse } from '../../core/models/prescription.model';

@Component({
  selector: 'app-appointments',
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
    MatTabsModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatChipsModule,
    MatDatepickerModule,
    MatNativeDateModule
  ],
  templateUrl: './appointments.component.html',
  styleUrl: './appointments.component.scss'
})
export class AppointmentsComponent implements OnInit {
  upcomingAppointments: AppointmentResponse[] = [];
  allAppointments: AppointmentResponse[] = [];
  loading = true;
  showAddForm = false;
  addForm: FormGroup;
  saving = false;

  constructor(
    private fb: FormBuilder,
    private appointmentService: AppointmentService,
    private snackBar: MatSnackBar
  ) {
    this.addForm = this.fb.group({
      doctorName: ['', Validators.required],
      hospital: ['', Validators.required],
      specialization: [''],
      purpose: ['', Validators.required],
      scheduledDate: [null, Validators.required],
      scheduledTime: ['', Validators.required],
      notes: ['']
    });
  }

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading = true;

    this.appointmentService.getUpcoming().subscribe({
      next: apts => {
        this.upcomingAppointments = apts || [];
      },
      error: () => this.upcomingAppointments = []
    });

    this.appointmentService.getAppointments().subscribe({
      next: apts => {
        this.allAppointments = apts || [];
        this.loading = false;
      },
      error: () => {
        this.allAppointments = [];
        this.loading = false;
      }
    });
  }

  onAddAppointment(): void {
    if (this.addForm.invalid) return;

    this.saving = true;
    const formValue = this.addForm.value;

    const date = new Date(formValue.scheduledDate);
    const [hours, minutes] = formValue.scheduledTime.split(':');
    date.setHours(parseInt(hours), parseInt(minutes));

    const payload = {
      doctorName: formValue.doctorName,
      hospital: formValue.hospital,
      specialization: formValue.specialization,
      purpose: formValue.purpose,
      scheduledAt: date.toISOString(),
      notes: formValue.notes
    };

    this.appointmentService.createAppointment(payload).subscribe({
      next: () => {
        this.saving = false;
        this.showAddForm = false;
        this.addForm.reset();
        this.snackBar.open('Appointment created!', 'Close', { duration: 3000 });
        this.loadData();
      },
      error: (err) => {
        this.saving = false;
        this.snackBar.open(err.error?.message || 'Failed to create appointment.', 'Close', { duration: 5000 });
      }
    });
  }

  cancelAppointment(id: string): void {
    this.appointmentService.cancelAppointment(id).subscribe({
      next: () => {
        this.snackBar.open('Appointment cancelled.', 'Close', { duration: 3000 });
        this.loadData();
      },
      error: (err) => {
        this.snackBar.open(err.error?.message || 'Failed to cancel appointment.', 'Close', { duration: 5000 });
      }
    });
  }

  getStatusColor(status: string): string {
    const colors: { [key: string]: string } = {
      SCHEDULED: 'primary',
      COMPLETED: 'accent',
      CANCELLED: 'warn'
    };
    return colors[status] || 'primary';
  }
}
