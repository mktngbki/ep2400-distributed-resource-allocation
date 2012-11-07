package peersim.EP2400.resourcealloc.tasks;

import java.util.Set;

import peersim.EP2400.resourcealloc.base.ApplicationsList;
import peersim.EP2400.resourcealloc.base.DistributedPlacementProtocol;
import peersim.EP2400.resourcealloc.tasks.placementStartegy.PlacementStrategy;
import peersim.config.FastConfig;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Node;

public class DistributedResourceAllocationEasy extends
		DistributedPlacementProtocol {

	private PlacementStrategy	pStrategy;
	private Set<Integer>	ownReceivedApps;	// apps that i receive so from the reconfiguration cost, i already paid 1 for it, so moving further does not cost us extra
	
	public DistributedResourceAllocationEasy(String prefix) {
		super(prefix);
	}

	public DistributedResourceAllocationEasy(String prefix, double cpu_capacity_value) {
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

		DistributedResourceAllocationEasy n_prime = (DistributedResourceAllocationEasy) peer.getProtocol(protocolID);

		// send and receive message by method call. This follows the
		// cycle-driven simulation approach.
		ApplicationsList ownApps = this.applicationsList();
		ApplicationsList neighborApps = n_prime.passiveThread(ownApps);

		this.updatePlacement(neighborApps);

	}

	public ApplicationsList passiveThread(ApplicationsList A_n_prime) {
		ApplicationsList tempA_n = this.applicationsList();
		this.updatePlacement(A_n_prime);
		return tempA_n;
	}

	public void updatePlacement(ApplicationsList A_n_prime)
	{
	}
	
	
	
	
	public Object clone() {
		DistributedResourceAllocation proto = new DistributedResourceAllocation(
				this.prefix, this.cpuCapacity);
		return proto;
	}

}
