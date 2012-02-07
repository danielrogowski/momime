package com.ndg.graphics.ndgbmp;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Iterator;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

/**
 * Class for decoding .ndgbmp streams into images
 */
public final class NdgBmpReader extends ImageReader
{
	/** Image type for 32 bit colour ARGB images */
	public static final ImageTypeSpecifier IMAGE_TYPE_ARGB = ImageTypeSpecifier.createInterleaved (ColorSpace.getInstance (ColorSpace.CS_sRGB), new int [] {3, 2, 1, 0}, DataBuffer.TYPE_BYTE, true, false);

	/** Image type for 24 bit colour RGB images (no transparency) */
	public static final ImageTypeSpecifier IMAGE_TYPE_RGB = ImageTypeSpecifier.createInterleaved (ColorSpace.getInstance (ColorSpace.CS_sRGB), new int [] {2, 1, 0}, DataBuffer.TYPE_BYTE, false, false);

	/** True if the header has been read from the stream */
	private boolean headerRead;

	/** Header: The width of the encodded image */
	private int width;

	/** Header: The height of the encodded image */
	private int height;

	/** Header: The parsing direction the image was encoded with */
	private NdgBmpParseDirection parseDirection;

	/**
	 * Header: The number of bits of precision for each colour component in the palette
	 * Typically this is 8, but it can be reduced to decrease image quality and increase compression
	 */
	private int bitsPerPaletteComponent;

	/**
	 * Header: The length in bits of RLE values.  Note that RLE values are always stored at 3 less than their actual value.
	 * That is if say the longest string of continuous same-colour pixels in an image is e.g. 33, then this will
	 * be stored as 30, so rleBitLength = 5.
	 */
	private int rleBitLength;

	/** Header: The number of unique colours in this image = the number of palette entries */
	private int uniqueColours;

	/**
	 * Derived from header value uniqueColours.  The number of bits needed to store the value "uniqueColours".  Note colours are identified by their
	 * index into the palette, so in an image with say 256 unique colours, these will be numbered 0 thru 255.
	 * Colour index "uniqueColours", 256 in this case, is the special identifier indicating the start of a run of
	 * continuous colour pixels.  So for an image with 256 colours, its not sufficient to be able to represent
	 * only 0 thru 255, we need to be able to store 256 as well, so colourBitLength = 9.
	 */
	private int colourBitLength;

	/** The palette of all the colours in this image, we can tell if the colour table has been read yet or not by checking whether this is null */
	private long [] colourTable;

	/** In a run of continuous colour pixels, how many more pixels of this colour will there be */
	private long thisRLE;

	/** In a run of continuous colour pixels, the colour of the pixels */
	private long currentColour;

	/**
	 * Creates a new NdgBmpReader object
	 * @param anOriginatingProvider The service provider object which created this reader
	 */
	public NdgBmpReader (final ImageReaderSpi anOriginatingProvider)
	{
		super (anOriginatingProvider);
		headerRead = false;
	}

	/**
	 * @param allowSearch Whether to proceed with determining the number of images even if it will be an expensive operation - which it isn't, so this is ignored
	 * @return Number of images in the .ndgbmp file (always 1)
	 */
	@Override
	public final int getNumImages (final boolean allowSearch)
	{
		return 1;
	}

	/**
	 * @param imageIndex The number of the image in the .ndgbmp file that we want (must always be 0)
	 * @return List of types of image (e.g. colour model) which we can decode to, which depends on the colours in the colour table
	 * @throws IOException If there is a problem reading the image header or colour table
	 */
	@Override
	public final Iterator<ImageTypeSpecifier> getImageTypes (final int imageIndex)
		throws IOException
	{
		if (imageIndex != 0)
			throw new IndexOutOfBoundsException ("com.ndg.ndgbmp.NdgBmpReader.getImageTypes: .ndgbmp format only supports a single image, but requested index " + imageIndex);

		final ArrayList<ImageTypeSpecifier> list = new ArrayList<ImageTypeSpecifier> ();

		// We can only decode to RGB format if none of the colours in the colour table have an Alpha value
		// If this is the case, then RGB is actually more appropriate than ARGB so put it first in the list
		ensureColourTableRead ();

		boolean rgbOk = true;
		int colourNo = 0;
		while ((rgbOk) && (colourNo < colourTable.length))
		{
			final long c = colourTable [colourNo];
			if ((c >> 24) != 0xFF)
				rgbOk = false;
			else
				colourNo++;
		}

		if (rgbOk)
			list.add (IMAGE_TYPE_RGB);

		// We can always decode to ARGB format
		list.add (IMAGE_TYPE_ARGB);

		return list.iterator ();
	}

