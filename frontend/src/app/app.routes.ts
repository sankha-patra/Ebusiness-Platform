import { Routes } from '@angular/router';
import { OrdersComponent } from './orders/orders';
import { ProductsComponent } from './products/products';
import { PaymentsComponent } from './payments/payments';
import { NotificationsComponent } from './notifications/notifications';

export const routes: Routes = [
  { path: '', redirectTo: '/orders', pathMatch: 'full' },
  { path: 'orders', component: OrdersComponent },
  { path: 'products', component: ProductsComponent },
  { path: 'payments', component: PaymentsComponent },
  { path: 'notifications', component: NotificationsComponent },
];
