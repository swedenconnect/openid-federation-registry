package se.swedenconnect.oidf.entity.registry.config;

import com.nimbusds.jose.jwk.JWK;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.Base64;

/**
 * Properties for RegistryService
 *
 * @author Per Fredrik Plars
 */
@ConfigurationProperties("openid.federation.registry")
public record RegistryProperties(String federationserviceapiSignKey) {


  @PostConstruct
  public void validate(){
    Assert.hasText(federationserviceapiSignKey,"Expected federationserviceapiSignKey");
    try {
      federationserviceapiSignKey();
    }
    catch (Exception e) {
      throw new IllegalArgumentException("FederationserviceapiSignKey has the wrong format can not be parsed",e);
    }
  }

  /**
   * Parsing  federationserviceapiSignKey to a JWK
   * @return JWK
   * @throws ParseException If there is an error to pars signkey
   */
  public JWK federationserviceapiSignKeyJWK() throws ParseException {
    return JWK.parse(new String(Base64.getDecoder().decode(federationserviceapiSignKey), Charset.defaultCharset()));
  }
}
