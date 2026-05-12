package se.swedenconnect.oidf.guitest;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import se.swedenconnect.oidf.guitest.pages.LoginPage;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility that performs a one-time login and persists the browser session
 * (cookies + localStorage) to {@value #STATE_FILE}.
 *
 * <p>Call {@link #ensureAuthenticated(String, String, String)} at the start of
 * a test suite. Subsequent calls are no-ops if the state file already exists
 * in the current JVM run.</p>
 */
public final class AuthSetup {

  static final Path STATE_FILE = Paths.get(
      System.getProperty("java.io.tmpdir"), "oidf-guitest-auth-state.json");

  private static volatile boolean done = false;

  private AuthSetup() {
  }

  /**
   * Logs in once and saves the session state to {@value #STATE_FILE}.
   * Thread-safe; subsequent calls are no-ops within the same JVM run.
   *
   * @param baseUrl  application base URL
   * @param username IdP username (from system property {@code guitest.username})
   * @param password IdP password (from system property {@code guitest.password})
   */
  public static synchronized void ensureAuthenticated(
      final String baseUrl, final String username, final String password) {

    if (done) {
      return;
    }

    try (Playwright playwright = Playwright.create()) {
      final Browser browser = playwright.chromium().launch(
          new BrowserType.LaunchOptions().setHeadless(true));
      final BrowserContext ctx = browser.newContext(
          new Browser.NewContextOptions().setIgnoreHTTPSErrors(true));
      final Page page = ctx.newPage();

      new LoginPage(page, baseUrl).login(username, password);

      ctx.storageState(new BrowserContext.StorageStateOptions().setPath(STATE_FILE));
    }

    done = true;
  }
}
