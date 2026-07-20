import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import {
  INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG,
  includeBearerTokenInterceptor,
  provideKeycloak,
} from 'keycloak-angular';

import { routes } from './app.routes';
import { apiBearerTokenCondition } from './core/auth/bearer-token.interceptor';
import { keycloakConfig } from './core/auth/keycloak.config';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideKeycloak(keycloakConfig),
    { provide: INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG, useValue: [apiBearerTokenCondition] },
    provideHttpClient(withInterceptors([includeBearerTokenInterceptor])),
  ],
};
