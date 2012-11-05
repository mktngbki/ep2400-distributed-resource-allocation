package peersim.EP2400.resourcealloc.tasks.placementStartegy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import peersim.EP2400.resourcealloc.tasks.util.AppCPUComparator;
import peersim.EP2400.resourcealloc.tasks.util.ApplicationInfo;
import peersim.EP2400.resourcealloc.tasks.util.Proposal;
import peersim.EP2400.resourcealloc.tasks.util.Proposal.ProposalType;

public class EnhancedStrategy extends PlacementStrategy {
	private static final double	CPU_USAGE_THRESHOLD	= 70;
	
	@Override
	public synchronized Proposal getProposal(final List<ApplicationInfo> ownAppList, final List<ApplicationInfo> partnerAppList,
		final List<ApplicationInfo> leasedAppList) {
		double ownCPUUsage = getCPUUsage(ownAppList);
		double partnerCPUUsage = getCPUUsage(partnerAppList);
		ProposalType pType;
		List<ApplicationInfo> propAppList;
		
		// Apply Energy Efficient Strategy if both the nodes are not overloaded
		if (ownCPUUsage < CPU_USAGE_THRESHOLD && partnerCPUUsage < CPU_USAGE_THRESHOLD) {
			if (partnerCPUUsage > ownCPUUsage) {
				pType = ProposalType.PUSH;
				Collections.sort(ownAppList, new AppCPUComparator());
				propAppList = getAppListToPropose(CPU_USAGE_THRESHOLD - partnerCPUUsage, ownAppList, leasedAppList);
			} else {
				pType = ProposalType.PULL;
				Collections.sort(partnerAppList, new AppCPUComparator());
				propAppList = getAppListToPropose(CPU_USAGE_THRESHOLD - ownCPUUsage, partnerAppList, new ArrayList<ApplicationInfo>());
			}
		}
		// Apply Load Balance Strategy if one of the nodes is overloaded (Maximize energy efficiency)
		else if (ownCPUUsage > CPU_USAGE_THRESHOLD && partnerCPUUsage < CPU_USAGE_THRESHOLD) {
			pType = ProposalType.PUSH;
			Collections.sort(ownAppList, new AppCPUComparator());
			propAppList = getAppListToPropose(ownCPUUsage - CPU_USAGE_THRESHOLD, ownAppList, leasedAppList, true);
		} else if (partnerCPUUsage > CPU_USAGE_THRESHOLD && ownCPUUsage < CPU_USAGE_THRESHOLD) {
			pType = ProposalType.PULL;
			Collections.sort(partnerAppList, new AppCPUComparator());
			propAppList = getAppListToPropose(partnerCPUUsage - CPU_USAGE_THRESHOLD, partnerAppList, new ArrayList<ApplicationInfo>(), true);
		}
		// Apply pure Load Balance Strategy if both the nodes are overloaded 
		else {
			if (ownCPUUsage > partnerCPUUsage) {
				pType = ProposalType.PUSH;
				Collections.sort(ownAppList, new AppCPUComparator());
				propAppList = getAppListToPropose((ownCPUUsage - partnerCPUUsage) / 2, ownAppList, leasedAppList);
			} else {
				pType = ProposalType.PULL;
				Collections.sort(partnerAppList, new AppCPUComparator());
				propAppList = getAppListToPropose((partnerCPUUsage - ownCPUUsage) / 2, partnerAppList, new ArrayList<ApplicationInfo>());
			}
		}
		
		return new Proposal(pType, propAppList);
	}
}
