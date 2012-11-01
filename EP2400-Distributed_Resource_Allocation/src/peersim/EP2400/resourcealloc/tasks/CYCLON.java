/*
 * Copyright (c) 2010 LCN, EE school, KTH
 *
 */


package peersim.EP2400.resourcealloc.tasks;


import java.util.ArrayList;
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

	public static class Entry
	{
		public Entry(Node node, int age)
		{
			this.node = node;
			this.age = age;

		}
		private Node node;
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
		private int age;


	}


	private List<Entry> entries;

	private final int cacheSize;
	private final int shuffleLength;


	private String prefix;
	/**
	 * Cache size.
	 * @config
	 */
	private static final String PAR_CACHE = "cache_size";


	/**
	 * Shuffle Length.
	 * @config
	 */
	private static final String PAR_SHUFFLE_LENGTH = "shuffle_length";

	// ====================== initialization ===============================
	// =====================================================================

	public CYCLON(String prefix)
	{
		this.prefix = prefix;
		this.cacheSize = Configuration.getInt(prefix + "." + PAR_CACHE);
		this.shuffleLength = Configuration.getInt(prefix + "." + PAR_SHUFFLE_LENGTH);
		this.entries = new ArrayList<Entry>(cacheSize);
	}

	public CYCLON(String prefix, int cacheSize, int shuffleLength)
	{
		this.prefix = prefix;
		this.cacheSize = cacheSize;
		this.shuffleLength = shuffleLength;
		this.entries  = new ArrayList<Entry>(cacheSize);
	}

	// ---------------------------------------------------------------------

	public Object clone()
	{

		CYCLON cyclon = new CYCLON(this.prefix, this.cacheSize, this.shuffleLength);
		return cyclon;
	}



	// ====================== Linkable implementation =====================
	// ====================================================================


	public Node getNeighbor(int i)
	{
		return entries.get(i).getNode();
	}

	// --------------------------------------------------------------------

	/** Might be less than cache size. */
	public int degree()
	{
		return entries.size();
	}

	// --------------------------------------------------------------------

	public boolean addNeighbor(Node node){ 
		Entry a = new Entry(node, 0);
		return this.entries.add(a);

	}

	// --------------------------------------------------------------------

	public void pack(){}

	// --------------------------------------------------------------------

	public boolean contains(Node n)
	{

		for (int i=0; i < entries.size(); i++){

			if (entries.get(i).getNode().equals(n))
			{
				return true;
			}
		}
		return false;
	}


	// ===================== CDProtocol implementations ===================
	// ====================================================================

	public void nextCycle(Node n, int protocolID)
	{
		validate();
		
		//step 1 increase age of ndoes
		increaseNeighborsAge();
		
		//step 2 get oldest alive node
		Node oldestNode = null;
		do {
			int indexON = getOldestNodeIndex();
			oldestNode = entries.get(indexON).getNode();
			entries.remove(indexON); //I have to remove the node from the entries if the node is not up or if i am exchanging shuffle lists with it
			if(!oldestNode.isUp()) {
				oldestNode = null;
			}
		} while(null == oldestNode);
		
		//step 2 get nodes for shuffle
		List<Entry> sentShuffleList = getShuffleNodes(shuffleLength - 1);
		
		//step 3 adding self with timestamp 0 in the list
		sentShuffleList.add(new Entry(n, 0));
		
		//step 4 and 5
		CYCLON neighborCyclon = (CYCLON)oldestNode.getProtocol(protocolID);
		List<Entry> receivedShuffleList = neighborCyclon.shuffle(oldestNode, sentShuffleList);
		
		//step 6 and 7
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
		for(int i = 0; i < entries.size(); i++) {
			int oldAge = entries.get(i).getAge();
			entries.get(i).setAge(oldAge+1);
		}
	}

	private int getOldestNodeIndex() {
		//it would be easier if I could sort the neighbors by age and insert new neighbors based on age
		//but because of the current addNeighbors implementation it can't be done. Not sure if method can be changed
		int indexON = 0; //index of oldest node
		int ageON = entries.get(0).getAge(); //age of oldest node
		for(int i = 1; i < entries.size(); i++) {
			if(ageON < entries.get(i).getAge()) {
				ageON = entries.get(i).getAge();
				indexON = i;
			}
		}
		return indexON;
	}
	
	private List<Entry> getShuffleNodes(int nrOfNodes) {
		List<Entry> shuffleNodes = new ArrayList<Entry>();
		Random r = new Random(1234567890);
		int entriesSize = entries.size();
		for(int i = 0; i < nrOfNodes; i++) {
			int nodeIndex = r.nextInt(entriesSize);
			shuffleNodes.add(entries.get(nodeIndex));
		}
		return shuffleNodes;
	}
	
	public void mergeNeighborLists(Node self, List<Entry> receivedList, List<Entry> sentList) {
		//discard entries pointing at self
		while(receivedList.contains(self)) {
			receivedList.remove(self);
		}
		//entries should not contain self... but just to make sure. I do not know how the init of entries gets done...so initially self could be there.
		while(entries.contains(self)) {
			entries.remove(self);
		}
		
		//discard entries from received that are already contained in the owned list(entries)
		for(Entry e : receivedList) {
			if(entries.contains(e)) {
				receivedList.remove(e);
			}
		}
		
		//reduce entries list size by removing random nodes that were sent to the other node
		entries.addAll(receivedList);
		Random r = new Random(1234567890);
		while(entries.size() > cacheSize) {
			int index = r.nextInt(sentList.size());
			Entry removedEntry = sentList.remove(index);
			entries.remove(removedEntry);
		}
		
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