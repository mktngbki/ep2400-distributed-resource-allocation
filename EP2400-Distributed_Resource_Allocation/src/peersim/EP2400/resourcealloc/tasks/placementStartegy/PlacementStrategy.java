package peersim.EP2400.resourcealloc.tasks.placementStartegy;

import java.util.List;

import peersim.EP2400.resourcealloc.tasks.util.ApplicationInfo;
import peersim.EP2400.resourcealloc.tasks.util.Proposal;

public interface PlacementStrategy {
	public Proposal getProposal(List<ApplicationInfo> ownAppList, List<ApplicationInfo> partnerAppList, List<ApplicationInfo> leasedAppList);
}
