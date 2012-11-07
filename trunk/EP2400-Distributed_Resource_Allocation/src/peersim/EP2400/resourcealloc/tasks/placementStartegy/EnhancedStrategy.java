package peersim.EP2400.resourcealloc.tasks.placementStartegy;

import java.util.HashSet;
import java.util.Set;

import peersim.EP2400.resourcealloc.base.Application;
import peersim.EP2400.resourcealloc.base.ApplicationsList;
import peersim.EP2400.resourcealloc.tasks.util.Proposal;
import peersim.EP2400.resourcealloc.tasks.util.Proposal.ProposalType;

public class EnhancedStrategy extends PlacementStrategy {
	private static final double	CPU_USAGE_THRESHOLD	= 70;
	
	private double				CPUCapacity;
	
	public EnhancedStrategy(final double CPUCapacity) {
		this.CPUCapacity = CPUCapacity;
	}
	
	@Override
	public Proposal getProposal(final ApplicationsList ownAppList, final ApplicationsList partnerAppList,
		final Set<Integer> ownReceivedApps, final Set<Integer> partnerReceivedApps, final Set<Integer> ownPromisedApps) {
		double ownCPUUsage = ownAppList.totalCPUDemand();
		double partnerCPUUsage = partnerAppList.totalCPUDemand();
		ProposalType pType = null;
		ApplicationsList propAppList = new ApplicationsList();
		
		//Apply Energy Efficient Strategy if the sum of the CPU Usage of both nodes is below threshold * 2
		if (ownCPUUsage + partnerCPUUsage <= CPU_USAGE_THRESHOLD * 2) {
			// Give Apps to the node that has more CPU usage if both the nodes are under the threshold
			if (ownCPUUsage < CPU_USAGE_THRESHOLD && partnerCPUUsage < CPU_USAGE_THRESHOLD) {
				if (partnerCPUUsage > ownCPUUsage) {
					pType = ProposalType.PUSH;
					propAppList = getAppListToPropose(CPU_USAGE_THRESHOLD - partnerCPUUsage, ownAppList, ownReceivedApps, ownPromisedApps);
				} else {
					pType = ProposalType.PULL;
					propAppList = getAppListToPropose(CPU_USAGE_THRESHOLD - ownCPUUsage, partnerAppList, partnerReceivedApps, new HashSet<Integer>());
				}
			}
			// Apply Load Balance Strategy if one of the nodes is overloaded (Maximize energy efficiency)
			else if (ownCPUUsage > CPU_USAGE_THRESHOLD && partnerCPUUsage < CPU_USAGE_THRESHOLD) {
				pType = ProposalType.PUSH;
				propAppList = getAppListToPropose(ownCPUUsage - CPU_USAGE_THRESHOLD, ownAppList, ownReceivedApps, ownPromisedApps, true);
			} else if (partnerCPUUsage > CPU_USAGE_THRESHOLD && ownCPUUsage < CPU_USAGE_THRESHOLD) {
				pType = ProposalType.PULL;
				propAppList = getAppListToPropose(partnerCPUUsage - CPU_USAGE_THRESHOLD, partnerAppList, partnerReceivedApps, new HashSet<Integer>(), true);
			}
		}
		// Apply pure Load Balance Strategy if both the nodes are overloaded
		else {
			if (ownCPUUsage > partnerCPUUsage) {
				pType = ProposalType.OVERLOADED_PUSH;
				propAppList = getAppListToPropose((ownCPUUsage - partnerCPUUsage) / 2, ownAppList, ownReceivedApps, ownPromisedApps);
			} else {
				pType = ProposalType.PULL;
				propAppList = getAppListToPropose((partnerCPUUsage - ownCPUUsage) / 2, partnerAppList, partnerReceivedApps, new HashSet<Integer>());
			}
		}
		
		return new Proposal(pType, propAppList);
	}
	
	@Override
	public Proposal processProposal(final Proposal receivedProposal, final ApplicationsList ownAppList, final Set<Integer> ownPromisedApps) {
		Proposal acceptedProposal = null;
		switch (receivedProposal.getProposalType()) {
			case PUSH:
				// Receive Apps - Check that there is still available CPU Usage
				if (ownAppList.totalCPUDemand() >= receivedProposal.getApplicationsList().totalCPUDemand()) {
					acceptedProposal = receivedProposal;
				} else {
					ApplicationsList newAppSetPush = buildAppSetFromProposal(receivedProposal, new HashSet<Integer>(), CPU_USAGE_THRESHOLD);
					acceptedProposal = new Proposal(receivedProposal.getProposalType(), newAppSetPush);
				}
				break;
			case PULL:
				// Send Apps - Check that the Apps requested are still available
				ApplicationsList newAppSetPull = buildAppSetFromProposal(receivedProposal, ownPromisedApps, CPU_USAGE_THRESHOLD);
				acceptedProposal = new Proposal(receivedProposal.getProposalType(), newAppSetPull);
				break;
			case OVERLOADED_PUSH:
				// Receive Apps - Check that CPU Usage do not go over CPU Capacity
				ApplicationsList newAppSetOverloadedPush = buildAppSetFromProposal(receivedProposal, ownPromisedApps, CPUCapacity);
				acceptedProposal = new Proposal(receivedProposal.getProposalType(), newAppSetOverloadedPush);
				break;
			default:
				// Unknown proposal type - Create a fake reply
				acceptedProposal = new Proposal(receivedProposal.getProposalType(), new ApplicationsList());
				break;
		}
		
		if (acceptedProposal.getApplicationsList().isEmpty()) {
			acceptedProposal = new Proposal(ProposalType.NO_ACTION, null);
		}
		
		return acceptedProposal;
	}
	
	private ApplicationsList buildAppSetFromProposal(final Proposal receivedProposal, final Set<Integer> ownPromisedApps, final double maxCPUUsage) {
		ApplicationsList appList = new ApplicationsList();
		double usedCPUUnits = 0;
		for (Application app : receivedProposal.getApplicationsList()) {
			if (!ownPromisedApps.contains(app)) {
				double appCPUDemand = app.getCPUDemand();
				if (usedCPUUnits + appCPUDemand <= maxCPUUsage) {
					appList.add(app);
					usedCPUUnits += appCPUDemand;
				}
			}
		}
		return appList;
	}
}
