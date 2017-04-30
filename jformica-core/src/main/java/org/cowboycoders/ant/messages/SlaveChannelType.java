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
package org.cowboycoders.ant.messages;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a slave channel configuration
 *
 * @author will
 *
 */
public class SlaveChannelType extends ChannelType
{

	/**
	 * Slave configuration with all options
	 * @param shared if shared channel
	 * @param rxOnly if receive only
	 */
	public SlaveChannelType( boolean shared, boolean rxOnly )
	{
		super( getChannelTypesArray( shared, rxOnly ) );
	}

	/**
	 * Default slave configuration : two-way, non-shared
	 */
	public SlaveChannelType()
	{
		super( getChannelTypesArray( false, false ) );
	}

	private static ChannelType.Types[] getChannelTypesArray( boolean shared, boolean rxOnly )
	{
		List<ChannelType.Types> types = new ArrayList<>();
		types.add( ChannelType.Types.SLAVE );
		if( shared )
		{
			types.add( ChannelType.Types.SHARED_RECEIVE );
		}
		if( rxOnly )
		{
			types.add( ChannelType.Types.ONEWAY_RECEIVE );
		}
		return types.toArray( new ChannelType.Types[0] );
	}

}