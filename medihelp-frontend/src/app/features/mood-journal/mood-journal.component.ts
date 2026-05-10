import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MoodService, MoodEntry } from '../../core/services/mood.service';
import { MoodLogDialogComponent } from './mood-log-dialog.component';

interface CalendarCell {
  date: Date;
  iso: string;
  inMonth: boolean;
  entry?: MoodEntry;
}

const MOOD_EMOJI = ['', '😢', '😞', '😐', '🙂', '😄'];
const MOOD_COLOR = ['', '#d32f2f', '#f57c00', '#fdd835', '#7cb342', '#43a047'];

/**
 * Mental-health track from the original brief — backed by the existing mood
 * MoodController on health-service. Renders a calendar of the past 6 weeks
 * with mood-coloured cells, a "Log mood" button that opens a Material dialog,
 * and a simple correlation summary.
 *
 * Encryption: the backend stores journalText AES-256 (existing implementation),
 * so even raw DB access can't read entries.
 */
@Component({
  selector: 'app-mood-journal',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    MatDialogModule,
    MatTooltipModule
  ],
  template: `
    <div class="mood-page">
      <header class="page-header">
        <div>
          <h1>Mood Journal</h1>
          <p>Daily mental-health check-in. Encrypted at rest. Only you can read it.</p>
        </div>
        <button mat-flat-button color="primary" (click)="openLog()">
          <mat-icon>add</mat-icon> Log today
        </button>
      </header>

      <mat-card class="legend-card">
        <span *ngFor="let i of [1,2,3,4,5]">
          <span class="dot" [style.background]="moodColor(i)"></span>
          {{ moodEmoji(i) }} {{ moodLabel(i) }}
        </span>
      </mat-card>

      <mat-card class="calendar-card">
        <div class="calendar-grid">
          <div class="day-name" *ngFor="let d of dayNames">{{ d }}</div>
          <div *ngFor="let cell of calendar"
               class="day-cell"
               [class.dim]="!cell.inMonth"
               [class.today]="isToday(cell.date)"
               [style.background]="cell.entry ? moodColor(cell.entry.mood) + '33' : 'transparent'"
               [matTooltip]="cell.entry ? tooltipFor(cell) : (cell.iso + ' — no entry')">
            <div class="date-num">{{ cell.date.getDate() }}</div>
            <div class="emoji" *ngIf="cell.entry">{{ moodEmoji(cell.entry.mood) }}</div>
          </div>
        </div>
      </mat-card>

      <mat-card class="insight-card" *ngIf="entries.length >= 5">
        <h3><mat-icon>insights</mat-icon> Insights</h3>
        <p *ngIf="exerciseCorrelation !== null">
          On days you exercised <strong>30+ min</strong>, your average mood was
          <strong>{{ exerciseCorrelation.withExercise.toFixed(1) }} / 5</strong>
          vs <strong>{{ exerciseCorrelation.withoutExercise.toFixed(1) }} / 5</strong>
          on other days.
        </p>
        <p *ngIf="sleepCorrelation !== null">
          With <strong>7+ hours of sleep</strong> your mood averaged
          <strong>{{ sleepCorrelation.withSleep.toFixed(1) }} / 5</strong>
          ({{ sleepCorrelation.withoutSleep.toFixed(1) }} otherwise).
        </p>
        <p class="muted">Based on your last {{ entries.length }} entries.</p>
      </mat-card>
    </div>
  `,
  styles: [`
    .mood-page { padding: 24px; max-width: 1100px; margin: 0 auto; }
    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;

      h1 { margin: 0; font-size: 24px; }
      p { margin: 4px 0 0; color: #666; font-size: 13px; }
    }

    .legend-card {
      padding: 12px 16px;
      margin-bottom: 16px;
      display: flex;
      gap: 24px;
      flex-wrap: wrap;
      font-size: 13px;
    }
    .dot {
      display: inline-block;
      width: 10px;
      height: 10px;
      border-radius: 50%;
      margin-right: 6px;
      vertical-align: middle;
    }

    .calendar-card { padding: 16px; }

    .calendar-grid {
      display: grid;
      grid-template-columns: repeat(7, 1fr);
      gap: 6px;
    }

    .day-name {
      text-align: center;
      font-size: 11px;
      color: #888;
      text-transform: uppercase;
      letter-spacing: 0.04em;
    }

    .day-cell {
      aspect-ratio: 1;
      border: 1px solid #e7ebef;
      border-radius: 8px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      cursor: default;
      transition: transform 0.1s;

      &.dim { opacity: 0.35; }
      &.today { border-color: #3f51b5; box-shadow: 0 0 0 2px #3f51b522; }

      &:hover { transform: scale(1.04); }
    }

    .date-num {
      font-size: 11px;
      color: #555;
    }

    .emoji {
      font-size: 22px;
      line-height: 1;
    }

    .insight-card {
      padding: 16px;
      margin-top: 16px;

      h3 {
        margin: 0 0 8px;
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 16px;
      }

      .muted { color: #888; font-size: 12px; margin-top: 12px; }
    }
  `]
})
export class MoodJournalComponent implements OnInit {
  entries: MoodEntry[] = [];
  calendar: CalendarCell[] = [];
  dayNames = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
  exerciseCorrelation: { withExercise: number; withoutExercise: number } | null = null;
  sleepCorrelation: { withSleep: number; withoutSleep: number } | null = null;

