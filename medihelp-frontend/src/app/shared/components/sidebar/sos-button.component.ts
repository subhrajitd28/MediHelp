import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { SosService } from '../../../core/services/sos.service';

/**
 * Persistent emergency SOS button in the sidebar. One tap opens a confirm
 * dialog (prevents accidental triggers); on confirm, asks for browser
 * geolocation, fires backend POST /users/me/sos, and shows a toast with
 * how many contacts were notified.
 */
@Component({
  selector: 'app-sos-button',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MatDialogModule, MatSnackBarModule],
  template: `
    <button mat-flat-button class="sos-btn" (click)="confirm()" [disabled]="busy">
      <mat-icon>sos</mat-icon>
      <span>{{ busy ? 'Sending…' : 'Emergency SOS' }}</span>
    </button>
  `,
  styles: [`
    .sos-btn {
      width: 100%;
      background: #d32f2f;
      color: #fff;
      font-weight: 600;
      border-radius: 8px;
      padding: 8px;
      letter-spacing: 0.04em;
    }
    .sos-btn:hover { background: #b71c1c; }
    .sos-btn[disabled] { background: #888; color: #fff; }
    mat-icon { vertical-align: middle; margin-right: 6px; }
  `]
})
export class SosButtonComponent {
  busy = false;

  constructor(private dialog: MatDialog, private sos: SosService, private snack: MatSnackBar) {}

  confirm(): void {
    const ref = this.dialog.open(SosConfirmDialog, { width: '380px' });
    ref.afterClosed().subscribe((ok) => { if (ok) this.fire(); });
  }

  private fire(): void {
    this.busy = true;
    const finish = (lat?: number, lng?: number) => {
      this.sos.trigger(lat, lng).subscribe({
        next: (res) => {
          this.busy = false;
          this.snack.open(
            res.message + (res.contactsNotified ? ` — ${res.contactsNotified} contact(s) notified` : ''),
            'Close',
            { duration: 6000, panelClass: 'sos-snack' }
          );
        },
        error: () => {
          this.busy = false;
          this.snack.open('SOS failed — please call 108 directly.', 'Close', { duration: 6000 });
        }
      });
    };

    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (p) => finish(p.coords.latitude, p.coords.longitude),
        () => finish(),
        { timeout: 5000 }
      );
    } else {
      finish();
    }
  }
}

@Component({
  standalone: true,
  imports: [MatButtonModule, MatIconModule, MatDialogModule],
  template: `
    <div style="padding:16px">
      <h2 style="margin:0 0 8px;color:#d32f2f">
        <mat-icon style="vertical-align:middle">warning</mat-icon> Trigger Emergency SOS?
      </h2>
      <p>This will alert all your emergency contacts with your current location and a link to your medical summary.</p>
      <div style="display:flex;justify-content:flex-end;gap:8px;margin-top:16px">
        <button mat-button (click)="ref.close(false)">Cancel</button>
        <button mat-flat-button color="warn" (click)="ref.close(true)">Send Alert</button>
      </div>
    </div>
  `
})
export class SosConfirmDialog {
  constructor(public ref: MatDialogRef<SosConfirmDialog>) {}
}
