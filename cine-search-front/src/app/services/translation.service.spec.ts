import { TestBed } from '@angular/core/testing';
import { TranslationService } from './translation.service';

describe('TranslationService', () => {
  let service: TranslationService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({});
    service = TestBed.inject(TranslationService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should default to French', () => {
    expect(service.lang()).toBe('fr');
  });

  it('should return correct locale for French', () => {
    service.setLang('fr');
    expect(service.locale()).toBe('fr-FR');
  });

  it('should return correct locale for English', () => {
    service.setLang('en');
    expect(service.locale()).toBe('en-US');
  });

  it('should switch language', () => {
    service.setLang('en');
    expect(service.lang()).toBe('en');
    service.setLang('fr');
    expect(service.lang()).toBe('fr');
  });

  it('should persist language to localStorage', () => {
    service.setLang('en');
    expect(localStorage.getItem('movieseeker-lang')).toBe('en');
  });

  it('should return key when translation not found', () => {
    expect(service.t('nonexistent.key')).toBe('nonexistent.key');
  });

  it('should return translated string for a valid key', () => {
    service.setLang('fr');
    const result = service.t('home.heroTitle');
    // Should return a French string, not the key itself
    expect(result).not.toBe('home.heroTitle');
    expect(typeof result).toBe('string');
  });
});
