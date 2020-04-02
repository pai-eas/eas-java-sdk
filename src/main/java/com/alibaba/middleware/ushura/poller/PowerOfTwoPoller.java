package com.alibaba.middleware.ushura.poller;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class PowerOfTwoPoller<T> implements Poller<T>{
    private AtomicInteger index = new AtomicInteger(0);
    private List<T> items = new ArrayList<T>();

    public PowerOfTwoPoller(List<T> items){
        this.items = items;
    }

    public T next() {
        return items.get(index.getAndIncrement() & items.size() - 1);
    }

    public Poller<T> refresh(List<T> items) {
        return new PowerOfTwoPoller<T>(items);
    }
}
