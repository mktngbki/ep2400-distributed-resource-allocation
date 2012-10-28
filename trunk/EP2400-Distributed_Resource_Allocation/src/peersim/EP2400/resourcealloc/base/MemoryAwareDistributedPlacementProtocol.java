package peersim.EP2400.resourcealloc.base;

import peersim.config.Configuration;

public class MemoryAwareDistributedPlacementProtocol extends
		DistributedPlacementProtocol {

	protected static final String PAR_MEMORY_CAPACITY = "memory_capacity";

	/**
	 * Memory capacity value
	 */
	protected final double memoryCapacity;

	/**
	 * Accessor for the Memory capacity. This is the the same for all servers.
	 * 
	 * @return
	 */
	public double getMemoryCapacity() {
		return memoryCapacity;
	}

	
	public MemoryAwareDistributedPlacementProtocol(String prefix) {
		super(prefix);
	    memoryCapacity = Configuration.getDouble(prefix + "." + PAR_MEMORY_CAPACITY);
	}
	

    public MemoryAwareDistributedPlacementProtocol(String prefix, double cpu_capacity_value, double memory_capacity_value) {
    	super(prefix, cpu_capacity_value);
        this.prefix = prefix;
        this.memoryCapacity = memory_capacity_value; 
       
       
    }
    
    public Object clone()
    {
       MemoryAwareDistributedPlacementProtocol proto = new MemoryAwareDistributedPlacementProtocol(this.prefix, this.cpuCapacity,  this.memoryCapacity);
 	   return proto;
    }

}
