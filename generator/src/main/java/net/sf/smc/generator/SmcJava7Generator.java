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
// Copyright (C) 2005 - 2009, 2019. Charles W. Rapp.
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
// $Id: SmcJava7Generator.java,v 1.2 2015/08/02 19:44:36 cwrapp Exp $
//

package net.sf.smc.generator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.sf.smc.model.SmcAction;
import net.sf.smc.model.SmcElement;
import net.sf.smc.model.SmcElement.TransType;
import net.sf.smc.model.SmcFSM;
import net.sf.smc.model.SmcGuard;
import net.sf.smc.model.SmcMap;
import net.sf.smc.model.SmcParameter;
import net.sf.smc.model.SmcState;
import net.sf.smc.model.SmcTransition;
import net.sf.smc.model.SmcVisitor;
import net.sf.smc.model.TargetLanguage;

/**
 * Visits the abstract syntax tree, emitting Java code. Generates
 * a transition table which maps the current state to a method
 * handle. Each state map has a separate transition table but
 * all maps use a union of all transitions.
 *
 * @see SmcJavaGenerator
 * @see SmcElement
 * @see SmcCodeGenerator
 * @see SmcVisitor
 * @see SmcOptions
 *
 * @author <a href="mailto:rapp@acm.org">Charles Rapp</a>
 */

