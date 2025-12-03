/*
 * Copyright 2025 Sweden Connect
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

package se.swedenconnect.oidf.registry.domain;

import se.swedenconnect.oidf.registry.validation.PropertyValidators;
import se.swedenconnect.oidf.registry.validation.VariabelValueResolver;

/**
 * oidf-entity-registry
 *
 * @author Per Fredrik Plars
 */
public class ValidateDomain {
  private final PropertyValidators.ValidationStringBuilder v;

  public ValidateDomain() {
    final PropertyValidators propertyValidators = new PropertyValidators();
    this.v = propertyValidators.builder(VariabelValueResolver.defaultResolver());
  }

  public void validate(TrustmarkIssuer trustmarkIssuer) {
    this.v.required().uuid().build().validate("entityId", trustmarkIssuer.getEntityId().toString());
    this.v.required().uuid().build().validate("active", trustmarkIssuer.getActive().toString());
    this.v.required().uuid().build().validate("trustMarkTokenValidityDuration",
        trustmarkIssuer.getTrustMarkTokenValidityDuration().toString());
  }

  public void validate(TrustAnchor trustAnchor) {
    String entityIdValidation = "required | uuid";
    String activeValidation = "required";
    String trustMarkIssuerValidation = "json";
  }

  public void validate(Intermediate intermediate) {
    String entityIdValidation = "required | uuid";
    String activeValidation = "required";
  }

  public void validate(Resolver resolver) {
    String entityIdValidation = "required | uuid";
    String activeValidation = "required";
    String resolveResponseDurationValidation = "required | duration";
    String trustAnchorValidation = "required | url";
    String trustedKeysValidation = "required | jwks";
    String stepRetryDurationValidation = "required | duration";
  }

  public void validate(Trustmark trustmark) {
    String trustmarkissuerIdValidation = "required | uuid";
    String trustMarkEntityIdValidation = "required | url";
    String logoUriValidation = "url";
    String refUriValidation = "url";
    String delegationValidation = "jwt:delegation";
  }

  public void validate(Policies policies) {
    String nameValidation = "required";
    String policyValidation = "required | json";
  }

  public void validate(TrustmarkSubject trustmarkSubject) {
    String trustmarkIdValidation = "required";
    String subjectValidation = "required | url";
    String revokedValidation = "required";
  }

  public void validate(FederationEntity federationEntity) {
    String subjectValidation = "required | url | starts_with:@{entityprefix}";
    String metadataValidation = "json";
    String issuerValidation = "required | url | starts_with:@{entityprefix}";
  }

  public void validate(SubordinateEntity subordinateEntity) {
    String subjectValidation = "required | url";
    String issuerValidation = "required | url";
    String jwksValidation = "required | jwks";
  }

}
