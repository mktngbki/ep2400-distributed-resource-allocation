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
	
	@Override
	public Result getPlacement(NodeView activeView, NodeView passiveView) {
		// Split nodes' application lists in native app list and moved app list
		final Auxiliary activeSplitResult = splitNativeReceived(activeView);
		final Auxiliary passiveSplitResult = splitNativeReceived(passiveView);
		
		final double activeNativeCPU = activeSplitResult.getListNative().totalCPUDemand();
		final double passiveNativeCPU = passiveSplitResult.getListNative().totalCPUDemand();
		final double activeMovedCPU = activeSplitResult.getListMoved().totalCPUDemand();
		final double passiveMovedCPU = passiveSplitResult.getListMoved().totalCPUDemand();
		final double initActiveCPU = activeNativeCPU + activeMovedCPU;
		final double initPassiveCPU = passiveNativeCPU + passiveMovedCPU;
		
		// Calculate initial variance in order to assess at the end if the computed placement is better than the initial
		double initAvg = (initActiveCPU + initPassiveCPU) / 2;
		double initStd = Math.sqrt((Math.pow(initActiveCPU - initAvg, 2) + Math.pow(initPassiveCPU - initAvg, 2)) / 2);
		double initVar = initStd / initAvg;
		
		Result result;
		
		// If both the nodes have the same CPU demand, the load is already balanced -  Abort
		if (initActiveCPU == initPassiveCPU) {
			result = new Result();
			result.setActiveMovedAppIds(new HashSet<Integer>(activeView.getMovedApps()));
			result.setPassiveMovedAppIds(new HashSet<Integer>(passiveView.getMovedApps()));
			return result;
		}
		
		// If the sum of the CPU demand of the moved apps of both the active and the passive node is less than difference of the CPU demand of the native apps,
		// transfer all moved apps to the node that has less load and fill half of the remaining difference with native apps from the loaded one.
		if (Math.abs(activeNativeCPU - passiveNativeCPU) > (activeMovedCPU + passiveMovedCPU)) {
			double transferCPU = (Math.abs(activeNativeCPU - passiveNativeCPU) - activeMovedCPU - passiveMovedCPU) / 2;
			
			Set<Integer> lowerMovedAppIds = new HashSet<Integer>();
			Set<Application> higherAllocated = new HashSet<Application>();
			Set<Application> higherDeallocated = new HashSet<Application>();
			Set<Application> lowerAllocated = new HashSet<Application>();
			Set<Application> lowerDeallocated = new HashSet<Application>();
			ApplicationsList higherApps = new ApplicationsList();
			
			// Create two abstractions: lower and higher configuration, in order to make the algorithm symmetrical
			if (activeNativeCPU > passiveNativeCPU) {
				higherApps.addAll(activeSplitResult.getListNative());
				higherDeallocated.addAll(activeSplitResult.getListMoved());
				lowerAllocated.addAll(activeSplitResult.getListMoved());
			} else {
				higherApps.addAll(passiveSplitResult.getListNative());
				higherDeallocated.addAll(passiveSplitResult.getListMoved());
				lowerAllocated.addAll(passiveSplitResult.getListMoved());
			}
			lowerMovedAppIds.addAll(activeView.getMovedApps());
			lowerMovedAppIds.addAll(passiveView.getMovedApps());
			
			// Fill half of the remaining difference with native apps from the loaded one (starting from the biggest).
			Collections.sort(higherApps, new AppCPUComparator());
			Iterator<Application> it = higherApps.iterator();
			while (it.hasNext()) {
				if (0 == transferCPU) {
					break;
				}
				Application app = it.next();
				if (transferCPU >= app.getCPUDemand()) {
					higherDeallocated.add(app);
					lowerAllocated.add(app);
					lowerMovedAppIds.add(app.getID());
					transferCPU = transferCPU - app.getCPUDemand();
				}
			}
			
			result = new Result();
			
			// Given which node had initially the highest CPU demand for native apps, build the result accordingly
			if (activeNativeCPU > passiveNativeCPU) {
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
		}
		// The CPU demand of the moved apps is higher that the difference of the CPU demand of the native apps of the nodes 
		else {
			// Variables required to create the final result
			Set<Integer> newActiveMovedAppIds = new HashSet<Integer>();
			Set<Integer> newPassiveMovedAppIds = new HashSet<Integer>();
			Set<Application> activeAllocated = new HashSet<Application>();
			Set<Application> activeDeallocated = new HashSet<Application>();
			Set<Application> passiveAllocated = new HashSet<Application>();
			Set<Application> passiveDeallocated = new HashSet<Application>();
			
			double currActiveCPU = activeNativeCPU;
			double currPassiveCPU = passiveNativeCPU;
			
			// Distribute as equal as possible the apps that are in moved list between the two nodes (start allocating the biggest)
			ApplicationsList movedApps = new ApplicationsList();
			movedApps.addAll(activeSplitResult.getListMoved());
			movedApps.addAll(passiveSplitResult.getListMoved());
			Collections.sort(movedApps, new AppCPUComparator());
			for (Application app : movedApps) {
				if (currActiveCPU > currPassiveCPU) {
					if (activeView.getMovedApps().contains(app.getID())) {
						activeDeallocated.add(app);
						passiveAllocated.add(app);
					}
					newPassiveMovedAppIds.add(app.getID());
					currPassiveCPU += app.getCPUDemand();
				} else {
					if (passiveView.getMovedApps().contains(app.getID())) {
						passiveDeallocated.add(app);
						activeAllocated.add(app);
					}
					newActiveMovedAppIds.add(app.getID());
					currActiveCPU += app.getCPUDemand();
				}
			}
			
			// Build the result
			result = new Result();
			result.setActiveAllocated(activeAllocated);
			result.setActiveDeallocated(activeDeallocated);
			result.setActiveMovedAppIds(newActiveMovedAppIds);
			result.setPassiveAllocated(passiveAllocated);
			result.setPassiveDeallocated(passiveDeallocated);
			result.setPassiveMovedAppIds(newPassiveMovedAppIds);
		}
		
		// Calculate the final variance
		ApplicationsList finalActiveApps = new ApplicationsList();
		finalActiveApps.addAll(activeView.getAppList());
		finalActiveApps.addAll(result.getActiveAllocated());
		finalActiveApps.removeAll(result.getActiveDeallocated());
		
		ApplicationsList finalPassiveApps = new ApplicationsList();
		finalPassiveApps.addAll(passiveView.getAppList());
		finalPassiveApps.addAll(result.getPassiveAllocated());
		finalPassiveApps.removeAll(result.getPassiveDeallocated());
		
		
		double finalActiveCPU = finalActiveApps.totalCPUDemand();
		double finalPassiveCPU = finalPassiveApps.totalCPUDemand();
		double finalAvg = (finalActiveCPU + finalPassiveCPU) / 2;
		double finalStd = Math.sqrt((Math.pow(finalActiveCPU - finalAvg, 2) + Math.pow(finalPassiveCPU - finalAvg, 2)) / 2);
		double finalVar = finalStd / finalAvg;
		
		//// If the final variance is greater than the initial one  - Abort (Initial state was better, so a do nothing placement is returned instead) 
		if (finalVar > initVar) {
			result = new Result();
			result.setActiveMovedAppIds(new HashSet<Integer>(activeView.getMovedApps()));
			result.setPassiveMovedAppIds(new HashSet<Integer>(passiveView.getMovedApps()));
		}
		return result;
	}
}