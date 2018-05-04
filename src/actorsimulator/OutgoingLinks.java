/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package actorsimulator;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author IronFox
 */
public class OutgoingLinks
{
	
	private static final Random rand = new Random();
	private final Lock outgoingLock = new ReentrantLock();
	private final ArrayList<ActorLink> outgoing = new ArrayList<>();

	public ActorLink findConnectionTo(Actor remote)
	{
		Ref<ActorLink> link = new Ref<>();
		doLocked(() ->
		{
			outgoing.stream().filter((lnk) -> (lnk.getDestinationActor() == remote)).forEachOrdered((lnk) ->
			{
				link.ref = lnk;
			});
		});
		return link.ref;
	}
	
	public void visitAll(Consumer<? super ActorLink> visitor)
	{
		doLocked(() -> outgoing.forEach(visitor));
	}
	

	public void		doLocked(Runnable action)
	{
		try
		{
			int ms = rand.nextInt(500)+500;
			if (outgoingLock.tryLock(ms, TimeUnit.MILLISECONDS)) {
				try {
					action.run();
				} finally {
					outgoingLock.unlock();
				}
			}
			else
				throw new DeadlockError("Failed to acquire lock in "+ms
						+" ms. Deadlock assumed");
		}
		catch (InterruptedException ex)
		{
			/*
			documentation indicates this never actually happens, so let's
			treat it like lock failure in case it ever does
			*/
			throw new DeadlockError(ex);
		}
	}
	
	public void add(ActorLink link)
	{
		doLocked(() -> outgoing.add(link));
	}
	
	public ActorLink getAny()
	{
		Ref<ActorLink> link = new Ref<>();
		doLocked(() ->
		{
			if (!outgoing.isEmpty())
				link.ref = outgoing.get(0);
		});
		return link.ref;
	}
	


}
