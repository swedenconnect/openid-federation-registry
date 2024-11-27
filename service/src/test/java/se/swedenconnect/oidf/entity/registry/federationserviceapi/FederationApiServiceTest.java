package se.swedenconnect.oidf.entity.registry.federationserviceapi;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import se.swedenconnect.oidf.entity.util.EntityFactory;

import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * oidf-entity-registry
 *
 * @author Per Fredrik Plars
 */
class FederationApiServiceTest {
  final String signKey = "eyJwIjoiOGVqdjlJYWFXVUZGVFNDcE1VVFd2cDA1Wl9zZW9pak5rZGFodWw3UWpqOGNVN1Y2LVd5YWl4NnlxYlZrUHR2eDNrRlFud2RSb2FBRGRseFZXcjRRY0MxWlBscjVDY2FGOXA2RkRtTDZGVi1SSThIVXNHTmp4UHBYTldPYUZSdHNWclFrdXh6NUpVelEzYm1SaGVjSmp0b0p5T1ZYRFZGcGFXbnZFZFZ2NFJjIiwia3R5IjoiUlNBIiwicSI6IjFYYzZpVEk0WUlTY0hRSFFjUWJzQndHbDJBOEhrckZ2YnRNU1kzV013aHV3WFpDdjhKTzA3ZWpkQUdUUlBtdFk0YTRtNkI4MEIxdEFYWE5feUdacGpkd1ZyTkNJbDAwUUh1VldjXzIyRzBFYy1MU2FqUmxFWmZoM1prMmd2cFdzMjZOeFdDLTdFdnRoTjgxcnc4d2J6eldPamdicjJ3S2pjZkM3ai05WGw5cyIsImQiOiJQY1UtM1FBWnR0Z2ZhSjYxZUNsN1hDRUpvTWRkUUI4dXdtSkE1TDdYbDRhZGlXNGs1elVZZUZlR0stZmRNUHdraGhYZExIcVEwbFZ2RmEyeVJlN09qVVo2bkFrZDRpX2VZZGY5YVRIVWNCM2hhRjZYNWxKU0x5WTVIVF9GM2ViQXlVcHBLc1lRQkRZMVJ4YS1GMTR5Nnk5TnhKelRESlFiQ1U1MldGazhET2JSdTctU25fTVZhbXNreUJJNng4eHdRY3B1QV9GOTJTMnowLTdxNHFlQmJIa3p3ZXRMQUNVUFhyWVNqbWh3aExiZ0lxNjB3UWlHdlY1QmNrWUFWX3VPV25VNWZONW1DbzBIYWlfTm1Ma19ROUpBZTdUSHRwRFI5N0NOM1ctR2Vza01tT2ladVAtVFRiZXlLQkdDRnF3TWJoZFdKX2Y1NlIyVEsxX0t3NUVRTFEiLCJlIjoiQVFBQiIsInVzZSI6InNpZyIsImtpZCI6IjVjMTMwZTliLWU2M2MtNDE4ZS1iNWU1LTA4ZWVjYzc5NTIyOSIsInFpIjoiTjdGZGdiRkNnbnZ2LXF4b3prckJDcXVWR2Y4a283RG1zbi1HdXgxMWpjNjVTX0RqYkd5V1FGVklOa3RlWkhQMUlyWVRaSUtZQmF6VmgzOVpZaHItMEFPQU5wQWVpZk1GRm5xQ3dLUzd5d3pUbTZBT2o2VkFQLWxtRXV4YURkNWJ1Zmw5WE16MUJsRnhQZjhaOGU5aDNYWTJ2NUJmNTBFZ2FDZkFHRU95MUY0IiwiZHAiOiJHMXZ3NTRMaHFmNkx5X0ZKc1Z6THZMc2padk1ydjhORm5KemRwYXBiZ19yM1JUQmRQQ0JnU2lPcXFTN3VxMzFNbVlwaGg5cllES09BUUw2b21KNnVWVUdMdWxXbm5NZHJGejFPWnhZaW1wQTRaZ2JoLXlFQ2c2Q2NoRmxEYi1ldUZSQkNwemJicHJCTlh3WkJ3eGNpS1puWFNYVkdweWJ4LVE0V0Q3cEg4UGsiLCJpYXQiOjE3MzA0NjczNTQsImRxIjoiU3NqcWlRWV9HaEZiWUE0eXFHWjBhal81aWlnNXp2cTZ2MmpUR0dVbVd4cTRQdzlobERjck8tNUlfc3BUUkRyM2VKazZxRGZHVW8xMWowZlhqSzNVYlE2ZHlWMkZmUHhTbVlCZk9XTzVXUE1HNDFyaWltd191am5DR3VVM1c0MjBjWDVoUnpQeHBrVExXbkZ0UDd0U1F0WFFpNUdCTDhsTTJhNHRCOUFpTC1FIiwibiI6InliZDZQbW9qazJTMk4wLTlKT3BMeEFBNV9JUjc3al9LaV85R3hTM0NrbVB4cWJCYkdGTmVlZmtFS2ZlNF9FZU9DbmNCQXhpc2hnU0lvWUExdVRmUVN5dmtVM2pDaTZaQmotZXpfVWs2T3Z6ZVpDSngwaFRNZU1kbnI4MDRTYnpQYWJsamY4UldhWjlhcGRxSnR4Q2Y3VUJrUXRMTkc5WG1ld01yLXZGNTNCamdFTlBlc3g0VWVYZHRkM3N3Tk9ycWJfT1g3dVZxaG9jYUs1VHppYml2b1FENEw2cXRVNWhYY0tuWDJmdURMN3lZVmM1am1LREd3X0V4RS1EMUNzamhNaHRUOHVnVmpRTHVOQy15TWFFZl9xVzR2RnA5MVNPbkR0bjBrQ3ZXYWF2dkJ6bmJqUUtfNVFzV1lRS3l6SGZtWmhQbHNSMFBHRTZ1Y0k0cGpFc2ZyUSJ9";

  public JWK federationserviceapiSignKeyJWK() throws ParseException {
    return JWK.parse(new String(Base64.getDecoder().decode(this.signKey), Charset.defaultCharset()));
  }

  @Test
  void signJsonRecords() throws ParseException, JOSEException {
    final FederationApiService federationApiService =
        new FederationApiService(null,federationserviceapiSignKeyJWK(),null,null);

    final  SignedJWT jwt = federationApiService.signJsonRecords("trust-marks",
        List.of(EntityFactory.createDefaultJsonEntity(),
            EntityFactory.createDefaultJsonEntity("http://iss1","http://sub1")));

    final String jwtser = jwt.serialize();

    final SignedJWT signedJWT  = SignedJWT.parse(jwtser);
    assertEquals(new JOSEObjectType("trust-marks+jwt"),signedJWT.getHeader().getType());
    assertNotNull(signedJWT.getHeader().getKeyID());
    final List<Object> claim = signedJWT.getJWTClaimsSet().getListClaim("trust_marks");
    assertNotNull(claim);
    assertTrue(!claim.isEmpty());

  }
}