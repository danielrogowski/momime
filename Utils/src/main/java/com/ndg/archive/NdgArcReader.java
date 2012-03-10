package com.ndg.archive;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;

import com.ndg.graphics.ndgbmp.NdgBmpReaderSpi;
import com.ndg.utils.FileNameUtils;
import com.ndg.utils.StreamUtils;

/**
 * Class for picking out one file from a .ndgarc archive
 */
public final class NdgArcReader
{
	/**
	 * Extension of .ndgarc archives
	 */
	public static final String NDGARC_EXTENSION = ".ndgarc";

	/**
	 * Creates a stream object positioned to a particular subfile in a .ndgarc file
	 * This is a basic InputStream, so contains no length information, so there's nothing to stop the calling
	 * routine reading off the end of the returned stream and into the next file in the archive
	 * @param stream Input stream of the .ndgarc file
	 * @param fileIdentifier The file identiier encoded into the .ndgarc file
	 * @param subFileNumber Entry number within the .ndgarc file, with the first sub file being 0
	 * @return Stream positioned to the start of the requested subfile
	 * @throws IOException if there is a problem reading the archive, or the file identifier doesn't match
	 */
	public final static InputStream getSubFileInputStream (final InputStream stream, final String fileIdentifier, final int subFileNumber)
		throws IOException
	{
		if (subFileNumber < 0)
			throw new ArchiveException ("Requested a negative sub file number from a .ndgarc file");

		// File identifier
		final String actualIdentifier = StreamUtils.readLengthAndStringFromStream (stream, ".ndgarc file identifier");
		if ((fileIdentifier != null) && (!fileIdentifier.equals (actualIdentifier)))
			throw new ArchiveException (".ndgarc file identifier read was '" + actualIdentifier + "' instead of expected value '" + fileIdentifier + "'");

		// Number of files
		final long subFileCount = StreamUtils.readUnsigned4ByteLongFromStream (stream, ByteOrder.LITTLE_ENDIAN, ".ndgarc sub file count");
		if (subFileNumber >= subFileCount)
			throw new ArchiveException ("Requested sub file " + subFileNumber + " of file '" + stream + "' which only contains " + subFileCount + " sub files");

		// Keep track of how many bytes we've read from the stream, because the offsets are based from the start of the file
		long bytesRead = actualIdentifier.length () + 8;

		// Throw away all the filenames
		for (long subFileNo = 0; subFileNo < subFileCount; subFileNo++)
		{
			final String subFileName = StreamUtils.readLengthAndStringFromStream (stream, ".ndgarc sub file name");
			bytesRead = bytesRead + subFileName.length () + 4;
		}

		// Read all the offsets until we get the one we want
		long offset = 0;
		for (int subFileNo = 0; subFileNo <= subFileNumber; subFileNo++)
		{
			offset = StreamUtils.readUnsigned4ByteLongFromStream (stream, ByteOrder.LITTLE_ENDIAN, ".ndgarc sub file offset");
			bytesRead = bytesRead + 4;
		}

		// Skip to the start of the requested subfile
		final long bytesToSkip = offset - bytesRead;
		for (long n = 0; n < bytesToSkip; n++)
			stream.read ();

		return stream;
	}

	/**
	 * @param archiveName Name of the archive file
	 * @return List of all the files contained within this archive
	 * @throws IOException if there is a problem reading the archive
	 */
	public final static List<ArchivedFile> readContents (final String archiveName)
		throws IOException
	{
		final File archiveFile = new File (archiveName);

		final String archiveFolder = FileNameUtils.changeFileExt (archiveName, "") + "\\";
		new File (archiveFolder).mkdir ();

		final List<ArchivedFile> contents = new ArrayList<ArchivedFile> ();

		final InputStream stream = new BufferedInputStream (new FileInputStream (archiveName));
		try
		{
			// File identifier
			StreamUtils.readLengthAndStringFromStream (stream, ".ndgarc file identifier");

			// Number of files
			final int subFileCount = StreamUtils.readSigned4ByteIntFromStream (stream, ByteOrder.LITTLE_ENDIAN, ".ndgarc sub file count");

			// Make a list of all the filenames
			final List<String> fileNames = new ArrayList<String> ();
			for (int subFileNo = 0; subFileNo < subFileCount; subFileNo++)
			{
				final String subFileName = StreamUtils.readLengthAndStringFromStream (stream, ".ndgarc sub file name");
				fileNames.add (subFileName);
			}

			// Read the first offset
			if (subFileCount > 0)
			{
				long previousOffset = StreamUtils.readUnsigned4ByteLongFromStream (stream, ByteOrder.LITTLE_ENDIAN, ".ndgarc first sub file offset");

				// Then go through each of the remaining offsets
				// Note each time we're reading the offset of a file, we're outputting the PREVIOUS file, since we need the next offset in order to know its size
				for (int subFileNo = 1; subFileNo < subFileCount; subFileNo++)
				{
					final long nextOffset = StreamUtils.readUnsigned4ByteLongFromStream (stream, ByteOrder.LITTLE_ENDIAN, ".ndgarc sub file offset");
					contents.add (new ArchivedFile (fileNames.get (subFileNo - 1), archiveFolder + fileNames.get (subFileNo - 1), previousOffset, nextOffset - previousOffset, subFileNo - 1));

					previousOffset = nextOffset;
				}

				// Deal with the final file
				contents.add (new ArchivedFile (fileNames.get (subFileCount - 1), archiveFolder + fileNames.get (subFileCount - 1), previousOffset, archiveFile.length () - previousOffset, subFileCount - 1));
			}
		}
		finally
		{
			stream.close ();
		}

		return contents;
	}

