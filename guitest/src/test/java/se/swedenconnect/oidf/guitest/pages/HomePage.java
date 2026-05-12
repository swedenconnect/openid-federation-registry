package se.swedenconnect.oidf.guitest.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Page object for the home view (entity list, {@code /}).
 */
public class HomePage {

  private final Page page;
  private final String baseUrl;

  public HomePage(final Page page, final String baseUrl) {
    this.page = page;
    this.baseUrl = baseUrl;
  }

  public void navigate() {
    this.page.navigate(this.baseUrl + "/");
    this.page.waitForSelector("#btn-add-federation-entity");
  }

  public void clickAddFederationEntity() {
    this.page.locator("#btn-add-federation-entity").click();
  }

  public Locator entityRows() {
    return this.page.locator("table tbody tr");
  }

  /** Waits until an entity with the given identifier is visible in the list. */
  public void waitForEntity(final String entityIdentifier) {
    this.page.locator("table tbody tr td")
        .filter(new Locator.FilterOptions().setHasText(entityIdentifier))
        .first()
        .waitFor();
  }

  /**
   * Returns the row index for the entity with the given identifier, or -1 if not found.
   */
  public int findRowIndex(final String entityIdentifier) {
    final Locator rows = entityRows();
    final int count = rows.count();
    for (int i = 0; i < count; i++) {
      final String text = rows.nth(i).locator("td").first().textContent().trim();
      if (text.equals(entityIdentifier)) {
        return i;
      }
    }
    return -1;
  }

  public boolean hasEntity(final String entityIdentifier) {
    return findRowIndex(entityIdentifier) >= 0;
  }

  public void clickEditForEntity(final String entityIdentifier) {
    final int idx = findRowIndex(entityIdentifier);
    if (idx < 0) {
      throw new AssertionError("Entity not found in list: " + entityIdentifier);
    }
    this.page.locator("#btn-edit-entity-" + idx).click();
  }

  public void clickModuleButton(final String entityIdentifier, final String moduleType) {
    waitForEntity(entityIdentifier);
    final int idx = findRowIndex(entityIdentifier);
    if (idx < 0) {
      throw new AssertionError("Entity not found in list: " + entityIdentifier);
    }
    final String buttonId = "#btn-module-" + moduleType + "-" + idx;
    this.page.waitForSelector(buttonId);
    this.page.locator(buttonId).click();
  }

  public void deleteEntity(final String entityIdentifier) {
    final int idx = findRowIndex(entityIdentifier);
    if (idx < 0) {
      return;
    }
    this.page.locator("#btn-delete-entity-" + idx).click();
    this.page.waitForSelector("#btn-delete-entity-confirm");
    this.page.locator("#btn-delete-entity-confirm").click();
    this.page.waitForSelector("#btn-add-federation-entity");
  }
}
