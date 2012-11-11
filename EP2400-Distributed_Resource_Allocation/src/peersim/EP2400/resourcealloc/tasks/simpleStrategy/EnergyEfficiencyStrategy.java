package peersim.EP2400.resourcealloc.tasks.simpleStrategy;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import peersim.EP2400.resourcealloc.base.Application;
import peersim.EP2400.resourcealloc.base.ApplicationsList;
import peersim.EP2400.resourcealloc.tasks.util.AppCPUComparator;
import peersim.EP2400.resourcealloc.tasks.util.NodeView;


public class EnergyEfficiencyStrategy extends Strategy {
	
	private double	cpuCapacity;
	
	public EnergyEfficiencyStrategy(double cpuCapacity) {
		this.cpuCapacity = cpuCapacity;
	}
	
	@Override
	public Result getPlacement(NodeView activeView, NodeView passiveView) {
		final Set<Integer> activeMovedAppIds = new HashSet<Integer>(activeView.getMovedApps());
		final Set<Integer> passiveMovedAppIds = new HashSet<Integer>(passiveView.getMovedApps());
		
		// Split nodes' application lists in native app list and moved app list
		final Auxiliary activeSplitResult = splitNativeReceived(activeView);
		final Auxiliary passiveSplitResult = splitNativeReceived(passiveView);
		
		final double activeNativeCPU = activeSplitResult.getListNative().totalCPUDemand();
		final double passiveNativeCPU = passiveSplitResult.getListNative().totalCPUDemand();
		final double activeMovedCPU = activeSplitResult.getListMoved().totalCPUDemand();
		final double passiveMovedCPU = passiveSplitResult.getListMoved().totalCPUDemand();
		final double initActiveCPU = activeNativeCPU + activeMovedCPU;
		final double initPassiveCPU = passiveNativeCPU + passiveMovedCPU;
		
		Result result = new Result();
		
		// If one of the servers is inactive and the other is not over CPU Capacity, there is no a better energy efficiency placement - Therefore return a No Action placement
		if ((0 == initActiveCPU || 0 == initPassiveCPU) && !(cpuCapacity < initActiveCPU || cpuCapacity < initPassiveCPU)) {
			result.setActiveMovedAppIds(new HashSet<Integer>(activeView.getMovedApps()));
			result.setPassiveMovedAppIds(new HashSet<Integer>(passiveView.getMovedApps()));
			return result;
		}
		
		// If the sum of the CPU demand of both nodes is less than CPU Capacity - Move all load to one node (this is the best energy efficiency placement)
		if (initActiveCPU + initPassiveCPU <= cpuCapacity) {
			result.setActiveAllocated(new HashSet<Application>(passiveView.getAppList()));
			for (Application app : passiveView.getAppList()) {
				activeMovedAppIds.add(app.getID());
			}
			result.setActiveMovedAppIds(new HashSet<Integer>(activeMovedAppIds));
			result.setPassiveDeallocated(new HashSet<Application>(passiveView.getAppList()));
			return result;
		}
		
		// Calculate initial variance in order to assess at the end if the computed placement is better than the initial
		double initAvg = (initActiveCPU + initPassiveCPU) / 2;
		double initStd = Math.sqrt((Math.pow(initActiveCPU - initAvg, 2) + Math.pow(initPassiveCPU - initAvg, 2)) / 2);
		double initVar = initStd / initAvg;
		
		ApplicationsList activeNativeApps = activeSplitResult.getListNative();
		ApplicationsList passiveNativeApps = passiveSplitResult.getListNative();
		ApplicationsList movedApps = new ApplicationsList();
		movedApps.addAll(activeSplitResult.getListMoved());
		movedApps.addAll(passiveSplitResult.getListMoved());
		
		// Variables required to create the final result
		Set<Integer> lowerMovedAppIds = new HashSet<Integer>();
		Set<Integer> higherMovedAppIds = new HashSet<Integer>();
		Set<Application> higherAllocated = new HashSet<Application>();
		Set<Application> higherDeallocated = new HashSet<Application>();
		Set<Application> lowerAllocated = new HashSet<Application>();
		Set<Application> lowerDeallocated = new HashSet<Application>();
		ApplicationsList lowerApps = new ApplicationsList();
		ApplicationsList higherApps = new ApplicationsList();
		
		double higherCPU;
		
		// Create two abstractions: lower and higher configuration, in order to make the algorithm symmetrical
		if (activeNativeCPU > passiveNativeCPU) {
			lowerApps.addAll(passiveNativeApps);
			higherApps.addAll(activeNativeApps);
			higherCPU = activeNativeCPU;
			lowerMovedAppIds = new HashSet<Integer>(passiveMovedAppIds);
			higherMovedAppIds = new HashSet<Integer>(activeMovedAppIds);
		} else {
			lowerApps.addAll(activeNativeApps);
			higherApps.addAll(passiveNativeApps);
			higherCPU = passiveNativeCPU;
			lowerMovedAppIds = new HashSet<Integer>(activeMovedAppIds);
			higherMovedAppIds = new HashSet<Integer>(passiveMovedAppIds);
		}
		
		// If the CPU demand of the higher configuration is over CPU Capacity
		// Remove all native apps that are required (starting form the biggest) in order to lower the demand below CPU Capacity 
		Iterator<Application> it;
		if (higherCPU > cpuCapacity) {
			Collections.sort(higherApps, new AppCPUComparator());
			it = higherApps.iterator();
			while (it.hasNext()) {
				if (cpuCapacity >= higherCPU) {
					break;
				}
				Application app = it.next();
				movedApps.add(app);
				it.remove();
				higherCPU = higherCPU - app.getCPUDemand();
			}
		}
		
		// If the CPU demand of the higher configuration is now below CPU Capacity
		// Fill it in with all apps that are in the moved list that are required (starting form the biggest) in order to maximize the demand over CPU Capacity  
		Collections.sort(movedApps, new AppCPUComparator());
		it = movedApps.iterator();
		while (it.hasNext()) {
			if (cpuCapacity == higherCPU) {
				break;
			}
			Application app = it.next();
			if (cpuCapacity - higherCPU >= app.getCPUDemand()) {
				if (lowerMovedAppIds.contains(app.getID())) {
					higherAllocated.add(app);
					lowerDeallocated.add(app);
					lowerMovedAppIds.remove(app.getID());
					higherMovedAppIds.add(app.getID());
				}
				it.remove();
				higherCPU = higherCPU + app.getCPUDemand();
			}
		}
		
		// If the CPU demand of the higher configuration is now below CPU Capacity
		// Fill it in with all apps that belong to the lower node that are required (starting form the biggest) in order to maximize the demand over CPU Capacity  
		Collections.sort(lowerApps, new AppCPUComparator());
		it = lowerApps.iterator();
		while (it.hasNext()) {
			if (cpuCapacity == higherCPU) {
				break;
			}
			Application app = it.next();
			if (cpuCapacity - higherCPU >= app.getCPUDemand()) {
				higherAllocated.add(app);
				lowerDeallocated.add(app);
				higherMovedAppIds.add(app.getID());
				it.remove();
				higherCPU = higherCPU + app.getCPUDemand();
			}
		}
		
		// Add all unallocated moved apps to the lower node
		for (Application app : movedApps) {
			if (!lowerMovedAppIds.contains(app.getID())) {
				higherDeallocated.add(app);
				lowerAllocated.add(app);
				higherMovedAppIds.remove(app.getID());
				lowerMovedAppIds.add(app.getID());
			}
		}
		
		// Given which node had initially the highest CPU demand for native apps, build the result accordingly
		if (activeNativeCPU > passiveNativeCPU) {
			result.setActiveAllocated(higherAllocated);
			result.setActiveDeallocated(higherDeallocated);
			result.setActiveMovedAppIds(higherMovedAppIds);
			result.setPassiveAllocated(lowerAllocated);
			result.setPassiveDeallocated(lowerDeallocated);
			result.setPassiveMovedAppIds(lowerMovedAppIds);
		} else {
			result.setActiveAllocated(lowerAllocated);
			result.setActiveDeallocated(lowerDeallocated);
			result.setActiveMovedAppIds(lowerMovedAppIds);
			result.setPassiveAllocated(higherAllocated);
			result.setPassiveDeallocated(higherDeallocated);
			result.setPassiveMovedAppIds(higherMovedAppIds);
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
		
		// If the final CPU demand of any of the nodes is above CPU Capacity - Abort (Initial state was better, so a do nothing placement is returned instead)
		if (finalActiveCPU > cpuCapacity || finalPassiveCPU > cpuCapacity) {
			result = new Result();
			result.setActiveMovedAppIds(new HashSet<Integer>(activeView.getMovedApps()));
			result.setPassiveMovedAppIds(new HashSet<Integer>(passiveView.getMovedApps()));
		}
		// If the initial CPU demand of both the nodes were less than CPU Capacity and if the final variance is less than initial one  - Abort (Initial state was better, so a do nothing placement is returned instead)
		else {
			if (initActiveCPU < cpuCapacity && initPassiveCPU < cpuCapacity) {
				if (finalVar < initVar) {
					result = new Result();
					result.setActiveMovedAppIds(new HashSet<Integer>(activeView.getMovedApps()));
					result.setPassiveMovedAppIds(new HashSet<Integer>(passiveView.getMovedApps()));
				}
			}
		}
		return result;
	}
}