package com.ndg.archive;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;

import javax.imageio.stream.ImageInputStream;

import com.ndg.utils.FixedLengthMemoryCacheImageInputStream;
import com.ndg.utils.StreamUtils;

/**
 * Class for picking out one file from a .lbx archive
 */
public class LbxArchiveReader
{
	/**
	 * Extension of .lbx archives
	 */
	public static final String LBX_EXTENSION = ".lbx";

	/**
	 * Expected signature of LBX files in bytes 3 thru 6.
	 */
	private static final int LBX_SIGNATURE = 0xFEAD;

	/**
	 * Reads the LBX archive header from an open stream, and checks the requested sub file number is valid
	 * @param stream Stream of the .lbx file
	 * @param subFileNumber Entry number within the .lbx file, with the first sub file being 0 (note each "entry" can contain multiple frames)
	 * @return Stream positioned to the end of the LBX header, and the start of the subfile offsets table
	 * @throws IOException If there is a problem reading the file, it doesn't have the expected LBX archive header, or the requested sub file number is invalid
	 */
	protected static InputStream readLbxArchiveHeader (final InputStream stream, final int subFileNumber)
		throws IOException
	{
		if (subFileNumber < 0)
			throw new ArchiveException ("Requested a negative sub file number from a .lbx file");

		// Read the number of sub files
		final int subFileCount = StreamUtils.readUnsigned2ByteIntFromStream (stream, ByteOrder.LITTLE_ENDIAN, "LBX sub file count");
		if (subFileNumber >= subFileCount)
			throw new ArchiveException ("File " + stream + " has " + subFileCount +
				" sub files - attempted to use sub file number " + subFileNumber + " which is not valid");

		// Read the LBX signature
		if (StreamUtils.readSigned4ByteIntFromStream (stream, ByteOrder.LITTLE_ENDIAN, "LBX signature") != LBX_SIGNATURE)
			throw new ArchiveException ("File " + stream + " does not have expected LBX signature in bytes 3 thru 6");

		// Skip bytes 7 and 8
		StreamUtils.readUnsigned2ByteIntFromStream (stream, ByteOrder.LITTLE_ENDIAN, "LBX skipped bytes 7 and 8");

		return stream;
	}

	/**
	 * Creates a stream object positioned to a particular subfile in a .lbx file
	 * This is a basic InputStream, so contains no length information, so there's nothing to stop the calling
	 * routine reading off the end of the returned stream and into the next file in the archive
	 * @param in Stream of the .lbx file
	 * @param subFileNumber Entry number within the .lbx file, with the first sub file being 0 (note each "entry" can contain multiple frames)
	 * @return Stream positioned to the start of the requested subfile
	 * @throws IOException If there is a problem reading the file, or it doesn't have the expected LBX archive header
	 */
	public static InputStream getSubFileInputStream (final InputStream in, final int subFileNumber)
		throws IOException
	{
		final InputStream stream = readLbxArchiveHeader (in, subFileNumber);

		// Keep track of how many bytes we've read from the stream, because the offsets are based from the start of the file
		long bytesRead = 8;

		// Read all the offsets until we get the one we want
		long offset = 0;
		for (int subFileNo = 0; subFileNo <= subFileNumber; subFileNo++)
		{
			offset = StreamUtils.readUnsigned4ByteLongFromStream (stream, ByteOrder.LITTLE_ENDIAN, ".lbx sub file offset");
			bytesRead = bytesRead + 4;
		}

		// Skip to the start of the requested subfile
		final long bytesToSkip = offset - bytesRead;
		for (long n = 0; n < bytesToSkip; n++)
			stream.read ();

		return stream;
	}

	/**
	 * Creates an image input stream object positioned to a particular subfile in a .lbx file
	 * This is an ImageInputStream with the length specifically constrained to the length of the subfile
	 * The LBX image decoder needs the specific length in order to correctly identity LBX images, since they have no identifyable header
	 * @param in Stream of the .lbx file
	 * @param subFileNumber Entry number within the .lbx file, with the first sub file being 0 (note each "entry" can contain multiple frames)
	 * @return Stream positioned to the start of the requested subfile
	 * @throws IOException if there is a problem reading the file, or the file identifier doesn't match
	 */
	public static ImageInputStream getSubFileImageInputStream (final InputStream in, final int subFileNumber)
		throws IOException
	{
		final InputStream stream = readLbxArchiveHeader (in, subFileNumber);

		// Keep track of how many bytes we've read from the stream, because the offsets are based from the start of the file
		long bytesRead = 8;

		// Read all the offsets until we get the one we want
		long offset = 0;
		for (int subFileNo = 0; subFileNo <= subFileNumber; subFileNo++)
		{
			offset = StreamUtils.readUnsigned4ByteLongFromStream (stream, ByteOrder.LITTLE_ENDIAN, ".lbx sub file offset");
			bytesRead = bytesRead + 4;
		}

		// Read the next offset, or length of the file
		final long ending = StreamUtils.readUnsigned4ByteLongFromStream (stream, ByteOrder.LITTLE_ENDIAN, ".lbx sub file ending");
		bytesRead = bytesRead + 4;

		// Skip to the start of the requested subfile
		final long bytesToSkip = offset - bytesRead;
		for (long n = 0; n < bytesToSkip; n++)
			stream.read ();

		return new FixedLengthMemoryCacheImageInputStream (stream, ending - offset);
	}
}
