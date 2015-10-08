package org.adaolena.reactor;

import org.apache.log4j.Logger;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by adaolena on 07/10/15.
 */
public class SingleReactor<V, T extends Processable<V>> implements Runnable {

    final static Logger LOGGER = Logger.getLogger(SingleReactor.class);
    T processor;
    String name;
    ConcurrentLinkedQueue<V> channel = new ConcurrentLinkedQueue<>();

    AtomicInteger count = new AtomicInteger(0);

    ReentrantLock lock = new ReentrantLock();
    Condition workToDo = lock.newCondition();
    Lock endLock = new ReentrantLock();
    Condition waitingForEnd = endLock.newCondition();

    public Lock getLock() {
        return lock;
    }

    public Condition getWorkToDo() {
        return workToDo;
    }

    public SingleReactor(T processor, String name) {
        this.processor = processor;
        this.name = name;
        new Thread(this).start();

    }

    private AtomicBoolean work = new AtomicBoolean(true);

    @Override
    public void run() {
        while (work.get() || channel.size() > 0) {
            // lock.lock();
            boolean processing = true;
            while (processing) {
                V val = channel.poll();
                if (val == null) {
                    processing = false;
                } else {
                    // LOGGER.info(name + " processing");
                    processor.process(val);
                    count.incrementAndGet();
                }
            }
           /* lock.lock();
            try {
                workToDo.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            lock.unlock();*/
         /*   if(work.get()){

                try {
                    LOGGER.info(name + " await for work");

                    workToDo.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
            lock.unlock();*/

        }
        LOGGER.info(name + " stopping thread");

        endLock.lock();
        waitingForEnd.signal();
        endLock.unlock();

    }

    public ConcurrentLinkedQueue<V> getChannel() {
        return channel;
    }

    public AtomicBoolean getWork() {
        return work;
    }

    public int stop() throws Exception {
        work.compareAndSet(true, false);
        //lock.lock();
        //workToDo.signal();
        //lock.unlock();
        endLock.lock();
        waitingForEnd.await();
        endLock.unlock();
        return count.intValue();
    }


    public void offer(V v) {

        channel.offer(v);
        /*if(!lock.isLocked()){
            lock.lock();
            workToDo.signal();
            lock.unlock();
        }*/

    }
}
