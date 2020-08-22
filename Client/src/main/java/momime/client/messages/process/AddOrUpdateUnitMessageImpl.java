package momime.client.messages.process;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.base.client.BaseServerToClientMessage;
import com.ndg.utils.Holder;

import momime.client.MomClient;
import momime.client.process.CombatMapProcessing;
import momime.client.process.OverlandMapProcessing;
import momime.client.ui.frames.ArmyListUI;
import momime.client.ui.frames.CityViewUI;
import momime.client.ui.frames.HeroItemsUI;
import momime.client.ui.frames.UnitInfoUI;
import momime.common.database.CommonDatabaseConstants;
import momime.common.messages.MemoryUnit;
import momime.common.messages.UnitStatusID;
import momime.common.messages.servertoclient.AddOrUpdateUnitMessage;
import momime.common.utils.UnitUtils;

/**
 * Server sends this to clients to tell them about a new unit added to the map, or can add them in bulk as part of fogOfWarVisibleAreaChanged.
 * 
 * If readSkillsFromXML is true, client will read unit skills from the XML database (otherwise the client, receiving a message with zero skills, cannot tell if it is a hero who
 * genuinely has no skills (?) or is expected to read in the skills from the XML database).
 * 
 * If skills are included, the Experience value is not used so is omitted, since the Experience value will be included in the skill list.
 * 
 * Bulk adds (fogOfWarVisibleAreaChanged) can contain a mixture of units with and without skill lists included.
 */
