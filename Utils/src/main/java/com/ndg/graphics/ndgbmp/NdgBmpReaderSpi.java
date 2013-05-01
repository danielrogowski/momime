package com.ndg.graphics.ndgbmp;

import java.io.IOException;
import java.util.Locale;

import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

/**
 * Service Provider Interface for reading .ndgbmp format images
 */
public final class NdgBmpReaderSpi extends ImageReaderSpi
{
	/** Name of the vendor of the .ndgbmp format */
	private static final String ndgbmpVendorName = "Nigel Gay";

	/** Version of the .ndgbmp file structure */
	private static final String ndgbmpVersion = "1.0";

	/** Class responsible for decoding .ndgbmp streams */
	private static final String ndgbmpReaderClassName = "com.ndg.graphics.ndgbmp.NdgBmpReader";

	/** SPI for encoding .ndgbmp streams */
	private static final String [] ndgbmpWriterSpiNames = { "com.ndg.graphics.ndgbmp.NdgBmpWriterSpi" };

	/** Accepts ImageInputStream only */
	@SuppressWarnings ("rawtypes")
	private static final Class [] ndgbmpInputTypes = {ImageInputStream.class};

	/** Descriptive list of the format(s) supported by this reader */
	private static final String [] ndgbmpNames = { "NDG BMP", "ndg bmp" };

	/** List of the file extension(s) supported by this reader */
	private static final String [] ndgbmpSuffixes = { "NDGBMP", "ndgbmp" };

	/** List of the MIME type(s) supported by this reader */
	private static final String [] ndgbmpMIMETypes = { "image/ndgbmp" };

	/** .ndgbmp reader currently doesn't support any metadata */
	private static final boolean ndgbmpSupportsStandardStreamMetadataFormat = false;

	/** .ndgbmp reader currently doesn't support any metadata */
	private static final String ndgbmpNativeStreamMetadataFormatName = null;

	/** .ndgbmp reader currently doesn't support any metadata */
	private static final String ndgbmpNativeStreamMetadataFormatClassName = null;

	/** .ndgbmp reader currently doesn't support any metadata */
	private static final String [] ndgbmpExtraStreamMetadataFormatNames = null;

	/** .ndgbmp reader currently doesn't support any metadata */
	private static final String [] ndgbmpExtraStreamMetadataFormatClassNames = null;

	/** .ndgbmp reader currently doesn't support any metadata */
	private static final boolean ndgbmpSupportsStandardImageMetadataFormat = false;

	/** .ndgbmp reader currently doesn't support any metadata */
	private static final String ndgbmpNativeImageMetadataFormatName = null;

	/** .ndgbmp reader currently doesn't support any metadata */
	private static final String ndgbmpNativeImageMetadataFormatClassName = null;

	/** .ndgbmp reader currently doesn't support any metadata */
	private static final String [] ndgbmpExtraImageMetadataFormatNames = null;

	/** .ndgbmp reader currently doesn't support any metadata */
	private static final String [] ndgbmpExtraImageMetadataFormatClassNames = null;

	/**
	 * Fills in all the details about the .ndgbmp format
	 */
	public NdgBmpReaderSpi ()
	{
		super (ndgbmpVendorName, ndgbmpVersion, ndgbmpNames, ndgbmpSuffixes, ndgbmpMIMETypes, ndgbmpReaderClassName, ndgbmpInputTypes, ndgbmpWriterSpiNames,
			ndgbmpSupportsStandardStreamMetadataFormat, ndgbmpNativeStreamMetadataFormatName, ndgbmpNativeStreamMetadataFormatClassName,
			ndgbmpExtraStreamMetadataFormatNames, ndgbmpExtraStreamMetadataFormatClassNames, ndgbmpSupportsStandardImageMetadataFormat,
			ndgbmpNativeImageMetadataFormatName, ndgbmpNativeImageMetadataFormatClassName, ndgbmpExtraImageMetadataFormatNames, ndgbmpExtraImageMetadataFormatClassNames);
	}

	/**
	 * Creates the corresponding reader associated with this service provider interfaces
	 * @param extension Used for certain image formats, but not this one - ignored
	 * @return Appropriate class for decoding .ndgbmp streams
	 */
	@Override
	public final ImageReader createReaderInstance (final Object extension)
	{
		return new NdgBmpReader (this);
	}

	/**
	 * @param locale The locale for which the description should be appropriate (i.e. if we wanted descriptions in multiple languages)
	 * @return Description of this SPI
	 */
	@Override
	public final String getDescription (final Locale locale)
	{
		return "NDG BMP Reader SPI";
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

			// Note this is testing one .ndgbmp file - not the whole .ndgarc file
			// so its the .ndgbmp header we need to look for
			final byte [] formatIdentifier = new byte [NdgBmpCommon.NDGBMP_FORMAT_IDENTIFIER.length];

			stream.mark ();
			stream.readFully (formatIdentifier);
			stream.reset ();

			// Compare first 6 bytes to the string 'ndgbmp'
			result = NdgBmpCommon.equalsFormatIdentifier (formatIdentifier);
		}

		return result;
	}
}
