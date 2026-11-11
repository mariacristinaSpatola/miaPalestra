import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { App } from './app';
import { AuthApi } from './core/auth.api';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        { provide: AuthApi, useValue: { user: () => null, restore: () => { } } },
      ],
    })
      .compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should render the formaFit navigation', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.brand')?.textContent).toContain('formaFIT');
    const areaCliente = compiled.querySelector<HTMLAnchorElement>('.nav-action');
    expect(areaCliente?.textContent).toContain('Area Cliente');
    expect(areaCliente?.getAttribute('href')).toBe('/accedi');
  });
});
