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
import org.junit.*;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class NetworkHandleTest
{

	@Test( expected = IllegalStateException.class )
	public void checkFreeCanOnlyBeCalledOnce()
	{
		NetworkKey key = new NetworkKey( 1, 2, 3, 4, 5, 6, 7, 8 );
		Network n = new Network( 0, key, null );
		NetworkHandle h = new NetworkHandle( n );
		assertEquals( key, h.getNetworkKey() );
		h.free();
		h.getNetworkKey();
	}

	@Test
	public void checkRefCountIncremented()
	{
		NetworkKey key = new NetworkKey( 1, 2, 3, 4, 5, 6, 7, 8 );
		Network n = new Network( 0, key, null );
		ArrayList<NetworkHandle> handles = new ArrayList<>();
		for( int i = 0; i < 10; i++ )
		{
			NetworkHandle h = new NetworkHandle( n );
			handles.add( h );
		}
		AtomicInteger count = (AtomicInteger) new Mirror().on( n ).get().field( "refCount" );
		assertEquals( 10, count.get() );
		for( NetworkHandle h : handles )
		{
			h.free();
		}
		assertEquals( 0, count.get() );
	}

}
