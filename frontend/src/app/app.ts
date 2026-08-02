import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatBadgeModule } from '@angular/material/badge';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { HttpClient } from '@angular/common/http';
import { environment } from '../environments/environment';

interface NotificationItem {
  notificationId: string;
  orderId: string;
  paymentId: string;
  message: string;
  channel: string;
  read: boolean;
  createdAt: string;
}

@Component({
  selector: 'app-root',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatBadgeModule,
    MatSnackBarModule
  ],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App implements OnInit, OnDestroy {
  private readonly http = inject(HttpClient);
  private readonly snackBar = inject(MatSnackBar);
  private readonly apiUrl = environment.apiUrl;
  private pollTimer: ReturnType<typeof setInterval> | null = null;
  private greetingTimer: ReturnType<typeof setTimeout> | null = null;
  private paymentBlurTimer: ReturnType<typeof setTimeout> | null = null;
  private seenIds = new Set<string>();

  readonly unreadCount = signal(0);
  readonly greeting = signal<string | null>(null);
  readonly paymentBlur = signal(false);

  /** Page blur only for welcome popup or Kafka payment confirmation toast */
  readonly pageBlurred = computed(() => !!this.greeting() || this.paymentBlur());

  private static readonly GREETING_SEEN_KEY = 'ebusiness_greeting_seen';
  private static readonly GREETINGS = [
    'Hey! How is your day?',
    'Good to see you back.',
    'Hope you are having a smooth day.',
    'Hey there — ready when you are.',
    'Take it easy. You have got this.',
    'Nice to have you here.',
    'Hey! Hope things are going well.',
    'Welcome back. Coffee ready?',
  ];

  ngOnInit() {
    this.maybeShowGreeting();
    this.maybeShowDemoStates();
    this.pollNotifications();
    this.pollTimer = setInterval(() => this.pollNotifications(), 4000);
  }

  dismissGreeting() {
    this.greeting.set(null);
    if (this.greetingTimer) {
      clearTimeout(this.greetingTimer);
      this.greetingTimer = null;
    }
  }

  private maybeShowGreeting() {
    const demo = new URLSearchParams(window.location.search).get('demo');
    // ?demo=greeting keeps popup open for docs screenshots
    if (demo === 'greeting') {
      this.showGreeting(App.GREETINGS[0], /*autoDismissMs*/ null);
      return;
    }

    if (sessionStorage.getItem(App.GREETING_SEEN_KEY)) {
      return;
    }
    sessionStorage.setItem(App.GREETING_SEEN_KEY, 'true');

    const message = App.GREETINGS[Math.floor(Math.random() * App.GREETINGS.length)];
    this.showGreeting(message, 4500);
  }

  private showGreeting(message: string, autoDismissMs: number | null) {
    // Show immediately so change detection is not delayed behind setTimeout races
    this.greeting.set(message);
    if (autoDismissMs != null) {
      this.greetingTimer = setTimeout(() => this.dismissGreeting(), autoDismissMs);
    }
  }

  /** Optional UI preview states for docs/screenshots (?demo=greeting|kafka) */
  private maybeShowDemoStates() {
    const demo = new URLSearchParams(window.location.search).get('demo');
    if (demo === 'kafka') {
      this.showPaymentConfirmation({
        notificationId: 'ntf-demo-kafka',
        orderId: 'ord-demo-1001',
        paymentId: 'pay-demo-1001',
        message: 'Your payment was successful. Order ord-demo-1001 is confirmed.',
        channel: 'SMS',
        read: false,
        createdAt: new Date().toISOString()
      }, /*sticky*/ true);
    }
  }

  ngOnDestroy() {
    if (this.pollTimer) {
      clearInterval(this.pollTimer);
    }
    if (this.greetingTimer) {
      clearTimeout(this.greetingTimer);
    }
    if (this.paymentBlurTimer) {
      clearTimeout(this.paymentBlurTimer);
    }
  }

  private pollNotifications() {
    this.http.get<NotificationItem[]>(`${this.apiUrl}/notifications/unread`).subscribe({
      next: (items) => {
        this.unreadCount.set(items.length);
        for (const item of items) {
          if (!this.seenIds.has(item.notificationId)) {
            this.seenIds.add(item.notificationId);
            this.showPaymentConfirmation(item);
            this.http.post(`${this.apiUrl}/notifications/${item.notificationId}/read`, {}).subscribe();
          }
        }
      },
      error: () => {
        /* gateway/backend may be restarting */
      }
    });
  }

  private showPaymentConfirmation(item: NotificationItem, sticky = false) {
    this.paymentBlur.set(true);
    if (this.paymentBlurTimer) {
      clearTimeout(this.paymentBlurTimer);
    }

    const duration = sticky ? 0 : 7000;
    const ref = this.snackBar.open(item.message, 'View', {
      duration,
      horizontalPosition: 'end',
      verticalPosition: 'top',
      panelClass: ['payment-success-snack']
    });

    ref.onAction().subscribe(() => {
      window.location.href = '/notifications';
    });

    ref.afterDismissed().subscribe(() => {
      this.paymentBlur.set(false);
    });

    if (!sticky) {
      // Safety clear if snackbar is dismissed without afterDismissed firing promptly
      this.paymentBlurTimer = setTimeout(() => this.paymentBlur.set(false), 7200);
    }
  }
}