	/**
	 * Extracts a file back out of an archive and saves it out to disk
	 * @param stream Stream of the archive file
	 * @param archivedFile File to extract (previously obtained by readContents)
	 * @throws IOException If there is a problem reading the archive or writing the file
	 */
	public final static void extractFromArchiveToFile (final InputStream stream, final ArchivedFile archivedFile)
		throws IOException
	{
		final InputStream in = getSubFileInputStream (stream, null, archivedFile.getSubFileNumber ());
		try
		{
			final BufferedOutputStream out = new BufferedOutputStream (new FileOutputStream (archivedFile.getFullPath ()));
			try
			{
				// Convert .ndgbmps to .pngs as they are extracted from the archive
				// The reason I decided to switch to using .pngs is because they can save out the Alpha channel whereas .bmps cannot
				// You can define another supported alpha-less image format for the .ndgbmp decoder, and hence allow .ndgbmp to .bmp conversion,
				// but the resulting .bmp file has had PSP7's pre-white-multiplying undone by the old Delphi texture encoder and so it does not match the
				// original .bmp prior to .ndgbmp conversion as you would reasonably expect
				// You could reverse the conversion, but this would not be precise, so decided to avoid the problem altogether by using .png instead
				if ((FileNameUtils.extractFileExt (archivedFile.getFileName ()).equalsIgnoreCase (".ndgbmp")) &&
					(FileNameUtils.extractFileExt (archivedFile.getFullPath ()).equalsIgnoreCase (".png")))
				{
					// Decode and re-encode the image
					final BufferedImage image = ImageIO.read (in);
					if (image == null)
    					throw new IOException ("Failed to load image from sub file number " + archivedFile.getSubFileNumber () + " from file '" + stream + "' (no image readers claim to be able to decode the stream)");

   					if (!ImageIO.write (image, "png", out))
   						throw new IOException ("Failed to write .png image created from .ndgbmp image in sub file number " + archivedFile.getSubFileNumber () + " of \"" + stream + "\"");
				}
				else
				{
					// Standard file, just dump it out directly
					StreamUtils.copyBetweenStreams (in, out, false, false, archivedFile.getFileSize ());
				}
			}
			finally
			{
				out.close ();
			}
		}
		finally
		{
			in.close ();
		}
	}

	/**
	 * Extracts all the files from a .ndgarc archive
	 * @param args Names of archives to extract from
	 */
	public final static void main (final String [] args)
	{
		if (args.length != 1)
			System.out.println ("Usage: java com.ndg.archive.NdgArcReader <name of .ndgarc file>");
		else
		{
			// Force registration of .ndgbmp format (externally, this would happen via the META-INF/services method in the JAR file)
			IIORegistry.getDefaultInstance ().registerServiceProvider (new NdgBmpReaderSpi ());

			try
			{
				final String archiveName = args [0];

				// Read contents
				final List<ArchivedFile> contents = readContents (archiveName);

				// Change .ndgbmp files back to .png
				for (final ArchivedFile thisFile : contents)
					if (FileNameUtils.extractFileExt (thisFile.getFileName ()).equalsIgnoreCase (".ndgbmp"))
						thisFile.setFullPath (FileNameUtils.changeFileExt (thisFile.getFullPath (), ".png"));

				// Save each file
				for (final ArchivedFile thisFile : contents)
				{
					System.out.println (thisFile.getFileName () + "...");
					extractFromArchiveToFile (new FileInputStream (archiveName), thisFile);
				}

				System.out.println ("Done!");
			}
			catch (final IOException e)
			{
				e.printStackTrace ();
			}
		}
	}
}
