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
package org.cowboycoders.ant.events;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Used to return a lock to a calling thread. Should check returnLock for null,
 * if null : lockAvailable.await(),
 * else: return the the returnLock using  a Future
 *
 * @author will
 */
public class LockExchangeContainer {

    /**
     * locks this class
     */
    Lock lock = new ReentrantLock();

    /**
     * signalled when a lock has been added to this object
     */
    Condition lockAvailable = lock.newCondition();

    /**
     * the lock that is exchanged
     */
    Lock returnLock;

}
