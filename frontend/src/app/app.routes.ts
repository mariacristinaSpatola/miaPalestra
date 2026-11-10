import { Routes } from '@angular/router';
import { HomePage } from './pages/home.page';
import { LoginPage } from './pages/login.page';
import { DashboardPage } from './pages/dashboard.page';

export const routes: Routes = [
	{ path: '', component: HomePage },
	{ path: 'accedi', component: LoginPage },
	{ path: 'gestione', component: DashboardPage },
	{ path: '**', redirectTo: '' },
];
