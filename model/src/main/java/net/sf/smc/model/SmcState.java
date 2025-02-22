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
// Copyright (C) 2000 - 2005. Charles W. Rapp.
// All Rights Reserved.
//
// Contributor(s):
//   Eitan Suez contributed examples/Ant.
//   (Name withheld) contributed the C# code generation and
//   examples/C#.
//   Francois Perrad contributed the Python code generation and
//   examples/Python.
//   Chris Liscio contributed the Objective-C code generation
//   and examples/ObjC.
//
// RCS ID
// Id: SmcState.java,v 1.4 2013/07/14 14:32:38 cwrapp Exp
//
// CHANGE LOG
// (See the bottom of this file.)
//

package net.sf.smc.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Contains the entry action, exit action and transition lists.
 * Also stores the owning {@link net.sf.smc.model.SmcMap map}
 * and this state's name.
 *
 * @see net.sf.smc.model.SmcMap
 * @see net.sf.smc.model.SmcTransition
 * @author <a href="mailto:rapp@acm.org">Charles Rapp</a>
 */

public final class SmcState
    extends SmcElement
{
//---------------------------------------------------------------
// Member data
//

    //-----------------------------------------------------------
    // Constants.
    //

    /**
     * The default state instance name is "DefaultState".
     */
    public static final String DEFAULT_STATE = "DefaultState";

    //-----------------------------------------------------------
    // Locals.
    //

    private final SmcMap mMap;
    private final String mClassName;
    private final String mInstanceName;
    private List<SmcAction> mEntryActions;
    private List<SmcAction> mExitActions;
    private final List<SmcTransition> mTransitions;

//---------------------------------------------------------------
// Member methods
//

    //-----------------------------------------------------------
    // Constructors.
    //

    /**
     * Creates a state instance for the given state name, the
     * line number where the state appears in the .sm file and
     * the map to which this state belongs.
     * @param name the state name.
     * @param lineNumber where the state appears in the .sm file.
     * @param map the state is in this map.
     */
    public SmcState(String name, int lineNumber, SmcMap map)
    {
        super (name, lineNumber);

        mMap = map;

        if (name.compareToIgnoreCase("Default") == 0)
        {
            mInstanceName = "DefaultState";
        }
        else
        {
            mInstanceName = name;
        }

        mClassName = name;

        mEntryActions = null;
        mExitActions = null;
        mTransitions = new ArrayList<>();
    } // end of SmcState(String, int, SmcMap)

    //
    // end of Constructors.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // SmcElement Abstract Methods.
    //

    /**
     * Calls the visitor's visit method for this finite state
     * machine element.
     * @param visitor The visitor instance.
     * @see SmcVisitor
     */
    @Override
    public void accept(final SmcVisitor visitor)
    {
        visitor.visit(this);
        return;
    } // end of accept(SmcVisitor)

    //
    // end of SmcElement Abstract Methods.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // Object Method Overrides.
    //

    /**
     * Returns this state text representation.
     * @return this state text representation.
     */
    @Override
    public String toString()
    {
        final StringBuilder retval = new StringBuilder();

        retval.append(mInstanceName);

        if (mEntryActions != null && !mEntryActions.isEmpty())
        {
            retval.append("\n\tEntry {");
            for (SmcAction action: mEntryActions)
            {
                retval.append(action);
                retval.append('\n');
            }
            retval.append("}");
        }

        if (mExitActions != null && !mExitActions.isEmpty())
        {
            retval.append("\n\tExit {");
            for (SmcAction action: mExitActions)
            {
                retval.append(action);
                retval.append('\n');
            }
            retval.append("}");
        }

        for (SmcTransition transition: mTransitions)
        {
            retval.append("\n");
            retval.append(transition);
        }

        return (retval.toString());
    } // end of toString()

    //
    // end of Object Method Overrides.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // Get Methods.
    //

    /**
     * Returns the map to which this state belongs.
     * @return the map to which this state belongs.
     */
    public SmcMap getMap()
    {
        return (mMap);
    } // end of getMap()

    /**
     * Returns the state name.
     * @return the state name.
     */
    @Override
    public String getName()
    {
        return(mClassName + "." + mInstanceName);
    } // end of getName()

    /**
     * Returns the state class name.
     * @return the state class name.
     */
    public String getClassName()
    {
        return(mClassName);
    } // end of getClassName()

    /**
     * Returns the state instance name.
     * @return the state instance name.
     */
    public String getInstanceName()
    {
        return(mInstanceName);
    } // end of getInstanceName()

    /**
     * Returns {@code true} if this is the default state and
     * {@code false} otherwise.
     * @return {@code true} if this is the default state.
     */
    public boolean isDefaultState()
    {
        return (mInstanceName.equals(DEFAULT_STATE));
    } // end of isDefaultState()

    /**
     * Returns the entry action list.
     * @return the entry action list.
     */
    public List<SmcAction> getEntryActions()
    {
        return(mEntryActions);
    } // end of getEntryActions()

    /**
     * Returns the exit action list.
     * @return the exit action list.
     */
    public List<SmcAction> getExitActions()
    {
        return(mExitActions);
    } // end of getExitActions()

    /**
     * Returns the state transitions.
     * @return the state transitions.
     */
    public List<SmcTransition> getTransitions()
    {
        return(mTransitions);
    } // end of getTransitions()

    /**
     * Returns the transition with the specified name and
     * parameters. May return {@code null}.
     * @param name the transition name.
     * @param parameters the transition parameters.
     * @return the transition with the specified name and
     * parameters.
     */
    public SmcTransition
        findTransition(final String name,
                       final List<SmcParameter> parameters)
    {
        Iterator<SmcTransition> transIt;
        SmcTransition transition;
        SmcTransition retval;

        for (transIt = mTransitions.iterator(), retval = null;
             transIt.hasNext() && retval == null;
            )
        {
            transition = transIt.next();
            if (name.equals(transition.getName()) &&
                transition.compareTo(name, parameters) == 0)
            {
                retval = transition;
            }
        }

        return (retval);
    } // end of findTransition(String, List<SmcParameter>)

    /**
     * Returns the guard with the specified name and
     * condition. May return {@code null}.
     * @param name the transition name.
     * @param condition the condition guard
     * @return the guard with the specified name and
     * condition.
     */
    public SmcGuard findGuard(final String name,
                              final String condition)
    {
        for (SmcTransition transition: mTransitions)
        {
            if (name.equals(transition.getName()))
            {
                for (SmcGuard guard: transition.getGuards())
                {
                    if (condition.equals(guard.getCondition()))
                    {
                        return guard;
                    }
                }
            }
        }

        return null;
    } // end of findGuard(String, String)

    /**
     * @param name the transition name.
     * @return true or false
     */
    public boolean callDefault(final String name)
    {
        for (SmcTransition transition: mTransitions)
        {
            if (name.equals(transition.getName()))
            {
                for (SmcGuard guard : transition.getGuards())
                {
                    if ((guard.getCondition()).isEmpty())
                    {
                        return false;
                    }
                }

                return true;
            }
        }

        for (SmcTransition transition : mTransitions)
        {
            if (transition.getName().equals("Default"))
            {
                return false;
            }
        }

        return true;
    } // end of callDefault(String)

    //
    // end of Get methods.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // Set methods.
    //

    /**
     * Sets the state entry actions.
     * @param actions the state entry actions.
     */
    public void setEntryActions(final List<SmcAction> actions)
    {
        mEntryActions = new ArrayList<>(actions);
        return;
    } // end of setEntryActions(List<SmcAction>)

    /**
     * Sets the state exit actions.
     * @param actions the state exit actions.
     */
    public void setExitActions(final List<SmcAction> actions)
    {
        mExitActions = new ArrayList<>(actions);
        return;
    } // end of setExitActions(List<SmcAction>)

    /**
     * Adds a transition to the list.
     * @param transition add this transition.
     */
    public void addTransition(final SmcTransition transition)
    {
        // Add the transition only if it is not already in the
        // list.
        if (!mTransitions.contains(transition))
        {
            mTransitions.add(transition);
        }

        return;
    } // end of addTransition(SmcTransition)

    //
    // end of Set methods.
    //-----------------------------------------------------------
} // end of class SmcState

