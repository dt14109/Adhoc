package com.noc_list.noc_list;

/**
 * A simple Tuple Generic class
 */
public class Tuple<A, B> {
	public A tuple1;
    public B tuple2;

    public Tuple() {
    }
    
    public Tuple(A a, B b) {
        this.tuple1 = a;
        this.tuple2 = b;
    }
}