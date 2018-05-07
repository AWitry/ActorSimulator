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

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ActorControl implementation
 */
class ActorControlImpl implements ActorControl
{
	private final BlockingQueue<Message> pending = new BlockingQueue<>();
	private final AtomicInteger	messagesSent = new AtomicInteger();
	private final OutgoingLinks outgoingLinks = new OutgoingLinks();
	private final Network network;

	@Override
	public ActorLink connectTo(Actor remote)
	{
		if (network != remote.getNetwork())
			throw new IllegalArgumentException("Trying to connect actors of different networks: "+this+"->"+remote);
		return network.link(this, remote);
	}
	
	@Override
	public Actor getLocalActor()
	{
		return this;
	}

	@Override
	public OutgoingLinks getOutgoingLinks()
	{
		return outgoingLinks;
	}

	
	
	
	@Override
	public ActorLink findConnectionTo(Actor remote)
	{
		return outgoingLinks.findConnectionTo(remote);
	}
	
	
	@Override
	public void visitOutgoing(Consumer<? super ActorLink> visitor)
	{
		outgoingLinks.visitAll(visitor);
	}
	
	@Override
	public 	ActorLink	getAnyOutgoing()
	{
		return outgoingLinks.getAny();
	}

	
	@Override
	public void broadcast(Object msg)
	{
		visitOutgoing((lnk) ->
		{
			lnk.sendMessage(msg);
		});
	}

	@Override
	public void signalMessageSent()
	{
		messagesSent.incrementAndGet();
	}
	
	private static final AtomicInteger counter = new AtomicInteger();
	private final int myIndex = counter.incrementAndGet();
	
	@Override
	public String toString()
	{
		return network+":A"+myIndex;
	}

	
	
	
	
	@Override
	public Message tryGetNextMessage()
	{
		return pending.tryTake();
	}

	@Override
	public Message waitGetNextMessage()
	{
		return pending.take(network);
	}

	
	@Override
	public ActorLink instantiate(ActorLogic logic)
	{
		ActorControl ac = network.instantiate(logic);
		ActorLink rs = network.link(this,ac);
		ac.start();
		return rs;
	}


	@Override
	public void shutdown()
	{
		wrapper.quit = true;
		pending.quit();
		wake();
		thread.interrupt();
		try
		{
			thread.join();
		} catch (InterruptedException ex)
		{
			Logger.getLogger(ActorControlImpl.class.getName()).log(Level.SEVERE, null, ex);
		}
		Log.println(Log.Significance.MinorNetworkEvent, this+": Shut down");
		
	}

	
	
	
	@Override
	public synchronized void receive(Message ev)
	{
		boolean wasEmpty = pending.isEmpty();
		pending.add(ev);
		if (wasEmpty)
		{
			/*
			No uniqueness guarantee. multiple threads might detect wasEmpty 
			as true. so wake multiple times, w/e.
			Most of the time it'll work
			*/
			wake();	
		}
	}
	
	@Override
	public synchronized void start()
	{
		thread = new Thread(wrapper);
		thread.start();
	}

	@Override
	public void log(String msg)
	{
		Log.println( Log.Significance.ActorMessage, this+": "+msg);
	}

	@Override
	public Network getNetwork()
	{
		return network;
	}

	private class LogicWrapper implements Runnable
	{

		public volatile boolean quit = false;
		private final ActorLogic logic;
		public volatile boolean isActive = false;
		
		private final Semaphore sem = new Semaphore(0);
		
		private LogicWrapper(ActorLogic logic)
		{
			this.logic = logic;
		}
		
		@Override
		public void run()
		{
			while (!quit)
			{
				isActive = true;
				try
				{
					logic.execute(ActorControlImpl.this);
				}
				catch (BlockingQueue.Quit q)
				{
					quit = true;
					//all good
				}
				catch (Exception ex)
				{
					//what are we supposed to do now...?
					Logger.getLogger(ActorControlImpl.class.getName()).log(Level.SEVERE, null, ex);
				}
				isActive = false;
				
				network.triggerTerminationCheck();
				
				while (pending.isEmpty() && !quit)
				{
					try
					{
						sem.acquire();
						sem.drainPermits();
					}
					catch (InterruptedException ex)
					{
						if (!quit)
							Log.println(Log.Significance.Error, this+": "+ ex);
					}
				}
			}
		}
		
		@Override
		public String toString()
		{
			return ActorControlImpl.this.toString();
		}

		private void wake()
		{
			sem.release();
		}
		
	}
	
	
	private Thread thread;
	private final LogicWrapper wrapper;
	
	public ActorControlImpl(Network network, ActorLogic logic)
	{
		this.network = network;
		wrapper = new LogicWrapper(logic);
	}
	
	public void wake()
	{
		wrapper.wake();
	}
	

	@Override
	public synchronized Status getStatus()
	{
		/*
		Messages count and thread status may be out of sync.
		Changes are still consistent for termination checks because:
			pending.countDispatchesMessages() increments => thread is active
				or has finished processing message. can see:
					thread active, message count incremented
					thread inactive, message count incremented
					
			messagesSent increments => thread is active
				or has become passive AFTER sending
			neither can be detected incremented before thread is considered
				active
			both message receiving and getStatus() are synchronized:
				no messages can be received between getThreadStatus()
				and countDispatchesMessages()
		*/
		return new Status(getThreadStatus(), 
				messagesSent.get(), 
				pending.countDispatchesMessages());
	}
	
	public synchronized ThreadStatus getThreadStatus()
	{
		if (!pending.isNotEmptyOrNotWaiting())
			return ThreadStatus.PASSIVE_BLOCKED;
		if (wrapper.isActive)
			return ThreadStatus.ACTIVE;
		if (!pending.isEmpty())
			return ThreadStatus.MESSAGES_PENDING;
		return ThreadStatus.PASSIVE_RETURNED;
	}
}
