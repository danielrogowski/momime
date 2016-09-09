package momime.server.messages.process;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.calculations.HeroItemCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.HeroSlotAllowedItemType;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MemoryUnitHeroItemSlot;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.NumberedHeroItem;
import momime.common.messages.UnitStatusID;
import momime.common.messages.clienttoserver.RequestMoveHeroItemMessage;
import momime.common.messages.servertoclient.AddUnassignedHeroItemMessage;
import momime.common.messages.servertoclient.RemoveUnassignedHeroItemMessage;
import momime.common.messages.servertoclient.TextPopupMessage;
import momime.common.utils.HeroItemUtils;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.calculations.ServerResourceCalculations;
import momime.server.database.HeroItemSlotTypeSvr;
import momime.server.database.UnitSvr;
import momime.server.fogofwar.FogOfWarMidTurnChanges;

/**
 * Client sends to request that a hero item be moved from one location to another.
 * The to/from locations can be their bank, the anvil, or actually being used by a hero.
 */
public final class RequestMoveHeroItemMessageImpl extends RequestMoveHeroItemMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (RequestMoveHeroItemMessageImpl.class);

	/** Hero item utils */
	private HeroItemUtils heroItemUtils;
	
	/** Hero item calculations */
	private HeroItemCalculations heroItemCalculations;
	
	/** Resource value utils */
	private ResourceValueUtils resourceValueUtils;
	
	/** Resource calculations */
	private ServerResourceCalculations serverResourceCalculations;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnChanges fogOfWarMidTurnChanges;
	
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If various elements cannot be found in the DB
	 * @throws MomException If an AI player has enough books that they should get some free spells, but we can't find any suitable free spells to give them
	 * @throws PlayerNotFoundException If the player who owns the unit cannot be found, when updating FOW from unit changes
	 */
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
	{
		log.trace ("Entering process: Hero Item URN " + getHeroItemURN () + " from " + getFromLocation () + " to " + getToLocation ());
		
		final MomSessionVariables mom = (MomSessionVariables) thread;

		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) sender.getPersistentPlayerPrivateKnowledge ();
		String error = null;
		NumberedHeroItem item = null;

		// Find unit(s) involved
		final MemoryUnit fromHero = (getFromUnitURN () == null) ? null : getUnitUtils ().findUnitURN
			(getFromUnitURN (), mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ());

		final MemoryUnit toHero = (getToUnitURN () == null) ? null : getUnitUtils ().findUnitURN
			(getToUnitURN (), mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ());
		
		// Validate that the item is where they claim it is
		if (getFromLocation () == null)
			error = "You tried to move a hero item from an unspecified location.";
		else
			switch (getFromLocation ())
			{
				case UNASSIGNED:
					item = getHeroItemUtils ().findHeroItemURN (getHeroItemURN (), priv.getUnassignedHeroItem ());
					if (item == null)
						error = "Cannot find the hero item you are trying to move from your bank.";
					break;
					
				case HERO:
					if (fromHero == null)
						error = "Cannot find the hero who you want to move the item from.";
					else if (fromHero.getOwningPlayerID () != sender.getPlayerDescription ().getPlayerID ())
						error = "The hero who you want to move the item from isn't yours.";
					else if (fromHero.getStatus () != UnitStatusID.ALIVE)
						error = "The hero who you want to move the item from is dead or was never summoned.";
					else if ((getFromSlotNumber () == null) || (getFromSlotNumber () < 0) || (getFromSlotNumber () >= fromHero.getHeroItemSlot ().size ()))
						error = "The hero who you want to move the item from doesn't have the specified slot number.";
					{
						final MemoryUnitHeroItemSlot slot = fromHero.getHeroItemSlot ().get (getFromSlotNumber ());
						if (slot.getHeroItem () == null)
							error = "The hero who you want to move the item from has no item in the specified slot number.";
						else if (slot.getHeroItem ().getHeroItemURN () != getHeroItemURN ())
							error = "The hero who you want to move the item from has a different item in the specified slot number.";
						else
							item = slot.getHeroItem ();
					}
					break;
					
				default:
					error = "You tried to move a hero item from an unrecognized location " + getFromLocation () + ".";
			}
		
		// Validate destination
		if (error == null)
		{
			if (getToLocation () == null)
				error = "You tried to move a hero item to an unspecified location.";
			else
				switch (getToLocation ())
				{
					// Breaking item on the anvil is always OK
					// Moving to the bank is always OK
					case DESTROY:
					case UNASSIGNED:
						break;

					case HERO:
						if (toHero == null)
							error = "Cannot find the hero who you want to move the item to.";
						else if (toHero.getOwningPlayerID () != sender.getPlayerDescription ().getPlayerID ())
							error = "The hero who you want to move the item to isn't yours.";
						else if (toHero.getStatus () != UnitStatusID.ALIVE)
							error = "The hero who you want to move the item to is dead or was never summoned.";
						else if ((getToSlotNumber () == null) || (getToSlotNumber () < 0) || (getToSlotNumber () >= toHero.getHeroItemSlot ().size ()))
							error = "The hero who you want to move the item to doesn't have the specified slot number.";
						{
							final MemoryUnitHeroItemSlot slot = toHero.getHeroItemSlot ().get (getToSlotNumber ());
							if (slot.getHeroItem () != null)
								error = "The hero who you want to move the item to already has another item in the specified slot number.";
							else
							{
								// Now have to go get the slot type to verify the item can be put in it
								final UnitSvr unitDef = mom.getServerDB ().findUnit (toHero.getUnitID (), "RequestMoveHeroItemMessageImpl");
								if (getToSlotNumber () >= unitDef.getHeroItemSlot ().size ())
									error = "The hero who you want to move the item to doesn't define the specified slot number.";
								else
								{
									final String slotTypeID = unitDef.getHeroItemSlot ().get (getToSlotNumber ()).getHeroItemSlotTypeID ();
									final HeroItemSlotTypeSvr slotType = mom.getServerDB ().findHeroItemSlotType (slotTypeID, "RequestMoveHeroItemMessageImpl");
									boolean ok = false;
									for (final HeroSlotAllowedItemType allowed : slotType.getHeroSlotAllowedItemType ())
										if (allowed.getHeroItemTypeID ().equals (item.getHeroItemTypeID ()))
											ok = true;
									
									if (!ok)
										error = "The chosen item is the wrong type to go into this slot.";
								}
							}
						}
						break;
						
					default:
						error = "You tried to move a hero item to an unrecognized location " + getToLocation () + ".";
				}
		}
		
		// All ok?
		if (error != null)
		{
			// Return error
			log.warn ("process: " + sender.getPlayerDescription ().getPlayerName () + " got an error: " + error);

			final TextPopupMessage reply = new TextPopupMessage ();
			reply.setText (error);
			sender.getConnection ().sendMessageToClient (reply);
		}
		else
		{
			// Remove item from location
			switch (getFromLocation ())
			{
				case UNASSIGNED:
					// Remove on server
					priv.getUnassignedHeroItem ().remove (item);
					
					// Remove on client
					final RemoveUnassignedHeroItemMessage msg = new RemoveUnassignedHeroItemMessage ();
					msg.setHeroItemURN (getHeroItemURN ());
					sender.getConnection ().sendMessageToClient (msg);					
					break;
					
				case HERO:
					fromHero.getHeroItemSlot ().get (getFromSlotNumber ()).setHeroItem (null);
					getFogOfWarMidTurnChanges ().updatePlayerMemoryOfUnit (fromHero, mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
						mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription ().getFogOfWarSetting ());
					break;
					
				default:
					throw new MomException ("RequestMoveHeroItemMessage doesn't know how to remove hero item from location " + getFromLocation ());
			}
			
			// Move item new location
			switch (getToLocation ())
			{
				// Break item on the anvil
				case DESTROY:
					final int manaGained = getHeroItemCalculations ().calculateCraftingCost (item, mom.getServerDB ()) / 2;
					getResourceValueUtils ().addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, manaGained);
					getServerResourceCalculations ().sendGlobalProductionValues (sender, null);
					break;
					
				case UNASSIGNED:
					// Add on server
					priv.getUnassignedHeroItem ().add (item);
					
					// Add on client
					final AddUnassignedHeroItemMessage msg = new AddUnassignedHeroItemMessage ();
					msg.setHeroItem (item);
					sender.getConnection ().sendMessageToClient (msg);					
					break;

				case HERO:
					toHero.getHeroItemSlot ().get (getToSlotNumber ()).setHeroItem (item);
					getFogOfWarMidTurnChanges ().updatePlayerMemoryOfUnit (toHero, mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
						mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription ().getFogOfWarSetting ());
					break;

				default:
					throw new MomException ("RequestMoveHeroItemMessage doesn't know how to move hero item to location " + getToLocation ());
			}
		}

		log.trace ("Exiting process");
	}

	/**
	 * @return Hero item utils
	 */
	public final HeroItemUtils getHeroItemUtils ()
	{
		return heroItemUtils;
	}

	/**
	 * @param util Hero item utils
	 */
	public final void setHeroItemUtils (final HeroItemUtils util)
	{
		heroItemUtils = util;
	}

	/**
	 * @return Hero item calculations
	 */
	public final HeroItemCalculations getHeroItemCalculations ()
	{
		return heroItemCalculations;
	}

	/**
	 * @param calc Hero item calculations
	 */
	public final void setHeroItemCalculations (final HeroItemCalculations calc)
	{
		heroItemCalculations = calc;
	}

	/**
	 * @return Resource value utils
	 */
	public final ResourceValueUtils getResourceValueUtils ()
	{
		return resourceValueUtils;
	}

	/**
	 * @param utils Resource value utils
	 */
	public final void setResourceValueUtils (final ResourceValueUtils utils)
	{
		resourceValueUtils = utils;
	}

	/**
	 * @return Resource calculations
	 */
	public final ServerResourceCalculations getServerResourceCalculations ()
	{
		return serverResourceCalculations;
	}

	/**
	 * @param calc Resource calculations
	 */
	public final void setServerResourceCalculations (final ServerResourceCalculations calc)
	{
		serverResourceCalculations = calc;
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
	 * @return Methods for updating true map + players' memory
	 */
	public final FogOfWarMidTurnChanges getFogOfWarMidTurnChanges ()
	{
		return fogOfWarMidTurnChanges;
	}

	/**
	 * @param obj Methods for updating true map + players' memory
	 */
	public final void setFogOfWarMidTurnChanges (final FogOfWarMidTurnChanges obj)
	{
		fogOfWarMidTurnChanges = obj;
	}
}