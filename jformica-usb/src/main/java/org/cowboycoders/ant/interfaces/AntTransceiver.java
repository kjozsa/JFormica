package org.cowboycoders.ant.interfaces;

/**
 * Copyright (c) 2012-2013, Will Szumski, David George
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

import org.cowboycoders.ant.messages.StandardMessage;
import org.cowboycoders.ant.messages.commands.ResetMessage;
import org.cowboycoders.ant.utils.UsbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.usb.UsbConst;
import javax.usb.UsbDevice;
import javax.usb.UsbDisconnectedException;
import javax.usb.UsbEndpoint;
import javax.usb.UsbException;
import javax.usb.UsbHostManager;
import javax.usb.UsbHub;
import javax.usb.UsbInterface;
import javax.usb.UsbNotActiveException;
import javax.usb.UsbNotOpenException;
import javax.usb.UsbPipe;
import javax.usb.UsbServices;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class AntTransceiver extends AbstractAntTransceiver
{
	UsbPipe inPipe = null;
	private static final Logger log = LoggerFactory.getLogger( AntTransceiver.class );
	/**
	 * sync byte
	 */
	private static byte MESSAGE_TX_SYNC = (byte) 0xA4;
	/**
	 * Usb Interface
	 */
	private UsbInterface _interface;
	/**
	 * opened
	 */
	private volatile boolean running = false;
	/**
	 * class lock
	 */
	private ReentrantLock lock = new ReentrantLock();
	/**
	 * interface claimed lock
	 */
	private ReentrantLock interfaceLock = new ReentrantLock();
	private boolean readEndpoint = true;
	private UsbEndpoint endpointIn;
	private UsbEndpoint endpointOut;
	private UsbDevice device;
	private UsbReader usbReader;

	// private int deviceNumber;

	/**
	 * Looks for all known {@link org.cowboycoders.ant.interfaces.AntDeviceId}}
	 *
	 * @param deviceNumber
	 */
	public AntTransceiver( int deviceNumber )
	{
		this( null, deviceNumber );

	}

	/**
	 * Search for a specific device
	 *
	 * @param antId
	 * @param deviceNumber
	 */
	public AntTransceiver( AntDeviceId antId, int deviceNumber )
	{
		doInit( antId, deviceNumber );
	}

	/**
	 * Testing only
	 */
	AntTransceiver()
	{
	}

	@Override
	public boolean start()
	{
		try
		{
			lock.lock();
			// already started
			if( running )
			{
				return true;
			}

			if( !device.isConfigured() )
			{
				throw new AntCommunicationException( "Ant stick not configured by OS" );
			}

			UsbInterface _interface = device.getActiveUsbConfiguration().getUsbInterface( (byte) 0 );

			this._interface = _interface;

			claimInterface( _interface, true );

			@SuppressWarnings( "unchecked" )
			List<UsbEndpoint> endpoints = _interface.getUsbEndpoints();

			if( endpoints.size() != 2 )
			{
				throw new AntCommunicationException( "Unexpected number of endpoints" );
			}

			for( UsbEndpoint endpoint : endpoints )
			{
				if( endpoint.getDirection() == UsbConst.ENDPOINT_DIRECTION_IN )
				{
					this.endpointIn = endpoint;
				}
				else
				{
					this.endpointOut = endpoint;
				}
			}

			if( this.endpointOut == null || this.endpointIn == null )
			{
				throw new AntCommunicationException( "Endpoints not found" );
			}

			inPipe = endpointIn.getUsbPipe();

			try
			{
				inPipe.open();
			}
			catch( UsbException e )
			{
				throw new AntCommunicationException( "Error opening inPipe" );
			}

			readEndpoint = true;

			this.usbReader = new UsbReader();

			this.usbReader.start();

			running = true;

		}
		catch( RuntimeException e )
		{
			e.printStackTrace();
			claimInterface( _interface, false );
			throw e;
		}
		finally
		{
			lock.unlock();
		}

		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void stop()
	{
		try
		{
			lock.lock();

			if( !running )
			{
				return;
			}

			killUsbReader();

			try
			{
				inPipe.close();
			}
			catch( UsbException e )
			{
				throw new AntCommunicationException( "Error closing inPipe", e );
			}

			_interface.release();

			running = false;
		}
		catch( UsbException e )
		{
			throw new AntCommunicationException( e );
		}
		finally
		{
			lock.unlock();
		}

	}

	@Override
	public void send( byte[] message ) throws AntCommunicationException
	{
		try
		{
			if( !running )
			{
				throw new AntCommunicationException( "AntTransceiver not running. Use start()" );
			}
			write( addExtras( message ) );

		}
		catch( UsbNotActiveException | UsbException | UsbDisconnectedException | IllegalArgumentException | UsbNotOpenException e )
		{
			throw new AntCommunicationException( e );
		}
	}

	@Override
	public boolean isRunning()
	{
		try
		{
			lock.lock();
			return running;
		}
		finally
		{
			lock.unlock();
		}
	}

	public byte getChecksum( byte[] nocheck )
	{
		byte checksum;
		checksum = MESSAGE_TX_SYNC;
		for( byte b : nocheck )
		{
			checksum ^= b % 0xff;
		}
		return checksum;
	}

	public byte[] addExtras( byte[] nocheck )
	{
		byte[] data = new byte[nocheck.length + 2];
		data[0] = MESSAGE_TX_SYNC;
		System.arraycopy( nocheck, 0, data, 1, data.length - 1 - 1 );
		data[data.length - 1] = getChecksum( nocheck );
		return data;
	}

	private void doInit( AntDeviceId antId, int deviceNumber )
	{
		UsbServices usbServices;
		UsbHub rootHub;

		try
		{
			usbServices = UsbHostManager.getUsbServices();
			rootHub = usbServices.getRootUsbHub();
		}
		catch( SecurityException | UsbException e )
		{
			throw new AntCommunicationException( e );
		}

		List<UsbDevice> devices = new ArrayList<>();
		AntDeviceId[] deviceSearchList;

		// populate an array of device ids we wish to search for
		if( antId != null )
		{ // case : specific device requested
			deviceSearchList = new AntDeviceId[]{ antId };
		}
		else
		{ // case : look for all suitable devices
			deviceSearchList = AntDeviceId.values();
		}

		for( AntDeviceId device : deviceSearchList )
		{
			List<UsbDevice> matchingDevices;
			DeviceDescriptor usbDescriptor = device.getUsbDescriptor();
			short vendorId = usbDescriptor.getVendorId();
			short deviceId = usbDescriptor.getDeviceId();
			matchingDevices = UsbUtils.getUsbDevicesWithId( rootHub, vendorId, deviceId );
			if( matchingDevices.isEmpty() )
			{
				continue;
			}
			devices.addAll( matchingDevices );
		}

		log.info( "Number of detected USB devices: " + devices.size() );

		if( devices.size() < deviceNumber + 1 )
		{
			throw new AntCommunicationException( "Device not found" );
		}

		this.device = devices.get( deviceNumber );
	}

	private void logData( int len, byte[] data, String tag )
	{
		StringBuilder logBuffer = new StringBuilder();

		for( int i = 0; i < len; i++ )
		{
			byte b = data[i];
			logBuffer.append( String.format( "%02x ", b ) );
		}

		log.trace( tag + " : " + logBuffer );
	}

	/**
	 * @param _interface interface to claim / release
	 * @param claim      true to claim, false to release
	 */
	private void claimInterface( UsbInterface _interface, boolean claim )
	{

		try
		{
			interfaceLock.lock();
			if( claim )
			{
				_interface.claim();
				log.info( "USB Interface claimed." );
			}
			else
			{
				if( _interface.isClaimed() )
				{
					_interface.release();
					log.info( "USB Interface released." );
				}
			}
		}
		catch( UsbNotActiveException | UsbDisconnectedException | UsbException e )
		{
			throw new AntCommunicationException( e );
		}
		finally
		{
			interfaceLock.unlock();
		}

	}

	private void killUsbReader()
	{
		readEndpoint = false;

		// Doesn't seem to work so we send a message instead
		// inPipe.abortAllSubmissions();

		StandardMessage msg = new ResetMessage();

		send( msg.encode() );

		try
		{
			usbReader.join();
		}
		catch( InterruptedException e )
		{
			log.warn( "interrupted waiting to shutdown device" );
		}
	}

	private void write( byte[] data ) throws
	                                  UsbNotActiveException,
	                                  UsbNotOpenException,
	                                  IllegalArgumentException,
	                                  UsbDisconnectedException,
	                                  UsbException
	{
		UsbPipe pipe = null;

		try
		{
			lock.lock();
			pipe = endpointOut.getUsbPipe();
			if( !pipe.isOpen() )
			{
				pipe.open();
			}
			pipe.syncSubmit( data );
			logData( data.length, data, "wrote" );
		}
		finally
		{
			if( pipe != null )
			{
				pipe.close();
			}
			lock.unlock();
		}

	}

	public class UsbReader extends Thread
	{
		private static final int BUFFER_SIZE = 64;

		@Override
		public void run()
		{
			AntMessageParser antMessageParser = new AntMessageParser();

			try
			{
				while( readEndpoint )
				{
					byte[] data = new byte[BUFFER_SIZE];
					int bytesReceived;

					try
					{
						bytesReceived = inPipe.syncSubmit( data );
						if( bytesReceived == 0 )
						{
							continue;
						}
					}
					catch( UsbException e )
					{
						// Timeouts are expected in some implementations - these manifest
						// themselves as UsbExceptions. We should continue, but log the error
						// in case it indicates something more serious.
						log.warn( e.getMessage() );
						continue;
					}

					logData( bytesReceived, data, "read" );
					List<byte[]> messages = antMessageParser.parse( data, bytesReceived );

					for( byte[] bytes : messages )
					{
						AntTransceiver.this.broadcastRxMessage( bytes );
					}
				}

			}
			catch( UsbNotActiveException | UsbDisconnectedException | IllegalArgumentException | UsbNotOpenException e )
			{
				log.error( "Error encountered while reading data", e );
			}

			log.trace( this.getClass().toString() + " killed" );
		}
	}
}
