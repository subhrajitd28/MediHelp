import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, NavigationEnd } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subscription, filter } from 'rxjs';

/**
 * Persistent "Ask AI" floating action button — bottom-right, IRCTC AskDISHA style.
 * Hidden on /ai-chat itself (would be redundant), and on unauthenticated routes
 * because it's mounted inside the authenticated branch of app.component.html.
 */
@Component({
  selector: 'app-chatbot-fab',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatTooltipModule],
  template: `
    @if (visible) {
      <button class="ai-fab"
              type="button"
              matTooltip="Ask the AI Health Assistant"
              matTooltipPosition="left"
              (click)="open()">
        <span class="pulse-ring"></span>
        <mat-icon class="fab-icon">smart_toy</mat-icon>
      </button>
    }
  `,
  styles: [`
    .ai-fab {
      position: fixed;
      right: 24px;
      bottom: 24px;
      width: 60px;
      height: 60px;
      border-radius: 50%;
      border: none;
      cursor: pointer;
      background: linear-gradient(135deg, #6750a4 0%, #3f51b5 100%);
      box-shadow: 0 6px 20px rgba(63, 81, 181, 0.4);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
      transition: transform 150ms ease, box-shadow 150ms ease;
    }
    .ai-fab:hover {
      transform: translateY(-3px) scale(1.05);
      box-shadow: 0 10px 28px rgba(63, 81, 181, 0.55);
    }
    .ai-fab:active {
      transform: translateY(-1px) scale(1.02);
    }
    .fab-icon {
      color: #fff;
      font-size: 30px;
      width: 30px;
      height: 30px;
      z-index: 2;
    }
    /* Subtle pulse ring to draw the eye, without being annoying */
    .pulse-ring {
      position: absolute;
      inset: 0;
      border-radius: 50%;
      border: 3px solid rgba(103, 80, 164, 0.5);
      animation: pulse 2.2s ease-out infinite;
    }
    @keyframes pulse {
      0%   { transform: scale(1);   opacity: 0.6; }
      80%  { transform: scale(1.4); opacity: 0;   }
      100% { transform: scale(1.4); opacity: 0;   }
    }
    @media (max-width: 600px) {
      .ai-fab { right: 16px; bottom: 16px; width: 54px; height: 54px; }
      .fab-icon { font-size: 26px; width: 26px; height: 26px; }
    }
  `]
})
export class ChatbotFabComponent implements OnInit, OnDestroy {
  visible = true;
  private sub?: Subscription;

  constructor(private router: Router) {}

  ngOnInit(): void {
    this.updateVisibility(this.router.url);
    this.sub = this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe((e: any) => this.updateVisibility(e.urlAfterRedirects || e.url));
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  open(): void {
    this.router.navigateByUrl('/ai-chat');
  }

  private updateVisibility(url: string): void {
    this.visible = !url.startsWith('/ai-chat');
  }
}
