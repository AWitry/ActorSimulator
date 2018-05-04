/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
	public void shutdown()
	{
		mapper.quit = true;	
		pending.quit();
		wake();
		try
		{
			thread.join();
		} catch (InterruptedException ex)
		{
			Logger.getLogger(ActorControlImpl.class.getName()).log(Level.SEVERE, null, ex);
		}
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
	public void start()
	{
		thread.start();
	}


	
	private class Mapper implements Runnable
	{

		public volatile boolean quit = false;
		private final ActorLogic logic;
		public volatile boolean isActive = false;
		
		private Mapper(ActorLogic logic)
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
		
		
	}
	
	
	private final Thread thread;
	private final Mapper mapper;
	
	public ActorControlImpl(ActorLogic logic)
	{
		mapper = new Mapper(logic);
		thread = new Thread(mapper);
	}
	
	public void wake()
	{
		synchronized(mapper)
		{
			mapper.notify();
		}
	}
	
	
	
	@Override
	public synchronized boolean isActive()
	{
		return pending.isNotEmptyOrNotWaiting() && (!pending.isEmpty() || mapper.isActive);
	}
}
