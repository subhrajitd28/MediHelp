import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatListModule } from '@angular/material/list';
import { MatDividerModule } from '@angular/material/divider';
import { MatChipsModule } from '@angular/material/chips';
import { ProfileService } from '../../core/services/profile.service';
import { UserProfile, Allergy, EmergencyContact } from '../../core/models/user.model';

@Component({
  selector: 'app-profile',
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
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatListModule,
    MatDividerModule,
    MatChipsModule
  ],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss'
})
export class ProfileComponent implements OnInit {
  profileForm: FormGroup;
  allergyForm: FormGroup;
  contactForm: FormGroup;
  loading = true;
  saving = false;
  profile: UserProfile | null = null;
  allergies: Allergy[] = [];
  contacts: EmergencyContact[] = [];
  showAllergyForm = false;
  showContactForm = false;

  genders = ['MALE', 'FEMALE', 'OTHER', 'PREFER_NOT_TO_SAY'];
  bloodTypes = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];
  severities = ['MILD', 'MODERATE', 'SEVERE'];

  constructor(
    private fb: FormBuilder,
    private profileService: ProfileService,
    private snackBar: MatSnackBar
  ) {
    this.profileForm = this.fb.group({
      firstName: ['', Validators.required],
      lastName: [''],
      dateOfBirth: [null],
      gender: [''],
      bloodType: [''],
      height: [null],
      weight: [null],
      bio: ['']
    });

    this.allergyForm = this.fb.group({
      allergen: ['', Validators.required],
      severity: ['MILD', Validators.required],
      reaction: [''],
      notes: ['']
    });

    this.contactForm = this.fb.group({
      name: ['', Validators.required],
      relationship: ['', Validators.required],
      phone: ['', Validators.required],
      email: ['']
    });
  }

  ngOnInit(): void {
    this.loadProfile();
    this.loadAllergies();
    this.loadContacts();
  }

  loadProfile(): void {
    this.loading = true;
    this.profileService.getProfile().subscribe({
      next: profile => {
        this.profile = profile;
        this.profileForm.patchValue({
          firstName: profile.firstName,
          lastName: profile.lastName,
          dateOfBirth: profile.dateOfBirth ? new Date(profile.dateOfBirth) : null,
          gender: profile.gender,
          bloodType: profile.bloodType,
          height: profile.height,
          weight: profile.weight,
          bio: profile.bio
        });
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  loadAllergies(): void {
    this.profileService.getAllergies().subscribe({
      next: allergies => this.allergies = allergies || [],
      error: () => this.allergies = []
    });
  }

  loadContacts(): void {
    this.profileService.getEmergencyContacts().subscribe({
      next: contacts => this.contacts = contacts || [],
      error: () => this.contacts = []
    });
  }

  onSaveProfile(): void {
    if (this.profileForm.invalid) return;

    this.saving = true;
    const formValue = this.profileForm.value;
    const payload = {
      ...formValue,
      dateOfBirth: formValue.dateOfBirth
        ? new Date(formValue.dateOfBirth).toISOString().split('T')[0]
        : null
    };

    this.profileService.updateProfile(payload).subscribe({
      next: () => {
        this.saving = false;
        this.snackBar.open('Profile updated!', 'Close', { duration: 3000 });
      },
      error: (err) => {
        this.saving = false;
        this.snackBar.open(err.error?.message || 'Failed to update profile.', 'Close', { duration: 5000 });
      }
    });
  }

  onAddAllergy(): void {
    if (this.allergyForm.invalid) return;

    this.profileService.addAllergy(this.allergyForm.value).subscribe({
      next: () => {
        this.showAllergyForm = false;
        this.allergyForm.reset({ severity: 'MILD' });
        this.snackBar.open('Allergy added!', 'Close', { duration: 3000 });
        this.loadAllergies();
      },
      error: (err) => {
        this.snackBar.open(err.error?.message || 'Failed to add allergy.', 'Close', { duration: 5000 });
      }
    });
  }

  onAddContact(): void {
    if (this.contactForm.invalid) return;

    this.profileService.addEmergencyContact(this.contactForm.value).subscribe({
      next: () => {
        this.showContactForm = false;
        this.contactForm.reset();
        this.snackBar.open('Emergency contact added!', 'Close', { duration: 3000 });
        this.loadContacts();
      },
      error: (err) => {
        this.snackBar.open(err.error?.message || 'Failed to add contact.', 'Close', { duration: 5000 });
      }
    });
  }
}
