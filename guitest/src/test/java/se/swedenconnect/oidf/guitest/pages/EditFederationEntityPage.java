package se.swedenconnect.oidf.guitest.pages;

import com.microsoft.playwright.Page;

/**
 * Page object for the edit federation entity view ({@code /entities/federation/:id/edit}).
 *
 * <p>Covers the module configuration section (Trustanchor, Intermediate,
 * Resolver, Trustmark Issuer expansion panels).</p>
 */
public class EditFederationEntityPage {

  private final Page page;

  public EditFederationEntityPage(final Page page) {
    this.page = page;
  }

  public void waitForLoad() {
    this.page.waitForSelector("#btn-save-entity");
  }

  // ---- Trustanchor -------------------------------------------------------

  public void expandTrustanchorPanel() {
    this.page.locator(".v-expansion-panel-title:has-text('Trustanchor')").first().click();
    this.page.waitForSelector("#btn-save-trustanchor");
  }

  public void saveTrustanchor() {
    this.page.locator("#btn-save-trustanchor").click();
    // Wait for the panel chip to reflect the saved state (Active / Inactive)
    this.page.waitForSelector(
        ".v-expansion-panel-title:has-text('Trustanchor') .v-chip:not(:has-text('Not configured'))");
  }

  public boolean isTrustanchorActive() {
    return this.page
        .locator(".v-expansion-panel-title:has-text('Trustanchor') .v-chip")
        .textContent()
        .trim()
        .equals("Active");
  }

  public boolean hasTrustanchorModule() {
    final String chip = this.page
        .locator(".v-expansion-panel-title:has-text('Trustanchor') .v-chip")
        .textContent()
        .trim();
    return chip.equals("Active") || chip.equals("Inactive");
  }

  // ---- Intermediate -------------------------------------------------------

  public void expandIntermediatePanel() {
    this.page.locator(".v-expansion-panel-title:has-text('Intermediate')").first().click();
    this.page.waitForSelector("#btn-save-intermediate");
  }

  public void saveIntermediate() {
    this.page.locator("#btn-save-intermediate").click();
    this.page.waitForSelector(
        ".v-expansion-panel-title:has-text('Intermediate') .v-chip:not(:has-text('Not configured'))");
  }

  public boolean isIntermediateActive() {
    return this.page
        .locator(".v-expansion-panel-title:has-text('Intermediate') .v-chip")
        .textContent()
        .trim()
        .equals("Active");
  }

  // ---- Trustmark Issuer --------------------------------------------------

  public void expandTrustmarkIssuerPanel() {
    this.page.locator(".v-expansion-panel-title:has-text('Trustmark Issuer')").first().click();
    this.page.waitForSelector("#btn-save-trustmarkissuer");
  }

  public void saveTrustmarkIssuer() {
    this.page.locator("#btn-save-trustmarkissuer").click();
    this.page.waitForSelector(
        ".v-expansion-panel-title:has-text('Trustmark Issuer') .v-chip:not(:has-text('Not configured'))");
  }

  public boolean isTrustmarkIssuerActive() {
    return this.page
        .locator(".v-expansion-panel-title:has-text('Trustmark Issuer') .v-chip")
        .textContent()
        .trim()
        .equals("Active");
  }

  // ---- Shared ------------------------------------------------------------

  public void clickBack() {
    this.page.locator("#btn-back").click();
    this.page.waitForSelector("#btn-add-federation-entity");
  }

  /**
   * Returns the current page URL — useful for extracting the entity ID after
   * navigating from the home list.
   */
  public String currentUrl() {
    return this.page.url();
  }
}
