package peersim.EP2400.resourcealloc.tasks.placementStartegy;

import java.util.Collections;
import java.util.Set;

import peersim.EP2400.resourcealloc.base.Application;
import peersim.EP2400.resourcealloc.base.ApplicationsList;
import peersim.EP2400.resourcealloc.tasks.util.AppCPUComparator;
import peersim.EP2400.resourcealloc.tasks.util.Proposal;

public abstract class PlacementStrategy {
	public abstract Proposal getProposal(final ApplicationsList ownAppList, final ApplicationsList partnerAppList, final Set<Integer> ownReceivedApps,
		final Set<Integer> partnerReceivedApps, final Set<Integer> ownPromisedApps);
	
	public Proposal processProposal(final Proposal receivedProposal, final ApplicationsList ownAppList, final Set<Integer> ownPromisedApps) {
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
	protected ApplicationsList getAppListToPropose(final double cpuUnits, final ApplicationsList appList, final Set<Integer> receivedApps, 
			final Set<Integer> promisedApps) {
		return getAppListToPropose(cpuUnits, appList, promisedApps, receivedApps, false);
	}
	
	protected ApplicationsList getAppListToPropose(final double cpuUnits, final ApplicationsList appList, final Set<Integer> receivedApps, 
			final Set<Integer> promisedApps, final boolean returnExtraSmallestApp) {
		double usedCPUUnits = 0;
		ApplicationsList retList = new ApplicationsList();
		
		Collections.sort(appList, new AppCPUComparator());
		Application smallestNotPromisedApp = null;
		
		// Try to move applications that someone else gave to the node in order to minimize the reconfiguration cost
		for (Application app : appList) {
			if (!promisedApps.contains(app) && receivedApps.contains(app)) {
				double appCPUDemand = app.getCPUDemand();
				if (usedCPUUnits + appCPUDemand <= cpuUnits) {
					retList.add(app);
					promisedApps.add(app.getID());
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
					promisedApps.add(app.getID());
					usedCPUUnits += appCPUDemand;
				} 
			}
		}
		
		for(Application app : appList) {
			if(!promisedApps.contains(app)) {
				smallestNotPromisedApp = app;
			}
		}
		
		if(usedCPUUnits < cpuUnits && returnExtraSmallestApp && null != smallestNotPromisedApp) {
			if(!promisedApps.contains(smallestNotPromisedApp)) {
				retList.add(smallestNotPromisedApp);
				promisedApps.add(smallestNotPromisedApp.getID());
			}
		}
		
		return retList;
	}
}
