package peersim.EP2400.resourcealloc.tasks.simpleStrategy;

import java.util.Set;

import peersim.EP2400.resourcealloc.base.Application;
import peersim.EP2400.resourcealloc.base.ApplicationsList;
import peersim.EP2400.resourcealloc.tasks.util.AppListInfo;

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
		public ApplicationsList getListReceived() {
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

	public abstract Result getPlacement(AppListInfo activeList, AppListInfo passiveList);
	
	protected Auxiliary splitNativeReceived(ApplicationsList appList, Set<Integer> receivedApps) {
		ApplicationsList listNative = new ApplicationsList();
		ApplicationsList listReceived = new ApplicationsList();

		for(Application app : appList) {
			if(receivedApps.contains(app.getID())) {
				listReceived.add(app);
			} else {
				listNative.add(app);
			}
		}

		return new Auxiliary(listNative, listReceived);
	}
}
