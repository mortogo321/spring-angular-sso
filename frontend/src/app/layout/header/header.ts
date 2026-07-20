import { Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import Keycloak from 'keycloak-js';
import { KEYCLOAK_EVENT_SIGNAL } from 'keycloak-angular';

@Component({
  selector: 'app-header',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './header.html',
  styleUrl: './header.scss',
})
export class Header {
  private readonly keycloak = inject(Keycloak);
  private readonly keycloakEvent = inject(KEYCLOAK_EVENT_SIGNAL);

  /** Re-derived on every Keycloak event; the boolean itself lives on the adapter. */
  readonly authenticated = computed(() => {
    this.keycloakEvent();
    return this.keycloak.authenticated ?? false;
  });

  readonly username = computed(() => {
    this.keycloakEvent();
    return (this.keycloak.tokenParsed?.['preferred_username'] as string | undefined) ?? null;
  });

  login(): void {
    void this.keycloak.login();
  }

  logout(): void {
    void this.keycloak.logout({ redirectUri: window.location.origin });
  }
}
