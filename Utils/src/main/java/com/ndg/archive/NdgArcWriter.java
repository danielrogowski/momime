package com.ndg.archive;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;

import com.ndg.graphics.ndgbmp.NdgBmpWriterSpi;
import com.ndg.utils.FileNameUtils;
import com.ndg.utils.StreamUtils;

/**
 * Class for writing .ndgarc archives
 */
public final class NdgArcWriter
{
	/** Extension of .ndgarc archives */
	public static final String NDGARC_EXTENSION = ".ndgarc";

	/**
	 * Creates a .ndgarc archive
	 * @param fileIdentifier Identifier to encode into the file
	 * @param contents List of files to put into the archive; the fileOffset and fileSize members of each entry do not have to be filled in and are ignored if supplied, however the subFileNumber entries must be sequenced correctly
	 * @param archiveName Filename to save the archive out as
	 * @throws IOException If there is a problem reading the source files or creating the archive
	 */
	public final static void createArchive (final String fileIdentifier, final List<ArchivedFile> contents, final String archiveName)
		throws IOException
	{
		if (fileIdentifier.length () > 255)
			throw new IllegalArgumentException (".ndgarc file identifier cannot be longer than 255, but got \"" + fileIdentifier + "\"");

		// Load in all the source files
		final List<ByteArrayOutputStream> sourceFiles = new ArrayList<ByteArrayOutputStream> ();
		for (final ArchivedFile thisFile : contents)
		{
			System.out.println (thisFile.getFileName () + "...");

			final BufferedInputStream in = new BufferedInputStream (new FileInputStream (thisFile.getFullPath ()));
			try
			{
				final ByteArrayOutputStream out = new ByteArrayOutputStream ();

				// Convert .bmps and .pngs to .ndgbmps as we read them in
				final String sourceExtension = FileNameUtils.extractFileExt (thisFile.getFullPath ());
				if (((sourceExtension.equalsIgnoreCase (".bmp")) || (sourceExtension.equalsIgnoreCase (".png"))) &&
					(FileNameUtils.extractFileExt (thisFile.getFileName ()).equalsIgnoreCase (".ndgbmp")))
				{
					// Decode and re-encode the image
					final BufferedImage image = ImageIO.read (in);
					if (image == null)
    					throw new IOException ("Failed to load image from source file \"" + thisFile.getFullPath () + "\" (no image readers claim to be able to decode the stream)");

   					if (!ImageIO.write (image, "ndg bmp", out))
   						throw new IOException ("Failed to write .ndgbmp image created from source file \"" + thisFile.getFullPath () + "\"");
				}
				else
				{
					// Standard file, just read it in directly
					StreamUtils.copyBetweenStreams (in, out, false, false, 0);
				}

				sourceFiles.add (out);
			}
			finally
			{
				in.close ();
			}
		}

		// Create the archive
		System.out.println ("Creating archive...");
		final BufferedOutputStream out = new BufferedOutputStream (new FileOutputStream (archiveName));

		// File identifier - this can be empty
		// As we write out the header, keep track of how many bytes we've written
		long offset = 0;
		if ((fileIdentifier != null) && (fileIdentifier.equals ("")))
			offset = offset + StreamUtils.writeLengthAndStringToStream (out, null);
		else
			offset = offset + StreamUtils.writeLengthAndStringToStream (out, fileIdentifier);

		// Number of files
		offset = offset + StreamUtils.writeSigned4ByteIntToStream (out, ByteOrder.LITTLE_ENDIAN, contents.size ());

		// File names
		for (final ArchivedFile thisFile : contents)
			offset = offset + StreamUtils.writeLengthAndStringToStream (out, thisFile.getFileName ());

		// All that's left to output for the header is the file offsets, and we can predict how much space they'll take up
		offset = offset + (4l * contents.size ());

		// Offsets
		for (final ArchivedFile thisFile : contents)
		{
			StreamUtils.writeUnsigned4ByteLongToStream (out, ByteOrder.LITTLE_ENDIAN, offset);
			offset = offset + sourceFiles.get (thisFile.getSubFileNumber ()).size ();
		}

		// Write out the actual files
		for (final ByteArrayOutputStream thisFile : sourceFiles)
			StreamUtils.copyBetweenStreams (new ByteArrayInputStream (thisFile.toByteArray ()), out, false, false, 0);

		out.close ();
	}

	/**
	 * Extracts all the files from a .ndgarc archive
	 * @param args Names of archives to extract from
	 */
	public final static void main (final String [] args)
	{
		if (args.length != 1)
			System.out.println ("Usage: java com.ndg.archive.NdgArcWriter <name of text file listing files to put into archive>");
		else
		{
			// Force registration of .ndgbmp format (externally, this would happen via the META-INF/services method in the JAR file)
			IIORegistry.getDefaultInstance ().registerServiceProvider (new NdgBmpWriterSpi ());

			try
			{
				final String contentsName = args [0];
				final String archiveFolder = FileNameUtils.changeFileExt (contentsName, "") + "\\";

				// Read contents from text file
				final BufferedReader in = new BufferedReader (new FileReader (contentsName));
				final List<ArchivedFile> contents = new ArrayList<ArchivedFile> ();

				int subFileNumber = 0;
				final String fileIdentifier = in.readLine ();
				if (fileIdentifier != null)
				{
					String fileName = "";
					while (fileName != null)
					{
						fileName = in.readLine ();
						if (fileName != null)
						{
							fileName = fileName.trim ();

							if (!fileName.equals (""))
							{
								// Add file
								contents.add (new ArchivedFile (fileName, archiveFolder + fileName, 0, 0, subFileNumber));
								subFileNumber++;
							}
						}
					}
				}
				in.close ();

				// Change .bmp and .png files to .ndgbmp
				for (final ArchivedFile thisFile : contents)
				{
					final String extension = FileNameUtils.extractFileExt (thisFile.getFileName ());

					if ((extension.equalsIgnoreCase (".bmp")) || (extension.equalsIgnoreCase (".png")))
						thisFile.setFileName (FileNameUtils.changeFileExt (thisFile.getFileName (), ".ndgbmp"));
				}

				// Create the archive
				createArchive (fileIdentifier, contents, FileNameUtils.changeFileExt (contentsName, NDGARC_EXTENSION));
				System.out.println ("Done!");
			}
			catch (final IOException e)
			{
				e.printStackTrace ();
			}
		}
	}

}
