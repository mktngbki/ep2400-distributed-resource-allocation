package peersim.EP2400.resourcealloc.tasks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import peersim.EP2400.resourcealloc.base.ApplicationsManager;
import peersim.EP2400.resourcealloc.base.DistributedPlacementProtocol;
import peersim.EP2400.resourcealloc.util.FileIO;
import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

public class PerformanceObserver implements Control {

	//TODO change separator from ; to ,
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
	}

	protected final static int	SERVER_COUNT	= 10000;
	int							j				= -1;
	Set<Integer>				overloaded		= new HashSet<Integer>();

	//@Override
	public boolean execute() {
		j++;

		//TODO change separator from ; to ,
		if(0 == j) {
			FileIO.append("N;V;R;C;S\n", "cycles.csv");
			FileIO.append("N;V;R;C;S\n", "epochs.csv");
			FileIO.append("N;V;Active;Overloaded;Loaded;Middleloaded;Underloaded;ReconfigCost;CPUDemand\n", "auxEpochs.csv");
		}

		ArrayList<Double> cpuDemandList = new ArrayList<Double>(SERVER_COUNT);

		String cycleResult = "";
		String epochResult = "";
		String auxResult = "";


		int cpuCapacity = 100;
		double tau = DistributedResourceAllocation.TAU;
		int nrOfApps = ApplicationsManager.getInstance().applications().size();

		double totalCPUDemand = ApplicationsManager.getInstance().applications().totalCPUDemand();
		double average =  totalCPUDemand / SERVER_COUNT;
		double realAverage = 0;
		double var1 = 0;
		double var2 = 0;
		int totalReconfigCost = 0;

		int activeServers = 0;
		int overloadedServers = 0; //fraction of nodes over omega(cpu capacity)
		int loadedServers = 0; //fraction of nodes over tau(cpu threshold)
		int underloadedServers = 0;
		int middleloadedServers = 0;

		for(int i = 0; i < SERVER_COUNT; i++) {
			Node peer = Network.get(i);
			DistributedPlacementProtocol p = ((DistributedPlacementProtocol) peer.getProtocol(protocolID));
			double cpuDemand = p.getTotalDemand();
			cpuDemandList.add(cpuDemand);
			if(cpuDemand != 0) {
				activeServers++;
			}
			if(0 < cpuDemand && cpuDemand <= cpuCapacity*0.25) {
				underloadedServers++;
			}
			if(0.25*cpuCapacity < cpuDemand && cpuDemand < tau) {
				middleloadedServers++;
			}
			if(tau <= cpuDemand && cpuDemand <= cpuCapacity) { 
				loadedServers++;
			}
			if(cpuCapacity < cpuDemand) {
				overloadedServers++;
			}

			DistributedResourceAllocation p2 = (DistributedResourceAllocation)p;
			totalReconfigCost += p2.getReconfigCost();
	
			if(0 == j%30) {
				p2.resetView();
			}
		}

		realAverage = totalCPUDemand / activeServers;

		for(Double cpuDemand : cpuDemandList) {
			var1 += Math.pow(cpuDemand - average, 2);
			if(cpuDemand != 0) {
				var2 += Math.pow(cpuDemand - realAverage, 2);
			}
		}	

		//standard deviation
		var1 = Math.sqrt(var1/SERVER_COUNT);

		//variation coefficient
		var1 = var1 / average;

		if(j < 30) {
			//NrOfCycles
			cycleResult += j;
			//V - variation
			cycleResult += SEPARATOR + var1;
			//R - active servers
			cycleResult += SEPARATOR + (float)activeServers/SERVER_COUNT;
			//C - cost of reconfiguration
			cycleResult += SEPARATOR + (float)totalReconfigCost/nrOfApps;
			//S - overloaded servers
			cycleResult += SEPARATOR + (float)overloadedServers/SERVER_COUNT;

			cycleResult += "\n";
			FileIO.append(cycleResult, "cycles.csv");
		}

		if(29 == j%30) {

			//NrOfCycles
			epochResult += j/30;
			//V - variation
			epochResult += SEPARATOR + var1;
			//R - active servers
			epochResult += SEPARATOR + (float)activeServers/SERVER_COUNT;
			//C - cost of reconfiguration
			epochResult += SEPARATOR + (float)totalReconfigCost/nrOfApps;
			//S - overloaded servers
			epochResult += SEPARATOR + (float)overloadedServers/SERVER_COUNT;

			epochResult += "\n";
			FileIO.append(epochResult, "epochs.csv");

			//standard deviation
			var2 = Math.sqrt(var2/activeServers);
			//variation coefficient
			var2 = var2 / realAverage;

			//NrOfCycles
			auxResult += j/30;
			//V
			auxResult += SEPARATOR + var2;
			//Active servers
			auxResult += SEPARATOR + activeServers;
			//Overloaded servers - over cpu capacity
			auxResult += SEPARATOR + overloadedServers;
			//Loaded servers - over tau capacity
			auxResult += SEPARATOR + loadedServers;
			//Middle loaded - between 0.25*cpuCapacity to tau
			auxResult += SEPARATOR + middleloadedServers;
			//Underloaded servers - bellow tau/2
			auxResult += SEPARATOR + underloadedServers;
			//Cost of reconfiguration
			auxResult += SEPARATOR + totalReconfigCost;
			//total CPU demand
			auxResult += SEPARATOR + totalCPUDemand;

			auxResult += "\n";
			FileIO.append(auxResult, "auxEpochs.csv");
		}

		return false;
	}
}