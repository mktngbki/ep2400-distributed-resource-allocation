package peersim.EP2400.resourcealloc.tasks.util;

import java.util.Comparator;

import peersim.EP2400.resourcealloc.base.Application;


/**
 * this comparator returns -1 if cpu1 > cpu2 and 1 if cpu1 < cpu2 
 * because I want to sort the List in reverse
 * - biggest CPU demand should be first
 */
public class AppCPUComparator implements Comparator<Application> {
	
	@Override
	public int compare(Application app1, Application app2) {
		double cpu1 = app1.getCPUDemand();
		double cpu2 = app2.getCPUDemand();
		
		if (cpu1 == cpu2) {
			return 0;
		}
		return cpu1 > cpu2 ? -1 : 1;
	}
	
}
