package peersim.EP2400.resourcealloc.tasks;

import java.util.HashSet;
import java.util.Set;

import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;

public class PerformanceObserver implements Control {

	/**
	 * The protocol to operate on.
	 * 
	 * @config
	 */
	private static final String	PAR_PROT		= "protocol";

	/**
	 * The number of applications
	 * 
	 * @config
	 */
	private static final String	PAR_APPSCOUNT	= "apps_count";

	private static final String	PAR_R_MAX		= "r_max";

	/** Protocol identifier, obtained from config property {@link #PAR_PROT}. */
	private final int			protocolID;

	private final String		prefix;

	/**
	 * Constant r_max in the simulation
	 */
	private final int			r_max;

	/**
	 * Number of application
	 */
	protected int				appsCount;


	/**
	 * Standard constructor that reads the configuration parameters. Invoked by
	 * the simulation engine.
	 * 
	 * @param prefix
	 *            the configuration prefix identifier for this class.
	 */
	public PerformanceObserver(String prefix) {
		this.prefix = prefix;
		protocolID = Configuration.getPid(prefix + "." + PAR_PROT);
		r_max = Configuration.getInt(prefix + "." + PAR_R_MAX);
		appsCount = Configuration.getInt(prefix + "." + PAR_APPSCOUNT);
	}

	protected final static int SERVER_COUNT = 10000;
	int j=-1;
	Set<Integer> overloaded = new HashSet<Integer>();
	@Override
	public boolean execute() {
		j++;
		// TODO: Implement your code for task 1 here...
		if(j == 0) {
			for(int i = 0; i < SERVER_COUNT; i++) {
				DistributedResourceAllocation p = ((DistributedResourceAllocation) Network.get(i).getProtocol(protocolID));
				if(p.getTotalDemand() != 0) {
					System.out.println(i + " " + p.getTotalDemand());
				}
			}
		}
		long totalReconfigCost = 0;
		if(j % 30 == 0) {
			System.out.println(appsCount);
			for(int i = 0; i < SERVER_COUNT; i++) {
				DistributedResourceAllocation p = ((DistributedResourceAllocation) Network.get(i).getProtocol(protocolID));

				totalReconfigCost += p.getReconfigCost();
			}
			System.out.println(totalReconfigCost);
		}
		//				System.out.println("______________");
		//		
		//		if(!(j == 30)) {
		//			for(int i = 0; i < SERVER_COUNT; i++) {
		//				DistributedPlacementProtocol p = ((DistributedPlacementProtocol) Network.get(i).getProtocol(protocolID));
		//				if(p.getTotalDemand() > p.getCpuCapacity()) {
		//					System.out.println("overloaded");
		//					System.err.println(i + " " + p.getTotalDemand());
		//					overloaded.add(i);
		//				}
		//				if(overloaded.contains(i)) {
		//					System.out.println(i + " " + p.getTotalDemand());
		//				}
		//				//			if(p.getTotalDemand() != 0&& j ==40) {
		//				//				System.out.println(i + " " + p.getTotalDemand());
		//				//			}
		//			}
		//		}
		return false;
	}

}
