package nachos.threads;

import java.util.LinkedList;

import nachos.machine.*;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see	nachos.threads.Condition
 */
public class Condition2 {
    /**
     * Allocate a new condition variable.
     *
     * @param	conditionLock	the lock associated with this condition
     *				variable. The current thread must hold this
     *				lock whenever it uses <tt>sleep()</tt>,
     *				<tt>wake()</tt>, or <tt>wakeAll()</tt>.
     */
    public Condition2(Lock conditionLock) {
	this.conditionLock = conditionLock;
	waitQueue = new LinkedList<KThread>();
    }

    /**
     * Atomically release the associated lock and go to sleep on this condition
     * variable until another thread wakes it using <tt>wake()</tt>. The
     * current thread must hold the associated lock. The thread will
     * automatically reacquire the lock before <tt>sleep()</tt> returns.
     */
    public void sleep() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
	Machine.interrupt().disable();
	conditionLock.release();

	waitQueue.add(KThread.currentThread());  
	KThread.sleep();
	
	conditionLock.acquire();
	Machine.interrupt().enable();
    }

    /**
     * Wake up at most one thread sleeping on this condition variable. The
     * current thread must hold the associated lock.
     */
    public void wake() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
		if (!waitQueue.isEmpty()){
			Machine.interrupt().disable();
			(waitQueue.removeFirst()).ready();  
			Machine.interrupt().enable();
		}
    } 

    /**
     * Wake up all threads sleeping on this condition variable. The current
     * thread must hold the associated lock.
     */
    public void wakeAll() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
	Machine.interrupt().disable();
	while (!waitQueue.isEmpty())
	    wake();
	Machine.interrupt().enable();
    }
    
    private LinkedList<KThread> waitQueue;
    private Lock conditionLock;
    private static class Condition2Test implements Runnable {
    	Condition2Test(Lock lock, Condition2 condition) {
    	    this.condition = condition;
            this.lock = lock;
    	}
    	
    	public void run() {
            lock.acquire();
            System.out.print(KThread.currentThread().getName() + " acquired lock\n");	
            System.out.print(KThread.currentThread().getName() + " released lock and sleeped\n");
            condition.sleep();
            System.out.print(KThread.currentThread().getName() + " woken and acquired lock again\n");	
            lock.release();
            System.out.print(KThread.currentThread().getName() + " released lock again\n");	
    	}

        private Lock lock; 
        private Condition2 condition; 
    }
    public static void selfTest(){
    	System.out.println("-------- Condition2 Test --------");
    	Lock conditionLock = new Lock();
    	Condition2 condition2 = new Condition2(conditionLock);
    	
    	KThread[] threads = new KThread[7];
		for (int i = 0; i < 7; i ++){
			threads[i] = new KThread(new Condition2Test(conditionLock, condition2)).setName("Thread" + i);
		}
		for(int i = 0; i < 7; i ++)
			threads[i].fork();
		
		
		KThread.yield();
		conditionLock.acquire();
		
		System.out.print("conditionLock.wake() called\n");
		condition2.wake();
		conditionLock.release();
		threads[0].join();
		
		conditionLock.acquire();
		System.out.print("conditionLock.wakeAll() called\n");
		condition2.wakeAll();
		
		conditionLock.release();
		threads[threads.length-1].join();
    }
}
