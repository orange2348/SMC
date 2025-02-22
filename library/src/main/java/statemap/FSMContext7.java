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
// The Original Code is  State Machine Compiler(SMC).
//
// The Initial Developer of the Original Code is Charles W. Rapp.
// Portions created by Charles W. Rapp are
// Copyright (C) 2014. Charles W. Rapp.
// All Rights Reserved.
//
// Contributor(s):
//
// statemap.java --
//
//  This package defines the FSMContext7 class which must be
//  inherited by any Java class wanting to use an smc-generated
//  state machine.
//
// RCS ID
// $Id: FSMContext7.java,v 1.2 2015/08/02 19:44:02 cwrapp Exp $
//

package statemap;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EmptyStackException;

/**
 * Base class for all SMC-generated application context classes.
 * This class stores the FSM name, current and previous states,
 * the state stack, debugging information and state change
 * listeners.
 *
 * @author <a href="mailto:rapp@acm.org">Charles Rapp</a>
 */

public abstract class FSMContext7
    implements Serializable
{
//---------------------------------------------------------------
// Member data
//

    //-----------------------------------------------------------
    // Constants.
    //

    /**
     * The state change property name.
     */
    public static final String STATE_PROPERTY = "State";

    /**
     * The default transition identifier is zero.
     */
    protected static final int DEFAULT_TRANSITION_ID = 0;

    /**
     * Default state and transition name is "Default".
     */
    protected static final String DEFAULT_NAME =
        "Default";

    /**
     * The ultimate system default transition method is
     * "defaultTransition".
     */
    protected static final String SYSTEM_DEFAULT =
        "defaultTransition";

    /**
     * The state entry method name is:
     * "&lt;map&gt;_&lt;state&gt;".
     */
    protected static final String STATE_NAME_FORMAT =
        "%s_%s";

    /**
     * The state entry method name is:
     * "&lt;map&gt;_&lt;state&gt;__Entry_".
     */
    protected static final String ENTRY_NAME =
        "%s_%s__Entry_";

    /**
     * The state exit method name is:
     * "&lt;map&gt;_&lt;state&gt;__Exit_".
     */
    protected static final String EXIT_NAME =
        "%s_%s__Exit_";

    /**
     * The state exit method name is:
     * "&lt;map&gt;_&lt;state&gt;_&lt;transition&gt;".
     */
    protected static final String TRANSITION_NAME_FORMAT =
        "%s_%s_%s";

    /**
     * The method signature for state entry, exit actions is:
     * {@code void &lt;method name&gt;()}.
     */
    protected static final MethodType NO_ARGS_TYPE =
        MethodType.methodType(void.class);

    /**
     * The SMC version for Java serialization purposes.
     */
    private static final long serialVersionUID = 0x070100L;

    //-----------------------------------------------------------
    // Locals.
    //

    /**
     * The FSM name.
     */
    transient protected String mName;

    /**
     * The current state. Will be {@code null} while in
     * transition.
     */
    transient protected State7 mState;

    /**
     * The current transition name. Used for debugging
     * purposes. Will be en empty string when not in
     * transition.
     */
    transient protected String mTransition;

    /**
     * Stores which state a transition left. May be {@code null}.
     */
    transient protected State7 mPreviousState;

    /**
     * This stack is used to store the current state when a push
     * transition is taken.
     */
    transient protected Deque<State7> mStateStack;

    /**
     * When this flag is set to {@code true}, this class will
     * print out debug messages.
     */
    transient protected boolean mDebugFlag;

    /**
     * Write debug output to this stream.
     */
    transient protected PrintStream mDebugStream;

    /**
     * Store the property change listeners here.
     */
    transient private PropertyChangeSupport mListeners;

//---------------------------------------------------------------
// Member functions
//

    //-----------------------------------------------------------
    // Constructors.
    //

    /**
     * Creates a finite state machine context for the given
     * initial state.
     * @param initState the finite state machine's start state.
     */
    protected FSMContext7(State7 initState)
    {
        mName = "FSMContext";
        mState = initState;
        mTransition = "";
        mPreviousState = null;
        mStateStack = null;
        mDebugFlag = false;
        mDebugStream = System.err;
        mListeners = new PropertyChangeSupport(this);
    } // end of FSMContext7(State7)

    //
    // end of Constructors.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // Abstract Method Declarations.
    //

    /**
     * Executes the state entry or exit action. Because the entry
     * and exit methods are defined in the subclass and
     * {@link MethodHandle#invokeExact(Object[])} requires that
     * the method be invoked against the subclass instance, the
     * entry/exit method handle is passed to the subclass for
     * execution.
     * @param mh execute this method handle.
     * @see #enterStartState
     * @see #enterState
     * @see #exitState
     */
    protected abstract void executeAction(MethodHandle mh);

    //
    // end of Abstract Method Declarations.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // Serializable Interface Implementation.
    //

    private void readObject(ObjectInputStream istream)
        throws IOException,
               ClassNotFoundException
    {
        istream.defaultReadObject();

        // Create an empty listeners list.
        mListeners = new PropertyChangeSupport(this);

        return;
    } // end of readObject(ObjectInputStream)

    //
    // end of Serializable Interface Implementation.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // Get methods.
    //

    /**
     * Returns the FSM name.
     * @return the FSM name.
     */
    public String getName()
    {
        return (mName);
    } // end of getName()

    /**
     * When debug is set to {@code true}, the state machine
     * will print messages to the console.
     * @return {@code true} if debug output is generated.
     */
    public boolean getDebugFlag()
    {
        return (mDebugFlag && mDebugStream != null);
    } // end of getDebugFlag()

    /**
     * Writes the debug output to this stream.
     * @return the debug output stream.
     */
    public PrintStream getDebugStream()
    {
        return (mDebugStream == null ?
                System.err :
                mDebugStream);
    } // end of getDebugStream()

    /**
     * Returns {@code true} if this FSM is in a transition and
     * {@code false} otherwise.
     * @return {@code true} if this FSM is in a transition and
     * {@code false} otherwise.
     */
    public boolean isInTransition()
    {
        return (mState == null);
    } // end of isInTransition()

    /**
     * Returns the current state.
     * @return current state.
     * @exception StateUndefinedException
     * if the FSM is in transition.
     */
    public State7 getState()
        throws StateUndefinedException
    {
        if (mState == null)
        {
            throw (new StateUndefinedException());
        }

        return (mState);
    } // end of getState()

    /**
     * If this FSM is in transition, then returns the previous
     * state which the last transition left.
     * @return the previous state which the current transition
     * left. May return {@code null}.
     */
    public State7 getPreviousState()
        throws NullPointerException
    {
        return (mPreviousState);
    } // end of getPreviousState()

    /**
     * If this FSM is in transition, then returns the transition
     * name. If not in transition, then returns an empty string.
     * @return the current transition name.
     */
    public String getTransition()
    {
        return (mTransition);
    } // end of getTransition()

    //
    // end of Get methods.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // Set methods.
    //

    /**
     * Sets the FSM name.
     * @param name The finite state machine name.
     */
    public void setName(String name)
    {
        if (name != null &&
            name.length() > 0 &&
            name.equals(mName) == false)
        {
            mName = name;
        }

        return;
    } // end of setName(String)

    /**
     * Turns debug output on if {@code flag} is {@code true} and
     * off if {@code flag} is {@code false}.
     * @param flag {@code true} to turn debuggin on and
     * {@code false} to turn debugging off.
     */
    public void setDebugFlag(boolean flag)
    {
        mDebugFlag = flag;
        return;
    } // end of setDebugFlag(boolean)

    /**
     * Sets the debug output stream to the given value.
     * @param stream The debug output stream.
     */
    public void setDebugStream(PrintStream stream)
    {
        mDebugStream = stream;
        return;
    } // end of setDebugStream(PrintStream)

    /**
     * Sets the current state to the given value.
     * @param state The current state.
     */
    public void setState(State7 state)
    {
        if (mDebugFlag == true)
        {
            mDebugStream.println(
                "ENTERING STATE  : " + state.getName());
        }

        // clearState() is not called when a transition has
        // no actions, so set _previousState to _state in
        // that situation. We know clearState() was not
        // called when _state is not null.
        if (mState != null)
        {
            mPreviousState = mState;
        }

        mState = state;

        // Inform any and all listeners about this state
        // change.
        mListeners.firePropertyChange(STATE_PROPERTY, mPreviousState, mState);

        return;
    } // end of setState(State7)

    /**
     * Places the current state into the previous state sets
     * the current state to {@code null}.
     */
    public void clearState()
    {
        mPreviousState = mState;
        mState = null;

        return;
    } // end of clearState()

    /**
     * Pushes the current state on top of the state stack and
     * sets the current state to {@code state}.
     * @param state The new current state.
     * @exception NullPointerException
     * if {@code state} is {@code null}.
     */
    public void pushState(State7 state)
    {
        if (mState == null)
        {
            throw (new NullPointerException());
        }

        if (mDebugFlag == true)
        {
            mDebugStream.println(
                "PUSH TO STATE   : " + state.getName());
        }

        if (mStateStack == null)
        {
            mStateStack = new ArrayDeque<>();
        }

        mPreviousState = mState;
        mStateStack.push(mState);
        mState = state;

        // Inform any and all listeners about this state
        // change.
        mListeners.firePropertyChange(STATE_PROPERTY, mPreviousState, mState);

        return;
    } // end of pushState(State7)

    /**
     * Sets the previous state to the current state and pops
     * the top state off the stack and places it into the
     * current state.
     * @exception EmptyStackException
     * if the state stack is empty.
     */
    public void popState()
        throws EmptyStackException
    {
        if (mStateStack == null ||
            mStateStack.isEmpty() == true)
        {
            if (mDebugFlag == true)
            {
                mDebugStream.println(
                    "POPPING ON EMPTY STATE STACK.");
            }

            throw (new EmptyStackException());
        }
        else
        {
            // clearState() is not called when a transition has
            // no actions, so set _previousState to _state in
            // that situation. We know clearState() was not
            // called when _state is not null.
            if (mState != null)
            {
                mPreviousState = mState;
            }

            // The pop method removes the top element
            // from the stack and returns it.
            mState = mStateStack.pop();

            if (mStateStack.isEmpty() == true)
            {
                mStateStack = null;
            }

            if (mDebugFlag == true)
            {
                mDebugStream.println("POP TO STATE    : " + mState.getName());
            }

            // Inform any and all listeners about this state
            // change.
            mListeners.firePropertyChange(STATE_PROPERTY, mPreviousState, mState);
        }

        return;
    } // end of popState()

    /**
     * Empties the state stack.
     */
    public void emptyStateStack()
    {
        if (mStateStack != null)
        {
            mStateStack.clear();
            mStateStack = null;
        }

        return;
    } // end of emptyStateStack()

    /**
     * Executes the current state entry action. This method is
     * redundant with {@link #enterState} and is provided for
     * backward compatibility.
     * @see #enterState
     */
    public void enterStartState()
    {
        enterState();
        return;
    } // end of enterStartState()

    //
    // end of Set methods.
    //-----------------------------------------------------------

    // The following methods allow listeners to watch this
    // finite state machine for state changes.
    // Note: if a transition does not cause a state change,
    // then no state change event is fired.

    /**
     * Adds a PropertyChangeListener to the listener list. The
     * listener is registered for state property changes only.
     * The same listener may be added more than once. For each
     * state change, the listener will be invoked the number of
     * times it was added. If {@code listener} is {@code null},
     * no exception is thrown and no action is taken.
     * @param listener The PropertyChangeListener to be added.
     */
    public void
        addStateChangeListener(PropertyChangeListener listener)
    {
        mListeners.addPropertyChangeListener(
            STATE_PROPERTY, listener);
        return;
    }

    /**
     * Removes a PropertyChangeListener for the state change
     * property. If {@code listener} was added more than once
     * to the same event source, it will be notified one less
     * time after being removed. If {@code listener} is
     * {@code null} or was never added, no exception is thrown
     * and no action is taken.
     * @param listener The PropertyChangeListener to be removed.
     */
    public void
        removeStateChangeListener(
            PropertyChangeListener listener)
    {
        mListeners.removePropertyChangeListener(
            STATE_PROPERTY, listener);
        return;
    } // end of removeStateChangeListener(PropertyChangeListener)

    /**
     * The default Default state/Default transition definition is
     * to throw a {@link TransitionUndefinedException} which
     * contains the current state and current transition in the
     * detailed message.
     */
    protected void defaultTransition()
    {
        if (mDebugFlag == true)
        {
            mDebugStream.println("TRANSITION      : Default");
        }

        throw (
            new TransitionUndefinedException(
                "State: " +
                stateName() +
                ", Transition: " +
                getTransition()));
    } // end of defaultTransition()

    /**
     * Returns the name of the current state or previous state
     * if in transition. If there is no current or previous state
     * set, then returns "(state unknown)".
     * @return the current or previous state name.
     */
    protected String stateName()
    {
        return (mState != null ?
                mState.getName() :
                (mPreviousState != null ?
                 mPreviousState.getName() :
                 "(state unknown)"));
    } // end of stateName()

    /**
     * Executes the current state entry action, if defined.
     */
    protected void enterState()
    {
        final MethodHandle mh = mState.enterState();

        if (mh != null)
        {
            executeAction(mh);
        }

        return;
    } // end of enterState()

    /**
     * Executes the current state exit action, if defined.
     */
    protected void exitState()
    {
        final MethodHandle mh = mState.exitState();

        if (mh != null)
        {
            executeAction(mh);
        }

        return;
    } // end of exitState()

    /**
     * Returns the virtual method handle found by {@code lookup}
     * using the given parameters. If the lookup fails, then
     * returns {@code null}.
     * @param lookup used to retrieve the virtual method handle.
     * @param clazz the method is implemented in this class.
     * @param name the method name.
     * @param mt the method signature.
     * @return the virtual method handle for the given class,
     * method name, and signature.
     */
    protected static MethodHandle
        lookupMethod(final Lookup lookup,
                     final Class<?> clazz,
                     final String name,
                     final MethodType mt)
    {
        MethodHandle retval = null;

        try
        {
            retval = lookup.findVirtual(clazz, name, mt);
        }
        catch (NoSuchMethodException |
               IllegalAccessException |
               SecurityException |
               NullPointerException jex)
        {
            // Ignore and return null.
        }

        return (retval);
    } // end of lookupMethod(Lookup, String, MethodType)

    /**
     * Returns the transition method handle for the given map,
     * state, transition name, and transition method signature.
     * If the method is not found in the current state, then
     * returns the appropriate default transition. The search
     * order is:
     * <ol>
     *   <li>
     *     &lt;map&gt;::&lt;state&gt;::&lt;transition&gt;(parameters)
     *   </li>
     *   <li>
     *     &lt;map&gt;::Default::&lt;transition&gt;(parameters)
     *   </li>
     *   <li>
     *     &lt;map&gt;::&lt;state&gt;::Default()
     *   </li>
     *   <li>
     *     &lt;map&gt;::Default::Default()
     *   </li>
     *   <li>
     *     {@link FSMContext7#defaultTransition}
     *   </li>
     * </ol>
     * @param lookup used to retrieve the virtual method handle.
     * @param clazz the method is implemented in this class.
     * @param mapName the map name.
     * @param stateName the state name.
     * @param transName the transition name.
     * @param methodType the transition method signature.
     * @return the transition method handle.
     */
    protected static TransitionHandle
        lookupTransition(final Lookup lookup,
                         final Class<?> clazz,
                         final String mapName,
                         final String stateName,
                         final String transName,
                         final MethodType methodType)
    {
        MethodType mt = methodType;
        String mn = String.format(TRANSITION_NAME_FORMAT,
                                  mapName,
                                  stateName,
                                  transName);
        boolean isDefault = false;
        MethodHandle mh = lookupMethod(lookup, clazz, mn, mt);

        // Is this method in the current state?
        if (mh == null)
        {
            // No. Is this method in the default state?
            mn =
                String.format(TRANSITION_NAME_FORMAT,
                              mapName,
                              DEFAULT_NAME,
                              transName);
            mh = lookupMethod(lookup, clazz, mn, mt);
            if (mh == null)
            {
                // No. this means that a default transition must
                // be used.
                isDefault = true;
                mt = NO_ARGS_TYPE;

                // Does the current state have a default
                // transition?
                mn = String.format(TRANSITION_NAME_FORMAT,
                                   mapName,
                                   stateName,
                                   DEFAULT_NAME);
                mh = lookupMethod(lookup, clazz, mn, mt);
                if (mh == null)
                {
                    // No, again.
                    // Finally, does the default state have a
                    // default transition?
                    mn = String.format(TRANSITION_NAME_FORMAT,
                                       mapName,
                                       DEFAULT_NAME,
                                       DEFAULT_NAME);
                    mh = lookupMethod(lookup, clazz, mn, mt);
                    if (mh == null)
                    {
                        // No, four strikes and you are out.
                        // Use the system default transition.
                        mh = lookupMethod(lookup,
                                          clazz,
                                          SYSTEM_DEFAULT,
                                          mt);
                    }
                }
            }
        }

        return (new TransitionHandle(isDefault, mh));
    } // end of lookupTransition(...)
} // end of class FSMContext7
