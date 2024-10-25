package se.swedenconnect.oidf.entity.api;

/**
 * Internal class used for serialization across library classes.
 *
 * @author Martin Lindström
 */
public final class LibraryVersion {

  private static final int MAJOR = 1;
  private static final int MINOR = 0;
  private static final int PATCH = 0;

  /**
   * Global serialization value for classes.
   */
  public static final long SERIAL_VERSION_UID = getVersion().hashCode();

  /**
   * Gets the version string.
   *
   * @return the version string
   */
  public static String getVersion() {
    return MAJOR + "." + MINOR + "." + PATCH;
  }

}