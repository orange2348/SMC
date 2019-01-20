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
// Copyright (C) 2000 - 2007. Charles W. Rapp.
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
// RCS ID
// $Id$
//
// CHANGE LOG
// $Log$
// Revision 1.7  2009/03/01 18:20:39  cwrapp
// Preliminary v. 6.0.0 commit.
//
// Revision 1.6  2007/12/28 12:34:40  cwrapp
// Version 5.0.1 check-in.
//
// Revision 1.5  2005/11/07 19:34:54  cwrapp
// Changes in release 4.3.0:
// New features:
//
// + Added -reflect option for Java, C#, VB.Net and Tcl code
//   generation. When used, allows applications to query a state
//   about its supported transitions. Returns a list of transition
//   names. This feature is useful to GUI developers who want to
//   enable/disable features based on the current state. See
//   Programmer's Manual section 11: On Reflection for more
//   information.
//
// + Updated LICENSE.txt with a missing final paragraph which allows
//   MPL 1.1 covered code to work with the GNU GPL.
//
// + Added a Maven plug-in and an ant task to a new tools directory.
//   Added Eiten Suez's SMC tutorial (in PDF) to a new docs
//   directory.
//
// Fixed the following bugs:
//
// + (GraphViz) DOT file generation did not properly escape
//   double quotes appearing in transition guards. This has been
//   corrected.
//
// + A note: the SMC FAQ incorrectly stated that C/C++ generated
//   code is thread safe. This is wrong. C/C++ generated is
//   certainly *not* thread safe. Multi-threaded C/C++ applications
//   are required to synchronize access to the FSM to allow for
//   correct performance.
//
// + (Java) The generated getState() method is now public.
//
// Revision 1.4  2005/05/28 13:51:24  cwrapp
// Update Java examples 1 - 7.
//
// Revision 1.0  2003/12/14 20:21:46  charlesr
// Initial revision
//

package smc_ex6;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;

/**
 * This class provides the TCP client {@code main} method.
 * Handles {@link TcpConnection} callbacks.
 *
 * @see TcpConnection
 *
 * @author <a href="mailto:rapp@acm.org">Charles Rapp</a>
 */

