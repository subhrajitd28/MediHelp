import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { UserProfile, Allergy, EmergencyContact, HealthCondition } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class ProfileService {
  private path = '/api/v1/users';

  constructor(private api: ApiService) {}

  getProfile(): Observable<UserProfile> {
    return this.api.get<UserProfile>(`${this.path}/me`);
  }

  updateProfile(profile: Partial<UserProfile>): Observable<UserProfile> {
    return this.api.put<UserProfile>(`${this.path}/me`, profile);
  }

  getAllergies(): Observable<Allergy[]> {
    return this.api.get<Allergy[]>(`${this.path}/me/allergies`);
  }

  addAllergy(allergy: Allergy): Observable<Allergy> {
    return this.api.post<Allergy>(`${this.path}/me/allergies`, allergy);
  }

  getEmergencyContacts(): Observable<EmergencyContact[]> {
    return this.api.get<EmergencyContact[]>(`${this.path}/me/emergency-contacts`);
  }

  addEmergencyContact(contact: EmergencyContact): Observable<EmergencyContact> {
    return this.api.post<EmergencyContact>(`${this.path}/me/emergency-contacts`, contact);
  }

  getConditions(): Observable<HealthCondition[]> {
    return this.api.get<HealthCondition[]>(`${this.path}/me/conditions`);
  }

  addCondition(condition: HealthCondition): Observable<HealthCondition> {
    return this.api.post<HealthCondition>(`${this.path}/me/conditions`, condition);
  }
}
