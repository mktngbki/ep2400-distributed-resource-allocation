/*
 * Copyright (c) 2010 LCN, EE school, KTH
 *
 */


package peersim.EP2400.resourcealloc.tasks;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import peersim.cdsim.CDProtocol;
import peersim.config.Configuration;
import peersim.core.Linkable;
import peersim.core.Node;

/**
 * This class is for CYCLON protocol implementation
 */
public class CYCLON implements CDProtocol, Linkable
{
	
	public static class Entry implements Comparable<Object>
	{
		public Entry(Node node, int age)
		{
			this.node = node;
			this.age = age;
			
		}
		
		private Node	node;
		
		public Node getNode() {
			return node;
		}
		
		public void setNode(Node node) {
			this.node = node;
		}
		
		public int getAge() {
			return age;
		}
		
		public void setAge(int age) {
			this.age = age;
		}
		
		private int	age;
		
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
	
	@Override
	public void nextCycle(Node n, int protocolID)
	{
		System.out.println("--------------> entries.size(): " + entries.size());
		
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
		List<Entry> sentShuffleList = new ArrayList<Entry>();
		if (!entries.isEmpty()) {
			sentShuffleList = getShuffleNodes(shuffleLength - 1);
		}
		
		//Step 3 adding self with timestamp 0 in the list
		sentShuffleList.add(new Entry(n, 0));
		
		//Step 4 and 5 send and receive a set of peers
		CYCLON neighborCyclon = (CYCLON) oldestNode.getProtocol(protocolID);
		List<Entry> receivedShuffleList = neighborCyclon.shuffle(oldestNode, sentShuffleList);
		
		//Step 6 and 7 update entries list
		mergeNeighborLists(n, receivedShuffleList, sentShuffleList);
	}
	
	public List<Entry> shuffle(Node destination, List<Entry> recShuffleList) {
		//get shuffle response
		List<Entry> sentShuffleList = getShuffleNodes(shuffleLength);
		
		//merge shuffleList with own list
		mergeNeighborLists(destination, recShuffleList, sentShuffleList);
		
		return sentShuffleList;
	}
	
	private void increaseNeighborsAge() {
		for (Entry entry : entries) {
			int oldAge = entry.getAge();
			entry.setAge(oldAge + 1);
		}
	}
	
	private Node getOldestNode() {
		Node oldestNode = null;
		
		do {
			if (entries.isEmpty()) {
				return oldestNode;
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
	
	private List<Entry> getShuffleNodes(int nrOfNodes) {
		List<Entry> shuffleNodes = new ArrayList<Entry>();
		Random r = new Random(1234567890);
		int entriesSize = entries.size();
		for (int i = 0; i < nrOfNodes; i++) {
			int nodeIndex = r.nextInt(entriesSize);
			shuffleNodes.add(entries.get(nodeIndex));
		}
		return shuffleNodes;
	}
	
	public void mergeNeighborLists(Node self, List<Entry> receivedList, List<Entry> sentList) {
		//Discard entries pointing at self
		if (receivedList.contains(self)) {
			List<Node> selfList = new ArrayList<Node>();
			selfList.add(self);
			receivedList.removeAll(selfList);
		}
		
		//To be safe - Double check that entries does not contain self
		if (entries.contains(self)) {
			List<Node> selfList = new ArrayList<Node>();
			selfList.add(self);
			entries.removeAll(selfList);
		}
		
		//Discard entries from received that are already contained in the owned list(entries)
		for (Entry entry : receivedList) {
			if (entries.contains(entry)) {
				receivedList.remove(entry);
			}
		}
		
		//Reduce entries list size by removing random nodes that were sent to the other node
		entries.addAll(receivedList);
		Random r = new Random(1234567890);
		while (entries.size() > cacheSize) {
			int index = r.nextInt(sentList.size());
			Entry removedEntry = sentList.remove(index);
			entries.remove(removedEntry);
		}
		
		Collections.sort(entries);
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