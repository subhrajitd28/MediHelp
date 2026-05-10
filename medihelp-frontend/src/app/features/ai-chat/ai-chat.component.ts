import { Component, ElementRef, OnInit, ViewChild, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatListModule } from '@angular/material/list';
import { MatMenuModule } from '@angular/material/menu';
import {
  ChatbotService,
  ChatbotResponse,
  SeverityInfo,
  SessionSummary,
  CheckinPayload,
  MealPlan,
  MealItem
} from '../../core/services/chatbot.service';
import { ProfileService } from '../../core/services/profile.service';
import { AuthService } from '../../core/services/auth.service';
import { MarkdownPipe } from '../../shared/pipes/markdown.pipe';
import { SeverityModalComponent } from '../../shared/components/severity-modal/severity-modal.component';

interface ChatMessage {
  role: 'user' | 'assistant';
  text: string;
  attachment?: 'voice' | 'image';
  attachmentUrl?: string;
  severity?: SeverityInfo;
  disease?: string;
  mealPlan?: MealPlan;
  loadingMealPlan?: boolean;
  timestamp: Date;
}

/**
 * Unified conversational chatbot UI — replaces the old tabbed (Symptom / Diet
 * / Exercise) interface with a single thread that calls the RAG-powered
 * medihelp-chatbot-service. Supports text, voice (MediaRecorder), image upload,
 * severity modal for CRITICAL replies, and a sessions sidebar.
 */
@Component({
  selector: 'app-ai-chat',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTooltipModule,
    MatDialogModule,
    MatListModule,
    MatMenuModule,
    MarkdownPipe
  ],
  templateUrl: './ai-chat.component.html',
  styleUrl: './ai-chat.component.scss'
})
export class AiChatComponent implements OnInit, AfterViewChecked {
  messages: ChatMessage[] = [];
  userInput = '';
  loading = false;
  sessionId: string | undefined;

  // Sidebar
  sessions: SessionSummary[] = [];
  showSidebar = true;

  // Voice
  recording = false;
  private mediaRecorder?: MediaRecorder;
  private audioChunks: Blob[] = [];

  // Check-in
  checkin: CheckinPayload | null = null;

  @ViewChild('chatScroll') private chatScroll!: ElementRef<HTMLDivElement>;
  @ViewChild('imageInput') private imageInput!: ElementRef<HTMLInputElement>;

  // Cached state pulled from /users/me — used to drive cultural advice region
  private userState = '';

  constructor(
    private chatbot: ChatbotService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog,
    private profile: ProfileService,
    private auth: AuthService
  ) {}

  ngOnInit(): void {
    this.loadSessions();
    this.checkReturningUser();
    this.profile.getProfile().subscribe({
      next: (p) => { this.userState = p.state || 'India'; },
      error: () => { this.userState = 'India'; }
    });
  }

  // Fetches cultural-adapted Indian food suggestions for the diagnosed disease.
  // Backend uses Groq's food_suggestion_chain — disease + region + macros → meal plan JSON.
  fetchMealPlan(msg: ChatMessage): void {
    if (!msg.disease || msg.loadingMealPlan) return;
    const userId = this.auth.getUserId();
    if (!userId) return;
    msg.loadingMealPlan = true;
    this.chatbot.getFoodSuggestions(userId, this.userState, this.sessionId).subscribe({
      next: (plan) => {
        msg.mealPlan = plan;
        msg.loadingMealPlan = false;
      },
      error: (err) => {
        msg.loadingMealPlan = false;
        const m = err?.error?.error || 'Could not fetch food suggestions';
        this.snackBar.open(m, 'Close', { duration: 4000 });
      }
    });
  }

  mealEntries(plan: MealPlan): { name: string; items: MealItem[]; cal?: string }[] {
    return Object.entries(plan.meal_plan || {}).map(([name, m]) => ({
      name,
      items: (m as any).items || [],
      cal: (m as any).target_calories
    }));
  }

  // Render one meal item as a single line. The LLM emits structured objects
  // ({food, quantity_g, carbs_g, protein_g, fat_g, note}); collapsing to text
  // keeps the chat bubble compact while preserving all useful info.
  formatMealItem(item: MealItem): string {
    if (!item || typeof item === 'string') return String(item);
    const parts: string[] = [];
    if (item.food) parts.push(item.food);
    if (item.quantity_g) parts.push(`${item.quantity_g}g`);
    return parts.join(' · ') || JSON.stringify(item);
  }

  ngAfterViewChecked(): void {
    if (this.chatScroll) {
      const el = this.chatScroll.nativeElement;
      el.scrollTop = el.scrollHeight;
    }
  }

  // ── Text ────────────────────────────────────────────────────────────────
  sendText(): void {
    const text = this.userInput.trim();
    if (!text || this.loading) return;
    this.pushUser(text);
    this.userInput = '';
    this.loading = true;
    this.chatbot.sendText(text, this.sessionId).subscribe({
      next: (res) => this.handleResponse(res),
      error: (err) => this.handleError(err)
    });
  }

