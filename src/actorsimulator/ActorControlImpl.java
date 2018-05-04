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
	private final OutgoingLinks outgoingLinks = new OutgoingLinks();


	@Override
	public ActorLink connectTo(Actor remote)
	{
		return Network.link(this, remote);
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


	private static final AtomicInteger counter = new AtomicInteger();
	private final int myIndex = counter.incrementAndGet();
	
	@Override
	public String toString()
	{
		return "Actor "+myIndex;
	}

	
	
	
	
	@Override
	public Message tryGetNextMessage()
	{
		return pending.tryTake();
	}

	@Override
	public Message waitGetNextMessage()
	{
		return pending.take();
	}

	
	@Override
	public ActorLink instantiate(ActorLogic logic)
	{
		ActorControl ac = Network.instantiate(logic);
		ActorLink rs = Network.link(this,ac);
		ac.start();
		return rs;
	}


	@Override
	public synchronized void shutdown()
	{
		wrapper.quit = true;	
		pending.quit();
		wake();
		try
		{
			thread.join();
		} catch (InterruptedException ex)
		{
			Logger.getLogger(ActorControlImpl.class.getName()).log(Level.SEVERE, null, ex);
		}
		Log.println(Log.Verbosity.MinorNetworkEvent, this+": Shut down");
		
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
		Log.println( Log.Verbosity.ActorMessage, this+": "+msg);
	}

	private class LogicWrapper implements Runnable
	{

		public volatile boolean quit = false;
		private final ActorLogic logic;
		public volatile boolean isActive = false;
		
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
				
				Network.triggerTerminationCheck();
				
				while (pending.isEmpty() && !quit)
				{
					synchronized (this)
					{
						try
						{
							wait();
						}
						catch (InterruptedException ex)
						{
							//we don't do this
						}
					}
				}
			}
		}
		
		@Override
		public String toString()
		{
			return ActorControlImpl.this.toString();
		}
		
	}
	
	
	private Thread thread;
	private final LogicWrapper wrapper;
	
	public ActorControlImpl(ActorLogic logic)
	{
		wrapper = new LogicWrapper(logic);
	}
	
	public void wake()
	{
		synchronized(wrapper)
		{
			wrapper.notify();
		}
	}
	
	
	
	@Override
	public synchronized Status getStatus()
	{
		if (!pending.isNotEmptyOrNotWaiting())
			return Status.PassiveBlocked;
		if (wrapper.isActive)
			return Status.Active;
		if (!pending.isEmpty())
			return Status.MessagesPending;
		return Status.PassiveReturned;
	}
}
