import { createInterceptorCondition, IncludeBearerTokenCondition } from 'keycloak-angular';

/**
 * Attaches the Keycloak bearer token to every request made to the backend API.
 *
 * Matches any URL containing `/api/` so it works both in dev (proxied via
 * proxy.conf.json to http://localhost:8080) and in the production Docker
 * image (proxied by nginx to the `backend` service) without hardcoding a host.
 */
export const apiBearerTokenCondition = createInterceptorCondition<IncludeBearerTokenCondition>({
  urlPattern: /\/api\//,
});
