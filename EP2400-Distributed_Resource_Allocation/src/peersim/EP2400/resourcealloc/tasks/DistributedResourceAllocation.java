package peersim.EP2400.resourcealloc.tasks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import peersim.EP2400.resourcealloc.base.Application;
import peersim.EP2400.resourcealloc.base.ApplicationsList;
import peersim.EP2400.resourcealloc.base.DistributedPlacementProtocol;
import peersim.EP2400.resourcealloc.tasks.placementStartegy.EnhancedStrategy;
import peersim.EP2400.resourcealloc.tasks.placementStartegy.PlacementStrategy;
import peersim.EP2400.resourcealloc.tasks.util.ApplicationInfo;
import peersim.EP2400.resourcealloc.tasks.util.Proposal;
import peersim.EP2400.resourcealloc.tasks.util.Proposal.ProposalType;
import peersim.config.FastConfig;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Node;

public class DistributedResourceAllocation extends DistributedPlacementProtocol {
	private PlacementStrategy		pStrategy	= new EnhancedStrategy();
	private Set<Integer> receivedApps; // apps that i receive so from the reconfiguration cost, i already paid 1 for it, so moving further does not cost us extra
	private Set<Integer> promisedApps; //promised to give this apps to another node
	
	public DistributedResourceAllocation(String prefix) {
		super(prefix);
		this.receivedApps = new HashSet<Integer>();
		tempLeasedApps = new ArrayList<ApplicationInfo>();
	}
	
	public DistributedResourceAllocation(String prefix, double cpu_capacity_value) {
		super(prefix, cpu_capacity_value);
		this.receivedApps = new HashSet<Integer>();
		tempLeasedApps = new ArrayList<ApplicationInfo>();
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
		
		//List<ApplicationInfo> ownAppList = buildAppInfoList(node, applicationsList());
		
		Proposal receivedProposal = n_prime.passiveThread_generateProposal(applicationsList(), receivedApps);
		Proposal acceptedProposal = processProposal(receivedProposal);
		n_prime.passiveThread_getAcceptedProposal(acceptedProposal);
		
		// Since the type of Proposal is according to the passive Node we need to switch it in order to perform the correct updatePlacement
		// A PUSH proposal for the passive Node is a PULL proposal for the active one
		acceptedProposal = switchType(acceptedProposal);
		updatePlacement(acceptedProposal);
	}
	
	public Proposal processProposal(final Proposal receivedProposal) {
		Proposal acceptedProposal = null;
		synchronized (pStrategy) {
			switch (receivedProposal.getProposalType()) {
				case PUSH:
					// Check that there is still available CPU Usage
					if (getTotalDemand() >= pStrategy.getCPUUsage(receivedProposal.getApplicationsList())) {
						acceptedProposal = receivedProposal;
					} else {
						List<ApplicationInfo> newAppSet = buildAppSetFromProposal(receivedProposal, new ArrayList<ApplicationInfo>());
						acceptedProposal = new Proposal(receivedProposal.getProposalType(), newAppSet);
					}
					break;
				case PULL:
					// Check that the Apps requested are still available
					List<ApplicationInfo> newAppSet = buildAppSetFromProposal(receivedProposal, tempLeasedApps);
					acceptedProposal = new Proposal(receivedProposal.getProposalType(), newAppSet);
					break;
				default:
					// Unknown proposal type - Create a fake reply
					acceptedProposal = new Proposal(receivedProposal.getProposalType(), new ArrayList<ApplicationInfo>());
					break;
			}
		}
		
		if (acceptedProposal.getApplicationsList().isEmpty()) {
			acceptedProposal = new Proposal(ProposalType.NO_ACTION, null);
		}
		
		return acceptedProposal;
	}
	
	//passive thread
	public Proposal passiveThread_generateProposal(final ApplicationsList partenerAppList, final Set<Integer> partnerReceivedApps) {
		return pStrategy.getProposal(applicationsList(), partnerAppList, partnerReceivedApps, promisedApps);
	}
	
	public void passiveThread_getAcceptedProposal(Proposal acceptedProposal) {
		updatePlacement(acceptedProposal);
	}
	
	public void updatePlacement(Proposal acceptedProposal) {
		
		// TODO: tempLeasedApps needs to be updated somewhere!!
		
		switch (acceptedProposal.getProposalType()) {
			case PUSH:
				// Deallocate accepted apps
				for (Application app : acceptedProposal.getApplicationsList()) {
					if (!applicationsList().contains(app)) {
						throw new RuntimeException();
					}
					deallocateApplication(app);
				}
				break;
			case PULL:
				// Allocate new apps
				for (ApplicationInfo appInfo : acceptedProposal.getApplicationsList()) {
					allocateApplication(appInfo.getApplication());
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
	
	@Override
	public Object clone() {
		DistributedResourceAllocation proto = new DistributedResourceAllocation(
			prefix, cpuCapacity);
		return proto;
	}
	
	@Deprecated
	public ApplicationsList passiveThread(ApplicationsList A_n_prime) {
		ApplicationsList tempA_n = applicationsList();
		updatePlacement(A_n_prime);
		return tempA_n;
	}
	
	@Deprecated
	public void updatePlacement(ApplicationsList A_n_prime)
	{
		// TODO: Implement your code for task 2 here.
		
	}
	
	private List<ApplicationInfo> buildAppSetFromProposal(final Proposal receivedProposal, final List<ApplicationInfo> leasedAppList) {
		List<ApplicationInfo> appInfoList = new ArrayList<ApplicationInfo>();
		double usedCPUUnits = 0;
		for (ApplicationInfo appInfo : receivedProposal.getApplicationsList()) {
			if (!appInfo.appMoved() && !leasedAppList.contains(appInfo)) {
				double appCPUDemand = appInfo.getApplication().getCPUDemand();
				if (usedCPUUnits + appCPUDemand <= getTotalDemand()) {
					appInfoList.add(appInfo);
					usedCPUUnits += appCPUDemand;
				}
			}
		}
		return appInfoList;
	}
	
	private Proposal switchType(Proposal proposal) {
		if (proposal.getProposalType() == ProposalType.PUSH) {
			proposal = new Proposal(ProposalType.PULL, proposal.getApplicationsList());
		} else if (proposal.getProposalType() == ProposalType.PULL) {
			proposal = new Proposal(ProposalType.PUSH, proposal.getApplicationsList());
		}
		return proposal;
	}
}