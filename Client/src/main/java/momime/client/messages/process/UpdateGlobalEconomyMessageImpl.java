package momime.client.messages.process;

import java.io.IOException;

import jakarta.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.client.MomClient;
import momime.client.ui.frames.AlchemyUI;
import momime.client.ui.frames.CitiesListUI;
import momime.client.ui.frames.CityViewUI;
import momime.client.ui.frames.CombatUI;
import momime.client.ui.frames.HeroItemsUI;
import momime.client.ui.frames.MagicSlidersUI;
import momime.client.ui.frames.WizardsUI;
import momime.client.ui.panels.OverlandMapRightHandPanel;
import momime.common.messages.servertoclient.UpdateGlobalEconomyMessage;

/**
 * Server sends this to each client to tell them what their current production rates and storage are.
 * 
 * This is a good place to send OverlandCastingSkillRemainingThisTurn to the client as well, since any instantly cast spells
 * will result in mana being reduced so new GPVs will need to be sent anyway (and recalc'd in case the new instantly cast spell has some maintenance).
 * 
 * Similarly the OverlandCastingSkillRemainingThisTurn value needs to be set on the client at the start of each turn, so why not include it in the GPV message.
 * 
 * Also both stored mana and OverlandCastingSkillRemainingThisTurn being set on the client simultaneously is convenient
 * for working out EffectiveCastingSkillRemainingThisTurn.
 * 
 * CastingSkillRemainingThisCombat is also sent by the server to avoid having to repeat the skill calc on the client,
 * since new GPVs are sent (to update mana) every time we cast a combat spell.
 */
public final class UpdateGlobalEconomyMessageImpl extends UpdateGlobalEconomyMessage implements BaseServerToClientMessage
{
	/** Multiplayer client */
	private MomClient client;
	
	/** Overland map right hand panel showing economy etc */
	private OverlandMapRightHandPanel overlandMapRightHandPanel;

	/** Magic sliders screen */
	private MagicSlidersUI magicSlidersUI;
	
	/** Alchemy UI */
	private AlchemyUI alchemyUI;
	
	/** Combat UI */
	private CombatUI combatUI;
	
	/** Hero items UI */
	private HeroItemsUI heroItemsUI;
	
	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/** Wizards UI */
	private WizardsUI wizardsUI;
	
	/** Cities list */
	private CitiesListUI citiesListUI;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		// Accept new values
		getClient ().getOurPersistentPlayerPrivateKnowledge ().getResourceValue ().clear ();
		getClient ().getOurPersistentPlayerPrivateKnowledge ().getResourceValue ().addAll (getResourceValue ());
		
		getClient ().getOurTransientPlayerPrivateKnowledge ().setOverlandCastingSkillRemainingThisTurn (getOverlandCastingSkillRemainingThisTurn ());
		
		// Update new values in UI screens that rely on any resource values
		getOverlandMapRightHandPanel ().updateGlobalEconomyValues ();
		getOverlandMapRightHandPanel ().updateProductionTypesStoppingUsFromEndingTurn ();
		getMagicSlidersUI ().updateProductionLabels ();
		getAlchemyUI ().updateSliderMaximum ();
		getHeroItemsUI ().updateGlobalEconomyValues ();
		
		// We may now have more gold to rush buy construction projects
		for (final CityViewUI cityView : getClient ().getCityViews ().values ())
			cityView.recheckRushBuyEnabled ();
		
		getCitiesListUI ();
		
		// Update remaining casting skill and MP in the combat we're in.
		// Usually this value is ever non-null is if our wizard casts a combat spell, so stop us from casting another one this turn, but Mana Leak is an exception
		if ((getCombatUI ().isVisible ()) && (getCastingSkillRemainingThisCombat () != null))
		{
			getCombatUI ().updateRemainingCastingSkill (getCastingSkillRemainingThisCombat ());
			
			if ((isSpellCastThisCombatTurn () != null) && (isSpellCastThisCombatTurn ()))
				getCombatUI ().setSpellCastThisCombatTurn (true);
		}
		
		// Update fame, if we're looking at ourselves
		final PlayerPublicDetails ourPlayer = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getClient ().getOurPlayerID (), "UpdateGlobalEconomyMessageImpl");
		getWizardsUI ().wizardUpdated (ourPlayer);
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
	 * @return Overland map right hand panel showing economy etc
	 */
	public final OverlandMapRightHandPanel getOverlandMapRightHandPanel ()
	{
		return overlandMapRightHandPanel;
	}

	/**
	 * @param panel Overland map right hand panel showing economy etc
	 */
	public final void setOverlandMapRightHandPanel (final OverlandMapRightHandPanel panel)
	{
		overlandMapRightHandPanel = panel;
	}

	/**
	 * @return Magic sliders screen
	 */
	public final MagicSlidersUI getMagicSlidersUI ()
	{
		return magicSlidersUI;
	}

	/**
	 * @param ui Magic sliders screen
	 */
	public final void setMagicSlidersUI (final MagicSlidersUI ui)
	{
		magicSlidersUI = ui;
	}

	/**
	 * @return Alchemy UI
	 */
	public final AlchemyUI getAlchemyUI ()
	{
		return alchemyUI;
	}

	/**
	 * @param ui Alchemy UI
	 */
	public final void setAlchemyUI (final AlchemyUI ui)
	{
		alchemyUI = ui;
	}

	/**
	 * @return Combat UI
	 */
	public final CombatUI getCombatUI ()
	{
		return combatUI;
	}

	/**
	 * @param ui Combat UI
	 */
	public final void setCombatUI (final CombatUI ui)
	{
		combatUI = ui;
	}

	/**
	 * @return Hero items UI
	 */
	public final HeroItemsUI getHeroItemsUI ()
	{
		return heroItemsUI;
	}

	/**
	 * @param ui Hero items UI
	 */
	public final void setHeroItemsUI (final HeroItemsUI ui)
	{
		heroItemsUI = ui;
	}

	/**
	 * @return Session utils
	 */
	public final MultiplayerSessionUtils getMultiplayerSessionUtils ()
	{
		return multiplayerSessionUtils;
	}

	/**
	 * @param util Session utils
	 */
	public final void setMultiplayerSessionUtils (final MultiplayerSessionUtils util)
	{
		multiplayerSessionUtils = util;
	}

	/**
	 * @return Wizards UI
	 */
	public final WizardsUI getWizardsUI ()
	{
		return wizardsUI;
	}

	/**
	 * @param ui Wizards UI
	 */
	public final void setWizardsUI (final WizardsUI ui)
	{
		wizardsUI = ui;
	}

	/**
	 * @return Cities list
	 */
	public final CitiesListUI getCitiesListUI ()
	{
		return citiesListUI;
	}

	/**
	 * @param ui Cities list
	 */
	public final void setCitiesListUI (final CitiesListUI ui)
	{
		citiesListUI = ui;
	}
}