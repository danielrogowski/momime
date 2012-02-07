package com.ndg.archive;

/**
 * File contained inside some kind of archive container, e.g. .ndgarc or .lbx
 */
public class ArchivedFile
{
	/** Name of the file within the archive */
	private String fileName;

	/** Full path to the location of the file on disk outside of the archive */
	private String fullPath;

	/** Offset into the archive where this file starts */
	private final long fileOffset;

	/** Length of this file within the archive */
	private final long fileSize;

	/** Entry number of this file within the archive, with the first sub file being 0 */
	private final int subFileNumber;

	/**
	 * @param aFileName Name of the file within the archive
	 * @param aFullPath Full path to the location of the file on disk outside of the archive
	 * @param aFileOffset Offset into the archive where this file starts
	 * @param aFileSize Length of this file within the archive
	 * @param aSubFileNumber Entry number of this file within the archive, with the first sub file being 0
	 */
	public ArchivedFile (final String aFileName, final String aFullPath, final long aFileOffset, final long aFileSize, final int aSubFileNumber)
	{
		super ();

		fileName = aFileName;
		fullPath = aFullPath;
		fileOffset = aFileOffset;
		fileSize = aFileSize;
		subFileNumber = aSubFileNumber;
	}

	/**
	 * @return Name of the file within the archive
	 */
	public final String getFileName ()
	{
		return fileName;
	}

	/**
	 * @param newFileName Name of the file within the archive
	 */
	public final void setFileName (final String newFileName)
	{
		fileName = newFileName;
	}

	/**
	 * @return Full path to the location of the file on disk outside of the archive
	 */
	public final String getFullPath ()
	{
		return fullPath;
	}

	/**
	 * @param newPath Full path to the location of the file on disk outside of the archive
	 */
	public void setFullPath (final String newPath)
	{
		fullPath = newPath;
	}

	/**
	 * @return Offset into the archive where this file starts
	 */
	public final long getFileOffset ()
	{
		return fileOffset;
	}

	/**
	 * @return Length of this file within the archive
	 */
	public final long getFileSize ()
	{
		return fileSize;
	}

	/**
	 * @return Entry number of this file within the archive, with the first sub file being 0
	 */
	public final int getSubFileNumber ()
	{
		return subFileNumber;
	}
}
