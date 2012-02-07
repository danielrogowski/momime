package com.ndg.utils;

import java.io.InputStream;

import javax.imageio.stream.MemoryCacheImageInputStream;

/**
 * Specialised MemoryCacheImageInputStream which imposes a fixed length on the stream 
 */
public class FixedLengthMemoryCacheImageInputStream extends MemoryCacheImageInputStream
{
	/**
	 * The length to constrain the image input stream to
	 */
	private final long fixedLength;

	/**
	 * Creates a specialised MemoryCacheImageInputStream which imposes a fixed length on the stream 
	 * @param stream The input stream to be wrapped by the image input stream
	 * @param aFixedLength The length to constrain the image input stream to
	 */
	public FixedLengthMemoryCacheImageInputStream (InputStream stream, long aFixedLength)
	{
		super (stream);
		
		fixedLength = aFixedLength;
	}

	/**
	 * @return The fixed length this image stream is constrained to
	 */
	@Override
    public long length ()
    {
        return fixedLength;
    }
}
