/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package actorsimulator;

/**
 * Error thrown if deadlock occurrence is assumed.
 */
public class DeadlockError extends Error
{

	DeadlockError(String string)
	{
		super(string);
	}

	DeadlockError(InterruptedException ex)
	{
		super(ex);
	}
	
}
