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

package org.cowboycoders.ant.messages.notifications;

import org.cowboycoders.ant.messages.MessageException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SerialErrorMessageTest {

    @Test
    public void test() throws MessageException {
        SerialErrorMessage msg = new SerialErrorMessage();
        msg.decode(new byte[]{1, (byte) 0xAE, (byte) 255, 1, 2, 3});

        int count = 0;
        // should be 1,2,3 ...
        for (Byte b : msg.getOriginalMessage()) {
            assertEquals(b.byteValue(), ((byte) ++count));
        }

    }

}
