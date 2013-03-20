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
	private static final String vendorName = "Nigel Gay";

	/** Version of the .ndgbmp file structure */
	private static final String version = "1.0";

	/** Class responsible for encoding .ndgbmp streams */
	private static final String writerClassName = "com.ndg.graphics.ndgbmp.NdgBmpWriter";

	/** SPI for decoding .ndgbmp streams */
	private static final String [] readerSpiNames = { "com.ndg.graphics.ndgbmp.NdgBmpReaderSpi" };

	/** Writes to ImageOutputStream only */
	@SuppressWarnings ("rawtypes")
	private static final Class [] outputTypes = {ImageOutputStream.class};

	/** Descriptive list of the format(s) supported by this writer */
	private static final String [] names = { "NDG BMP", "ndg bmp" };

	/** List of the file extension(s) supported by this writer */
	private static final String [] suffixes = { "NDGBMP", "ndgbmp" };

	/** List of the MIME type(s) supported by this writer */
	private static final String [] MIMETypes = { "image/ndgbmp" };

	/** .ndgbmp writer currently doesn't support any metadata */
	private static final boolean supportsStandardStreamMetadataFormat = false;

	/** .ndgbmp writer currently doesn't support any metadata */
	private static final String nativeStreamMetadataFormatName = null;

	/** .ndgbmp writer currently doesn't support any metadata */
	private static final String nativeStreamMetadataFormatClassName = null;

	/** .ndgbmp writer currently doesn't support any metadata */
	private static final String [] extraStreamMetadataFormatNames = null;

	/** .ndgbmp writer currently doesn't support any metadata */
	private static final String [] extraStreamMetadataFormatClassNames = null;

	/** .ndgbmp writer currently doesn't support any metadata */
	private static final boolean supportsStandardImageMetadataFormat = false;

	/** .ndgbmp writer currently doesn't support any metadata */
	private static final String nativeImageMetadataFormatName = null;

	/** .ndgbmp writer currently doesn't support any metadata */
	private static final String nativeImageMetadataFormatClassName = null;

	/** .ndgbmp writer currently doesn't support any metadata */
	private static final String [] extraImageMetadataFormatNames = null;

	/** .ndgbmp writer currently doesn't support any metadata */
	private static final String [] extraImageMetadataFormatClassNames = null;

	/**
	 * Fills in all the details about the .ndgbmp format
	 */
	public NdgBmpWriterSpi ()
	{
		super (vendorName, version, names, suffixes, MIMETypes, writerClassName, outputTypes, readerSpiNames,
			supportsStandardStreamMetadataFormat, nativeStreamMetadataFormatName, nativeStreamMetadataFormatClassName,
			extraStreamMetadataFormatNames, extraStreamMetadataFormatClassNames, supportsStandardImageMetadataFormat,
			nativeImageMetadataFormatName, nativeImageMetadataFormatClassName, extraImageMetadataFormatNames, extraImageMetadataFormatClassNames);
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
