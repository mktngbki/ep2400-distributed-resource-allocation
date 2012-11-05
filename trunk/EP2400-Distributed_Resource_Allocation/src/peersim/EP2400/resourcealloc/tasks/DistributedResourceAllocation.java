package peersim.EP2400.resourcealloc.tasks;

import java.util.ArrayList;
import java.util.List;

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
	//TODO: DO not forget to initialize
	private List<ApplicationInfo>	tempLeasedApps;						//promised to give this apps to another node
	private PlacementStrategy		pStrategy	= new EnhancedStrategy();
	
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
		
		List<ApplicationInfo> ownAppList = buildAppInfoList(node, applicationsList());
		
		Proposal receivedProposal = n_prime.passiveThread_generateProposal(peer, ownAppList);
		Proposal acceptedProposal = processProposal(receivedProposal);
		n_prime.passiveThread_getAcceptedProposal(acceptedProposal);
		updatePlacement(acceptedProposal, true);
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
	public Proposal passiveThread_generateProposal(final Node node, final List<ApplicationInfo> ownAppList) {
		List<ApplicationInfo> partnerAppList = buildAppInfoList(node, applicationsList());
		return pStrategy.getProposal(ownAppList, partnerAppList, tempLeasedApps);
	}
	
	public void passiveThread_getAcceptedProposal(Proposal acceptedProposal) {
		updatePlacement(acceptedProposal, false);
	}
	
	public void updatePlacement(Proposal acceptedProposal, final boolean reverse) {
		if (reverse) {
			// TODO: Instantiate new proposal because we do not have setters... should we introduce them???
			if (acceptedProposal.getProposalType() == ProposalType.PUSH) {
				acceptedProposal = new Proposal(ProposalType.PULL, acceptedProposal.getApplicationsList());
			} else if (acceptedProposal.getProposalType() == ProposalType.PULL) {
				acceptedProposal = new Proposal(ProposalType.PUSH, acceptedProposal.getApplicationsList());
			}
		}
		
		// TODO: tempLeasedApps needs to be updated somewhere!!
		
		switch (acceptedProposal.getProposalType()) {
			case PUSH:
				// Deallocate accepted apps
				for (Application app : applicationsList()) {
					if (acceptedProposal.getApplicationsList().contains(app)) {
						deallocateApplication(app);
					}
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
	
	private List<ApplicationInfo> buildAppInfoList(Node node, ApplicationsList applicationsList) {
		List<ApplicationInfo> appInfoList = new ArrayList<ApplicationInfo>();
		for (Application app : applicationsList) {
			appInfoList.add(new ApplicationInfo(app, node));
		}
		return appInfoList;
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
}