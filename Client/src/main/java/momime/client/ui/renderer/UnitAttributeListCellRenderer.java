package momime.client.ui.renderer;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import momime.client.MomClient;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;

/**
 * Renderer for drawing the name and component breakdown of a unit attribute
 */
public final class UnitAttributeListCellRenderer extends JPanel implements ListCellRenderer<UnitAttributeWithBreakdownImage>
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (UnitAttributeListCellRenderer.class);
	
	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Label displaying the attribute name */
	private JLabel textLabel;
	
	/** Label displaying the component breakdown */
	private JLabel imageLabel;
	
	/**
	 * Sets up the layout of the panel
	 */
	public final void init ()
	{
		setLayout (new BorderLayout ());
		
		textLabel = new JLabel ();
		add (textLabel, BorderLayout.WEST);
		
		imageLabel = new JLabel ();
		add (imageLabel, BorderLayout.EAST);
		
		setOpaque (false);

		// Leave a gap between one icon and the next
		setBorder (BorderFactory.createEmptyBorder (1, 0, 1, 0));
	}
	
	/**
	 * Sets up the image and label to draw the list cell
	 */
	@SuppressWarnings ("unused")
	@Override
	public final Component getListCellRendererComponent (final JList<? extends UnitAttributeWithBreakdownImage> list, final UnitAttributeWithBreakdownImage value,
		final int index, final boolean isSelected, final boolean cellHasFocus)
	{
		textLabel.setFont (getFont ());
		textLabel.setForeground (getForeground ());

		// Look up the name of the skill
		try
		{
			textLabel.setText (getLanguageHolder ().findDescription (getClient ().getClientDB ().findUnitSkill
				(value.getUnitSkillID (), "UnitAttributeListCellRenderer").getUnitSkillDescription ()));
		}
		catch (final Exception e)
		{
			log.error (e, e);
		}
		
		if (value.getUnitAttributeBreakdownImage () == null)
			imageLabel.setIcon (null);
		else
			imageLabel.setIcon (new ImageIcon (value.getUnitAttributeBreakdownImage ()));

		return this;
	}

	/**
	 * @return Language database holder
	 */
	public final LanguageDatabaseHolder getLanguageHolder ()
	{
		return languageHolder;
	}
	
	/**
	 * @param holder Language database holder
	 */
	public final void setLanguageHolder (final LanguageDatabaseHolder holder)
	{
		languageHolder = holder;
	}

	/**
	 * Convenience shortcut for accessing the Language XML database
	 * @return Language database
	 */
	public final MomLanguagesEx getLanguages ()
	{
		return languageHolder.getLanguages ();
	}

	/**
	 * @return Multiplayer client
	 */
	public final MomClient getClient ()
	{
		return client;
	}
	
	/**
	 * @param obj Multiplayer client
	 */
	public final void setClient (final MomClient obj)
	{
		client = obj;
	}
}