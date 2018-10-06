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
package org.cowboycoders.ant.messages.responses.exceptions;

import org.cowboycoders.ant.ChannelError;

/**
 * Thrown when trying to open a channel without first setting the id.
 *
 * @author will
 */
public class ChannelIdNotSetException extends ChannelError {

    /**
     *
     */
    private static final long serialVersionUID = -767937266435117460L;

    public ChannelIdNotSetException() {
        super();
    }

    public ChannelIdNotSetException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public ChannelIdNotSetException(String detailMessage) {
        super(detailMessage);
    }

    public ChannelIdNotSetException(Throwable throwable) {
        super(throwable);
    }

}
