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
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Global registry for actors and links.
 * All active actors and links must be registered here, and started/stopped
 * from here.
 */
public class Network
{
	private final ArrayList<ActorControl> actors = new ArrayList<>();
	private final ArrayList<AbstractLink> links = new ArrayList<>();
	
	
	private static final AtomicInteger counter = new AtomicInteger();
	private final int myIndex = counter.incrementAndGet();
	
	private final int defaultDelayMS;
	
	public Network(int defaultLinkDelayMS)
	{
		defaultDelayMS = defaultLinkDelayMS;
	}
	
	@Override
	public String toString()
	{
		return "N"+myIndex;
	}
	
	
	private void log(boolean major, String msg)
	{
		Log.println(major ? Log.Significance.MajorNetworkEvent : Log.Significance.MinorNetworkEvent, this + ": "+msg);
	}
	
	private static ActorControl toControl(Actor actor)
	{
		if (actor instanceof ActorControl)
			return (ActorControl)actor;
		throw new IllegalArgumentException("Given actor "+actor
				+" does not implement ActorControl interface");
	}
	
	
	/**
	 * Instantiates a new actor for the given actor logic.
	 * @param logic Logic (instance) to use for the new actor
	 * @return Control to the newly instantiated actor
	 */
	public ActorControl instantiate(ActorLogic logic)
	{
		ActorControl ctrl = new ActorControlImpl(this,logic);
		synchronized(actors)
		{
			actors.add(ctrl);
		}
		if (isStarted())
			ctrl.start();
		return ctrl;
	}
	
	
	/**
	 * Creates or retrieves a link from one actor to another.
	 * The applied delay, if any, is determined internally
	 * @param source Source actor
	 * @param sink Destination actor
	 * @return New or existing actor link from source to sink
	 */
	public ActorLink link(Actor source, Actor sink)
	{
		return link(toControl(source),toControl(sink));
	}

	/**
	 * Creates or retrieves a link from one actor to another.
	 * @param source Source actor
	 * @param sink Destination actor
	 * @param msDelay Millisecond delay applied to all messages transferred
	 * along the resulting link. Ignored if a link already exists.
	 * @return New or existing actor link from source to sink
	 */
	public ActorLink link(Actor source, Actor sink, int msDelay)
	{
		return link(toControl(source),toControl(sink),msDelay);
	}
	
	private int getDelay(ActorControl source, ActorControl sink)
	{
		return defaultDelayMS;
	}
			
			
	
	
	ActorLink link(ActorControl source, ActorControl sink)
	{
		return link(source,sink,getDelay(source,sink));
	}
	


	
	private ActorLink link(ActorControl source, ActorControl sink, int delay)
	{
		if (source == null)
			throw new IllegalArgumentException("Network.link(): source is null");
		if (sink == null)
			throw new IllegalArgumentException("Network.link(): sink is null");
		if (sink == source)
			throw new IllegalArgumentException("Network.link(): source and sink are identical: "+source);
		if (sink.getNetwork() != this)
			throw new IllegalArgumentException("Network.link(): sink "+sink+" is not part of the local network");
		if (source.getNetwork() != this)
			throw new IllegalArgumentException("Network.link(): source "+source+" is not part of the local network");
		
		Ref<AbstractLink> link = new Ref<>();
		Ref<Boolean> isNew = new Ref<>(Boolean.FALSE);
		source.getOutgoingLinks().doLocked(() ->
		{
			ActorLink rs = source.findConnectionTo(sink);
			if (rs != null)
			{
				if (!(rs instanceof AbstractLink))
					throw new IllegalStateException("Found '"+rs+"', but link is not of type AbstractLink");
				link.ref = (AbstractLink)rs;
				return;
			}
			
			AbstractLink forward = delay > 0
					? new DelayedLink(source,sink,delay)
					: new InstantLink(source,sink);
			source.getOutgoingLinks().add(forward);
			link.ref = forward;
			isNew.ref = Boolean.TRUE;
			log(false, "Connection established: "+source+"->"+sink);
		});

		if (!isNew.ref)
			return link.ref;
		
		synchronized(links)
		{
			links.add(link.ref);
			
			ActorLink rev = sink.findConnectionTo(source);
			if (rev != null && rev instanceof AbstractLink)
			{
				((AbstractLink)link.ref).entangle((AbstractLink)rev);
				log(false, "Connection entangled: "+source+"<->"+sink);
			}
		}
		return link.ref;
	}

	
	/**
	 * Retrieves the termination state.
	 * @return True if termination was detected, false otherwise
	 */
	public boolean hasTerminated()
	{
		return terminated.get();
	}
	
	public synchronized boolean isStarted()
	{
		return checkThread.isAlive();
	}

	/**
	 * Starts the local simulation.
	 * Must be called exactly once.
	 * Dynamically instantiated actors during runtime are started automatically
	 */
	public synchronized void start()
	{
		if (checkThread.isAlive())
			throw new IllegalAccessError("Trying to restart simulation");
		
		terminated.reset();
		checkThread.start();
		log(true, "Starting simulation...");
		synchronized(actors)
		{
			actors.forEach((act) ->
			{
				act.start();
			});
		}
		checkThread.allowTermination();
	}

