/*
 * Copyright (c) 2010 LCN, EE school, KTH
 *
 */

package peersim.EP2400.resourcealloc.controls;
import peersim.EP2400.resourcealloc.base.Application;
import peersim.EP2400.resourcealloc.base.ApplicationsManager;
import peersim.EP2400.resourcealloc.base.MemoryAwareDistributedPlacementProtocol;
import peersim.config.Configuration;
import peersim.core.*;

/**
 * This class provides random initializer where applications are randomly allocated to each node
 * but not allocated more than its memory capacity
 * @author Rerngvit Yanggratoke
 */
public class MemoryAwarePlacementInitializer extends PlacementInitializer{
	private int memoryDemand;
	
	protected static final String PAR_MEM_DEMAND = "memory_demand";
    public MemoryAwarePlacementInitializer(String prefix) {
		super(prefix);
		memoryDemand = Configuration.getInt(prefix + "." + PAR_MEM_DEMAND);
	}

	
    public boolean execute() {
    	super.execute();
    	
    	
    	for (Application a : ApplicationsManager.getInstance().applications())
    	{
    		a.setMemoryDemand(memoryDemand);
    		MemoryAwareDistributedPlacementProtocol p;
    		
    		int nsize = Network.size();
    		do{
    		int nodeIndex =  CommonState.r.nextInt(nsize);
    		
    		p = ((MemoryAwareDistributedPlacementProtocol) Network.get(nodeIndex).getProtocol(protocolID));
    		
    		if (p.applicationsList().totalMemoryDemand() + a.getMemoryDemand() <= p.getMemoryCapacity())
    			break;
    		
    		} while(true);
    		p.allocateApplication(a);
    		
    		
    	}
    	
    	
    	return false;
    }

}
