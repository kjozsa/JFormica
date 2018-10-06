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
package org.cowboycoders.ant.messages.data;

import org.cowboycoders.ant.messages.Message;
import org.cowboycoders.ant.messages.MessageId;

/**
 * Standard acknowledged broadcast message
 * @author will
 *
 */
public class AcknowledgedDataMessage extends DataMessage {

    public AcknowledgedDataMessage() {
        this(0);
    }

    public AcknowledgedDataMessage(Integer channelNo) {
        this(new Message(), MessageId.ACKNOWLEDGED_DATA, channelNo);
    }

    protected AcknowledgedDataMessage(Message message, MessageId id, Integer channelNo) {
        super(message, id, channelNo);
    }

}