	private static class Status
	{
		public int	sent = 0,
					received = 0,
					active = 0;
		
		public boolean equals(Status other)
		{
			return other != null
					&& sent == other.sent 
					&& received == other.received 
					&& active == other.active;
		}
		
		public boolean isActive()
		{
			return sent != received || active != 0;
		}
		
		public String toString()
		{
			return "sent="+sent+",recv="+received+",active="+active;
		}
		
	};
	
	
	
	private Status detectStatus()
	{
		synchronized(actors)
		{
			Status s = new Status();

			for (ActorControl act : actors)
			{
				Actor.Status st = act.getStatus();
				s.received += st.receivedMessages;
				s.sent += st.sentMessages;
				if (st.isActive())
					s.active ++;
			}
			
			return s;
		}
	}
	
	/**
	 * Terminates simulation execution.
	 * Should be called exactly once at the end
	 */
	public synchronized void shutdown()
	{
		log(false, "Starting simulation shut down");
		checkThread.stop();
		synchronized(links)
		{
			links.forEach((lnk) ->
			{
				lnk.shutdown();
			});
			links.clear();
		}
		
		synchronized(actors)
		{
			actors.forEach((act) ->
			{
				act.shutdown();
			});
			actors.clear();
		}
		log(true, "Simulation shut down");
	}
	
	private class TerminationState
	{
		private volatile boolean isSet = false;
		
		
		public boolean get()
		{
			return isSet;
		}
		
		
		public void reset()
		{
			if (isSet)
				log(false, "Termination state reset");
			isSet = false;

		}
		
		public synchronized void set()
		{
			if (isSet)
				return;
			isSet = true;
			log(false, "Termination state set");
	
			notifyAll();
		}
		
		
		public synchronized void awaitTermination() throws InterruptedException
		{
			if (isSet)
				return;
			wait();
		}
	}
	
	
	
	private final TerminationState terminated = new TerminationState();
	
	private class TerminationCheckThread implements Runnable
	{
		private final CyclicBarrier startCheck = new CyclicBarrier(2);
		private Thread thread;
		private volatile boolean doQuit = false, terminationAllowed = false;
		private final Object threadControl = new Object();
		
		private final Semaphore sem = new Semaphore(0);
		
		@Override
		public void run()
		{
			log(false, "TerminationChecker: Thread started");
			try
			{
				startCheck.await();
			} catch (InterruptedException | BrokenBarrierException ex)
			{
				Log.println(Log.Significance.Error, Network.this+ ": TerminationChecker: "+ex);
			}

			try
			{
				while (!doQuit)
				{
					//log("TerminationChecker: Waiting");
					try
					{
						sem.acquire();
						sem.drainPermits();
					}
					catch (InterruptedException ex)
					{
						if (!doQuit)
							Logger.getLogger(Network.class.getName()).log(Level.SEVERE, null, ex);
						return;
					}
					if (!terminationAllowed)
						continue;

					Status s0 = detectStatus();
					log(false, "TerminationChecker: s0="+s0);
					if (s0.isActive())
						continue;
					Status s1 = detectStatus();
					if (!s1.equals(s0))
						continue;
					
					terminated.set();
					log(false, "TerminationChecker: Exit");
					return;
				}
			}
			catch (Exception | Error ex)
			{
				Log.println(Log.Significance.Error, Network.this+ ": TerminationChecker: "+ex);

			}

		}
		
		public void allowTermination()
		{
			terminationAllowed = true;
			wake();
		}
	
		public void start()
		{
			synchronized(threadControl)
			{
				if (isAlive())
					stop();
				terminationAllowed = false;
				doQuit = false;
				sem.drainPermits();
				thread = new Thread(this);
				thread.start();
				log(false, "TerminationCheck: Started. Waiting...");
			}
			try
			{
				startCheck.await();
			} catch (InterruptedException | BrokenBarrierException ex)
			{
				Logger.getLogger(Network.class.getName()).log(Level.SEVERE, null, ex);
			}
			log(false, "TerminationCheck: Thread responded.");
		
		}
		
		public void stop()
		{
			synchronized (threadControl)
			{
				if (!isAlive())
					return;
				doQuit = true;
				thread.interrupt();
				try
				{
					thread.join();
				} catch (InterruptedException ex)
				{
					Logger.getLogger(Network.class.getName()).log(Level.SEVERE, null, ex);
				}
				log(false, "TerminationCheck: Closed down thread");
			}
		}

		public void wake()
		{
			sem.release();
		}

		public boolean isAlive()
		{
			synchronized (threadControl)
			{
				return thread != null && thread.isAlive();
			}
		}
	}
	
	private final TerminationCheckThread checkThread = new TerminationCheckThread();

	
	
	
	
	/**
	 * Rechecks termination status.
	 * May detect termination and wake threads awaiting termination.
	 * Called by actors when potentially terminal events have occurred
	 */
	public void triggerTerminationCheck()
	{
		checkThread.wake();
	}
	
	/**
	 * Blocks the local thread until termination is detected
	 * @throws InterruptedException Throws if the local thread was externally
	 * interrupted. In this case termination might not have occurred.
	 */
	public void awaitTermination() throws InterruptedException
	{
		terminated.awaitTermination();
	}

	
}
