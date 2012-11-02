package peersim.EP2400.resourcealloc.tasks.util;

import peersim.EP2400.resourcealloc.base.Application;
import peersim.core.Node;

public class ApplicationInfo {
	private Application app;
	private Node appHome; //this epoch's initial home of the app
	private boolean appMoved; // the application is native or not. I put this here as Node does not offer an equal method(compare nodes just based on nodeId?

	public ApplicationInfo(Application app, Node appHome) {
		this.app = app;
		this.appHome = appHome;
		this.appMoved = false;
	}
	
	public Application getApplication() {
		return app;
	}
	
	public Node getHomeNode() {
		return appHome;
	}
	
	public boolean appMoved() {
		return appMoved;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + app.getID();
		result = prime * result + (int) (appHome.getID() ^ (appHome.getID() >>> 32));
		result = prime * result + (appMoved ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ApplicationInfo other = (ApplicationInfo) obj;
		if (app.getID() != other.app.getID())
			return false;
		if (appMoved != other.appMoved)
			return false;
		if (appHome.getID() != other.appHome.getID())
			return false;
		return true;
	}
}

