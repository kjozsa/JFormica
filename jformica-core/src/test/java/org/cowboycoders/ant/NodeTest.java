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

import net.vidageek.mirror.dsl.Mirror;
import org.cowboycoders.ant.interfaces.AbstractAntTransceiver;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.*;

public class NodeTest {

    Node node;
    Channel channel1;
    Network[] networks;
    NetworkKey networkKey1 = new NetworkKey(0, 0, 0, 0, 0, 0, 0, 0);
    NetworkKey networkKey2 = new NetworkKey(1, 0, 0, 0, 0, 0, 0, 0);
    private static int NUMBER_OF_NETWORKS = 2;

    @Before
    public void setup() {
        node = new Node(mock(AbstractAntTransceiver.class));
        channel1 = mock(Channel.class);
        networks = new Network[NUMBER_OF_NETWORKS];
        networks[0] = new Network(0, networkKey1, null);
        networks[1] = new Network(1, networkKey2, null);

        new Mirror().on(node).set().field("channels").withValue(new Channel[]{channel1});
        new Mirror().on(node).set().field("networks").withValue(networks);
    }

    @Test
    public void shouldSetChannelFreeFlagToFalse() {
        when(channel1.isFree()).thenReturn(true);
        Channel channel = node.getFreeChannel();

        assertSame(channel1, channel);
        verify(channel1).setFree(false);
    }

    @Test
    public void shouldNotGetNonFreeChannels() {
        when(channel1.isFree()).thenReturn(false);

        assertNull(node.getFreeChannel());
    }

    @Test
    public void shouldReturnFreeChannelToPool() {
        node.freeChannel(channel1);

        verify(channel1).setFree(true);
    }

    @Test
    public void shouldReturnDifferentNetworksForDifferentKeys() {
        Network n1 = node.getNetworkForKey(networkKey1);
        Network n2 = node.getNetworkForKey(networkKey2);
        assertNotSame(n1.getNumber(), n2.getNumber());
    }

    @Test
    public void shouldReturnSameNetworkForSameKey() {
        Network n1 = node.getNetworkForKey(networkKey1);
        Network n2 = node.getNetworkForKey(networkKey1);
        assertSame(n1.getNumber(), n2.getNumber());
    }

    @Test(expected = NetworkAllocationException.class)
    public void shouldLimitNumberOfNetworks() {
        for (int i = 0; i <= NUMBER_OF_NETWORKS; i++) {
            node.getNetworkForKey(new NetworkKey(i, 0, 0, 0, 0, 0, 0, 0));
        }
    }
}
