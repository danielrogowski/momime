package momime.client.resourceconversion;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;

import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutComponent;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainer;

/**
 * Converts TNDGDirect2DForms from Delphi DFM files (such as the MoM IME 0.9.4 client) into XML files suitable for use by the XML layout manager
 */
public final class DelphiDfmToXmlLayout
{
	/** Name of the form object that we want to convert */
	private String formName;
	
	/** Depth inside the desired form; null = not inside desired form */
	private Integer depth;
	
	/** Layout we're generating into */
	private XmlLayoutContainer layout;
	
	/** Component currently being parsed */
	private XmlLayoutComponent comp;
	
	/** Whether we ever did find the form we wanted */
	private boolean found;
	
	/**
	 * Processing at the start of an object
	 * 
	 * @param name Object name
	 * @param type Object type
	 * @throws IOException If there is a problem
	 */
	public final void startObject (final String name, final String type) throws IOException
	{
		if (depth != null)
			depth++;
		else if ((type.equals ("TNDGDirect2DForm")) && (name.equals (formName)))
		{
			found = true;
			depth = 0;
		}

		System.out.println (((depth == null) ? "" : "d" + depth + "> ") + "object " + name + ": " + type);
		
		if (depth != null)
		{
			if (depth == 1)
			{
				comp = new XmlLayoutComponent ();
				comp.setName (name);
				layout.getComponent ().add (comp);
			}
			else if (depth != 0)
				throw new IOException ("Don't know how to handle a component at depth > 1");
		}
	}

	/**
	 * Processing at the end of an object
	 */
	public final void endObject ()
	{
		System.out.println (((depth == null) ? "" : "d" + depth + "> ") + "end");
		
		if (depth != null)
		{
			depth--;
			if (depth < 0)
				depth = null;
		}
	}

	/**
	 * Processing one property value
	 * 
	 * @param name Property name
	 * @param value Property value
	 * @throws IOException If there is a problem
	 */
	public final void simpleProperty (final String name, final String value) throws IOException
	{
		System.out.println ("\t" + ((depth == null) ? "" : "d" + depth + "> ") + name + " = " + value);
		
		if (depth != null)
		{
			if (depth == 0)
			{
				if (name.equals ("FormWidth"))
					layout.setFormWidth (Integer.parseInt (value));
				else if (name.equals ("FormHeight"))
					layout.setFormHeight (Integer.parseInt (value));
			}
			else if (depth == 1)
			{
				if (name.equals ("DefinedX"))
					comp.setLeft (Integer.parseInt (value));
				else if (name.equals ("DefinedY"))
					comp.setTop (Integer.parseInt (value));
				else if (name.equals ("HorizontalAlignment"))
				{
					if (value.equals ("taLeftJustify"))
						comp.setHorizontalAlignment (0f);
					else if (value.equals ("taCenter"))
						comp.setHorizontalAlignment (0.5f);
					else if (value.equals ("taRightJustify"))
						comp.setHorizontalAlignment (1f);
					else
						throw new IOException ("Unknown value for HorizontalAlignment");
				}
				else if (name.equals ("VerticalAlignment"))
				{
					if (value.equals ("vaTopJustify"))
						comp.setVerticalAlignment (0f);
					else if (value.equals ("vaCenter"))
						comp.setVerticalAlignment (0.5f);
					else if (value.equals ("vaBottomJustify"))
						comp.setVerticalAlignment (1f);
					else
						throw new IOException ("Unknown value for VerticalAlignment");
				}
			}
		}
	}
	
	/**
	 * Processing a list property
	 * 
	 * @param name Property name
	 * @param values List of property values
	 */
	public final void listProperty (final String name, final List<String> values)
	{
		final StringBuilder valuesList = new StringBuilder ();
		for (final String value : values)
		{
			if (valuesList.length () > 0)
				valuesList.append (",");
			
			valuesList.append (value);
		}

		System.out.println (((depth == null) ? "" : "d" + depth + "> ") + "\t" + name + " = (" + valuesList + ")");
	}
	
	/**
	 * Converts one form
	 * 
	 * @param source Path to DFM file
	 * @param dest Path to XML file
	 * @throws Exception If there is a problem
	 */
	public final void convert (final File source, final File dest) throws Exception
	{
		layout = new XmlLayoutContainer ();

		// Parse input file
		try (final FileReader fin = new FileReader (source))
		{
			try (final BufferedReader in = new BufferedReader (fin))
			{
				String line = in.readLine ();
				while (line != null)
				{
					// Is this line an object definition, or a property value?
					line = line.trim ();
					final int colonPos = line.indexOf (": ");
					final int equalsPos = line.indexOf (" = ");
					
					// End of an object
					if (line.equals ("end"))
						endObject ();
					
					// Single line property value
					else if (equalsPos >= 0)
					{
						final String propertyName = line.substring (0, equalsPos);
						final String propertyValue = line.substring (equalsPos + 3);
						
						if (!propertyValue.equals ("("))
							simpleProperty (propertyName, propertyValue);
						else
						{
							// List property, keep reading until the )
							final List<String> propertyValues = new ArrayList<String> (); 
							
							boolean done = false;
							while (!done)
							{
								line = in.readLine ().trim ();
								
								done = line.endsWith (")");
								if (done)
									line = line.substring (0, line.length () - 1);
								
								if ((line.startsWith ("'")) && (line.endsWith ("'")))
									line = line.substring (1, line.length () - 1);
								
								propertyValues.add (line);
							}
							
							listProperty (propertyName, propertyValues);
						}
					}
					
					// Start of an object
					else if (colonPos >= 0)
					{
						if (!line.startsWith ("object "))
							throw new IOException ("Line contains a : but doesn't start with \"object\"");
						
						startObject (line.substring (7, colonPos), line.substring (colonPos + 2));
					}
					
					// Multi line property value
					else if (line.endsWith (" ="))
					{
						final String propertyName = line.substring (0, line.length () - 2);
						final StringBuilder propertyValue = new StringBuilder ();
						
						// Keep going until we get a line that doesn't end with a +
						boolean done = false;
						while (!done)
						{
							line = in.readLine ().trim ();
							if (!line.startsWith ("'"))
								throw new IOException ("Multi-line property value didn't start with '");
							
							done = !line.endsWith ("' +");
							if (done)
								propertyValue.append (line.substring (1, line.length () - 1));
							else
								propertyValue.append (line.substring (1, line.length () - 3));
						}
						
						simpleProperty (propertyName, propertyValue.toString ());
					}
					else
						throw new IOException ("Line container neither : nor =");
					
					// Read next line
					line = in.readLine ();
				}
			}
		}
		
		// Save XML
		if (!found)
			throw new IOException ("Didn't find form \"" + formName + "\" in DFM file");
		
		JAXBContext.newInstance (XmlLayoutContainer.class).createMarshaller ().marshal (layout, dest);
	}
	
	/**
	 * @param args Command line arguments, ignored
	 */
	public static final void main (final String [] args)
	{
		try
		{
			final DelphiDfmToXmlLayout converter = new DelphiDfmToXmlLayout ();
			converter.formName = "frmMiniCity";
			
			converter.convert (new File ("W:\\Delphi\\MoMIMEClient\\MomClientMain.dfm"),
				new File ("W:\\EclipseHome\\SourceForge\\MoMIMEClient\\src\\main\\resources\\momime.client.ui.dialogs\\frmMiniCity.xml"));
			
			System.out.println ("");
			System.out.println ("Done!");
		}
		catch (final Exception e)
		{
			e.printStackTrace ();
		}
	}
}