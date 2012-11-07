package peersim.EP2400.resourcealloc.tasks.placementStartegy;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import peersim.EP2400.resourcealloc.base.ApplicationsList;
import peersim.EP2400.resourcealloc.tasks.util.AppCPUComparator;
import peersim.EP2400.resourcealloc.tasks.util.Proposal;
import peersim.EP2400.resourcealloc.tasks.util.Proposal.ProposalType;

public class EnergyEfficientStrategy extends PlacementStrategy {
	private static final double	CPU_USAGE_THRESHOLD	= 70;
	
	@Override
	public synchronized Proposal getProposal(final ApplicationsList ownAppList, final ApplicationsList partnerAppList,
		final Set<Integer> ownReceivedApps, final Set<Integer> partnerReceivedApps, final Set<Integer> ownPromisedApps) {
		double ownCPUUsage = ownAppList.totalCPUDemand();
		double partnerCPUUsage = partnerAppList.totalCPUDemand();
		ProposalType pType = null;
		ApplicationsList propAppList = new ApplicationsList();
		
		double maxCPUUsage = Math.max(ownCPUUsage, partnerCPUUsage);
		
		if (maxCPUUsage == partnerCPUUsage && partnerCPUUsage < CPU_USAGE_THRESHOLD) {
			pType = ProposalType.PUSH;
			Collections.sort(ownAppList, new AppCPUComparator());
			propAppList = getAppListToPropose(CPU_USAGE_THRESHOLD - partnerCPUUsage, ownAppList, ownReceivedApps, ownPromisedApps);
		} else if (maxCPUUsage == ownCPUUsage && ownCPUUsage < CPU_USAGE_THRESHOLD) {
			pType = ProposalType.PULL;
			Collections.sort(partnerAppList, new AppCPUComparator());
			propAppList = getAppListToPropose(CPU_USAGE_THRESHOLD - ownCPUUsage, partnerAppList, partnerReceivedApps, new HashSet<Integer>());
		} else {
			pType = ProposalType.NO_ACTION;
		}
		
		return new Proposal(pType, propAppList);
	}
}
