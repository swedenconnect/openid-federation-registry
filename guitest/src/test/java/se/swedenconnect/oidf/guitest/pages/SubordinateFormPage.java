package se.swedenconnect.oidf.guitest.pages;

import com.microsoft.playwright.Page;

/**
 * Page object for the create/edit subordinate form
 * ({@code /entities/:entityId/modules/:moduleType/subordinates/new}).
 */
public class SubordinateFormPage {

  /** Minimal valid RSA public key JWKS for use in tests. */
  public static final String TEST_JWKS =
      "{\"keys\":[{\"kty\":\"RSA\",\"use\":\"sig\",\"alg\":\"RS256\",\"kid\":\"test-1\","
          + "\"n\":\"0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx4cbbfAAtVT86zwu1RK7a"
          + "PFFxuhDR1L6tSoc_BJECPebWKRXjBZCiFV4n3oknjhMstn64tZ_2W-5JsGY4Hc5n9yBXArwl93lqt7"
          + "_RN5w6Cf0h4QyQ5v-65YGjQR0_FDW2QvzqY368QQMicAtaSqzs8KJZgnYb9c7d0zgdAZHzu6qMQvRL"
          + "5hajrn1n91CbOpbISD08qNLyrdkt-bFTWhAI4vMQFh6WeZu0fM4lFd2NcRwr3XPksINHaQ-G_xBniI"
          + "qbw0Ls1jF44-csFCur-kEgU8awapJzKnqDKgw\","
          + "\"e\":\"AQAB\"}]}";

  private final Page page;

  public SubordinateFormPage(final Page page) {
    this.page = page;
  }

  public void fillEntityIdentifier(final String identifier) {
    this.page.locator("#entity-identifier").fill(identifier);
  }

  public void fillJwks(final String jwks) {
    this.page.locator("#subordinate-jwks").fill(jwks);
  }

  public void submit() {
    this.page.locator("#btn-save").click();
    this.page.waitForSelector("#btn-add-subordinate");
  }
}