  // ── Voice via MediaRecorder ─────────────────────────────────────────────
  async toggleRecording(): Promise<void> {
    if (this.recording) {
      this.mediaRecorder?.stop();
      return;
    }
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      this.mediaRecorder = new MediaRecorder(stream, { mimeType: 'audio/webm' });
      this.audioChunks = [];
      this.mediaRecorder.ondataavailable = (e) => this.audioChunks.push(e.data);
      this.mediaRecorder.onstop = () => {
        stream.getTracks().forEach(t => t.stop());
        this.recording = false;
        const blob = new Blob(this.audioChunks, { type: 'audio/webm' });
        this.uploadVoice(blob);
      };
      this.mediaRecorder.start();
      this.recording = true;
    } catch (err) {
      this.snackBar.open('Microphone access denied', 'Close', { duration: 3000 });
    }
  }

  private uploadVoice(blob: Blob): void {
    this.pushUser('🎙️ Voice message', { attachment: 'voice', attachmentUrl: URL.createObjectURL(blob) });
    this.loading = true;
    this.chatbot.sendVoice(blob, this.sessionId).subscribe({
      next: (res) => {
        if (res.transcript) {
          // Replace placeholder with transcript so user sees what was heard
          const last = [...this.messages].reverse().find(m => m.role === 'user');
          if (last) last.text = `🎙️ ${res.transcript}`;
        }
        this.handleResponse(res);
      },
      error: (err) => this.handleError(err)
    });
  }

  // ── Image upload ─────────────────────────────────────────────────────────
  triggerImagePick(): void { this.imageInput.nativeElement.click(); }

  onImagePicked(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    this.pushUser(`📷 ${file.name}`, { attachment: 'image', attachmentUrl: URL.createObjectURL(file) });
    this.loading = true;
    this.chatbot.sendImage(file, undefined, this.sessionId).subscribe({
      next: (res) => this.handleResponse(res),
      error: (err) => this.handleError(err)
    });
    (event.target as HTMLInputElement).value = '';
  }

  // ── Sessions sidebar ─────────────────────────────────────────────────────
  loadSessions(): void {
    this.chatbot.listSessions().subscribe({
      next: (res) => this.sessions = res.sessions,
      error: () => { /* sidebar stays empty for first-time users */ }
    });
  }

  openSession(session: SessionSummary): void {
    this.sessionId = session.session_id;
    this.chatbot.loadSession(session.session_id).subscribe({
      next: (res) => {
        this.messages = res.messages.map(m => ({
          role: m.role,
          text: m.content,
          timestamp: new Date(m.ts)
        }));
        // Re-attach the diagnosed disease + persisted meal plan to the most
        // recent assistant bubble so the cultural-food card reappears on
        // history load (without this, the *ngIf="msg.disease" guard hides it).
        if (res.last_disease) {
          for (let i = this.messages.length - 1; i >= 0; i--) {
            if (this.messages[i].role === 'assistant') {
              this.messages[i].disease = res.last_disease;
              if (res.meal_plan) this.messages[i].mealPlan = res.meal_plan;
              break;
            }
          }
        }
      }
    });
  }

  newChat(): void {
    this.messages = [];
    this.sessionId = undefined;
    this.checkin = null;
  }

  deleteSession(session: SessionSummary, event: Event): void {
    event.stopPropagation();
    this.chatbot.deleteSession(session.session_id).subscribe({
      next: () => {
        this.sessions = this.sessions.filter(s => s.session_id !== session.session_id);
        if (this.sessionId === session.session_id) this.newChat();
      }
    });
  }

  // ── Returning-user check-in ──────────────────────────────────────────────
  checkReturningUser(): void {
    this.chatbot.getCheckin().subscribe({
      next: (res) => { if (res.show) this.checkin = res; },
      error: () => { /* first-time user — endpoint may 404 */ }
    });
  }

  submitCheckin(reply: string): void {
    if (!this.checkin || !reply.trim()) return;
    const payload = this.checkin;
    this.checkin = null;
    this.loading = true;
    this.pushUser(`📝 ${reply}`);
    this.chatbot.submitCheckinReply(reply, payload, this.sessionId).subscribe({
      next: (res) => {
        this.sessionId = res.session_id;
        this.messages.push({
          role: 'assistant',
          text: `${res.icon} **${res.label}**\n\n${res.assessment}\n\n_${res.advice}_`,
          timestamp: new Date()
        });
        this.loading = false;
        this.loadSessions();
      },
      error: (err) => this.handleError(err)
    });
  }

  dismissCheckin(): void { this.checkin = null; }

  // ── Internal helpers ─────────────────────────────────────────────────────
  private pushUser(text: string, extras: Partial<ChatMessage> = {}): void {
    this.messages.push({ role: 'user', text, timestamp: new Date(), ...extras });
  }

  private handleResponse(res: ChatbotResponse): void {
    this.sessionId = res.session_id;
    this.messages.push({
      role: 'assistant',
      text: res.reply,
      severity: res.severity,
      disease: res.disease,
      timestamp: new Date()
    });
    this.loading = false;
    this.loadSessions();
    if (res.severity?.show_alert) {
      this.dialog.open(SeverityModalComponent, {
        data: res.severity,
        panelClass: 'critical-dialog',
        disableClose: false
      });
    }
  }

  private handleError(err: any): void {
    this.loading = false;
    const msg = err?.error?.error || err?.error?.message || 'The assistant is unavailable. Please try again.';
    this.snackBar.open(msg, 'Close', { duration: 5000 });
    this.messages.push({ role: 'assistant', text: `_Error: ${msg}_`, timestamp: new Date() });
  }

  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendText();
    }
  }

  severityClass(s?: SeverityInfo): string {
    if (!s) return '';
    return 'sev-' + s.severity.toLowerCase();
  }
}
