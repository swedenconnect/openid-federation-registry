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
package se.swedenconnect.oidf.registry.registrationflow.process.step.impl;

import com.nimbusds.jose.jwk.JWKSet;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import se.swedenconnect.oidf.registry.entity.dto.HostedEntityDto;
import se.swedenconnect.oidf.registry.entity.service.EntityConfigService;
import se.swedenconnect.oidf.registry.guioperations.OidfServiceIntegration;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrganizationRecord;
import se.swedenconnect.oidf.registry.infrastructure.validation.CleanInput;
import se.swedenconnect.oidf.registry.module.model.TrustAnchorIntermediateModule;
import se.swedenconnect.oidf.registry.module.repository.TaImRepository;
import se.swedenconnect.oidf.registry.organization.service.InstancePlacementService;
import se.swedenconnect.oidf.registry.registrationflow.process.ContextKey;
import se.swedenconnect.oidf.registry.registrationflow.process.ProcessContext;
import se.swedenconnect.oidf.registry.registrationflow.process.step.Severity;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepConfig;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepConfigurationValue;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepIssue;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepResult;

import java.net.URI;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles the hosted-entity path of a registration join.
 * <p>
 * When the join request body contains a {@code metadata} map this step:
 * <ol>
 *   <li>Creates or updates the hosted entity via {@link EntityConfigService} so the
 *       audit trail is triggered correctly.</li>
 *   <li>Extracts the JWKS from the {@code jwks} key of the metadata body and places
 *       it in {@link ContextKey#ENTITY_CONFIGURATION_JWKS} for downstream steps.</li>
 *   <li>Places the metadata body in {@link ContextKey#ENTITY_CONFIGURATION_METADATA}.</li>
 * </ol>
 * When {@link ContextKey#REQUEST_METADATA} is absent the step is a no-op, allowing
 * the same flow to handle both hosted and non-hosted joins.
 * <p>
 * The entity is owned by the calling organisation (from the auth token). The {@code subject}
 * field — which determines the registry base-URL used for {@code ec_location} computation —
 * is derived from the TaIm's organisation prefix so that the URL points to the registry
 * host rather than to the joining entity's own domain.
 *
 * @author Felix Hellman
 */
@Slf4j
@SuppressWarnings("unchecked")
@Component
public class HostedEntityRegistrationStep extends NoConfigStepAdapter {

  private final EntityConfigService entityConfigService;
  private final TaImRepository taImRepository;
  private final InstancePlacementService instancePlacementService;
  private final OidfServiceIntegration oidfServiceIntegration;

  /**
   * Constructor.
   *
   * @param entityConfigService      service used to persist the hosted entity and trigger audit
   * @param taImRepository           repository used to load the TaIm for its entity prefix
   * @param instancePlacementService service used to resolve the registry entity prefix and base URL
   * @param oidfServiceIntegration   integration used to fetch JWKS from the service node
   */
  public HostedEntityRegistrationStep(final EntityConfigService entityConfigService,
      final TaImRepository taImRepository,
      final InstancePlacementService instancePlacementService,
      final OidfServiceIntegration oidfServiceIntegration) {
    this.entityConfigService = entityConfigService;
    this.taImRepository = taImRepository;
    this.instancePlacementService = instancePlacementService;
    this.oidfServiceIntegration = oidfServiceIntegration;
  }

  @Override
  public StepType stepType() {
    return StepType.MID;
  }

  @Override
  public boolean isPublic() {
    return true;
  }

  @Override
  public String getDescription() {
    return "If metadata is present in the join request body, create or update a hosted entity "
        + "via EntityConfigService so audit is triggered, then load JWKS for downstream steps.";
  }

  @Override
  public boolean canApply(final ProcessContext ctx, final StepConfig config) {
    return ctx.get(ContextKey.REQUEST_METADATA).isPresent();
  }

  @Override
  @Transactional
  public StepResult execute(final ProcessContext ctx, final StepConfig config) {
    final JSONObject metadata = ctx.<JSONObject>getRequired(ContextKey.REQUEST_METADATA);
    final String entityId = ctx.getRequired(ContextKey.ENTITY_ID);
    final OrganizationRecord org = ctx.getRequired(ContextKey.ORG);

    // Derive registry entity-prefix from the TaIm's org so that the hosted-entity
    // subject points to the registry host URL, not to the joining entity's own domain.
    // Entity ownership remains with the calling org (orgNumber from auth token).
    final UUID taImId = ctx.getRequired(ContextKey.TAIM_ID);
    final TrustAnchorIntermediateModule taIm = this.taImRepository.findById(taImId)
        .orElseThrow(() -> new IllegalStateException("TaIm not found: " + taImId));
    final String registryEntityPrefix = this.instancePlacementService
        .resolveEntityPrefix(taIm.getOrganization().getOrgNumber(), null)
        .orElse(org.entityPrefix());
    final OrganizationRecord orgWithRegistryPrefix = new OrganizationRecord(
        org.orgNumber(), org.orgName(), registryEntityPrefix, org.functionGroup());

    // Persist via service so audit fires
    final List<HostedEntityDto> existing = this.entityConfigService.listHostedEntity(org, entityId);
    if (existing.isEmpty()) {
      final HostedEntityDto input = new HostedEntityDto();
      input.setEntityIdentifier(entityId);
      input.setMetadata(metadata);
      this.entityConfigService.createHostedEntity(orgWithRegistryPrefix, UUID.randomUUID(), input);
    }
    else {
      final HostedEntityDto current = existing.getFirst();
      current.setMetadata(metadata);
      this.entityConfigService.updateHostedEntity(orgWithRegistryPrefix, current.getEntityId(), current);
    }

    // Extract JWKS for downstream steps — expected at top-level "jwks" key
    final Object jwksRaw = metadata.get("jwks");
    if (jwksRaw == null) {
      return StepResult.failure("metadata body missing 'jwks' — cannot create subordinate",
          List.of(new StepIssue("HostedEntityRegistrationStep.jwks",
              "jwks is required in metadata body", Severity.ERROR)));
    }

    try {
      final JWKSet jwkSet = JWKSet.parse(new JSONObject((Map<String, Object>) jwksRaw));
      ctx.put(ContextKey.ENTITY_CONFIGURATION_JWKS, CleanInput.removeExpIatNbfFromJwks(jwkSet));
      ctx.put(ContextKey.ENTITY_CONFIGURATION_METADATA, metadata);
    }
    catch (final ParseException e) {
      return StepResult.failure("Failed to parse jwks from metadata body",
          List.of(new StepIssue("HostedEntityRegistrationStep.jwks",
              "jwks could not be parsed: " + e.getMessage(), Severity.ERROR)));
    }

    // Merge hosted keys from the service-node's /jwk endpoint.
    // These are the keys that will actually be used when the entity is hosted, so they must
    // be included in the subordinate statement even though the entity isn't live yet.
    final Optional<URI> serviceNodeBaseUrl = this.instancePlacementService
        .resolveBaseUrl(taIm.getOrganization().getOrgNumber(), null);

    serviceNodeBaseUrl.ifPresent(baseUrl -> {
      try {
        final JWKSet hostedJwks = this.oidfServiceIntegration.fetchHostedJwksFromServiceNode(baseUrl);
        ctx.put(ContextKey.ENTITY_CONFIGURATION_JWKS, CleanInput.removeExpIatNbfFromJwks(hostedJwks));
        log.debug("Replaced JWKS with {} hosted keys from service node {} for entity {}",
            hostedJwks.getKeys().size(), baseUrl, entityId);
      }
      catch (final Exception e) {
        log.warn("Could not fetch hosted JWKS from service node {}, falling back to metadata JWKS: {}",
            baseUrl, e.getMessage());
      }
    });

    return StepResult.success("Hosted entity stored and JWKS loaded for: " + entityId);
  }

  @Override
  public UUID getStepId() {
    return UUID.fromString("C3F1A820-5D7B-4E9A-B034-1F6D9A3C7E82");
  }

  @Override
  public List<StepConfigurationValue> getStepConfigurationValues() {
    return List.of();
  }
}
