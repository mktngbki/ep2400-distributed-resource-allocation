package peersim.EP2400.resourcealloc.tasks.simpleStrategy;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import peersim.EP2400.resourcealloc.base.Application;
import peersim.EP2400.resourcealloc.base.ApplicationsList;
import peersim.EP2400.resourcealloc.tasks.util.AppCPUComparator;
import peersim.EP2400.resourcealloc.tasks.util.AppListInfo;

public class LoadBalanceStrategy extends Strategy {

	public Result getPlacement(AppListInfo activeList, AppListInfo passiveList) {
		
		Set<Integer> activeMovedAppId = activeList.getMovedApps();
		Set<Integer> passiveMovedAppId = passiveList.getMovedApps();

		Auxiliary activeSplitResult = splitNativeReceived(activeList.getAppList(), activeMovedAppId);
		Auxiliary passiveSplitResult = splitNativeReceived(passiveList.getAppList(), passiveMovedAppId);

		ApplicationsList activeNative = activeSplitResult.getListNative();
		ApplicationsList passiveNative = passiveSplitResult.getListNative();
		ApplicationsList movedApps = new ApplicationsList();
		movedApps.addAll(activeSplitResult.getListReceived());
		movedApps.addAll(passiveSplitResult.getListReceived());

		double activeNativeCPU = activeNative.totalCPUDemand();
		double passiveNativeCPU = passiveNative.totalCPUDemand();
		double movedCPU = movedApps.totalCPUDemand();

		//TODO figure in report
		if(Math.abs(activeNativeCPU - passiveNativeCPU) > movedCPU) {
			double transferCPU = (activeNativeCPU - passiveNativeCPU - movedCPU)/2;
			//this sorts the list in revers, bigger cpu utilization app is first
			
			ApplicationsList higherApps = new ApplicationsList();
			ApplicationsList lowerApps = new ApplicationsList();
			Set<Integer> higherMovedAppIds = new HashSet<Integer>();
			Set<Integer> lowerMovedAppIds = new HashSet<Integer>();
			
			if(activeNativeCPU > passiveNativeCPU) {
				higherApps.addAll(activeNative);
				lowerApps.addAll(passiveNative);
			} else {
				higherApps.addAll(passiveNative);
				lowerApps.addAll(activeNative);
			}
			
			Collections.sort(higherApps, new AppCPUComparator());
			Iterator<Application> it = higherApps.iterator();
			while(it.hasNext()) {
				if(0 == transferCPU) {
					break;
				}
				Application app = it.next();
				if(transferCPU >= app.getCPUDemand()) {
					it.remove();
					lowerApps.add(app);
					lowerMovedAppIds.add(app.getID());
					transferCPU = transferCPU - app.getCPUDemand();
				}
			}

			lowerApps.addAll(movedApps);
			lowerMovedAppIds.addAll(activeMovedAppId);
			lowerMovedAppIds.addAll(passiveMovedAppId);

			Result result;
			if(activeNativeCPU > passiveNativeCPU) {
				result = new Result(new AppListInfo(higherApps), new AppListInfo(lowerApps, lowerMovedAppIds));
			} else {
				result = new Result(new AppListInfo(lowerApps, lowerMovedAppIds), new AppListInfo(higherApps));			
			}
			return result;
		} else {
			Collections.sort(movedApps, new AppCPUComparator());

			activeMovedAppId = new HashSet<Integer>();
			passiveMovedAppId = new HashSet<Integer>();
			ApplicationsList activeApps = new ApplicationsList();
			activeApps.addAll(activeNative);
			ApplicationsList passiveApps = new ApplicationsList();
			passiveApps.addAll(passiveNative);

			double activeCPU = activeNativeCPU;
			double passiveCPU = passiveNativeCPU;

			for(Application app : movedApps) {
				if(activeCPU > passiveCPU) {
					activeApps.add(app);
					activeMovedAppId.add(app.getID());
				} else {
					passiveApps.add(app);
					passiveMovedAppId.add(app.getID());
				}
			}
			
			Result result = new Result(new AppListInfo(activeApps, activeMovedAppId), new AppListInfo(passiveApps, passiveMovedAppId));
			return result;
		}
	}
}
