package peersim.EP2400.resourcealloc.tasks.simpleStrategy;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import peersim.EP2400.resourcealloc.base.Application;
import peersim.EP2400.resourcealloc.base.ApplicationsList;
import peersim.EP2400.resourcealloc.tasks.util.AppCPUComparator;
import peersim.EP2400.resourcealloc.tasks.util.AppListInfo;

public class EnergyEfficiencyStrategy extends Strategy {

	private double cpuCapacity;
	
	public EnergyEfficiencyStrategy(double cpuCapacity) {
		this.cpuCapacity = cpuCapacity;
	}
	
	@Override
	public Result getPlacement(AppListInfo activeList, AppListInfo passiveList) {
		
		Set<Integer> activeMovedAppIds = activeList.getMovedApps();
		Set<Integer> passiveMovedAppIds = passiveList.getMovedApps();

		Auxiliary activeSplitResult = splitNativeReceived(activeList.getAppList(), activeMovedAppIds);
		Auxiliary passiveSplitResult = splitNativeReceived(passiveList.getAppList(), passiveMovedAppIds);

		ApplicationsList activeNativeApps = activeSplitResult.getListNative();
		ApplicationsList pasiveNativeApps = passiveSplitResult.getListNative();
		ApplicationsList movedApps = new ApplicationsList();
		movedApps.addAll(activeSplitResult.getListMoved());
		movedApps.addAll(passiveSplitResult.getListMoved());

		double activeNativeCPU = activeNativeApps.totalCPUDemand();
		double passiveNativeCPU = pasiveNativeApps.totalCPUDemand();

		//this list we try to get it as close as we can to cpuCapacity, without going over tau
		ApplicationsList maxApps = new ApplicationsList();
		Set<Integer> maxMovedApps = new HashSet<Integer>();
		double maxCPU;
		//this list we try to get it as close as we can to 0 so that we can shut down the server
		ApplicationsList minApps = new ApplicationsList();
		Set<Integer> minMovedApps = new HashSet<Integer>();

		if(activeNativeCPU > tau) {
			//move max and then see
		} else if(passiveNativeCPU > tau) {

		}
		if(activeNativeCPU > passiveNativeCPU) {
			maxApps.addAll(activeNativeApps);
			maxCPU = activeNativeCPU;
			minApps.addAll(pasiveNativeApps);
		} else {
			maxApps.addAll(pasiveNativeApps);
			maxCPU = passiveNativeCPU;
			minApps.addAll(activeNativeApps);
		}

		//first we try to move as many of the moved apps to the maximized node
		Collections.sort(movedApps, new AppCPUComparator());
		Iterator<Application> it = movedApps.iterator();
		while(it.hasNext()) {
			Application app = it.next();
			if(maxCPU + app.getCPUDemand() <= tau) {
				maxCPU = maxCPU + app.getCPUDemand();
				maxApps.add(app);
				maxMovedApps.add(app.getID());
				it.remove();
			}
		}

		//now we try to move any native apps to the maximized node if possible
		if(maxCPU < tau) {
			it = minApps.iterator();
			while(it.hasNext()) {
				Application app = it.next();
				if(maxCPU + app.getCPUDemand() <= tau) {
					maxCPU = maxCPU + app.getCPUDemand();
					maxApps.add(app);
					maxMovedApps.add(app.getID());
					it.remove();
				}
			}
		}

		//add all of the moved apps that cannot fit to the maximized node to the minimized node
		it = movedApps.iterator();
		while(it.hasNext()) {
			Application app = it.next();
			minApps.add(app);
			minMovedApps.add(app.getID());
			it.remove();
		}

		AppListInfo list1Result;
		AppListInfo list2Result;
		if(activeNativeCPU > passiveNativeCPU) {
			list1Result = new AppListInfo(maxApps, maxMovedApps);
			list2Result = new AppListInfo(minApps, minMovedApps);
		} else {
			list1Result = new AppListInfo(minApps, minMovedApps);
			list2Result = new AppListInfo(maxApps, maxMovedApps);
		}

		return new Result(list1Result, list2Result);
	}

}
