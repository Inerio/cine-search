import { Injectable } from '@angular/core';
import { environment } from '@env/environment';

export type ImageSize = 'w92' | 'w154' | 'w185' | 'w342' | 'w500' | 'w780' | 'original';

/** Builds full TMDB image URLs from partial paths. */
@Injectable({ providedIn: 'root' })
export class ImageService {
  private baseUrl = environment.imageBaseUrl;

  getPosterUrl(path: string | null, size: ImageSize = 'w342'): string {
    return path ? `${this.baseUrl}/${size}${path}` : 'assets/no-poster.svg';
  }

  getBackdropUrl(path: string | null, size: ImageSize = 'w780'): string {
    return path ? `${this.baseUrl}/${size}${path}` : '';
  }

  getProfileUrl(path: string | null, size: ImageSize = 'w185'): string {
    return path ? `${this.baseUrl}/${size}${path}` : 'assets/no-profile.svg';
  }

  getLogoUrl(path: string | null, size: ImageSize = 'w92'): string {
    return path ? `${this.baseUrl}/${size}${path}` : '';
  }
}
