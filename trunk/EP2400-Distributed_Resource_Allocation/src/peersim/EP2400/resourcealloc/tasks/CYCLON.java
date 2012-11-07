/*
 * Copyright (c) 2010 LCN, EE school, KTH
 *
 */


package peersim.EP2400.resourcealloc.tasks;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
		private Node node;
		private int	age;

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

		//we need to check a lot of times if an entry of a certain node is contained in a list
		//so we do equality of Entry only based on nodes

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			//TODO Alex - can i used the node hashCode method here?
			result = prime * result + (int) (node.getID() ^ (node.getID() >>> 32)); 
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
			Entry other = (Entry) obj;
			//TODO Alex - can i used the node equals method here?
			if (node.getID() != other.getNode().getID()) 
				return false;
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
	private List<Entry> entries;

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
		entries = Collections.synchronizedList(new ArrayList<Entry>(cacheSize));
	}

	public CYCLON(String prefix, int cacheSize, int shuffleLength)
	{
		this.prefix = prefix;
		this.cacheSize = cacheSize;
		this.shuffleLength = shuffleLength;
		this.entries = Collections.synchronizedList(new ArrayList<Entry>(cacheSize));
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
		HashSet<Entry> shuffleList = new HashSet<Entry>();
		if (!entries.isEmpty()) {
			shuffleList = getShuffleNodes(shuffleLength - 1);
		}

		//Step 3 adding self with timestamp 0 in the list
		shuffleList.add(new Entry(n, 0));
		Set<Entry> sentShuffleList = (Set<Entry>)shuffleList.clone();
		//Step 4 and 5 send and receive a set of peers
		CYCLON neighborCyclon = (CYCLON) oldestNode.getProtocol(protocolID);
		Set<Entry> receivedShuffleList = neighborCyclon.shuffle(oldestNode, sentShuffleList);

		//Step 6 and 7 update entries list
		mergeNeighborLists(n, receivedShuffleList, shuffleList);
	}

	/*
	 * CYCLON passive thread method
	 */
	public Set<Entry> shuffle(Node destination, Set<Entry> recShuffleList) {
		//get shuffle response
		HashSet<Entry> shuffleList = getShuffleNodes(shuffleLength);
		//clone list that we send because we modify this list in the merge
		Set<Entry> sentShuffleList = (Set<Entry>)shuffleList.clone();
		//merge received shuffleList with own list
		mergeNeighborLists(destination, recShuffleList, shuffleList);
		return sentShuffleList;
	}

	/*
	 * synchronized on entries to make sure that entries is in a consistent state
	 * meaning that another thread is not deleting or adding nodes at this time
	 */
	private HashSet<Entry> getShuffleNodes(int nrOfNodes) {
		HashSet<Entry> shuffleNodes = new HashSet<Entry>();
		synchronized(entries) {
			int entriesSize = entries.size();
			if(entriesSize < nrOfNodes) {
				shuffleNodes.addAll(entries);
			} else {
				while(shuffleNodes.size() < nrOfNodes) {
					int nodeIndex = CommonState.r.nextInt(entriesSize);
					shuffleNodes.add(entries.get(nodeIndex));
				}
			}
		}
		return shuffleNodes;
	}

	public void mergeNeighborLists(Node self, Set<Entry> receivedNodes, Set<Entry> sentNodes) {
		Iterator<Entry> iterator;

		//Discard from receivedNodes the following entries:
		//1. entries pointing at self
		//2. entries that you already have - not sure if should remove the entry from received or the oldest entry
		//for(Entry e : receivedNodes) { ... receivedNodes.remove..} throws ConcurrentModeificationException
		//		iterator = receivedNodes.iterator();
		//		while (iterator.hasNext()) {
		//		    Entry e = iterator.next();
		//		    if (e.getNode().getID() == self.getID()) {
		//		        iterator.remove();
		//		    }
		//		    if()
		//		}		

		//our Entry equals does the equal method only on node not on age. so we consider entries of different age on same node as beeing equal
		receivedNodes.remove(new Entry(self, 0));
		receivedNodes.removeAll(entries);

		//To be safe - Double check that entries does not contain self
		//since i make sure that what i add(receivedNodes) does not contain self
		//I could remove this if I know for sure that the initialization of this list does not contain self
		//		synchronized(entries) {
		//			iterator = entries.iterator();
		//			while (iterator.hasNext()) {
		//			    Entry e = iterator.next();
		//			    if (e.getNode().getID() == self.getID()) {
		//			        iterator.remove();
		//			    }
		//			}	
		//		}
		entries.remove(new Entry(self, 0));

		//Reduce entries list size by removing random nodes that were sent to the other node
		entries.addAll(receivedNodes);
		List<Entry> sentNodesList = new ArrayList<Entry>(sentNodes);
		System.out.println(sentNodes.size() + " " + entries.size());
		while (entries.size() > cacheSize) {
			int index = CommonState.r.nextInt(sentNodes.size());
			Entry removedEntry = sentNodesList.remove(index);
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