public final class client
    extends Thread
    implements TcpConnectionListener
{

    /**
     * TCP client main application method.
     * @param args command line arguments containing the host
     * name/dotted IP address and TCP port.
     */
    public static void main(String[] args)
    {
        int port;
        String host;

        if (args.length != 2)
        {
            System.err.println("client: Incorrect number of arguments.");
            System.err.println("usage: client host port");
            System.exit(1);
        }
        else
        {
            host = args[0];

            // Now try to connect to the server and start sending
            // data.
            try
            {
                InetAddress address;
                client client;

                address = InetAddress.getByName(host);
                port = Integer.parseInt(args[1]);

                client = new client();

                // Create client and start running.
                System.out.println("(Starting execution. Hit Enter to stop.)");
                client.run(address, port);
                System.out.println("(Stopping execution.)");

                System.exit(0);
            }
            catch (UnknownHostException ex)
            {
                ex.printStackTrace(System.err);
                System.exit(5);
            }
            catch (NumberFormatException ex)
            {
                System.err.println("Invalid port number - \"" +
                                   args[1] +
                                   "\".");
                System.exit(2);
            }
        }
    }

    /**
     * Creates a new TCP client.
     */
    public client()
    {
        _client_socket = null;
        _my_thread = null;
        _opened = false;
        _isRunning = false;
        _randomizer = new Random(System.currentTimeMillis());
        _owner = null;
        _errorMessage = null;

        return;
    }

    /**
     * Creates a client connection for an accepted connection.
     * @param client_socket accepted client connection.
     * @param owner TCP server accepting the client.
     */
    public client(TcpClient client_socket, server owner)
    {
        _client_socket = client_socket;
        _my_thread = null;
        _opened = false;
        _isRunning = false;
        _randomizer = new Random(System.currentTimeMillis());
        _owner = owner;
        _errorMessage = null;

        _client_socket.setListener(this);

        return;
    }

    /**
     * Opens the TCP client connection to the given host and port
     * and then starts the application thread running when open.
     * @param address destination host.
     * @param port destination port.
     */
    public void run(InetAddress address, int port)
    {
        String port_string = Integer.toString(port);

        _my_thread = Thread.currentThread();

        // If there is no client connection, create one and open
        // it.
        _client_socket = new TcpClient(this);

        // Open the client connection.
        System.out.print("Opening connection to " +
                         address.getHostName() +
                         ":" +
                         port_string +
                         " ... ");
        _opened = false;
        _client_socket.start();
        _client_socket.open(address, port);

        // Wait for open to complete.
        try
        {
            _isRunning = true;
            while (_isRunning == true)
            {
                Thread.sleep(1000);
            }
        }
        catch (InterruptedException interrupt) {}

        if (_opened == false)
        {
            System.out.print("open failed");
            if (_errorMessage == null)
            {
                System.out.println(".");
            }
            else
            {
                System.out.println(" - " + _errorMessage);
                _errorMessage = null;
            }
        }
        else
        {
            System.out.println("open successful.");
            run();
        }

        return;
    }

    /**
     * Application thread continues to transmit and receive
     * messages at random intervals until stopped.
     */
    @Override
    public void run()
    {
        long sleeptime;
        InetAddress address = _client_socket.getAddress();
        int port = _client_socket.getPort();
        String port_string = Integer.toString(port);
        StopThread thread = new StopThread(this);
        int message_count = 1;
        String message_base = "This is message #";
        String message;
        byte[] data;

        // Remember this thread for later.
        if (_my_thread == null)
        {
            _my_thread = Thread.currentThread();
        }

        // Create a thread to watch for a keystroke.
        thread.start();

        // Now sit here waiting to send and receive.
        _isRunning = true;
        while (_isRunning == true)
        {
            try
            {
                // Decide how long before the next alarm is issued.
                // Sleep time is in milliseconds but no less than 100.
                sleeptime = (_randomizer.nextLong() % MAX_SLEEP_TIME);
                if (sleeptime < MIN_SLEEP_TIME)
                {
                    sleeptime = MIN_SLEEP_TIME;
                }

                Thread.sleep(sleeptime);

                // Now send a message.
                message = message_base +
                        Integer.toString(message_count) +
                        ".";
                message_count++;
                data = message.getBytes();
                System.out.print("Transmitting to " +
                                 address.getHostName() +
                                 ":" +
                                 port_string +
                                 ": \"" +
                                 message +
                                 "\" ... ");
                _client_socket.transmit(data, 0, data.length);
            }
            catch (InterruptedException interrupt) {}
            catch (Exception jex)
            {
                jex.printStackTrace(System.err);
                _isRunning = false;
            }
        }

        // Now that we are no longer running, close the
        // connection.
        System.out.print("Closing connection to " +
                         address.getHostName() +
                         ":" +
                         port_string +
                         " ... ");
        _client_socket.close();
        System.out.println("closed.");

        if (_owner != null)
        {
            _owner.clientClosed(this);
        }

        return;
    }

    // Stop the app.

    /**
     * Stops the application.
     */
    public void halt()
    {
        _isRunning = false;

        // Wake me up in case I am sleeping.
        _my_thread.interrupt();

        return;
    }

    /**
     * Handles newly opened TCP client connection.
     * @param client callback from this TCP client connection.
     */
    @Override
    public void opened(TcpConnection client)
    {
        _opened = true;
        _my_thread.interrupt();
        return;
    }

    /**
     * Handles TCP client connection open failure.
     * @param reason reason explaining connection failure.
     * @param client callback from this TCP client connection.
     */
    @Override
    public void openFailed(String reason, TcpConnection client)
    {
        _opened = false;
        _errorMessage = reason;
        _my_thread.interrupt();
        return;
    }

    /**
     * Client connection is now half closed.
     * @param client callback from this TCP client connection.
     */
    @Override
    public void halfClosed(TcpConnection client)
    {
        InetAddress address = _client_socket.getAddress();
        String port_string = Integer.toString(_client_socket.getPort());

        System.out.print("Connection from " +
                         address.getHostName() +
                         ":" +
                         port_string +
                         " has closed its side. ");

        // The far end has closed its connection. Stop running
        // since it is no longer listening.
        _isRunning = false;
        _my_thread.interrupt();

        return;
    }

    /**
     * Client connection is now fully closed.
     * @param reason reason explaining connection close.
     * @param client callback from this TCP client connection.
     */
    @Override
    public void closed(String reason, TcpConnection client)
    {
        _opened = false;
        _isRunning = false;
        _my_thread.interrupt();
        return;
    }

    /**
     * Most recent data transmitted.
     * @param client callback from this TCP client connection.
     */
    @Override
    public void transmitted(TcpConnection client)
    {
        System.out.println("transmit successful.");
        return;
    }

    /**
     * Most recent transmit failed.
     * @param reason text explaining transmission failure.
     * @param client callback from this TCP client connection.
     */
    @Override
    public void transmitFailed(String reason, TcpConnection client)
    {
        System.out.println("transmit failed - " +
                           reason +
                           ".");
        return;
    }

    /**
     * Most recently received data.
     * @param data received data.
     * @param client callback from this TCP client connection.
     */
    @Override
    public void receive(byte[] data, TcpConnection client)
    {
        String message = new String(data);

        System.out.println("Received data from " +
                           ((TcpClient) client).getAddress() +
                           ":" +
                           Integer.toString(((TcpClient) client).getPort()) +
                           ": \"" +
                           message +
                           "\"");
        return;
    }

    /**
     * Most recently accept TCP client connection.
     * @param client accepted client connection.
     * @param server callback from this TCP server connection.
     */
    @Override
    public void accepted(TcpClient client, TcpServer server) {}

// Member data

    private TcpClient _client_socket;
    private boolean _isRunning;
    private boolean _opened;
    private Thread _my_thread;
    private final server _owner;
    private String _errorMessage;

    // Use the following to randomly decide when to issue an
    // alarm and what type, etc.
    private final Random _randomizer;

    // Constants.
    private static final long MIN_SLEEP_TIME = 100;
    private static final long MAX_SLEEP_TIME = 30000; // 30 seconds.

// Inner classes.

    private final class StopThread
        extends Thread
    {
        private StopThread(client client)
        {
            _client = client;
        }

        @Override
        public void run()
        {
            // As soon as any key is hit, stop.
            try
            {
                System.in.read();
            }
            catch (IOException io_exception)
            {}

            _client.halt();

            return;
        }

        private final client _client;
    }
}
