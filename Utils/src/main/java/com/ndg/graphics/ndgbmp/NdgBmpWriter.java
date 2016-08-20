package com.ndg.graphics.ndgbmp;

import java.awt.image.Raster;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.IIOImage;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;

/**
 * Class for encoding images into .ndgbmp streams
 */
public final class NdgBmpWriter extends ImageWriter
{
    /** The output stream where written to */
    private ImageOutputStream ios = null;

	/**
	 * Creates a new NdgBmpWriter object
	 * @param anOriginatingProvider The service provider object which created this writer
	 */
	public NdgBmpWriter (final ImageWriterSpi anOriginatingProvider)
	{
		super (anOriginatingProvider);
	}

	/**
	 * Copied from the way the .bmp, .jpg, .png, etc readers in the JDK handle their streams
	 */
	@Override
	public final void setOutput (final Object out)
	{
		super.setOutput (out);
		if (out != null)
		{
			if (!(out instanceof ImageOutputStream))
				throw new IllegalArgumentException ("NdgBmpWriter: Output not an ImageOutputStream!");
			
			ios = (ImageOutputStream) out;
			ios.setByteOrder (ByteOrder.LITTLE_ENDIAN);
		}
		else
			ios = null;
	}
	
	/**
	 * Encodes an image in .ndgbmp format
	 * @param streamMetadata Stream metadata, ignored
	 * @param image The image to encode
	 * @param param Parameters specifying how the image is written, ignored
	 * @throws IOException If there is a problem writing to the stream
	 */
	@Override
	public final void write (@SuppressWarnings ("unused") final IIOMetadata streamMetadata, final IIOImage image, @SuppressWarnings ("unused") final ImageWriteParam param)
		throws IOException
	{
		// Get stream
		if (ios == null)
			throw new IOException ("com.ndg.ndgbmp.NdgBmpReader.read: Don't have ImageOutputStream to write to");

		// Get at the image data
		final Raster raster = image.getRenderedImage ().getData ();

		// Java writer doesn't currently support requesting less than 8 bits per pixel to reduce the encoded image size
		// However the code for this has been included, so it should simply be a case of reducing this value
		// If we did want to add this, would have to supply it through the ImageWriteParam
		// Something else to consider if we add this support is that the reader assumes any image with any alpha values other than 0xFF
		// contains transparency, and must therefore be represented as ARGB instead of RGB
		// So even specifying 7 here will reduce the alpha values to 0xFE and make the image appear somewhat transparent after decoding
		final int bitsPerPaletteComponent = 8;

		// Generate bitmask that will reduce colours to the specified bitsPerPaletteComponent
		// i.e. for 7 we want binary 11111110111111101111111011111110
		final long colourBitmaskSingleChannel = (0xFF >> (8 - bitsPerPaletteComponent)) << (8 - bitsPerPaletteComponent);
		final long colourBitmask = (colourBitmaskSingleChannel << 24) + (colourBitmaskSingleChannel << 16) + (colourBitmaskSingleChannel << 8) + colourBitmaskSingleChannel;

		// FIRST PASS (column-first direction) - we do 3 things on the first pass:
		// 1) Build the colour table
		// 2) Build an array of the colour indices, so we don't have to re-look them up on future passes
		// 3) We find the longest run length of a particular colour in the column-first parsing direction
		final List<Long> colourTable = new ArrayList<Long> ();
		final int [] [] colourIndices = new int [raster.getWidth ()] [raster.getHeight ()];
		final int [] colourBytes = new int [raster.getNumBands ()];
		final int rleBitLengthColFirst;

		{
			long longestRunLength = 0;
			long thisRunLength = 0;		// To force the first colour to be used
			int currentColourIndex = 0;

			for (int x = 0; x < raster.getWidth (); x++)
				for (int y = 0; y < raster.getHeight (); y++)
				{
					raster.getPixel (x, y, colourBytes);
					final Long c = buildColourValue (colourBytes, colourBitmask);

					// Already in the colour table?
					int colourIndex = colourTable.indexOf (c);
					if (colourIndex < 0)
					{
						colourIndex = colourTable.size ();
						colourTable.add (c);
					}

					if (colourTable.indexOf (c) < 0)
						colourTable.add (c);

					// Store colour index
					colourIndices [x] [y] = colourIndex;

					// Check run length
					if ((thisRunLength == 0) || (colourIndex != currentColourIndex))
					{
						thisRunLength = 1;
						currentColourIndex = colourIndex;
					}
					else
						thisRunLength++;

					if (thisRunLength > longestRunLength)
						longestRunLength = thisRunLength;
				}

			// Now convert from a number (e.g. 100) to a number of bits (e.g. 7) }
			if (longestRunLength == 0)
				rleBitLengthColFirst = 0;	// Must be a 0x0 image
			else
				rleBitLengthColFirst = NdgBmpCommon.bitsNeededToStore (longestRunLength - 3);
		}

		// SECOND PASS (row-first direction) - find the longest run length of a particular colour in the row-first parsing direction
		final int rleBitLengthRowFirst;
		{
			long longestRunLength = 0;
			long thisRunLength = 0;		// To force the first colour to be used
			int currentColourIndex = 0;

			for (int y = 0; y < raster.getHeight (); y++)
				for (int x = 0; x < raster.getWidth (); x++)
				{
					final int colourIndex = colourIndices [x] [y];

					// Check run length
					if ((thisRunLength == 0) || (colourIndex != currentColourIndex))
					{
						thisRunLength = 1;
						currentColourIndex = colourIndex;
					}
					else
						thisRunLength++;

					if (thisRunLength > longestRunLength)
						longestRunLength = thisRunLength;
				}

			// Now convert from a number (e.g. 100) to a number of bits (e.g. 7) }
			if (longestRunLength == 0)
				rleBitLengthRowFirst = 0;	// Must be a 0x0 image
			else
				rleBitLengthRowFirst = NdgBmpCommon.bitsNeededToStore (longestRunLength - 3);
		}

		// THIRD & FOURTH PASS - test encoding the image in both parsing directions to see which gives a smaller file
		final long compressedSizeColFirst = compress (colourTable, colourIndices, bitsPerPaletteComponent, raster.getWidth (), raster.getHeight (),
			rleBitLengthColFirst, NdgBmpParseDirection.NDGBMP_PARSE_DIRECTION_COLUMN_FIRST, null);

		final long compressedSizeRowFirst = compress (colourTable, colourIndices, bitsPerPaletteComponent, raster.getWidth (), raster.getHeight (),
			rleBitLengthRowFirst, NdgBmpParseDirection.NDGBMP_PARSE_DIRECTION_ROW_FIRST, null);

		// So now can decide which is better
		final NdgBmpParseDirection parseDirection;
		final int rleBitLength;

		if (compressedSizeRowFirst < compressedSizeColFirst)
		{
			parseDirection = NdgBmpParseDirection.NDGBMP_PARSE_DIRECTION_ROW_FIRST;
			rleBitLength = rleBitLengthRowFirst;
		}
		else
		{
			parseDirection = NdgBmpParseDirection.NDGBMP_PARSE_DIRECTION_COLUMN_FIRST;
			rleBitLength = rleBitLengthColFirst;
		}

		// FIFTH PASS - actually output the image, starting with the header
		// Format identifier
		ios.write (NdgBmpCommon.NDGBMP_FORMAT_IDENTIFIER);

		// Major and minor version
		ios.writeByte (NdgBmpCommon.NDGBMP_MAJOR_VERSION);
		ios.writeByte (NdgBmpCommon.NDGBMP_MINOR_VERSION);

		// Write remaining header values
		ios.writeInt (raster.getWidth ());
		ios.writeInt (raster.getHeight ());
		ios.writeInt (parseDirection.convertToExternalValue ());
		ios.writeInt (rleBitLength);
		ios.writeInt (colourTable.size ());
		ios.writeInt (bitsPerPaletteComponent);

		// Write the colour table and actual image data
		compress (colourTable, colourIndices, bitsPerPaletteComponent, raster.getWidth (), raster.getHeight (), rleBitLength, parseDirection, ios);
		ios.flush ();
	}

