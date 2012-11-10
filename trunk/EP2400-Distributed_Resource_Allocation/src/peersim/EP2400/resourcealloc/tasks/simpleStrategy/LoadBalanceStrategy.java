package peersim.EP2400.resourcealloc.tasks.simpleStrategy;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import peersim.EP2400.resourcealloc.base.Application;
import peersim.EP2400.resourcealloc.base.ApplicationsList;
import peersim.EP2400.resourcealloc.tasks.util.AppCPUComparator;
import peersim.EP2400.resourcealloc.tasks.util.NodeView;

public class LoadBalanceStrategy extends Strategy {

	public Result getPlacement(NodeView activeView, NodeView passiveView) {
		
		final Set<Integer> activeMovedAppIds = activeView.getMovedApps();
		final Set<Integer> passiveMovedAppIds = passiveView.getMovedApps();

		final Auxiliary activeSplitResult = splitNativeReceived(activeView.getAppList(), activeMovedAppIds);
		final Auxiliary passiveSplitResult = splitNativeReceived(passiveView.getAppList(), passiveMovedAppIds);

		final double activeNativeCPU = activeSplitResult.getListNative().totalCPUDemand();
		final double passiveNativeCPU = passiveSplitResult.getListNative().totalCPUDemand();
		final double activeMovedCPU = activeSplitResult.getListMoved().totalCPUDemand();
		final double passiveMovedCPU = passiveSplitResult.getListMoved().totalCPUDemand();

		//TODO figure in report
		if(Math.abs(activeNativeCPU - passiveNativeCPU) > (activeMovedCPU + passiveMovedCPU)) {
			double transferCPU = (Math.abs(activeNativeCPU - passiveNativeCPU) - activeMovedCPU - passiveMovedCPU)/2;
			
			Set<Integer> lowerMovedAppIds = new HashSet<Integer>();
			Set<Application> higherAllocated = new HashSet<Application>();
			Set<Application> higherDeallocated = new HashSet<Application>();
			Set<Application> lowerAllocated = new HashSet<Application>();
			Set<Application> lowerDeallocated = new HashSet<Application>();
			ApplicationsList higherApps = new ApplicationsList();
			
			if(activeNativeCPU > passiveNativeCPU) {
				higherApps.addAll(activeSplitResult.getListNative());
				higherDeallocated.addAll(activeSplitResult.getListMoved());
				lowerAllocated.addAll(activeSplitResult.getListMoved());
			} else {
				higherApps.addAll(passiveSplitResult.getListNative());
				higherDeallocated.addAll(passiveSplitResult.getListMoved());
				lowerAllocated.addAll(passiveSplitResult.getListMoved());
			}
			lowerMovedAppIds.addAll(activeMovedAppIds);
			lowerMovedAppIds.addAll(passiveMovedAppIds);
			
			Collections.sort(higherApps, new AppCPUComparator());
			Iterator<Application> it = higherApps.iterator();
			while(it.hasNext()) {
				if(0 == transferCPU) {
					break;
				}
				Application app = it.next();
				if(transferCPU >= app.getCPUDemand()) {
					higherDeallocated.add(app);
					
					lowerAllocated.add(app);
					lowerMovedAppIds.add(app.getID());
					
					transferCPU = transferCPU - app.getCPUDemand();
				}
			}
			
			Result result = new Result();
			if(activeNativeCPU > passiveNativeCPU) {
				result.setActiveAllocated(higherAllocated);
				result.setActiveDeallocated(higherDeallocated);
				result.setActiveMovedAppIds(new HashSet<Integer>());
				result.setPassiveAllocated(lowerAllocated);
				result.setPassiveDeallocated(lowerDeallocated);
				result.setPassiveMovedAppIds(lowerMovedAppIds);
			} else {
				result.setActiveAllocated(lowerAllocated);
				result.setActiveDeallocated(lowerDeallocated);
				result.setActiveMovedAppIds(lowerMovedAppIds);
				result.setPassiveAllocated(higherAllocated);
				result.setPassiveDeallocated(higherDeallocated);
				result.setPassiveMovedAppIds(new HashSet<Integer>());
			}
			return result;
		} else {
			Set<Integer> newActiveMovedAppIds = new HashSet<Integer>();
			Set<Integer> newPassiveMovedAppIds = new HashSet<Integer>();
			
			Set<Application> activeAllocated = new HashSet<Application>();
			Set<Application> activeDeallocated = new HashSet<Application>();
			Set<Application> passiveAllocated = new HashSet<Application>();
			Set<Application> passiveDeallocated = new HashSet<Application>();
			
			double activeCPU = activeNativeCPU;
			double passiveCPU = passiveNativeCPU;

			//this sorts the list in revers, bigger cpu utilization app is first
			ApplicationsList movedApps = new ApplicationsList();
			movedApps.addAll(activeSplitResult.getListMoved());
			movedApps.addAll(passiveSplitResult.getListMoved());
			Collections.sort(movedApps, new AppCPUComparator());
			for(Application app : movedApps) {
				if(activeCPU > passiveCPU) {
					if(passiveMovedAppIds.contains(app.getID())) {
						passiveDeallocated.add(app);
						activeAllocated.add(app);
					}
					newActiveMovedAppIds.add(app.getID());
					activeCPU += app.getCPUDemand();
				} else {
					if(activeMovedAppIds.contains(app.getID())) {
						activeDeallocated.add(app);
						passiveAllocated.add(app);
					}
					newPassiveMovedAppIds.add(app.getID());
					passiveCPU += app.getCPUDemand();
				}
			}
			
			Result result = new Result();
			result.setActiveAllocated(activeAllocated);
			result.setActiveDeallocated(activeDeallocated);
			result.setActiveMovedAppIds(newActiveMovedAppIds);
			result.setPassiveAllocated(passiveAllocated);
			result.setPassiveDeallocated(passiveDeallocated);
			result.setPassiveMovedAppIds(newPassiveMovedAppIds);
			return result;
		}
	}
}
