package se.swedenconnect.oidf.guitest.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Page object for the federation view (entity cards, {@code /federation}). Each federation entity is a card with
 * id {@code entity-card-<index>}; the title carries the entity identifier in its {@code title} attribute whether or
 * not the entity has a name.
 */
public class HomePage {

  private static final String PATH = "/federation";

  private final Page page;
  private final String baseUrl;

  public HomePage(final Page page, final String baseUrl) {
    this.page = page;
    this.baseUrl = baseUrl;
  }

  public void navigate() {
    this.page.navigate(this.baseUrl + PATH);
    this.page.waitForSelector("#btn-add-federation-entity");
  }

  public void clickAddFederationEntity() {
    this.page.locator("#btn-add-federation-entity").click();
  }

  public Locator entityCards() {
    return this.page.locator(".entity-card");
  }

  /** Waits until an entity with the given identifier is visible in the list. */
  public void waitForEntity(final String entityIdentifier) {
    cardTitle(entityIdentifier).first().waitFor();
  }

  /**
   * Returns the card index for the entity with the given identifier, or -1 if not found.
   */
  public int findRowIndex(final String entityIdentifier) {
    final Locator cards = entityCards();
    final int count = cards.count();
    for (int i = 0; i < count; i++) {
      final String title = cards.nth(i).locator(".entity-name").getAttribute("title");
      if (entityIdentifier.equals(title)) {
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

  /** Deletes the entity from its edit page, the list has no delete button. */
  public void deleteEntity(final String entityIdentifier) {
    final int idx = findRowIndex(entityIdentifier);
    if (idx < 0) {
      return;
    }
    this.page.locator("#btn-edit-entity-" + idx).click();
    this.page.waitForSelector("#btn-delete-entity");
    this.page.locator("#btn-delete-entity").click();
    this.page.waitForSelector("#btn-delete-entity-confirm");
    this.page.locator("#btn-delete-entity-confirm").click();
    this.page.waitForSelector("#btn-add-federation-entity");
  }

  private Locator cardTitle(final String entityIdentifier) {
    return this.page.locator(".entity-name[title=\"" + entityIdentifier + "\"]");
  }
}
