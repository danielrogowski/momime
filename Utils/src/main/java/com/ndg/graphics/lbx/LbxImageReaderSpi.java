package com.ndg.graphics.lbx;

import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Locale;

import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

/**
 * Service Provider Interface for reading .lbx format images
 */
public class LbxImageReaderSpi extends ImageReaderSpi
{
	/**
	 * Name of the vendor of the .lbx format
	 */
	private static final String vendorName = "Nigel Gay";
	
	/**
	 * Version of the .lbx file structure
	 */
	private static final String version = "1.0";
	
	/**
	 * Class responsible for decoding .lbx streams
	 */
	private static final String readerClassName = "com.ndg.graphics.lbx.LbxReader";
	
	/**
	 * Encoding .lbx streams currently not supported in Java
	 */
	private static final String [] writerSpiNames = null;
	
	/**
	 * Accepts ImageInputStream only
	 */
	@SuppressWarnings ("unchecked")
	private static final Class [] inputTypes = STANDARD_INPUT_TYPE;

	/**
	 * Descriptive list of the format(s) supported by this reader
	 */
	private static final String [] names = { "LBX" };

	/**
	 * List of the file extension(s) supported by this reader
	 */
	private static final String [] suffixes = { "lbx" };

	/**
	 * List of the MIME type(s) supported by this reader
	 */
	private static final String [] MIMETypes = { "image/lbx" };
	
	/**
	 * .lbx reader currently doesn't support any metadata
	 */
	private static final boolean supportsStandardStreamMetadataFormat = false;
	
	/**
	 * .lbx reader currently doesn't support any metadata
	 */
	private static final String nativeStreamMetadataFormatName = null;
	
	/**
	 * .lbx reader currently doesn't support any metadata
	 */
	private static final String nativeStreamMetadataFormatClassName = null;
	
	/**
	 * .lbx reader currently doesn't support any metadata
	 */
	private static final String [] extraStreamMetadataFormatNames = null;
	
	/**
	 * .lbx reader currently doesn't support any metadata
	 */
	private static final String [] extraStreamMetadataFormatClassNames = null;
	
	/**
	 * .lbx reader currently doesn't support any metadata
	 */
	private static final boolean supportsStandardImageMetadataFormat = false;
	
	/**
	 * .lbx reader currently doesn't support any metadata
	 */
	private static final String nativeImageMetadataFormatName = null;
	
	/**
	 * .lbx reader currently doesn't support any metadata
	 */
	private static final String nativeImageMetadataFormatClassName = null;
	
	/**
	 * .lbx reader currently doesn't support any metadata
	 */
	private static final String [] extraImageMetadataFormatNames = null;
	
	/**
	 * .lbx reader currently doesn't support any metadata
	 */
	private static final String [] extraImageMetadataFormatClassNames = null;
	
	/**
	 * Fills in all the details about the .lbx format
	 */
	public LbxImageReaderSpi ()
	{
		super (vendorName, version, names, suffixes, MIMETypes, readerClassName, inputTypes, writerSpiNames,
			supportsStandardStreamMetadataFormat, nativeStreamMetadataFormatName, nativeStreamMetadataFormatClassName,
			extraStreamMetadataFormatNames, extraStreamMetadataFormatClassNames, supportsStandardImageMetadataFormat,
			nativeImageMetadataFormatName, nativeImageMetadataFormatClassName, extraImageMetadataFormatNames, extraImageMetadataFormatClassNames);
	}

	/**
	 * Creates the corresponding reader associated with this service provider interfaces
	 * @param extension Used for certain image formats, but not this one - ignored
	 * @return Appropriate class for decoding .ndgbmp streams
	 */
	@Override
	public ImageReader createReaderInstance (Object extension)
	{
		return new LbxImageReader (this);
	}

	/**
	 * @param locale The locale for which the description should be appropriate (i.e. if we wanted descriptions in multiple languages)
	 * @return Description of this SPI
	 */
	@Override
	public String getDescription (Locale locale)
	{
		return "LBX Reader SPI";
	}

    /**
     * @param source the object (typically an ImageInputStream) to be decoded
     * @return Whether or not the supplied source object appears to be of the format supported by this reader
     * @throws IOException If there is an error reading from the stream
     */
	@Override
	public boolean canDecodeInput (Object source)
		throws IOException
	{
		boolean result = false;
		
		if (source instanceof ImageInputStream)
		{
			ImageInputStream stream = (ImageInputStream) source;

			// Check the image file has enough space to hold at least the graphics header and an ending offset
			if (stream.length () >= LbxImageReader.LBX_IMAGE_HEADER_SIZE + 4)
			{               
				// Note this is testing one lbx image - not the whole lbx archive
				// so this isn't as simple as looking for the LBX signature at the front of the archive
				// We actually need to read and parse the LBX image header and check its validity
				stream.mark ();

				// Read the header
				stream.setByteOrder (ByteOrder.LITTLE_ENDIAN);
				
				// Skip width, height, junk value
				for (int skip = 0; skip < 3; skip++)
					stream.readUnsignedShort ();

				int frameCount = stream.readUnsignedShort ();
			
				// Skip 3 junk values, palette info offset, plus the final junk value
				for (int skip = 0; skip < 5; skip++)
					stream.readUnsignedShort ();

				// Check the file has at least enough space to hold the graphics header plus offsets
				// for the number of bitmaps it specifies
				if (stream.length () >= LbxImageReader.LBX_IMAGE_HEADER_SIZE + ((frameCount + 1) * 4))
				{
					// Skip through until we read the offset of the end of the last frame
					long offset = 0;
					for (int frameNo = 0; frameNo <= frameCount; frameNo++)
						offset = stream.readUnsignedInt ();
					
					// This should exactly equal the length of the stream
					result = (offset == stream.length ());
				}
				
				stream.reset ();
			}
		}
			
		return result;
	}
}
