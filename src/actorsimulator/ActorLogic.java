/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package actorsimulator;

/**
 * Logic associated with an actor.
 * Individual instances of this interface should not be reused across multiple
 * actors unless they are stateless.
 */
public interface ActorLogic
{
	/**
	 * Executed when created and re-executed upon arrival of new messages.
	 * While running, and not waiting via ActorLogicInterface.waitGetNextMessage()
	 * the hosting actor is considered active.
	 * There is no time-bound on when execute() should return, and new messages
	 * will be delivered even if it does not, but until it does or waits for new
	 * messages, the entire network must be considered running.
	 * Unless reused across multiple actors, the implementing object need not be
	 * thread safe as it is executed only by one thread.
	 * @param iface Actor control interface to the local actor
	 * to receive/send messages, query/manipulate the topology,
	 * or spawn new actors.
	 */
	public void execute(ActorLogicInterface iface);
}
