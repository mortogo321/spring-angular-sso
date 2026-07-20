import { Component, computed, inject } from '@angular/core';
import Keycloak from 'keycloak-js';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.html',
  styleUrl: './profile.scss',
})
export class Profile {
  private readonly keycloak = inject(Keycloak);

  private readonly claims = computed(() => this.keycloak.tokenParsed ?? {});

  readonly name = computed(
    () =>
      (this.claims()['name'] as string | undefined) ??
      this.keycloak.tokenParsed?.['preferred_username'],
  );
  readonly email = computed(() => this.claims()['email'] as string | undefined);
  readonly roles = computed(() => this.keycloak.realmAccess?.roles ?? []);
  readonly claimsJson = computed(() => JSON.stringify(this.claims(), null, 2));
}
