import { ProvideKeycloakOptions } from 'keycloak-angular';

import { environment } from '../../../environments/environment';

/**
 * Keycloak initialization options for `provideKeycloak`.
 *
 * Uses the Authorization Code + PKCE (S256) flow. `check-sso` silently detects
 * an existing SSO session (via a hidden iframe) without forcing a redirect,
 * so public pages remain accessible to anonymous visitors.
 */
export const keycloakConfig: ProvideKeycloakOptions = {
  config: {
    url: environment.keycloak.url,
    realm: environment.keycloak.realm,
    clientId: environment.keycloak.clientId,
  },
  initOptions: {
    onLoad: 'check-sso',
    silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
    pkceMethod: 'S256',
  },
};
