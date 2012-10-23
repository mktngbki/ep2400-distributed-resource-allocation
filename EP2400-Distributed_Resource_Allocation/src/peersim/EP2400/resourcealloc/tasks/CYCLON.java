/*
 * Copyright (c) 2010 LCN, EE school, KTH
 *
 */


package peersim.EP2400.resourcealloc.tasks;


import java.util.ArrayList;
import java.util.List;

import peersim.cdsim.*;
import peersim.config.*;
import peersim.core.*;

/**
 * 
 * Template class for CYCLON implementation
 *
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


private void validate()
{
	if (entries.size() > cacheSize)
	{
		System.out.println(" CYCLON constraint is invalid : Entry size is higher than cache size");
		System.out.println(" Terminating now");
	}

}



// ===================== CDProtocol implementations ===================
// ====================================================================

public void nextCycle(Node n, int protocolID)
{
	validate();
	//TODO Implement your code for task 1.1 here
	
}




@Override
public void onKill() {}


}
