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
		String s = Integer.valueOf (subFileNumber).toString ();
		while (s.length () < 3)
			s = "0" + s;
		
		System.out.println ("Extracting " + lbxName + " sub file " + subFileNumber);
		try (final FileInputStream in1 = new FileInputStream ("C:\\32 bit Program Files\\DosBox - Master of Magic\\Magic\\" + lbxName + ".LBX"))
		{
			final ImageInputStream in2 = LbxArchiveReader.getSubFileImageInputStreamSkippingHeader (in1, subFileNumber, 16);
			try (final FileOutputStream out = new FileOutputStream ("E:\\Install Games\\Master of Magic\\Music\\XMIs\\" + lbxName + "_" + s + ".xmi"))
			{
				for (int n = 0; n < in2.length (); n++)
					out.write (in2.read ());
			}
		}
	}
	
	/**
	 * @param lbxName Name of LBX file containing the XMI music
	 */
	public final void extractAll (final String lbxName)
	{
		try
		{
			// Keep going until we run out of sub files, which throws an exception
			int subFileNumber = 0;
			while (true)
			{
				extract (lbxName, subFileNumber);
				subFileNumber++;
			}
		}
		catch (final Exception e)
		{
			e.printStackTrace ();
		}
	}

	/**
	 * @param args Command line arguments, ignored
	 */
	public final static void main (final String args [])
	{
		final XmiExtract extract = new XmiExtract ();
		extract.extractAll ("INTROSND");
		extract.extractAll ("MUSIC");
	}
}