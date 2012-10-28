package peersim.EP2400.resourcealloc.tasks;

import peersim.EP2400.resourcealloc.base.ApplicationsList;
import peersim.EP2400.resourcealloc.base.DistributedPlacementProtocol;
import peersim.config.FastConfig;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Node;

public class DistributedResourceAllocation extends
		DistributedPlacementProtocol {

	public DistributedResourceAllocation(String prefix) {
		super(prefix);
	}

	public DistributedResourceAllocation(String prefix, double cpu_capacity_value) {
		super(prefix, cpu_capacity_value);

	}

	public void nextCycle(Node node, int protocolID) {
		int linkableID = FastConfig.getLinkable(protocolID);
		Linkable linkable = (Linkable) node.getProtocol(linkableID);

		int degree = linkable.degree();
		int nbIndex = CommonState.r.nextInt(degree);
		Node peer = linkable.getNeighbor(nbIndex);
		// The selected peer could be inactive
		if (!peer.isUp())
			return;

		DistributedResourceAllocation n_prime = (DistributedResourceAllocation) peer
				.getProtocol(protocolID);

		// send and receive message by method call. This follows the
		// cycle-driven simulation approach.
		ApplicationsList A_n_prime = n_prime.passiveThread(this
				.applicationsList());

		this.updatePlacement(A_n_prime);

	}

	public ApplicationsList passiveThread(ApplicationsList A_n_prime) {
		ApplicationsList tempA_n = this.applicationsList();
		this.updatePlacement(A_n_prime);
		return tempA_n;
	}

	public void updatePlacement(ApplicationsList A_n_prime)
	{
		// Implement your code for task 2 here.
		
	}
	
	
	
	
	public Object clone() {
		DistributedResourceAllocation proto = new DistributedResourceAllocation(
				this.prefix, this.cpuCapacity);
		return proto;
	}

}
