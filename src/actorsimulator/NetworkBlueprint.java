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
import java.util.List;
import java.util.function.IntFunction;

/**
 * Actor network descriptor.
 * Defines the number of actors and their logics as well as which actors
 * are connected.
 */
public class NetworkBlueprint
{
	/**
	 * Unidirectional link between actors
	 */
	public static class Link
	{
		/**
		 * Index of the link source actor, starting from 0
		 */
		public final int	sourceActorIndex;
		/**
		 * Index of the link source actor, starting from 1.
		 * May be equal to sourceActorIndex
		 */
		public final int	sinkActorIndex;
		/**
		 * Message delay in milliseconds.
		 * 0 indicates instant delivery.
		 * Negative values imply network default delay.
		 */
		public final int	msDelay;
		
		/**
		 * Set true to create a link in both directions.
		 */
		public final boolean
				bidirectional;
		
		/**
		 * Constructs a new unidirectional link with network default delay
		 * @param sourceNodeIndex
		 * @param sinkNodeIndex 
		 */
		public Link(int sourceNodeIndex, int sinkNodeIndex)
		{
			this(sourceNodeIndex,sinkNodeIndex,-1,false);
		}
		public Link(int sourceNodeIndex, int sinkNodeIndex, boolean bidirectional)
		{
			this(sourceNodeIndex,sinkNodeIndex,-1,bidirectional);
		}
		public Link(int sourceNodeIndex, int sinkNodeIndex, int msDelay, boolean bidirectional)
		{
			this.sourceActorIndex = sourceNodeIndex;
			this.sinkActorIndex = sinkNodeIndex;
			this.msDelay = msDelay;
			this.bidirectional = bidirectional;
		}
	};
	
	/**
	 * Number of actors in the local topology
	 */
	public final int numActors;
	/**
	 * Factory to construct new actor logics with.
	 * The passed parameter is set to the index of the respective actor
	 * in [0,numActors-1)
	 */
	public final IntFunction<ActorLogic> logicFactory;
	/**
	 * Links between actors
	 */
	public final Link[] links;
	
	
	/**
	 * Constructs a new topology with the given configuration
	 * @param numNodes Number of nodes in the local topology
	 * @param logicFactory Factory for new actor logics. Must not be null
	 * if numNodes is greater than 0.
	 * @param links Array of links. May be empty or null
	 */
	public NetworkBlueprint(int numNodes, IntFunction<ActorLogic> logicFactory, Link[] links)
	{
		if (numNodes < 0)
			throw new IllegalArgumentException("numNodes is negative");
		if (logicFactory == null && numNodes > 0)
			throw new IllegalArgumentException("logicFactory is null");

		this.numActors = numNodes;	
		this.logicFactory = logicFactory;
		this.links = links;
	}
	
	public NetworkBlueprint(int numNodes, IntFunction<ActorLogic> logicFactory, List<Link> links)
	{
		this(numNodes,logicFactory,links.toArray(new Link[0]));
	}
	
	/**
	 * Implements the local topology in the specified network
	 * @param n Network to implement the topology in
	 */
	public void implementIn(Network n)
	{
		if (numActors == 0)
			return;
		ActorControl[] newNodes = new ActorControl[numActors];
		for (int i = 0; i < numActors; i++)
			newNodes[i] = n.instantiate(logicFactory.apply(i));
		if (links != null)
			for (NetworkBlueprint.Link lnk : links)
			{
				ActorControl src = newNodes[lnk.sourceActorIndex];
				ActorControl snk = newNodes[lnk.sinkActorIndex];
				if (lnk.msDelay >= 0)
					n.link(src, snk, lnk.msDelay);
				else
					n.link(src, snk);
				if (lnk.bidirectional)
				{
					if (lnk.msDelay >= 0)
						n.link(snk, src, lnk.msDelay);
					else
						n.link(snk, src);
				}
			}
	}
	
	/**
	 * Creates a new ring topology.
	 * The last actor is linked back to the first.
	 * @param numActors Number of actors in the ring
	 * @param logic Individual actor logic factory.
	 * The passed parameter maps to the respective actor index in [0,numNodes).
	 * @param bidirectional Set true to create reverse links
	 * @return New blueprint according to the specification
	 */
	public static NetworkBlueprint createRing(int numActors, IntFunction<ActorLogic> logic, boolean bidirectional)
	{
		ArrayList<Link> links = new ArrayList<>();
		for (int i = 0; i < numActors; i++)
			links.add(new Link(i,(i+1)%numActors,bidirectional));
		
		return new NetworkBlueprint(numActors, logic, links);
	
	}
	/**
	 * Constructs a full mesh topology.
	 * Each actor is connected to every other actor.
	 * @param numActors Number of actors in the network
	 * @param logic Individual actor logic factory.
	 * The passed parameter maps to the respective actor index in [0,numNodes).
	 * @param linkActorsToSelf Set true to connect actors to themselves
	 * @return Created blueprint
	 */
	public static NetworkBlueprint createFullMesh(int numActors, IntFunction<ActorLogic> logic, boolean linkActorsToSelf)
	{
		ArrayList<Link> links = new ArrayList<>();
		for (int i = 0; i+1 < numActors; i++)
		{
			for (int j = i+1; j < numActors; j++)
				links.add(new Link(i, j, true));
		}
		if (linkActorsToSelf)
			for (int i = 0; i < numActors; i++)
				links.add(new Link(i,i,false));
		return new NetworkBlueprint(numActors,logic,links);
	}
	
}
