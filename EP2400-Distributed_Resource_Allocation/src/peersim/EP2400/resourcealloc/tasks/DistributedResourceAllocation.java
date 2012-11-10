package peersim.EP2400.resourcealloc.tasks;

import java.util.HashSet;
import java.util.Set;

import peersim.EP2400.resourcealloc.base.Application;
import peersim.EP2400.resourcealloc.base.ApplicationsList;
import peersim.EP2400.resourcealloc.base.DistributedPlacementProtocol;
import peersim.EP2400.resourcealloc.tasks.simpleStrategy.EnergyEfficiencyStrategy;
import peersim.EP2400.resourcealloc.tasks.simpleStrategy.LoadBalanceStrategy;
import peersim.EP2400.resourcealloc.tasks.simpleStrategy.Strategy;
import peersim.EP2400.resourcealloc.tasks.simpleStrategy.Strategy.Result;
import peersim.EP2400.resourcealloc.tasks.util.NodeView;
import peersim.config.FastConfig;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Node;

public class DistributedResourceAllocation extends DistributedPlacementProtocol {
	public static final double	TAU						= 80;

	private Strategy			pStrategy;
	private Set<Integer>		ownReceivedApps = new HashSet<Integer>();				// apps that i receive so from the reconfiguration cost, i already paid 1 for it, so moving further does not cost us extra
	private double				currentSystemLoadView	= -1;

	public DistributedResourceAllocation(String prefix) {
		super(prefix);
	}

	public DistributedResourceAllocation(String prefix, double cpu_capacity_value) {
		super(prefix, cpu_capacity_value);
	}

	@Override
	public void nextCycle(Node node, int protocolID) {
		int linkableID = FastConfig.getLinkable(protocolID);
		Linkable linkable = (Linkable) node.getProtocol(linkableID);

		int degree = linkable.degree();
		int nbIndex = CommonState.r.nextInt(degree);
		Node peer = linkable.getNeighbor(nbIndex);
		// The selected peer could be inactive
		if (!peer.isUp()) {
			return;
		}

		DistributedResourceAllocation neighbor = (DistributedResourceAllocation) peer.getProtocol(protocolID);

		ApplicationsList ownApps = applicationsList();
		ApplicationsList sentApps = (ApplicationsList)ownApps.clone();
		Set<Integer> sentOwnReceivedApps = (Set<Integer>)((HashSet<Integer>)ownReceivedApps).clone();
		// Initialize in every epoch the current system load view with the local CPU demand
		if (-1 == currentSystemLoadView) {
			currentSystemLoadView = ownApps.totalCPUDemand();
		}
		// Build the node view and send it to the passive thread of the selected neighbor
		NodeView myView = new NodeView(ownApps, ownReceivedApps, currentSystemLoadView);
		NodeView sentView = new NodeView(sentApps, sentOwnReceivedApps, currentSystemLoadView);
		NodeView receivedView = neighbor.passiveThread(sentView);

		// Update the current system load view by averaging the two node views
		currentSystemLoadView = (currentSystemLoadView + receivedView.getCurrentSystemLoadView()) / 2;

		// Decide which strategy the node should enforce, given its the updated view of the load of the system
		if (currentSystemLoadView > TAU) {
			pStrategy = new LoadBalanceStrategy();
		} else {
			pStrategy = new EnergyEfficiencyStrategy(getCpuCapacity());
		}

		// Process the information, apply the relevant strategy and get the updated info
		Result result = pStrategy.getPlacement(myView, receivedView);

		// Update the list of received/moved apps
		ownReceivedApps = result.getActiveMovedAppIds();

		// Allocate new received apps and deallocate moved apps
		updatePlacement(result.getActiveAllocated(), result.getActiveDeallocated());
	}

	public NodeView passiveThread(NodeView receivedView) {
		// Build the node view
		ApplicationsList ownApps = applicationsList();
		ApplicationsList sentApps = (ApplicationsList)ownApps.clone();
		Set<Integer> sentOwnReceivedApps = (Set<Integer>)((HashSet<Integer>)ownReceivedApps).clone();
		NodeView myView = new NodeView(ownApps, ownReceivedApps, currentSystemLoadView);
		NodeView sentView = new NodeView(sentApps, sentOwnReceivedApps, currentSystemLoadView);

		// Update the current system load view by averaging the two node views
		currentSystemLoadView = (currentSystemLoadView + receivedView.getCurrentSystemLoadView()) / 2;

		// Decide which strategy the node should enforce, given its view of the load of the system
		if (currentSystemLoadView > TAU) {
			pStrategy = new LoadBalanceStrategy();
		} else {
			pStrategy = new EnergyEfficiencyStrategy(getCpuCapacity());
		}

		// Process the information, apply the relevant strategy and get the updated info
		Result result = pStrategy.getPlacement(receivedView, myView);

		// Update the list of received/moved apps
		ownReceivedApps = result.getPassiveMovedAppIds();

		// Allocate new received apps and deallocate moved apps
		updatePlacement(result.getPassiveAllocated(), result.getPassiveDeallocated());

		// Return the initial view so the active thread can correctly update its state
		return sentView;
	}

	public void updatePlacement(Set<Application> allocated, Set<Application> deallocated) {
		// Allocate apps received in this cycle
		for (Application app : allocated) {
			allocateApplication(app);
		}

		// Deallocate apps given away in this cycle
		for (Application app : deallocated) {
			deallocateApplication(app);
		}
	}

	public int getReconfigCost() {
		int reconfigCost = ownReceivedApps.size();
		ownReceivedApps = new HashSet<Integer>();
		currentSystemLoadView = -1;
		return reconfigCost;
	}

	@Override
	public Object clone() {
		DistributedResourceAllocation proto = new DistributedResourceAllocation(
				prefix, cpuCapacity);
		return proto;
	}

}
