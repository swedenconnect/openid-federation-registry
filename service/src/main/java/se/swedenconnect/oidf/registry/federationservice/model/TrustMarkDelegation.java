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
package se.swedenconnect.oidf.registry.federationservice.model;

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;
import java.text.ParseException;
import java.util.function.Function;

/**
 * TrustMarkDelegation according to 7.2.1 in OpenID Federation 1.0 draft 40
 *
 * @author Per Fredrik Plars
 */
@Getter
@EqualsAndHashCode
public class TrustMarkDelegation implements Serializable {
  public static final String DELEGATION_TYPE = "trust-mark-delegation+jwt";
  private final String delegation;

  /**
   * Testing DelegationJWT so that it is compliant with specification.
   *
   * @param delegation delegationJWT
   */
  public TrustMarkDelegation(final String delegation) throws IllegalArgumentException {
    this.delegation = validateInternal(delegation, IllegalArgumentException::new);
  }

  /**
   * Static method to create TrustMarkDelegation
   *
   * @param trustMarkDelegation trustMarkDelegation JWT
   * @return TrustMarkDelegation
   */
  public static TrustMarkDelegation create(final String trustMarkDelegation) {
    return new TrustMarkDelegation(trustMarkDelegation);
  }

  private static <EX extends Exception> String validateInternal(final String delegation,
      final Function<String, EX> ex) throws EX {
    if (delegation == null || delegation.isBlank()) {
      throw ex.apply("Unable to create TrustMarkDelegation since input is null.");
    }
    try {
      final SignedJWT delegationJWT = SignedJWT.parse(delegation);
      final JWSHeader jwsHeader = delegationJWT.getHeader();

      FederationAssert.assertNotEmptyThrows(jwsHeader.getKeyID(), () -> ex.apply("KeyId is expected in JWT header"));
      FederationAssert.assertNotEmptyThrows(jwsHeader.getType(), () -> ex.apply("Type is expected in JWT header"));
      FederationAssert.assertNotEmptyThrows(jwsHeader.getType().getType(),
          () -> ex.apply("Type is expected in JWT header"));

      if (!jwsHeader.getType().getType().equals(DELEGATION_TYPE)) {
        ex.apply("Delegation header type is expected to be:'" + DELEGATION_TYPE +
            "' actual value:'" + jwsHeader.getType().getType() + "'");
      }

      final JWTClaimsSet claimsSet = delegationJWT.getJWTClaimsSet();

      FederationAssert.assertNotEmptyThrows(claimsSet.getIssuer(), () -> ex.apply("Issuer is expected in JWT claim"));
      FederationAssert.assertNotEmptyThrows(claimsSet.getSubject(), () -> ex.apply("Subject is expected in JWT claim"));
      FederationAssert.assertNotEmptyThrows(claimsSet.getClaim("id"), () -> ex.apply("ID is expected in JWT claim"));
      FederationAssert.assertNotEmptyThrows(
          claimsSet.getIssueTime(), () -> ex.apply("IssueTime is expected in JWT claim"));
    }
    catch (final ParseException e) {
      throw ex.apply("Unable to parse delegation JWT: '" + e.getMessage() + "'");
    }
    return delegation;
  }

  /**
   * Validated TrustMark, if it failes then exception is thrown according to ex
   *
   * @param trustMarkDelegation TrustMarkDelegation JWT
   * @param ex Function called to get an exception. A error string is supplied with the problem.
   * @param <EX> Exception
   * @return TrustMarkId
   * @throws EX Exception thrown if trustmark is not validated
   */
  public static <EX extends Exception> TrustMarkDelegation validate(final String trustMarkDelegation,
      final Function<String, EX> ex)
      throws EX {
    return new TrustMarkDelegation(validateInternal(trustMarkDelegation, ex));
  }

  @Override
  public String toString() {
    return this.delegation;
  }

}
