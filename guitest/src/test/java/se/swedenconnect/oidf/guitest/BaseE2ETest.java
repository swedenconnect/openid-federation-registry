package se.swedenconnect.oidf.guitest;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.ConsoleMessage;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for all E2E tests. Manages Playwright lifecycle and provides
 * a fresh, authenticated {@link Page} per test method.
 *
 * <p>On first use the base class performs a real login via {@link AuthSetup}
 * and saves the session state to a temp file. Every subsequent test loads
 * that state, avoiding repeated OIDC round-trips.</p>
 *
 * <p>Required system properties (set in pom.xml or on the command line):
 * <ul>
 *   <li>{@code guitest.username} — IdP username</li>
 *   <li>{@code guitest.password} — IdP password</li>
 * </ul>
 * </p>
 */
public abstract class BaseE2ETest {

  protected static final String BASE_URL =
      "https://registry.local.swedenconnect.se";

  private static final boolean HEADLESS = false;

  private static final String USERNAME = "oidf-sc";
  private static final String PASSWORD = "oidf-sc";

  private static Playwright playwright;
  private static Browser browser;

  protected BrowserContext context;
  protected Page page;

  private final List<String> consoleLogs = new ArrayList<>();

  @BeforeAll
  static void launchBrowser() {
    AuthSetup.ensureAuthenticated(BASE_URL, USERNAME, PASSWORD);
    playwright = Playwright.create();
    browser = playwright.chromium().launch(
        new BrowserType.LaunchOptions().setHeadless(HEADLESS));
  }

  @AfterAll
  static void closeBrowser() {
    playwright.close();
  }

  @BeforeEach
  void createContext() {
    consoleLogs.clear();
    context = browser.newContext(new Browser.NewContextOptions()
        .setIgnoreHTTPSErrors(true)
        .setStorageStatePath(AuthSetup.STATE_FILE));
    page = context.newPage();
    page.onConsoleMessage(this::captureConsole);
    page.onPageError(err -> consoleLogs.add("[page-error] " + err));
    page.onRequestFailed(req ->
        consoleLogs.add("[request-failed] " + req.method() + " " + req.url()
            + " — " + req.failure()));
    page.navigate(BASE_URL + "/");
  }

  @AfterEach
  void closeContext() {
    context.close();
  }

  protected String url(final String path) {
    return BASE_URL + path;
  }

  /** Returns all captured browser console/error lines for the current test. */
  protected List<String> browserLogs() {
    return List.copyOf(consoleLogs);
  }

  /**
   * Prints captured browser logs to stdout — call from a catch block or
   * {@code @AfterEach} to surface diagnostics on failure.
   */
  protected void dumpBrowserLogs() {
    if (consoleLogs.isEmpty()) {
      System.out.println("[browser-log] (no console output captured)");
      return;
    }
    consoleLogs.forEach(line -> System.out.println("[browser-log] " + line));
  }

  private void captureConsole(final ConsoleMessage msg) {
    consoleLogs.add("[" + msg.type() + "] " + msg.text());
  }
}
