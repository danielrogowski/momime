package com.ndg.graphics.ndgbmp;

import java.util.Locale;

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;

/**
 * Service Provider Interface for writing .ndgbmp format images
 */
public final class NdgBmpWriterSpi extends ImageWriterSpi
{
	/** Name of the vendor of the .ndgbmp format */
	private static final String ndgbmpVendorName = "Nigel Gay";

	/** Version of the .ndgbmp file structure */
	private static final String ndgbmpVersion = "1.0";

	/** Class responsible for encoding .ndgbmp streams */
	private static final String ndgbmpWriterClassName = "com.ndg.graphics.ndgbmp.NdgBmpWriter";

	/** SPI for decoding .ndgbmp streams */
	private static final String [] ndgbmpReaderSpiNames = { "com.ndg.graphics.ndgbmp.NdgBmpReaderSpi" };

	/** Writes to ImageOutputStream only */
	@SuppressWarnings ("rawtypes")
	private static final Class [] ndgbmpOutputTypes = {ImageOutputStream.class};

	/** Descriptive list of the format(s) supported by this writer */
	private static final String [] ndgbmpNames = { "NDG BMP", "ndg bmp" };

	/** List of the file extension(s) supported by this writer */
	private static final String [] ndgbmpSuffixes = { "NDGBMP", "ndgbmp" };

	/** List of the MIME type(s) supported by this writer */
	private static final String [] ndgbmpMIMETypes = { "image/ndgbmp" };

	/** .ndgbmp writer currently doesn't support any metadata */
	private static final boolean ndgbmpSupportsStandardStreamMetadataFormat = false;

	/** .ndgbmp writer currently doesn't support any metadata */
	private static final String ndgbmpNativeStreamMetadataFormatName = null;

	/** .ndgbmp writer currently doesn't support any metadata */
	private static final String ndgbmpNativeStreamMetadataFormatClassName = null;

	/** .ndgbmp writer currently doesn't support any metadata */
	private static final String [] ndgbmpExtraStreamMetadataFormatNames = null;

	/** .ndgbmp writer currently doesn't support any metadata */
	private static final String [] ndgbmpExtraStreamMetadataFormatClassNames = null;

	/** .ndgbmp writer currently doesn't support any metadata */
	private static final boolean ndgbmpSupportsStandardImageMetadataFormat = false;

	/** .ndgbmp writer currently doesn't support any metadata */
	private static final String ndgbmpNativeImageMetadataFormatName = null;

	/** .ndgbmp writer currently doesn't support any metadata */
	private static final String ndgbmpNativeImageMetadataFormatClassName = null;

	/** .ndgbmp writer currently doesn't support any metadata */
	private static final String [] ndgbmpExtraImageMetadataFormatNames = null;

	/** .ndgbmp writer currently doesn't support any metadata */
	private static final String [] ndgbmpExtraImageMetadataFormatClassNames = null;

	/**
	 * Fills in all the details about the .ndgbmp format
	 */
	public NdgBmpWriterSpi ()
	{
		super (ndgbmpVendorName, ndgbmpVersion, ndgbmpNames, ndgbmpSuffixes, ndgbmpMIMETypes, ndgbmpWriterClassName, ndgbmpOutputTypes, ndgbmpReaderSpiNames,
			ndgbmpSupportsStandardStreamMetadataFormat, ndgbmpNativeStreamMetadataFormatName, ndgbmpNativeStreamMetadataFormatClassName,
			ndgbmpExtraStreamMetadataFormatNames, ndgbmpExtraStreamMetadataFormatClassNames, ndgbmpSupportsStandardImageMetadataFormat,
			ndgbmpNativeImageMetadataFormatName, ndgbmpNativeImageMetadataFormatClassName, ndgbmpExtraImageMetadataFormatNames, ndgbmpExtraImageMetadataFormatClassNames);
	}

	/**
	 * Creates the corresponding writer associated with this service provider interfaces
	 * @param extension Used for certain image formats, but not this one - ignored
	 * @return Appropriate class for encoding .ndgbmp streams
	 */
	@Override
	public final ImageWriter createWriterInstance (final Object extension)
	{
		return new NdgBmpWriter (this);
	}

	/**
	 * @param locale The locale for which the description should be appropriate (i.e. if we wanted descriptions in multiple languages)
	 * @return Description of this SPI
	 */
	@Override
	public final String getDescription (final Locale locale)
	{
		return "NDG BMP Writer SPI";
	}

	/**
	 * @param type Type of image we want to encode
	 * @return Whether or not we are capable of encoding an image of this type
	 */
	@Override
	public final boolean canEncodeImage (final ImageTypeSpecifier type)
	{
		final int bands = type.getNumBands ();
		return ((bands == 1) || (bands == 3) || (bands == 4));		// Greyscale, RGB or RGBA
	}
}
