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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/**
 * Network graph
 */
public class Topology
{
	public static class Link
	{
		public final int	
				sourceNodeIndex,
				sinkNodeIndex,
				msDelay;
		public final boolean
				bidirectional;
		
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
			this.sourceNodeIndex = sourceNodeIndex;
			this.sinkNodeIndex = sinkNodeIndex;
			this.msDelay = msDelay;
			this.bidirectional = bidirectional;
		}
	};
	
	public int numNodes;
	public IntFunction<ActorLogic> logicFactory;
	public Link[] links;
	
	public void ImplementIn(Network n)
	{
		if (numNodes == 0)
			return;
		ActorControl[] newNodes = new ActorControl[numNodes];
		for (int i = 0; i < numNodes; i++)
			newNodes[i] = n.instantiate(logicFactory.apply(i));
		for (Topology.Link lnk : links)
		{
			ActorControl src = newNodes[lnk.sourceNodeIndex];
			ActorControl snk = newNodes[lnk.sinkNodeIndex];
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
	
	
	public static Topology FullMeshUniform(int numNodes, Supplier<ActorLogic> logic, boolean linkActorsToSelf)
	{
		Topology rs = new Topology();
		ArrayList<Link> links = new ArrayList<>();
		rs.numNodes = numNodes;
		rs.logicFactory = (i) -> logic.get();
		for (int i = 0; i+1 < numNodes; i++)
		{
			for (int j = i+1; j < numNodes; j++)
				links.add(new Link(i, j, true));
		}
		if (linkActorsToSelf)
			for (int i = 0; i < numNodes; i++)
				links.add(new Link(i,i,false));
		rs.links = (Link[]) links.toArray();
		return rs;
	}
	
	public static Topology FullMesh(int numNodes, IntFunction<ActorLogic> logic, boolean linkActorsToSelf)
	{
		Topology rs = new Topology();
		ArrayList<Link> links = new ArrayList<>();
		rs.numNodes = numNodes;
		rs.logicFactory = logic;
		for (int i = 0; i+1 < numNodes; i++)
		{
			for (int j = i+1; j < numNodes; j++)
				links.add(new Link(i, j, true));
		}
		if (linkActorsToSelf)
			for (int i = 0; i < numNodes; i++)
				links.add(new Link(i,i,false));
		rs.links = links.toArray(new Link[0]);
		return rs;
	}
	
}
