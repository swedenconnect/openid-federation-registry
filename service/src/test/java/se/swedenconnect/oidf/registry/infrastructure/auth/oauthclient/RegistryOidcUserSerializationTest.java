/*
 * Copyright 2026 Sweden Connect
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package se.swedenconnect.oidf.registry.infrastructure.auth.oauthclient;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.serializer.support.DeserializingConverter;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the security context of a GUI login can be stored in the database session, which Spring Session does
 * with Java serialization.
 *
 * @author Per Fredrik Plars
 */
class RegistryOidcUserSerializationTest {

  @Test
  @DisplayName("The security context with a RegistryOidcUser survives serialization")
  void securityContextIsSerializable() {
    final OidcIdToken idToken = OidcIdToken.withTokenValue("id-token")
        .subject("test-user-subject")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(300))
        .claim("org_rights", List.of(Map.of(
            "organization_identifier", "5520001263",
            "functions", List.of(Map.of("name", "swedenconnect", "rights", List.of("admin"))))))
        .build();
    final RegistryOidcUser user = new RegistryOidcUser(
        new DefaultOidcUser(List.of(new SimpleGrantedAuthority("OIDC_USER")), idToken));
    final SecurityContextImpl context = new SecurityContextImpl(
        new OAuth2AuthenticationToken(user, user.getAuthorities(), "entity-registry"));

    final byte[] bytes = new SerializingConverter().convert(context);
    final SecurityContextImpl restored = (SecurityContextImpl) new DeserializingConverter().convert(bytes);

    final RegistryOidcUser restoredUser = (RegistryOidcUser) restored.getAuthentication().getPrincipal();
    assertThat(restoredUser.getName()).isEqualTo("test-user-subject");
    assertThat(restoredUser.getIdToken().getClaims()).isEqualTo(idToken.getClaims());
  }
}
