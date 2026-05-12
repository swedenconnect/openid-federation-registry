package se.swedenconnect.oidf.guitest.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Page object for the registrations list view (/registrations).
 */
public class RegistrationsListPage {

  private static final String PATH = "/registrations";

  private final Page page;
  private final String baseUrl;

  public RegistrationsListPage(final Page page, final String baseUrl) {
    this.page = page;
    this.baseUrl = baseUrl;
  }

  public void navigate() {
    this.page.navigate(this.baseUrl + PATH);
  }

  public void clickTriggerRegistration() {
    this.page.locator("#btn-trigger-registration").click();
  }

  public Locator rows() {
    return this.page.locator("tbody tr");
  }

  public void searchFor(final String query) {
    this.page.locator("input[type='text']").first().fill(query);
  }

  public void clickRow(final int index) {
    this.rows().nth(index).click();
  }
}