	/**
	 * @param imageIndex The number of the image in the .ndgbmp file that we want (must always be 0)
	 * @return The width of the image
	 * @throws IOException If there is a problem reading the image header
	 */
	@Override
	public final int getWidth (final int imageIndex)
		throws IOException
	{
		if (imageIndex != 0)
			throw new IndexOutOfBoundsException ("com.ndg.ndgbmp.NdgBmpReader.getWidth: .ndgbmp format only supports a single image, but requested index " + imageIndex);

		ensureHeaderRead ();
		return width;
	}

	/**
	 * @param imageIndex The number of the image in the .ndgbmp file that we want (must always be 0)
	 * @return The height of the image
	 * @throws IOException If there is a problem reading the image header
	 */
	@Override
	public final int getHeight (final int imageIndex)
		throws IOException
	{
		if (imageIndex != 0)
			throw new IndexOutOfBoundsException ("com.ndg.ndgbmp.NdgBmpReader.getHeight: .ndgbmp format only supports a single image, but requested index " + imageIndex);

		ensureHeaderRead ();
		return height;
	}

	/**
	 * Reads in the .ndgbmp header from the stream, if we haven't done so already
	 * @throws IOException If there is a problem reading the image header
	 */
	private final void ensureHeaderRead ()
		throws IOException
	{
		if (!headerRead)
		{
			// Get stream
			if (!(getInput () instanceof ImageInputStream))
				throw new IOException ("com.ndg.ndgbmp.NdgBmpReader.ensureHeaderRead: Don't have ImageInputStream to read from");

			final ImageInputStream stream = (ImageInputStream) getInput ();
			stream.setByteOrder (ByteOrder.LITTLE_ENDIAN);

			// Read and check fhe format identifier
			final byte [] formatIdentifier = new byte [NdgBmpCommon.NDGBMP_FORMAT_IDENTIFIER.length];

			stream.readFully (formatIdentifier);
			if (!NdgBmpCommon.equalsFormatIdentifier (formatIdentifier))
				throw new NdgBmpException ("com.ndg.ndgbmp.NdgBmpReader.ensureHeaderRead: Stream does not begin with expected .ndgbmp format identifier");

			// Read and check major and minor versions
			final int majorVersion = stream.readUnsignedByte ();
			final int minorVersion = stream.readUnsignedByte ();

			if ((majorVersion != NdgBmpCommon.NDGBMP_MAJOR_VERSION) || (minorVersion != NdgBmpCommon.NDGBMP_MINOR_VERSION))
				throw new NdgBmpException ("com.ndg.ndgbmp.NdgBmpReader.ensureHeaderRead: Stream is not a supported major/minor version");

			// Read remaining header values
			width = stream.readInt ();
			height = stream.readInt ();
			parseDirection = NdgBmpParseDirection.convertFromExternalValue (stream.readInt ());
			rleBitLength = stream.readInt ();
			uniqueColours = stream.readInt ();
			bitsPerPaletteComponent = stream.readInt ();

			// Find the number of bits we need to use for palette indexes
			// Not length-1, because colour 'length' is the RLE indicator
			colourBitLength = NdgBmpCommon.bitsNeededToStore (uniqueColours);

			// Ensure we only read the header once
			headerRead = true;
		}
	}

