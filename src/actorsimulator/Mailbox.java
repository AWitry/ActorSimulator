/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package actorsimulator;

import java.util.UUID;


/**
 *
 * @author IronFox
 */
public interface Mailbox
{
	/**
	 * Immediately receives a message.
	 * Used by implementations of ActorLink to deliver messages.
	 * @param env Envelope to deliver
	 */
	void receive(Message env);
}

