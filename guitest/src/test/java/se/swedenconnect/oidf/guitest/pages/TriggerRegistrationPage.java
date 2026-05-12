package se.swedenconnect.oidf.guitest.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/**
 * Page object for the trigger registration view (/registrations/trigger).
 */
public class TriggerRegistrationPage {

  private static final String PATH = "/registrations/trigger";

  private final Page page;
  private final String baseUrl;

  public TriggerRegistrationPage(final Page page, final String baseUrl) {
    this.page = page;
    this.baseUrl = baseUrl;
  }

  public void navigate() {
    this.page.navigate(this.baseUrl + PATH);
  }

  public void selectFlow(final String flowName) {
    this.page.getByRole(AriaRole.COMBOBOX).first().click();
    this.page.getByRole(AriaRole.OPTION,
        new Page.GetByRoleOptions().setName(flowName)).click();
  }

  public void fillEntityIdentifier(final String entityId) {
    this.page.locator("input[type='text']").nth(1).fill(entityId);
  }

  public void submit() {
    this.page.getByRole(AriaRole.BUTTON,
        new Page.GetByRoleOptions().setName("Submit")).click();
  }

  public Locator resultStatus() {
    return this.page.locator(".v-chip").first();
  }

  public boolean isResultVisible() {
    return this.page.locator("text=Registration Result").isVisible();
  }

  public void clickViewRegistration() {
    this.page.getByRole(AriaRole.BUTTON,
        new Page.GetByRoleOptions().setName("View Registration")).click();
  }
}
