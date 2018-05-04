/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package actorsimulator;

import java.util.ArrayList;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Global registry for actors and links.
 * All active actors and links must be registered here, and started/stopped
 * from here.
 */
public class Network
{
	private final static ArrayList<ActorControl> ACTORS = new ArrayList<>();
	private final static ArrayList<AbstractLink> LINKS = new ArrayList<>();
	
	
	/**
	 * Registers a new actor in the local network.
	 * @param actor Actor to register
	 * @return actor
	 */
	public static ActorControl register(ActorControl actor)
	{
		synchronized(ACTORS)
		{
			ACTORS.add(actor);
		}
		return actor;
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
	public static ActorControl instantiate(ActorLogic logic)
	{
		return register(new ActorControlImpl(logic));
	}
	
	
	/**
	 * Creates or retrieves a link from one actor to another.
	 * The applied delay, if any, is determined internally
	 * @param source Source actor
	 * @param sink Destination actor
	 * @return New or existing actor link from source to sink
	 */
	public static ActorLink link(Actor source, Actor sink)
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
	public static ActorLink link(Actor source, Actor sink, int msDelay)
	{
		return link(toControl(source),toControl(sink),msDelay);
	}
	
	private static int getDelay(ActorControl source, ActorControl sink)
	{
		return 300;
	}
			
			
	
	
	static ActorLink link(ActorControl source, ActorControl sink)
	{
		return link(source,sink,getDelay(source,sink));
	}
	


	
	private static ActorLink link(ActorControl source, ActorControl sink, int delay)
	{
		if (source == null || sink == null || source==sink)
			throw new IllegalArgumentException("link(): Source and/or sink handles are invalid");
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
			Log.println(Log.Verbosity.MinorNetworkEvent, "Connection established: "+source+"->"+sink);
		});

		if (!isNew.ref)
			return link.ref;
		
