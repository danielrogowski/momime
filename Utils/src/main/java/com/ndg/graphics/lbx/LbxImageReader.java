package com.ndg.graphics.lbx;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

/**
 * Class for decoding LBX format images into images
 * This knows nothing of the LBX archive format - must have already positioned the stream to the correct subfile
 */
public final class LbxImageReader extends ImageReader
{
    /** The input stream where reads from */
    private ImageInputStream iis = null;

	/** True if the header has been read from the stream */
	private boolean headerRead = false;
	
	/**
	 * Header: The width of the encoded image
	 * Even in an LBX image with multiple frames, all the frames are always the same size
	 */
	private int width;

	/**
	 * Header: The height of the encoded image
	 * Even in an LBX image with multiple frames, all the frames are always the same size
	 */
	private int height;
	
	/** Header: The number of frames in this LBX image */
	private int frameCount;
	
	/** Header: The offset to the location of the palette info */
	private int paletteInfoOffset;
	
	/**
	 * Values of at least this indicate run length values
	 * Delphi version and old Java version preserves this value between each frame, so better declare it here to keep it working the same
	 */
	private int RLE_val;
	
	/** Size of the LBX image header */
	public static int LBX_IMAGE_HEADER_SIZE = 18;
	
	/**
	 * LBX images are progressive, that is frame 1 only contains the pixels that have changed since frame 0
	 * Therefore we have to decode frame 0 in order to correctly decode frame 1
	 * This special setting turns this behaviour off, so we only decode the single frame
	 */
	public static boolean decodeRequestedFrameOnly = false;
	
