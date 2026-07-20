package com.showcase.backend.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Extracts Keycloak realm roles from the {@code realm_access.roles} claim of the access token and
 * maps each one to a Spring {@code ROLE_*} authority (e.g. realm role {@code user} becomes
 * authority {@code ROLE_user}).
 */
public class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

  private static final String REALM_ACCESS_CLAIM = "realm_access";
  private static final String ROLES_CLAIM = "roles";
  private static final String ROLE_PREFIX = "ROLE_";

  @Override
  public Collection<GrantedAuthority> convert(Jwt jwt) {
    Map<String, Object> realmAccess = jwt.getClaim(REALM_ACCESS_CLAIM);
    if (realmAccess == null) {
      return List.of();
    }

    Object rolesClaim = realmAccess.get(ROLES_CLAIM);
    if (!(rolesClaim instanceof Collection<?> roles)) {
      return List.of();
    }

    return roles.stream()
        .map(String::valueOf)
        .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(ROLE_PREFIX + role))
        .toList();
  }
}
