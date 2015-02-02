package com.welshare.util.collection;

import java.util.Collection;
import java.util.Deque;

public interface ListDeque<E> extends Deque<E> {

    boolean addAllFirst(Collection<? extends E> c);

    int indexOf(Object object);
}
