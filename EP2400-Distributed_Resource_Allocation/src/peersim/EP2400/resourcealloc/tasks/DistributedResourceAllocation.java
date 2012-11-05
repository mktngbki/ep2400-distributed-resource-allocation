package peersim.EP2400.resourcealloc.tasks;

import java.util.List;

import peersim.EP2400.resourcealloc.base.Application;
import peersim.EP2400.resourcealloc.base.ApplicationsList;
import peersim.EP2400.resourcealloc.base.DistributedPlacementProtocol;
import peersim.EP2400.resourcealloc.tasks.placementStartegy.LoadBalanceStrategy;
import peersim.EP2400.resourcealloc.tasks.placementStartegy.PlacementStrategy;
import peersim.EP2400.resourcealloc.tasks.util.Proposal;
import peersim.config.FastConfig;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Node;

public class DistributedResourceAllocation extends DistributedPlacementProtocol {
	private List<Application>	tempLeasedApps;							//promised to give this apps to another node
	private PlacementStrategy	pStrategy	= new LoadBalanceStrategy();
	
	public DistributedResourceAllocation(String prefix) {
		super(prefix);
	}
	
	public DistributedResourceAllocation(String prefix, double cpu_capacity_value) {
		super(prefix, cpu_capacity_value);
		
	}
	
	@Override
	public void nextCycle(Node node, int protocolID) {
		int linkableID = FastConfig.getLinkable(protocolID);
		Linkable linkable = (Linkable) node.getProtocol(linkableID);
		
		int degree = linkable.degree();
		int nbIndex = CommonState.r.nextInt(degree);
		Node peer = linkable.getNeighbor(nbIndex);
		// The selected peer could be inactive
		if (!peer.isUp()) {
			return;
		}
		
		DistributedResourceAllocation n_prime = (DistributedResourceAllocation) peer.getProtocol(protocolID);
		
		// send and receive message by method call. This follows the
		// cycle-driven simulation approach.
		//		ApplicationsList A_n_prime = n_prime.passiveThread(this.applicationsList());
		//
		//		this.updatePlacement(A_n_prime);
		
		Proposal receivedProposal = n_prime.passiveThread_getProposal(applicationsList());
		Proposal acceptedProposal = acceptProposal(receivedProposal);
		n_prime.passiveThread_acceptProposal(acceptedProposal);
		updatePlacement(acceptedProposal);
	}
	
	public Proposal acceptProposal(Proposal receivedProposal) {
		Proposal acceptedProposal = null;
		synchronized (pStrategy) {
			//build accepted proposal
		}
		return acceptedProposal;
	}
	
	//passive thread
	public Proposal passiveThread_getProposal(ApplicationsList appList) {
		
		return null;
	}
	
	public void passiveThread_acceptProposal(Proposal acceptedProposal) {
		updatePlacement(acceptedProposal);
	}
	
	public ApplicationsList passiveThread(ApplicationsList A_n_prime) {
		ApplicationsList tempA_n = applicationsList();
		updatePlacement(A_n_prime);
		return tempA_n;
	}
	
	public void updatePlacement(ApplicationsList A_n_prime)
	{
		// TODO: Implement your code for task 2 here.
		
	}
	
	public void updatePlacement(final Proposal receivedProposal) {
		switch (receivedProposal.getProposalType()) {
			case PUSH:
				// TODO: Check that there is still available CPU Usage
				
				break;
			case PULL:
				// TODO: Check that the Apps requested are still available
				
				break;
			case NO_ACTION:
				// No action is required
				break;
			default:
				// Unknown proposal type - No action needs to be performed
				break;
		}
	}
	
	@Override
	public Object clone() {
		DistributedResourceAllocation proto = new DistributedResourceAllocation(
			prefix, cpuCapacity);
		return proto;
	}
}
