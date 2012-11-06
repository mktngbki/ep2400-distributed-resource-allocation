package peersim.EP2400.resourcealloc.tasks.placementStartegy;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import peersim.EP2400.resourcealloc.base.ApplicationsList;
import peersim.EP2400.resourcealloc.tasks.util.AppCPUComparator;
import peersim.EP2400.resourcealloc.tasks.util.Proposal;
import peersim.EP2400.resourcealloc.tasks.util.Proposal.ProposalType;

public class EnergyEfficientStrategy extends PlacementStrategy {
	private static final double	CPU_USAGE_THRESHOLD	= 70;
	
	@Override
	public synchronized Proposal getProposal(final ApplicationsList ownAppList, final ApplicationsList partnerAppList,
		final Collection<Integer> ownReceivedApps, final Collection<Integer> partnerReceivedApps, final Collection<Integer> ownPromisedApps) {
		double ownCPUUsage = ownAppList.totalCPUDemand();
		double partnerCPUUsage = partnerAppList.totalCPUDemand();
		ProposalType pType = null;
		ApplicationsList propAppList = null;
		
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
			propAppList = new ApplicationsList();
		}
		
		return new Proposal(pType, propAppList);
	}
}
