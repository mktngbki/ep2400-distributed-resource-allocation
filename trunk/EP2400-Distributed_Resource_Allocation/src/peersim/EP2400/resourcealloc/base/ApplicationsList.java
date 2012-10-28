/*
 * Copyright (c) 2010 LCN, EE school, KTH
 *
 */

package peersim.EP2400.resourcealloc.base;

import java.util.ArrayList;

/**
 * A class for representing placement set for each server. For simplicity, this is represented by ArrayList
 * @author Rerngvit Yanggratoke
 */
public class ApplicationsList extends ArrayList<Application> {

	private static final long serialVersionUID = -2896826171558618679L;

	/**
	 * Constructor which initialize the list and total demand.
	 */
	public ApplicationsList()
	{
		
	}
	
	/**
	 * add application to this placement set. This will update the totalDemand cache.
	 */
	public boolean add(Application a)
	{
		
		boolean result = super.add(a);
		return result;
		
	}
	
	/**
	 * remove application from this placement set. This will update the totalDemand cache.
	 * @param a
	 */
	public boolean remove(Application a)
	{
		boolean result = super.remove(a);
		return result;
	}
	
	
	/**
	 * Retrieve total CPU demand from this placement. 
	 * @return
	 */
	public double totalCPUDemand()
	{
		
		double totalDemand = 0;
		for( Application a: this)
		{
			totalDemand += a.getCPUDemand();
		}
		
		
		
		return totalDemand;
	}

	/**
	 * Retrieve total memory demand from this placement. 
	 * @return
	 */
	public double totalMemoryDemand()
	{
		
		double totalDemand = 0;
		for( Application a: this)
		{
			totalDemand += a.getMemoryDemand();
		}
		
		
		
		return totalDemand;
	}


	
}
