import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { KEYCLOAK_EVENT_SIGNAL, KeycloakEvent, KeycloakEventType } from 'keycloak-angular';
import Keycloak from 'keycloak-js';

import { Header } from './header';

interface KeycloakMock {
  authenticated: boolean;
  tokenParsed: Record<string, unknown>;
  login: ReturnType<typeof vi.fn>;
  logout: ReturnType<typeof vi.fn>;
}

function setup(authenticated: boolean): {
  fixture: ComponentFixture<Header>;
  keycloakMock: KeycloakMock;
} {
  const keycloakMock: KeycloakMock = {
    authenticated,
    tokenParsed: authenticated ? { preferred_username: 'somchai' } : {},
    login: vi.fn(),
    logout: vi.fn(),
  };
  const eventSignal = signal<KeycloakEvent>({ type: KeycloakEventType.Ready, args: authenticated });

  TestBed.configureTestingModule({
    imports: [Header],
    providers: [
      provideRouter([]),
      { provide: Keycloak, useValue: keycloakMock },
      { provide: KEYCLOAK_EVENT_SIGNAL, useValue: eventSignal },
    ],
  });

  const fixture = TestBed.createComponent(Header);
  fixture.detectChanges();
  return { fixture, keycloakMock };
}

describe('Header', () => {
  it('shows a login button when unauthenticated', () => {
    const { fixture } = setup(false);
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Log in');
    expect(compiled.textContent).not.toContain('Log out');
  });

  it('shows the username and logout button when authenticated', () => {
    const { fixture } = setup(true);
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('somchai');
    expect(compiled.textContent).toContain('Log out');
  });

  it('calls keycloak.login when the login button is clicked', () => {
    const { fixture, keycloakMock } = setup(false);
    const button = fixture.nativeElement.querySelector('button') as HTMLButtonElement;
    button.click();
    expect(keycloakMock.login).toHaveBeenCalled();
  });

  it('calls keycloak.logout when the logout button is clicked', () => {
    const { fixture, keycloakMock } = setup(true);
    const button = fixture.nativeElement.querySelector('button') as HTMLButtonElement;
    button.click();
    expect(keycloakMock.logout).toHaveBeenCalled();
  });
});
