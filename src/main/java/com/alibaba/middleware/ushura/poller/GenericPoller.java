package com.alibaba.middleware.ushura.poller;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Hanson on 15/12/28.
 */
public class GenericPoller<T> implements Poller<T>{
    private AtomicInteger index = new AtomicInteger(0);
    private List<T> items = new ArrayList<T>();

    public GenericPoller(List<T> items){
        this.items = items;
    }

    public T next() {
        return items.get(Math.abs(index.getAndIncrement() % items.size()));
    }

    public Poller<T> refresh(List<T> items) {
       return new GenericPoller<T>(items);
    }
}
