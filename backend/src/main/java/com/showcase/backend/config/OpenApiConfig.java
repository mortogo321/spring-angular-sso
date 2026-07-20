package com.showcase.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  private static final String SECURITY_SCHEME_NAME = "keycloak-oauth2";

  @Value("${KEYCLOAK_ISSUER_URI:http://localhost:8081/realms/showcase}")
  private String issuerUri;

  @Bean
  public OpenAPI backendOpenApi() {
    String authorizationUrl = issuerUri + "/protocol/openid-connect/auth";
    String tokenUrl = issuerUri + "/protocol/openid-connect/token";

    SecurityScheme oauth2Scheme =
        new SecurityScheme()
            .type(SecurityScheme.Type.OAUTH2)
            .flows(
                new OAuthFlows()
                    .authorizationCode(
                        new OAuthFlow()
                            .authorizationUrl(authorizationUrl)
                            .tokenUrl(tokenUrl)
                            .scopes(new Scopes().addString("openid", "OpenID Connect scope"))));

    return new OpenAPI()
        .info(
            new Info()
                .title("Task Tracker API")
                .description(
                    "Showcase REST API for managing tasks, secured with Keycloak SSO (OAuth2 / JWT)")
                .version("v1"))
        .components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME, oauth2Scheme))
        .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
  }
}
