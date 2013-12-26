package com.ndg.graphics.lbx;

import java.io.IOException;
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
	private static final String lbxVendorName = "Nigel Gay";

	/**
	 * Version of the .lbx file structure
	 */
	private static final String lbxVersion = "1.0";

	/**
	 * Class responsible for decoding .lbx streams
	 */
	private static final String lbxReaderClassName = "com.ndg.graphics.lbx.LbxReader";

	/**
	 * Encoding .lbx streams currently not supported in Java
	 */
	private static final String [] lbxWriterSpiNames = null;

	/** Accepts ImageInputStream only */
	@SuppressWarnings ("rawtypes")
	private static final Class [] lbxInputTypes = {ImageInputStream.class};

	/**
	 * Descriptive list of the format(s) supported by this reader
	 */
	private static final String [] lbxNames = { "LBX" };

	/**
	 * List of the file extension(s) supported by this reader
	 */
	private static final String [] lbxSuffixes = { "lbx" };

	/**
	 * List of the MIME type(s) supported by this reader
	 */
	private static final String [] lbxMIMETypes = { "image/lbx" };

	/**
	 * .lbx reader currently doesn't support any metadata
	 */
	private static final boolean lbxSupportsStandardStreamMetadataFormat = false;

	/**
	 * .lbx reader currently doesn't support any metadata
	 */
	private static final String lbxNativeStreamMetadataFormatName = null;

	/**
	 * .lbx reader currently doesn't support any metadata
	 */
	private static final String lbxNativeStreamMetadataFormatClassName = null;

	/**
	 * .lbx reader currently doesn't support any metadata
	 */
	private static final String [] lbxExtraStreamMetadataFormatNames = null;

	/**
	 * .lbx reader currently doesn't support any metadata
	 */
	private static final String [] lbxExtraStreamMetadataFormatClassNames = null;

	/**
	 * .lbx reader currently doesn't support any metadata
	 */
	private static final boolean lbxSupportsStandardImageMetadataFormat = false;

	/**
	 * .lbx reader currently doesn't support any metadata
	 */
	private static final String lbxNativeImageMetadataFormatName = null;

	/**
	 * .lbx reader currently doesn't support any metadata
	 */
	private static final String lbxNativeImageMetadataFormatClassName = null;

	/**
	 * .lbx reader currently doesn't support any metadata
	 */
	private static final String [] lbxExtraImageMetadataFormatNames = null;

	/**
	 * .lbx reader currently doesn't support any metadata
	 */
	private static final String [] lbxExtraImageMetadataFormatClassNames = null;

	/**
	 * Fills in all the details about the .lbx format
	 */
	public LbxImageReaderSpi ()
	{
		super (lbxVendorName, lbxVersion, lbxNames, lbxSuffixes, lbxMIMETypes, lbxReaderClassName, lbxInputTypes, lbxWriterSpiNames,
			lbxSupportsStandardStreamMetadataFormat, lbxNativeStreamMetadataFormatName, lbxNativeStreamMetadataFormatClassName,
			lbxExtraStreamMetadataFormatNames, lbxExtraStreamMetadataFormatClassNames, lbxSupportsStandardImageMetadataFormat,
			lbxNativeImageMetadataFormatName, lbxNativeImageMetadataFormatClassName, lbxExtraImageMetadataFormatNames, lbxExtraImageMetadataFormatClassNames);
	}

	/**
	 * Creates the corresponding reader associated with this service provider interfaces
	 * @param extension Used for certain image formats, but not this one - ignored
	 * @return Appropriate class for decoding .ndgbmp streams
	 */
	@Override
	public final ImageReader createReaderInstance (final Object extension)
	{
		return new LbxImageReader (this);
	}

	/**
	 * @param locale The locale for which the description should be appropriate (i.e. if we wanted descriptions in multiple languages)
	 * @return Description of this SPI
	 */
	@Override
	public final String getDescription (final Locale locale)
	{
		return "LBX Reader SPI";
	}

    /**
     * @param source the object (typically an ImageInputStream) to be decoded
     * @return Whether or not the supplied source object appears to be of the format supported by this reader
     * @throws IOException If there is an error reading from the stream
     */
	@Override
	public final boolean canDecodeInput (final Object source)
		throws IOException
	{
		boolean result = false;
		if (source instanceof ImageInputStream)
		{
			final ImageInputStream stream = (ImageInputStream) source;
			result = (LbxImageReader.testStream (stream) != null);
		}

		return result;
	}
}
