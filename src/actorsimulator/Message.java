/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
