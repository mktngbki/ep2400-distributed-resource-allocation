/*
 * Copyright (c) 2010 LCN, EE school, KTH
 *
 */


package peersim.EP2400.resourcealloc.tasks;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import peersim.cdsim.CDProtocol;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Node;

/**
 * This class is for CYCLON protocol implementation
 */
public class CYCLON implements CDProtocol, Linkable
{
	
	public static class Entry implements Comparable<Object>
	{
		private Node	node;
		private int		age;
		
		public Entry(Node node, int age) {
			this.node = node;
			this.age = age;
		}
		
		public Node getNode() {
			return node;
		}
		
		public int getAge() {
			return age;
		}
		
		public void incrementAge() {
			age++;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (int) (node.getID() ^ (node.getID() >>> 32));
			return result;
		}
		
		
		//we need to check a lot of times if an entry of a certain node is contained in a list
		//so we do equality of Entry only based on comparison of nodes
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
			Entry other = (Entry) obj;
			if (node.getID() != other.getNode().getID()) {
				return false;
			}
			return true;
		}
		
		@Override
		public int compareTo(Object otherEntry) {
			// If passed object is of type other than Entry, throw ClassCastException.
			if (!(otherEntry instanceof Entry)) {
				throw new ClassCastException("It has been detected the presence of an invalid object!");
			}
			int age = ((Entry) otherEntry).getAge();
			
			if (getAge() > age) {
				return 1;
			} else if (getAge() < age) {
				return -1;
			} else {
				return 0;
			}
		}
		
	}
	
	private List<Entry>			entries;
	
	private final int			cacheSize;
	private final int			shuffleLength;
	private String				prefix;
	/**
	 * Cache size.
	 * @config
	 */
	private static final String	PAR_CACHE			= "cache_size";
	/**
	 * Shuffle Length.
	 * @config
	 */
	private static final String	PAR_SHUFFLE_LENGTH	= "shuffle_length";
	
	// ====================== initialization ===============================
	// =====================================================================
	
	public CYCLON(String prefix)
	{
		this.prefix = prefix;
		cacheSize = Configuration.getInt(prefix + "." + PAR_CACHE);
		shuffleLength = Configuration.getInt(prefix + "." + PAR_SHUFFLE_LENGTH);
		entries = new ArrayList<Entry>(cacheSize);
	}
	
	public CYCLON(String prefix, int cacheSize, int shuffleLength)
	{
		this.prefix = prefix;
		this.cacheSize = cacheSize;
		this.shuffleLength = shuffleLength;
		entries = new ArrayList<Entry>(cacheSize);
	}
	
	// ---------------------------------------------------------------------
	
	@Override
	public Object clone()
	{
		CYCLON cyclon = new CYCLON(prefix, cacheSize, shuffleLength);
		return cyclon;
	}
	
	// ====================== Linkable implementation =====================
	// ====================================================================
	
	@Override
	public Node getNeighbor(int i)
	{
		return entries.get(i).getNode();
	}
	
	// --------------------------------------------------------------------
	
	/** Might be less than cache size. */
	@Override
	public int degree()
	{
		return entries.size();
	}
	
	// --------------------------------------------------------------------
	
	@Override
	public boolean addNeighbor(Node node) {
		Entry a = new Entry(node, 0);
		return entries.add(a);
		
	}
	
	// --------------------------------------------------------------------
	
	@Override
	public void pack() {
	}
	
	// --------------------------------------------------------------------
	
	@Override
	public boolean contains(Node n)
	{
		
		for (int i = 0; i < entries.size(); i++) {
			
			if (entries.get(i).getNode().equals(n))
			{
				return true;
			}
		}
		return false;
	}
	
	
	// ===================== CDProtocol implementations ===================
	// ====================================================================
	
	/*
	 * CYCLON active thread method 
	 */
	@Override
	public void nextCycle(Node n, int protocolID)
	{
		//If the current node does not have any neighbors there is no reason to run the method
		if (entries.size() == 0) {
			return;
		}
		
		validate();
		
		//Step 1 increase age of nodes
		increaseNeighborsAge();
		
		//Step 2.1 get oldest alive node
		Node oldestNode = getOldestNode();
		
		//If the oldestNode is null it means that the current node does not have any active neighbor
		//therefore there is no reason to continue the execution of the method
		if (oldestNode == null) {
			return;
		}
		
		//Step 2.2 get nodes for shuffle - It is used shuffleLength - 1 since the node puts its reference in the next step
		ArrayList<Entry> shuffleList = new ArrayList<Entry>();
		if (!entries.isEmpty()) {
			shuffleList = getShuffleNodes(shuffleLength - 1);
		}
		
		//Step 3 adding self with timestamp 0 in the list
		shuffleList.add(new Entry(n, 0));
		@SuppressWarnings("unchecked")
		List<Entry> sentShuffleList = (List<Entry>) shuffleList.clone();
		//Step 4 and 5 send and receive a set of peers
		CYCLON neighborCyclon = (CYCLON) oldestNode.getProtocol(protocolID);
		List<Entry> receivedShuffleList = neighborCyclon.shuffle(oldestNode, sentShuffleList);
		
		//Step 6 and 7 update entries list
		mergeNeighborLists(n, receivedShuffleList, shuffleList);
	}
	
	/*
	 * CYCLON passive thread method
	 */
	public List<Entry> shuffle(Node destination, List<Entry> recShuffleList) {
		//get shuffle response
		ArrayList<Entry> shuffleList = getShuffleNodes(shuffleLength);
		//clone list that we send because we modify this list in the merge
		@SuppressWarnings("unchecked")
		List<Entry> sentShuffleList = (List<Entry>) shuffleList.clone();
		//merge received shuffleList with own list
		mergeNeighborLists(destination, recShuffleList, shuffleList);
		return sentShuffleList;
	}
	
	private ArrayList<Entry> getShuffleNodes(int nrOfNodes) {
		ArrayList<Entry> shuffleNodes = new ArrayList<Entry>();
		int entriesSize = entries.size();
		if (entriesSize < nrOfNodes) {
			shuffleNodes.addAll(entries);
		} else {
			while (shuffleNodes.size() < nrOfNodes) {
				int nodeIndex = CommonState.r.nextInt(entriesSize);
				Entry e = entries.get(nodeIndex);
				if (!shuffleNodes.contains(e)) {
					shuffleNodes.add(e);
				}
			}
		}
		return shuffleNodes;
	}
	
	public void mergeNeighborLists(Node self, List<Entry> receivedNodes, List<Entry> sentNodes) {
		//Discard from receivedNodes the following entries:
		//1. entries pointing at self
		//2. entries that you already have - not sure if should remove the entry from received or the oldest entry
		//our Entry equals does the equal method only on node not on age. so we consider entries of different age on same node as beeing equal
		List<Entry> l = new ArrayList<Entry>();
		l.add(new Entry(self, 0));
		receivedNodes.removeAll(l);
		receivedNodes.removeAll(entries);
		
		//To be safe - Double check that entries does not contain self
		//since i make sure that what i add(receivedNodes) does not contain self
		//I could remove this if I know for sure that the initialization of this list does not contain self
		entries.remove(l);
		
		//Reduce entries list size by removing random nodes that were sent to the other node
		entries.addAll(receivedNodes);
		while (entries.size() > cacheSize) {
			int index = CommonState.r.nextInt(sentNodes.size());
			Entry removedEntry = sentNodes.remove(index);
			entries.remove(removedEntry);
		}
	}
	
	private void increaseNeighborsAge() {
		for (Entry entry : entries) {
			entry.incrementAge();
		}
	}
	
	private Node getOldestNode() {
		Node oldestNode = null;
		
		do {
			if (entries.isEmpty()) {
				return null;
			}
			
			Collections.sort(entries);
			int lastIndex = entries.size() - 1;
			oldestNode = entries.get(lastIndex).getNode();
			
			//The node needs to be removed both if it is not up or if it is the chosen one to shuffle lists with
			entries.remove(lastIndex);
			
			if (!oldestNode.isUp()) {
				oldestNode = null;
			}
		} while (null == oldestNode);
		
		return oldestNode;
	}
	
	// Simple validation
	private void validate()
	{
		if (entries.size() > cacheSize)
		{
			System.out.println(" CYCLON constraint is invalid : Entry size is higher than cache size");
			System.out.println(" Terminating now");
			System.exit(1);
		}
		
	}
	
	@Override
	public void onKill() {
		
	}
	
}