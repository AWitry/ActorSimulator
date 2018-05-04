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
 *
 * @author IronFox
 */
public class Message
{
	private final ActorLink linkToSender;
	private final Actor sender;
	private final Object message;
	
	public Message(Actor sender, ActorLink linkToSender, Object message)
	{
		this.sender = sender;
		this.linkToSender = linkToSender;	
		this.message = message;
	}
	
	/**
	 * Fetches the sender of the local instance
	 * @return Sending actor. Never null
	 */
	public Actor getSender()
	{
		return	sender;
	}
	
	/**
	 * Fetches a reverse link to reply on.
	 * May be null
	 * @return Link to the sending actor if such exists, or null if not
	 */
	public ActorLink getLinkToSender()
	{
		return linkToSender;
	}
	
	/**
	 * Fetches the message content.
	 * May be null
	 * @return Message content or null if no such exists
	 */
	public Object getContent()
	{
		return message;	
	}
}
