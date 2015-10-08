package org.adaolena.reactor;

/**
 * Created by adaolena on 07/10/15.
 */
public interface Processable<V> {
    void process(V v);
}
