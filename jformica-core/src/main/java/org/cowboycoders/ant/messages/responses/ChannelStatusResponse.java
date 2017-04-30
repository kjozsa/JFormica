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
package org.cowboycoders.ant.messages.responses;

import org.cowboycoders.ant.messages.ChannelMessage;
import org.cowboycoders.ant.messages.ChannelType;
import org.cowboycoders.ant.messages.Constants.DataElement;
import org.cowboycoders.ant.messages.MasterChannelType;
import org.cowboycoders.ant.messages.MessageException;
import org.cowboycoders.ant.messages.MessageId;
import org.cowboycoders.ant.messages.SlaveChannelType;

import java.util.HashMap;
import java.util.Map;

/**
 * provides channel type / network number / channel state
 * @author will
 *
 */
public class ChannelStatusResponse extends ChannelMessage
{

	private static final int STATE_MASK = (1 << 0) + (1 << 1);
	private static final int NETWORK_NUMBER_MASK = (1 << 2) + (1 << 3);
	private static final int CHANNEL_TYPE_MASK = ((1 << 4) + (1 << 5) + (1 << 6) + (1 << 7));
	/**
	 * The additional elements we are adding to channel message
	 */
	private static DataElement[] additionalElements = new DataElement[]{
			DataElement.CHANNEL_STATUS,
			};
	public enum State
	{
		UNASSIGNED, ASSIGNED, SEARCHING, TRACKING,;

		private static Map<Byte, State> ordinalMap = new HashMap<>();

		static
		{
			for( State s : State.values() )
			{
				ordinalMap.put( (byte) s.ordinal(), s );
			}
		}

		/**
		 *
		 * @param code to lookup (cast to a byte)
		 * @return State object associated with ordinal, or null if not known
		 */
		public static State lookUp( int code )
		{
			return ordinalMap.get( (byte) code );
		}

	}

	public static void main( String[] args )
	{
		ChannelStatusResponse res = new ChannelStatusResponse();
		res.setStatusByte( 2 );
		System.out.println( res.getState() );
		System.out.println( State.lookUp( 2 ) );
	}

	public ChannelStatusResponse( Integer channelNo )
	{
		super( MessageId.CHANNEL_STATUS, channelNo, additionalElements );
	}

	public ChannelStatusResponse()
	{
		this( 0 );
	}

	/* (non-Javadoc)
	 * @see org.cowboycoders.ant.messages.ChannelMessage#validate()
	 */
	@Override
	public void validate() throws MessageException
	{
		super.validate();
		if( getStandardPayload().size() < 2 )
		{
			throw new MessageException( "insufficent data" );
		}
	}

	/**
	 * @return State of Channel
	 */
	public State getState()
	{
		int stateCode = getDataElement( DataElement.CHANNEL_STATUS ) & STATE_MASK;
		return State.lookUp( stateCode );
	}

	/**
	 * @return network number channel is set to
	 */
	public int getNetworkNumber()
	{
		return getDataElement( DataElement.CHANNEL_STATUS ) & NETWORK_NUMBER_MASK;
	}

	/**
	 * @return channel type of requested channel
	 */
	public ChannelType getChannelType()
	{
		int channelType = getDataElement( DataElement.CHANNEL_STATUS ) & CHANNEL_TYPE_MASK;
		boolean shared = false;
		boolean oneway = false;
		if( (channelType & ChannelType.Types.ONEWAY_RECEIVE.code) != 0 || (channelType & ChannelType.Types.ONEWAY_TRANSMIT.code) != 0 )
		{
			oneway = true;
		}
		if( (channelType & ChannelType.Types.SHARED_RECEIVE.code) != 0 || (channelType & ChannelType.Types.SHARED_TRANSMIT.code) != 0 )
		{
			shared = true;
		}
		if( (channelType & ChannelType.Types.MASTER.code) != 0 )
		{
			return new MasterChannelType( shared, oneway );
		}

		return new SlaveChannelType( shared, oneway );
	}

	private void setStatusByte( int value )
	{
		setDataElement( DataElement.CHANNEL_STATUS, value );
	}

}
