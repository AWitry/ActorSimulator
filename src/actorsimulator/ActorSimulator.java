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
		Network network = new Network(0);		//instant delivery on default links
		//Network network = new Network(300);	//300ms delay on default links

		//network.instantiate(new Ping());	//dynamically establishes its network
		
		//construct 10 actor ring network:
		NetworkBlueprint
				.CreateRing(10,(i) -> new BlockingRingLogic(i==0), false)
				.ImplementIn(network);
		//alternatively manually:
//		Actor[] actors = new Actor[10];
//		for (int i = 0; i < actors.length; i++)
//			actors[i] = network.instantiate(new BlockingRingLogic(i==0));	//can also use RingLogic(i==0) here
//		for (int i = 0; i < actors.length; i++)
//			network.link(actors[i], actors[(i+1)%actors.length]);	//create uni-directional link

		network.start();	//start threads and run the system
		network.awaitTermination();	//sleep until the network has stopped
		Log.println(Log.Significance.MajorNetworkEvent, "Termination detected. Shutting down "+network+"...");
		network.shutdown();	//join all threads
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
				iface.log("Creating pong");
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
					iface.log("Stopping");
					return;
				}
				iface.log("Pinging "+p.numPongs);
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
				iface.log("Ponging "+p.numPongs);
				
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
				iface.log("Handled->"+numHandled);
				if (numHandled > 2)
				{
					iface.log("Stopping");
					return;
				}
				
				Message env2 = env;
				iface.visitOutgoing((lnk) ->
				{
					if (lnk != env2.getSender())
					{
						iface.log("Forwarding to "+lnk.getDestinationActor());
						lnk.sendMessage(env2.getContent());
					}
				});
			}
			if (amFirst && numHandled == 0)
			{
				ActorLink lnk = iface.getAnyOutgoing();
				if (lnk != null)
				{
					iface.log("Sending pivot to "+lnk.getDestinationActor());
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
					iface.log("Sending pivot to "+lnk.getDestinationActor());
					lnk.sendMessage(null);
					numHandled++;
				}
			}	
			
			while (true)
			{
				Message env = iface.waitGetNextMessage();
				numHandled++;
				iface.log("Handled->"+numHandled);
				if (numHandled > 2)
				{
					iface.log("Stopping");
					continue;
				}
				
				Message env2 = env;
				iface.visitOutgoing((lnk) ->
				{
					if (lnk != env2.getSender())
					{
						iface.log("Forwarding to "+lnk.getDestinationActor());
						lnk.sendMessage(env2.getContent());
					}
				});
			}
		}
	}
	
}
