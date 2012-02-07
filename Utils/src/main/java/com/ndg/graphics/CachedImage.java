package com.ndg.graphics;

import java.awt.image.BufferedImage;

/**
 * Class which stores an image, and remembers what filename and image number the image came from
 */
public class CachedImage
{
	/**
	 * The actual cached image
	 */
	private BufferedImage image;
	
	/**
	 * The filename the cached image was loaded from
	 */
	private String fileName;
	
	/**
	 * The sub file number within the file this image was loaded from
	 */
	private int subFileNumber;
	
	/**
	 * The frame number within the sub file this image was loaded from (not for .ndgbmps, which are only ever single framed)
	 */
	private int frameNumber;

	/**
	 * Creates a cached image object
	 * @param anImage The actual image, note the caller must have preloaded this (to save the CachedImage class from having to deal with complexities of differing file formats)
	 * @param aFileName The filename the image was loaded from
	 * @param aSubFileNumber The sub file number within the file this image was loaded from
	 * @param aFrameNumber The frame number within the sub file this image was loaded from (not for .ndgbmps, which are only ever single framed)
	 */
	public CachedImage (BufferedImage anImage, String aFileName, int aSubFileNumber, int aFrameNumber)
	{
		super ();
		
		image = anImage;
		fileName = aFileName;
		subFileNumber = aSubFileNumber;
		frameNumber = aFrameNumber;
	}
	
	/**
	 * @return The actual cached image
	 */
	public BufferedImage getImage ()
	{
		return image;
	}
	
	/**
	 * @return The filename the cached image was loaded from
	 */
	public String getFileName ()
	{
		return fileName;
	}
	
	/**
	 * Many formats only support a single image in a file (e.g. .bmp, .jpg, .ndgbmp)
	 * This is typically used for .ndgbmp files within a .ndgarc file, where the image number
	 * indicates the number of the subfile within the .ndgarc file
	 * 
	 * For formats like .lbx where even each subfile can contain multiple frames, this still
	 * indicates the sub file number, not like the "file frame no" in Delphi which numbers
	 * all the frames from all the sub files in a single sequence
	 * 
	 * @return The sub file number within the file this image was loaded from
	 */
	public int getSubFileNumber ()
	{
		return subFileNumber;
	}
	
	/**
	 * @return The frame number within the sub file this image was loaded from (not for .ndgbmps, which are only ever single framed)
	 */
	public int getFrameNumber ()
	{
		return frameNumber;
	}
}
