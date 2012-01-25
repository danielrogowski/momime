package momime.server.database;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Filter that only selects files with a specific ending, this is slightly different from ExtensionFilenameFilter because it can test parts of the actual filename too
 */
public final class SuffixFilenameFilter implements FilenameFilter
{
	/**
	 * Desired suffix
	 */
	private final String suffix;

	/**
	 * @param aSuffix Desired suffix
	 */
	public SuffixFilenameFilter (final String aSuffix)
	{
		super ();

		suffix = aSuffix;
	}

	/**
	 * @param dir The directory containing the file to test
	 * @param name The filename to test
	 * @return True if this file has the desired suffix, False if it doesn't
	 */
	@Override
	public final boolean accept (final File dir, final String name)
	{
		return name.endsWith (suffix);
	}
}
