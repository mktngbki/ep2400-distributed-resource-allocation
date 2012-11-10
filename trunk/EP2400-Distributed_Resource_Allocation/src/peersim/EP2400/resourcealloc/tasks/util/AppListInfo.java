package peersim.EP2400.resourcealloc.tasks.util;

import java.util.HashSet;
import java.util.Set;

import peersim.EP2400.resourcealloc.base.ApplicationsList;

public class AppListInfo {
	private ApplicationsList appList;
	private Set<Integer> receivedApps;
	
	public AppListInfo(ApplicationsList appList) {
		this.appList = appList;
		this.receivedApps = new HashSet<Integer>();
	}
	
	public AppListInfo(ApplicationsList appList, Set<Integer> receivedApps) {
		this.appList = appList;
		this.receivedApps = receivedApps;
	}

	public ApplicationsList getAppList() {
		return appList;
	}

	public Set<Integer> getMovedApps() {
		return receivedApps;
	}
}
