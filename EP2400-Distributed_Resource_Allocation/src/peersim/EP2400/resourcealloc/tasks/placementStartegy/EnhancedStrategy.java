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
	public Proposal getProposal(final List<ApplicationInfo> ownAppList, final List<ApplicationInfo> partnerAppList, final List<ApplicationInfo> leasedAppList) {
		double ownCPUUsage = getCPUUsage(ownAppList);
		double partnerCPUUsage = getCPUUsage(partnerAppList);
		ProposalType pType;
		List<ApplicationInfo> propAppList;
		
		// Apply Energy Efficient Strategy if both the nodes are not overloaded
		if (ownCPUUsage < CPU_USAGE_THRESHOLD && partnerCPUUsage < CPU_USAGE_THRESHOLD) {
			if (partnerCPUUsage == Math.max(ownCPUUsage, partnerCPUUsage)) {
				pType = ProposalType.PUSH;
				Collections.sort(ownAppList, new AppCPUComparator());
				propAppList = getProposedAppList(CPU_USAGE_THRESHOLD - partnerCPUUsage, ownAppList, leasedAppList);
			} else {
				pType = ProposalType.PULL;
				Collections.sort(partnerAppList, new AppCPUComparator());
				propAppList = getProposedAppList(CPU_USAGE_THRESHOLD - ownCPUUsage, partnerAppList, new ArrayList<ApplicationInfo>());
			}
		}
		// Apply Load Balance Strategy if one of the nodes is overloaded
		else if (ownCPUUsage > CPU_USAGE_THRESHOLD && partnerCPUUsage < CPU_USAGE_THRESHOLD) {
			pType = ProposalType.PUSH;
			Collections.sort(ownAppList, new AppCPUComparator());
			propAppList = getProposedAppList(ownCPUUsage - CPU_USAGE_THRESHOLD, ownAppList, leasedAppList);
		} else if (partnerCPUUsage > CPU_USAGE_THRESHOLD && ownCPUUsage < CPU_USAGE_THRESHOLD) {
			pType = ProposalType.PULL;
			Collections.sort(partnerAppList, new AppCPUComparator());
			propAppList = getProposedAppList(partnerCPUUsage - CPU_USAGE_THRESHOLD, partnerAppList, new ArrayList<ApplicationInfo>());
		}
		// Do nothing if both the nodes are overloaded
		else {
			pType = ProposalType.NO_ACTION;
			propAppList = new ArrayList<ApplicationInfo>();
		}
		
		return new Proposal(pType, propAppList);
	}
}
