package org.adaolena.reactor;

import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by adaolena on 07/10/15.
 *
 */
public class MultipleReactor<V, T extends Processable<V>>{
    final static Logger LOGGER = Logger.getLogger(MultipleReactor.class);

    volatile SingleReactor<V, T> reactorToWrite;
    Map<String, SingleReactor<V, T>> reactors = new ConcurrentHashMap<>();
    private AtomicBoolean work = new AtomicBoolean(true);
    Iterator<SingleReactor<V,T>> iterator ;
    Lock lock = new ReentrantLock();
    Condition isProcessing = lock.newCondition();
    public MultipleReactor(int reactorsNumber, T processor) {
        for (int i = 0; i < reactorsNumber; i++) {
            reactors.put("" + i, new SingleReactor<V, T>(processor, "" + i));
        }
        iterator = newCycleIterator(new ArrayList<SingleReactor<V, T>>(reactors.values()));
        //new Thread(this).start();
    }

    public void offer(V v) {


        SingleReactor<V,T> reactor = iterator.next();
        //LOGGER.info("dispatch to "+reactor.name);
        reactor.offer(v);

    }
    public static <T> Iterator<T> cycle(final Iterable<T> iterable) {
        return new Iterator<T>() {
            Iterator<T> iterator = new ArrayList<T>().iterator();
            Iterator<T> removeFrom;

            public boolean hasNext() {
                if (!iterator.hasNext()) {
                    iterator = iterable.iterator();
                }
                return iterator.hasNext();
            }
            public T next() {
                if (!hasNext()) {
                    iterator = iterable.iterator();
                }
                removeFrom = iterator;
                return iterator.next();
            }
            public void remove() {

            }
        };
    }

    CycleIterator<SingleReactor<V, T>> newCycleIterator(List<SingleReactor<V, T> > reactors){
        return new CycleIterator<SingleReactor<V, T>>(reactors);
    }
    public class CycleIterator<T> implements  Iterator<T>{
        Lock lock = new ReentrantLock();
        List<T> vals;
        volatile int currentindex;
        public CycleIterator(List<T> values) {
            vals = values;
        }

        @Override
        public boolean hasNext() {
            return true;
        }

        @Override
        public T next() {
            T val = vals.get(currentindex);
            lock.lock();
            if(currentindex == vals.size()-1){
                currentindex = 0;
            }
            else{
                currentindex++;
            }
            lock.unlock();
            return val;
        }

        @Override
        public void remove() {
            return;
        }
    }



    @Override
    public void run() {
        LOGGER.info("##########################################");

        while (work.get()) {
            int min = Integer.MAX_VALUE;
            SingleReactor<V,T> nextReactorToWork = reactorToWrite;
            for (SingleReactor<V, T> reactor : reactors.values()) {
                if(reactor.getChannel().size() == 0){
                    nextReactorToWork = reactor;
                    break;
                }
                if(reactor.getChannel().size() < min){
                    min = reactor.getChannel().size();
                    nextReactorToWork = reactor;
                }
            }
            if(reactorToWrite == null){
                LOGGER.info("##########################################choosed "+nextReactorToWork.name );

            }else{
                LOGGER.info("##########################################from "+reactorToWrite.name + " to "+nextReactorToWork.name);

            }
            reactorToWrite = nextReactorToWork;
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
                try{
                    stopAll();

                }catch(Exception ex){
                    ex.printStackTrace();
                }
            }

        }
    }

    public void stopAll() throws Exception{
        for (SingleReactor<V, T> reactor : reactors.values()) {
            LOGGER.info("waiting for "+reactor.name);
            int count = reactor.stop();
            LOGGER.info(reactor.name + " stopped, have processed "+count);

        }
        work.compareAndSet(true, false);

    }




}
