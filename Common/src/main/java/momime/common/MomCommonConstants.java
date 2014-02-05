package momime.common;

/**
 * Constants used on both the client and server which aren't related to any of the XML files
 */
public final class MomCommonConstants
{
	/** Minimum major version of JVM required to run MoM IME */
	public final static int JAVA_REQUIRED_MAJOR_VERSION = 1;

	/** Minimum minor version of JVM required to run MoM IME */
	public final static int JAVA_REQUIRED_MINOR_VERSION = 7;

	/** Number of diagonal tiles horizontally across the combat map */
	public final static int COMBAT_MAP_WIDTH = 12;

	/** Number of diagonal tiles vertically zig-zagged down the combat map */
	public final static int COMBAT_MAP_HEIGHT = 25;

	/** The combat map area is 2D, however some parts of the map generator use 3D areas so need a non-zero value in order for these to work correctly */
	public final static int COMBAT_MAP_DEPTH = 1;
}
