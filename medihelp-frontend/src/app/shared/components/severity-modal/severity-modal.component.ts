import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { SeverityInfo } from '../../../core/services/chatbot.service';

/**
 * Emergency modal triggered automatically when the chatbot returns a CRITICAL
 * severity. Mirrors the Flask UI's red modal: 108 call button + Google-Maps
 * "hospital near me" link + an option for the user to dismiss after acknowledging.
 *
 * For URGENT/MODERATE/MILD the chat just renders an inline severity bar; this
 * modal is reserved for CRITICAL only (severity.show_alert === true).
 */
@Component({
  selector: 'app-severity-modal',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MatDialogModule],
  template: `
    <div class="severity-modal" [style.--accent]="data.alert_color">
      <div class="header">
        <span class="icon">{{ data.alert_icon }}</span>
        <h2>{{ data.alert_title }}</h2>
      </div>

      <div class="body">
        <p class="message">{{ data.alert_message }}</p>
        @if (data.reason) {
          <p class="reason"><strong>Why:</strong> {{ data.reason }}</p>
        }
      </div>

      <div class="actions">
        @if (data.emergency_call) {
          <a mat-flat-button color="warn" [href]="'tel:' + data.emergency_call" class="call-btn">
            <mat-icon>call</mat-icon>
            Call {{ data.emergency_call }} (Emergency)
          </a>
        }
        <button mat-stroked-button type="button"
                (click)="findHospital()"
                [disabled]="locating">
          <mat-icon>local_hospital</mat-icon>
          @if (locating) {
            Locating you…
          } @else {
            Find nearest hospital
          }
        </button>
        @if (locationError) {
          <p class="loc-note">{{ locationError }}</p>
        }
        <button mat-button (click)="close()">I understand</button>
      </div>

      <p class="disclaimer">
        This is a pre-screening alert based on the symptoms you described.
        It is NOT a substitute for professional medical evaluation.
      </p>
    </div>
  `,
  styles: [`
    .severity-modal {
      max-width: 480px;
      padding: 8px 16px 16px;
      border-top: 6px solid var(--accent, #ff1744);
    }
    .header {
      display: flex;
      align-items: center;
      gap: 12px;
      margin: 8px 0 16px;
    }
    .header .icon { font-size: 32px; }
    .header h2 {
      margin: 0;
      font-size: 20px;
      color: var(--accent, #ff1744);
    }
    .body p { margin: 8px 0; font-size: 15px; line-height: 1.5; }
    .reason { color: #555; font-size: 13px; }
    .actions {
      display: flex;
      flex-direction: column;
      gap: 10px;
      margin-top: 16px;
    }
    .call-btn { font-weight: 600; }
    .disclaimer {
      margin-top: 16px;
      padding-top: 12px;
      border-top: 1px solid #eee;
      font-size: 12px;
      color: #888;
    }
    .loc-note {
      margin: -4px 0 0;
      font-size: 12px;
      color: #c62828;
    }
  `]
})
export class SeverityModalComponent {
  locating = false;
  locationError: string | null = null;

  constructor(
    public dialogRef: MatDialogRef<SeverityModalComponent>,
    @Inject(MAT_DIALOG_DATA) public data: SeverityInfo
  ) {}

  close(): void { this.dialogRef.close(); }

  /**
   * Open Google Maps zoomed to the user's actual coordinates so they see
   * hospitals *around them*, not a generic "near me" search that depends on
   * Google's IP-geolocation guess. Falls back to the backend's coarse
   * hospital_url if permission is denied, geolocation isn't available, or
   * the request times out.
   *
   * Trade-off: enableHighAccuracy=false + 5s timeout chosen on purpose —
   * in an emergency, a quick fix on a WiFi/IP fix beats a slow GPS lock.
   */
  findHospital(): void {
    this.locationError = null;

    const openCoarse = () => window.open(this.data.hospital_url, '_blank', 'noopener');

    if (!('geolocation' in navigator)) {
      openCoarse();
      return;
    }

    this.locating = true;
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        this.locating = false;
        const { latitude, longitude } = pos.coords;
        const url = `https://www.google.com/maps/search/hospital/@${latitude},${longitude},15z`;
        window.open(url, '_blank', 'noopener');
      },
      (err) => {
        this.locating = false;
        this.locationError =
          err.code === err.PERMISSION_DENIED
            ? 'Location permission denied — opening generic search.'
            : 'Could not get location — opening generic search.';
        openCoarse();
      },
      { enableHighAccuracy: false, timeout: 5000, maximumAge: 60_000 }
    );
  }
}
