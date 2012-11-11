package peersim.EP2400.resourcealloc.tasks.simpleStrategy;

import java.util.HashSet;
import java.util.Set;

import peersim.EP2400.resourcealloc.base.Application;
import peersim.EP2400.resourcealloc.base.ApplicationsList;
import peersim.EP2400.resourcealloc.tasks.util.NodeView;

public abstract class Strategy {
	
	protected static class Auxiliary {
		private ApplicationsList listNative;
		private ApplicationsList listReceived;

		public Auxiliary(ApplicationsList listNative, ApplicationsList listReceived) {
			this.listNative = listNative;
			this.listReceived = listReceived;
		}

		public ApplicationsList getListNative() {
			return listNative;
		}
		public ApplicationsList getListMoved() {
			return listReceived;
		}
	}
	
	public static class Result {
		private Set<Application> activeAllocated;
		private Set<Application> activeDeallocated;
		private Set<Integer> activeMovedAppIds;
		
		private Set<Application> passiveAllocated;
		private Set<Application> passiveDeallocated;
		private Set<Integer> passiveMovedAppIds;
		
		public Result() {
			activeAllocated = new HashSet<Application>();
			activeDeallocated = new HashSet<Application>();
			activeMovedAppIds = new HashSet<Integer>();
			
			passiveAllocated = new HashSet<Application>();
			passiveDeallocated = new HashSet<Application>();
			passiveMovedAppIds = new HashSet<Integer>();
		}
		
		public Set<Application> getActiveAllocated() {
			return activeAllocated;
		}
		public void setActiveAllocated(Set<Application> activeAllocated) {
			this.activeAllocated = activeAllocated;
		}
		public Set<Application> getActiveDeallocated() {
			return activeDeallocated;
		}
		public void setActiveDeallocated(Set<Application> activeDeallocated) {
			this.activeDeallocated = activeDeallocated;
		}
		public Set<Integer> getActiveMovedAppIds() {
			return activeMovedAppIds;
		}
		public void setActiveMovedAppIds(Set<Integer> activeMovedAppIds) {
			this.activeMovedAppIds = activeMovedAppIds;
		}
		public Set<Application> getPassiveAllocated() {
			return passiveAllocated;
		}
		public void setPassiveAllocated(Set<Application> passiveAllocated) {
			this.passiveAllocated = passiveAllocated;
		}
		public Set<Application> getPassiveDeallocated() {
			return passiveDeallocated;
		}
		public void setPassiveDeallocated(Set<Application> passiveDeallocated) {
			this.passiveDeallocated = passiveDeallocated;
		}
		public Set<Integer> getPassiveMovedAppIds() {
			return passiveMovedAppIds;
		}
		public void setPassiveMovedAppIds(Set<Integer> passiveMovedAppIds) {
			this.passiveMovedAppIds = passiveMovedAppIds;
		}
	}

	public abstract Result getPlacement(NodeView activeView, NodeView passiveView);
	
	protected Auxiliary splitNativeReceived(NodeView nodeView) {
		ApplicationsList listNative = new ApplicationsList();
		ApplicationsList listReceived = new ApplicationsList();

		//don't change just for iterating
		ApplicationsList finalAppList = nodeView.getAppList(); 
		Set<Integer> finalReceivedAppIds = nodeView.getMovedApps();
		//
		
		for(Application app : finalAppList) {
			if(finalReceivedAppIds.contains(app.getID())) {
				listReceived.add(app);
			} else {
				listNative.add(app);
			}
		}

		return new Auxiliary(listNative, listReceived);
	}
}
