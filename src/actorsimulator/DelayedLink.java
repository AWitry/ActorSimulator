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

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thread-based delaying actor link.
 * Messages sent along this link are delayed by a configurable time-delta before
 * delivery.
 */
public class DelayedLink extends AbstractLink implements Runnable
{
	private final Thread thread;



	@Override
	public boolean isIdle()
	{
		return items.isEmpty();
	}

	private volatile boolean terminate = false;
	
	@Override
	public void shutdown()
	{
		terminate = true;
		thread.interrupt();
		try
		{
			thread.join();
		}
		catch (InterruptedException ex)
		{}
	}
	
	@Override
	public void run()
	{
		while (!terminate)
		{
			try
			{
				getDestinationMailbox().receive(new Message(getSourceActor(), getReverse(), items.take().message));
			}
			catch (InterruptedException ex)
			{
				if (!terminate)
					throw new RuntimeException(ex);
			}
		}
	}


	
	private class Item implements Delayed
	{
		private final Object message;
		private final long deliverAtNanoTime;
		
		public Item(Object msg, long deliverAtNanoTime)
		{
			message = msg;
			this.deliverAtNanoTime = deliverAtNanoTime;
		}

		@Override
		public long getDelay(TimeUnit unit)
		{
			final long t = System.nanoTime();
			final long nanoDelay = deliverAtNanoTime - t;
			switch (unit)
			{
				case DAYS:
					return nanoDelay / 24 / 60 / 60 / 1000000000;
				case MICROSECONDS:
					return nanoDelay / 1000;
				case MILLISECONDS:
					return nanoDelay / 1000 / 1000;
				case HOURS:
					return nanoDelay / 60 / 60 / 1000000000;
				case MINUTES:
					return nanoDelay / 60 / 1000000000;
				case NANOSECONDS:
					return nanoDelay;
				case SECONDS:
					return nanoDelay/ 1000000000;
				default:
					return 0;
			}
		}

		@Override
		public int compareTo(Delayed o)
		{
			return Long.compare(deliverAtNanoTime, ((Item)o).deliverAtNanoTime);
		}
	}
	
	private final DelayQueue<Item>	items = new DelayQueue<>();
	
	private final int msDelay;
	
	DelayedLink(ActorControl source, ActorControl destination, int msDelay)
	{
		super(source,destination);
		this.msDelay = msDelay;
		this.thread = new Thread(this);
		this.thread.start();
	}
	
	
	
	
	@Override
	public void sendMessage(Object message)
	{
		super.signalSend();
		items.add(new Item(message,System.nanoTime()+msDelay * 1000000));
	}

	
}