//
// CHANGE LOG
// Log: SmcState.java,v
// Revision 1.4  2013/07/14 14:32:38  cwrapp
// check in for release 6.2.0
//
// Revision 1.3  2011/11/20 14:58:33  cwrapp
// Check in for SMC v. 6.1.0
//
// Revision 1.2  2010/03/08 17:02:41  fperrad
// New representation of the Default state. The result is full UML.
//
// Revision 1.1  2009/03/01 18:20:42  cwrapp
// Preliminary v. 6.0.0 commit.
//
// Revision 1.9  2007/02/21 13:56:44  cwrapp
// Moved Java code to release 1.5.0
//
// Revision 1.8  2007/01/15 00:23:52  cwrapp
// Release 4.4.0 initial commit.
//
// Revision 1.7  2006/09/16 15:04:29  cwrapp
// Initial v. 4.3.3 check-in.
//
// Revision 1.6  2005/11/07 19:34:54  cwrapp
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
// Revision 1.5  2005/05/28 19:28:42  cwrapp
// Moved to visitor pattern.
//
// Revision 1.6  2005/02/21 15:38:04  charlesr
// Added Francois Perrad to Contributors section for Python work.
//
// Revision 1.5  2005/02/03 16:49:43  charlesr
// In implementing the Visitor pattern, the generateCode()
// methods have been moved to the appropriate Visitor
// subclasses (e.g. SmcJavaGenerator). This class now extends
// SmcElement.
//
// Revision 1.4  2004/10/30 16:07:55  charlesr
// Added Graphviz DOT file generation.
//
// Revision 1.3  2004/10/08 18:55:41  charlesr
// Fixed C# exit action generation.
//
// Revision 1.2  2004/09/06 16:41:40  charlesr
// Added C# support.
//
// Revision 1.1  2004/05/31 13:57:04  charlesr
// Added support for VB.net code generation.
//
// Revision 1.0  2003/12/14 21:06:45  charlesr
// Initial revision
//
