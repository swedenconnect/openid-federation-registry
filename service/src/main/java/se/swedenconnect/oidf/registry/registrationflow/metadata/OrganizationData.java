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
package se.swedenconnect.oidf.registry.registrationflow.metadata;

/**
 * What the registry knows about the organization behind an entity, as used by metadata rules and checks.
 *
 * @param legalName the organization's legal name, {@code null} if not recorded
 * @param organizationNumber the ten-digit organization number
 * @author Felix Hellman
 */
public record OrganizationData(String legalName, String organizationNumber) {

  /** The ISO 6523 International Code Designator for Swedish organization numbers. */
  public static final String DEFAULT_ICD = "0007";

  /**
   * Returns one field of the organization data.
   *
   * @param field the field
   * @param icd the International Code Designator, used for the organization identifier; {@link #DEFAULT_ICD} when
   *     {@code null}
   * @return the value, {@code null} if the registry does not hold it
   */
  public String value(final MetadataPolicyTemplate.OrganizationField field, final String icd) {
    return switch (field) {
      case LEGAL_NAME -> this.legalName;
      case ORGANIZATION_NUMBER -> this.organizationNumber;
      case ORGANIZATION_IDENTIFIER -> this.organizationNumber == null ? null
          : "urn:glue:iso6523:%s:%s".formatted(icd == null || icd.isBlank() ? DEFAULT_ICD : icd,
              this.organizationNumber);
    };
  }
}
