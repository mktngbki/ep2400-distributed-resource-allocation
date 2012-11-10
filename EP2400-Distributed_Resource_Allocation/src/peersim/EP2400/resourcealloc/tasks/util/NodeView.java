package peersim.EP2400.resourcealloc.tasks.util;

import java.util.HashSet;
import java.util.Set;

import peersim.EP2400.resourcealloc.base.ApplicationsList;

public class NodeView {
	private ApplicationsList	appList;
	private Set<Integer>		receivedApps;
	private double				currentSystemLoadView;
	
	public NodeView(ApplicationsList appList, double currentSystemLoadView) {
		this.appList = appList;
		receivedApps = new HashSet<Integer>();
		this.currentSystemLoadView = currentSystemLoadView;
	}
	
	public NodeView(ApplicationsList appList, Set<Integer> receivedApps, double currentSystemLoadView) {
		this.appList = appList;
		this.receivedApps = receivedApps;
		this.currentSystemLoadView = currentSystemLoadView;
	}
	
	public ApplicationsList getAppList() {
		return appList;
	}
	
	public Set<Integer> getMovedApps() {
		return receivedApps;
	}
	
	public double getCurrentSystemLoadView() {
		return currentSystemLoadView;
	}
}
