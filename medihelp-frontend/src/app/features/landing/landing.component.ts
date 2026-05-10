import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';

interface Feature {
  icon: string;
  title: string;
  desc: string;
  hue: 'red' | 'blue' | 'green' | 'orange' | 'purple' | 'teal' | 'pink' | 'amber' | 'indigo';
}

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, RouterModule, MatButtonModule, MatIconModule, MatCardModule],
  templateUrl: './landing.component.html',
  styleUrl: './landing.component.scss'
})
export class LandingComponent {
  readonly features: Feature[] = [
    {
      icon: 'monitor_heart',
      title: 'Vitals Tracking',
      desc: 'Heart rate, blood pressure, glucose, SpO₂, temperature. Real-time anomaly detection via Welford\'s algorithm.',
      hue: 'red',
    },
    {
      icon: 'smart_toy',
      title: 'AI Health Chat',
      desc: 'RAG-powered chatbot over medical PDFs (Pinecone + Groq Llama-3.3). Voice + image + text input.',
      hue: 'blue',
    },
    {
      icon: 'medication',
      title: 'Medications + Reminders',
      desc: 'OCR-scan prescriptions, auto-schedule reminders, OpenFDA drug-interaction warnings.',
      hue: 'orange',
    },
    {
      icon: 'mood',
      title: 'Mood Journal',
      desc: 'Daily mental-health check-in with AES-256 encrypted notes. See correlation with sleep + exercise.',
      hue: 'pink',
    },
    {
      icon: 'family_restroom',
      title: 'Family Health Hub',
      desc: 'Manage parents, kids, and dependents with role-based access (Owner / Caregiver / Viewer).',
      hue: 'teal',
    },
    {
      icon: 'restaurant',
      title: 'Cultural Diet Plans',
      desc: 'Personalised meal plans honouring your state\'s cuisine + diet preference (Veg / Non-veg / Vegan).',
      hue: 'amber',
    },
    {
      icon: 'sos',
      title: 'Emergency SOS',
      desc: 'One-tap alert pushes GPS location + medical summary to your emergency contacts.',
      hue: 'red',
    },
    {
      icon: 'cloud_download',
      title: 'FHIR R4 Export',
      desc: 'Download your records as a Bundle any hospital system (Epic, Cerner) can import. Industry-standard LOINC codes.',
      hue: 'indigo',
    },
    {
      icon: 'shield',
      title: 'Privacy First',
      desc: 'JWT auth at the gateway only. Postgres-per-service isolation. Encrypted-at-rest mental-health journals.',
      hue: 'green',
    },
  ];

  readonly stats = [
    { value: '8',     label: 'Microservices' },
    { value: '5,775', label: 'RAG knowledge chunks' },
    { value: '70+',   label: 'REST endpoints' },
    { value: '99%',   label: 'Self-hosted (no cloud DB lock-in)' },
  ];

  readonly steps = [
    {
      n: '1',
      icon: 'app_registration',
      title: 'Sign up in 60 seconds',
      desc: 'Tell us your state, diet, and date of birth — drives age-appropriate, regionally-aware advice.',
    },
    {
      n: '2',
      icon: 'chat',
      title: 'Describe your symptoms',
      desc: 'Type, speak, or upload a prescription photo. The chatbot grounds its reply in real medical PDFs.',
    },
    {
      n: '3',
      icon: 'timeline',
      title: 'Track recovery',
      desc: 'Log vitals + mood daily. Three days later, the assistant checks in with a progress assessment.',
    },
  ];
}
