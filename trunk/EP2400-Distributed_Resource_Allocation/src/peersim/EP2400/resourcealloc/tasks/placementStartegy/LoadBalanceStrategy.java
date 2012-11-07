package peersim.EP2400.resourcealloc.tasks.placementStartegy;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import peersim.EP2400.resourcealloc.base.ApplicationsList;
import peersim.EP2400.resourcealloc.tasks.util.AppCPUComparator;
import peersim.EP2400.resourcealloc.tasks.util.Proposal;
import peersim.EP2400.resourcealloc.tasks.util.Proposal.ProposalType;

public class LoadBalanceStrategy extends PlacementStrategy {
	
	@Override
	public synchronized Proposal getProposal(final ApplicationsList ownAppList, final ApplicationsList partnerAppList,
		final Set<Integer> ownReceivedApps, final Set<Integer> partnerReceivedApps, final Set<Integer> ownPromisedApps) {
		double ownCPUUsage = ownAppList.totalCPUDemand();
		double partnerCPUUsage = partnerAppList.totalCPUDemand();
		ProposalType pType = null;
		ApplicationsList propAppList = null;
		
		if (ownCPUUsage > partnerCPUUsage) {
			pType = ProposalType.PUSH;
			Collections.sort(ownAppList, new AppCPUComparator());
			propAppList = getAppListToPropose((ownCPUUsage - partnerCPUUsage) / 2, ownAppList, ownReceivedApps, ownPromisedApps);
		} else {
			pType = ProposalType.PULL;
			Collections.sort(partnerAppList, new AppCPUComparator());
			propAppList = getAppListToPropose((partnerCPUUsage - ownCPUUsage) / 2, partnerAppList, partnerReceivedApps, new HashSet<Integer>());
		}
		
		return new Proposal(pType, propAppList);
	}
}