	/**
	 * Reads in the .ndgbmp header and colour table from the stream, if we haven't done so already
	 * @throws IOException If there is a problem reading the image header or colour table
	 */
	private final void ensureColourTableRead ()
		throws IOException
	{
		if (colourTable == null)
		{
			ensureHeaderRead ();

			// Get stream
			if (!(getInput () instanceof ImageInputStream))
				throw new IOException ("com.ndg.ndgbmp.NdgBmpReader.ensureColourTableRead: Don't have ImageInputStream to read from");

			final ImageInputStream stream = (ImageInputStream) getInput ();

			// The bitmask we need to isolate the colour components in the palette is not the same as the bitmask
			// we need to reduce colours during compression
			final long colourBitmask = 0xFF >> (8 - bitsPerPaletteComponent);

			// Load the colour table
			colourTable = new long [uniqueColours];
			for (int colourNo = 0; colourNo < uniqueColours; colourNo++)
			{
				long c = stream.readBits (4 * bitsPerPaletteComponent);

				// Fiddle palette order, and deal with bitsPerPaletteComponent at the same time if need be
				final long c1 = (c & colourBitmask) << (8 - bitsPerPaletteComponent);
			 	final long c2 = ((c >> bitsPerPaletteComponent) & colourBitmask) << (8 - bitsPerPaletteComponent);
			 	final long c3 = ((c >> (bitsPerPaletteComponent * 2)) & colourBitmask) << (8 - bitsPerPaletteComponent);
			 	final long c4 = ((c >> (bitsPerPaletteComponent * 3)) & colourBitmask) << (8 - bitsPerPaletteComponent);

			 	c = (c4 << 24) | (c1 << 16) | (c2 << 8) | c3;

				 // Store colour
				 colourTable [colourNo] = c;
			}
		}
	}

	/**
	 * Handles actually reading the stream and decoding the image
	 * @param imageIndex The number of the image in the .ndgbmp file that we want (must always be 0)
	 * @param param Parameters controlling decoding of the image
	 * @return The loaded image
	 * @throws IOException If there is a problem reading from the stream
	 */
	@Override
	public final BufferedImage read (final int imageIndex, final ImageReadParam param)
		throws IOException
	{
		if (imageIndex != 0)
			throw new IndexOutOfBoundsException ("com.ndg.ndgbmp.NdgBmpReader.read: .ndgbmp format only supports a single image, but requested index " + imageIndex);

		ensureColourTableRead ();

		// Get stream
		if (!(getInput () instanceof ImageInputStream))
			throw new IOException ("com.ndg.ndgbmp.NdgBmpReader.read: Don't have ImageInputStream to read from");

		final ImageInputStream stream = (ImageInputStream) getInput ();

		// Create image
		final BufferedImage image = getDestination (param, getImageTypes (imageIndex), width, height);

		// Decode the image
		thisRLE = 0;
		currentColour = 0;

		switch (parseDirection)
		{
			case NDGBMP_PARSE_DIRECTION_ROW_FIRST:
				for (int y = 0; y < height; y++)
					for (int x = 0; x < width; x++)
						image.setRGB (x, y, (int) getNextPixelColour (stream));

				break;

			case NDGBMP_PARSE_DIRECTION_COLUMN_FIRST:
				for (int x = 0; x < width; x++)
					for (int y = 0; y < height; y++)
						image.setRGB (x, y, (int) getNextPixelColour (stream));

				break;

			default:
				throw new NdgBmpException ("com.ndg.ndgbmp.NdgBmpReader.read: Unsupported parse direction");
		}

		return image;
	}

	/**
	 * @param stream The stream to read the pixel colour from
	 * @return Next pixel colour value read from the stream, after decoding the RLE encoding
	 * @throws IOException If there is a problem reading from the stream
	 */
	private final long getNextPixelColour (final ImageInputStream stream)
		throws IOException
	{
		// Do we need to read another code from the stream?
		if (thisRLE == 0)
		{
			int n = (int) stream.readBits (colourBitLength);
			if (n == colourTable.length)
			{
				// This is a RLE set
				thisRLE = stream.readBits (rleBitLength) + 3;
				n = (int) stream.readBits (colourBitLength);
				currentColour = colourTable [n];
			}
			else
			{
				// This is a single colour value
				currentColour = colourTable [n];
				thisRLE = 1;
			}
		}

		// Now there must be a pixel available
		thisRLE--;
		return currentColour;
	}

	/**
	 * @param imageIndex The number of the image in the .ndgbmp file that we want (must always be 0)
	 * @return null, because no metadata is supported for .ndgbmp images
	 */
	@Override
	public final IIOMetadata getImageMetadata (final int imageIndex)
	{
		if (imageIndex != 0)
			throw new IndexOutOfBoundsException ("com.ndg.ndgbmp.NdgBmpReader.getImageMetadata: .ndgbmp format only supports a single image, but requested index " + imageIndex);

		return null;
	}

	/**
	 * @return null, because no metadata is supported for .ndgbmp images
	 */
	@Override
	public final IIOMetadata getStreamMetadata ()
	{
		return null;
	}
}
