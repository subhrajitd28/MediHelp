import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatSelectModule } from '@angular/material/select';
import { MatTabsModule } from '@angular/material/tabs';
import { MatBadgeModule } from '@angular/material/badge';
import {
  AiChatService,
  TriageRequest,
  TriageResponse,
  DietRequest,
  DietResponse,
  ExerciseRequest,
  ExerciseResponse
} from '../../core/services/ai-chat.service';

type TabType = 'symptom' | 'diet' | 'exercise';

interface ChatMessage {
  role: 'user' | 'ai';
  text: string;
  triageData?: TriageResponse;
  dietData?: DietResponse;
  exerciseData?: ExerciseResponse;
  timestamp: Date;
}

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
    MatChipsModule,
    MatSelectModule,
    MatTabsModule,
    MatBadgeModule
  ],
  templateUrl: './ai-chat.component.html',
  styleUrl: './ai-chat.component.scss'
})
export class AiChatComponent {
  activeTab: TabType = 'symptom';
  messages: ChatMessage[] = [];
  userInput = '';
  loading = false;

  // Symptom check optional fields
  age: number | null = null;
  gender = '';

  // Diet/Exercise condition input
  conditionsInput = '';

  genderOptions = ['male', 'female', 'other'];

  constructor(
    private aiChatService: AiChatService,
    private snackBar: MatSnackBar
  ) {}

  switchTab(tab: TabType): void {
    this.activeTab = tab;
    this.messages = [];
    this.userInput = '';
  }

  send(): void {
    if (!this.userInput.trim()) return;

    switch (this.activeTab) {
      case 'symptom':
        this.sendSymptomCheck();
        break;
      case 'diet':
        this.sendDietRequest();
        break;
      case 'exercise':
        this.sendExerciseRequest();
        break;
    }
  }

  private sendSymptomCheck(): void {
    const userText = this.userInput.trim();
    this.addUserMessage(userText);

    const request: TriageRequest = { symptoms: userText };
    if (this.age) request.age = this.age;
    if (this.gender) request.gender = this.gender;

    this.loading = true;
    this.userInput = '';

    this.aiChatService.triageSymptoms(request).subscribe({
      next: (res) => {
        this.messages.push({
          role: 'ai',
          text: '',
          triageData: res,
          timestamp: new Date()
        });
        this.loading = false;
      },
      error: (err) => {
        this.handleError(err);
      }
    });
  }

  private sendDietRequest(): void {
    const userText = this.userInput.trim();
    this.addUserMessage(userText);

    const conditions = this.parseConditions(userText);
    const request: DietRequest = { conditions };

    this.loading = true;
    this.userInput = '';

    this.aiChatService.getDietRecommendations(request).subscribe({
      next: (res) => {
        this.messages.push({
          role: 'ai',
          text: '',
          dietData: res,
          timestamp: new Date()
        });
        this.loading = false;
      },
      error: (err) => {
        this.handleError(err);
      }
    });
  }

  private sendExerciseRequest(): void {
    const userText = this.userInput.trim();
    this.addUserMessage(userText);

    const conditions = this.parseConditions(userText);
    const request: ExerciseRequest = { conditions };

    this.loading = true;
    this.userInput = '';

    this.aiChatService.getExerciseRecommendations(request).subscribe({
      next: (res) => {
        this.messages.push({
          role: 'ai',
          text: '',
          exerciseData: res,
          timestamp: new Date()
        });
        this.loading = false;
      },
      error: (err) => {
        this.handleError(err);
      }
    });
  }

  private addUserMessage(text: string): void {
    this.messages.push({
      role: 'user',
      text,
      timestamp: new Date()
    });
  }

  private parseConditions(text: string): string[] {
    return text.split(',').map(c => c.trim()).filter(c => c.length > 0);
  }

  private handleError(err: any): void {
    this.loading = false;
    const errorMsg = err.error?.detail || err.error?.message || 'Failed to get AI response. Please try again.';
    this.messages.push({
      role: 'ai',
      text: errorMsg,
      timestamp: new Date()
    });
    this.snackBar.open(errorMsg, 'Close', { duration: 5000 });
  }

  getUrgencyColor(urgency: string): string {
    switch (urgency?.toUpperCase()) {
      case 'LOW': return '#4caf50';
      case 'MEDIUM': return '#ff9800';
      case 'HIGH': return '#f44336';
      case 'EMERGENCY': return '#b71c1c';
      default: return '#9e9e9e';
    }
  }

  getUrgencyClass(urgency: string): string {
    return 'urgency-' + (urgency?.toLowerCase() || 'unknown');
  }

  getInputPlaceholder(): string {
    switch (this.activeTab) {
      case 'symptom':
        return 'Describe your symptoms...';
      case 'diet':
        return 'Enter conditions (comma-separated, e.g. diabetes, hypertension)...';
      case 'exercise':
        return 'Enter conditions (comma-separated, e.g. back pain, obesity)...';
    }
  }

  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.send();
    }
  }

  getRecommendationTitle(rec: any): string {
    return rec.name || rec.title || rec.type || 'Recommendation';
  }

  getRecommendationDescription(rec: any): string {
    return rec.description || rec.details || rec.summary || JSON.stringify(rec);
  }
}
