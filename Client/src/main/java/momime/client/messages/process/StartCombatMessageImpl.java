package momime.client.messages.process;

import java.io.IOException;

import jakarta.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

import momime.client.MomClient;
import momime.client.ui.frames.CombatUI;
import momime.client.ui.frames.CreateArtifactUI;
import momime.client.ui.frames.SpellBookUI;
import momime.client.ui.frames.UnitInfoUI;
import momime.common.calculations.UnitCalculations;
import momime.common.messages.MemoryUnit;
import momime.common.messages.servertoclient.StartCombatMessage;
import momime.common.messages.servertoclient.StartCombatMessageUnit;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.SpellCastType;
import momime.common.utils.UnitUtils;

/**
 * Server sends this to the client when they are involved in a combat to start things off - this includes
 * details of all the units in the combat and the terrain, so is probably the most complex multiplayer messages other than the massive FOW message.
 */
public final class StartCombatMessageImpl extends StartCombatMessage implements BaseServerToClientMessage
{
	/** Combat UI */
	private CombatUI combatUI;
	
	/** Unit utils */
	private UnitUtils unitUtils;

	/** Unit calculations */
	private UnitCalculations unitCalculations;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Spell book */
	private SpellBookUI spellBookUI;
	
	/** Crafting popup */
	private CreateArtifactUI createArtifactUI;

	/** expandUnitDetails method */
	private ExpandUnitDetails expandUnitDetails;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		// Put all units into combat
		for (final StartCombatMessageUnit unitLoc : getUnitPlacement ())
		{
			final MemoryUnit unit = getUnitUtils ().findUnitURN
				(unitLoc.getUnitURN (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit (), "StartCombatMessageImpl");
			
			unit.setCombatLocation (new MapCoordinates3DEx ((MapCoordinates3DEx) getCombatLocation ()));
			unit.setCombatPosition (unitLoc.getCombatPosition ());
			unit.setCombatHeading (unitLoc.getCombatHeading ());
			unit.setCombatSide (unitLoc.getCombatSide ());
			
			final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (unit, null, null, null,
				getClient ().getPlayers (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), getClient ().getClientDB ());
			
			getUnitCalculations ().giveUnitFullRangedAmmoAndMana (xu);
		}
		
		// Update any unit info screen that may be open
		// Do this as a separate pass, as we need to know all the units in the combat to correctly calculate skills as some other unit might give us a bonus
		for (final StartCombatMessageUnit unitLoc : getUnitPlacement ())
		{
			UnitInfoUI unitInfo = getClient ().getUnitInfos ().get (unitLoc.getUnitURN ());
			if (unitInfo != null)
				unitInfo.getUnitInfoPanel ().refreshUnitDetails ();
		}
		
		// Start up the UI
		getCombatUI ().setCombatLocation ((MapCoordinates3DEx) getCombatLocation ());
		getCombatUI ().setCombatTerrain (getCombatTerrain ());
		getCombatUI ().initNewCombat ();
		getCombatUI ().setVisible (true);
		
		// Switch spell book to showing combat spells
		getCreateArtifactUI ().setVisible (false);
		getSpellBookUI ().setCastType (SpellCastType.COMBAT);
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
	 * @return Unit calculations
	 */
	public final UnitCalculations getUnitCalculations ()
	{
		return unitCalculations;
	}

	/**
	 * @param calc Unit calculations
	 */
	public final void setUnitCalculations (final UnitCalculations calc)
	{
		unitCalculations = calc;
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
	 * @return Spell book
	 */
	public final SpellBookUI getSpellBookUI ()
	{
		return spellBookUI;
	}

	/**
	 * @param ui Spell book
	 */
	public final void setSpellBookUI (final SpellBookUI ui)
	{
		spellBookUI = ui;
	}

	/**
	 * @return Crafting popup
	 */
	public final CreateArtifactUI getCreateArtifactUI ()
	{
		return createArtifactUI;
	}

	/**
	 * @param ui Crafting popup
	 */
	public final void setCreateArtifactUI (final CreateArtifactUI ui)
	{
		createArtifactUI = ui;
	}

	/**
	 * @return expandUnitDetails method
	 */
	public final ExpandUnitDetails getExpandUnitDetails ()
	{
		return expandUnitDetails;
	}

	/**
	 * @param e expandUnitDetails method
	 */
	public final void setExpandUnitDetails (final ExpandUnitDetails e)
	{
		expandUnitDetails = e;
	}
}