package peersim.EP2400.resourcealloc.tasks;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
/**
 * Template class for Performance observer according to metrics discussed in the project description
 */
public class PerformanceObserver implements Control {

	/**
	 * The protocol to operate on.
	 * 
	 * @config
	 */
	private static final String PAR_PROT = "protocol";

	/**
	 * The number of applications
	 * 
	 * @config
	 */
	private static final String PAR_APPSCOUNT = "apps_count";

	private static final String PAR_R_MAX = "r_max";

	/** Protocol identifier, obtained from config property {@link #PAR_PROT}. */
	private final int pid;

	private final String prefix;
	private final int r_max;

/**
	 * Number of application
	 */
	protected int appsCount;

	
	 /**
	 * Standard constructor that reads the configuration parameters. Invoked by
	 * the simulation engine.
	 * 
	 * @param prefix
	 *            the configuration prefix identifier for this class.
	 */
	public PerformanceObserver(String prefix) {
		this.prefix = prefix;
		pid = Configuration.getPid(prefix + "." + PAR_PROT);
		appsCount = Configuration.getInt(prefix + "." + PAR_APPSCOUNT);
		r_max = Configuration.getInt(prefix + "." + PAR_R_MAX);

	}

	@Override
	public boolean execute() {

		int cycle = (int) CommonState.getTime();

		//TODO Implement your code for task 1.2 here
		

		return false;
	}

}
