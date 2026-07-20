import { inject } from '@angular/core';
import {
  ActivatedRouteSnapshot,
  CanActivateFn,
  Router,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';
import { AuthGuardData, createAuthGuard } from 'keycloak-angular';

/**
 * Functional guard: requires an authenticated session, and, when a route
 * declares `data: { role }`, requires that realm role to also be granted
 * (roles are read from the `realm_access.roles` token claim).
 */
const isAccessAllowed = async (
  route: ActivatedRouteSnapshot,
  _state: RouterStateSnapshot,
  authData: AuthGuardData,
): Promise<boolean | UrlTree> => {
  const { authenticated, grantedRoles } = authData;
  const router = inject(Router);

  if (!authenticated) {
    return router.parseUrl('/');
  }

  const requiredRole = route.data['role'] as string | undefined;
  if (!requiredRole || grantedRoles.realmRoles.includes(requiredRole)) {
    return true;
  }

  return router.parseUrl('/forbidden');
};

export const authGuard: CanActivateFn = createAuthGuard<CanActivateFn>(isAccessAllowed);