	/**
	 * Default Master of Magic palette, borrowed this from another LBX converter, no idea how they extracted it.
	 * Yellow font palette indexes start at		$350 in file 2
	 * Red font (some values appear wrong)	$360
	 * White font											$390
	 */
	protected static final short [] [] MOM_PALETTE =
	{
		{0x0, 0x0, 0x0},		// This first entry is transparent, not black - determined by checking Armourers' Guild frame 1 closely
		{0x8, 0x4, 0x4},
		{0x24, 0x1c, 0x18},
		{0x38, 0x30, 0x2c},
		{0x48, 0x40, 0x3c},
		{0x58, 0x50, 0x4c},
		{0x68, 0x60, 0x5c},
		{0x7c, 0x74, 0x70},
		{0x8c, 0x84, 0x80},
		{0x9c, 0x94, 0x90},
		{0xac, 0xa4, 0xa0},
		{0xc0, 0xb8, 0xb4},
		{0xd0, 0xc8, 0xc4},
		{0xe0, 0xd8, 0xd4},
		{0xf0, 0xe8, 0xe4},
		{0xfc, 0xfc, 0xfc},
		{0x38, 0x20, 0x1c},
		{0x40, 0x2c, 0x24},
		{0x48, 0x34, 0x2c},
		{0x50, 0x3c, 0x30},
		{0x58, 0x40, 0x34},
		{0x5c, 0x44, 0x38},
		{0x60, 0x48, 0x3c},
		{0x64, 0x4c, 0x3c},
		{0x68, 0x50, 0x40},
		{0x70, 0x54, 0x44},
		{0x78, 0x5c, 0x4c},
		{0x80, 0x64, 0x50},
		{0x8c, 0x70, 0x58},
		{0x94, 0x74, 0x5c},
		{0x9c, 0x7c, 0x64},
		{0xa4, 0x84, 0x68},
		{0xec, 0xc0, 0xd4},
		{0xd4, 0x98, 0xb4},
		{0xbc, 0x74, 0x94},
		{0xa4, 0x54, 0x7c},
		{0x8c, 0x38, 0x60},
		{0x74, 0x24, 0x4c},
		{0x5c, 0x10, 0x34},
		{0x44, 0x4, 0x20},
		{0xec, 0xc0, 0xc0},
		{0xd4, 0x94, 0x94},
		{0xbc, 0x74, 0x74},
		{0xa4, 0x54, 0x54},
		{0x8c, 0x38, 0x38},
		{0x74, 0x24, 0x24},
		{0x5c, 0x10, 0x10},
		{0x44, 0x4, 0x4},
		{0xec, 0xd4, 0xc0},
		{0xd4, 0xb4, 0x98},
		{0xbc, 0x98, 0x74},
		{0xa4, 0x7c, 0x54},
		{0x8c, 0x60, 0x38},
		{0x74, 0x4c, 0x24},
		{0x5c, 0x34, 0x10},
		{0x44, 0x24, 0x4},
		{0xec, 0xec, 0xc0},
		{0xd4, 0xd4, 0x94},
		{0xbc, 0xbc, 0x74},
		{0xa4, 0xa4, 0x54},
		{0x8c, 0x8c, 0x38},
		{0x74, 0x74, 0x24},
		{0x5c, 0x5c, 0x10},
		{0x44, 0x44, 0x4},
		{0xd4, 0xec, 0xbc},
		{0xb8, 0xd4, 0x98},
		{0x98, 0xbc, 0x74},
		{0x7c, 0xa4, 0x54},
		{0x60, 0x8c, 0x38},
		{0x4c, 0x74, 0x24},
		{0x38, 0x5c, 0x10},
		{0x24, 0x44, 0x4},
		{0xc0, 0xec, 0xc0},
		{0x98, 0xd4, 0x98},
		{0x74, 0xbc, 0x74},
		{0x54, 0xa4, 0x54},
		{0x38, 0x8c, 0x38},
		{0x24, 0x74, 0x24},
		{0x10, 0x5c, 0x10},
		{0x4, 0x44, 0x4},
		{0xc0, 0xec, 0xd8},
		{0x98, 0xd4, 0xb8},
		{0x74, 0xbc, 0x98},
		{0x54, 0xa4, 0x7c},
		{0x38, 0x8c, 0x60},
		{0x24, 0x74, 0x4c},
		{0x10, 0x5c, 0x38},
		{0x4, 0x44, 0x24},
		{0xf4, 0xc0, 0xa0},
		{0xe0, 0xa0, 0x84},
		{0xcc, 0x84, 0x6c},
		{0xc8, 0x8c, 0x68},
		{0xa8, 0x78, 0x54},
		{0x98, 0x68, 0x4c},
		{0x8c, 0x60, 0x44},
		{0x7c, 0x50, 0x3c},
		{0xc0, 0xd8, 0xec},
		{0x94, 0xb4, 0xd4},
		{0x70, 0x98, 0xbc},
		{0x54, 0x7c, 0xa4},
		{0x38, 0x64, 0x8c},
		{0x24, 0x4c, 0x74},
		{0x10, 0x38, 0x5c},
		{0x4, 0x24, 0x44},
		{0xc0, 0xc0, 0xec},
		{0x98, 0x98, 0xd4},
		{0x74, 0x74, 0xbc},
		{0x54, 0x54, 0xa4},
		{0x3c, 0x38, 0x8c},
		{0x24, 0x24, 0x74},
		{0x10, 0x10, 0x5c},
		{0x4, 0x4, 0x44},
		{0xd8, 0xc0, 0xec},
		{0xb8, 0x98, 0xd4},
		{0x98, 0x74, 0xbc},
		{0x7c, 0x54, 0xa4},
		{0x60, 0x38, 0x8c},
		{0x4c, 0x24, 0x74},
		{0x38, 0x10, 0x5c},
		{0x24, 0x4, 0x44},
		{0xec, 0xc0, 0xec},
		{0xd4, 0x98, 0xd4},
		{0xbc, 0x74, 0xbc},
		{0xa4, 0x54, 0xa4},
		{0x8c, 0x38, 0x8c},
		{0x74, 0x24, 0x74},
		{0x5c, 0x10, 0x5c},
		{0x44, 0x4, 0x44},
		{0xd8, 0xd0, 0xd0},
		{0xc0, 0xb0, 0xb0},
		{0xa4, 0x90, 0x90},
		{0x8c, 0x74, 0x74},
		{0x78, 0x5c, 0x5c},
		{0x68, 0x4c, 0x4c},
		{0x5c, 0x3c, 0x3c},
		{0x48, 0x2c, 0x2c},
		{0xd0, 0xd8, 0xd0},
		{0xb0, 0xc0, 0xb0},
		{0x90, 0xa4, 0x90},
		{0x74, 0x8c, 0x74},
		{0x5c, 0x78, 0x5c},
		{0x4c, 0x68, 0x4c},
		{0x3c, 0x5c, 0x3c},
		{0x2c, 0x48, 0x2c},
		{0xc8, 0xc8, 0xd4},
		{0xb0, 0xb0, 0xc0},
		{0x90, 0x90, 0xa4},
		{0x74, 0x74, 0x8c},
		{0x5c, 0x5c, 0x78},
		{0x4c, 0x4c, 0x68},
		{0x3c, 0x3c, 0x5c},
		{0x2c, 0x2c, 0x48},
		{0xd8, 0xdc, 0xec},
		{0xc8, 0xcc, 0xdc},
		{0xb8, 0xc0, 0xd4},
		{0xa8, 0xb8, 0xcc},
		{0x9c, 0xb0, 0xcc},
		{0x94, 0xac, 0xcc},
		{0x88, 0xa4, 0xcc},
		{0x88, 0x94, 0xdc},
		{0xfc, 0xf0, 0x90},
		{0xfc, 0xe4, 0x60},
		{0xfc, 0xc8, 0x24},
		{0xfc, 0xac, 0xc},
		{0xfc, 0x78, 0x10},
		{0xd0, 0x1c, 0x0},
		{0x98, 0x0, 0x0},
		{0x58, 0x0, 0x0},
		{0x90, 0xf0, 0xfc},
		{0x60, 0xe4, 0xfc},
		{0x24, 0xc8, 0xfc},
		{0xc, 0xac, 0xfc},
		{0x10, 0x78, 0xfc},
		{0x0, 0x1c, 0xd0},
		{0x0, 0x0, 0x98},
		{0x0, 0x0, 0x58},
		{0xfc, 0xc8, 0x64},
		{0xfc, 0xb4, 0x2c},
		{0xec, 0xa4, 0x24},
		{0xdc, 0x94, 0x1c},
		{0xcc, 0x88, 0x18},
		{0xbc, 0x7c, 0x14},
		{0xa4, 0x6c, 0x1c},
		{0x8c, 0x60, 0x24},
		{0x78, 0x54, 0x24},
		{0x60, 0x44, 0x24},
		{0x48, 0x38, 0x24},
		{0x34, 0x28, 0x1c},
		{0x90, 0x68, 0x34},
		{0x90, 0x64, 0x2c},
		{0x94, 0x6c, 0x34},
		{0x94, 0x70, 0x40},
		{0x8c, 0x5c, 0x24},
		{0x90, 0x64, 0x2c},
		{0x90, 0x68, 0x30},
		{0x98, 0x78, 0x4c},
		{0x60, 0x3c, 0x2c},
		{0x54, 0xa4, 0xa4},
		{0xc0, 0x0, 0x0},
		{0xfc, 0x88, 0xe0},
		{0xfc, 0x58, 0x84},
		{0xf4, 0x0, 0xc},
		{0xd4, 0x0, 0x0},
		{0xac, 0x0, 0x0},
		{0xe8, 0xa8, 0xfc},
		{0xe0, 0x7c, 0xfc},
		{0xd0, 0x3c, 0xfc},
		{0xc4, 0x0, 0xfc},
		{0x90, 0x0, 0xbc},
		{0xfc, 0xf4, 0x7c},
		{0xfc, 0xe4, 0x0},
		{0xe4, 0xd0, 0x0},
		{0xa4, 0x98, 0x0},
		{0x64, 0x58, 0x0},
		{0xac, 0xfc, 0xa8},
		{0x74, 0xe4, 0x70},
		{0x0, 0xbc, 0x0},
		{0x0, 0xa4, 0x0},
		{0x0, 0x7c, 0x0},
		{0xac, 0xa8, 0xfc},
		{0x80, 0x7c, 0xfc},
		{0x0, 0x0, 0xfc},
		{0x0, 0x0, 0xbc},
		{0x0, 0x0, 0x7c},
		{0x30, 0x30, 0x50},
		{0x28, 0x28, 0x48},
		{0x24, 0x24, 0x40},
		{0x20, 0x1c, 0x38},
		{0x1c, 0x18, 0x34},
		{0x18, 0x14, 0x2c},
		{0x14, 0x10, 0x24},
		{0x10, 0xc, 0x20},
		{0xa0, 0xa0, 0xb4},
		{0x88, 0x88, 0xa4},
		{0x74, 0x74, 0x90},
		{0x60, 0x60, 0x80},
		{0x50, 0x4c, 0x70},
		{0x40, 0x3c, 0x60},
		{0x30, 0x2c, 0x50},
		{0x24, 0x20, 0x40},
		{0x18, 0x14, 0x30},
		{0x10, 0xc, 0x20},
		{0x14, 0xc, 0x8},
		{0x18, 0x10, 0xc},
		{0x0, 0x0, 0x0},
		{0x0, 0x0, 0x0},
		{0x0, 0x0, 0x0},
		{0x0, 0x0, 0x0},
		{0x0, 0x0, 0x0},
		{0x0, 0x0, 0x0},
		{0x0, 0x0, 0x0},
		{0x0, 0x0, 0x0},
		{0x0, 0x0, 0x0},
		{0x0, 0x0, 0x0},
		{0x0, 0x0, 0x0},
		{0x0, 0x0, 0x0}
	};

