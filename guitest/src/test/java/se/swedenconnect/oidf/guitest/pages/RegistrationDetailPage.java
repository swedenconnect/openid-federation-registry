package se.swedenconnect.oidf.guitest.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/**
 * Page object for the registration detail view (/registrations/{id}).
 */
public class RegistrationDetailPage {

  private final Page page;

  public RegistrationDetailPage(final Page page) {
    this.page = page;
  }

  public String entityId() {
    return this.page.locator("tr:has-text('Entity ID') td").nth(1).textContent();
  }

  public String status() {
    return this.page.locator(".v-chip").first().textContent().trim();
  }

  public void openTrustmarkTab() {
    this.page.getByRole(AriaRole.TAB,
        new Page.GetByRoleOptions().setName("Trustmark requests")).click();
  }

  public Locator trustmarkRows() {
    return this.page.locator("text=Trustmark type").locator("..").locator("tbody tr");
  }

  public void clickReject() {
    this.page.locator("#btn-reject").click();
  }

  public void fillRejectionReason(final String reason) {
    this.page.locator("textarea").fill(reason);
  }

  public void confirmReject() {
    this.page.locator("#btn-reject-confirm").click();
  }

  public void clickBack() {
    this.page.locator("#btn-back").click();
  }
}
