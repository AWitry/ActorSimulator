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
 * Logic associated with an actor.
 * Individual instances of this interface should not be reused across multiple
 * actors unless they are stateless.
 */
public interface ActorLogic
{
	/**
	 * Executed when created and re-executed upon arrival of new messages.
	 * While running, and not waiting via ActorLogicInterface.waitGetNextMessage()
	 * the hosting actor is considered active.
	 * There is no time-bound on when execute() should return, and new messages
	 * will be delivered even if it does not, but until it does or waits for new
	 * messages, the entire network must be considered running.
	 * Unless reused across multiple actors, the implementing object need not be
	 * thread safe as it is executed only by one thread.
	 * @param iface Actor control interface to the local actor
	 * to receive/send messages, query/manipulate the topology,
	 * or spawn new actors.
	 */
	public void execute(ActorLogicInterface iface);
}
