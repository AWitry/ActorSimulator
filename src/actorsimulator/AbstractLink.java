/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
