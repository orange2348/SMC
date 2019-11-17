/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.sf.smc.generator;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author charlesr
 */

public final class JavaGeneratorTest
{
//---------------------------------------------------------------
// Member data.
//

    //-----------------------------------------------------------
    // Constants.
    //

    private static final InetAddress TEST_HOST;
    private static final int TEST_PORT = 9009;

    //
    // server process configuration.
    //

    /**
     * Contains the standard java command line parameters. The
     * service port will be added later.
     */
    private static final List<String> ECHO_ARGS;

    /**
     * Wait {@value} milliseconds for the test server process
     * to stop.
     */
    private static final long PROC_WAIT_TIME = 1000L;

    //-----------------------------------------------------------
    // Statics.
    //

    private static Process sTestServer = null;

    // Class static initialization.
    static
    {
        TEST_HOST = InetAddress.getLoopbackAddress();

        final String javaBin =
            System.getProperty("java.home") +
            File.separator +
            "bin" +
            File.separator;

        ECHO_ARGS = new ArrayList<>();
        ECHO_ARGS.add(javaBin + "java");

        ECHO_ARGS.add("-classpath");
        ECHO_ARGS.add(System.getProperty("java.class.path"));
        ECHO_ARGS.add((server.class).getCanonicalName());
    } // end of class static initialization.

//---------------------------------------------------------------
// Member methods.
//

    //-----------------------------------------------------------
    // JUnit Tests.
    //

    @Ignore
    @Test
    public void generateCode()
        throws IOException,
               IllegalAccessException,
               InvocationTargetException
    {
        final int xmitCount = 100;
        final client client = new client(xmitCount);

        startServer();

        synchronized (client)
        {
            client.start(TEST_HOST, TEST_PORT);

            while (client.isRunning() && client.isOpen())
            {
                try
                {
                    client.wait();
                }
                catch (InterruptedException interrupt)
                {}
            }
        }

        stopServer();
    } // end of generateCode()

    //
    // end of JUnit Tests.
    //-----------------------------------------------------------

    private static void startServer()
    {
        final List<String> args = new ArrayList<>(ECHO_ARGS);
        Throwable tex = null;

        args.add(Integer.toString(TEST_PORT));

        try
        {
            final ProcessBuilder pb = new ProcessBuilder(args);

            sTestServer = pb.start();

            // Wait for the subprocess to start.
            try
            {
                Thread.sleep(PROC_WAIT_TIME);
            }
            catch (InterruptedException interrupt)
            {}

            final BufferedReader reader =
                new BufferedReader(
                    new InputStreamReader(
                        sTestServer.getInputStream()));

//            reader.readLine();
            System.out.println(reader.readLine());
        }
        catch (IOException ioex)
        {
            if (sTestServer != null)
            {
                sTestServer.destroy();
            }

            throw (
                new IllegalStateException(
                    "echo server open failed", tex));
        }
    } // end of startServer()

    private static void stopServer()
    {
        if (sTestServer != null)
        {
            sTestServer.destroy();
            sTestServer = null;

            // Give the process a chance to stop.
            try
            {
                Thread.sleep(PROC_WAIT_TIME);
            }
            catch (InterruptedException interrupt)
            {}
        }
    } // end of stopServer()
} // end of JavaGeneratorTest