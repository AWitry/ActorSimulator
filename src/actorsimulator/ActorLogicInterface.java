/* 
 * Copyright 2018 IronFox.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package actorsimulator;

import java.util.function.Consumer;

/**
 * Control interface for actor logics.
 * All methods are thread safe
 */
public interface ActorLogicInterface
{
	/**
	 * Tries to fetch the next incoming message.
	 * If no messages are pending, then null is returned immediately.
	 * Received messages are in deterministic order only where received from
	 * the same sender.
	 * @return Next message or null if none are pending.
	 */
	Message		tryGetNextMessage();
	
	/**
	 * Fetches the next incoming message or waits until one is received.
	 * If no messages are pending, the method blocks until the next message
	 * is received. The local actor is considered passive until the method
	 * returns.
	 * Received messages are in deterministic order only where received from
	 * the same sender.
	 * @return Next message. Never null
	 */
	Message		waitGetNextMessage();
	
	/**
	 * Visits all outgoing actor links.
	 * @param visitor Visitor to receive all outgoing actor links.
	 */
	void		visitOutgoing(Consumer<? super ActorLink> visitor);
	
	/**
	 * Fetches any one existing outgoing link, or null if none exist.
	 * It is up to the interface implementation to determine which link
	 * is returned if multiple exist.
	 * @return Link along any outgoing communication edge, or null if there
	 * are none.
	 */
	ActorLink	getAnyOutgoing();
	/**
	 * Broadcasts a message to all immediately connected remote actors.
	 * @param msg Message to broadcast
	 */
	void		broadcast(Object msg);
	/**
	 * Retrieves the actor associated with the local interface.
	 * @return Local actor. Never null
	 */
	Actor		getLocalActor();
	/**
	 * Instantiates a new actor using the specified logic and creates
	 * a one-way link to it.
	 * @param logic Actor logic to use
	 * @return Link to the newly created actor
	 */
	ActorLink	instantiate(ActorLogic logic);
	/**
	 * Attempts to establish a connection to the specified actor.
	 * If a connection already exists, the existing connection is returned.
	 * @param remote Actor to connect to
	 * @return New or existing link to the specified remote actor
	 */
	ActorLink	connectTo(Actor remote);
	/**
	 * Attempts to locate an existing link to the specified actor.
	 * @param remote Actor to find a link for
	 * @return Existing link to the specified remote actor, or null if no such
	 * was found.
	 */
	ActorLink	findConnectionTo(Actor remote);

	/**
	 * Appends a message to the execution log.
	 * @param msg Message to append.
	 */
	void		log(String msg);
	
}
