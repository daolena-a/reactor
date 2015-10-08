package org.adaolena.reactor;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by adaolena on 07/10/15.
 *
 */
public class MultipleReactor<V, T extends Processable<V>>{
    final static Logger LOGGER = Logger.getLogger(MultipleReactor.class);

    volatile SingleReactor<V, T> reactorToWrite;
    Map<String, SingleReactor<V, T>> reactors = new ConcurrentHashMap<>();
    Iterator<SingleReactor<V,T>> iterator ;
    public MultipleReactor(int reactorsNumber, T processor) {
        for (int i = 0; i < reactorsNumber; i++) {
            reactors.put("" + i, new SingleReactor<V, T>(processor, "" + i));
        }
        iterator = cycle(reactors.values());
        //new Thread(this).start();
    }

    public void offer(V v) {

        //really simple round robin
        SingleReactor<V,T> reactor = iterator.next();
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
                    throw new NoSuchElementException();
                }
                removeFrom = iterator;
                return iterator.next();
            }
            public void remove() {

            }
        };
    }


/**
 * Will be used to a more sophiticate
    @Override
    public void run() {

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

            reactorToWrite = nextReactorToWork;
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
                stopAll();
            }

        }
    }
*/
    /**
     * Stop all reactors
     */
    public void stopAll() {
        for (SingleReactor<V, T> reactor : reactors.values()) {
            reactor.stop();
        }
    }

    public boolean isDone(){
        for (SingleReactor<V, T> reactor : reactors.values()) {
            if (reactor.getChannel().size() > 0){
                return false;
            }
        }
        return true;
    }

}
