package peersim.EP2400.resourcealloc.tasks;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import peersim.EP2400.resourcealloc.base.Application;
import peersim.EP2400.resourcealloc.base.ApplicationsList;
import peersim.EP2400.resourcealloc.base.DistributedPlacementProtocol;
import peersim.EP2400.resourcealloc.tasks.placementStartegy.EnhancedStrategy;
import peersim.EP2400.resourcealloc.tasks.placementStartegy.PlacementStrategy;
import peersim.EP2400.resourcealloc.tasks.util.Proposal;
import peersim.config.FastConfig;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Node;

public class DistributedResourceAllocation extends DistributedPlacementProtocol {
	private PlacementStrategy pStrategy;
	private Collection<Integer> ownReceivedApps; // apps that i receive so from the reconfiguration cost, i already paid 1 for it, so moving further does not cost us extra
	private Collection<Integer> ownPromisedApps; //promised to give this apps to another node

	public DistributedResourceAllocation(String prefix) {
		super(prefix);
		pStrategy = new EnhancedStrategy(cpuCapacity);
		this.ownReceivedApps = Collections.synchronizedCollection(new HashSet<Integer>());
		this.ownPromisedApps = Collections.synchronizedCollection(new HashSet<Integer>());
	}

	public DistributedResourceAllocation(String prefix, double cpu_capacity_value) {
		super(prefix, cpu_capacity_value);
		pStrategy = new EnhancedStrategy(cpuCapacity);
		this.ownReceivedApps = Collections.synchronizedCollection(new HashSet<Integer>());
		this.ownPromisedApps = Collections.synchronizedCollection(new HashSet<Integer>());
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

		ApplicationsList ownApplicationList = applicationsList();
		Proposal receivedProposal = n_prime.passiveThread_generateProposal(ownApplicationList, ownReceivedApps);
		Proposal acceptedProposal = pStrategy.processProposal(receivedProposal, ownApplicationList, ownPromisedApps);
		n_prime.passiveThread_getAcceptedProposal(receivedProposal, acceptedProposal);

		// Since the type of Proposal is according to the passive Node we need to switch it in order to perform the correct updatePlacement
		// A PUSH proposal for the passive Node is a PULL proposal for the active one
		acceptedProposal = acceptedProposal.switchType();
		updatePlacement(receivedProposal, acceptedProposal);
	}

	//passive thread
	public Proposal passiveThread_generateProposal(final ApplicationsList partnerAppList, final Collection <Integer> partnerReceivedApps) {
		return pStrategy.getProposal(applicationsList(), partnerAppList, ownReceivedApps, partnerReceivedApps, ownPromisedApps);
	}

	public void passiveThread_getAcceptedProposal(Proposal initialProposal, Proposal acceptedProposal) {
		updatePlacement(initialProposal, acceptedProposal);
	}

	public void updatePlacement(Proposal initialProposal, Proposal acceptedProposal) {
		//access to ownProposedApps and ownReceivedApps has to be synchronized in order to have a consistent state
		synchronized(pStrategy) {
			switch (acceptedProposal.getProposalType()) {
			case OVERLOADED_PUSH:
			case PUSH: 
				// 1. Deallocate accepted apps
				// 2. remove apps from promisedApps list
				for (Application app : acceptedProposal.getApplicationsList()) {
					if (!applicationsList().contains(app)) {
						throw new RuntimeException();
					}
					deallocateApplication(app);
					ownPromisedApps.remove(app.getID());
				}
				break;
			case PULL:
				// 1. Allocate new apps
				// 2. Mark received applications as beeing received and not local
				for (Application app : acceptedProposal.getApplicationsList()) {
					allocateApplication(app);
					ownReceivedApps.add(app.getID());
				}
				break;
			case NO_ACTION:
				// No action is required
				break;
			default:
				// Unknown proposal type - No action needs to be performed
				break;
			}
		}
	}

	@Override
	public Object clone() {
		DistributedResourceAllocation proto = new DistributedResourceAllocation(prefix, cpuCapacity);
		return proto;
	}
}