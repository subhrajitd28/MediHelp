import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';

export interface FamilyGroup {
  id?: string;
  name: string;
  createdByUserId?: string;
  createdAt?: string;
}

export interface FamilyMember {
  id?: string;
  familyGroupId?: string;
  userId: string;
  role?: string;       // OWNER | CAREGIVER | VIEWER
  addedAt?: string;
}

@Injectable({ providedIn: 'root' })
export class FamilyService {
  private path = '/api/v1/users/me/family';

  constructor(private api: ApiService) {}

  getGroups(): Observable<FamilyGroup[]> {
    return this.api.get<FamilyGroup[]>(`${this.path}/groups`);
  }

  createGroup(group: FamilyGroup): Observable<FamilyGroup> {
    return this.api.post<FamilyGroup>(`${this.path}/groups`, group);
  }

  getMembers(groupId: string): Observable<FamilyMember[]> {
    return this.api.get<FamilyMember[]>(`${this.path}/groups/${groupId}/members`);
  }

  addMember(groupId: string, member: FamilyMember): Observable<FamilyMember> {
    return this.api.post<FamilyMember>(`${this.path}/groups/${groupId}/members`, member);
  }

  removeMember(groupId: string, memberId: string): Observable<void> {
    return this.api.delete<void>(`${this.path}/groups/${groupId}/members/${memberId}`);
  }
}
