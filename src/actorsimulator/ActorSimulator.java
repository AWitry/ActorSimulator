/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package actorsimulator;


/**
 * Static main class to test actor programs
*/
public class ActorSimulator
{

	
	
	
	/**
	 * @param args the command line arguments
	 * @throws java.lang.InterruptedException
	 */
	public static void main(String[] args) throws InterruptedException
	{
//		Network.instantiate(new Ping());
		
		Actor[] actors = new Actor[10];
		for (int i = 0; i < actors.length; i++)
			actors[i] = Network.instantiate(new RingLogic(i==0));
		for (int i = 0; i < actors.length; i++)
			Network.link(actors[i], actors[(i+1)%actors.length],300);

		Network.start();
	
		Network.awaitTermination();
		System.out.println("Termination detected. Shutting down...");
		Network.shutdown();
	}
	
	private static class PingPong
	{
		public int numPongs = 0;	
	}

	private static class Ping implements ActorLogic
	{
		ActorLink pong = null;
		public Ping()
		{
		}

		@Override
		public void execute(ActorLogicInterface iface)
		{
			if (pong == null)
			{
				System.out.println("Creating pong");
				pong = iface.instantiate(new Pong());
				pong.sendMessage(new PingPong());
			}
			Message env = iface.tryGetNextMessage();
			if (env != null)
			{
				PingPong p = (PingPong)env.getContent();
				p.numPongs ++;
				if (p.numPongs > 6)
				{
					System.out.println("Stopping");
					return;
				}
				System.out.println(iface.getLocalActor()+ ": Pinging "+p.numPongs);
				env.getLinkToSender().sendMessage(p);
			}
		}
	}

	private static class Pong implements ActorLogic
	{

		public Pong()
		{
		}

		@Override
		public void execute(ActorLogicInterface iface)
		{
			Message env = iface.tryGetNextMessage();
			if (env != null)
			{
				PingPong p = (PingPong)env.getContent();
				p.numPongs ++;
				System.out.println(iface.getLocalActor()+ ": Ponging "+p.numPongs);
				
				ActorLink lnk = env.getLinkToSender();
				if (lnk == null)
					lnk = iface.connectTo(env.getSender());
				lnk.sendMessage(p);
			}
		}
	}

	private static class RingLogic implements ActorLogic
	{
		boolean amFirst;
		int numHandled = 0;
		
		public RingLogic(boolean amFirst)
		{
			this.amFirst = amFirst;
		}

		@Override
		public void execute(ActorLogicInterface iface)
		{
			Message env;
			while ((env = iface.tryGetNextMessage())!=null)
			{
				numHandled++;
				System.out.println(iface.getLocalActor()+": Handled->"+numHandled);
				if (numHandled > 2)
				{
					System.out.println(iface.getLocalActor()+": Stopping");
					return;
				}
				
				Message env2 = env;
				iface.visitOutgoing((lnk) ->
				{
					if (lnk != env2.getSender())
					{
						System.out.println(iface.getLocalActor()+": Forwarding to "+lnk.getDestinationActor());
						lnk.sendMessage(env2.getContent());
					}
				});
			}
			if (amFirst && numHandled == 0)
			{
				ActorLink lnk = iface.getAnyOutgoing();
				if (lnk != null)
				{
					System.out.println(iface.getLocalActor()+": Sending pivot to "+lnk.getDestinationActor());
					lnk.sendMessage(null);
					numHandled++;
				}
			}
		}
	}
	
	private static class BlockingRingLogic implements ActorLogic
	{
		boolean amFirst;
		int numHandled = 0;
		
		public BlockingRingLogic(boolean amFirst)
		{
			this.amFirst = amFirst;
		}

		@Override
		public void execute(ActorLogicInterface iface)
		{
			if (amFirst)
			{
				ActorLink lnk = iface.getAnyOutgoing();
				if (lnk != null)
				{
					System.out.println(iface.getLocalActor()+": Sending pivot to "+lnk.getDestinationActor());
					lnk.sendMessage(null);
					numHandled++;
				}
			}	
			
			while (true)
			{
				Message env = iface.waitGetNextMessage();
				numHandled++;
				System.out.println(iface.getLocalActor()+": Handled->"+numHandled);
				if (numHandled > 2)
				{
					System.out.println(iface.getLocalActor()+": Stopping");
					continue;
				}
				
				Message env2 = env;
				iface.visitOutgoing((lnk) ->
				{
					if (lnk != env2.getSender())
					{
						System.out.println(iface.getLocalActor()+": Forwarding to "+lnk.getDestinationActor());
						lnk.sendMessage(env2.getContent());
					}
				});
			}
		}
	}
	
}
