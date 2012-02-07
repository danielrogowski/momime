package com.ndg.archive;

import java.io.IOException;

/**
 * Type of exception for problems dealing with archive files
 */
public class ArchiveException extends IOException
{
	/**
	 * Unique value for serialization
	 */
	private static final long serialVersionUID = -882438400038763821L;

	/**
	 * Creates an .ndgarc exception with no message
	 */
	public ArchiveException ()
	{
	}

	/**
	 * Creates an .ndgarc exception with a detailed message
	 * @param s The detailed message describing the error
	 */
	public ArchiveException (String s)
	{
		super (s);
	}

}
