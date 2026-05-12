package se.swedenconnect.oidf.guitest.pages;

import com.microsoft.playwright.Page;

/**
 * Page object for the login flow.
 *
 * <p>Navigates to {@code /} which redirects unauthenticated users to the app's
 * login page, then clicks Login to trigger the OIDC redirect to the IdP, and
 * finally fills in credentials on the IdP's form.</p>
 */
public class LoginPage {

  private final Page page;
  private final String baseUrl;

  public LoginPage(final Page page, final String baseUrl) {
    this.page = page;
    this.baseUrl = baseUrl;
  }

  /**
   * Performs the full login sequence and waits until the home page is reached.
   *
   * @param username IdP username
   * @param password IdP password
   */
  public void login(final String username, final String password) {
    this.page.navigate(this.baseUrl + "/");

    // Wait for app login button in main content, then click to trigger OIDC redirect to Keycloak
    this.page.waitForSelector("main #btn-login");
    this.page.locator("main #btn-login").click();

    // Wait for Keycloak login form
    this.page.waitForSelector("#kc-form-login");
    this.page.locator("#username").fill(username);
    this.page.locator("#password").fill(password);
    this.page.locator("#kc-login").click();

    // Wait until we are back at the app home page after OIDC callback
    this.page.waitForURL(this.baseUrl + "/");
    this.page.waitForSelector("h2");
  }
}