	/**
	 * Creates a new LbxImageReader object
	 * @param anOriginatingProvider The service provider object which created this reader
	 */
	public LbxImageReader (final ImageReaderSpi anOriginatingProvider)
	{
		super (anOriginatingProvider);
	}

    /**
     * Copied from the way the .bmp, .jpg, .png, etc readers in the JDK handle their streams
     * NB. The iis read here intentionally never gets closed - the contracts for ImageIO.read and ImageIO.write state that they should not close the streams
     */
	@Override
    public final void setInput (final Object anInput, final boolean aSeekForwardOnly, final boolean anIgnoreMetadata)
	{
		super.setInput (anInput, aSeekForwardOnly, anIgnoreMetadata);
		
		iis = (ImageInputStream) input; // Always works
		if (iis != null)
			iis.setByteOrder (ByteOrder.LITTLE_ENDIAN);
		headerRead = false;
    }
	
	/**
	 * @param allowSearch Whether to proceed with determining the number of images even if it will be an expensive operation - which it isn't, so this is ignored
	 * @return Number of images in the LBX image
	 * @throws IOException If there is a problem reading the image header
	 */
	@Override
	public final int getNumImages (final boolean allowSearch)
		throws IOException
	{
		ensureHeaderRead ();
		return frameCount;
	}

