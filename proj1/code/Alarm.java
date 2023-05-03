package nachos.threads;

import java.util.LinkedList;
import java.util.PriorityQueue;
import nachos.machine.*;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm() {
    	
	waitQueue = new PriorityQueue<AlarmedThread>();
	Machine.timer().setInterruptHandler(new Runnable() {
		public void run() { timerInterrupt(); }
	    });
	}

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {
    	Machine.interrupt().disable();
    	while(!waitQueue.isEmpty()){
    		if (waitQueue.peek().timeToWake <= Machine.timer().getTime())
    			waitQueue.poll().thread.ready();
    		else
    			break;
    	}
		Machine.interrupt().enable();
		KThread.currentThread().yield();
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
	// for now, cheat just to get something working (busy waiting is bad)
	long timeToWake = Machine.timer().getTime() + x;
	Machine.interrupt().disable();
	waitQueue.add(new AlarmedThread(KThread.currentThread(), timeToWake));
	KThread.currentThread().sleep();
	Machine.interrupt().enable();
    }
    
    private class AlarmedThread implements Comparable<AlarmedThread> {
    	private KThread thread;
    	private long timeToWake;
    	AlarmedThread(KThread thread, long timeToWake){
    		this.thread = thread;
    		this.timeToWake = timeToWake;
    	}
    	
    	@Override
    	public int compareTo(AlarmedThread that){
    		if (timeToWake <= that.timeToWake)
    			return -1;
    		else
    			return 1;
    	}
    }
    
    private PriorityQueue<AlarmedThread> waitQueue;
    
    private static class AlarmTest implements Runnable{
    	AlarmTest(long x){
    		this.time = x;
    	}
    	
    	public void run(){
    		long time2 = Machine.timer().getTime();
    		ThreadedKernel.alarm.waitUntil(time);
    		System.out.println(KThread.currentThread().getName() + 
    				" should be woken after " + 
    				time);
    		System.out.println(KThread.currentThread().getName() + 
    				" woken after " + 
    				(Machine.timer().getTime()-time2));
    		
    	}
    	
    	private long time;
    }
    
    public static void selfTest(){
    	System.out.println("-------- Alarm Test --------");
    	Runnable r = new Runnable() {
    	    public void run() {
                    KThread t[] = new KThread[7];
                    for (int i=0; i<7; i++) {
                         t[i] = new KThread(new AlarmTest((970 + i*970) % 2011));
                         t[i].setName("Thread" + i);
                         t[i].fork();
                    }
                    for(int i=0; i<7; i++){
                    	t[i].join();
                    }
                }
        };

        KThread t = new KThread(r);
        t.fork();
        t.join();
    }
}
