/**
 * Copyright (c) 2013, Will Szumski
 * <p>
 * This file is part of formicidae.
 * <p>
 * formicidae is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * formicidae is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with formicidae.  If not, see <http://www.gnu.org/licenses/>.
 */
/**
 *
 */
package org.cowboycoders.ant.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Stores ant messages with thread safe access
 *
 * @author will
 *
 */
public class BroadcastMessenger<V> {
    private static final Logger log = LoggerFactory.getLogger(BroadcastMessenger.class);
    /**
     * Used to concurrently notify listeners
     */
    private ExecutorService dispatchPool;
    /**
     * Contains all classes listening for new messages
     */
    private Set<BroadcastListener<V>> listeners = new HashSet<>();
    /**
     * Used to lock {@code listeners}
     */
    private ReentrantReadWriteLock listenerLock = new ReentrantReadWriteLock();
    private static final ExecutorService SHARED_SINGLE_THREAD_EXECUTOR = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = Executors.defaultThreadFactory().newThread(runnable);
            thread.setName("BroadcastMessengerThread");
            thread.setDaemon(true);
            return thread;
        }
    });

    /**
     * Backed by an unbounded TODO : fix this see java.util.concurrent.ThreadPoolExecutor
     */
    public BroadcastMessenger() {
        dispatchPool = SHARED_SINGLE_THREAD_EXECUTOR;
    }

    /**
     * Use a custom TODO : fix this code java.util.concurrent.ThreadPoolExecutor
     *
     * TODO : fix this see java.util.concurrent.ThreadPoolExecutor for explantion of
     * parameters.
     *
     * @param coreSize TODO: document this
     * @param maxSize TODO: document this
     * @param timeout TODO: document this
     * @param timeoutUnit TODO: document this
     * @param backingQueue TODO: document this
     */
    public BroadcastMessenger(int coreSize, int maxSize, int timeout, TimeUnit timeoutUnit, BlockingQueue<Runnable> backingQueue) {
        dispatchPool = new ThreadPoolExecutor(coreSize, maxSize, timeout, timeoutUnit, backingQueue);
    }

    /**
     * Adds a listener
     *
     * @param listener TODO: document this
     */
    public void addBroadcastListener(BroadcastListener<V> listener) {
        try {
            listenerLock.writeLock().lock();
            listeners.add(listener);
        } finally {
            listenerLock.writeLock().unlock();
        }
    }

    /**
     * removes a listener
     *
     * @param listener TODO: document this
     */
    public void removeBroadcastListener(BroadcastListener<V> listener) {
        try {
            listenerLock.writeLock().lock();
            listeners.remove(listener);
        } finally {
            listenerLock.writeLock().unlock();
        }
    }

    /**
     * Returns current number of listeners
     * @return number of listeners
     */
    public int getListenerCount() {
        try {
            listenerLock.readLock().lock();
            return listeners.size();
        } finally {
            listenerLock.readLock().unlock();
        }
    }

    /**
     * sends all listeners the message
     *
     * @param message TODO: document this
     */
    public void sendMessage(final V message) {
        try {
            listenerLock.readLock().lock();
            for (final BroadcastListener<V> listener : listeners) {
                ;
                dispatchPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            listener.receiveMessage(message);
                        } catch (Exception e) {
                            log.error("Error from listener: {}", e.getMessage());
                        }
                    }
                });
            }
        } finally {
            listenerLock.readLock().unlock();
        }

    }

}