	/**
	 * @param imageIndex The number of the image in the LBX image file that we want
	 * @return The type of image (e.g. colour model) returned, which is always 32 bit ARGB colour
	 * @throws IOException If there is a problem reading the image header
	 */
	@Override
	public final Iterator<ImageTypeSpecifier> getImageTypes (final int imageIndex)
		throws IOException
	{
		if ((imageIndex < 0) || (imageIndex >= getNumImages (true)))
			throw new IndexOutOfBoundsException ("com.ndg.graphics.lbx.LbxImageReader.getImageTypes: LBX file contains " +
				getNumImages (true) + " frames but requested frame " + imageIndex);
		
		// Create image type
		ColorSpace colourSpace = ColorSpace.getInstance (ColorSpace.CS_sRGB);
		int [] bOffsABGR = {3, 2, 1, 0};	// This seems to have very little effect
		ImageTypeSpecifier imageType = ImageTypeSpecifier.createInterleaved (colourSpace, bOffsABGR, DataBuffer.TYPE_BYTE, true, false);
		
		// Put it in a list
		ArrayList<ImageTypeSpecifier> list = new ArrayList<ImageTypeSpecifier> ();
		list.add (imageType);
		return list.iterator ();
	}

	/**
	 * @param imageIndex The number of the image in the LBX image file that we want
	 * @return The width of the image
	 * @throws IOException If there is a problem reading the image header
	 */
	@Override
	public final int getWidth (final int imageIndex)
		throws IOException
	{
		if ((imageIndex < 0) || (imageIndex >= getNumImages (true)))
			throw new IndexOutOfBoundsException ("com.ndg.graphics.lbx.LbxImageReader.getWidth: LBX file contains " +
				getNumImages (true) + " frames but requested frame " + imageIndex);
		
		ensureHeaderRead ();
		return width;
	}

