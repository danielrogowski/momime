package com.ndg.graphics.ndgbmp;

import java.io.IOException;

/**
 * Class of exceptions raised when encoding or decoding the .ndgbmp format 
 */
public class NdgBmpException extends IOException
{
	/**
	 * Unique value for serialization
	 */
	private static final long serialVersionUID = -4228470485577462747L;

	/**
	 * Creates an exception to with a .ndgbmp format image
	 */
	public NdgBmpException ()
	{
		super ();
	}

	/**
	 * Creates an exception to with a .ndgbmp format image, with a specified message
	 * @param s The detailed exception message
	 */
	public NdgBmpException (String s)
	{
		super (s);
	}
	
}
