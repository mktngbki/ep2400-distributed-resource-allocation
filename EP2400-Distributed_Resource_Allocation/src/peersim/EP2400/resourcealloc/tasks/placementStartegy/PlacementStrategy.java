package peersim.EP2400.resourcealloc.tasks.placementStartegy;

import java.util.ArrayList;
import java.util.List;

import peersim.EP2400.resourcealloc.tasks.util.ApplicationInfo;
import peersim.EP2400.resourcealloc.tasks.util.Proposal;

public abstract class PlacementStrategy {
	public abstract Proposal getProposal(final List<ApplicationInfo> ownAppList, final List<ApplicationInfo> partnerAppList,
		final List<ApplicationInfo> leasedAppList);
	
	/**
	 * This method calculate the amount of CPU used by a set of Apps
	 * 
	 * @param appList
	 * @return
	 */
	protected double getCPUUsage(final List<ApplicationInfo> appList) {
		double cpuUsage = 0;
		for (ApplicationInfo appInfo : appList) {
			cpuUsage += appInfo.getApplication().getCPUDemand();
		}
		return cpuUsage;
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
	protected List<ApplicationInfo> getProposedAppList(final double cpuUnits, final List<ApplicationInfo> appList, List<ApplicationInfo> leasedAppList) {
		List<ApplicationInfo> retList = new ArrayList<ApplicationInfo>();
		double usedCPUUnits = 0;
		// first i try to move applications that someone else gave to me because
		// i already paid the virtual cost of moving the app
		// the cost for moving the app in one epoch is 1 if you move it once or
		// move it 30 times
		// because the actual phisical moving is done only at the end of the
		// epoch
		for (ApplicationInfo appInfo : appList) {
			if (appInfo.appMoved() && !leasedAppList.contains(appInfo)) {
				double appCPUDemand = appInfo.getApplication().getCPUDemand();
				if (usedCPUUnits + appCPUDemand <= cpuUnits) {
					retList.add(appInfo);
					usedCPUUnits += appCPUDemand;
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
			}
		}
		
		// all apps from proposal get leased
		leasedAppList.addAll(retList);
		
		return retList;
	}
}
