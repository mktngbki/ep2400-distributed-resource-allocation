package peersim.EP2400.resourcealloc.tasks.placementStartegy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import peersim.EP2400.resourcealloc.tasks.util.AppCPUComparator;
import peersim.EP2400.resourcealloc.tasks.util.ApplicationInfo;
import peersim.EP2400.resourcealloc.tasks.util.Proposal;
import peersim.EP2400.resourcealloc.tasks.util.Proposal.ProposalType;

public class LoadBalanceStrategy implements PlacementStrategy {
	
	@Override
	public Proposal getProposal(List<ApplicationInfo> ownAppList,
			List<ApplicationInfo> partnerAppList,
			List<ApplicationInfo> leasedAppList) {
		
		double ownCPUUsage = getCPUUsage(ownAppList);
		double partnerCPUUsage = getCPUUsage(partnerAppList);
		ProposalType pType;
		List<ApplicationInfo> propAppList;
		
		if(ownCPUUsage > partnerCPUUsage) {
			pType = ProposalType.PUSH;
			Collections.sort(ownAppList, new AppCPUComparator());
			propAppList = getProposedAppList(ownCPUUsage - partnerCPUUsage, ownAppList, leasedAppList);
		} else {
			pType = ProposalType.PULL;
			Collections.sort(partnerAppList, new AppCPUComparator());
			propAppList = getProposedAppList(partnerCPUUsage - ownCPUUsage, partnerAppList, new ArrayList<ApplicationInfo>());
		}
		
		return new Proposal(pType, propAppList);
	}

	private double getCPUUsage(List<ApplicationInfo> appList) {
		double cpuUsage = 0;
		for(ApplicationInfo appInfo : appList) {
			cpuUsage += appInfo.getApplication().getCPUDemand();
		}
		return cpuUsage;
	}
	
	/**
	 * this is a very simple first implementation.
	 * appList is expected to be sorted using the AppCPUComparator
	 * 
	 * @param cpuUnits
	 * @param appList
	 * @param leasedAppList - contains apps that i promised to give to someone else. I do at most one promise on each app. Once I promise an app I do not propose it to someone else
	 * @return
	 */
	private List<ApplicationInfo> getProposedAppList(double cpuUnits, List<ApplicationInfo> appList, List<ApplicationInfo> leasedAppList) {
		List<ApplicationInfo> retList = new ArrayList<ApplicationInfo>();
		double usedCPUUnits = 0;
		//first i try to move applications that someone else gave to me because i already paid the virtual cost of moving the app
		//the cost for moving the app in one epoch is 1 if you move it once or move it 30 times
		//because the actual phisical moving is done only at the end of the epoch
		for(ApplicationInfo appInfo : appList) {
			if(appInfo.appMoved() && !leasedAppList.contains(appInfo)) {
				double appCPUDemand = appInfo.getApplication().getCPUDemand(); 
				if(usedCPUUnits + appCPUDemand <= cpuUnits) {
					retList.add(appInfo);
					usedCPUUnits += appCPUDemand;
				}
			}
		}
		
		//i search to find apps of my own that are small enough to add in the list
		for(ApplicationInfo appInfo : appList) {
			if(!appInfo.appMoved() && !leasedAppList.contains(appInfo)) {
				double appCPUDemand = appInfo.getApplication().getCPUDemand(); 
				if(usedCPUUnits + appCPUDemand <= cpuUnits) {
					retList.add(appInfo);
					usedCPUUnits += appCPUDemand;
				}
			}
		}
		
		//all apps from proposal get leased
		leasedAppList.addAll(retList);
		
		return retList;
	}

}
