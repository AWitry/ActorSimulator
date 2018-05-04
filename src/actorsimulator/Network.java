/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package actorsimulator;

import java.util.ArrayList;

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
			System.out.println("Connection established: "+source+"->"+sink);
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
				System.out.println("Connection entangled: "+source+"<->"+sink);
			}
		}
		return link.ref;
	}

	private static boolean terminated = false;
	
	/**
	 * Retrieves the termination state.
	 * @return True if termination was detected, false otherwise
	 */
	public static boolean hasTerminated()
	{
		return terminated;
	}

	/**
	 * Starts the local simulation.
	 * Must be called exactly once.
	 * Dynamically instantiated actors during runtime are started automatically
	 */
	public static void start()
	{
		if (terminationChecker.isAlive())
			throw new IllegalAccessError("Trying to restart simulation");
				
		synchronized(ACTORS)
		{
			ACTORS.forEach((act) ->
			{
				act.start();
			});
		}
		terminationChecker.start();
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

			for (Actor act : ACTORS)
				if (act.isActive())
					s.actorsActive ++;
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
		
	}

	private static final Object terminalWaiter = new Object(), 
			checkWaiter = new Object();
	private static final Thread terminationChecker = new Thread(()->
	{
		while (true)
		{
			synchronized(checkWaiter)
			{
				try
				{
					checkWaiter.wait();	
				}
				catch (Exception ex)
				{}
			}

			Status s0 = detectStatus();
			System.out.println("s0="+s0);
			if (s0.isActive())
				continue;
			terminated = true;
			if (terminated)
			{
				synchronized (terminalWaiter)
				{
					terminalWaiter.notifyAll();
				}
				return;
			}
		}
	});
	
	
	/**
	 * Rechecks termination status.
	 * Called by actors when potentially terminal events have occurred
	 */
	public static void triggerTerminationCheck()
	{
		synchronized (checkWaiter)
		{
			checkWaiter.notify();
		}
	}
	
	/**
	 * Blocks the local thread until termination is detected
	 * @throws InterruptedException Throws if the local thread was externally
	 * interrupted. In this case termination might not have occurred.
	 */
	public static void awaitTermination() throws InterruptedException
	{
		synchronized(terminalWaiter)
		{
			terminalWaiter.wait();
		}
	}

	
}
