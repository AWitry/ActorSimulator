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
