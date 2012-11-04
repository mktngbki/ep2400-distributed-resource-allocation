package peersim.EP2400.resourcealloc.tasks.util;

import java.util.List;

public class Proposal {
	public enum ProposalType {
		PUSH, PULL, NO_ACTION;
	}
	
	private ProposalType			pType;
	private List<ApplicationInfo>	appList;
	
	public Proposal(ProposalType pType, List<ApplicationInfo> appList) {
		this.pType = pType;
		this.appList = appList;
	}
	
	public ProposalType getProposalType() {
		return pType;
	}
	
	public List<ApplicationInfo> getApplicationsList() {
		return appList;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((appList == null) ? 0 : appList.hashCode());
		result = prime * result + ((pType == null) ? 0 : pType.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Proposal other = (Proposal) obj;
		if (appList == null) {
			if (other.appList != null) {
				return false;
			}
		} else if (!appList.equals(other.appList)) {
			return false;
		}
		if (pType != other.pType) {
			return false;
		}
		return true;
	}
	
	
}
