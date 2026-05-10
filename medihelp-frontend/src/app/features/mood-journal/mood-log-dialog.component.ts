import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MoodEntry } from '../../core/services/mood.service';

@Component({
  selector: 'app-mood-log-dialog',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatDialogModule
  ],
  template: `
    <div class="dialog">
      <h2>How are you feeling?</h2>
      <div class="emojis">
        <button *ngFor="let m of moods" (click)="rating = m"
                [class.selected]="rating === m"
                [attr.aria-label]="'Mood ' + m">
          {{ emoji(m) }}
        </button>
      </div>

      <mat-form-field appearance="outline" class="full">
        <mat-label>Journal note (optional)</mat-label>
        <textarea matInput [(ngModel)]="entry.journalText" rows="3"></textarea>
      </mat-form-field>

      <div class="row">
        <mat-form-field appearance="outline">
          <mat-label>Sleep (hours)</mat-label>
          <input matInput type="number" min="0" max="24" step="0.5" [(ngModel)]="entry.sleepHours">
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Exercise (minutes)</mat-label>
          <input matInput type="number" min="0" max="600" [(ngModel)]="entry.exerciseMinutes">
        </mat-form-field>
      </div>

      <div class="actions">
        <button mat-button (click)="cancel()">Cancel</button>
        <button mat-flat-button color="primary" [disabled]="!rating" (click)="save()">Save</button>
      </div>
    </div>
  `,
  styles: [`
    .dialog { padding: 16px; }
    h2 { margin: 0 0 12px; font-size: 18px; }
    .emojis {
      display: flex;
      justify-content: space-around;
      margin: 16px 0;

      button {
        background: transparent;
        border: 2px solid transparent;
        border-radius: 50%;
        width: 56px;
        height: 56px;
        font-size: 28px;
        cursor: pointer;
        transition: transform 0.1s, border-color 0.1s;
      }
      button:hover { transform: scale(1.1); }
      button.selected {
        border-color: #3f51b5;
        background: #e3edff;
      }
    }
    .full { width: 100%; }
    .row { display: flex; gap: 12px; }
    .row mat-form-field { flex: 1; }
    .actions {
      display: flex;
      justify-content: flex-end;
      gap: 8px;
      margin-top: 8px;
    }
  `]
})
export class MoodLogDialogComponent {
  moods = [1, 2, 3, 4, 5];
  rating: number | null = null;
  entry: MoodEntry = { mood: 0 };

  constructor(private ref: MatDialogRef<MoodLogDialogComponent>) {}

  emoji(m: number): string {
    return ['', '😢', '😞', '😐', '🙂', '😄'][m] || '';
  }

  cancel(): void { this.ref.close(); }

  save(): void {
    if (!this.rating) return;
    this.ref.close({ ...this.entry, mood: this.rating });
  }
}
