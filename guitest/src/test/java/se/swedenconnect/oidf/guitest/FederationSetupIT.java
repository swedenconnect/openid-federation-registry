package se.swedenconnect.oidf.guitest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se.swedenconnect.oidf.guitest.pages.CreateFederationEntityPage;
import se.swedenconnect.oidf.guitest.pages.EditFederationEntityPage;
import se.swedenconnect.oidf.guitest.pages.HomePage;
import se.swedenconnect.oidf.guitest.pages.SubordinateFormPage;
import se.swedenconnect.oidf.guitest.pages.SubordinatesListPage;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E test that sets up a minimal federation:
 * Trust Anchor → Intermediate → Trust Mark Issuer,
 * then registers the Intermediate as a subordinate of the Trust Anchor.
 */
class FederationSetupIT extends BaseE2ETest {

  private HomePage homePage;
  private CreateFederationEntityPage createPage;
  private EditFederationEntityPage editPage;
  private SubordinatesListPage subordinatesListPage;
  private SubordinateFormPage subordinateFormPage;

  private String taIdentifier;
  private String imIdentifier;
  private String tmiIdentifier;

  private String runId() {
    return UUID.randomUUID().toString().substring(0, 8);
  }

  @BeforeEach
  void setupPages() {
    this.homePage = new HomePage(this.page, BASE_URL);
    this.createPage = new CreateFederationEntityPage(this.page, BASE_URL);
    this.editPage = new EditFederationEntityPage(this.page);
    this.subordinatesListPage = new SubordinatesListPage(this.page);
    this.subordinateFormPage = new SubordinateFormPage(this.page);
  }

  @AfterEach
  void cleanup() {
    try {
      this.homePage.navigate();
      if (this.taIdentifier != null && this.homePage.hasEntity(this.taIdentifier)) {
        this.homePage.deleteEntity(this.taIdentifier);
      }
      if (this.imIdentifier != null && this.homePage.hasEntity(this.imIdentifier)) {
        this.homePage.deleteEntity(this.imIdentifier);
      }
      if (this.tmiIdentifier != null && this.homePage.hasEntity(this.tmiIdentifier)) {
        this.homePage.deleteEntity(this.tmiIdentifier);
      }
    } catch (final Exception e) {
      System.err.println("[cleanup] Failed to delete test entities: " + e.getMessage());
    }
  }

  @Test
  void setupFederation_createsTrustAnchorIntermediateTmiAndSubordinate() {
    final String id = runId();
    try {
      // 1. Create Trust Anchor with Trustanchor module
      this.homePage.navigate();
      this.homePage.clickAddFederationEntity();
      this.taIdentifier = this.createPage.fillEntityIdentifier("ta-" + id);
      this.createPage.submit();
      this.editPage.waitForLoad();
      this.editPage.expandTrustanchorPanel();
      this.editPage.saveTrustanchor();
      assertThat(this.editPage.isTrustanchorActive())
          .as("Trustanchor module should be Active")
          .isTrue();
      this.editPage.clickBack();

      // 2. Create Intermediate with Intermediate module
      this.homePage.clickAddFederationEntity();
      this.imIdentifier = this.createPage.fillEntityIdentifier("im-" + id);
      this.createPage.submit();
      this.editPage.waitForLoad();
      this.editPage.expandIntermediatePanel();
      this.editPage.saveIntermediate();
      assertThat(this.editPage.isIntermediateActive())
          .as("Intermediate module should be Active")
          .isTrue();
      this.editPage.clickBack();

      // 3. Create Trust Mark Issuer with TMI module
      this.homePage.clickAddFederationEntity();
      this.tmiIdentifier = this.createPage.fillEntityIdentifier("tmi-" + id);
      this.createPage.submit();
      this.editPage.waitForLoad();
      this.editPage.expandTrustmarkIssuerPanel();
      this.editPage.saveTrustmarkIssuer();
      assertThat(this.editPage.isTrustmarkIssuerActive())
          .as("Trustmark Issuer module should be Active")
          .isTrue();
      this.editPage.clickBack();

      // 4. Navigate to Trust Anchor's subordinates list
      this.homePage.clickModuleButton(this.taIdentifier, "trustanchor");
      this.subordinatesListPage.waitForLoad();

      // 5. Add Intermediate as a subordinate of the Trust Anchor
      this.subordinatesListPage.clickAddSubordinate();
      this.subordinateFormPage.fillEntityIdentifier(this.imIdentifier);
      this.subordinateFormPage.fillJwks(SubordinateFormPage.TEST_JWKS);
      this.subordinateFormPage.submit();

      assertThat(this.subordinatesListPage.hasSubordinate(this.imIdentifier))
          .as("Intermediate should appear as subordinate of Trust Anchor")
          .isTrue();

    } catch (final Exception e) {
      dumpBrowserLogs();
      throw e;
    }
  }
}