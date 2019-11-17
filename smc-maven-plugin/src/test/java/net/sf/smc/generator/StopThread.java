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
// Copyright (C) 2019. Charles W. Rapp.
// All Rights Reserved.
//

package net.sf.smc.generator;

import java.io.IOException;


/**
 * Informs the target when the enter/return key to be pressed.
 *
 * @param <T> target object type.
 *
 * @author <a href="mailto:rapp@acm.org">Charles W. Rapp</a>
 */

public final class StopThread<T extends IStoppable>
    extends Thread
{
//---------------------------------------------------------------
// Member data.
//

    //-----------------------------------------------------------
    // Constants.
    //

    private static final String THREAD_NAME = "__StopThread__";

    //-----------------------------------------------------------
    // Locals.
    //

    private final T mTarget;

//---------------------------------------------------------------
// Member methods.
//

    //-----------------------------------------------------------
    // Constructors.
    //

    /**
     * Creates a new stop thread for the given target.
     * @param target stoppable target object.
     */
    public StopThread(final T target)
    {
        this (target, THREAD_NAME);
    } // end of StopThread(T)

    /**
     * Creates a new stop thread for the given name and target.
     * @param target stoppable target object.
     * @param name thread name.
     */
    public StopThread(final T target,
                      final String name)
    {
        super (name);

        mTarget = target;
    } // end of StopThread(T, String)

    //
    // end of Constructors.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // Thread Method Overrides.
    //

    @Override
    public void run()
    {
        System.out.println(
            "(Starting execution. Hit Enter to stop.)");

        // As soon as any key is hit, stop.
        try
        {
            System.in.read();
        }
        catch (IOException io_exception)
        {}

        System.out.println("(Stopping execution.)");

        mTarget.halt();
    } // end of run()

    //
    // end of Thread Method Overrides.
    //-----------------------------------------------------------
} // end of class StopThread
