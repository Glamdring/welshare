package com.welshare.service.jobs;

import java.util.Comparator;
import java.util.PriorityQueue;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.welshare.model.ScheduledMessage;

/**
 * Need some instantiation logic for the queue bean, hence the configuration object
 *
 * @author bozho
 *
 */
@Configuration
public class PriorityQueueBean {

    @Bean(name="scheduledMessagePriorityQueue")
    public PriorityQueue<ScheduledMessage> createScheduledMessagePriorityQueue() {
        return new PriorityQueue<ScheduledMessage>(20, new ScheduledMessageComparator());
    }

    private static class ScheduledMessageComparator implements Comparator<ScheduledMessage> {
        @Override
        public int compare(ScheduledMessage m1, ScheduledMessage m2) {
            return m1.getScheduledTime().compareTo(m2.getScheduledTime());
        }
    }
}
