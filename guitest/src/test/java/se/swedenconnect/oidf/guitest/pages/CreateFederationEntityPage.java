package se.swedenconnect.oidf.guitest.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.regex.Pattern;

/**
 * Page object for the create federation entity form ({@code /entities/federation/new}).
 */
public class CreateFederationEntityPage {

  private static final String PATH = "/entities/federation/new";

  private final Page page;
  private final String baseUrl;

  private String entityIdentifier;

  public CreateFederationEntityPage(final Page page, final String baseUrl) {
    this.page = page;
    this.baseUrl = baseUrl;
  }

  public void navigate() {
    this.page.navigate(this.baseUrl + PATH);
    this.page.waitForSelector("#btn-create");
  }

  /**
   * Appends {@code pathSuffix} to whatever the field is pre-filled with (the
   * user's entity prefix). Adds a {@code /} separator when the current value
   * does not already end with one.
   *
   * @param pathSuffix path segment to append, e.g. {@code "trust-anchor"}
   */
  public String fillEntityIdentifier(final String pathSuffix) {
    final Locator field = this.page.locator("#entity-identifier");
    final String current = field.inputValue();
    final String separator = current.endsWith("/") ? "" : "/";
    final String entityIdentifier = current + separator + pathSuffix;
    field.fill(entityIdentifier);
    return entityIdentifier;
  }

  public void submit() {
    this.page.locator("#btn-create").click();
    // After creation the app navigates to the edit view for the new entity.
    this.page.waitForURL(Pattern.compile(".*\\/entities\\/federation\\/.*\\/edit"));
  }

  public String getentityIdentifier() {
    return entityIdentifier;
  }
}
