package peersim.EP2400.resourcealloc.tasks.util;

import java.util.Comparator;


/**
 * this comparator returns -1 if cpu1 > cpu2 and 1 if cpu1 < cpu2 
 * because I want to sort the List in reverse
 * - biggest CPU demand should be first
 */
public class AppCPUComparator implements Comparator<ApplicationInfo> {

	@Override
	public int compare(ApplicationInfo appInfo1, ApplicationInfo appInfo2) {
		double cpu1 = appInfo1.getApplication().getCPUDemand();
		double cpu2 = appInfo2.getApplication().getCPUDemand();
		
		if(cpu1 == cpu2) 
			return 0;
		return cpu1 > cpu2 ? -1 : 1; 
	}

}
