import { TestBed } from '@angular/core/testing';
import { ImageService } from './image.service';

describe('ImageService', () => {
  let service: ImageService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ImageService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  // --- getPosterUrl ---
  it('should build poster URL with default size', () => {
    expect(service.getPosterUrl('/abc.jpg')).toBe('https://image.tmdb.org/t/p/w342/abc.jpg');
  });

  it('should build poster URL with custom size', () => {
    expect(service.getPosterUrl('/abc.jpg', 'w500')).toBe('https://image.tmdb.org/t/p/w500/abc.jpg');
  });

  it('should return fallback for null poster path', () => {
    expect(service.getPosterUrl(null)).toBe('assets/no-poster.svg');
  });

  // --- getBackdropUrl ---
  it('should build backdrop URL with default size', () => {
    expect(service.getBackdropUrl('/bg.jpg')).toBe('https://image.tmdb.org/t/p/w780/bg.jpg');
  });

  it('should return empty string for null backdrop', () => {
    expect(service.getBackdropUrl(null)).toBe('');
  });

  // --- getProfileUrl ---
  it('should build profile URL with default size', () => {
    expect(service.getProfileUrl('/profile.jpg')).toBe('https://image.tmdb.org/t/p/w185/profile.jpg');
  });

  it('should return fallback for null profile path', () => {
    expect(service.getProfileUrl(null)).toBe('assets/no-profile.svg');
  });

  // --- getLogoUrl ---
  it('should build logo URL with default size', () => {
    expect(service.getLogoUrl('/logo.png')).toBe('https://image.tmdb.org/t/p/w92/logo.png');
  });

  it('should return empty string for null logo path', () => {
    expect(service.getLogoUrl(null)).toBe('');
  });
});
