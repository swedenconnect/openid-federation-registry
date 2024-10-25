package se.swedenconnect.oidf.entity.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Properties;

/**
 * Test cases for {@link LibraryVersion}.
 *
 * @author Martin Lindström
 */
public class LibraryVersionTest {

  private String version;

  /**
   * Constructs a new LibraryVersionTest instance and loads the version information from the
   * "version.properties" resource file.
   *
   * @throws Exception if there is an issue loading the properties file or reading the version property
   */
  public LibraryVersionTest() throws Exception {
    final Properties properties = new Properties();
    properties.load(this.getClass().getClassLoader().getResourceAsStream("version.properties"));

    this.version = properties.getProperty("library.version");
    if (this.version.endsWith("-SNAPSHOT")) {
      this.version = this.version.substring(0, version.length() - 9);
    }
  }

  /**
   * Tests the unique identifier (UID) of the library version against a predefined serialization UID.
   * This ensures that the version's hash code corresponds to the library's serialization UID.
   */
  @Test
  public void testUid() {
    Assertions.assertEquals(this.version.hashCode(), LibraryVersion.SERIAL_VERSION_UID);
  }

  /**
   * Tests that the current version of the library matches the expected version.
   * <p>
   * The method asserts that the version string retrieved from {@link LibraryVersion#getVersion()}
   * is equal to the version loaded from the "version.properties" resource file.
   */
  @Test
  public void testVersion() {
    Assertions.assertEquals(this.version, LibraryVersion.getVersion(),
        "Expected LibraryVersion.getVersion() to return " + version);
  }

}