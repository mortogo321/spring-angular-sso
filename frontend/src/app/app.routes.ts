import { Routes } from '@angular/router';

import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/home/home').then((m) => m.Home),
  },
  {
    path: 'tasks',
    loadComponent: () => import('./features/tasks/tasks-list/tasks-list').then((m) => m.TasksList),
    canActivate: [authGuard],
    data: { role: 'user' },
  },
  {
    path: 'profile',
    loadComponent: () => import('./features/profile/profile').then((m) => m.Profile),
    canActivate: [authGuard],
  },
  {
    path: 'forbidden',
    loadComponent: () => import('./features/forbidden/forbidden').then((m) => m.Forbidden),
  },
  {
    path: '**',
    redirectTo: '',
  },
];
