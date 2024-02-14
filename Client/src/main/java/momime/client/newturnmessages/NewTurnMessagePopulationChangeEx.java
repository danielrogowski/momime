package momime.client.newturnmessages;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.util.List;

import com.ndg.map.coordinates.MapCoordinates3DEx;

import momime.client.MomClient;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.ui.MomUIConstants;
import momime.client.ui.frames.CityViewUI;
import momime.client.ui.frames.PrototypeFrameCreator;
import momime.client.utils.TextUtils;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.LanguageText;
import momime.common.messages.NewTurnMessagePopulationChange;
import momime.common.messages.OverlandMapCityData;

/**
 * NTM about the population of a city either growing or dying over a 1,000 boundary
 */
public final class NewTurnMessagePopulationChangeEx extends NewTurnMessagePopulationChange
	implements NewTurnMessageExpiration, NewTurnMessageSimpleUI, NewTurnMessageClickable
{
	/** Current status of this NTM */
	private NewTurnMessageStatus status;
	
	/** Small font */
	private Font smallFont;
	
	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;
	
	/** Text utils */
	private TextUtils textUtils;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;
	
	/**
	 * @return One of the SORT_ORDER_ constants, indicating the sort order/title category to group this message under
	 */
	@Override
	public final NewTurnMessageSortOrder getSortOrder ()
	{
		final NewTurnMessageSortOrder sortOrder;
		if (getNewPopulation () > getOldPopulation ())
			sortOrder = NewTurnMessageSortOrder.SORT_ORDER_CITY_GROWTH;
		else
			sortOrder = NewTurnMessageSortOrder.SORT_ORDER_CITY_DEATH;
		
		return sortOrder;
	}

	/**
	 * @return Image to draw for this NTM, or null to display only text
	 */
	@Override
	public final Image getImage ()
	{
		return null;
	}
	
	/**
	 * @return Text to display for this NTM
	 */
	@Override
	public final String getText ()
	{
		final List<LanguageText> languageText;
		if (getOldPopulation () < CommonDatabaseConstants.MIN_CITY_POPULATION)
			languageText = (getNewPopulation () > getOldPopulation ()) ?
				getLanguages ().getNewTurnMessages ().getOutpostGrowth () : getLanguages ().getNewTurnMessages ().getOutpostDeath ();
		else
			languageText = (getNewPopulation () > getOldPopulation ()) ?
				getLanguages ().getNewTurnMessages ().getCityGrowth () : getLanguages ().getNewTurnMessages ().getCityDeath ();
		
		return getLanguageHolder ().findDescription (languageText).replaceAll
			("CITY_NAME", getCityName ()).replaceAll
			("OLD_POPULATION", getTextUtils ().intToStrCommas (getOldPopulation ())).replaceAll
			("NEW_POPULATION", getTextUtils ().intToStrCommas (getNewPopulation ()));
	}
	
	/**
	 * @return Font to display the text in
	 */
	@Override
	public final Font getFont ()
	{
		return getSmallFont ();
	}
	
	/**
	 * @return Colour to display the text in
	 */
	@Override
	public final Color getColour ()
	{
		return MomUIConstants.SILVER;
	}
	
	/**
	 * Clicking on population changes brings up the city screen
	 * @throws Exception If there is a problem
	 */
	@Override
	public final void clicked () throws Exception
	{
		final OverlandMapCityData cityData = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
			(getCityLocation ().getZ ()).getRow ().get (getCityLocation ().getY ()).getCell ().get (getCityLocation ().getX ()).getCityData ();
		if (cityData != null)
		{
			// Is there a city view already open for this city?
			CityViewUI cityView = getClient ().getCityViews ().get (getCityLocation ().toString ());
			if (cityView == null)
			{
				cityView = getPrototypeFrameCreator ().createCityView ();
				cityView.setCityLocation (new MapCoordinates3DEx ((MapCoordinates3DEx) getCityLocation ()));
				getClient ().getCityViews ().put (getCityLocation ().toString (), cityView);
			}
			
			cityView.setVisible (true);
		}
	}
	
	/**
	 * @return Current status of this NTM
	 */
	@Override
	public final NewTurnMessageStatus getStatus ()
	{
		return status;
	}
	
	/**
	 * @param newStatus New status for this NTM
	 */
	@Override
	public final void setStatus (final NewTurnMessageStatus newStatus)
	{
		status = newStatus;
	}

	/**
	 * @return Small font
	 */
	public final Font getSmallFont ()
	{
		return smallFont;
	}

	/**
	 * @param font Small font
	 */
	public final void setSmallFont (final Font font)
	{
		smallFont = font;
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
	 * @return Text utils
	 */
	public final TextUtils getTextUtils ()
	{
		return textUtils;
	}

	/**
	 * @param tu Text utils
	 */
	public final void setTextUtils (final TextUtils tu)
	{
		textUtils = tu;
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

	/**
	 * @return Prototype frame creator
	 */
	public final PrototypeFrameCreator getPrototypeFrameCreator ()
	{
		return prototypeFrameCreator;
	}

	/**
	 * @param obj Prototype frame creator
	 */
	public final void setPrototypeFrameCreator (final PrototypeFrameCreator obj)
	{
		prototypeFrameCreator = obj;
	}
}