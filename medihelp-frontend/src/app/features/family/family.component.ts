import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { FamilyService, FamilyGroup, FamilyMember } from '../../core/services/family.service';

interface GroupView extends FamilyGroup {
  members?: FamilyMember[];
  loadingMembers?: boolean;
  expanded?: boolean;
  newMemberId?: string;
  newMemberRole?: string;
}

const ROLE_COLORS: { [key: string]: string } = {
  OWNER: '#3f51b5',
  CAREGIVER: '#43a047',
  VIEWER: '#757575'
};

const ROLES = ['OWNER', 'CAREGIVER', 'VIEWER'];

/**
 * Family Health Hub — manage one or more family groups and the dependents in
 * each. Each member has a role (Owner / Caregiver / Viewer) that drives access
 * level on the canonical user-service backend (/api/v1/users/me/family).
 *
 * Use case: a parent managing their elderly mother's vitals + medications, or
 * a caregiver viewing a patient's records without write access.
 */
@Component({
  selector: 'app-family',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatCardModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatChipsModule,
    MatProgressSpinnerModule, MatSnackBarModule, MatTooltipModule
  ],
  template: `
    <div class="family-page">
      <header class="page-header">
        <div>
          <h1>Family Health Hub</h1>
          <p>Manage health records and medications for the people you care for.</p>
        </div>
      </header>

      <!-- Create new group -->
      <mat-card class="create-card">
        <mat-card-content>
          <h3><mat-icon>group_add</mat-icon> Create a new family group</h3>
          <div class="create-row">
            <mat-form-field appearance="outline">
              <mat-label>Group name (e.g. "Family", "Parents")</mat-label>
              <input matInput [(ngModel)]="newGroupName" (keydown.enter)="createGroup()" placeholder="My Family">
            </mat-form-field>
            <button mat-flat-button color="primary"
                    (click)="createGroup()"
                    [disabled]="!newGroupName.trim() || creating">
              <mat-icon>add</mat-icon> Create group
            </button>
          </div>
        </mat-card-content>
      </mat-card>

      <!-- Existing groups -->
      <div *ngIf="loading" class="loading"><mat-spinner diameter="32"></mat-spinner></div>

      <div *ngIf="!loading && groups.length === 0" class="empty-state">
        <mat-icon>diversity_3</mat-icon>
        <p>No family groups yet.</p>
        <span>Start by creating one above. Then add members to share care.</span>
      </div>

      <mat-card *ngFor="let g of groups" class="group-card">
        <mat-card-header>
          <mat-card-title>
            <mat-icon>family_restroom</mat-icon>
            {{ g.name }}
          </mat-card-title>
          <mat-card-subtitle>Created {{ g.createdAt | date:'MMM d, yyyy' }}</mat-card-subtitle>
          <button mat-icon-button (click)="toggleGroup(g)" [matTooltip]="g.expanded ? 'Hide members' : 'Show members'">
            <mat-icon>{{ g.expanded ? 'expand_less' : 'expand_more' }}</mat-icon>
          </button>
        </mat-card-header>

        <mat-card-content *ngIf="g.expanded">

          <div *ngIf="g.loadingMembers" class="loading">
            <mat-spinner diameter="24"></mat-spinner>
          </div>

          <div *ngIf="!g.loadingMembers" class="member-list">
            <div *ngFor="let m of g.members" class="member-row">
              <mat-icon class="avatar">person</mat-icon>
              <div class="member-info">
                <div class="member-id" [matTooltip]="m.userId">{{ shortId(m.userId) }}</div>
                <span class="role-chip" [style.background]="roleColor(m.role)">{{ m.role || 'VIEWER' }}</span>
              </div>
              <button mat-icon-button color="warn" (click)="removeMember(g, m)" matTooltip="Remove member">
                <mat-icon>person_remove</mat-icon>
              </button>
            </div>

            <div *ngIf="(g.members?.length || 0) === 0" class="no-members">
              No members yet.
            </div>
          </div>

          <!-- Add member form -->
          <div class="add-member">
            <mat-form-field appearance="outline" class="grow">
              <mat-label>User ID (UUID of the dependent's MediHelp account)</mat-label>
              <input matInput [(ngModel)]="g.newMemberId" placeholder="e.g. 9e4c3c5d-2f31-48db-90ff-5577292a3054">
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Role</mat-label>
              <mat-select [(ngModel)]="g.newMemberRole">
                <mat-option *ngFor="let r of roles" [value]="r">{{ r }}</mat-option>
              </mat-select>
            </mat-form-field>
            <button mat-stroked-button color="primary"
                    (click)="addMember(g)"
                    [disabled]="!g.newMemberId">
              <mat-icon>person_add</mat-icon> Add
            </button>
          </div>

          <p class="hint">
            <mat-icon>info</mat-icon>
            Each dependent must already have a MediHelp account. Share their <strong>User ID</strong>
            (visible on their Profile page) to add them here.
          </p>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .family-page { padding: 24px; max-width: 900px; margin: 0 auto; }
    .page-header h1 { margin: 0; font-size: 24px; }
    .page-header p { margin: 4px 0 16px; color: #666; font-size: 13px; }

    .create-card { margin-bottom: 16px; background: #f8fafc; border: 1px dashed #cbd5e1; }
    .create-card h3 {
      display: flex; align-items: center; gap: 8px;
      margin: 0 0 12px; font-size: 15px; color: #555;
    }
    .create-row { display: flex; gap: 12px; align-items: center; }
    .create-row mat-form-field { flex: 1; }

    .loading { display: flex; justify-content: center; padding: 32px; }

    .empty-state {
      text-align: center; padding: 48px 16px; color: #888;
      mat-icon { font-size: 48px; width: 48px; height: 48px; color: #c5cae9; }
      p { margin: 12px 0 4px; font-size: 16px; }
      span { font-size: 13px; color: #aaa; }
    }

    .group-card { margin-bottom: 16px; }
    .group-card mat-card-title { display: flex; align-items: center; gap: 8px; }

    .member-list { padding: 8px 0; }
    .member-row {
      display: flex; align-items: center; gap: 12px;
      padding: 8px 12px; border-bottom: 1px solid #f0f0f0;
      &:last-child { border-bottom: none; }
    }
    .avatar {
      background: #e3edff; color: #3f51b5;
      border-radius: 50%; padding: 6px;
    }
    .member-info { flex: 1; }
    .member-id { font-family: ui-monospace, monospace; font-size: 13px; }
    .role-chip {
      display: inline-block;
      padding: 2px 10px; border-radius: 12px;
      color: #fff; font-size: 11px;
      font-weight: 600; letter-spacing: 0.04em;
      margin-top: 2px;
    }
    .no-members { padding: 16px; text-align: center; color: #aaa; font-size: 13px; }

    .add-member {
      display: flex; gap: 12px; align-items: flex-start;
      padding: 16px 0 4px;
      .grow { flex: 1; }
    }

    .hint {
      display: flex; align-items: flex-start; gap: 8px;
      margin: 8px 0 0; padding: 12px; background: #fafafa;
      border-left: 3px solid #cbd5e1; border-radius: 6px;
      font-size: 12px; color: #666;
      mat-icon { font-size: 16px; width: 16px; height: 16px; color: #888; }
    }
  `]
})
export class FamilyComponent implements OnInit {
  groups: GroupView[] = [];
  loading = false;
  creating = false;
  newGroupName = '';
  readonly roles = ROLES;

