package se.swedenconnect.oidf.entity.registry.audit;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import org.springframework.context.ApplicationEventPublisher;

import java.util.UUID;

public class RegistryAuditService {

  private final ApplicationEventPublisher publisher;

  public RegistryAuditService(ApplicationEventPublisher publisher) {
    this.publisher = publisher;
  }

  public void federationEntityRead(final EntityID issuer) {
    this.publisher.publishEvent(
        FederationApiEvent.builder()
            .event(RegistryAuditEvent.FEDERATION_API_ENTITY_READ)
            .issuer(issuer.toString())
            .build());
  }

  public void federationTrustMarkSubjectRead(final EntityID issuer,final String trustMarkId, final String subject) {
    this.publisher.publishEvent(
        FederationApiEvent.builder()
            .event(RegistryAuditEvent.FEDERATION_API_TRUSTMARK_SUBJECT_READ)
            .issuer(issuer.toString())
            .trustMarkId(trustMarkId)
            .subject(subject)
            .build());
  }

  public void federationPolicyRead(final UUID policyId) {
    this.publisher.publishEvent(
        FederationApiEvent.builder()
            .event(RegistryAuditEvent.FEDERATION_API_POLICY_READ)
            .policyId(policyId.toString())
            .build());
  }

}
