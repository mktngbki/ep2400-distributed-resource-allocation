package peersim.EP2400.resourcealloc.tasks;

import peersim.EP2400.resourcealloc.base.ApplicationsList;
import peersim.EP2400.resourcealloc.base.MemoryAwareDistributedPlacementProtocol;
import peersim.cdsim.CDProtocol;
import peersim.config.FastConfig;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Node;
/**
 * THis class is for the optional task.
 * @author mac
 *
 */
public class AdvancedDistributedResourceAllocation extends MemoryAwareDistributedPlacementProtocol implements CDProtocol{

	public AdvancedDistributedResourceAllocation(String prefix) {
		super(prefix);
		
	}
	
	public AdvancedDistributedResourceAllocation(String prefix,  double cpu_capacity_value, double memory_capacity_value) {
		super(prefix, cpu_capacity_value, memory_capacity_value);
	}
	
	private void validate()
	{
		if (this.applicationsList().totalMemoryDemand() > this.memoryCapacity)
		{
			// validation fail
			
			String errorMessage = String.format(" This node uses total %f GB when memory capacity is %f GB" ,
					this.applicationsList().totalMemoryDemand(),  this.memoryCapacity);
			System.out.println(errorMessage);
			System.out.println("Terminating now");
			System.exit(1);
		}
		
	}
	

public void nextCycle(Node node, int protocolID) {
		
	int linkableID = FastConfig.getLinkable(protocolID);
    Linkable linkable = (Linkable) node.getProtocol(linkableID);
    
    int degree  = linkable.degree();
    int nbIndex = CommonState.r.nextInt(degree);
    Node peer = linkable.getNeighbor(nbIndex);
      // The selected peer could be inactive
      if (!peer.isUp())
         return;
      
      AdvancedDistributedResourceAllocation n_prime = (AdvancedDistributedResourceAllocation) peer.getProtocol(protocolID);
      ApplicationsList A_n_prime = n_prime.passiveThread(this.applicationsList());
      this.updatePlacement(A_n_prime);
    
     validate();
    }
	

	public ApplicationsList passiveThread(ApplicationsList A_n_prime)
	{
		ApplicationsList tempA_n = this.applicationsList();
		this.updatePlacement(A_n_prime);
		return tempA_n;
	}
	
	
	
	public void updatePlacement(ApplicationsList A_n_prime) {

		// Implement your code for task 4 (optional) here.

	}

	
	public Object clone() {
		AdvancedDistributedResourceAllocation proto = new AdvancedDistributedResourceAllocation(
				this.prefix, this.cpuCapacity, this.memoryCapacity);
		return proto;
	}

	

}
