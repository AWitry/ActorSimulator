/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package actorsimulator;

/**
 *
 * @author IronFox
 */
class Ref<T>
{
	public T ref = null;
	
	public Ref()
	{}
	
	public Ref(T item)
	{
		ref = item;	
	}
}