public final class AddOrUpdateUnitMessageImpl extends AddOrUpdateUnitMessage implements BaseServerToClientMessage
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (AddOrUpdateUnitMessageImpl.class);

	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Army list */
	private ArmyListUI armyListUI;

	/** Hero items UI */
	private HeroItemsUI heroItemsUI;
	
	/** Turn sequence and movement helper methods */
	private OverlandMapProcessing overlandMapProcessing;
	
	/** Combat map processing */
	private CombatMapProcessing combatMapProcessing;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering start: Unit URN " + getMemoryUnit ().getUnitURN ());
		
		final List<MapCoordinates3DEx> unitLocations = new ArrayList<MapCoordinates3DEx> ();
		final Holder<MapCoordinates3DEx> ourUnitLocation = new Holder<MapCoordinates3DEx> ();
		final Holder<Boolean> anyOfOurHeroes = new Holder<Boolean> (false);
		
		processOneUpdate (unitLocations, ourUnitLocation, anyOfOurHeroes);
		endUpdates (unitLocations, ourUnitLocation, anyOfOurHeroes);
		
		log.trace ("Exiting start");
	}
	
	/**
	 * Method called for each individual update; so called once if message was sent in isolation, or multiple times if part of FogOfWarVisibleAreaChangedMessage
	 * 
	 * @param unitLocations Keeps track of the locations where all units were updated (even ones that are not ours)
	 * @param ourUnitLocation Keeps track the location of any updated unit that belongs to us
	 * @param anyOfOurHeroes Keeps track of whether any of the updated units belong to us and are heroes
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	public final void processOneUpdate (final List<MapCoordinates3DEx> unitLocations, final Holder<MapCoordinates3DEx> ourUnitLocation, final Holder<Boolean> anyOfOurHeroes)
		throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering processOneUpdate: Unit URN " + getMemoryUnit ().getUnitURN ());

		// Since Java server now supports units set to 'remember as last seen', its possible to get an 'add unit' message just to
		// update a unit that we remember in a different state - so if we already have the unit, update the existing one, otherwise add it.
		// This stops us screwing up references to the existing MemoryUnit obj, especially in the unitsLeftToMoveOverland list and the selectUnitButtons.
		final MemoryUnit oldUnit = getUnitUtils ().findUnitURN (getMemoryUnit ().getUnitURN (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit ());
		if (oldUnit != null)
		{
			// If it had no special order before, and it now does - then the only way that can happen if its our unit and our turn
			if ((oldUnit.getSpecialOrder () == null) && (getMemoryUnit ().getSpecialOrder () != null))
			{
				// So in that case, remove it from the wait list
				getOverlandMapProcessing ().removeUnitFromLeftToMoveOverland (oldUnit);
				getOverlandMapProcessing ().selectNextUnitToMoveOverland ();
			}
			
			// Also remove if it has no movement (but in this case a separate message it sent to trigger selecting the next unit)
			else if (getMemoryUnit ().getDoubleOverlandMovesLeft () == 0)
				getOverlandMapProcessing ().removeUnitFromLeftToMoveOverland (oldUnit);

			// Check this now, before we trash over the oldUnit values
			final boolean selectNextCombatUnit = ((getMemoryUnit ().getOwningPlayerID () == getClient ().getOurPlayerID ()) && (getMemoryUnit ().getCombatLocation () != null) &&
				(oldUnit.getDoubleCombatMovesLeft () != null) && (oldUnit.getDoubleCombatMovesLeft () > 0) &&
				(getMemoryUnit ().getDoubleCombatMovesLeft () != null) && (getMemoryUnit ().getDoubleCombatMovesLeft () <= 0));
				
			// Now copy it
			getUnitUtils ().copyUnitValues (getMemoryUnit (), oldUnit, true);
			
			// Update any unit info screen that may be open
			UnitInfoUI unitInfo = getClient ().getUnitInfos ().get (oldUnit.getUnitURN ());
			if (unitInfo != null)
				unitInfo.getUnitInfoPanel ().refreshUnitDetails ();
			
			// If its our unit, and it expended all its combat movement, then trigger next unit to move
			if (selectNextCombatUnit)
			{
				getCombatMapProcessing ().removeUnitFromLeftToMoveCombat (oldUnit);
				getCombatMapProcessing ().selectNextUnitToMoveCombat ();
			}
		}
		else
			getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit ().add (getMemoryUnit ());
		
		// Record where any of our units were updated
		if ((getMemoryUnit ().getStatus () == UnitStatusID.ALIVE) && (getMemoryUnit ().getUnitLocation () != null) && (!unitLocations.contains (getMemoryUnit ().getUnitLocation ())))
			unitLocations.add ((MapCoordinates3DEx) getMemoryUnit ().getUnitLocation ());
		
		// Keep track of things that only need updating once, if we have a lot of unit updates to process
		if (getMemoryUnit ().getOwningPlayerID () == getClient ().getOurPlayerID ())
		{
			ourUnitLocation.setValue ((MapCoordinates3DEx) getMemoryUnit ().getUnitLocation ());
			
			if (getClient ().getClientDB ().findUnit (getMemoryUnit ().getUnitID (), "AddOrUpdateUnitMessageImpl").getUnitMagicRealm ().equals
				(CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO))
				
				anyOfOurHeroes.setValue (true);
		}
		
		log.trace ("Exiting processOneUpdate");
	}

	/**
	 * Called after processOneUpdate has been called n times
	 * 
	 * @param unitLocations Keeps track of the locations where all units were updated (even ones that are not ours)
	 * @param ourUnitLocation Keeps track the location of any updated unit that belongs to us
	 * @param anyOfOurHeroes Keeps track of whether any of the updated units belong to us and are heroes
	 * @throws IOException If there is a problem
	 */
	public final void endUpdates (final List<MapCoordinates3DEx> unitLocations, final Holder<MapCoordinates3DEx> ourUnitLocation, final Holder<Boolean> anyOfOurHeroes)
		throws IOException
	{
		log.trace ("Entering endUpdates");
		
		// Select unit buttons on the City screen
		for (final MapCoordinates3DEx unitLocation : unitLocations)
		{
			final CityViewUI cityView = getClient ().getCityViews ().get (unitLocation.toString ());
			if (cityView != null)
				cityView.unitsChanged ();
		}
		
		if (ourUnitLocation.getValue () != null)
			getArmyListUI ().refreshArmyList (ourUnitLocation.getValue ());
		
		if (anyOfOurHeroes.getValue ())
			getHeroItemsUI ().refreshHeroes ();
		
		log.trace ("Exiting endUpdates");
	}
	
	/**
	 * @return Unit utils
	 */
	public final UnitUtils getUnitUtils ()
	{
		return unitUtils;
	}

	/**
	 * @param utils Unit utils
	 */
	public final void setUnitUtils (final UnitUtils utils)
	{
		unitUtils = utils;
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
	 * @return Army list
	 */
	public final ArmyListUI getArmyListUI ()
	{
		return armyListUI;
	}

	/**
	 * @param ui Army list
	 */
	public final void setArmyListUI (final ArmyListUI ui)
	{
		armyListUI = ui;
	}

	/**
	 * @return Turn sequence and movement helper methods
	 */
	public final OverlandMapProcessing getOverlandMapProcessing ()
	{
		return overlandMapProcessing;
	}

	/**
	 * @param proc Turn sequence and movement helper methods
	 */
	public final void setOverlandMapProcessing (final OverlandMapProcessing proc)
	{
		overlandMapProcessing = proc;
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
	 * @return Combat map processing
	 */
	public final CombatMapProcessing getCombatMapProcessing ()
	{
		return combatMapProcessing;
	}

	/**
	 * @param proc Combat map processing
	 */
	public final void setCombatMapProcessing (final CombatMapProcessing proc)
	{
		combatMapProcessing = proc;
	}
}