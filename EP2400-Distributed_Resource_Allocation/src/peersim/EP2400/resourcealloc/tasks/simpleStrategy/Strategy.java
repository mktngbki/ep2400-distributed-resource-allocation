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
	
	protected static class Result {
		private AppListInfo activeResult;
		private AppListInfo passiveResult;

		public Result(AppListInfo activeResult, AppListInfo passiveResult) {
			this.activeResult = activeResult;
			this.passiveResult = passiveResult;
		}

		public AppListInfo getActiveResult() {
			return activeResult;
		}

		public AppListInfo getPassiveResult() {
			return passiveResult;
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
