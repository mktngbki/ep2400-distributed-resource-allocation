package peersim.EP2400.resourcealloc.tasks.placementStartegy;

import java.util.Collection;
import java.util.Collections;

import peersim.EP2400.resourcealloc.base.Application;
import peersim.EP2400.resourcealloc.base.ApplicationsList;
import peersim.EP2400.resourcealloc.tasks.util.AppCPUComparator;
import peersim.EP2400.resourcealloc.tasks.util.Proposal;

public abstract class PlacementStrategy {
	public abstract Proposal getProposal(final ApplicationsList ownAppList, final ApplicationsList partnerAppList, final Collection<Integer> ownReceivedApps,
		final Collection<Integer> partnerReceivedApps, final Collection<Integer> ownPromisedApps);
	
	public synchronized Proposal processProposal(final Proposal receivedProposal, final ApplicationsList ownAppList, final Collection<Integer> ownPromisedApps) {
		// Override when required
		return receivedProposal;
	}
	
	/**
	* This method builds the list of Apps to propose
	* 
	* @param cpuUnits
	* @param appList
	* @param promisedAppList
	*            - contains apps that i promised to give to someone else. I do
	*            at most one promise on each app. Once I promise an app I do
	*            not propose it to someone else
	* @return
	*/
	protected ApplicationsList getAppListToPropose(final double cpuUnits, final ApplicationsList appList, final Collection<Integer> promisedApps,
		final Collection<Integer> receivedApps) {
		return getAppListToPropose(cpuUnits, appList, promisedApps, receivedApps, false);
	}
	
	protected ApplicationsList getAppListToPropose(final double cpuUnits, final ApplicationsList appList, final Collection<Integer> promisedApps,
		final Collection<Integer> receivedApps, final boolean returnExtraSmallestApp) {
		double usedCPUUnits = 0;
		ApplicationsList retList = new ApplicationsList();
		ApplicationsList smallestApp = new ApplicationsList();
		
		Collections.sort(appList, new AppCPUComparator());
		
		// Try to move applications that someone else gave to the node in order to minimize the reconfiguration cost
		for (Application app : appList) {
			if (!promisedApps.contains(app) && receivedApps.contains(app)) {
				double appCPUDemand = app.getCPUDemand();
				if (usedCPUUnits + appCPUDemand <= cpuUnits) {
					retList.add(app);
					usedCPUUnits += appCPUDemand;
				}
			}
		}
		
		// if i can add more applications of my own I add them
		for (Application app : appList) {
			if (!promisedApps.contains(app) && !receivedApps.contains(app)) {
				double appCPUDemand = app.getCPUDemand();
				if (usedCPUUnits + appCPUDemand <= cpuUnits) {
					retList.add(app);
					usedCPUUnits += appCPUDemand;
				}
			}
		}
		
		if(usedCPUUnits < cpuUnits && returnExtraSmallestApp) {
			
		}
		if (smallestApp.getApplication().getCPUDemand() > appCPUDemand) {
			smallestApp = app;
		}
		
		if (retList.isEmpty() && returnExtraSmallestApp && smallestApp.getApplication().getCPUDemand() != Integer.MAX_VALUE) {
			retList.add(smallestApp);
		}
		
		// all apps from proposal get leased
		leasedAppList.addAll(retList);
		
		return retList;
	}
}
