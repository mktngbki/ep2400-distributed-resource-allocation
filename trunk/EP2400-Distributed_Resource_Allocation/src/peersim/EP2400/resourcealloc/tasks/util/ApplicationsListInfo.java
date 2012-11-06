package peersim.EP2400.resourcealloc.tasks.util;

import java.util.Set;

import peersim.EP2400.resourcealloc.base.ApplicationsList;

public class ApplicationsListInfo {
	private ApplicationsList appList;
	private Set<Integer> receivedApps; // apps that i receive so from the reconfiguration cost, i already paid 1 for it, so moving further does not cost us extra
	
	public ApplicationsListInfo(ApplicationsList appList, Set<Integer> receivedApps) {
		this.appList = appList;
		this.receivedApps = receivedApps;
	}

	public ApplicationsList getAppList() {
		return appList;
	}

	public Set<Integer> getReceivedApps() {
		return receivedApps;
	}
	
}