  constructor(private family: FamilyService, private snack: MatSnackBar) {}

  ngOnInit(): void { this.refresh(); }

  refresh(): void {
    this.loading = true;
    this.family.getGroups().subscribe({
      next: (gs) => { this.groups = gs.map(g => ({ ...g, expanded: false, newMemberRole: 'CAREGIVER' })); this.loading = false; },
      error: () => { this.groups = []; this.loading = false; }
    });
  }

  createGroup(): void {
    const name = this.newGroupName.trim();
    if (!name || this.creating) return;
    this.creating = true;
    this.family.createGroup({ name }).subscribe({
      next: (g) => {
        this.groups.unshift({ ...g, expanded: true, members: [], newMemberRole: 'CAREGIVER' });
        this.newGroupName = '';
        this.creating = false;
        this.snack.open('Family group created.', 'Close', { duration: 2000 });
      },
      error: (err) => {
        this.creating = false;
        this.snack.open(err?.error?.message || 'Failed to create group', 'Close', { duration: 4000 });
      }
    });
  }

  toggleGroup(g: GroupView): void {
    g.expanded = !g.expanded;
    if (g.expanded && !g.members && g.id) {
      g.loadingMembers = true;
      this.family.getMembers(g.id).subscribe({
        next: (ms) => { g.members = ms; g.loadingMembers = false; },
        error: () => { g.members = []; g.loadingMembers = false; }
      });
    }
  }

  addMember(g: GroupView): void {
    if (!g.id || !g.newMemberId) return;
    const member: FamilyMember = {
      userId: g.newMemberId.trim(),
      role: g.newMemberRole || 'CAREGIVER'
    };
    this.family.addMember(g.id, member).subscribe({
      next: (m) => {
        g.members = [...(g.members || []), m];
        g.newMemberId = '';
        this.snack.open('Member added.', 'Close', { duration: 2000 });
      },
      error: (err) => {
        this.snack.open(err?.error?.message || 'Failed to add member', 'Close', { duration: 4000 });
      }
    });
  }

  removeMember(g: GroupView, m: FamilyMember): void {
    if (!g.id || !m.id) return;
    this.family.removeMember(g.id, m.id).subscribe({
      next: () => {
        g.members = (g.members || []).filter(x => x.id !== m.id);
        this.snack.open('Member removed.', 'Close', { duration: 2000 });
      },
      error: (err) => {
        this.snack.open(err?.error?.message || 'Failed to remove member', 'Close', { duration: 4000 });
      }
    });
  }

  shortId(uuid: string): string {
    if (!uuid) return '';
    return uuid.length > 12 ? uuid.slice(0, 8) + '…' + uuid.slice(-4) : uuid;
  }

  roleColor(role?: string): string {
    return ROLE_COLORS[(role || 'VIEWER').toUpperCase()] || '#9e9e9e';
  }
}