	/**
	 * @param imageIndex The number of the image in the LBX image file that we want
	 * @return The height of the image
	 * @throws IOException If there is a problem reading the image header
	 */
	@Override
	public final int getHeight (final int imageIndex)
		throws IOException
	{
		if ((imageIndex < 0) || (imageIndex >= getNumImages (true)))
			throw new IndexOutOfBoundsException ("com.ndg.graphics.lbx.LbxImageReader.getHeight: LBX file contains " +
				getNumImages (true) + " frames but requested frame " + imageIndex);
		
		ensureHeaderRead ();
		return height;
	}
	
	/**
	 * Reads in the LBX image header from the stream, if we haven't done so already
	 * @throws IOException If there is a problem reading the image header
	 */
	protected final void ensureHeaderRead ()
		throws IOException
	{
		if (!headerRead)
		{
			// Check stream
			if (iis == null)
				throw new IOException ("com.ndg.graphics.lbx.LbxImageReader.ensureHeaderRead: Don't have ImageInputStream to read from");
			
			// Read header values
			width = iis.readUnsignedShort ();
			height = iis.readUnsignedShort ();
			iis.readUnsignedShort ();
			frameCount = iis.readUnsignedShort ();

			for (int skip = 0; skip < 3; skip++)
				iis.readUnsignedShort ();
			
			paletteInfoOffset = iis.readUnsignedShort ();
			iis.readUnsignedShort ();
			
			// Ensure we only read the header once
			headerRead = true;
		}
	}

