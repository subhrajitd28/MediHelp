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
import { MatTooltipModule } from '@angular/material/tooltip';
import { Clipboard } from '@angular/cdk/clipboard';
import { ProfileService } from '../../core/services/profile.service';
import { AuthService } from '../../core/services/auth.service';
import { UserProfile, Allergy, EmergencyContact } from '../../core/models/user.model';
import { GENDERS, INDIA_STATES, DIET_PREFERENCES } from '../../core/models/india-states';

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
    MatChipsModule,
    MatTooltipModule
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

  // Reuse the same lists the registration form uses so values round-trip consistently.
  // Earlier mismatch ('MALE' vs 'Male') made Material's mat-select render blank because
  // the saved value didn't match any option.
  readonly genders = GENDERS;
  readonly states = INDIA_STATES;
  readonly diets = DIET_PREFERENCES;
  readonly bloodTypes = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];
  readonly severities = ['MILD', 'MODERATE', 'SEVERE'];

  // Exposed in the UI so users can copy their ID to share with family-hub admins.
  get userId(): string { return this.auth.getUserId() || ''; }
  shortUserId = '';

  copyUserId(): void {
    if (!this.userId) return;
    this.clipboard.copy(this.userId);
    this.snackBar.open('User ID copied to clipboard.', 'Close', { duration: 2000 });
  }

  constructor(
    private fb: FormBuilder,
    private profileService: ProfileService,
    private snackBar: MatSnackBar,
    private auth: AuthService,
    private clipboard: Clipboard
  ) {
    this.profileForm = this.fb.group({
      firstName: ['', Validators.required],
      lastName: [''],
      dateOfBirth: [null],
      gender: [''],
      bloodType: [''],
      height: [null],
      weight: [null],
      state: [''],
      dietPreference: [''],
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
          state: profile.state,
          dietPreference: profile.dietPreference,
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
