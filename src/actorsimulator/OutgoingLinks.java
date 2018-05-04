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
