package se.swedenconnect.oidf.entity.registry.audit;

import lombok.Builder;

import java.io.Serializable;

@Builder
public class FederationApiEvent implements Serializable {
  final RegistryAuditEvent event;
  final String issuer;
  final String policyId;
  final String subject;
  final String trustMarkId;
}