  constructor(
    private moodService: MoodService,
    private dialog: MatDialog,
    private snack: MatSnackBar
  ) {}

  ngOnInit(): void { this.refresh(); }

  refresh(): void {
    const from = new Date();
    from.setDate(from.getDate() - 41);   // 6-week window
    this.moodService.list(from.toISOString()).subscribe({
      next: (entries) => {
        this.entries = entries;
        this.buildCalendar();
        this.computeCorrelations();
      },
      error: () => { /* first-time user — empty calendar */
        this.buildCalendar();
      }
    });
  }

  private buildCalendar(): void {
    const cells: CalendarCell[] = [];
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    // Walk back to start of the week 5 weeks ago (so we show 6 full weeks ending today)
    const start = new Date(today);
    start.setDate(start.getDate() - 41);
    while (start.getDay() !== 0) start.setDate(start.getDate() - 1);

    const byIso = new Map<string, MoodEntry>();
    for (const e of this.entries) {
      if (!e.recordedAt) continue;
      const iso = new Date(e.recordedAt).toISOString().slice(0, 10);
      // Keep the latest entry per day
      byIso.set(iso, e);
    }

    for (let i = 0; i < 42; i++) {
      const d = new Date(start);
      d.setDate(start.getDate() + i);
      const iso = d.toISOString().slice(0, 10);
      cells.push({
        date: new Date(d),
        iso,
        inMonth: d.getMonth() === today.getMonth(),
        entry: byIso.get(iso)
      });
    }
    this.calendar = cells;
  }

  private computeCorrelations(): void {
    const recent = this.entries.slice(-30);
    const ex = recent.filter(e => (e.exerciseMinutes ?? 0) >= 30);
    const noEx = recent.filter(e => (e.exerciseMinutes ?? 0) < 30);
    this.exerciseCorrelation = (ex.length >= 2 && noEx.length >= 2) ? {
      withExercise: ex.reduce((a, b) => a + b.mood, 0) / ex.length,
      withoutExercise: noEx.reduce((a, b) => a + b.mood, 0) / noEx.length
    } : null;

    const slept = recent.filter(e => (e.sleepHours ?? 0) >= 7);
    const tired = recent.filter(e => (e.sleepHours ?? 0) < 7);
    this.sleepCorrelation = (slept.length >= 2 && tired.length >= 2) ? {
      withSleep: slept.reduce((a, b) => a + b.mood, 0) / slept.length,
      withoutSleep: tired.reduce((a, b) => a + b.mood, 0) / tired.length
    } : null;
  }

  openLog(): void {
    const ref = this.dialog.open(MoodLogDialogComponent, {
      width: '420px',
      panelClass: 'mood-dialog'
    });
    ref.afterClosed().subscribe((entry?: MoodEntry) => {
      if (!entry) return;
      this.moodService.add(entry).subscribe({
        next: () => {
          this.snack.open('Mood logged', 'Close', { duration: 2000 });
          this.refresh();
        },
        error: (err) => {
          this.snack.open(err?.error?.message || 'Failed to log mood', 'Close', { duration: 4000 });
        }
      });
    });
  }

  isToday(d: Date): boolean {
    const t = new Date();
    return d.toDateString() === t.toDateString();
  }

  moodEmoji(m?: number): string { return MOOD_EMOJI[m ?? 0] || ''; }
  moodColor(m?: number): string { return MOOD_COLOR[m ?? 0] || '#e0e0e0'; }
  moodLabel(m: number): string {
    return ['', 'Very low', 'Low', 'Neutral', 'Good', 'Great'][m] || '';
  }

  tooltipFor(cell: CalendarCell): string {
    if (!cell.entry) return cell.iso;
    const m = cell.entry;
    const parts = [`${cell.iso}: ${this.moodLabel(m.mood)} ${this.moodEmoji(m.mood)}`];
    if (m.sleepHours) parts.push(`${m.sleepHours}h sleep`);
    if (m.exerciseMinutes) parts.push(`${m.exerciseMinutes} min exercise`);
    if (m.journalText) parts.push(m.journalText.substring(0, 60));
    return parts.join(' · ');
  }
}
