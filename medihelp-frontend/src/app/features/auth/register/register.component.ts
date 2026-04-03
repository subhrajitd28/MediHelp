import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatStepperModule } from '@angular/material/stepper';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-register',
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
    MatStepperModule
  ],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss'
})
export class RegisterComponent {
  registerForm: FormGroup;
  otpForm: FormGroup;
  loading = false;
  step: 'register' | 'otp' = 'register';
  registeredEmail = '';
  hidePassword = true;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {
    this.registerForm = this.fb.group({
      firstName: ['', [Validators.required]],
      lastName: [''],
      email: ['', [Validators.required, Validators.email]],
      phone: [''],
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required]]
    });

    this.otpForm = this.fb.group({
      otp: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(6)]]
    });
  }

  onRegister(): void {
    if (this.registerForm.invalid) return;

    const formValue = this.registerForm.value;
    if (formValue.password !== formValue.confirmPassword) {
      this.snackBar.open('Passwords do not match', 'Close', { duration: 3000 });
      return;
    }

    this.loading = true;
    const { confirmPassword, ...registerData } = formValue;
    this.authService.register(registerData).subscribe({
      next: () => {
        this.loading = false;
        this.registeredEmail = formValue.email;
        this.step = 'otp';
        this.snackBar.open('Registration successful! Please enter the OTP sent to your email.', 'Close', { duration: 5000 });
      },
      error: (err) => {
        this.loading = false;
        const message = err.error?.message || 'Registration failed. Please try again.';
        this.snackBar.open(message, 'Close', { duration: 5000 });
      }
    });
  }

  onVerifyOtp(): void {
    if (this.otpForm.invalid) return;

    this.loading = true;
    this.authService.verifyOtp({
      email: this.registeredEmail,
      otp: this.otpForm.value.otp
    }).subscribe({
      next: () => {
        this.loading = false;
        this.snackBar.open('Email verified! You can now login.', 'Close', { duration: 3000 });
        this.router.navigate(['/login']);
      },
      error: (err) => {
        this.loading = false;
        const message = err.error?.message || 'OTP verification failed.';
        this.snackBar.open(message, 'Close', { duration: 5000 });
      }
    });
  }
}
