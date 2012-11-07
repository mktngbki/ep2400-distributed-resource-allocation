package peersim.EP2400.resourcealloc.tasks.util;

import peersim.EP2400.resourcealloc.base.ApplicationsList;

public class Proposal {
	public enum ProposalType {
		PUSH, PULL, OVERLOADED_PUSH, NO_ACTION;
	}
	
	private ProposalType		pType;
	private ApplicationsList	appList;
	
	public Proposal(ProposalType pType, ApplicationsList appList) {
		this.pType = pType;
		this.appList = appList;
	}
	
	public ProposalType getProposalType() {
		return pType;
	}
	
	public ApplicationsList getApplicationsList() {
		return appList;
	}
	
	public Proposal switchType() {
		if (pType == ProposalType.PUSH) {
			return new Proposal(ProposalType.PULL, appList);
		} else if (pType == ProposalType.PULL) {
			return new Proposal(ProposalType.PUSH, appList);
		} else if (pType == ProposalType.NO_ACTION) {
			return new Proposal(ProposalType.NO_ACTION, appList);
		}
		else { //else if(pType == ProposalType.OVELOADED_PUSH) {
			return new Proposal(ProposalType.PULL, appList);
		}
	}
}