	/**
	 * Converts colour band values read from the raster into a usable value
	 * @param bands Int array read from raster.getPixel ()
	 * @param colourBitmask Bitmask for reducing colour definition (so longer RLE values can be used)
	 * @return 32 bit colour value
	 */
	private static long buildColourValue (final int [] bands, final long colourBitmask)
	{
		long c;

		// Deal with greyscale first
		if (bands.length == 1)
		{
			final long c1 = bands [0];
			c = (c1 << 16) | (c1 << 8) | c1;
			c = c + 0xFF000000l;	// Solid colour
		}
		else
		{
			// Reverse values, ignore alpha for now
			final long c1 = bands [0];
			final long c2 = bands [1];
			final long c3 = bands [2];
			c = (c3 << 16) | (c2 << 8) | c1;

			// May or may not have explicit alpha values
			// If not, then treat magenta as transparent, since this is how I specify transparency in .bmp images
			if (bands.length == 3)
			{
				if (c == 0xFF00FFl)
					c = 0;		// Transparent
				else
					c = c + 0xFF000000l;	// Solid colour
			}
			else
			{
				// Explicit alpha
				final long c4 = bands [3];
				c = (c4 << 24) | c;
			}
		}

		// Colour reduction
		return c & colourBitmask;
	}

	/**
	 * Does bulk of compression work, writing out everything except for the header
	 * @param colourTable Table of unique colours in the image
	 * @param colourIndices Index into colour table of pixel at each location
	 * @param bitsPerPaletteComponent The number of bits of precision for each colour component in the palette
	 * @param width Width of the image
	 * @param height Height of the image
	 * @param rleBitLength Number of bits to use for writing RLE values
	 * @param parseDirection The direction to parse the pixels in
	 * @param stream Stream to output the bits to, or null to just count how many bytes would be written without actually outputting them
	 * @return Number of bytes written
	 * @throws IOException If stream is non-null and there's a problem writing to it
	 */
	private static long compress (final List<Long> colourTable, final int [] [] colourIndices, final int bitsPerPaletteComponent, final int width, final int height, final int rleBitLength,
		final NdgBmpParseDirection parseDirection, final ImageOutputStream stream)
		throws IOException
	{
		long bitsWritten = 0;

		// Write out colour table - if we're only looking for the size then skip the loop entirely
		if (stream == null)
			bitsWritten = bitsWritten + (colourTable.size () * bitsPerPaletteComponent * 4);
		else
			for (final Long c : colourTable)
			{
				// Repack colour bits if necessary (the values in the colour table are 32 bits with gaps in for the colour reduction)
				final long thisColour;
				if (bitsPerPaletteComponent == 8)
					thisColour = c;
				else
				{
					final long c1 = (c | 0xFF) >> (8 - bitsPerPaletteComponent);
					final long c2 = ((c >> 8) | 0xFF) >> (8 - bitsPerPaletteComponent);
					final long c3 = ((c >> 16) | 0xFF) >> (8 - bitsPerPaletteComponent);
					final long c4 = ((c >> 24) | 0xFF) >> (8 - bitsPerPaletteComponent);

					thisColour = (c4 << (bitsPerPaletteComponent * 3)) +
						(c3 << (bitsPerPaletteComponent * 2)) +
						(c2 << bitsPerPaletteComponent) +
						c1;
				}

				stream.writeBits (thisColour, bitsPerPaletteComponent * 4);
			}

		// Find the number of bits we need to use for palette indexes
		// Not length-1, because colour 'length' is the RLE indicator
		final int colourBitLength = NdgBmpCommon.bitsNeededToStore (colourTable.size ());
		long thisRunLength = 0;
		int currentColourIndex = 0;

		switch (parseDirection)
		{
			case NDGBMP_PARSE_DIRECTION_COLUMN_FIRST:
				for (int x = 0; x < width; x++)
					for (int y = 0; y < height; y++)
					{
						final int colourIndex = colourIndices [x] [y];
						if ((thisRunLength == 0) || (colourIndex != currentColourIndex))
						{
							// Write out old RLE
							bitsWritten = bitsWritten + writeRle (thisRunLength, currentColourIndex, colourBitLength, rleBitLength, colourTable.size (), stream);

							// Start a new RLE
							thisRunLength = 1;
							currentColourIndex = colourIndex;
						}
						else
							// Continuing RLE
							thisRunLength++;
					}
				break;

			case NDGBMP_PARSE_DIRECTION_ROW_FIRST:
				for (int y = 0; y < height; y++)
					for (int x = 0; x < width; x++)
					{
						final int colourIndex = colourIndices [x] [y];
						if ((thisRunLength == 0) || (colourIndex != currentColourIndex))
						{
							// Write out old RLE
							bitsWritten = bitsWritten + writeRle (thisRunLength, currentColourIndex, colourBitLength, rleBitLength, colourTable.size (), stream);

							// Start a new RLE
							thisRunLength = 1;
							currentColourIndex = colourIndex;
						}
						else
							// Continuing RLE
							thisRunLength++;
					}
				break;

			default:
				throw new IllegalArgumentException ("com.ndg.graphics.ndgbmp.NdgBmpWriter.compress: Invalid parse direction");
		}

		// Write out final RLE value
		bitsWritten = bitsWritten + writeRle (thisRunLength, currentColourIndex, colourBitLength, rleBitLength, colourTable.size (), stream);

		// Output number of bytes written
		return (bitsWritten + 7) / 8;
	}

