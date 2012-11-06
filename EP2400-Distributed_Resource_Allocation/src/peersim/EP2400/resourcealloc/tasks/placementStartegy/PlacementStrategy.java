package peersim.EP2400.resourcealloc.tasks.placementStartegy;

import java.util.Set;

import peersim.EP2400.resourcealloc.base.Application;
import peersim.EP2400.resourcealloc.base.ApplicationsList;
import peersim.EP2400.resourcealloc.tasks.util.Proposal;

public abstract class PlacementStrategy {
	public abstract Proposal getProposal(final ApplicationsList ownAppList, final ApplicationsList partnerAppList, final Set<Integer> ownReceivedApps,
		final Set<Integer> partnerReceivedApps, final Set<Integer> ownPromisedApps);
	
	public synchronized Proposal processProposal(final Proposal receivedProposal, final ApplicationsList ownAppList, final Set<Integer> ownPromisedApps) {
		// Override when required
		return receivedProposal;
	}
	
	/**
	* This method builds the list of Apps to propose
	* 
	* @param cpuUnits
	* @param appList
	* @param leasedAppList
	*            - contains apps that i promised to give to someone else. I do
	*            at most one promise on each app. Once I promise an app I do
	*            not propose it to someone else
	* @return
	*/
	protected ApplicationsList getAppListToPropose(final double cpuUnits, final ApplicationsList appList, final Set<Integer> promisedApps,
		final Set<Integer> receivedApps) {
		return getAppListToPropose(cpuUnits, appList, promisedApps, receivedApps, false);
	}
	
	protected ApplicationsList getAppListToPropose(final double cpuUnits, final ApplicationsList appList, final Set<Integer> promisedApps,
		final Set<Integer> receivedApps, final boolean returnAtLeastSmallestApp) {
		double usedCPUUnits = 0;
		ApplicationsList retList = new ApplicationsList();
		ApplicationsList smallestApp = new ApplicationsList();
		
		// Try to move applications that someone else gave to the node in order to minimize the reconfiguration cost
		for (Application appInfo : appList) {
			if (!leasedAppList.contains(appInfo)) {
				double appCPUDemand = appInfo.getApplication().getCPUDemand();
				if (usedCPUUnits + appCPUDemand <= cpuUnits) {
					retList.add(appInfo);
					usedCPUUnits += appCPUDemand;
				}
				
				if (smallestApp.getApplication().getCPUDemand() > appCPUDemand) {
					smallestApp = appInfo;
				}
			}
		}
		
		// i search to find apps of my own that are small enough to add in the
		// list
		for (ApplicationInfo appInfo : appList) {
			if (!appInfo.appMoved() && !leasedAppList.contains(appInfo)) {
				double appCPUDemand = appInfo.getApplication().getCPUDemand();
				if (usedCPUUnits + appCPUDemand <= cpuUnits) {
					retList.add(appInfo);
					usedCPUUnits += appCPUDemand;
				}
				
				if (smallestApp.getApplication().getCPUDemand() > appCPUDemand) {
					smallestApp = appInfo;
				}
			}
		}
		
		if (retList.isEmpty() && returnAtLeastSmallestApp && smallestApp.getApplication().getCPUDemand() != Integer.MAX_VALUE) {
			retList.add(smallestApp);
		}
		
		// all apps from proposal get leased
		leasedAppList.addAll(retList);
		
		return retList;
	}
}
