package peersim.EP2400.resourcealloc.tasks.placementStartegy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import peersim.EP2400.resourcealloc.tasks.util.AppCPUComparator;
import peersim.EP2400.resourcealloc.tasks.util.ApplicationInfo;
import peersim.EP2400.resourcealloc.tasks.util.Proposal;
import peersim.EP2400.resourcealloc.tasks.util.Proposal.ProposalType;

public class LoadBalanceStrategy extends PlacementStrategy {
	
	@Override
	public synchronized Proposal getProposal(final List<ApplicationInfo> ownAppList, final List<ApplicationInfo> partnerAppList,
		final List<ApplicationInfo> leasedAppList) {
		double ownCPUUsage = getCPUUsage(ownAppList);
		double partnerCPUUsage = getCPUUsage(partnerAppList);
		ProposalType pType;
		List<ApplicationInfo> propAppList;
		
		if (ownCPUUsage > partnerCPUUsage) {
			pType = ProposalType.PUSH;
			Collections.sort(ownAppList, new AppCPUComparator());
			propAppList = getAppListToPropose(ownCPUUsage - partnerCPUUsage, ownAppList, leasedAppList);
		} else {
			pType = ProposalType.PULL;
			Collections.sort(partnerAppList, new AppCPUComparator());
			propAppList = getAppListToPropose(partnerCPUUsage - ownCPUUsage, partnerAppList, new ArrayList<ApplicationInfo>());
		}
		
		return new Proposal(pType, propAppList);
	}
}
