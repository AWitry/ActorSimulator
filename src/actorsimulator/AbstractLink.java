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
 * Abstract base implementation of ActorLink.
 * Required base class of non-abstract ActorLink implementations.
 * Implements getReverse(), and getDestinationActor().
 */
public abstract class AbstractLink implements ActorLink
{
	private final ActorControl source,destination;
	private ActorLink reverse;

	
	public AbstractLink(ActorControl source, ActorControl destination)
	{
		this.destination = destination;
		this.source = source;
	}
	
	@Override
	public ActorLink getReverse()
	{
		return reverse;
	}

	@Override
	public Actor getDestinationActor()
	{
		return destination;
	}
	
	/**
	 * Fetches the local source actor.
	 * @return Link source actor. Never null
	 */
	Actor getSourceActor()
	{
		return source;
	}
	
	/**
	 * 
	 * @return 
	 */
	void signalSend()
	{
		source.signalMessageSent();
	}
	
	public Mailbox getDestinationMailbox()
	{
		return destination;
	}
	
	
	/**
	 * Establishes a reverse-link between the local and the given link.
	 * Neither the local nor the given other link must already have reverse
	 * links.
	 * @param other Link to entangle with. Must not be null. Must not be this
	 */
	void entangle(AbstractLink other)
	{
		if (reverse != null)
			throw new IllegalArgumentException(this+" is already entangled");
		if (other.reverse != null)
			throw new IllegalArgumentException(other+" is already entangled");
		if (other == this)
			throw new IllegalArgumentException("Cannot entangle this");
		this.reverse = other;
		other.reverse = this;
	}
	
	
	/**
	 * Shuts down any threads running on the local link
	 */
	abstract void shutdown();
			
}
