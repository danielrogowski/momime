package com.ndg.graphics;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import com.ndg.archive.LbxArchiveReader;
import com.ndg.archive.NdgArcReader;
import com.ndg.utils.FileNameUtils;

/**
 * Loads, caches and finds images, to ensure they only have to be loaded in once
 * Also deals with pulling images out of archive type files such as .ndgarc or .lbx, which the basic ImageIO does not do
 * Typically subclassed in order to override the getFileAsInputStream method
 */
public abstract class ImageCache
{
	/**
	 * The cache of all the images
	 */
	private final ArrayList<CachedImage> cache;

	/**
	 * Creates a new image cache
	 * Typically don't call this directly, use getDefaultInstance () instead
	 */
	public ImageCache ()
	{
		super ();
		cache = new ArrayList<CachedImage> ();
	}

    /**
     * Subclasses must override this to specify how to locate files; can either figure out a full path and
     * return a FileInputStream, or use getResourceAsStream or similar
     *
     * @param fileName The filename to locate
     * @return Input stream of the requested filename
     * @throws IOException If the file can't be read
     */
    public abstract InputStream getFileAsInputStream (final String fileName) throws IOException;

    /**
     * Retrieves an image from the cache, or loads it and adds it to the cache
     * @param fileName The filename of the image to load
     * @param subFileNumber The sub file number within the file this image was loaded from
     * @param frameNumber The frame number within the sub file this image was loaded from
     * @return The retrieved or loaded image
     * @throws IOException If there is a problem reading the specified file
     */
    public BufferedImage findOrLoadImage (final String fileName, final int subFileNumber, final int frameNumber)
    	throws IOException
    {
    	BufferedImage image = null;

    	synchronized (cache)
    	{
    		// First try to see if its already in the cache
    		int imageNo = 0;
    		while ((image == null) && (imageNo < cache.size ()))
			{
    			final CachedImage thisImage = cache.get (imageNo);
    			if ((thisImage.getFileName ().equals (fileName)) && (thisImage.getSubFileNumber () == subFileNumber) && (thisImage.getFrameNumber () == frameNumber))
    				image = thisImage.getImage ();
    			else
    				imageNo++;
			}

    		// If we didn't find it, then load it
    		if (image == null)
    		{
    			// How we load it depends on the type of file
    			// For now, assume anything other than known archive types must be a standalone image file
    			ImageInputStream stream;
    			final String extension = FileNameUtils.extractFileExt (fileName).toLowerCase ();
    			if (extension.equals (NdgArcReader.NDGARC_EXTENSION))
    				stream = ImageIO.createImageInputStream (NdgArcReader.getSubFileInputStream (getFileAsInputStream (fileName), null, subFileNumber));	// This will handle any image type contained within the .ndgarc file

    			else if (extension.equals (LbxArchiveReader.LBX_EXTENSION))
    				stream = LbxArchiveReader.getSubFileImageInputStream (getFileAsInputStream (fileName), subFileNumber);	// This will handle any image type contained within the .lbx file

    			else if (subFileNumber != 0)
    				throw new IOException ("com.ndg.graphics.ImageCache: Only archives can use a sub file number other than 0, but requested sub file" + subFileNumber);

    			else
    				stream = ImageIO.createImageInputStream (getFileAsInputStream (fileName));

    			if (stream == null)
    				throw new IOException ("Failed to load image from sub file number " + subFileNumber + " from file '" + fileName + "' (got a null stream)");

    			// Now we have the stream, can load the image
    			// Can't use ImageIO.read, since this is dumb and assumes a frame number of 0 - there's no version which allows the frame number to be passed in
    			// Instead we basically have to replicate what ImageIO.read does
    			// Brackets are simply to keep all the iter, reader, param within local scope
    			{
    				final Iterator<ImageReader> iter = ImageIO.getImageReaders (stream);
    				if (!iter.hasNext ())
    					throw new IOException ("Failed to load image from sub file number " + subFileNumber + " from file '" + fileName + "' (no image readers claim to be able to decode the stream)");

    				final ImageReader reader = iter.next ();
    				reader.setInput (stream, true, true);
    				try
    				{
    					image = reader.read (frameNumber);
    				}
    				finally
    				{
    					reader.dispose ();
    					stream.close ();
    				}
    			}

    			if (image == null)
    				throw new IOException ("Failed to load image from sub file number " + subFileNumber + " from file '" + fileName + "' (got a null image)");

    			// Add it to the cache
    			final CachedImage newImage = new CachedImage (image, fileName, subFileNumber, frameNumber);
    			cache.add (newImage);
    		}
    	}

    	return image;
    }
}
