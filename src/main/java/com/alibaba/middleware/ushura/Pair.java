package com.alibaba.middleware.ushura;

public class Pair<T> {
    private T item;
    private double weight;

    public Pair(T item,double weight){
        this.item = item;
        this.weight = weight;
    }

    public T item(){
        return item;
    }

    public double weight(){
        return weight;
    }

}
