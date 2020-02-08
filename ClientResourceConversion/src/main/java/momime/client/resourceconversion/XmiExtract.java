package momime.client.resourceconversion;

import java.io.FileInputStream;
import java.io.FileOutputStream;

import javax.imageio.stream.ImageInputStream;

import com.ndg.archive.LbxArchiveReader;

/**
 * Extracts the .XMI music files from MoM without converting them to .MIDs
 * This is an attempt to use another program besides XMI2MID to convert the two which the old Delphi LBXExtract can't handle. 
 */
public final class XmiExtract
{
	/**
	 * @param lbxName Name of LBX file containing the XMI music
	 * @param subFileNumber Number of the subfile within the LBX
	 * @throws Exception If there is a problem
	 */
	public final void extract (final String lbxName, final int subFileNumber) throws Exception
	{
		System.out.println ("Extracting " + lbxName + " sub file " + subFileNumber);
		try (final FileInputStream in1 = new FileInputStream ("C:\\32 bit Program Files\\DosBox - Master of Magic\\Magic\\" + lbxName + ".LBX"))
		{
			@SuppressWarnings ("resource")
			final ImageInputStream in2 = LbxArchiveReader.getSubFileImageInputStream (in1, subFileNumber);
			try (final FileOutputStream out = new FileOutputStream ("E:\\Install Games\\Master of Magic\\Music\\XMIs\\" + lbxName + "_" + subFileNumber + ".xmi"))
			{
				for (int n = 0; n < in2.length (); n++)
					out.write (in2.read ());
			}
		}
	}

	/**
	 * @param args Command line arguments, ignored
	 */
	public final static void main (final String args [])
	{
		try
		{
			final XmiExtract extract = new XmiExtract ();
			extract.extract ("MUSIC", 62);
			extract.extract ("MUSIC", 78);
			
			System.out.println ("All done!");
		}
		catch (final Exception e)
		{
			e.printStackTrace ();
		}
	}
}