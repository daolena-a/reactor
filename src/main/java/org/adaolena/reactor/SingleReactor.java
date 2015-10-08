package org.adaolena.reactor;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by adaolena on 07/10/15.
 */
public class SingleReactor  <V,T extends Processable<V>> implements Runnable{

    T processor;
    String name;
    ConcurrentLinkedQueue<V> channel = new ConcurrentLinkedQueue<>();

    public SingleReactor(T processor,String name) {
        this.processor = processor;
        this.name = name;
        new Thread(this).start();

    }

    private AtomicBoolean work = new AtomicBoolean(true);
    @Override
    public void run() {
        while(work.get() || channel.size() > 0){
            boolean processing = true;
            while(processing){
                V val = channel.poll();
                if(val == null){
                    processing = false;
                }else{
                    processor.process(val);
                }
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public ConcurrentLinkedQueue<V> getChannel() {
        return channel;
    }

    public AtomicBoolean getWork() {
        return work;
    }

    public void stop() {
        work.compareAndSet(true,false);
        while(channel.size() > 0){
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    public void offer(V v) {
        channel.offer(v);
    }
}