	/**
	 * Handles actually reading the stream and decoding the image
	 * @param imageIndex The number of the image in the LBX image file that we want
	 * @param param Parameters controlling decoding of the image
	 * @return The loaded image
	 * @throws IOException If there is a problem reading from the stream
	 */
	@Override
	public final BufferedImage read (final int imageIndex, final ImageReadParam param)
		throws IOException
	{
		if ((imageIndex < 0) || (imageIndex >= getNumImages (true)))
			throw new IndexOutOfBoundsException ("com.ndg.graphics.lbx.LbxImageReader.read: LBX file contains " +
				getNumImages (true) + " frames but requested frame " + imageIndex);
		
		ensureHeaderRead ();
		
		// Check stream
		if (iis == null)
			throw new IOException ("com.ndg.graphics.lbx.LbxImageReader.read: Don't have ImageInputStream to read from");
		
		// Read all the frame offsets into an array - we need all of them up to the frame we want to decode, since they must be decoded progressively
		// Technically we don't need the later ones, but may as well read them just to skip over the bytes in the stream
		List<Integer> frameOffsets = new ArrayList<Integer> ();
		for (int frameNo = 0; frameNo <= frameCount; frameNo++)
			frameOffsets.add (new Integer (iis.readInt ()));		

		// Keep track of how many bytes we've read from the stream, because the offsets are based from the start of the file
		long bytesRead = LBX_IMAGE_HEADER_SIZE + ((frameCount + 1) * 4); 

		// Default palette
		int [] palette = new int [256];
		for (int colourNumber = 0; colourNumber < 256; colourNumber++)
		{
			int red = MOM_PALETTE [colourNumber] [0];
			int green = MOM_PALETTE [colourNumber] [1];
			int blue = MOM_PALETTE [colourNumber] [2];
			
			palette [colourNumber] =
				0xFF000000 + (red << 16) + (green << 8) + blue;
			
			// Make shadows mostly transparent
			if ((palette [colourNumber] == 0xFFA0A0B4) || (palette [colourNumber] == 0xFF8888A4))
				palette [colourNumber] = 0x80000000;
			
			// Make palette index 0 totally transparent
			else if (colourNumber == 0)
				palette [colourNumber] = 0;
		}
		
		// Set default palette colour indexes
		int firstPaletteColourIndex = 0;
		int paletteColourCount = 255;

		// Load palette data
		if (paletteInfoOffset > 0)
		{
			// Jump to palette header
			long bytesToSkip = paletteInfoOffset - bytesRead;
			if (bytesToSkip < 0)
				throw new IOException ("LBX graphics image file is ordered such that we need to skip backwards to read the palette info");

			for (long n = 0; n < bytesToSkip; n++)
			{
				iis.read ();
				bytesRead++;
			}

			// Read palette header
			int paletteOffset				= iis.readUnsignedShort ();
			firstPaletteColourIndex	= iis.readUnsignedShort ();
			paletteColourCount			= iis.readUnsignedShort ();
			iis.readUnsignedShort ();
			bytesRead = bytesRead + (4 * 2);
			
			// Jump to actual palette data
			bytesToSkip = paletteOffset - bytesRead;
			if (bytesToSkip < 0)
				throw new IOException ("LBX graphics image file is ordered such that we need to skip backwards to read the palette data");
			
			for (long n = 0; n < bytesToSkip; n++)
			{
				iis.read ();
				bytesRead++;
			}

			// Read custom palette
			for (int colourNumber = 0; colourNumber < paletteColourCount; colourNumber++)
			{
				int red		= iis.readUnsignedByte ();
				int green	= iis.readUnsignedByte ();
				int blue		= iis.readUnsignedByte ();
				bytesRead = bytesRead + 3;
				
				// Multiply colour values up by 4 (so e.g. << 18 instead of << 16)
				palette [firstPaletteColourIndex + colourNumber] =
					0xFF000000 + (red << 18) + (green << 10) + (blue << 2);
				
				// Make shadows mostly transparent
				if ((palette [firstPaletteColourIndex + colourNumber] == 0xFFA0A0B4) ||
					(palette [firstPaletteColourIndex + colourNumber] == 0xFF8888A4))
					
					palette [firstPaletteColourIndex + colourNumber] = 0x80000000;
			}
		}

		// Values of at least this indicate run length values
		RLE_val = firstPaletteColourIndex + paletteColourCount;
		
		// Create image
		BufferedImage image = getDestination (param, getImageTypes (imageIndex), width, height);

		// Set image to transparent
		for (int x = 0; x < width; x++)
			for (int y = 0; y < height; y++)
				image.setRGB (x, y, 0);

		// Position the stream to the first frame
		// Thereafter we read the entire data block for each frame, so this is the last bit of positioning we have to do
		long bytesToSkip = frameOffsets.get (0).longValue () - bytesRead;
		if (bytesToSkip < 0)
			throw new IOException ("LBX graphics image file is ordered such that we need to skip backwards to read first frame");

		for (long n = 0; n < bytesToSkip; n++)
			iis.read ();
		
		// Convert each frame up to and including the one we want
		for (int frameNo = 0; frameNo <= imageIndex; frameNo++)
		{
			// Read in the data for this frame
			int frameDataSize = frameOffsets.get (frameNo + 1).intValue () - frameOffsets.get (frameNo).intValue ();
			if (frameDataSize <= 0)
				throw new IOException ("LBX graphics image file is ordered such that frame " + frameNo + "/" + frameCount + " has data size " + frameDataSize);
			
			byte [] byteBuffer = new byte [frameDataSize];
			iis.read (byteBuffer);
			
			// Special debug setting to force only a single frame to be decoded
			if ((!decodeRequestedFrameOnly) || (frameNo == imageIndex))
			{
				// Bytes are range -128..127 but we need 0..255
				int [] imageBuffer = new int [frameDataSize];
				for (int byteNo = 0; byteNo < frameDataSize; byteNo++)
				{
					int value = byteBuffer [byteNo];
					if (value < 0)
						value = value + 256;
					imageBuffer [byteNo] = value;
				}
			
				// Decode this frame
				decodeLbxFrame (image, imageBuffer, palette, frameNo, firstPaletteColourIndex, paletteColourCount);
			}
		}
		
		return image;
	}	
	
	
	
