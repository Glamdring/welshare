package com.welshare.util.collection;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.joda.time.DateMidnight;

public final class CollectionUtils {

    @SuppressWarnings("rawtypes")
    public static final Queue EMPTY_QUEUE = new LinkedList();

    @SuppressWarnings("rawtypes")
    public static final Deque EMPTY_DEQUE = new ArrayDeque();

    @SuppressWarnings("rawtypes")
    public static final ListDeque EMPTY_LIST_DEQUE = new AutoDiscardingDeque();

    private CollectionUtils() { }

    public static int[] getMaxAndSum(Map<DateMidnight, Integer> map) {
        int max = 0;
        int sum = 0;
        for (Integer count : map.values()) {
            sum += count;
            max = Math.max(max, count);
        }

        return new int[] {max, sum};
    }

    @SuppressWarnings("unchecked")
    public static <T> Queue<T> emptyQueue() {
        return EMPTY_QUEUE;
    }

    @SuppressWarnings("unchecked")
    public static <T> Deque<T> emptyDeque() {
        return EMPTY_DEQUE;
    }

    @SuppressWarnings("unchecked")
    public static <T> ListDeque<T> emptyListDeque() {
        return EMPTY_LIST_DEQUE;
    }

    @SuppressWarnings("rawtypes")
    public static boolean isEmpty( Collection collection) {
        return (collection == null || collection.isEmpty());
    }

    public static <T> List<T> nullToEmpty(List<T> list) {
        if (list == null) {
            return Collections.emptyList();
        }
        return list;
    }
}
