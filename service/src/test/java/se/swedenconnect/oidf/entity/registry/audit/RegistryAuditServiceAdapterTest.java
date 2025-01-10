package se.swedenconnect.oidf.entity.registry.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.audit.AuditEvent;
import se.swedenconnect.oidf.entity.registry.fixture.PolicyFactory;
import se.swedenconnect.oidf.registry.api.model.PolicyRecord;

import java.util.Stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class RegistryAuditServiceAdapterTest {

  /**
   * This class tests the `policyWrite` method in the `RegistryAuditServiceAdapter` class. The `policyWrite` method is
   * responsible for emitting an event indicating the creation or update of a policy record, including JSON
   * representations of the old and new policy records.
   */

  @Test
  void policyWrite_shouldEmitEventWithCorrectAttributes() throws JsonProcessingException {

    final Stack<AuditEvent> stack = new Stack<>();
    final RegistryAuditService auditService = new RegistryAuditServiceLog() {
      @Override
      protected void emitEvent(final FederationAuditEvent event) {
        super.emitEvent(event);
        stack.push(event.toAuditEvent("<NoPrincipal>"));
      }
    };

    final PolicyRecord record = PolicyFactory.record();
    auditService.policyWrite("pid",record,record);
    assertEquals(stack.peek().getType(),RegistryAuditEventType.POLICY_CREATE_UPDATED.name());
    assertNull(stack.peek().getData().get("oldData"));
    assertNotNull(stack.peek().getData().get("newData"));
    assertNotNull(stack.peek().getData().get("extId"));

    final PolicyRecord newRecord = PolicyFactory.record();
    auditService.policyWrite("pid",record,newRecord);
    assertEquals(stack.peek().getType(),RegistryAuditEventType.POLICY_CREATE_UPDATED.name());
    assertNotNull(stack.peek().getData().get("oldData"));
    assertNotNull(stack.peek().getData().get("newData"));
    assertNotNull(stack.peek().getData().get("extId"));

  }





}