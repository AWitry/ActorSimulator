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

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Special-purpose queue with optionally blocking read access.
 * Designed to accept writes from multiple threads, but reads only from one
 * thread.
 * Tracks when the read-thread has entered the blocking dequeue method
 * @param <T> Contained object
 */
public class BlockingQueue<T>
{
	private final ConcurrentLinkedQueue<T> queue = new ConcurrentLinkedQueue<>();
	private int	messagesDispatched = 0;
	private volatile boolean waiting = false, hasQuit = false;

	public boolean isEmpty()
	{
		return queue.isEmpty() || hasQuit;
	}

	/**
	 * Performs a synchronized wait operation for the next message.
	 * This method is invoked while inactive, waiting for messages before
	 * reactivating the owning actor.
	 * @throws InterruptedException 
	 */
	public synchronized void inactiveAwaitMessages() throws InterruptedException
	{
		if (hasQuit)
			return;
		if (!queue.isEmpty())
			return;
		wait();
	}

	
	
	/**
	 * On-quit pass-through exception.
	 * Thrown and caught by the thread calling take(),
	 * which quits as a result.
	 */
	public static class Quit extends RuntimeException
	{};
	
	/**
	 * Signals that local operations should terminate.
	 * Wakes any potentially sleeping read thread.
	 * Once quit, the local queue cannot continue operation,
	 * and must be recreated.
	 * This method is intended as a final operation to terminate the network.
	 */
	public synchronized void quit()
	{
		hasQuit = true;
		queue.clear();
		notifyAll();
	}
	
	/**
	 * Checks whether the read thread is currently waiting for items
	 * @return True if waiting, false otherwise
	 */
	public synchronized boolean isWaiting()
	{
		return waiting;
	}
	
	/**
	 * Attempts to dequeue the next queued item without blocking
	 * @return Item or null if none are waiting
	 */
	public synchronized T tryTake()
	{
		T rs = queue.poll();
		if (rs != null)
			messagesDispatched++;
		return rs;
	}
	
	/**
	 * Attempts to dequeue the next queued item, blocking if none are waiting
	 * @return Dequeued item (never null)
	 */
	public synchronized T take(Network reportTo)
	{
		try
		{
			if (hasQuit)
				throw new Quit();
			T rs = queue.poll();
			if (rs != null)
			{
				messagesDispatched++;
				return rs;
			}
			
			waiting = true;
			reportTo.triggerTerminationCheck();
			wait();
			waiting = false;
			
			rs = queue.poll();
			if (rs == null || hasQuit)
				throw new Quit();
			messagesDispatched++;
			return rs;
		}
		catch (InterruptedException ex)
		{
			//not happening. don't know what to do
			return null;
		}
	}
	
	/**
	 * Retrieves the total number of messages that have been delivered via
	 * take() or tryTake() since creation of the local object.
	 * @return Number of dispatches messages
	 */
	public int countDispatchesMessages()
	{
		return messagesDispatched;
	}
	
	/**
	 * Atomic query as the name indicates.
	 * Can be used to determine whether the read-thread is either currently
	 * active or will be active shortly.
	 * @return True if non-empty or thread is not waiting
	 */
	public synchronized boolean isNotEmptyOrNotWaiting()
	{
		return !waiting || !isEmpty();
	}
	
	/**
	 * Atomically adds an item to the end of the local queue.
	 * Any waiting read-thread will be woken
	 * @param item Item to enqueue
	 */
	public synchronized void add(T item)
	{
		if (hasQuit)
			throw new IllegalAccessError("Local queue has quit");
		queue.add(item);
		notify();
	}
}
