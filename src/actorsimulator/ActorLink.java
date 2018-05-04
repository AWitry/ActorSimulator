/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package actorsimulator;


/**
 * Represents a uni-directional link to a remote actor.
 * A reverse link may or may not exist.
 * Outgoing links are maintained by each actor individually.
 * All methods are required to be thread safe.
 */
public interface ActorLink
{
	/**
	 * Retrieves an actor link pointing in opposite direction.
	 * @return Reverse actor link, or null if no such currently exists
	 */
	ActorLink	getReverse();
	/**
	 * Sends a message along the local link.
	 * @param message Message to send. May be null
	 */
	void		sendMessage(Object message);
	/**
	 * Determines if there are no pending messages on the local link
	 * @return True if no messages are awaiting delivery, false otherwise
	 */
	boolean		isIdle();
	
	/**
	 * Fetches the destination actor
	 * @return Destination actor. Never null
	 */
	Actor		getDestinationActor();
}
