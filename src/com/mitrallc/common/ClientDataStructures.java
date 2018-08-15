package com.mitrallc.common;

/**
 * This class contains the data structures used to hold
 * 
 * Client ID - which is assigned by the coordinator
 * 
 * Port List - used to communicate with the coordinator
 * 
 * Invalidation Port Number - Port on which the client listens for invalidation
 * messages sent by the coordinator.
 * 
 * @author Lakshmy Mohanan
 * @author Neeraj Narang
 * 
 */
public class ClientDataStructures {
	private long ID;
//	private byte[] ports;
	private int invalidationPort; 
//	private int leaseTerm;
//	public HashMap<String, Long> queryLocks = new HashMap<String, Long>();

	public long getID() {
		return ID;
	}

	public void setID(long iD) {
		ID = iD;
	}

//	public byte[] getPorts() {
//		return ports;
//	}
//
//	public void setPorts(byte[] ports) {
//		this.ports = ports;
//	}

	public int getInvalidationPort() {
		return invalidationPort;
	}

	public void setInvalidationPort(int invalidationPort) {
		this.invalidationPort = invalidationPort;
	}
	
//	public void setLeaseTerm(int t) {
//		leaseTerm = t;
//	}
//	
//	public int getLeaseTerm() {
//		return leaseTerm;
//	}
}
