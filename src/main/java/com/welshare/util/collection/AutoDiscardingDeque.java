package com.welshare.util.collection;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingDeque;

public class AutoDiscardingDeque<E> extends LinkedBlockingDeque<E> implements ListDeque<E> {

    public AutoDiscardingDeque() {
        super();
    }

    public AutoDiscardingDeque(int capacity) {
        super(capacity);
    }

    @Override
    public synchronized boolean offerFirst(E e) {
        if (remainingCapacity() == 0) {
            removeLast();
        }
        super.offerFirst(e);
        return true;
    }

    public synchronized boolean addAllFirst(Collection<? extends E> c) {
        if (c == null) {
            throw new NullPointerException();
        }
        if (c == this) {
            throw new IllegalArgumentException();
        }
        boolean modified = false;
        Iterator<? extends E> e = c.iterator();
        while (e.hasNext()) {
            if (offerFirst(e.next())) {
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public int indexOf(Object object) {
        int i = 0;
        for (E e : this) {
            if (object == null && e == null) {
                return i;
            }
            if (e != null && e.equals(object)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    @Override
    public synchronized boolean addAll(Collection<? extends E> c) {
         if (c == null) {
             throw new NullPointerException();
         }
         if (c == this) {
             throw new IllegalArgumentException();
         }
         boolean modified = false;
         Iterator<? extends E> e = c.iterator();
         while (e.hasNext()) {
             if (remainingCapacity() == 0) {
                 return modified;
             }
             if (add(e.next())) {
                 modified = true;
             }
         }
         return modified;
    }
}