public final class SmcJava7Generator
    extends SmcCodeGenerator
{
//---------------------------------------------------------------
// Member data
//

    //-----------------------------------------------------------
    // Constants.
    //

    /**
     * The default state and transition name is "Default".
     */
    private static final String DEFAULT_NAME = "Default";

    /**
     * The ultimate system default transition method is
     * "defaultTransition".
     */
    private static final String SYSTEM_DEFAULT =
        "defaultTransition";

    /**
     * The default transition has no parameters.
     */
    private static final List<SmcParameter> DEFAULT_PARAMETERS =
        new ArrayList<>();

    /**
     * Loop back transitions use a {@value} end state.
     */
    private static final String NIL_STATE = "nil";

    /**
     * The constant integer state identifier suffix is {@value}.
     */
    private static final String STATE_ID_SUFFIX = "_STATE_ID";

    /**
     * The constant integer transition identifier suffix is
     * "_TRANSITION_ID".
     */
    private static final String TRANSITION_ID_SUFFIX =
        "_TRANSITION_ID";

    /**
     * The state entry method name is:
     * "&lt;map&gt;_&lt;state&gt;__Entry_".
     */
    private static final String ENTRY_NAME =
        "%s_%s__Entry_";

    /**
     * The state exit method name is:
     * "&lt;map&gt;_&lt;state&gt;__Exit_".
     */
    private static final String EXIT_NAME =
        "%s_%s__Exit_";

//---------------------------------------------------------------
// Member methods
//

    //-----------------------------------------------------------
    // Constructors.
    //

    /**
     * Creates a Java 7 code generator for the given options.
     * @param options The target code generator options.
     */
    public SmcJava7Generator(final SmcOptions options)
    {
        super (options, TargetLanguage.JAVA7.suffix());
    } // end of SmcJava7Generator(SmcOptions)

    //
    // end of Constructors.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // SmcVisitor Abstract Method Impelementation.
    //

    /**
     * Emits Java code for the finite state machine context
     * class.
     * @param fsm emit Java code for this finite state machine.
     */
    @Override
    public void visit(final SmcFSM fsm)
    {
        // 1. Output the read-only opening comments and user raw
        //    target code.
        outputHeader(fsm);

        // 2. Output the class import statements.
        outputImports(fsm);

        // 3. Output the class declaration and opening brace.
        outputClassDeclaration(fsm);

        // 4. Output the class constructors, owner get and set
        //    methods, and the transition methods.
        outputMethods(fsm);

        // 5. Output the class data members.
        outputData(fsm);

        // 6. Output the class closing brace and the read-only
        //    ending comments.
        outputFooter();

        return;
    } // end of visit(SmcFSM)

    /**
     * Emits Java code for the FSM map.
     * @param map emit Java code for this map.
     */
    @Override
    public void visit(final SmcMap map)
    {
        final SmcState defaultState = map.getDefaultState();

        // 1. Output the map default state transitions - if the
        //    map has a default state.
        if (defaultState != null)
        {
            defaultState.accept(this);
        }

        // 2. Output the map state transitions.
        for (SmcState state: map.getStates())
        {
            state.accept(this);
        }

        return;
    } // end of visit(SmcMap)

    /**
     * Emits Java code for this FSM state.
     * @param state emits Java code for this state.
     */
    @Override
    public void visit(final SmcState state)
    {
        final String mapName = (state.getMap()).getName();
        final String stateName = state.getClassName();
        final List<SmcAction> entryActions =
            state.getEntryActions();
        final List<SmcAction> exitActions =
            state.getExitActions();
        final List<SmcTransition> transitions =
            state.getTransitions();

        // Reset the default indentation to 4 spaces in case
        // there are state entry or exit actions.
        mIndent = "    ";

        if ((entryActions != null &&
             entryActions.isEmpty() == false) ||
            (exitActions != null &&
             exitActions.isEmpty() == false))
        {
            mTarget.println(
                "    //-----------------------------------------------------------");
            mTarget.print("    // ");
            mTarget.print(mapName);
            mTarget.print(".");
            mTarget.print(stateName);
            mTarget.println(" State Entry/Exit Actions.");
            mTarget.println("    //");

            if (entryActions != null &&
                entryActions.isEmpty() == false)
            {
                outputStateActions(mapName,
                                   stateName,
                                   ENTRY_NAME,
                                   entryActions);
            }

            if (exitActions != null &&
                exitActions.isEmpty() == false)
            {
                outputStateActions(mapName,
                                   stateName,
                                   EXIT_NAME,
                                   exitActions);
            }

            mTarget.println();
            mTarget.println("    //");
            mTarget.print("    // end of ");
            mTarget.print(mapName);
            mTarget.print(".");
            mTarget.print(stateName);
            mTarget.println(" State Entry/Exit Actions.");
            mTarget.println(
                "    //-----------------------------------------------------------");
            mTarget.println();
        }

        if (transitions.isEmpty() == false)
        {
            mTarget.println(
                "    //-----------------------------------------------------------");
            mTarget.print("    // ");
            mTarget.print(mapName);
            mTarget.print(".");
            mTarget.print(stateName);
            mTarget.println(" State Transitions.");
            mTarget.println("    //");

            // 1. Output each transition implementation.
            for (SmcTransition transition: transitions)
            {
                transition.accept(this);
            }

            mTarget.println();
            mTarget.println("    //");
            mTarget.print("    // end of ");
            mTarget.print(mapName);
            mTarget.print(".");
            mTarget.print(stateName);
            mTarget.println(" State Transitions.");
            mTarget.println(
                "    //-----------------------------------------------------------");
            mTarget.println();
        }

        return;
    } // end of visit(SmcState)

    /**
     * Emits Java code for this FSM state transition.
     * @param transition emits Java code for this state transition.
     */
    @Override
    public void visit(final SmcTransition transition)
    {
        final SmcState state = transition.getState();
        final SmcMap map = state.getMap();
        final String mapName = map.getName();
        final String stateName = state.getClassName();
        final String transName = transition.getName();
        String sep = "";

        // 1. Output transtion method declaration and opening
        //    brace.

        mTarget.println();
        mTarget.print("    private void ");
        mTarget.print(mapName);
        mTarget.print("_");
        mTarget.print(stateName);
        mTarget.print("_");
        mTarget.print(transName);
        mTarget.print("(");

        // 1.1. Output user-defined parameters.
        for (SmcParameter parameter: transition.getParameters())
        {
            mTarget.print(sep);
            parameter.accept(this);

            sep = ", ";
        }
        mTarget.println(")");

        // 1.2. Output opening brace.
        mTarget.println("    {");

        // 2. Output the transition guards.
        outputTransitionGuards(transition, mapName);

        // 3. Output the return and closing brace.
        mTarget.println();
        mTarget.println("        return;");
        mTarget.println("    }");
        mTarget.println();

        return;
    } // end of visit(SmcTransition)

    /**
     * Emits Java code for this FSM transition guard.
     * @param guard emits Java code for this transition guard.
     */
    @Override
    public void visit(final SmcGuard guard)
    {
        final TransType transType = guard.getTransType();
        final boolean hasActions =
            !(guard.getActions().isEmpty());
        final SmcTransition transition = guard.getTransition();
        final SmcState state = transition.getState();
        final SmcMap map = state.getMap();
        final String mapName = map.getName();
        final String stateName = state.getClassName();
        final String endStateName = guard.getEndState();
        String endStateId = "";
        String pushStateId = "";
        String pushStateName = "";
        boolean loopbackFlag =
            isLoopback(transType, endStateName);

        // 1. Set the end state identifier used in the setState
        //    or push state call.
        if (transType == TransType.TRANS_SET)
        {
            if (loopbackFlag)
            {
                endStateId = "stateId";
            }
            else
            {
                endStateId =
                    scopeStateName(endStateName, mapName, "_") +
                    STATE_ID_SUFFIX;
            }
        }
        // If this is a push transition, then generate the push
        // state identifier.
        // Note: a push transition has the option of
        // transitioning to another state and doing the push
        // from there. So a push transition has two end states:
        // the plain transition state and the push state.
        else if (transType == TransType.TRANS_PUSH)
        {
            if (endStateName.endsWith(DEFAULT_NAME) ||
                endStateName.endsWith(NIL_STATE))
            {
                endStateId = "stateId";
            }
            else
            {
                endStateId =
                    scopeStateName(endStateName, mapName, "_") +
                    STATE_ID_SUFFIX;
            }

            pushStateName = guard.getPushState();
            pushStateId =
                scopeStateName(pushStateName,
                               mapName,
                               "_") +
                STATE_ID_SUFFIX;
        }

        // 2. Output either "if", "else if", "else", or nothing
        // if this state's has only one unguarded transition
        // definition.
        outputGuardCondition(guard.getCondition());

        // 3. Output state exit - if this is *not* a push
        //    transition or an internal loopback.
        //
        //    Generate the next state identifier. This is done
        //    for plain transition and push transitions. This is
        //    not done for internal loopback transitions or pop
        //    transitions. It is not done for internal loopback
        //    transitions because the transition does not leave
        //    its original state. It is not done for pop
        //    transitions because the next state is extracted
        //    from the state stack.
        if (transType != TransType.TRANS_PUSH &&
            loopbackFlag == false)
        {
            outputStateExit(mapName, stateName);
        }

        // 4. Output the guard body inside a try/finally block.
        mTarget.print(mIndent);
        mTarget.println("try");
        mTarget.print(mIndent);
        mTarget.println('{');

        outputGuardBody(guard, transition, mapName, stateName);

        mTarget.print(mIndent);
        mTarget.println('}');
        mTarget.print(mIndent);
        mTarget.println("finally");
        mTarget.print(mIndent);
        mTarget.println('{');

        // 5. Output setting the next state. How this is done
        //    depends on whether this is plain transition,
        //    internal loopback, push, or pop.
        //    Note: this is done in a finally block.
        if (transType == TransType.TRANS_SET &&
            (hasActions || loopbackFlag == false))
        {
            mTarget.print(mIndent);
            mTarget.print("    setState(sStates[");
            mTarget.print(endStateId);
            mTarget.println("]);");
        }
        else if (transType == TransType.TRANS_PUSH)
        {
            // Set the next state so this it can be pushed
            // onto the state stack. But only do so if a clear
            // state was done.
            if (loopbackFlag == false || hasActions)
            {
                mTarget.print(mIndent);
                mTarget.print("    setState(sStates[");
                mTarget.print(endStateId);
                mTarget.println("]);");
            }

            // Before doing the push, execute the end state's
            // entry actions (if any) if this is not a loopback.
            if (loopbackFlag == false)
            {
                mTarget.print(mIndent);
                mTarget.println("    enterState();");
            }

            mTarget.print(mIndent);
            mTarget.print("    pushState(sStates[");
            mTarget.print(pushStateId);
            mTarget.println("]);");
        }
        else if (transType == TransType.TRANS_POP)
        {
            mTarget.print(mIndent);
            mTarget.println("    popState();");
        }

        // 6. Output the finally block closing brace.
        mTarget.print(mIndent);
        mTarget.println('}');
        mTarget.println();

        // 7. Output state entry - if this is *not* a pop
        //    transition or an internal loopback.
        if (transType == TransType.TRANS_SET &&
            loopbackFlag == false)
        {
            outputStateEnter(mapName, endStateName);
        }
        // If this a push transition, then use the push state
        // name.
        else if (transType == TransType.TRANS_PUSH)
        {
            outputStateEnter(mapName, pushStateName);
        }

        // 8. Output the pop transition.
        if (transType == TransType.TRANS_POP &&
            endStateName.isEmpty() == false &&
            endStateName.equals(SmcElement.NIL_STATE) == false)
        {
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.print(endStateName);
            mTarget.print("(");
            mTarget.print(guard.getPopArgs());
            mTarget.println(");");
        }

        // 9. If this is a guarded transition, it is necessary
        //    to close off the "if" body. DON'T PRINT A NEW LINE!
        //    Why? Because an "else" or "else if" may follow and
        //    we won't know until we go back to the transition
        //    target generator whether all clauses have been
        //    done.
        if (mGuardCount > 1 ||
            (guard.getCondition()).isEmpty() == false)
        {
            mTarget.print("        }");
        }

        return;
    } // end of visit(SmcGuard)

    /**
     * Emits Java code for this FSM action.
     * @param action emits Java code for this action.
     */
    @Override
    public void visit(final SmcAction action)
    {
        String name = action.getName();
        Iterator<String> it;
        String sep;

        // Need to distinguish between FSMContext actions and
        // application class actions. If the action is
        // "emptyStateStack", then pass it to the context.
        // Otherwise, let the application class handle it.
        mTarget.print(mIndent);
        if (action.isEmptyStateStack())
        {
            mTarget.println("    emptyStateStack();");
        }
        else
        {
        	if (action.isStatic( ) == false)
        	{
	            mTarget.print("    ctxt.");
        	}
	        mTarget.print(name);
	        mTarget.print("(");

	        for (it = action.getArguments().iterator(), sep = "";
	             it.hasNext();
	             sep = ", ")
	        {
	            mTarget.print(sep);
	            mTarget.print(it.next());
	        }

	        mTarget.println(");");
        }

        return;
    } // end of visit(SmcAction)

    /**
     * Emits Java code for this transition parameter.
     * @param parameter emits Java code for this transition
     * parameter.
     */
    @Override
    public void visit(final SmcParameter parameter)
    {
        mTarget.print(parameter.getType());
        mTarget.print(' ');
        mTarget.print(parameter.getName());

        return;
    } // end of visit(SmcParameter)

    //
    // end of SmcVisitor Abstract Method Impelementation.
    //-----------------------------------------------------------

    /**
     * Writes the unmodifiable comment, optional user raw target,
 and the package name.
     * @param fsm the parsed FSM model.
     */
    private void outputHeader(final SmcFSM fsm)
    {
        String rawSource = fsm.getSource();
        String packageName = fsm.getPackage();

        mTarget.println("/*");
        mTarget.println(" * ex: set ro:");
        mTarget.println(" * DO NOT EDIT.");
        mTarget.println(" * generated by smc (http://smc.sourceforge.net/)");
        mTarget.print(" * from file : ");
        mTarget.print(mSrcfileBase);
        mTarget.println(".sm");
        mTarget.println(" */");
        mTarget.println();

        // Dump out the raw target code, if any.
        if (rawSource != null && rawSource.length() > 0)
        {
            mTarget.println(rawSource);
            mTarget.println();
        }

        // If a package has been specified, generate the
        // package statement now.
        if (packageName != null && packageName.length() > 0)
        {
            mTarget.print("package ");
            mTarget.print(packageName);
            mTarget.println(";");
            mTarget.println();
        }

        return;
    } // end of outputHeader(SmcFSM)

    /**
     * Writes the user-defined and required import statements.
     * @param smc FSM model.
     */
    private void outputImports(final SmcFSM fsm)
    {
        // Do user-specified imports now.
        for (String imp: fsm.getImports())
        {
            mTarget.print("import ");
            mTarget.print(imp);
            mTarget.println(";");
        }

        // If serialization is on, then import the necessary
        // java.io classes.
        if (mSerialFlag)
        {
            mTarget.println("import java.io.IOException;");
            mTarget.println("import java.io.ObjectInputStream;");
            mTarget.println("import java.io.ObjectOutputStream;");
            mTarget.println("import java.io.Serializable;");
        }

        mTarget.println("import java.lang.invoke.MethodHandle;");
        mTarget.println("import java.lang.invoke.MethodHandles;");
        mTarget.println("import java.lang.invoke.MethodHandles.Lookup;");
        mTarget.println("import java.lang.invoke.MethodType;");

        // The following two imports are only used for
        // serializing/deserializing.
        if (mSerialFlag)
        {
            mTarget.println("import java.util.ArrayDeque;");
            mTarget.println("import java.util.Iterator;");
        }

        // Import the required FSMContext7 and State7 classes.
        mTarget.println("import statemap.FSMContext7;");
        mTarget.println("import statemap.State7;");
        mTarget.println("import statemap.TransitionHandle;");

        mTarget.println();

        return;
    } // end of outputImports(SmcFSM)

    /**
     * Writes the class declaration and opening brace.
     * @param smc FSM model.
     */
    private void outputClassDeclaration(final SmcFSM fsm)
    {
        String fsmClassName = fsm.getFsmClassName();

        // The context clas contains all the state classes as
        // inner classes, so generate the context first rather
        // than last.
        mTarget.print(mAccessLevel);
        mTarget.print(" class ");
        mTarget.print(fsmClassName);
        mTarget.println("");
        mTarget.println("    extends FSMContext7");

        if (mSerialFlag)
        {
            mTarget.println(
                "    implements Serializable");
        }

        mTarget.println("{");

        return;
    } // end of outputClassDeclaration(SmcFSM)

    /**
     * Writes the constructors and various FSM methods.
     * @param smc FSM model.
     */
    private void outputMethods(final SmcFSM fsm)
    {
        mTarget.println(
            "//---------------------------------------------------------------");
        mTarget.println("// Member methods.");
        mTarget.println("//");
        mTarget.println();

        // 4.1. Output the class constructors.
        outputConstructors(fsm);

        // 4.2. Output get/set methods.
        if (mSerialFlag || mReflectFlag)
        {
            outputGet(fsm.getContext());
        }
        outputSet(fsm);

        // 4.3. Output the required executeAction(MethodHandle)
        //      method override.
        outputExecuteAction();

        // Note: the enterStartState() method is now defined in FSMContext7
        // since State7 is now final.

        // 4.4. Output the transition interface methods.
        //      Note: these are not the method which implement a
        //            transition. The context class calls these
        //            methods to issue a transition.
        outputTransitionApi(fsm);

        // 4.5. Output the Java serialization writeObject and
        //      readObject methods, if serialization is on.
        if (mSerialFlag)
        {
            outputSerializeMethods(fsm);
        }

        // 4.6. Output the transition implementation methods
        //      for each map, state, and transition.
        for (SmcMap map: fsm.getMaps())
        {
            map.accept(this);
        }

        return;
    } // end of outputMethods(SmcFSM)

    /**
     * Outputs the three FSM class constructors:
     * <ol>
     *   <li>
     *     the default state constructor,
     *   </li>
     *   <li>
     *     the start state by ID constructor, and
     *   </li>
     *   <li>
     *     the start state by instance constructor. This
     *     constructor is used for de-serializing an FSM context.
     *   </li>
     * </ol>
     * @param smc the FSM model.
     */
    private void outputConstructors(final SmcFSM fsm)
    {
        final String context = fsm.getContext();
        final String fsmClassName = fsm.getFsmClassName();
        final String startState = fsm.getStartState();
        final int index = startState.indexOf("::");
        String javaState;

        mTarget.println(
            "    //-----------------------------------------------------------");
        mTarget.println("    // Constructors.");
        mTarget.println("    //");
        mTarget.println();

        // Generate the context class' constructor using the
        // configured start state.

        // The state name "map::state" must be changed to
        // a constant integer state identifier:
        // "map_state_STATE_ID".
        javaState =
            startState.substring(0, index) + // The map name.
            "_" +
            startState.substring(index + 2) + // The state name.
            STATE_ID_SUFFIX;

        mTarget.print("    ");
        mTarget.print(mAccessLevel);
        mTarget.print(" ");
        mTarget.print(fsmClassName);
        mTarget.print("(final ");
        mTarget.print(context);
        mTarget.println(" owner)");
        mTarget.println("    {");
        mTarget.print("        this (owner, sStates[");
        mTarget.print(javaState);
        mTarget.println("]);");
        mTarget.println("    }");
        mTarget.println();

        // Generate the second constructor which allows the
        // initial state to be dynamically set using the
        // state identifier.
        mTarget.print("    ");
        mTarget.print(mAccessLevel);
        mTarget.print(" ");
        mTarget.print(fsmClassName);
        mTarget.print("(final ");
        mTarget.print(context);
        mTarget.println(" owner, final int initStateId)");
        mTarget.println("    {");
        mTarget.print("        this (owner, sStates[initStateId]);");
        mTarget.println("    }");
        mTarget.println();


        // Generate the third constructor which allows the
        // initial state to be dynamically set. Overrides the
        // %start specifier.
        mTarget.print("    ");
        mTarget.print(mAccessLevel);
        mTarget.print(" ");
        mTarget.print(fsmClassName);
        mTarget.print("(final ");
        mTarget.print(context);
        mTarget.println(" owner, final State7 initState)");
        mTarget.println("    {");
        mTarget.println("        super (initState);");
        mTarget.println();
        mTarget.println("        ctxt = owner;");

        mTarget.println("    }");
        mTarget.println();

        mTarget.println("    //");
        mTarget.println("    // end of Constructors.");
        mTarget.println(
            "    //-----------------------------------------------------------");
        mTarget.println();

        return;
    } // end of outputConstructors(SmcFSM)

    /**
     * Writes the get emthods requested by the -serial and/or
     * -reflect flags.
     * @param context context class name.
     */
    private void outputGet(final String context)
    {
        mTarget.println(
            "    //-----------------------------------------------------------");
        mTarget.println("    // Get Methods.");
        mTarget.println("    //");
        mTarget.println();

        // NOTE: The getState() method is now implemented in
        // FSMContext7 since State7 is now final (i.e., has no
        // state sub-classes).

        // getOwner() method.
        mTarget.print("    public ");
        mTarget.print(context);
        mTarget.println(" getOwner()");
        mTarget.println("    {");
        mTarget.println("        return (ctxt);");
        mTarget.println("    }");
        mTarget.println();

        if (mReflectFlag)
        {
            // getState(int) method.
            mTarget.println(
                "    public static State7 getState(final int stateId)");
            mTarget.println(
                "        throws ArrayIndexOutOfBoundsException");
            mTarget.println("    {");
            mTarget.println(
                "        return (sStates[stateId]);");
            mTarget.println("    }");
            mTarget.println();

            // getStates() method.
            mTarget.println("    public static State7[] getStates()");
            mTarget.println("    {");
            mTarget.println("        return (sStates);");
            mTarget.println("    }");
            mTarget.println();

            // getTransitions() method.
            mTarget.println("    public static String[] getTransitions()");
            mTarget.println("    {");
            mTarget.println(
                "        return (TRANSITION_NAMES);");
            mTarget.println("    }");
            mTarget.println();
        }

        mTarget.println("    //");
        mTarget.println("    // end of Get Methods.");
        mTarget.println(
            "    //-----------------------------------------------------------");
        mTarget.println();

        return;
    } // end of outputGet(String)

    /**
     * Writes the owner set methods requested by the -serial
     * flag.
     * @param fsm the FSM model.
     */
    private void outputSet(final SmcFSM fsm)
    {
        if (mSerialFlag)
        {
            mTarget.println(
                "    //-----------------------------------------------------------");
            mTarget.println("    // Set Methods.");
            mTarget.println("    //");
            mTarget.println();

            // setOwner() method.
            mTarget.print("    public void setOwner(");
            mTarget.print(fsm.getContext());
            mTarget.println(" owner)");
            mTarget.println("    {");
            mTarget.println("        if (owner == null)");
            mTarget.println("        {");
            mTarget.println("            throw (new NullPointerException(\"null owner\"));");
            mTarget.println("        }");
            mTarget.println("        else");
            mTarget.println("        {");
            mTarget.println("            ctxt = owner;");
            mTarget.println("        }");
            mTarget.println();
            mTarget.println("        return;");
            mTarget.println("    }");
            mTarget.println();

            mTarget.println("    //");
            mTarget.println("    // end of Set Methods.");
            mTarget.println(
                "    //-----------------------------------------------------------");
            mTarget.println();
        }

        return;
    } // end of outputSet(SmcFSM)

    /**
     * Writes the required abstract method override
     * {@code executeAction}.
     */
    private void outputExecuteAction()
    {
        mTarget.println(
            "    //-----------------------------------------------------------");
        mTarget.println("    // FSMContext7 Abstract Method Override.");
        mTarget.println("    //");
        mTarget.println();

        mTarget.println("    @Override");
        mTarget.println("    protected void executeAction(final MethodHandle mh)");
        mTarget.println("    {");
        mTarget.println("        try");
        mTarget.println("        {");
        mTarget.println("            mh.invokeExact(this);");
        mTarget.println("        }");
        mTarget.println("        catch (Throwable tex)");
        mTarget.println("        {");
        mTarget.println("            if (mDebugFlag)");
        mTarget.println("            {");
        mTarget.println("                tex.printStackTrace(mDebugStream);");
        mTarget.println("            }");
        mTarget.println("        }");
        mTarget.println();
        mTarget.println("        return;");
        mTarget.println("    }");
        mTarget.println();

        mTarget.println("    //");
        mTarget.println("    // end of FSMContext7 Abstract Method Override.");
        mTarget.println(
            "    //-----------------------------------------------------------");
        mTarget.println();

        return;
    } // end of outputExecuteAction()

    /**
     * Writes the transition API methods. The owner context
     * instance calls these methods to issue transitions.
     * @param fsm the FSM model.
     */
    private void outputTransitionApi(final SmcFSM fsm)
    {
        String transName;
        List<SmcParameter> params;
        Iterator<SmcParameter> pit;
        String separator;

        mTarget.println(
            "    //-----------------------------------------------------------");
        mTarget.println("    // Transitions.");
        mTarget.println("    //");
        mTarget.println();

        // Generate the default transition methods.
        for (SmcTransition trans: fsm.getTransitions())
        {
            transName = trans.getName();

            // Handle the default transition separately.
            if (transName.equals(DEFAULT_NAME) == false)
            {
                mTarget.print("    public ");

                // If the -sync flag was specified, then output
                // the "synchronized" keyword.
                if (mSyncFlag)
                {
                    mTarget.print("synchronized ");
                }

                mTarget.print("void ");
                mTarget.print(transName);
                mTarget.print("(");

                params = trans.getParameters();
                for (pit = params.iterator(), separator = "";
                     pit.hasNext();
                     separator = ", ")
                {
                    mTarget.print(separator);
                    (pit.next()).accept(this);
                }
                mTarget.println(")");
                mTarget.println("    {");

                // Save away the transition name in case it is
                // need in an UndefinedTransitionException.
                mTarget.print("        mTransition = \"");
                mTarget.print(transName);
                mTarget.println("\";");

                mTarget.println("        try");
                mTarget.println("        {");
                mTarget.println(
                    "            final TransitionHandle th =");
                mTarget.print(
                    "                getState().transition(");
                mTarget.print(transName);
                mTarget.print(trans.getIdentifier());
                mTarget.print(TRANSITION_ID_SUFFIX);
                mTarget.println(");");
                mTarget.println();

                // If the transition takes no parameters, then it
                // has the same signature as a default
                // transition.
                if (params.isEmpty())
                {
                    mTarget.println(
                        "            (th.handle()).invokeExact(this);");
                }
                // Otherwise, there is a need to distinguish
                // between actual and default transitions.
                else
                {
                    mTarget.println(
                        "            if (th.isDefault())");
                    mTarget.println(
                        "            {");
                    mTarget.println(
                        "                (th.handle()).invokeExact(this);");
                    mTarget.println(
                        "            }");
                    mTarget.println(
                        "            else");
                    mTarget.println(
                        "            {");
                    mTarget.print(
                        "                (th.handle()).invokeExact(this");

                    for (pit = params.iterator();
                         pit.hasNext();
                        )
                    {
                        mTarget.print(", ");
                        mTarget.print((pit.next()).getName());
                    }
                    mTarget.println(");");
                    mTarget.println(
                        "            }");
                }

                mTarget.println("        }");
                mTarget.println("        catch (Throwable tex)");
                mTarget.println("        {");
                mTarget.println(
                    "            if (mDebugFlag)");
                mTarget.println("            {");
                mTarget.println(
                    "                tex.printStackTrace(mDebugStream);");
                mTarget.println("            }");
                mTarget.println("        }");

                // Clear the in-progress transition name before
                // returning.
                mTarget.println("        mTransition = \"\";");

                mTarget.println("        return;");
                mTarget.println("    }");
                mTarget.println();
            }
        }

        mTarget.println("    //");
        mTarget.println("    // end of Transitions.");
        mTarget.println(
            "    //-----------------------------------------------------------");
        mTarget.println();

        return;
    } // end of outputTransitionApi(SmcFSM)

    /**
     * Writes the writeObject() and readObject() methods.
     * @param fsm the FSM model.
     */
    private void outputSerializeMethods(final SmcFSM fsm)
    {
        mTarget.println(
            "    //-----------------------------------------------------------");
        mTarget.println("    // Serialization Methods.");
        mTarget.println("    //");
        mTarget.println();

        mTarget.println(
            "    private void writeObject(final ObjectOutputStream ostream)");
        mTarget.println("        throws IOException");
        mTarget.println("    {");
        mTarget.println(
            "        final int size =");
        mTarget.println(
            "            (mStateStack == null ? 0 : mStateStack.size());");
        mTarget.println("        int i;");
        mTarget.println();
        mTarget.println(
            "        ostream.writeInt(size);");
        mTarget.println();
        mTarget.println("        if (size > 0)");
        mTarget.println("        {");
        mTarget.println(
            "            final Iterator<State7> sit =");
        mTarget.println(
            "                mStateStack.iterator();");
        mTarget.println();
        mTarget.println(
            "            while (sit.hasNext())");
        mTarget.println("            {");
        mTarget.println(
            "                ostream.writeInt((sit.next()).getId());");
        mTarget.println("            }");
        mTarget.println("        }");
        mTarget.println();
        mTarget.println(
            "        ostream.writeInt(mState.getId());");
        mTarget.println();
        mTarget.println("        return;");
        mTarget.println("    }");
        mTarget.println();
        mTarget.println("    private void readObject(final ObjectInputStream istream)");
        mTarget.println(
            "        throws IOException");
        mTarget.println("    {");
        mTarget.println(
            "        final int size = istream.readInt();");
        mTarget.println();
        mTarget.println("        if (size == 0)");
        mTarget.println("        {");
        mTarget.println("            mStateStack = null;");
        mTarget.println("        }");
        mTarget.println("        else");
        mTarget.println("        {");
        mTarget.println("            int i;");
        mTarget.println();
        mTarget.println("            mStateStack = new ArrayDeque<>();");
        mTarget.println();
        mTarget.println(
            "            for (i = 0; i < size; ++i)");
        mTarget.println("            {");
        mTarget.println(
            "                mStateStack.addLast(sStates[istream.readInt()]);");
        mTarget.println("            }");
        mTarget.println("        }");
        mTarget.println();
        mTarget.println(
            "        mState = sStates[istream.readInt()];");
        mTarget.println();
        mTarget.println("        return;");
        mTarget.println("    }");
        mTarget.println();

        mTarget.println("    //");
        mTarget.println("    // end of Serialization Methods.");
        mTarget.println(
            "    //-----------------------------------------------------------");
        mTarget.println();

        return;
    } // end of outputSerializeMethods(SmcFSM)

    /**
     * Outputs the instance and class data members.
     * @param fsm the FSM model.
     */
    private void outputData(final SmcFSM fsm)
    {
        final String context = fsm.getContext();

        // 5.1. Declare member data.
        mTarget.println(
            "//---------------------------------------------------------------");
        mTarget.println("// Member data.");
        mTarget.println("//");
        mTarget.println();
        mTarget.print("    transient private ");
        mTarget.print(context);
        mTarget.println(" ctxt;");

        // 5.2. Declare the class constants.
        mTarget.println();
        mTarget.println(
            "    //-----------------------------------------------------------");
        mTarget.println("    // Constants.");
        mTarget.println("    //");
        mTarget.println();
        mTarget.println(
            "    private static final long serialVersionUID = 1L;");
        mTarget.println();

        // 5.2.1. Output the state identifiers.
        outputStateIds(fsm);

        // 5.2.2. Output the transition identifiers.
        outputTransitionIds(fsm);

        // 5.2.3. Output the transition method signatures.
        outputTransitionSignatures(fsm);

        // 5.2.4. Output the map, state and transition name arrays.
        outputNames(fsm);

        // 5.2.5. Output the states array.
        //        Note: the array is filled in the class init.
        mTarget.println(
            "    private static final State7[] sStates = new State7[STATE_COUNT];");
        mTarget.println();

        // 5.2.6. Output the class static initialization block.
        outputClassInit(fsm);

        // 5.2.7 If -reflection is set, then output the map
        //       classes containing the state instances.
        if (mReflectFlag)
        {
            outputMapClasses(fsm);
        }

        return;
    } // end of outputData(SmcFSM)

    /**
     * Writes the state identifiers as constant integer values.
     * @param fsm the FSM model.
     */
    private void outputStateIds(final SmcFSM fsm)
    {
        final List<SmcMap> maps = fsm.getMaps();
        String mapName;
        String stateIdName;
        int stateId = 0;

        for (SmcMap map : maps)
        {
            mapName = map.getName();

            for (SmcState state : map.getStates())
            {
                stateIdName =
                    String.format(
                        "%s_%s%s",
                        mapName,
                        state.getClassName(),
                        STATE_ID_SUFFIX);

                mTarget.print("    ");
                mTarget.print(mAccessLevel);
                mTarget.print(" static final int ");
                mTarget.print(stateIdName);
                mTarget.print(" = ");
                mTarget.print(stateId);
                mTarget.println(";");

                ++stateId;
            }

            mTarget.println();
        }

        // Now output the total number of states.
        mTarget.print(
            "    private static final int STATE_COUNT = ");
        mTarget.print(stateId);
        mTarget.println(";");
        mTarget.println();

        return;
    } // end of outputStateIds(SmcFSM)

    /**
     * Writes the transition identifier constants.
     * @param fsm the FSM model.
     */
    private void outputTransitionIds(final SmcFSM fsm)
    {
        String transName;
        String transIdName;
        int transId = 1;

        for (SmcTransition trans : fsm.getTransitions())
        {
            transName = trans.getName();

            // The default transition ID is already set to zero
            // in FSMContext7.
            if (!transName.equals(DEFAULT_NAME))
            {
                transIdName =
                    String.format(
                        "%s%d%s",
                        transName,
                        trans.getIdentifier(),
                        TRANSITION_ID_SUFFIX);

                mTarget.print("    private static final int ");
                mTarget.print(transIdName);
                mTarget.print(" = ");
                mTarget.print(transId);
                mTarget.println(";");

                ++transId;
            }
        }

        // Now output the total number of transitions.
        mTarget.println();
        mTarget.print(
            "    private static final int TRANSITION_COUNT = ");
        mTarget.print(transId);
        mTarget.println(";");
        mTarget.println();

        return;
    } // end of outputTransitionIds(SmcFSM)

    /**
     * Outputs an array containing each transition's method
     * signature.
     * @param fsm FSM model.
     */
    private void outputTransitionSignatures(final SmcFSM fsm)
    {
        String transName;
        List<SmcParameter> params;

        mTarget.println(
            "    private static final MethodType[] TRANSITION_TYPES =");
        mTarget.println("    {");

        // The first transition (index 0) is the Default
        // transition - which returns void and has no parameters.
        mTarget.print("        NO_ARGS_TYPE");

        for (SmcTransition trans : fsm.getTransitions())
        {
            transName = trans.getName();

            // Skip the default state.
            if (!transName.equals(DEFAULT_NAME))
            {
                // Output the separator between the previous
                // signature and this one.
                mTarget.println(",");

                params = trans.getParameters();

                // If the transition has no parameters, then
                // output NO_ARGS_TYPE.
                if (params.isEmpty())
                {
                    mTarget.print("        NO_ARGS_TYPE");
                }
                else
                {
                    // The first argument is the method return
                    // type.
                    mTarget.print(
                        "        MethodType.methodType(void.class");

                    // Output the method parameters.
                    for (SmcParameter param : params)
                    {
                        mTarget.print(", ");
                        mTarget.print(getJavaType(param.getType()));
                        mTarget.print(".class");
                    }

                    mTarget.print(")");
                }
            }
        }

        mTarget.println();
        mTarget.println("    };");
        mTarget.println();

        return;
    } // end of outputTransitionSignatures(SmcFSM)

    /**
     * Output the map, state, and transition name arrays.
     * @param fsm the FSM model.
     */
    private void outputNames(final SmcFSM fsm)
    {
        // 5.2.4.1. Output the map names.
        outputMapNames(fsm);

        // 5.2.4.2. Output the state names.
        outputStateNames(fsm);

        // 5.24.3. Output the state transition names.
        outputStateTransitions(fsm);

        // 5.2.4.4. Output the transition names.
        outputTransitionNames(fsm);

        return;
    } // end of outputNames(SmcFSM)

    /**
     * Writes the {@code MAP_NAMES} string array.
     * @param fsm the FSM model.
     */
    private void outputMapNames(final SmcFSM fsm)
    {
        String sep = "";

        mTarget.println(
            "    private static final String[] MAP_NAMES =");
        mTarget.print("    {");

        for (SmcMap map : fsm.getMaps())
        {
            mTarget.println(sep);
            mTarget.print("        \"");
            mTarget.print(map.getName());
            mTarget.print("\"");

            sep = ",";
        }

        mTarget.println();
        mTarget.println("    };");
        mTarget.println();

        return;
    } // end of outputMapNames(SmcFSM)

    /**
     * Writes a two-dimensional array containing the state names
     * for each map.
     * @param fsm the FSM model.
     */
    private void outputStateNames(final SmcFSM fsm)
    {
        String sep0 = "";
        String sep1 = "";

        mTarget.println(
            "    private static final String[][] STATE_NAMES =");
        mTarget.print("    {");

        for (SmcMap map : fsm.getMaps())
        {
            mTarget.println(sep0);
            mTarget.println("        new String[]");
            mTarget.print("        {");

            for (SmcState state : map.getStates())
            {
                mTarget.println(sep1);
                mTarget.print("            \"");
                mTarget.print(state.getInstanceName());
                mTarget.print("\"");

                sep1 = ",";
            }

            mTarget.println();
            mTarget.print("        }");

            sep0 = ",\n";
            sep1 = "";
        }

        mTarget.println();
        mTarget.println("    };");
        mTarget.println();

        return;
    } // end of outputStateNames(SmcFSM)

    /**
     * Writes a two-dimensional array containing the transition
     * names used by each state.
     * @param fsm the FSM model.
     */
    private void outputStateTransitions(final SmcFSM fsm)
    {
        String sep0 = "";
        String sep1 = "";

        mTarget.println(
            "    private static String[][] STATE_TRANSITIONS =");
        mTarget.print("    {");

        for (SmcMap map : fsm.getMaps())
        {
            for (SmcState state : map.getStates())
            {
                mTarget.println(sep0);
                mTarget.println("        new String[]");
                mTarget.print("        {");

                for (SmcTransition trans : state.getTransitions())
                {
                    mTarget.println(sep1);
                    mTarget.print("            \"");
                    mTarget.print(trans.getName());
                    mTarget.print("\"");

                    sep1 = ",";
                }

                mTarget.println();
                mTarget.print("        }");

                sep0 = ",\n";
                sep1 = "";
            }
        }

        mTarget.println();
        mTarget.println("    };");
        mTarget.println();

        return;
    } // end of outputStateTransitions(SmcFSM)

    /**
     * Writes an array containing all transition names,
     * independent of map and state.
     * @param fsm the FSM model.
     */
    private void outputTransitionNames(final SmcFSM fsm)
    {
        final String sep = ",";
        String transName;

        mTarget.println(
            "    private static final String[] TRANSITION_NAMES =");
        mTarget.println("    {");

        // Output the default name separately and first.
        mTarget.print("        \"");
        mTarget.print(DEFAULT_NAME);
        mTarget.print("\"");

        for (SmcTransition trans : fsm.getTransitions())
        {
            transName = trans.getName();

            // Skip the default transition since it is already
            // output.
            if (!DEFAULT_NAME.equals(transName))
            {
                mTarget.println(sep);
                mTarget.print("        \"");
                mTarget.print(transName);
                mTarget.print("\"");
            }
        }

        mTarget.println();
        mTarget.println("    };");
        mTarget.println();

        return;
    } // end of outputTransitionNames(SmcFSM)

    /**
     * Writes the class initialization block which fills in the
     * States array with State instances.
     * @param fsm the FSM model.
     */
    private void outputClassInit(final SmcFSM fsm)
    {
        mTarget.println("    static");
        mTarget.println("    {");

        // 5.2.6.1. Output the local variables used in generating
        //         the State7 instances.
        outputLocalVars(fsm);

        // 5.2.6.2. Output the map for-loop.
        mTarget.println(
            "        for (mapIndex = 0; mapIndex < mapSize; ++mapIndex)");
        mTarget.println("        {");
        mTarget.println(
            "            mapName = MAP_NAMES[mapIndex];");
        mTarget.println(
            "            stateSize = STATE_NAMES[mapIndex].length;");
        mTarget.println();

        // 5.2.6.3. Output the state for-loop.
        outputClassInitStateLoop();

        // 5.2.6.4. Output the map for-loop and class init block
        //          closing braces.
        mTarget.println("        }");
        mTarget.println("    }");

        return;
    } // end of outputClassInit(SmcFSM)

    /**
     * Writes the local variable declarations used in the class
     * initialization block.
     * @param fsm the FSM model.
     */
    private void outputLocalVars(final SmcFSM fsm)
    {
        mTarget.println(
            "        final Lookup lookup = MethodHandles.lookup();");
        mTarget.print("        final Class<?> clazz = ");
        mTarget.print(fsm.getFsmClassName());
        mTarget.println(".class;");
        mTarget.println(
            "        final int mapSize = MAP_NAMES.length;");
        mTarget.println("        int stateSize;");
        mTarget.println("        int mapIndex;");
        mTarget.println("        int stateIndex;");
        mTarget.println("        int transIndex;");
        mTarget.println("        int stateId = 0;");
        mTarget.println("        String mapName;");
        mTarget.println("        String stateName;");
        mTarget.println("        String transName;");
        mTarget.println("        String methodName;");
        mTarget.println("        MethodType transType;");
        mTarget.println("        MethodHandle entryHandle;");
        mTarget.println("        MethodHandle exitHandle;");
        mTarget.println("        TransitionHandle[] transitions;");
        mTarget.println();

        return;
    } // end of outputLocalVars(SmcFSM)

    /**
     * Writes the state instance creation loop.
     */
    private void outputClassInitStateLoop()
    {
        // 5.2.6.3.1. Output the state for-loop.
        mTarget.println(
            "            for (stateIndex = 0; stateIndex < stateSize; ++stateIndex, ++stateId)");
        mTarget.println("            {");
        mTarget.println(
            "                stateName = STATE_NAMES[mapIndex][stateIndex];");
        mTarget.println(
            "                transitions = new TransitionHandle[TRANSITION_COUNT];");
        mTarget.println();
        mTarget.println(
            "                methodName = String.format(ENTRY_NAME, mapName, stateName);");
        mTarget.println(
            "                entryHandle = lookupMethod(lookup, clazz, methodName, NO_ARGS_TYPE);");
        mTarget.println(
            "                methodName = String.format(EXIT_NAME, mapName, stateName);");
        mTarget.println(
            "                exitHandle = lookupMethod(lookup, clazz, methodName, NO_ARGS_TYPE);");
        mTarget.println();

        // 5.2.6.3.2. Output the transition table initialization.
        outputClassInitTransitions();

        // 5.2.6.3.3. Output the state instantiation.
        mTarget.println("                sStates[stateId] =");
        mTarget.println("                    new State7(");
        mTarget.println(
            "                        String.format(STATE_NAME_FORMAT, mapName, stateName),");
        mTarget.println("                        stateId,");
        mTarget.println("                        entryHandle,");
        mTarget.println("                        exitHandle,");
        mTarget.println("                        transitions,");
        mTarget.println("                        STATE_TRANSITIONS[stateId]);");

        mTarget.println("            }");

        return;
    } // end of outputClassInitStateLoop()

    /**
     * Writes the transition table initialization.
     */
    private void outputClassInitTransitions()
    {
        mTarget.println(
            "                for (transIndex = 1; transIndex < TRANSITION_COUNT; ++transIndex)");
        mTarget.println("                {");
        mTarget.println(
            "                    transName = TRANSITION_NAMES[transIndex];");
        mTarget.println(
            "                    transType = TRANSITION_TYPES[transIndex];");
        mTarget.println(
            "                    transitions[transIndex] =");
        mTarget.println(
            "                        lookupTransition(lookup, clazz, mapName, stateName, transName, transType);");
        mTarget.println("                }");
        mTarget.println();

        return;
    } // end of outputClassInitTransitions()

    /**
     * Writes the map classes containing a public static field
     * named for each map's states.
     * @param fsm the FSM model.
     */
    private void outputMapClasses(final SmcFSM fsm)
    {
        for (SmcMap map : fsm.getMaps())
        {
            mTarget.println();
            outputMapClass(map);
        }

        return;
    } // end of outputMapClasses(SmcFSM)

    /**
     * Writes a particular map class.
     * @param map the map model.
     */
    private void outputMapClass(final SmcMap map)
    {
        final String mapName = map.getName();
        String stateName;

        mTarget.print("    public static final class ");
        mTarget.println(mapName);
        mTarget.println("    {");

        // Private default constructor to prevent instantiation.
        mTarget.print("        private ");
        mTarget.print(mapName);
        mTarget.println("()");
        mTarget.println("        {}");
        mTarget.println();

        // Map state instances.
        for (SmcState state : map.getStates())
        {
            stateName = state.getClassName();

            mTarget.print("        public static final State7 ");
            mTarget.print(stateName);
            mTarget.print(" = sStates[");
            mTarget.format("%s_%s%s",
                           mapName,
                           stateName,
                           STATE_ID_SUFFIX);
            mTarget.println("];");
        }

        // End of class.
        mTarget.println("    }");

        return;
    } // end of outputMapClass(SmcMap)

    /**
     * Writes the class closing brace and ending read-only
     * comment block.
     */
    private void outputFooter()
    {
        mTarget.println("}");

        mTarget.println();
        mTarget.println("/*");
        mTarget.println(" * Local variables:");
        mTarget.println(" *  buffer-read-only: t");
        mTarget.println(" * End:");
        mTarget.println(" */");

        return;
    } // end of outputFooter()

    /**
     * Writes either the state entry or exit actions.
     * @param mapName the state resides in this map.
     * @param stateName the actions belong to this state.
     * @param nameFormat the method name format.
     * @param actions either entry or exit actions.
     */
    private void outputStateActions(final String mapName,
                                    final String stateName,
                                    final String nameFormat,
                                    final List<SmcAction> actions)
    {
        final String methodName =
            String.format(nameFormat, mapName, stateName);

        mTarget.println();
        mTarget.print("    private void ");
        mTarget.print(methodName);
        mTarget.println("()");
        mTarget.println("    {");

        for (SmcAction action : actions)
        {
            action.accept(this);
        }

        mTarget.println("    }");

        return;
    } // end of outputStateActions(String, String, String, List<>)

    /**
     * Writes the transitions guarded implementations. Sets the
     * code indentation appropriately based on the number of
     * guards and conditions.
     * @param transition output this transitions guarded
     * implementations.
     * @param mapName the transition resides in this map.
     */
    private void outputTransitionGuards(final SmcTransition transition,
                                        final String mapName)
    {
        final List<SmcGuard> guards = transition.getGuards();
        final Iterator<SmcGuard> git = guards.iterator();
        SmcGuard guard;
        SmcGuard nullGuard = null;

        mGuardIndex = 0;
        mGuardCount = guards.size();

        // If there is either more than one guard or one guard
        // with a condition, then indent the guard code one more
        // time because the guard code is inside an if-then body.
        mIndent = "        ";
        if (mGuardCount > 1 ||
            (mGuardCount == 1 &&
             ((guards.get(0)).getCondition()).isEmpty() == false))
        {
            mIndent += "    ";
        }
        // Else there are either no guards or one guard with no
        // condition.

        // 4.1. Output the "stateId" local variable in case it is
        //      needed.
        mTarget.println(
            "        final int stateId = mState.getId();");
        mTarget.println();

        // 4.2. Output each guard, tracking if there are any with
        //      no condition.
        while (git.hasNext())
        {
            guard = git.next();

            // Output the no condition guard *after* all other
            // guarded transitions.
            if ((guard.getCondition()).isEmpty())
            {
                nullGuard = guard;
            }
            else
            {
                guard.accept(this);
                ++mGuardIndex;
            }
        }

        // Is there an explicitly defined unguarded transition?
        if (nullGuard != null)
        {
            // Does this guard have any actions or is this guard
            // *not* an internal loopback transition?
            if (nullGuard.hasActions() ||
                !(nullGuard.getEndState()).equals(SmcElement.NIL_STATE) ||
                nullGuard.getTransType() == TransType.TRANS_PUSH ||
                nullGuard.getTransType() == TransType.TRANS_POP)
            {
                // Need to output either the action and/or the
                // next state, so output the guard.
                nullGuard.accept(this);
            }

            mTarget.println();
        }
        // If all guards have a condition, then create a final
        // "else" clause which passes control to the default
        // state implementation of this transition. Pass all
        // arguments into the default transition.
        else if (mGuardIndex > 0)
        {
            outputElseGuard(transition, mapName);
        }

        return;
    } // end of outputTransitionGuards(SmcTransition, ...)

    /**
     * If a state defines a transition using guard conditions
     * only, then SMC must generate the final "else" (unguarded)
     * transition for the programmer. This else body calls:
     * <ol>
     *   <li>
     *     the default state's definition of this transition, or
     *   </li>
     *   <li>
     *     the current state's default transition, or
     *   </li>
     *   <li>
     *     the default state's default transition.
     *   </li>
     * </ol>
     *  the
     * current
     * @param transition default unguarded "else" clause is for
     * this transition.
     * @param mapName the transition resides in this map.
     */
    private void outputElseGuard(final SmcTransition transition,
                                 final String mapName)
    {
        final SmcState currState = transition.getState();
        final SmcState defaultState =
            (currState.getMap()).getDefaultState();
        List<SmcParameter> params = transition.getParameters();
        String methodName;
        String sep = "";

        // Does the default state define this transition?
        if (defaultState.findTransition(
                transition.getName(), params) != null)
        {
            // Yes. Call that transition method.
            methodName = mapName +
                         "_" +
                         DEFAULT_NAME +
                         "_" +
                         transition.getName();
        }
        // No, the default state does not define that transition.
        // Does the current state have a default transition?
        else if (currState.findTransition(
                     DEFAULT_NAME, DEFAULT_PARAMETERS) != null)
        {
            // Yes, use the current state default transition.
            methodName = mapName +
                         "_" +
                         currState.getClassName() +
                         "_" +
                         DEFAULT_NAME;
            params = DEFAULT_PARAMETERS;
        }
        // No, the current state does not have a default
        // transition.
        // Does the default state have a default transition?
        else if (defaultState.findTransition(
                     DEFAULT_NAME, DEFAULT_PARAMETERS) != null)
        {
            // Yes, use the default state default transition.
            methodName = mapName +
                         "_" +
                         DEFAULT_NAME +
                         "_" +
                         DEFAULT_NAME;
            params = DEFAULT_PARAMETERS;
        }
        // No, the default state does not hava a default
        // transition.
        // Call the system default transition which throws a
        // TransitionUndefinedException.
        else
        {
            methodName = SYSTEM_DEFAULT;
            params = DEFAULT_PARAMETERS;
        }

        mTarget.println();
        mTarget.println("        else");
        mTarget.println("        {");
        mTarget.print("            ");
        mTarget.print(methodName);
        mTarget.print("(");

        for (SmcParameter param : params)
        {
            mTarget.print(sep);
            mTarget.print(param.getName());

            sep = ", ";
        }

        mTarget.println(");");
        mTarget.println("        }");
        mTarget.println();

        return;
    } // end of outputElseGuard(SmcTransition, String)

    /**
     * Writes the guard condition, using an "if", "else if",
     * "else", or nothing depending on the number of guards,
     * this guard's index, and whether this guard has a condition
     * or not.
     * @param condition guard condition code.
     */
    private void outputGuardCondition(final String condition)
    {
        // The guard code generation is a bit tricky. The first
        // question is how many guards are there? If there are
        // more than one, then we will need to generate the
        // proper "if-then-else" code.
        if (mGuardCount > 1)
        {
            // Is this the first guard?
            if (mGuardIndex == 0 && condition.length() > 0)
            {
                // Yes, this is the first. This means an "if"
                // should be used.
                mTarget.print("        if (");
                mTarget.print(condition);
                mTarget.println(")");
            }
            else if (condition.length() > 0)
            {
                // No, this is not the first transition but it
                // does have a condition. Use an "else if".
                mTarget.println();
                mTarget.print("        else if (");
                mTarget.print(condition);
                mTarget.println(")");
            }
            else
            {
                // This is not the first transition and it has
                // no condition.
                mTarget.println();
                mTarget.println("        else");
            }

            mTarget.println("        {");
        }
        // There is only one guard. Does this guard have a
        // condition?
        else if (condition.length() > 0)
        {
            // Yes there is a condition.
            mTarget.print("        if (");
            mTarget.print(condition);
            mTarget.println(")");
            mTarget.println("        {");
        }

        return;
    } // end of outputGuardCondition(String)

    /**
     * Outputs the state exit code, surrounded by option debug
     * logging statements.
     * @param stateName exiting this state.
     */
    private void outputStateExit(final String mapName,
                                 final String stateName)
    {
        if (mDebugLevel >= DEBUG_LEVEL_0)
        {
            mTarget.print(mIndent);
            mTarget.println("if (mDebugFlag)");
            mTarget.print(mIndent);
            mTarget.println("{");
            mTarget.print(mIndent);
            mTarget.print(
                "    mDebugStream.println(\"LEAVING STATE   : ");
            mTarget.print(mapName);
            mTarget.print('.');
            mTarget.print(stateName);
            mTarget.println("\");");
            mTarget.print(mIndent);
            mTarget.println("}");
            mTarget.println();
        }

        if (mDebugLevel >= DEBUG_LEVEL_1)
        {
            String sep;

            mTarget.print(mIndent);
            mTarget.println("if (mDebugFlag)");
            mTarget.print(mIndent);
            mTarget.println("{");
            mTarget.print(mIndent);
            mTarget.print(
                "    mDebugStream.println(\"BEFORE EXIT     : ");
            mTarget.print(stateName);
            mTarget.println(".exit()\");");
            mTarget.print(mIndent);
            mTarget.println("}");
            mTarget.println();
        }

        mTarget.print(mIndent);
        mTarget.println("exitState();");
        mTarget.println();

        if (mDebugLevel >= DEBUG_LEVEL_1)
        {
            mTarget.print(mIndent);
            mTarget.println("if (mDebugFlag)");
            mTarget.print(mIndent);
            mTarget.println("{");
            mTarget.print(mIndent);
            mTarget.print(
                "    mDebugStream.println(\"AFTER EXIT      : ");
            mTarget.print(stateName);
            mTarget.println(".exit()\");");
            mTarget.print(mIndent);
            mTarget.println("}");
            mTarget.println();
        }

        return;
    } // end of outputStateExit(String, String)

    /**
     * Writes the guard actions, adding the transition enter and
     * exit debug logging.
     * @param guard output this guard's body.
     * @param transition the guard is part of this transition.
     * @param mapName the transition is in this map.
     * @param stateName the transition is in this state.
     */
    private void outputGuardBody(final SmcGuard guard,
                                 final SmcTransition transition,
                                 final String mapName,
                                 final String stateName)
    {
        // 3.1. Output the transition enter logging.
        outputTransitionEnter(transition, mapName, stateName);

        // 3.2. Output the guard actions.
        outputGuardActions(guard);

        // 3.3. Output the transition exit logging.
        outputTransitionExit(transition, mapName, stateName);

        return;
    } // end of outputGuardBody()

    /**
     * Writes the transition enter logging, if debug logging is
     * enabled.
     * @param trans entering this transition.
     * @param mapName the transition is in this map.
     * @param stateName the transition is in this state.
     */
    private void outputTransitionEnter(final SmcTransition trans,
                                       final String mapName,
                                       final String stateName)
    {
        // Output transition to debug stream.
        if (mDebugLevel >= DEBUG_LEVEL_0)
        {
            final List<SmcParameter> parameters =
                trans.getParameters();
            final Iterator<SmcParameter> pit =
                parameters.iterator();
            String sep = "";

            mTarget.print(mIndent);
            mTarget.println(
                "    if (mDebugFlag)");
            mTarget.print(mIndent);
            mTarget.println("    {");
            mTarget.print(mIndent);
            mTarget.print(
                "        mDebugStream.println(\"ENTER TRANSITION: ");
            mTarget.print(mapName);
            mTarget.print('.');
            mTarget.print(stateName);
            mTarget.print('.');
            mTarget.print(trans.getName());

            mTarget.print('(');
            while (pit.hasNext())
            {
                mTarget.print(sep);
                (pit.next()).accept(this);

                sep = ", ";
            }
            mTarget.print(')');

            mTarget.println("\");");
            mTarget.print(mIndent);
            mTarget.println("    }");
            mTarget.println();
        }

        return;
    } // end of outputTransitionEnter(SmcTransition,String,String)

    /**
     * Writes the transition guard actions. If the guard has no
     * actions, then writes "// No actions.".
     */
    private void outputGuardActions(final SmcGuard guard)
    {
        final List<SmcAction> actions = guard.getActions();
        final boolean hasActions = !(actions.isEmpty());

        if (hasActions == false)
        {
            if ((guard.getCondition()).isEmpty() == false)
            {
                mTarget.print(mIndent);
                mTarget.println("    // No actions.");
            }
        }
        else
        {
            // Now that we are in the transition, clear the
            // current state.
            mTarget.print(mIndent);
            mTarget.println("    clearState();");

            for (SmcAction action: actions)
            {
                action.accept(this);
            }
        }

        return;
    } // end of outputGuardActions()

    /**
     * Writes the transition enter logging, if debug logging is
     * enabled.
     * @param trans entering this transition.
     * @param mapName the transition is in this map.
     * @param stateName the transition is in this state.
     */
    private void outputTransitionExit(final SmcTransition trans,
                                      final String mapName,
                                      final String stateName)
    {
        // Output transition to debug stream.
        if (mDebugLevel >= DEBUG_LEVEL_0)
        {
            final List<SmcParameter> parameters =
                trans.getParameters();
            final Iterator<SmcParameter> pit =
                parameters.iterator();
            String sep = "";

            mTarget.println();
            mTarget.print(mIndent);
            mTarget.println(
                "    if (mDebugFlag)");
            mTarget.print(mIndent);
            mTarget.println("    {");
            mTarget.print(mIndent);
            mTarget.print(
                "        mDebugStream.println(\"EXIT TRANSITION : ");
            mTarget.print(mapName);
            mTarget.print('.');
            mTarget.print(stateName);
            mTarget.print('.');
            mTarget.print(trans.getName());

            mTarget.print('(');
            while (pit.hasNext())
            {
                mTarget.print(sep);
                (pit.next()).accept(this);

                sep = ", ";
            }
            mTarget.print(')');

            mTarget.println("\");");
            mTarget.print(mIndent);
            mTarget.println("    }");
        }

        return;
    } // end of outputTransitionExit(SmcTransition,String,String)

    /**
     * Outputs the state exit code, surrounded by option debug
     * logging statements.
     * @param mapName the state resides in this map.
     * @param stateName exiting this state.
     */
    private void outputStateEnter(final String mapName,
                                  final String stateName)
    {
        if (mDebugLevel >= DEBUG_LEVEL_1)
        {
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.println("if (mDebugFlag)");
            mTarget.print(mIndent);
            mTarget.println("{");
            mTarget.print(mIndent);
            mTarget.print("    mDebugStream.println(\"BEFORE ENTRY    : ");
            mTarget.print(stateName);
            mTarget.println(".entry()\");");
            mTarget.print(mIndent);
            mTarget.println("}");
            mTarget.println();
        }

        mTarget.print(mIndent);
        mTarget.println("enterState();");

        if (mDebugLevel >= DEBUG_LEVEL_1)
        {
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.println(
                "if (mDebugFlag)");
            mTarget.print(mIndent);
            mTarget.println("{");
            mTarget.print(mIndent);
            mTarget.print(
                "    mDebugStream.println(\"AFTER ENTRY     : ");
            mTarget.print(stateName);
            mTarget.println(".entry()\");");
            mTarget.print(mIndent);
            mTarget.println("}");
        }

        return;
    } // end of outputStateEnter()

    /**
     * Returns the actual Java type name in the given string.
     * {@code s} may contain addition attributes besides the
     * Java type name (e.g. {@code final}). The Java type name
     * must be the last name in the string and the names are
     * separated by a space, search for the last blank in the
     * string and use the substring in front of the blank.
     * <p>
     * If the Java type name is a generic
     * (e.g. {@code List<Integer>}), then the generic is removed
     * from the returned type name.
     * </p>
     * @param s extract the Java type name from this string.
     * @return a Java type name.
     */
    private String getJavaType(final String s)
    {
        int index = s.lastIndexOf(' ');
        String retval = s;

        if (index >= 0)
        {
            retval = s.substring(index + 1);
        }

        // Is this a generic Java type.
        index = retval.lastIndexOf('<');
        if (index > 0)
        {
            // Yes. Remove the generic portion.
            retval = retval.substring(0, index);
        }

        return (retval);
    } // end of getJavaType(String)
} // end of class SmcJava7Generator
