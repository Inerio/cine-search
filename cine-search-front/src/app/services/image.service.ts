import { Injectable } from '@angular/core';
import { environment } from '@env/environment';

export type ImageSize = 'w92' | 'w154' | 'w185' | 'w342' | 'w500' | 'w780' | 'original';

@Injectable({ providedIn: 'root' })
export class ImageService {
  private baseUrl = environment.imageBaseUrl;

  getPosterUrl(path: string | null, size: ImageSize = 'w342'): string {
    if (!path) return 'assets/no-poster.svg';
    return `${this.baseUrl}/${size}${path}`;
  }

  getBackdropUrl(path: string | null, size: ImageSize = 'w780'): string {
    if (!path) return '';
    return `${this.baseUrl}/${size}${path}`;
  }

  getProfileUrl(path: string | null, size: ImageSize = 'w185'): string {
    if (!path) return 'assets/no-profile.svg';
    return `${this.baseUrl}/${size}${path}`;
  }
}
