package peersim.EP2400.resourcealloc.tasks.placementStartegy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import peersim.EP2400.resourcealloc.tasks.util.AppCPUComparator;
import peersim.EP2400.resourcealloc.tasks.util.ApplicationInfo;
import peersim.EP2400.resourcealloc.tasks.util.Proposal;
import peersim.EP2400.resourcealloc.tasks.util.Proposal.ProposalType;

public class EnergyEfficientStrategy extends PlacementStrategy {
	private static final double	CPU_USAGE_THRESHOLD	= 70;
	
	@Override
	public synchronized Proposal getProposal(final List<ApplicationInfo> ownAppList, final List<ApplicationInfo> partnerAppList,
		final List<ApplicationInfo> leasedAppList) {
		double ownCPUUsage = getCPUUsage(ownAppList);
		double partnerCPUUsage = getCPUUsage(partnerAppList);
		ProposalType pType;
		List<ApplicationInfo> propAppList;
		
		double maxCPUUsage = Math.max(ownCPUUsage, partnerCPUUsage);
		
		if (maxCPUUsage == partnerCPUUsage && partnerCPUUsage < CPU_USAGE_THRESHOLD) {
			pType = ProposalType.PUSH;
			Collections.sort(ownAppList, new AppCPUComparator());
			propAppList = getAppListToPropose(CPU_USAGE_THRESHOLD - partnerCPUUsage, ownAppList, leasedAppList);
		} else if (maxCPUUsage == ownCPUUsage && ownCPUUsage < CPU_USAGE_THRESHOLD) {
			pType = ProposalType.PULL;
			Collections.sort(partnerAppList, new AppCPUComparator());
			propAppList = getAppListToPropose(CPU_USAGE_THRESHOLD - ownCPUUsage, partnerAppList, new ArrayList<ApplicationInfo>());
		} else {
			pType = ProposalType.NO_ACTION;
			propAppList = new ArrayList<ApplicationInfo>();
		}
		
		return new Proposal(pType, propAppList);
	}
}
