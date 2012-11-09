package peersim.EP2400.resourcealloc.tasks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import peersim.EP2400.resourcealloc.base.ApplicationsManager;
import peersim.EP2400.resourcealloc.base.DistributedPlacementProtocol;
import peersim.EP2400.resourcealloc.tasks.placementStartegy.EnhancedStrategy;
import peersim.EP2400.resourcealloc.util.FileIO;
import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

public class PerformanceObserver implements Control {
	
	private static final String	SEPARATOR		= ";";
	
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
		//TODO change separator from ; to ,
		FileIO.append("N;V;S;R\n", "cycles.csv");
		FileIO.append("N;V;S;R;C\n", "epochs.csv");
		FileIO.append("N;V;OptimumTheoritical;Active;Overloaded;Loaded;Underloaded;OptimumThresholdActive;ReconfigCost\n", "auxEpochs.csv");
	}
	
	protected final static int	SERVER_COUNT	= 10000;
	int							j				= -1;
	Set<Integer>				overloaded		= new HashSet<Integer>();
	
	
	/**
	 * This method applies for Task 2.3
	 */
	@Override
	public boolean execute() {
		j++;
		
		ArrayList<Double> cpuDemandList = new ArrayList<Double>(SERVER_COUNT);
		
		String cycleResult = "";
		String epochResult = "";
		String auxResult = "";
		
		
		int cpuCapacity = 100;
		double tau = EnhancedStrategy.CPU_USAGE_THRESHOLD;
		
		double totalCPUDemand = ApplicationsManager.getInstance().applications().totalCPUDemand();
		double average = totalCPUDemand / SERVER_COUNT;
		double realAverage = 0;
		double var1 = 0;
		double var2 = 0;
		int totalReconfigCost = 0;
		
		int activeServers = 0;
		int overloadedServers = 0; //fraction of nodes over omega(cpu capacity)
		int loadedServers = 0; //fraction of nodes over tau(cpu threshold)
		int optimumThresholdActive = 0;
		int underloadedServers = 0;
		
		for (int i = 0; i < SERVER_COUNT; i++) {
			Node peer = Network.get(i);
			DistributedPlacementProtocol p = ((DistributedPlacementProtocol) peer.getProtocol(protocolID));
			double cpuDemand = p.getTotalDemand();
			cpuDemandList.add(cpuDemand);
			if (cpuDemand != 0) {
				activeServers++;
			}
			if (0 != cpuDemand && cpuDemand <= tau / 2) {
				underloadedServers++;
			}
			if ((tau - 2) < cpuDemand && cpuDemand <= tau) {
				optimumThresholdActive++;
			}
			if (cpuDemand > cpuCapacity) {
				overloadedServers++;
			}
			if (cpuDemand > tau) { //servers with cpuDemand over tau
				loadedServers++;
			}
			
			DistributedResourceAllocation p2 = (DistributedResourceAllocation) p;
			totalReconfigCost += p2.getReconfigCost();
		}
		
		realAverage = totalCPUDemand / activeServers;
		
		for (Double cpuDemand : cpuDemandList) {
			var1 += Math.pow(cpuDemand - average, 2);
			if (cpuDemand != 0) {
				var2 += Math.pow(cpuDemand - realAverage, 2);
			}
		}
		
		// It is required only for the first epoch
		if (j < 30) {
			//standard deviation
			var1 = Math.sqrt(var1 / SERVER_COUNT);
			//variation coefficient
			var1 = var1 / average;
			
			//NrOfCycles
			cycleResult += j;
			//V - variation
			cycleResult += SEPARATOR + var1;
			//C - cost of reconfiguration
			cycleResult += SEPARATOR + totalReconfigCost;
			
			cycleResult += "\n";
			FileIO.append(cycleResult, "cycles.csv");
		}
		
		if (0 == j % 30 || 29 == j % 30) {
			System.out.println("total CPU demand " + totalCPUDemand);
			
		}
		if (29 == j % 30) {
			//NrOfCycles
			epochResult += j / 30;
			//V - variation
			epochResult += SEPARATOR + var1;
			//C - cost of reconfiguration
			epochResult += SEPARATOR + totalReconfigCost;
			
			epochResult += "\n";
			FileIO.append(epochResult, "epochs.csv");
			
			//standard deviation
			var2 = Math.sqrt(var2 / activeServers);
			//variation coefficient
			var2 = var2 / realAverage;
			
			//NrOfCycles
			auxResult += j / 30;
			//V
			auxResult += SEPARATOR + var2;
			//Optimum theoritical active servers
			auxResult += SEPARATOR + totalCPUDemand / tau;
			//Active servers
			auxResult += SEPARATOR + activeServers;
			//Overloaded servers - over cpu capacity
			auxResult += SEPARATOR + overloadedServers;
			//Loaded servers - over tau capacity
			auxResult += SEPARATOR + loadedServers;
			//Underloaded servers - bellow tau/2
			auxResult += SEPARATOR + underloadedServers;
			//Active servers with optimum load - in (tau-2,tau]
			auxResult += SEPARATOR + optimumThresholdActive;
			//Cost of reconfiguration
			auxResult += SEPARATOR + totalReconfigCost;
			
			auxResult += "\n";
			FileIO.append(auxResult, "auxEpochs.csv");
		}
		return false;
	}
	
	
	/**
	 * This method applies for Task 2.4
	 */
	//		@Override
	//		public boolean execute() {
	//			j++;
	//			
	//			ArrayList<Double> cpuDemandList = new ArrayList<Double>(SERVER_COUNT);
	//	
	//			String cycleResult = "";
	//			String epochResult = "";
	//			String auxResult = "";
	//	
	//	
	//			int cpuCapacity = 100;
	//			int tau = 80;
	//	
	//			double totalCPUDemand = ApplicationsManager.getInstance().applications().totalCPUDemand();
	//			double average =  totalCPUDemand / SERVER_COUNT;
	//			double realAverage = 0;
	//			double var1 = 0;
	//			double var2 = 0;
	//			int totalReconfigCost = 0;
	//			
	//			int activeServers = 0;
	//			int overloadedServers = 0; //fraction of nodes over omega(cpu capacity)
	//			int loadedServers = 0; //fraction of nodes over tau(cpu threshold)
	//			int optimumThresholdActive = 0;
	//			int underloadedServers = 0;
	//			
	//			for(int i = 0; i < SERVER_COUNT; i++) {
	//				Node peer = Network.get(i);
	//				DistributedPlacementProtocol p = ((DistributedPlacementProtocol) peer.getProtocol(protocolID));
	//				double cpuDemand = p.getTotalDemand();
	//				cpuDemandList.add(cpuDemand);
	//				if(cpuDemand != 0) {
	//					activeServers++;
	//				}
	//				if(0 != cpuDemand && cpuDemand <= tau/2) {
	//					underloadedServers++;
	//				}
	//				if((tau-2) < cpuDemand && cpuDemand <= tau) {
	//					optimumThresholdActive++;
	//				}
	//				if(cpuDemand > cpuCapacity) {
	//					overloadedServers++;
	//				}
	//				if(cpuDemand > tau) { //servers with cpuDemand over tau
	//					loadedServers++;
	//				}
	//				if(29 == j%30) {
	//					DistributedResourceAllocation p2 = (DistributedResourceAllocation)p;
	//					totalReconfigCost += p2.getReconfigCost();
	//				}
	//			}
	//	
	//			realAverage = totalCPUDemand / activeServers;
	//	
	//			for(Double cpuDemand : cpuDemandList) {
	//				var1 += Math.pow(cpuDemand - average, 2);
	//				if(cpuDemand != 0) {
	//					var2 += Math.pow(cpuDemand - realAverage, 2);
	//				}
	//			}	
	//	
	//			//standard deviation
	//			var1 = Math.sqrt(var1/SERVER_COUNT);
	//	
	//			//variation coefficient
	//			var1 = var1 / average;
	//	
	//			//NrOfCycles
	//			cycleResult += j;
	//			//V - variation
	//			cycleResult += SEPARATOR + var1;
	//			//S - overloaded servers
	//			cycleResult += SEPARATOR + (float)overloadedServers/SERVER_COUNT;
	//			//R - active servers
	//			cycleResult += SEPARATOR + (float)activeServers/SERVER_COUNT;
	//	
	//			cycleResult += "\n";
	//			FileIO.append(cycleResult, "cycles.csv");
	//	
	//			if(0 == j%30 || 29 == j%30) {
	//				System.out.println("total CPU demand " + totalCPUDemand);
	//				
	//			}
	//			if(29 == j%30) {
	//				//NrOfCycles
	//				epochResult += j/30;
	//				//V - variation
	//				epochResult += SEPARATOR + var1;
	//				//S - overloaded servers
	//				epochResult += SEPARATOR + (float)overloadedServers/SERVER_COUNT;
	//				//R - active servers
	//				epochResult += SEPARATOR + (float)activeServers/SERVER_COUNT;
	//				//C - cost of reconfiguration
	//				epochResult += SEPARATOR + totalReconfigCost;
	//				
	//				epochResult += "\n";
	//				FileIO.append(epochResult, "epochs.csv");
	//	
	//				//standard deviation
	//				var2 = Math.sqrt(var2/activeServers);
	//				//variation coefficient
	//				var2 = var2 / realAverage;
	//				
	//				//NrOfCycles
	//				auxResult += j/30;
	//				//V
	//				auxResult += SEPARATOR + var2;
	//				//Optimum theoritical active servers
	//				auxResult += SEPARATOR + totalCPUDemand/tau;
	//				//Active servers
	//				auxResult += SEPARATOR + activeServers;
	//				//Overloaded servers - over cpu capacity
	//				auxResult += SEPARATOR + overloadedServers;
	//				//Loaded servers - over tau capacity
	//				auxResult += SEPARATOR + loadedServers;
	//				//Underloaded servers - bellow tau/2
	//				auxResult += SEPARATOR + underloadedServers;
	//				//Active servers with optimum load - in (tau-2,tau]
	//				auxResult += SEPARATOR + optimumThresholdActive;
	//				//Cost of reconfiguration
	//				auxResult += SEPARATOR + totalReconfigCost;
	//				
	//				auxResult += "\n";
	//				FileIO.append(auxResult, "auxEpochs.csv");
	//			}
	//			return false;
	//		}
	
	
	/**
	 * This method applies for Task 2.5
	 */
	//		@Override
	//		public boolean execute() {
	//			j++;
	//			
	//			ArrayList<Double> cpuDemandList = new ArrayList<Double>(SERVER_COUNT);
	//	
	//			String cycleResult = "";
	//			String epochResult = "";
	//			String auxResult = "";
	//	
	//	
	//			int cpuCapacity = 100;
	//			int tau = 80;
	//	
	//			double totalCPUDemand = ApplicationsManager.getInstance().applications().totalCPUDemand();
	//			double average =  totalCPUDemand / SERVER_COUNT;
	//			double realAverage = 0;
	//			double var1 = 0;
	//			double var2 = 0;
	//			int totalReconfigCost = 0;
	//			
	//			int activeServers = 0;
	//			int overloadedServers = 0; //fraction of nodes over omega(cpu capacity)
	//			int loadedServers = 0; //fraction of nodes over tau(cpu threshold)
	//			int optimumThresholdActive = 0;
	//			int underloadedServers = 0;
	//			
	//			for(int i = 0; i < SERVER_COUNT; i++) {
	//				Node peer = Network.get(i);
	//				DistributedPlacementProtocol p = ((DistributedPlacementProtocol) peer.getProtocol(protocolID));
	//				double cpuDemand = p.getTotalDemand();
	//				cpuDemandList.add(cpuDemand);
	//				if(cpuDemand != 0) {
	//					activeServers++;
	//				}
	//				if(0 != cpuDemand && cpuDemand <= tau/2) {
	//					underloadedServers++;
	//				}
	//				if((tau-2) < cpuDemand && cpuDemand <= tau) {
	//					optimumThresholdActive++;
	//				}
	//				if(cpuDemand > cpuCapacity) {
	//					overloadedServers++;
	//				}
	//				if(cpuDemand > tau) { //servers with cpuDemand over tau
	//					loadedServers++;
	//				}
	//				if(29 == j%30) {
	//					DistributedResourceAllocation p2 = (DistributedResourceAllocation)p;
	//					totalReconfigCost += p2.getReconfigCost();
	//				}
	//			}
	//	
	//			realAverage = totalCPUDemand / activeServers;
	//	
	//			for(Double cpuDemand : cpuDemandList) {
	//				var1 += Math.pow(cpuDemand - average, 2);
	//				if(cpuDemand != 0) {
	//					var2 += Math.pow(cpuDemand - realAverage, 2);
	//				}
	//			}	
	//	
	//			//standard deviation
	//			var1 = Math.sqrt(var1/SERVER_COUNT);
	//	
	//			//variation coefficient
	//			var1 = var1 / average;
	//	
	//			//NrOfCycles
	//			cycleResult += j;
	//			//V - variation
	//			cycleResult += SEPARATOR + var1;
	//			//S - overloaded servers
	//			cycleResult += SEPARATOR + (float)overloadedServers/SERVER_COUNT;
	//			//R - active servers
	//			cycleResult += SEPARATOR + (float)activeServers/SERVER_COUNT;
	//	
	//			cycleResult += "\n";
	//			FileIO.append(cycleResult, "cycles.csv");
	//	
	//			if(0 == j%30 || 29 == j%30) {
	//				System.out.println("total CPU demand " + totalCPUDemand);
	//				
	//			}
	//			if(29 == j%30) {
	//				//NrOfCycles
	//				epochResult += j/30;
	//				//V - variation
	//				epochResult += SEPARATOR + var1;
	//				//S - overloaded servers
	//				epochResult += SEPARATOR + (float)overloadedServers/SERVER_COUNT;
	//				//R - active servers
	//				epochResult += SEPARATOR + (float)activeServers/SERVER_COUNT;
	//				//C - cost of reconfiguration
	//				epochResult += SEPARATOR + totalReconfigCost;
	//				
	//				epochResult += "\n";
	//				FileIO.append(epochResult, "epochs.csv");
	//	
	//				//standard deviation
	//				var2 = Math.sqrt(var2/activeServers);
	//				//variation coefficient
	//				var2 = var2 / realAverage;
	//				
	//				//NrOfCycles
	//				auxResult += j/30;
	//				//V
	//				auxResult += SEPARATOR + var2;
	//				//Optimum theoritical active servers
	//				auxResult += SEPARATOR + totalCPUDemand/tau;
	//				//Active servers
	//				auxResult += SEPARATOR + activeServers;
	//				//Overloaded servers - over cpu capacity
	//				auxResult += SEPARATOR + overloadedServers;
	//				//Loaded servers - over tau capacity
	//				auxResult += SEPARATOR + loadedServers;
	//				//Underloaded servers - bellow tau/2
	//				auxResult += SEPARATOR + underloadedServers;
	//				//Active servers with optimum load - in (tau-2,tau]
	//				auxResult += SEPARATOR + optimumThresholdActive;
	//				//Cost of reconfiguration
	//				auxResult += SEPARATOR + totalReconfigCost;
	//				
	//				auxResult += "\n";
	//				FileIO.append(auxResult, "auxEpochs.csv");
	//			}
	//			return false;
	//		}
}