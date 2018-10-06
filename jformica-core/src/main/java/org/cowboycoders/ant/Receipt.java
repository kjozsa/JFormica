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
package org.cowboycoders.ant;

import org.cowboycoders.ant.messages.MessageMetaWrapper;
import org.cowboycoders.ant.messages.StandardMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

public class Receipt {

    private static final Logger log = LoggerFactory.getLogger(Receipt.class);

    private LinkedList<MessageMetaWrapper<? extends StandardMessage>> sentMessages = new LinkedList<>();
    private LinkedList<MessageMetaWrapper<? extends StandardMessage>> receivedMessages = new LinkedList<>();

    public void addReceived(MessageMetaWrapper<? extends StandardMessage> receivedMeta) {
        if (receivedMeta == null) {
            log.warn("passed null message, ignoring");
            return;
        }
        receivedMessages.add(receivedMeta);
    }

    public void addSent(MessageMetaWrapper<? extends StandardMessage> msg) {
        if (msg == null) {
            log.warn("passed null message, ignoring");
            return;
        }
        sentMessages.add(msg);
    }

    public void addSent(List<MessageMetaWrapper<? extends StandardMessage>> msgs) {
        if (msgs == null) {
            log.warn("passed null list, ignoring");
            return;
        }
        for (MessageMetaWrapper<? extends StandardMessage> msg : msgs) {
            addSent(msg);
        }
    }

    public MessageMetaWrapper<? extends StandardMessage> getLastSent() {
        return sentMessages.getLast();
    }

    public MessageMetaWrapper<? extends StandardMessage> getLastReceived() {
        return receivedMessages.getLast();
    }

    protected LinkedList<MessageMetaWrapper<? extends StandardMessage>> getSentMessages() {
        return sentMessages;
    }

    protected LinkedList<MessageMetaWrapper<? extends StandardMessage>> getReceivedMessages() {
        return receivedMessages;
    }

}
  
  