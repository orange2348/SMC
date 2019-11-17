//
// The contents of this file are subject to the Mozilla Public
// License Version 1.1 (the "License"); you may not use this file
// except in compliance with the License. You may obtain a copy
// of the License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an
// "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
// implied. See the License for the specific language governing
// rights and limitations under the License.
//
// The Original Code is State Machine Compiler (SMC).
//
// The Initial Developer of the Original Code is Charles W. Rapp.
// Portions created by Charles W. Rapp are
// Copyright (C) 2000 - 2007, 2019. Charles W. Rapp.
// All Rights Reserved.
//
// Contributor(s):
//
// Name
//  client.java
//
// Description
//  Encapsulates "TCP" client connection.
//

package net.sf.smc.generator;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;

public final class client
    extends Thread
    implements TcpConnectionListener,
               IStoppable
{
//---------------------------------------------------------------
// Member data.
//

    //-----------------------------------------------------------
    // Constants.
    //

    private static final long MIN_SLEEP_TIME = 100;
    private static final long MAX_SLEEP_TIME = 1_000; // 1 second.
    private static final int HOST = 0;
    private static final int PORT = 1;
    private static final int XMIT_COUNT = 2;
    private static final int NUM_ARGS = 3;

    //-----------------------------------------------------------
    // Statics.
    //

    // Use the following to randomly decide when to issue an
    // alarm and what type, etc.
    private static final Random sRandomizer =
        new Random(System.currentTimeMillis());

    //-----------------------------------------------------------
    // Locals.
    //

    private final server mOwner;
    private final int mXmitCount;
    private TcpClient mClientSocket;
    private volatile boolean mIsRunning;
    private volatile boolean mOpened;
    private String mErrorMessage;

//---------------------------------------------------------------
// Member methods.
//

    //-----------------------------------------------------------
    // Main Method.
    //

    public static void main(final String[] args)
    {
        final InetAddress address;
        final int port;
        final int xmitCount;
        int exitCode = 0;

        if (args.length != NUM_ARGS)
        {
            System.err.println("client: Incorrect number of arguments.");
            System.err.println("usage: client host port #transmits");

            exitCode = 1;
        }
        else if (
            (address = parseAddress(args[HOST])) != null &&
            (port = parseInt(args[PORT], "port", 0)) >= 0 &&
            (xmitCount = parseInt(args[XMIT_COUNT], "transmit count", 1)) >= 1)
        {
            // Now try to connect to the server and start sending
            // data.
            client client = new client(xmitCount);

            // Create client and start running.
            client.start(address, port);
        }

        System.exit(exitCode);
    } // end of main(String[])

    //
    // end of Main Method.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // Constructors.
    //

    /* package */ client(final int xmitCount)
    {
        mClientSocket = null;
        mXmitCount = xmitCount;
        mOpened = false;
        mIsRunning = false;
        mOwner = null;
        mErrorMessage = null;
    } // end of client(int)

    /* package */ client(final server owner)
    {
        mXmitCount = Integer.MAX_VALUE;
        mOpened = false;
        mIsRunning = false;
        mOwner = owner;
        mErrorMessage = null;
    } // end of client(TcpClient, server)

    //
    // end of Constructors.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // Thread Method Overrides.
    //

    @Override
    public void run()
    {
        int messageCount = 1;

        mIsRunning = true;

        // Now sit here waiting to send and receive.
        while (mIsRunning && messageCount <= mXmitCount)
        {
            // Sleep then send the next message.
            sendSleep();

            // Make sure we are still running before sending
            // the next message.
            if (mIsRunning)
            {
                sendMessage(messageCount++);
            }
        }

        // Now that we are no longer running, close the
        // connection.
        closeConnection();
    } // end of run()

    //
    // end of Thread Method Overrides.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // TcpConnectionListener Interface Impelementation.
    //

    @Override
    public void opened(final TcpConnection client)
    {
        synchronized (this)
        {
            mOpened = true;

            this.notify();
        }
    } // end of opened(TcpConnection)

    @Override
    public void openFailed(final String reason,
                           final TcpConnection client)
    {
        synchronized (this)
        {
            mIsRunning = false;
            mOpened = false;
            mErrorMessage = reason;

            this.notify();
        }
    } // end of openFailed(String, TcpConnection)

    @Override
    public void halfClosed(final TcpConnection client)
    {
        System.out.format(
            "Connection from %s closed its side.",
            mClientSocket.getAddress());

        // The far end has closed its connection. Stop running
        // since it is no longer listening.
        synchronized (this)
        {
            mIsRunning = false;

            this.notify();
        }
    } // end of halfClosed(TcpConnection)

    @Override
    public void closed(final String reason,
                       final TcpConnection client)
    {
        synchronized (this)
        {
            mOpened = false;
            mIsRunning = false;

            this.notify();
        }
    } // end of closed(String, TcpConnection)

    @Override
    public void transmitted(final TcpConnection client)
    {
        System.out.println("transmit successful.");
    } // end of transmitted(TcpConnection)

    @Override
    public void transmitFailed(final String reason,
                               final TcpConnection client)
    {
        System.out.format("transmit failed - %s.%n", reason);
    } // end of transmitFailed(String, TcpConnection)

    @Override
    public void receive(final byte[] data,
                        final TcpConnection client)
    {
        System.out.format("Received data from %s: \"%s\"%n",
                          client.getAddress(),
                          new String(data));
    } // end of receive(byte[], TcpConnection)

    @Override
    public void accepted(final TcpClient client,
                         final TcpServer server)
    {}

    //
    // end of TcpConnectionListener Interface Impelementation.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // IStoppable Interface Implementation.
    //

    // Stop the client.
    @Override
    public void halt()
    {
        synchronized (this)
        {
            mIsRunning = false;

            this.notify();
        }
    } // end of halt()

    //
    // end of IStoppable Interface Implementation.
    //-----------------------------------------------------------

    public void start(final InetAddress address,
                      final int port)
    {
        // If there is no client connection, create one and open
        // it.
        mClientSocket = new TcpClient(this);

        // Open the client connection.
        System.out.format(
            "Opening connection to %s:%d ... ",
            address.getHostAddress(),
            port);

        mOpened = false;
        mClientSocket.start();
        mClientSocket.open(address, port);

        // Wait for open to complete - either successfully or
        // unsuccessfully.
        synchronized (this)
        {
            mIsRunning = true;

            while (mIsRunning && !mOpened)
            {
                try
                {
                    this.wait();
                }
                catch (InterruptedException interrupt)
                {}
            }
        }

        if (!mOpened)
        {
            System.out.format("open failed%s.%n",
                              (mErrorMessage == null ?
                               "" :
                               mErrorMessage));
            mErrorMessage = null;
        }
        else
        {
            final StopThread<client> stopThread =
                new StopThread<>(this);

            System.out.println("open successful.");

            // Create a thread to watch for a keystroke.
            stopThread.setDaemon(true);
            stopThread.start();

            start();
        }
    } // end of start(InetAddress, int)

    public boolean isRunning()
    {
        return (mIsRunning);
    } // end of isRunning()

    public boolean isOpen()
    {
        return (mOpened);
    } // end of isOpen()

    /* package */ void setConnection(final TcpClient connection)
    {
        connection.setListener(this);
        mClientSocket = connection;
    } // end of setConnection(TcpClient)

    private void sendSleep()
    {
        final long sleeptime = calculateSleepTime();

        synchronized (this)
        {
            if (mIsRunning)
            {
                try
                {
                    this.wait(sleeptime);
                }
                catch (InterruptedException interrupt)
                {}
            }
        }
    } // end of sendSleep();

    private long calculateSleepTime()
    {
        final long retval =
            (sRandomizer.nextLong() % MAX_SLEEP_TIME);

        // Sleep time is in milliseconds but no less than 100.
        return (retval < MIN_SLEEP_TIME ?
                MIN_SLEEP_TIME :
                retval);
    } // end of calculateSleepTime()

    private void sendMessage(final int messageCount)
    {
        final String message =
            String.format(
                "This is message #%,d.", messageCount);
        final byte[] data = message.getBytes();

        try
        {
            System.out.format(
                "Transmitting to %s: \"%s\" ... ",
                mClientSocket.getAddress(),
                message);

            // Now send a message.
            mClientSocket.transmit(data, 0, data.length);
        }
        catch (Exception jex)
        {
            jex.printStackTrace(System.err);

            mIsRunning = false;
        }
        //
    } // end of sendMessage()

    private void closeConnection()
    {
        System.out.format(
            "Closing connection to %s ... ",
            mClientSocket.getAddress());

        synchronized (mClientSocket)
        {
            mClientSocket.close();

            while (mClientSocket.isOpen())
            {
                try
                {
                    mClientSocket.wait();
                }
                catch (InterruptedException interrupt)
                {}
            }
        }

        System.out.println("closed.");

        synchronized (this)
        {
            mIsRunning = false;
            mOpened = false;

            this.notifyAll();
        }

        // If this client connection was accepted by a service,
        // then inform the service that this client is closed.
        if (mOwner != null)
        {
            mOwner.clientClosed(this);
        }
    } // end fo closeConnection()

    private static InetAddress parseAddress(final String s)
    {
        InetAddress retval = null;

        try
        {
            retval = InetAddress.getByName(s);
        }
        catch (UnknownHostException jex)
        {
            System.err.format("\"%s\" is an unknown host.%n", s);
        }

        return (retval);
    } // end of parseAddress(String)

    private static int parseInt(final String s,
                                final String name,
                                final int minValue)
    {
        int retval = -1;

        try
        {
            retval = Integer.parseInt(s);

            if (retval < minValue)
            {
                System.err.format("%d < %d.%n", retval, minValue);
            }
        }
        catch (NumberFormatException jex)
        {
            System.err.format(
                "\"%s\" is not a valid %s.%n", s, name);
        }

        return (retval);
    } // end of parseInt(String, String, int)
} // end of class client
