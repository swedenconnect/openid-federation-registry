package se.swedenconnect.oidf.guitest.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Page object for the subordinates list view
 * ({@code /entities/:entityId/modules/:moduleType/subordinates}).
 */
public class SubordinatesListPage {

  private final Page page;

  public SubordinatesListPage(final Page page) {
    this.page = page;
  }

  public void waitForLoad() {
    this.page.waitForSelector("#btn-add-subordinate");
  }

  public void clickAddSubordinate() {
    this.page.locator("#btn-add-subordinate").click();
    this.page.waitForSelector("#btn-save");
  }

  public boolean hasSubordinate(final String entityIdentifier) {
    return this.page.locator("table tbody tr td")
        .filter(new Locator.FilterOptions().setHasText(entityIdentifier))
        .count() > 0;
  }
}