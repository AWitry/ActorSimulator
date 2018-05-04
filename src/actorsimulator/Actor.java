/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package actorsimulator;

import java.util.function.Consumer;

/**
 * Primary actor identification and abstraction
 * @author IronFox
 */
public interface Actor
{
	/**
	 * Visits all registered outgoing actor links.
	 * Must be thread safe.
	 * @param visitor Visitor to receive all outgoing links
	 */
	void		visitOutgoing(Consumer<? super ActorLink> visitor);
	
	
	enum Status
	{
		PassiveBlocked,
		PassiveReturned,
		MessagesPending,
		Active
	}
	Status		getStatus();

}
