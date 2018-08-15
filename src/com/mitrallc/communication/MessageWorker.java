package com.mitrallc.communication;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

/**
 * Worker to handle sending messages
 * @author hieun
 *
 */
public class MessageWorker implements Runnable {
	private ConcurrentLinkedQueue<MessageJob> msgJobQueue;
	private volatile boolean isStop;
	private Semaphore semaphore;
	
	protected CoreConnector coreConnector;
	
	public MessageWorker(ConcurrentLinkedQueue<MessageJob> msgJobQueue, Semaphore semaphore) {
		this.msgJobQueue = msgJobQueue;
		this.isStop = false;
		this.semaphore = semaphore;
		this.coreConnector = new CoreConnector();
	}
	
	public void stop() {
		isStop = true;
		
		for (int i = 0; i < 10; i++)
			semaphore.release();
	}

	@Override
	public void run() {
		while (true) {
			try {
				semaphore.acquire();
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace(System.out);
			}
			
			if (isStop) {
				break;
			}
			
			// pick up a job
			MessageJob job = msgJobQueue.poll();
			if (job == null) {
				System.out.println("MessageWorker: Got a null job");
				continue;
			}
			
			// execute the job
			job.execute(coreConnector);
		}
	}	
}