	/**
	 * Writes out bits representing one or more pixels of a single colour
	 * @param runLength Number of pixels of this colour to output
	 * @param colourIndex The colour to output
	 * @param colourBitLength Number of bits to use for writing colour indices
	 * @param rleBitLength Number of bits to use for writing RLE values
	 * @param uniqueColours Number of unique colours in the image = special value indicating an RLE
	 * @param stream Stream to output the bits to, or null to just count how many bytes would be written without actually outputting them
	 * @return Number of bits written
	 * @throws IOException
	 */
	private static final long writeRle (final long runLength, final int colourIndex, final int colourBitLength, final int rleBitLength, final int uniqueColours, final ImageOutputStream stream)
		throws IOException
	{
		long bitsWritten = 0;

		if (runLength > 0)
		{
			// How many bits will this take to write out as an RLE, and how many will it take if we just write the colour values out?
			final long bitsRle = rleBitLength + (colourBitLength * 2);		// RLE indicator, nbr pixels, colour
			final long bitsRaw = colourBitLength * runLength;

			if (bitsRle < bitsRaw)
			{
				// Should never happen
				if (runLength < 3)
					throw new NdgBmpException ("com.ndg.graphics.ndgbmp.NdgBmpWriter.writeRle: Writing out repeating RLE < 3 = " + runLength);

				// RLE value
				bitsWritten = bitsWritten + bitsRle;
				if (stream != null)
				{
					stream.writeBits (uniqueColours, colourBitLength);
					stream.writeBits (runLength - 3, rleBitLength);
					stream.writeBits (colourIndex, colourBitLength);
				}
			}
			else
			{
				// Just a bunch of colour pixels
				bitsWritten = bitsWritten + bitsRaw;
				if (stream != null)
					for (int n = 0; n < runLength; n++)
						stream.writeBits (colourIndex, colourBitLength);
			}
		}

		return bitsWritten;
	}

	/**
	 * @return null, because no metadata is supported for .ndgbmp images
	 */
	@SuppressWarnings ("unused")
	@Override
	public final IIOMetadata convertImageMetadata (final IIOMetadata inData, final ImageTypeSpecifier imageType, final ImageWriteParam param)
	{
		return null;
	}

	/**
	 * @return null, because no metadata is supported for .ndgbmp images
	 */
	@SuppressWarnings ("unused")
	@Override
	public final IIOMetadata convertStreamMetadata (final IIOMetadata inData, final ImageWriteParam param)
	{
		return null;
	}

	/**
	 * @return null, because no metadata is supported for .ndgbmp images
	 */
	@SuppressWarnings ("unused")
	@Override
	public final IIOMetadata getDefaultImageMetadata (final ImageTypeSpecifier imageType, final ImageWriteParam param)
	{
		return null;
	}

	/**
	 * @return null, because no metadata is supported for .ndgbmp images
	 */
	@SuppressWarnings ("unused")
	@Override
	public final IIOMetadata getDefaultStreamMetadata (final ImageWriteParam param)
	{
		return null;
	}
}
