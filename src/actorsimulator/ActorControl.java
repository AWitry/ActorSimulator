/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package actorsimulator;

/**
 * Primary actor control as seen by an owner.
 * Exposes link and thread controls
 */
public interface ActorControl extends Actor, Mailbox, ActorLogicInterface
{
	OutgoingLinks	getOutgoingLinks();

	/**
	 * Starts any necessary threads. 
	 * Must be called exactly once
	 */
	void		start();
	/**
	 * Terminates any running threads of the local actor
	 */
	void		shutdown();

}
