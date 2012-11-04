package peersim.EP2400.resourcealloc.tasks;

import peersim.config.Configuration;
import peersim.core.Control;

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
	private final int			pid;
	
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
		pid = Configuration.getPid(prefix + "." + PAR_PROT);
		r_max = Configuration.getInt(prefix + "." + PAR_R_MAX);
		appsCount = Configuration.getInt(prefix + "." + PAR_APPSCOUNT);
		
	}
	
	
	@Override
	public boolean execute() {
		
		// TODO: Implement your code for task 1 here...
		
		
		return false;
	}
	
}
