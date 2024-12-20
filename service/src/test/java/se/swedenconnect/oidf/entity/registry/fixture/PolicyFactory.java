package se.swedenconnect.oidf.entity.registry.fixture;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import se.swedenconnect.oidf.entity.registry.policy.PolicyEntity;
import se.swedenconnect.oidf.registry.api.model.PolicyRecord;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

public final class PolicyFactory {

  public static PolicyRecord record(){
    return new PolicyRecord.Builder()
        .name("policy-name-test")
        .policy(Map.of("openid_relying_party",
            Map.of("grant_types",
                Map.of("subset_of",
                    List.of("authorization_code")
                ))
        ))
        .policyRecordId(UUID.randomUUID().toString())
        .build();
  }

  public static Stream<PolicyRecord> records(){
    final AtomicInteger i = new AtomicInteger(1);
    return Stream.generate(() -> {
          final PolicyRecord policy = record();
          policy.setName(policy.getName() + ":" + i.getAndIncrement());
          return policy;
        });

  }

  public static Stream<PolicyEntity> entities(){
    return records().map(PolicyFactory::recordToEntity);
  }

  public static PolicyEntity entity(){
   return recordToEntity(record());
  }

  private static PolicyEntity recordToEntity(PolicyRecord record){
    try {

      final PolicyEntity pe = new PolicyEntity();
      pe.setName(record.getName());
      pe.setExternalId(record.getPolicyRecordId());
      final String json = new ObjectMapper()
          .writerWithDefaultPrettyPrinter()
          .writeValueAsString(record);
      pe.setPolicy(json);
      return pe;
    }
    catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
