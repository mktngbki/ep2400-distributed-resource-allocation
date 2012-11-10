package peersim.EP2400.resourcealloc.tasks.simpleStrategy;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.xml.transform.Result;

import peersim.EP2400.resourcealloc.base.Application;
import peersim.EP2400.resourcealloc.base.ApplicationsList;
import peersim.EP2400.resourcealloc.tasks.simpleStrategy.Strategy.Auxiliary;
import peersim.EP2400.resourcealloc.tasks.util.AppCPUComparator;
import peersim.EP2400.resourcealloc.tasks.util.AppListInfo;

public class EnhancedStrategy implements Strategy {

	public final static double tau = 80;
	

	@Override
	public AppListInfo getPlacement(AppListInfo myAppListInfo, AppListInfo neighborAppListInfo) {
		AppListInfo higherCPUDemandList;
		AppListInfo lowerCPUDemandList;
		double higherCPUDemand;
		double lowerCPUDemand;

		double myCPUDemand = myAppListInfo.getAppList().totalCPUDemand();
		double neighborCPUDemand = neighborAppListInfo.getAppList().totalCPUDemand();

		if(myCPUDemand > neighborCPUDemand) {
			higherCPUDemandList = myAppListInfo;
			higherCPUDemand = myCPUDemand;
			lowerCPUDemandList = neighborAppListInfo;
			lowerCPUDemand = neighborCPUDemand;
		} else {
			higherCPUDemandList = neighborAppListInfo;
			higherCPUDemand = neighborCPUDemand;
			lowerCPUDemandList = myAppListInfo;
			lowerCPUDemand = myCPUDemand;
		}

		Result result;
		//both nodes have cpu utilization above tau
		if(myCPUDemand > tau && neighborCPUDemand > tau) {
			result = applySimpleLoadBalance(higherCPUDemandList, higherCPUDemand, 
					lowerCPUDemandList, lowerCPUDemand);
		} //both nodes have cpu utilization below tau 
		else if(myCPUDemand <= tau && neighborCPUDemand <= tau) {
			result = applySimpleEnergyEfficiency(higherCPUDemandList, higherCPUDemand, 
					lowerCPUDemandList, lowerCPUDemand);
		} //one of the nodes has less than tau utilization and the other one above tau 
		else {
			if(myCPUDemand + neighborCPUDemand > 2 * tau) {
				result = applySimpleLoadBalance(higherCPUDemandList, higherCPUDemand, 
						lowerCPUDemandList, lowerCPUDemand);
			} else {
				result = applyEnhancedStrategy(higherCPUDemandList, higherCPUDemand, 
						lowerCPUDemandList, lowerCPUDemand);
			}
		}

		if(myCPUDemand > neighborCPUDemand) {
			return result.getActiveResult();
		} else {
			return result.getPassiveResult();
		}
	}

	//tries to make both lists have a cpu utilization as close as possible to their average
	private Result applySimpleLoadBalance(AppListInfo higherCPUDemandList, double higherCPUDemand,
			AppListInfo lowerCPUDemandList, double lowerCPUDemand) {

		Set<Integer> higherMovedAppId = higherCPUDemandList.getMovedApps();
		Set<Integer> lowerMovedAppId = lowerCPUDemandList.getMovedApps();

		Auxiliary higherSplitResult = splitNativeReceived(higherCPUDemandList.getAppList(), higherMovedAppId);
		Auxiliary lowerSplitResult = splitNativeReceived(lowerCPUDemandList.getAppList(), lowerMovedAppId);

		ApplicationsList higherNative = higherSplitResult.getListNative();
		ApplicationsList lowerNative = lowerSplitResult.getListNative();
		ApplicationsList movedApps = new ApplicationsList();
		movedApps.addAll(higherSplitResult.getListReceived());
		movedApps.addAll(lowerSplitResult.getListReceived());

		double higherNativeCPU = higherNative.totalCPUDemand();
		double lowerNativeCPU = lowerNative.totalCPUDemand();
		double movedCPU = movedApps.totalCPUDemand();

		//TODO figure in report
		if(Math.abs(higherNativeCPU - lowerNativeCPU) > movedCPU) {
			double transferCPU = (higherNativeCPU - lowerNativeCPU - movedCPU)/2;
			//this sorts the list in revers, bigger cpu utilization app is first
			ApplicationsList higherApps = new ApplicationsList();
			higherApps.addAll(higherNative);
			ApplicationsList lowerApps = new ApplicationsList();
			lowerApps.addAll(lowerNative);
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
					lowerMovedAppId.add(app.getID());
					transferCPU = transferCPU - app.getCPUDemand();
				}
			}

			lowerApps.addAll(movedApps);
			lowerMovedAppId.addAll(higherMovedAppId);

			AppListInfo higherResult = new AppListInfo(higherApps);
			AppListInfo lowerResult = new AppListInfo(lowerApps, lowerMovedAppId);
			return new Result(higherResult, lowerResult);
		} else {
			Collections.sort(movedApps, new AppCPUComparator());

			higherMovedAppId = new HashSet<Integer>();
			lowerMovedAppId = new HashSet<Integer>();
			ApplicationsList higherApps = new ApplicationsList();
			higherApps.addAll(higherNative);
			ApplicationsList lowerApps = new ApplicationsList();
			lowerApps.addAll(lowerNative);

			double higherCPU = higherNativeCPU;
			double lowerCPU = lowerNativeCPU;

			for(Application app : movedApps) {
				if(higherCPU > lowerCPU) {
					higherApps.add(app);
					higherMovedAppId.add(app.getID());
				} else {
					lowerApps.add(app);
					lowerMovedAppId.add(app.getID());
				}
			}
			AppListInfo higherResult = new AppListInfo(higherApps, higherMovedAppId);
			AppListInfo lowerResult = new AppListInfo(lowerApps, lowerMovedAppId);
			return new Result(higherResult, lowerResult);
		}
	}

	private Result applySimpleEnergyEfficiency(AppListInfo list1, double cpuDemand1, 
			AppListInfo list2, double cpuDemand2) {

			}

	private Result applyEnhancedStrategy(AppListInfo higherCPUDemandList, double higherCPUDemand, 
			AppListInfo lowerCPUDemandList, double lowerCPUDemand) {

		Auxiliary higherSplitResult = splitNativeReceived(higherCPUDemandList.getAppList(), higherCPUDemandList.getMovedApps());
		Auxiliary lowerSplitResult = splitNativeReceived(lowerCPUDemandList.getAppList(), lowerCPUDemandList.getMovedApps());

		return null;
	}
}
