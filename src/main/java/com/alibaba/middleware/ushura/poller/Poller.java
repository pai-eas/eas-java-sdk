package com.alibaba.middleware.ushura.poller;


import java.util.List;

/**
 * @author Hanson on 15/12/28.
 */
public interface Poller<T>{
    T next();
    Poller<T> refresh(List<T> items);
    enum PollerType{
        Generic,PowerOfTwoPoller;
    }
}
