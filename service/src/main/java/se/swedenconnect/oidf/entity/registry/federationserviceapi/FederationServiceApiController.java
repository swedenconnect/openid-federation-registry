package se.swedenconnect.oidf.entity.registry.federationserviceapi;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * FederationService API
 *
 * @author Per Fredrik Plars
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/federationservice")
public class FederationServiceApiController {

  private final FederationServiceApiService federationServiceApiService;

  /**
   * FederationService API
   * @param federationServiceApiService FederationService
   */
  public FederationServiceApiController(final FederationServiceApiService federationServiceApiService) {
    this.federationServiceApiService = federationServiceApiService;
  }


  @GetMapping(value="/trust_mark_sub_record", produces = "application/jwt")
  public String trustMarkRecord(
      @RequestParam(name="iss") final String issuer,
      @RequestParam(name="trustmark_id") final String trustmarkId,
      @RequestParam(name="sub", required = false) final String subject){

    return federationServiceApiService.trustMarkRecord(new EntityID(issuer),trustmarkId, Optional.ofNullable(subject));
  }


  @GetMapping(value="/policy_record", produces = "application/jwt")
  public String policyRecord(@RequestParam(name="policy_id") final String policyId){
    return federationServiceApiService.policyRecord(policyId);
  }

  @GetMapping(value="/entity_record", produces = "application/jwt")
  public String entityRecord(@RequestParam(name="iss") final String issuer){
    return federationServiceApiService.entityRecord(new EntityID(issuer));
  }



}