	/**
	 * Creates a buffered image of the correct size and fills in all pixel data from the .LBX graphics format file to be read from the stream.
	 * @param image The image to decode the LBX frame onto
	 * @param imageBuffer The raw LBX stream to decode the frame from
	 * @param palette The complete palette composed from the standard palette modified by any custom colours supplied with this image
	 * @param frameNo Which frame number is being decoded
	 * @param firstPaletteColourIndex First colour in the custom palette supplied with this image
	 * @param paletteColourCount Number of colours in the custom palette supplied with this image
	 * @throws IOException If there is a problem decoding the frame, i.e. the data does not represent a properly encoded LBX frame
	 */
	protected void decodeLbxFrame (final BufferedImage image, final int [] imageBuffer, final int [] palette,
		final int frameNo, final int firstPaletteColourIndex, final int paletteColourCount)
		throws IOException
	{
		// Byte 0 tells us whether to reset the image to transparent half way through an animation
		if ((imageBuffer [0] == 1) && (frameNo > 0))
			for (int x = 0; x < width; x++)
				for (int y = 0; y < height; y++)
					image.setRGB (x, y, 0);
			
		// Decode bitmap
		int bitmapIndex = 1;		// Current index into the image buffer
		int x = 0;
			
		while ((x < width) && (bitmapIndex < imageBuffer.length))
		{
			if (imageBuffer [bitmapIndex] == 0xFF)
			{
				bitmapIndex++;
				RLE_val = firstPaletteColourIndex + paletteColourCount;
			}
			else
			{
				int longData = imageBuffer [bitmapIndex + 2];
				int nextCtl = bitmapIndex + imageBuffer [bitmapIndex + 1] + 2;
					
				if (imageBuffer [bitmapIndex] == 0x00)
					RLE_val = firstPaletteColourIndex + paletteColourCount;
				else if (imageBuffer [bitmapIndex] == 0x80)
					RLE_val = 0xE0;
				else
					throw new IOException ("Unrecognized RLE value in LBX image");
					
				int y = imageBuffer [bitmapIndex + 3];
				bitmapIndex = bitmapIndex + 4;
					
				int n_r = bitmapIndex;
				while (n_r < nextCtl)
				{
					while ((n_r < bitmapIndex + longData) && (x < width))
					{
						if (imageBuffer [n_r] >= RLE_val)
						{
							// This value is an run length, the next value is the value to repeat
							int lastPos = n_r + 1;
							int rleLength = imageBuffer [n_r] - RLE_val + 1;
							int rleCounter = 0;
								
							while ((rleCounter < rleLength) && (y < height))
							{
								if ((x >= 0) && (y >= 0) && (x < width) && (y < height))
									image.setRGB (x, y, palette [imageBuffer [lastPos]]);
								else
									throw new IOException ("LBX graphics file: RLE length overrun on output");
									
								y++;
								rleCounter++;
							}
							n_r++;
							n_r++;
						}
						else
						{
							// Regular single pixel
							if ((x >= 0) && (y >= 0) && (x < width) && (y < height))
								image.setRGB (x, y, palette [imageBuffer [n_r]]);
								
							n_r++;
							y++;
						}
					}
						
					if (n_r < nextCtl)
					{
						y = y + imageBuffer [n_r + 1];
						bitmapIndex = n_r + 2;
						longData = imageBuffer [n_r];
						n_r++;
						n_r++;
					}
				}
					
				bitmapIndex = nextCtl;	// jump to next line
			}
				
			x++;
		}
	}
	
	/**
	 * @param imageIndex The number of the image in the .ndgbmp file that we want (must always be 0)
	 * @return null, because no metadata is supported for .ndgbmp images
	 * @throws IOException If there is a problem reading the image header
	 */
	@Override
	public IIOMetadata getImageMetadata (int imageIndex)
		throws IOException
	{
		if ((imageIndex < 0) || (imageIndex >= getNumImages (true)))
			throw new IndexOutOfBoundsException ("com.ndg.graphics.lbx.LbxImageReader.getImageMetadata: LBX file contains " +
				getNumImages (true) + " frames but requested frame " + imageIndex);

		return null;
	}

	/**
	 * @return null, because no metadata is supported for .ndgbmp images
	 */
	@Override
	public IIOMetadata getStreamMetadata ()
	{
		return null;
	}
	
	/**
	 * Debug setting
	 * @param b Whether to force only a single frame to be decoded, rather than all frames prior to it being decoded progressively
	 */
	public static void setDecodeRequestedFrameOnly (boolean b)
	{
		decodeRequestedFrameOnly = b;
	}

}