		synchronized(LINKS)
		{
			LINKS.add(link.ref);
			
			ActorLink rev = sink.findConnectionTo(source);
			if (rev != null && rev instanceof AbstractLink)
			{
				((AbstractLink)link.ref).entangle((AbstractLink)rev);
				Log.println(Log.Verbosity.MinorNetworkEvent, "Connection entangled: "+source+"<->"+sink);
			}
		}
		return link.ref;
	}

	
	/**
	 * Retrieves the termination state.
	 * @return True if termination was detected, false otherwise
	 */
	public static boolean hasTerminated()
	{
		return terminated.get();
	}

	/**
	 * Starts the local simulation.
	 * Must be called exactly once.
	 * Dynamically instantiated actors during runtime are started automatically
	 */
	public static void start()
	{
		if (checkThread.isAlive())
			throw new IllegalAccessError("Trying to restart simulation");
		
		terminated.reset();
		checkThread.start();
		Log.println(Log.Verbosity.MajorNetworkEvent, "Starting simulation...");
		synchronized(ACTORS)
		{
			ACTORS.forEach((act) ->
			{
				act.start();
			});
		}
		checkThread.allowTermination();
	}


	private static class Status
	{
		public int	actorsActive = 0, linksActive = 0;
		
		public boolean equals(Status other)
		{
			return other != null
					&& actorsActive == other.actorsActive
					&& linksActive == other.linksActive
					;
			
		}
		
		public boolean isActive()
		{
			return actorsActive != 0 || linksActive != 0;
		}
		
		@Override
		public String toString()
		{
			return "actors:"+actorsActive+",links:"+linksActive;
		}
		
	};
	
	private static Status detectStatus()
	{
		synchronized(ACTORS)
		{
			Status s = new Status();

			for (ActorControl act : ACTORS)
			{
				Actor.Status as = act.getStatus();
				
				if (as == Actor.Status.Active || as == Actor.Status.MessagesPending)
					s.actorsActive ++;
			}
			for (ActorLink lnk: LINKS)
				if (!lnk.isIdle())
					s.linksActive ++;
			
			return s;
		}
	}
	
	/**
	 * Terminates simulation execution.
	 * Should be called exactly once at the end
	 */
	public static void shutdown()
	{
		Log.println(Log.Verbosity.MinorNetworkEvent, "Starting simulation shut down");
		checkThread.stop();
		synchronized(LINKS)
		{
			LINKS.forEach((lnk) ->
			{
				lnk.shutdown();
			});
			LINKS.clear();
		}
		
		synchronized(ACTORS)
		{
			ACTORS.forEach((act) ->
			{
				act.shutdown();
			});
			ACTORS.clear();
		}
		Log.println(Log.Verbosity.MajorNetworkEvent, "Simulation shut down");
		
	}
	
	private static class TerminationCapsule
	{
		private volatile boolean isSet = false;
		
		
		public boolean get()
		{
			return isSet;
		}
		
		
		public void reset()
		{
			if (isSet)
				Log.println(Log.Verbosity.MinorNetworkEvent, "Termination state reset");
			isSet = false;

		}
		
		public synchronized void set()
		{
			if (isSet)
				return;
			isSet = true;
			Log.println(Log.Verbosity.MinorNetworkEvent, "Termination state set");
	
			notifyAll();
		}
		
		
		public synchronized void awaitTermination() throws InterruptedException
		{
			if (isSet)
				return;
			wait();
		}
	}
	
	
	
	private static final TerminationCapsule terminated = new TerminationCapsule();
	
	private static class TerminationCheckThread implements Runnable
	{
		private final CyclicBarrier startCheck = new CyclicBarrier(2);
		private Thread thread;
		private volatile boolean doQuit = false, terminationAllowed = false;
		private final Object threadControl = new Object();
		
		@Override
		public synchronized void run()
		{
			Log.println(Log.Verbosity.MinorNetworkEvent, "TerminationChecker: Thread started");
			try
			{
				startCheck.await();
			} catch (InterruptedException | BrokenBarrierException ex)
			{
				Logger.getLogger(Network.class.getName()).log(Level.SEVERE, null, ex);
			}

			try
			{
				while (!doQuit)
				{
					//log("TerminationChecker: Waiting");
					try
					{
						wait();	
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
					Log.println(Log.Verbosity.MinorNetworkEvent, "TerminationChecker: s0="+s0);
					if (s0.isActive())
						continue;
					terminated.set();
					Log.println(Log.Verbosity.MinorNetworkEvent, "TerminationChecker: Exit");
					return;
				}
			}
			catch (Exception | Error ex)
			{
				Log.println(Log.Verbosity.Error, "TerminationChecker: "+ex);

			}

		}
		
		public synchronized void allowTermination()
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
				thread = new Thread(this);
				thread.start();
				Log.println(Log.Verbosity.MinorNetworkEvent, "TerminationCheck: Started. Waiting...");
			}
			try
			{
				startCheck.await();
			} catch (InterruptedException | BrokenBarrierException ex)
			{
				Logger.getLogger(Network.class.getName()).log(Level.SEVERE, null, ex);
			}
			Log.println(Log.Verbosity.MinorNetworkEvent, "TerminationCheck: Thread responded.");
		
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
				Log.println(Log.Verbosity.MinorNetworkEvent, "TerminationCheck: Closed down thread");
			}
		}

		public synchronized void wake()
		{
			if (!isAlive())
				return;
			notify();
		}

		public boolean isAlive()
		{
			synchronized (threadControl)
			{
				return thread != null && thread.isAlive();
			}
		}
	}
	
	private static final TerminationCheckThread checkThread = new TerminationCheckThread();

	
	
	
	
	/**
	 * Rechecks termination status.
	 * Called by actors when potentially terminal events have occurred
	 */
	public static void triggerTerminationCheck()
	{
		checkThread.wake();
	}
	
	/**
	 * Blocks the local thread until termination is detected
	 * @throws InterruptedException Throws if the local thread was externally
	 * interrupted. In this case termination might not have occurred.
	 */
	public static void awaitTermination() throws InterruptedException
	{
		terminated.awaitTermination();
	}

	
}
