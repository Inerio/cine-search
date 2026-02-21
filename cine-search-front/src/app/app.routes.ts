import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./components/home/home.component').then(m => m.HomeComponent)
  },
  {
    path: 'search',
    loadComponent: () => import('./components/search/search.component').then(m => m.SearchComponent)
  },
  {
    path: 'movie/:id',
    loadComponent: () => import('./components/movie-detail/movie-detail.component').then(m => m.MovieDetailComponent)
  },
  {
    path: '**',
    redirectTo: ''
  }
];
