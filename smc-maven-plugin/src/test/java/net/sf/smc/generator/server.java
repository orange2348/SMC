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
//  TcpConnection.java
//
// Description
//  Encapsulates "TCP" server connection, accepting new client
//  connections.
//

package net.sf.smc.generator;

import java.util.LinkedList;
import java.util.List;

public final class server
    implements TcpConnectionListener,
               IStoppable
{
//---------------------------------------------------------------
// Member data
//

    //-----------------------------------------------------------
    // Constants.
    //

    public static final long MAX_SLEEP = 0x7fffffff;
    public static final int NUM_ARGS = 1;

    //-----------------------------------------------------------
    // Locals.
    //

    private volatile boolean mIsRunning;
    private volatile boolean mOpened;
    private Thread mMyThread;
    private String mReason;

    // Keep list of accepted connections.
    private final List<client> mClientList;

//---------------------------------------------------------------
// Member methods.
//

    //-----------------------------------------------------------
    // Main Method.
    //

    public static void main(String[] args)
    {
        final int port;
        int exitCode = 0;

        if (args.length != NUM_ARGS)
        {
            System.err.println("server: Incorrect number of arguments.");
            System.err.println("usage: server port");

            exitCode = 1;
        }
        else if ((port = parseInt(args[0], "port", 0)) >= 0)
        {
            final server server = new server();

            server.start(port);
        }

        System.exit(exitCode);
    } // end of main(String[])

    //
    // end of Main Method.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // Constructors.
    //

    public server()
    {
        mIsRunning = false;
        mOpened = false;
        mMyThread = null;
        mReason = null;
        mClientList = new LinkedList<>();
    } // end of server()

    //
    // end of Constructors.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // TcpConnectionListener Interface Impelementation.
    //

    @Override
    public void opened(final TcpConnection server)
    {
        synchronized (this)
        {
            mOpened = true;

            this.notify();
        }
    } // end of opend(TcpConnection server)

    @Override
    public void openFailed(final String reason,
                           final TcpConnection server)
    {
        synchronized (this)
        {
            mIsRunning = false;
            mOpened = false;
            mReason = reason;

            this.notify();
        }
    } // end of opendFailed(String, TcpConnection)

    @Override
    public void halfClosed(final TcpConnection client)
    {}

    @Override
    public void closed(final String reason,
                       final TcpConnection server)
    {
        System.out.println("closed.");
    } // end of closed(String, TcpConnection)

    @Override
    public void accepted(final TcpClient client,
                         final TcpServer server)
    {
        final client newClient = new client(this);

        System.out.format(
            "Accepted new connection from %s.%n",
            client.getAddress());

        synchronized (mClientList)
        {
            mClientList.add(newClient);
        }

        newClient.setConnection(client);
        newClient.start();
    } // end of accepted(TcpClient, TcpServer)

    @Override
    public void transmitted(final TcpConnection client)
    {}

    @Override
    public void transmitFailed(final String reason,
                               final TcpConnection client)
    {}

    @Override
    public void receive(final byte[] data,
                        final TcpConnection client)
    {}

    //
    // end of TcpConnectionListener Interface Impelementation.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // IStoppable Interface Implementation.
    //

    // Stop the app.
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

    public void clientClosed(final client tcpClient)
    {
        // Remove client from list and then let the world know
        // about the update.
        synchronized (mClientList)
        {
            mClientList.remove(tcpClient);

            mClientList.notify();
        }
    } // end of clientClosed(client)

    public void start(final int port)
    {
        final TcpServer serverSocket = new TcpServer(this);

        // Remember this thread for latter.
        mMyThread = Thread.currentThread();

        // Open the server connection.
        System.out.format(
            "Opening server on port %d ... ", port);

        mOpened = false;
        serverSocket.start();
        serverSocket.open(port);

        // Wait for open to complete - successfully or not.
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
            System.err.format("open failed%s.%n",
                              (mReason == null ?
                               "" :
                               mReason));
        }
        else
        {
            final StopThread<server> stopThread =
                new StopThread<>(this);

            System.out.println("open successful.");
            System.out.println("Listening for new connections.");

            // Create a thread to watch for a keystroke.
            stopThread.setDaemon(true);
            stopThread.start();

            run(serverSocket);
        }
    } // end of start(int)

    private void run(final TcpServer serverSocket)
    {
        // Wait here for application to be terminated.
        synchronized (this)
        {
            while (mIsRunning)
            {
                try
                {
                    this.wait();
                }
                catch (InterruptedException interrupt)
                {}
            }
        }

        // Now that we are no longer running, close the
        // service.
        System.out.print("Closing connection ... ");
        serverSocket.close();
        System.out.println("closed.");

        closeClients();
    } // end of run(TcpServer)

    private void closeClients()
    {
        System.out.println("Closing accepted clients ... ");

        // Wait for all accepted clients to stop running before
        // returning.
        synchronized (mClientList)
        {
            // Stop all remaining accepted clients.
            for (client client: mClientList)
            {
                client.halt();
            }

            // Wait for the clients to close.
            while (!mClientList.isEmpty())
            {
                try
                {
                    mClientList.wait();
                }
                catch (InterruptedException interrupt)
                {}
            }
        }

        System.out.println("done.");
    } // end of closeClients()

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
} // end of class server
