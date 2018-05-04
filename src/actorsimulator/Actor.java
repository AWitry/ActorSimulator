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

import java.util.function.Consumer;

/**
 * Primary actor identification and abstraction
 * @author IronFox
 */
public interface Actor
{
	/**
	 * Visits all registered outgoing actor links.
	 * Must be thread safe.
	 * @param visitor Visitor to receive all outgoing links
	 */
	void		visitOutgoing(Consumer<? super ActorLink> visitor);
	
	
	/**
	 * Potential actor states
	 */
	public enum Status
	{
		PassiveBlocked,
		PassiveReturned,
		MessagesPending,
		Active
	}
	
	/**
	 * Determines the current actor state.
	 * @return Actor state
	 */
	Status		getStatus();

	/**
	 * Fetches the owning network.
	 * @return Network this actor is part of
	 */
	Network		getNetwork();
}
