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
public class InstantLink extends AbstractLink
{

	InstantLink(ActorControl source, ActorControl dest)
	{
		super(source,dest);
	}
	

	@Override
	public boolean isIdle()
	{
		return true;
	}

	@Override
	public void sendMessage(Object message)
	{
		getDestinationMailbox().receive(new Message(getSourceActor(), getReverse(), message));
	}


	@Override
	public void shutdown()
	{
		
	}
	
}
