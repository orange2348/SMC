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
// Copyright (C) 2005, 2008 - 2009, 2019. Charles W. Rapp.
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
// $Id: SmcCSharpGenerator.java,v 1.14 2015/08/02 19:44:36 cwrapp Exp $
//
// CHANGE LOG
// (See the bottom of this file.)
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
 * Visits the abstract syntax tree, emitting C# code to an output
 * stream.
 * @see SmcElement
 * @see SmcCodeGenerator
 * @see SmcVisitor
 * @see SmcOptions
 *
 * @author <a href="mailto:rapp@acm.org">Charles Rapp</a>
 */

public final class SmcCSharpGenerator
    extends SmcCodeGenerator
{
//---------------------------------------------------------------
// Member data
//

//---------------------------------------------------------------
// Member methods
//

    //-----------------------------------------------------------
    // Constructors.
    //

    /**
     * Creates a C# code generator for the given options.
     * @param options The target code generator options.
     */
    public SmcCSharpGenerator(final SmcOptions options)
    {
        super (options, TargetLanguage.C_SHARP.suffix());
    } // end of SmcCSharpGenerator(SmcOptions)

    //
    // end of Constructors.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // SmcVisitor Abstract Method Impelementation.
    //

    /**
     * Emits C# code for the finite state machine.
     * Generates the using statements, namespace, the FSM
     * context class, the default transitions and the FSM map
     * classes.
     * @param fsm emit C# code for this finite state machine.
     */
    @Override
    public void visit(final SmcFSM fsm)
    {
        String rawSource = fsm.getSource();
        String packageName = fsm.getPackage();
        String context = fsm.getContext();
        String fsmClassName = fsm.getFsmClassName();
        String startState = fsm.getStartState();
        String accessLevel = fsm.getAccessLevel();
        List<SmcMap> maps = fsm.getMaps();
        List<SmcTransition> transitions;
        Iterator<SmcParameter> pit;
        String transName;
        String csState;
        String separator;
        int index;
        List<SmcParameter> params;
        String indent2;

        outputHeader();

        // If the access level has not been set, then the
        // default is "public".
        if (accessLevel == null || accessLevel.length() == 0)
        {
            accessLevel = "public";
        }

        // Dump out the raw target code, if any.
        if (rawSource != null && rawSource.length() > 0)
        {
            mTarget.println(rawSource);
            mTarget.println();
        }

        // Always include the system package.
        mTarget.println("using System;");

        // If debugging code is being generated, then import
        // system diagnostics package as well.
        if (mDebugLevel >= DEBUG_LEVEL_0)
        {
            mTarget.println("using System.Diagnostics;");
        }

        // If serialization is on, then import the .Net
        // serialization package.
        if (mSerialFlag)
        {
            mTarget.println(
                "using System.Runtime.Serialization;");
            mTarget.println("using System.Security;");
            mTarget.println(
                "using System.Security.Permissions;");
        }

        // If reflection is on, then import the .Net collections
        // package.
        if (mReflectFlag)
        {
            if (mGenericFlag)
            {
                mTarget.println("using System.Collections.Generic;");
            }
            else
            {
                mTarget.println("using System.Collections;");
            }
        }
        mTarget.println();

        // Do user-specified imports now.
        for (String imp: fsm.getImports())
        {
            mTarget.print("using ");
            mTarget.print(imp);
            mTarget.println(";");
        }

        // If a package has been specified, generate the package
        // statement now and set the indent.
        if (packageName != null && packageName.length() > 0)
        {
            mTarget.print("namespace ");
            mTarget.println(packageName);
            mTarget.println("{");
            mIndent = "    ";
        }

        // Does the user want to serialize this FSM?
        if (mSerialFlag)
        {
            mTarget.print(mIndent);
            mTarget.println("[Serializable]");
        }

        addGeneratedCodeAttribute("");

        // Now declare the FSM context class.
        mTarget.print(mIndent);
        mTarget.print(accessLevel);
        mTarget.print(" sealed class ");
        mTarget.print(fsmClassName);
        mTarget.println(" :");
        mTarget.print(mIndent);
        mTarget.print("    statemap.FSMContext");
        if (mSerialFlag == false)
        {
            mTarget.println();
        }
        else
        {
            mTarget.println(',');
            mTarget.print(mIndent);
            mTarget.println("    ISerializable");
        }
        mTarget.print(mIndent);
        mTarget.println("{");
        mTarget.print(mIndent);
        mTarget.println(
            "//---------------------------------------------------------------");
        mTarget.print(mIndent);
        mTarget.println("// Properties.");
        mTarget.print(mIndent);
        mTarget.println("//");
        mTarget.println();

        // State property.
        mTarget.print(mIndent);
        mTarget.print("    public ");
        mTarget.print(context);
        mTarget.println("State State");
        mTarget.print(mIndent);
        mTarget.println("    {");
        mTarget.print(mIndent);
        mTarget.println("        get");
        mTarget.print(mIndent);
        mTarget.println("        {");

        // Again, if synchronization is on, then protect access
        // to this FSM.
        if (mSyncFlag)
        {
            mTarget.print(mIndent);
            mTarget.println("            lock (this)");
            mTarget.print(mIndent);
            mTarget.println("            {");

            indent2 = mIndent + "                ";
        }
        else
        {
            indent2 = mIndent + "            ";
        }

        mTarget.print(indent2);
        mTarget.println("if (state_ == null)");
        mTarget.print(indent2);
        mTarget.println("{");
        mTarget.print(indent2);
        mTarget.println("    throw(");
        mTarget.print(indent2);
        mTarget.println(
            "        new statemap.StateUndefinedException());");
        mTarget.print(indent2);
        mTarget.println("}");
        mTarget.println();
        mTarget.print(indent2);
        mTarget.print("return ((");
        mTarget.print(context);
        mTarget.println("State) state_);");

        // If we are in a lock block, close it.
        if (mSyncFlag)
        {
            mTarget.print(mIndent);
            mTarget.println("            }");
        }

        // Close the State get.
        mTarget.print(mIndent);
        mTarget.println("        }");

        // Now generate the State set.
        mTarget.print(mIndent);
        mTarget.println("        set");
        mTarget.print(mIndent);
        mTarget.println("        {");

        // Again, if synchronization is on, then protect access
        // to this FSM.
        if (mSyncFlag)
        {
            mTarget.print(mIndent);
            mTarget.println("            lock (this)");
            mTarget.print(mIndent);
            mTarget.println("            {");

            indent2 = mIndent + "                ";
        }
        else
        {
            indent2 = mIndent + "            ";
        }

        mTarget.print(indent2);
        mTarget.println("SetState(value);");

        // If we are in a lock block, close it.
        if (mSyncFlag)
        {
            mTarget.print(mIndent);
            mTarget.println("            }");
        }

        // Close the State set.
        mTarget.print(mIndent);
        mTarget.println("        }");

        // Close the state property.
        mTarget.print(mIndent);
        mTarget.println("    }");
        mTarget.println();

        // Generate the Owner property.
        mTarget.print(mIndent);
        mTarget.print("    public ");
        mTarget.print(context);
        mTarget.println(" Owner");
        mTarget.print(mIndent);
        mTarget.println("    {");

        // Generate the property get method.
        mTarget.print(mIndent);
        mTarget.println("        get");
        mTarget.print(mIndent);
        mTarget.println("        {");
        mTarget.print(mIndent);
        mTarget.println("            return (_owner);");
        mTarget.print(mIndent);
        mTarget.println("        }");

        // Generate the property set method.
        mTarget.print(mIndent);
        mTarget.println("        set");
        mTarget.print(mIndent);
        mTarget.println("        {");

        // Again, if synchronization is on, then protect access
        // to this FSM.
        if (mSyncFlag)
        {
            mTarget.print(mIndent);
            mTarget.println("            lock (this)");
            mTarget.print(mIndent);
            mTarget.println("            {");

            indent2 = mIndent + "                ";
        }
        else
        {
            indent2 = mIndent + "            ";
        }

        mTarget.print(indent2);
        mTarget.println("_owner = value;");

        // If we are in a lock block, close it.
        if (mSyncFlag)
        {
            mTarget.print(mIndent);
            mTarget.println("            }");
        }

        // Close the Onwer set.
        mTarget.print(mIndent);
        mTarget.println("        }");

        // Close the Owner property.
        mTarget.print(mIndent);
        mTarget.println("    }");
        mTarget.println();

        // If reflect is on, then generate the States property.
        if (mReflectFlag)
        {
            mTarget.print(mIndent);
            mTarget.print("    public ");
            mTarget.print(context);
            mTarget.println("State[] States");
            mTarget.print(mIndent);
            mTarget.println("    {");

            // Generate the property get method. There is no set
            // method.
            mTarget.print(mIndent);
            mTarget.println("        get");
            mTarget.print(mIndent);
            mTarget.println("        {");
            mTarget.print(mIndent);
            mTarget.println("            return (_States);");
            mTarget.print(mIndent);
            mTarget.println("        }");

            // Close the States property.
            mTarget.print(mIndent);
            mTarget.println("    }");
            mTarget.println();
        }

        mTarget.print(mIndent);
        mTarget.println(
            "//---------------------------------------------------------------");
        mTarget.print(mIndent);
        mTarget.println("// Member methods.");
        mTarget.print(mIndent);
        mTarget.println("//");
        mTarget.println();

        // The state name "map::state" must be changed to
        // "map.state".
        if ((index = startState.indexOf("::")) >= 0)
        {
            csState = startState.substring(0, index) +
                      "." +
                      startState.substring(index + 2);
        }
        else
        {
            csState = startState;
        }

        // Generate the context class' constructor.
        mTarget.print(mIndent);
        mTarget.print("    public ");
        mTarget.print(fsmClassName);
        mTarget.print("(");
        mTarget.print(context);
        mTarget.println(" owner) :");
        mTarget.print(mIndent);
        mTarget.print("        base (");
        mTarget.print(csState);
        mTarget.println(")");
        mTarget.print(mIndent);
        mTarget.println("    {");
        mTarget.println("        _owner = owner;");
        mTarget.print(mIndent);
        mTarget.println("    }");
        mTarget.println();

        // The finite state machine start method.
        mTarget.print(mIndent);
        mTarget.println("    public override void EnterStartState()");
        mTarget.print(mIndent);
        mTarget.println("    {");
        mTarget.print(mIndent);
        mTarget.println("        State.Entry(this);");
        mTarget.print(mIndent);
        mTarget.println("        return;");
        mTarget.print(mIndent);
        mTarget.println("    }");
        mTarget.println();

        // If -serial was specified, then generate the
        // deserialize constructor.
        if (mSerialFlag)
        {
            mTarget.print(mIndent);
            mTarget.print("    public ");
            mTarget.print(fsmClassName);
            mTarget.print("(SerializationInfo info, ");
            mTarget.println("StreamingContext context) :");
            mTarget.print(mIndent);
            mTarget.println("        base ()");
            mTarget.print(mIndent);
            mTarget.println("    {");
            mTarget.print(mIndent);
            mTarget.println("        int stackSize;");
            mTarget.print(mIndent);
            mTarget.println("        int stateId;");
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.print(
                "        stackSize = ");
            mTarget.println("info.GetInt32(\"stackSize\");");
            mTarget.print(mIndent);
            mTarget.println("        if (stackSize > 0)");
            mTarget.print(mIndent);
            mTarget.println("        {");
            mTarget.print(mIndent);
            mTarget.println("            int index;");
            mTarget.print(mIndent);
            mTarget.println("            String name;");
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.print(
                "            for (index = (stackSize - 1); ");
            mTarget.println("index >= 0; --index)");
            mTarget.print(mIndent);
            mTarget.println("            {");
            mTarget.print(mIndent);
            mTarget.print("                ");
            mTarget.println("name = \"stackIndex\" + index;");
            mTarget.print(mIndent);
            mTarget.print("                ");
            mTarget.println("stateId = info.GetInt32(name);");
            mTarget.print(mIndent);
            mTarget.print("                ");
            mTarget.println("PushState(_States[stateId]);");
            mTarget.print(mIndent);
            mTarget.println("            }");
            mTarget.print(mIndent);
            mTarget.println("        }");
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.println(
                "        stateId = info.GetInt32(\"state\");");
            mTarget.print(mIndent);
            mTarget.println(
                "        PushState(_States[stateId]);");
            mTarget.print(mIndent);
            mTarget.println("    }");
            mTarget.println();
        }

        // Generate the default transition methods.
        // First get the transition list.
        transitions = fsm.getTransitions();
        for (SmcTransition trans: transitions)
        {
            transName = trans.getName();

            // Ignore the default transition.
            if (transName.equals("Default") == false)
            {
                mTarget.print(mIndent);
                mTarget.print("    public void ");
                mTarget.print(transName);
                mTarget.print("(");

                // Now output the transition's parameters.
                params = trans.getParameters();
                for (pit = params.iterator(), separator = "";
                     pit.hasNext();
                     separator = ", ")
                {
                    mTarget.print(separator);
                    (pit.next()).accept(this);
                }
                mTarget.println(")");
                mTarget.print(mIndent);
                mTarget.println("    {");

                // If the -sync flag was specified, then output
                // "lock (this)" to prevent multiple threads from
                // access this state machine simultaneously.
                if (mSyncFlag)
                {
                    mTarget.print(mIndent);
                    mTarget.println("        lock (this)");
                    mTarget.print(mIndent);
                    mTarget.println("        {");

                    indent2 = mIndent + "            ";
                }
                else
                {
                    indent2 = mIndent + "        ";
                }

                // Save away the transition name in case it is
                // need in an UndefinedTransitionException.
                mTarget.print(indent2);
                mTarget.print("transition_ = \"");
                mTarget.print(transName);
                mTarget.println("\";");

                mTarget.print(indent2);
                mTarget.print("State.");
                mTarget.print(transName);
                mTarget.print("(this");

                for (SmcParameter param: params)
                {
                    mTarget.print(", ");
                    passParameter(param);
                }
                mTarget.println(");");
                mTarget.print(indent2);
                mTarget.println("transition_ = \"\";");

                // If the -sync flag was specified, then output
                // the "End SyncLock".
                if (mSyncFlag)
                {
                    mTarget.print(mIndent);
                    mTarget.println("        }");
                    mTarget.println();
                }

                mTarget.print(mIndent);
                mTarget.println("        return;");
                mTarget.print(mIndent);
                mTarget.println("    }");
                mTarget.println();
            }
        }

        // If -serial specified, then output the valueOf(int)
        // method.
        if (mSerialFlag)
        {
            mTarget.print(mIndent);
            mTarget.print("    public ");
            mTarget.print(context);
            mTarget.println("State valueOf(int stateId)");
            mTarget.print(mIndent);
            mTarget.println("    {");
            mTarget.print(mIndent);
            mTarget.println("        return(_States[stateId]);");
            mTarget.print(mIndent);
            mTarget.println("    }");
            mTarget.println();
        }

        // If serialization is turned on, then output the
        // GetObjectData method.
        if (mSerialFlag)
        {
            mTarget.print(mIndent);
            mTarget.print("    [SecurityPermissionAttribute(");
            mTarget.print("SecurityAction.Demand, ");
            mTarget.println("SerializationFormatter=true)]");
            mTarget.print(mIndent);
            mTarget.print("    public void GetObjectData(");
            mTarget.println("SerializationInfo info,");
            mTarget.print(mIndent);
            mTarget.print("                              ");
            mTarget.println("StreamingContext context)");
            mTarget.print(mIndent);
            mTarget.println("    {");
            mTarget.print(mIndent);
            mTarget.println("        int stackSize = 0;");
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.println("        if (stateStack_ != null)");
            mTarget.print(mIndent);
            mTarget.println("        {");
            mTarget.print(mIndent);
            mTarget.println(
                "            stackSize = stateStack_.Count;");
            mTarget.print(mIndent);
            mTarget.println("        }");
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.print("        ");
            mTarget.println(
                "info.AddValue(\"stackSize\", stackSize);");
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.println("        if (stackSize > 0)");
            mTarget.print(mIndent);
            mTarget.println("        {");
            mTarget.print(mIndent);
            mTarget.println("            int index = 0;");
            mTarget.print(mIndent);
            mTarget.println("            String name;");
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.print("            foreach (");
            mTarget.print(context);
            mTarget.println("State state in stateStack_)");
            mTarget.print(mIndent);
            mTarget.println("            {");
            mTarget.print(mIndent);
            mTarget.print("                ");
            mTarget.println("name = \"stackIndex\" + index;");
            mTarget.print(mIndent);
            mTarget.print("                info.AddValue(");
            mTarget.println("name, state.Id);");
            mTarget.print(mIndent);
            mTarget.println("                ++index;");
            mTarget.print(mIndent);
            mTarget.println("            }");
            mTarget.print(mIndent);
            mTarget.println("        }");
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.println(
                "        info.AddValue(\"state\", state_.Id);");
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.println("        return;");
            mTarget.print(mIndent);
            mTarget.println("    }");
            mTarget.println();
        }

        // Declare member data.
        mTarget.print(mIndent);
        mTarget.println(
            "//---------------------------------------------------------------");
        mTarget.print(mIndent);
        mTarget.println("// Member data.");
        mTarget.print(mIndent);
        mTarget.println("//");
        mTarget.println();
        mTarget.print(mIndent);
        mTarget.println("    [NonSerialized]");
        mTarget.print(mIndent);
        mTarget.print("    private ");
        mTarget.print(context);
        mTarget.println(" _owner;");
        mTarget.println();

        // If serialization support is on, then create the state
        // array.
        if (mSerialFlag || mReflectFlag)
        {
            String mapName;

            mTarget.print(mIndent);
            mTarget.println(
                "    // Map state IDs to state objects.");
            mTarget.print(mIndent);
            mTarget.println(
                "    // Used to deserialize an FSM.");
            mTarget.print(mIndent);
            mTarget.println("    [NonSerialized]");
            mTarget.print(mIndent);
            mTarget.print("    private static ");
            mTarget.print(context);
            mTarget.println("State[] _States =");
            mTarget.print(mIndent);
            mTarget.print("    {");

            separator = "";
            for (SmcMap map: maps)
            {
                mapName = map.getName();

                for (SmcState state: map.getStates())
                {
                    mTarget.println(separator);
                    mTarget.print(mIndent);
                    mTarget.print("        ");
                    mTarget.print(mapName);
                    mTarget.print(".");
                    mTarget.print(state.getClassName());

                    separator = ",";
                }
            }

            mTarget.println();
            mTarget.print(mIndent);
            mTarget.println("    };");
            mTarget.println();
        }

        // Declare the inner state class.
        mTarget.print(mIndent);
        mTarget.println(
            "//---------------------------------------------------------------");
        mTarget.print(mIndent);
        mTarget.println("// Inner classes.");
        mTarget.print(mIndent);
        mTarget.println("//");
        mTarget.println();

        addGeneratedCodeAttribute("    ");

        mTarget.print(mIndent);
        mTarget.print("    public abstract class ");
        mTarget.print(context);
        mTarget.println("State :");
        mTarget.print(mIndent);
        mTarget.println("        statemap.State");
        mTarget.print(mIndent);
        mTarget.println("    {");

        // The abstract Transitions property - if reflection was
        // is specified.
        if (mReflectFlag)
        {
            mTarget.print(mIndent);
            mTarget.println("    //-----------------------------------------------------------");
            mTarget.print(mIndent);
            mTarget.println("    // Properties.");
            mTarget.print(mIndent);
            mTarget.println("    //");
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.print("        ");
            mTarget.print("public abstract IDictionary");
            if (mGenericFlag)
            {
                mTarget.print("<string, int>");
            }
            mTarget.println(" Transitions");
            mTarget.print(mIndent);
            mTarget.println("        {");
            mTarget.print(mIndent);
            mTarget.println("            get;");
            mTarget.print(mIndent);
            mTarget.println("        }");
            mTarget.println();
        }

        mTarget.print(mIndent);
        mTarget.println("    //-----------------------------------------------------------");
        mTarget.print(mIndent);
        mTarget.println("    // Member methods.");
        mTarget.print(mIndent);
        mTarget.println("    //");
        mTarget.println();

        // State constructor.
        mTarget.print(mIndent);
        mTarget.print("        internal ");
        mTarget.print(context);
        mTarget.println("State(string name, int id) :");
        mTarget.print(mIndent);
        mTarget.println("            base (name, id)");
        mTarget.print(mIndent);
        mTarget.println("        {}");
        mTarget.println();

        // Entry/Exit methods.
        mTarget.print(mIndent);
        mTarget.print(
            "        protected internal virtual void Entry(");
        mTarget.print(fsmClassName);
        mTarget.println(" context)");
        mTarget.print(mIndent);
        mTarget.println("        {}");
        mTarget.println();
        mTarget.print(mIndent);
        mTarget.print(
            "        protected internal virtual void Exit(");
        mTarget.print(fsmClassName);
        mTarget.println(" context)");
        mTarget.print(mIndent);
        mTarget.println("        {}");
        mTarget.println();

        // Transition methods (except default).
        for (SmcTransition trans: transitions)
        {
            transName = trans.getName();

            if (transName.equals("Default") == false)
            {
                mTarget.print(mIndent);
                mTarget.print(
                    "        protected internal virtual void ");
                mTarget.print(transName);
                mTarget.print("(");
                mTarget.print(fsmClassName);
                mTarget.print(" context");

                for (SmcParameter param: trans.getParameters())
                {
                    mTarget.print(", ");
                    param.accept(this);
                }

                mTarget.println(")");
                mTarget.print(mIndent);
                mTarget.println("        {");

                // If this method is reached, that means this
                // transition was passed to a state which does
                // not define the transition. Call the state's
                // default transition method.
                mTarget.print(mIndent);
                mTarget.println("            Default(context);");

                mTarget.print(mIndent);
                mTarget.println("        }");
                mTarget.println();
            }
        }

        // Generate the overall Default transition for all maps.
        mTarget.print(mIndent);
        mTarget.print(
            "        protected internal virtual void Default(");
        mTarget.print(fsmClassName);
        mTarget.println(" context)");
        mTarget.print(mIndent);
        mTarget.println("        {");

        // If generating debug code, then write this trace
        // message.
        if (mDebugLevel >= DEBUG_LEVEL_0)
        {
            mTarget.println("#if TRACE");
            mTarget.print(mIndent);
            mTarget.println("            Trace.WriteLine(");
            mTarget.print(mIndent);
            mTarget.print(
                "                \"TRANSITION : Default\"");
            mTarget.println(");");
            mTarget.println("#endif");
        }

        // The default transition action is to throw a
        // TransitionUndefinedException.
        mTarget.print(mIndent);
        mTarget.println("            throw (");
        mTarget.print(mIndent);
        mTarget.print("                ");
        mTarget.println(
            "new statemap.TransitionUndefinedException(");
        mTarget.print(mIndent);
        mTarget.println(
            "                    \"State: \" +");
        mTarget.print(mIndent);
        mTarget.println(
            "                    context.State.Name +");
        mTarget.print(mIndent);
        mTarget.println(
            "                    \", Transition: \" +");
        mTarget.print(mIndent);
        mTarget.println(
            "                    context.GetTransition()));");

        // Close the Default transition method.
        mTarget.print(mIndent);
        mTarget.println("        }");

        // Close the inner state class declaration.
        mTarget.print(mIndent);
        mTarget.println("    }");

        // Have each map print out its target code now.
        for (SmcMap map: maps)
        {
            map.accept(this);
        }

        // Close the context class.
        mTarget.print(mIndent);
        mTarget.println("}");
        mTarget.println();

        // If a package has been specified, then generate
        // the closing brace now.
        if (packageName != null && packageName.length() > 0)
        {
            mTarget.println("}");
        }

        outputFooter();

        return;
    } // end of visit(SmcFSM)

    /**
     * Emits {@code [System.CodeDom.Compiler.GenerateCode()]}
     * attribute above each emitted class declaration. This
     * attribute is used by code analysis tools such as FxCop.
     * @param indent class declaration indent.
     */
    private void addGeneratedCodeAttribute(final String indent)
    {
        mTarget.print(mIndent);
        mTarget.print(indent);
        mTarget.println(
            "[System.CodeDom.Compiler.GeneratedCode(\"" +
            mAppName +
            "\",\" " +
            mAppVersion +
            "\")]");

        return;
	} // end of addGeneratedCodeAttribute(String)

	/**
     * Emits C# code for the FSM map.
     * @param map emit C# code for this map.
     */
    @Override
    public void visit(final SmcMap map)
    {
        List<SmcTransition> definedDefaultTransitions;
        SmcState defaultState = map.getDefaultState();
        String context = map.getFSM().getContext();
        String mapName = map.getName();
        String indent2;
        List<SmcState> states = map.getStates();

        // Initialize the default transition list to all the
        // default state's transitions.
        if (defaultState != null)
        {
            definedDefaultTransitions =
                defaultState.getTransitions();
        }
        else
        {
            definedDefaultTransitions = new ArrayList<>();
        }

        addGeneratedCodeAttribute("    ");

        // Declare the map class and make it abstract to prevent
        // its instantiation.
        mTarget.println();
        mTarget.print(mIndent);
        mTarget.print("    internal abstract class ");
        mTarget.println(mapName);
        mTarget.print(mIndent);
        mTarget.println("    {");
        mTarget.print(mIndent);
        mTarget.println(
            "    //-----------------------------------------------------------");
        mTarget.print(mIndent);
        mTarget.println("    // Member methods.");
        mTarget.print(mIndent);
        mTarget.println("    //");
        mTarget.println();
        mTarget.print(mIndent);
        mTarget.println(
            "    //-----------------------------------------------------------");
        mTarget.print(mIndent);
        mTarget.println("    // Member data.");
        mTarget.print(mIndent);
        mTarget.println("    //");
        mTarget.println();
        mTarget.print(mIndent);
        mTarget.println(
            "        //-------------------------------------------------------");
        mTarget.print(mIndent);
        mTarget.println("        // Statics.");
        mTarget.print(mIndent);
        mTarget.println("        //");

        // Declare each of the state class member data.
        for (SmcState state: states)
        {
            mTarget.print(mIndent);
            mTarget.println("        [NonSerialized]");
            mTarget.print(mIndent);
            mTarget.print(
                "        internal static readonly ");
            mTarget.print(mapName);
            mTarget.print("_Default.");
            mTarget.print(mapName);
            mTarget.print('_');
            mTarget.print(state.getClassName());
            mTarget.print(' ');
            mTarget.print(state.getInstanceName());
            mTarget.println(" =");
            mTarget.print(mIndent);
            mTarget.print("            new ");
            mTarget.print(mapName);
            mTarget.print("_Default.");
            mTarget.print(mapName);
            mTarget.print("_");
            mTarget.print(state.getClassName());
            mTarget.print("(\"");
            mTarget.print(mapName);
            mTarget.print(".");
            mTarget.print(state.getClassName());
            mTarget.print("\", ");
            mTarget.print(SmcMap.getNextStateId());
            mTarget.println(");");
        }

        // Create the default state as well.
        mTarget.print(mIndent);
        mTarget.println("        [NonSerialized]");
        mTarget.print(mIndent);
        mTarget.print("        private static readonly ");
        mTarget.print(mapName);
        mTarget.println("_Default Default =");
        mTarget.print(mIndent);
        mTarget.print("            new ");
        mTarget.print(mapName);
        mTarget.print("_Default(\"");
        mTarget.print(mapName);
        mTarget.println(".Default\", -1);");
        mTarget.println();

        // End of map class.
        mTarget.print(mIndent);
        mTarget.println("    }");
        mTarget.println();

        addGeneratedCodeAttribute("    ");

        // Declare the map default state class.
        mTarget.print(mIndent);
        mTarget.print("    internal class ");
        mTarget.print(mapName);
        mTarget.println("_Default :");
        mTarget.print(mIndent);
        mTarget.print("        ");
        mTarget.print(context);
        mTarget.println("State");
        mTarget.print(mIndent);
        mTarget.println("    {");

        // If reflection is on, generate the Transition property.
        if (mReflectFlag)
        {
            mTarget.print(mIndent);
            mTarget.println(
                "    //-----------------------------------------------------------");
            mTarget.print(mIndent);
            mTarget.println("    // Properties.");
            mTarget.print(mIndent);
            mTarget.println("    //");
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.print("        ");
            mTarget.print("public override IDictionary");
            if (mGenericFlag)
            {
                mTarget.print("<string, int>");
            }
            mTarget.println(" Transitions");
            mTarget.print(mIndent);
            mTarget.println("        {");
            mTarget.print(mIndent);
            mTarget.println("            get");
            mTarget.print(mIndent);
            mTarget.println("            {");
            mTarget.print(mIndent);
            mTarget.println(
                "                return (_transitions);");
            mTarget.print(mIndent);
            mTarget.println("            }");
            mTarget.print(mIndent);
            mTarget.println("        }");
            mTarget.println();
        }

        // Generate the constructor.
        mTarget.print(mIndent);
        mTarget.println(
            "    //-----------------------------------------------------------");
        mTarget.print(mIndent);
        mTarget.println("    // Member methods.");
        mTarget.print(mIndent);
        mTarget.println("    //");
        mTarget.println();
        mTarget.print(mIndent);
        mTarget.print("        internal ");
        mTarget.print(mapName);
        mTarget.println(
            "_Default(string name, int id) :");
        mTarget.print(mIndent);
        mTarget.println("            base (name, id)");
        mTarget.print(mIndent);
        mTarget.println("        {}");

        // Declare the user-defined transitions first.
        indent2 = mIndent;
        mIndent += "        ";
        for (SmcTransition trans: definedDefaultTransitions)
        {
            trans.accept(this);
        }
        mIndent = indent2;

        // Have each state now generate its code. Each state
        // class is an inner class.
        mTarget.println();
        mTarget.print(mIndent);
        mTarget.println(
            "    //-----------------------------------------------------------");
        mTarget.print(mIndent);
        mTarget.println("    // Inner classes.");
        mTarget.print(mIndent);
        mTarget.println("    //");
        for (SmcState state: states)
        {
            state.accept(this);
        }

        // If reflection is on, then define the transitions list.
        if (mReflectFlag)
        {
            List<SmcTransition> allTransitions =
                map.getFSM().getTransitions();
            String transName;
            List<SmcParameter> parameters;
            int transDefinition;

            mTarget.println();
            mTarget.print(mIndent);
            mTarget.println(
                "    //-----------------------------------------------------------");
            mTarget.print(mIndent);
            mTarget.println("    // Member data.");
            mTarget.print(mIndent);
            mTarget.println("    //");
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.println(
                "        //-------------------------------------------------------");
            mTarget.print(mIndent);
            mTarget.println("        // Statics.");
            mTarget.print(mIndent);
            mTarget.println("        //");
            mTarget.print(mIndent);
            mTarget.print("        ");
            mTarget.print("private static IDictionary");
            if (mGenericFlag)
            {
                mTarget.print("<string, int>");
            }
            mTarget.println(" _transitions;");
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.print("        static ");
            mTarget.print(mapName);
            mTarget.println("_Default()");
            mTarget.print(mIndent);
            mTarget.println("        {");
            mTarget.print(mIndent);
            mTarget.print("            ");
            mTarget.print("_transitions = new ");
            if (mGenericFlag)
            {
                mTarget.print("Dictionary<string, int>");
            }
            else
            {
                mTarget.print("Hashtable");
            }
            mTarget.println("();");

            // Now place the transition names into the list.
            for (SmcTransition transition: allTransitions)
            {
                transName = transition.getName();
                transName += "(";
                parameters = transition.getParameters();
                for ( int i = 0; i < parameters.size(); i++)
                {
                	SmcParameter singleParam= parameters.get( i );
                	transName += singleParam.getType() + " " + singleParam.getName();
                	if ( i < parameters.size() - 1 )
                	{
		                transName += ", ";
                	}
                }
                transName += ")";

                // If the transition is defined in this map's
                // default state, then the value is 2.
                if (definedDefaultTransitions.contains(transition))
                {
                    transDefinition = 2;
                }
                // Otherwise the value is 0 - undefined.
                else
                {
                    transDefinition = 0;
                }

                mTarget.print("            ");
                mTarget.print("_transitions.Add(\"");
                mTarget.print(transName);
                mTarget.print("\", ");
                mTarget.print(transDefinition);
                mTarget.println(");");
            }
            mTarget.print(mIndent);
            mTarget.println("        }");
        }

        // End of the map default state class.
        mTarget.print(mIndent);
        mTarget.println("    }");

        return;
    } // end of visit(SmcMap)

    /**
     * Emits C# code for this FSM state.
     * @param state emits C# code for this state.
     */
    @Override
    public void visit(final SmcState state)
    {
        SmcMap map = state.getMap();
        String context = map.getFSM().getContext();
		String fsmClassName = map.getFSM().getFsmClassName();
        String mapName = map.getName();
        String stateName = state.getClassName();
        List<SmcAction> actions;
        String indent2;

        // Declare the inner state class.
        mTarget.println();

        addGeneratedCodeAttribute("        ");

        mTarget.print(mIndent);
        mTarget.print("        internal class ");
        mTarget.print(mapName);
        mTarget.print("_");
        mTarget.print(stateName);
        mTarget.println(" :");
        mTarget.print(mIndent);
        mTarget.print("            ");
        mTarget.print(mapName);
        mTarget.println("_Default");
        mTarget.print(mIndent);
        mTarget.println("        {");

        // Generate the Transitions property if reflection is on.
        if (mReflectFlag)
        {
            mTarget.print(mIndent);
            mTarget.println(
                "        //-------------------------------------------------------");
            mTarget.print(mIndent);
            mTarget.println("        // Properties.");
            mTarget.print(mIndent);
            mTarget.println("        //");
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.print("            ");
            mTarget.print("public override IDictionary");
            if (mGenericFlag)
            {
                mTarget.print("<string, int>");
            }
            mTarget.println(" Transitions");
            mTarget.print(mIndent);
            mTarget.println("            {");
            mTarget.print(mIndent);
            mTarget.println("                get");
            mTarget.print(mIndent);
            mTarget.println("                {");
            mTarget.print(mIndent);
            mTarget.println(
                "                    return (_transitions);");
            mTarget.print(mIndent);
            mTarget.println("                }");
            mTarget.print(mIndent);
            mTarget.println("            }");
            mTarget.println();
        }

        // Add the constructor.
        mTarget.print(mIndent);
        mTarget.println(
            "        //-------------------------------------------------------");
        mTarget.print(mIndent);
        mTarget.println("        // Member methods.");
        mTarget.print(mIndent);
        mTarget.println("        //");
        mTarget.println();
        mTarget.print(mIndent);
        mTarget.print("            internal ");
        mTarget.print(mapName);
        mTarget.print("_");
        mTarget.print(stateName);
        mTarget.println("(string name, int id) :");
        mTarget.print(mIndent);
        mTarget.println("                base (name, id)");
        mTarget.print(mIndent);
        mTarget.println("            {}");

        // Add the Entry() and Exit() methods if this state
        // defines them.
        actions = state.getEntryActions();
        if (actions != null && actions.isEmpty() == false)
        {
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.print("            ");
            mTarget.print(
                "protected internal override void Entry(");
            mTarget.print(fsmClassName);
            mTarget.println(" context)");
            mTarget.print(mIndent);
            mTarget.println("            {");

            // Declare the "ctxt" local variable.
            mTarget.print(mIndent);
            mTarget.print("                ");
            mTarget.print(context);
            mTarget.println(" ctxt = context.Owner;");
            mTarget.println();

            // Generate the actions associated with this code.
            indent2 = mIndent;
            mIndent += "                ";
            for (SmcAction action: actions)
            {
                action.accept(this);
            }
            mIndent = indent2;

            // End of the Entry() method.
            mTarget.print(mIndent);
            mTarget.println("                return;");
            mTarget.print(mIndent);
            mTarget.println("            }");
        }

        actions = state.getExitActions();
        if (actions != null && actions.isEmpty() == false)
        {
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.print("            ");
            mTarget.print(
                "protected internal override void Exit(");
            mTarget.print(fsmClassName);
            mTarget.println(" context)");
            mTarget.print(mIndent);
            mTarget.println("            {");

            // Declare the "ctxt" local variable.
            mTarget.print(mIndent);
            mTarget.print("                ");
            mTarget.print(context);
            mTarget.println(" ctxt = context.Owner;");
            mTarget.println();

            // Generate the actions associated with this code.
            mIndent += "                ";
            for (SmcAction action: actions)
            {
                action.accept(this);
            }

            // End of the Exit() method.
            mTarget.print(mIndent);
            mTarget.println("                return;");
            mTarget.print(mIndent);
            mTarget.println("            }");
        }

        // Have each transition generate its code.
        indent2 = mIndent;
        mIndent += "            ";
        for (SmcTransition trans: state.getTransitions())
        {
            trans.accept(this);
        }
        mIndent = indent2;

        // If reflection is on, then generate the transitions
        // map.
        if (mReflectFlag)
        {
            List<SmcTransition> allTransitions =
                map.getFSM().getTransitions();
            List<SmcTransition> stateTransitions =
                state.getTransitions();
            SmcState defaultState = map.getDefaultState();
            List<SmcTransition> defaultTransitions;
            String transName;
			List<SmcParameter> parameters;
            int transDefinition;

            // Initialize the default transition list to all the
            // default state's transitions.
            if (defaultState != null)
            {
                defaultTransitions =
                    defaultState.getTransitions();
            }
            else
            {
                defaultTransitions = new ArrayList<>();
            }

            mTarget.println();
            mTarget.print(mIndent);
            mTarget.println(
                "        //-------------------------------------------------------");
            mTarget.print(mIndent);
            mTarget.println("        // Member data.");
            mTarget.print(mIndent);
            mTarget.println("        //");
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.println(
                "            //---------------------------------------------------");
            mTarget.print(mIndent);
            mTarget.println("            // Statics.");
            mTarget.print(mIndent);
            mTarget.println("            //");
            mTarget.print(mIndent);
            mTarget.print("            ");
            mTarget.print("new private static IDictionary");
            if (mGenericFlag)
            {
                mTarget.print("<string, int>");
            }
            mTarget.println(" _transitions;");
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.print("            static ");
            mTarget.print(mapName);
            mTarget.print("_");
            mTarget.print(stateName);
            mTarget.println("()");
            mTarget.print(mIndent);
            mTarget.println("            {");
            mTarget.print(mIndent);
            mTarget.print("                ");
            mTarget.print("_transitions = new ");
            if (mGenericFlag)
            {
                mTarget.print("Dictionary<string, int>");
            }
            else
            {
                mTarget.print("Hashtable");
            }
            mTarget.println("();");

            // Now place the transition names into the list.
            for (SmcTransition transition: allTransitions)
            {
                transName = transition.getName();
                transName += "(";
                parameters = transition.getParameters();
                for ( int i = 0; i < parameters.size(); i++)
                {
                	SmcParameter singleParam= parameters.get( i );
                	transName += singleParam.getType() + " " + singleParam.getName();
                	if ( i < parameters.size() - 1 )
                	{
		                transName += ", ";
                	}
                }
                transName += ")";

                // If the transition is in this state, then its
                // value is 1.
                if (stateTransitions.contains(transition))
                {
                    transDefinition = 1;
                }
                // If the transition is defined in this map's
                // default state, then the value is 2.
                else if (defaultTransitions.contains(transition))
                {
                    transDefinition = 2;
                }
                // Otherwise the value is 0 - undefined.
                else
                {
                    transDefinition = 0;
                }

                mTarget.print("                ");
                mTarget.print("_transitions.Add(\"");
                mTarget.print(transName);
                mTarget.print("\", ");
                mTarget.print(transDefinition);
                mTarget.println(");");
            }

            mTarget.print(mIndent);
            mTarget.println("            }");
        }

        // End of state declaration.
        mTarget.print(mIndent);
        mTarget.println("        }");

        return;
    } // end of visit(SmcState)

    /**
     * Emits C# code for this FSM state transition.
     * @param transition emits C# code for this state transition.
     */
    @Override
    public void visit(final SmcTransition transition)
    {
        SmcState state = transition.getState();
        SmcMap map = state.getMap();
        String context = map.getFSM().getContext();
		String fsmClassName = map.getFSM().getFsmClassName();
        String mapName = map.getName();
        String stateName = state.getClassName();
        String transName = transition.getName();
        List<SmcParameter> parameters =
            transition.getParameters();
        List<SmcGuard> guards = transition.getGuards();
        SmcGuard nullGuard = null;
        Iterator<SmcGuard> git = guards.iterator();
        SmcGuard guard;

        mTarget.println();
        mTarget.print(mIndent);
        mTarget.print("protected internal override void ");
        mTarget.print(transName);
        mTarget.print("(");
        mTarget.print(fsmClassName);
        mTarget.print(" context");

        // Add user-defined parameters.
        for (SmcParameter param: parameters)
        {
            mTarget.print(", ");
            param.accept(this);
        }
        mTarget.println(")");

        mTarget.print(mIndent);
        mTarget.println("{");

        // Almost all transitions have a "ctxt" local variable.
        if (transition.hasCtxtReference())
        {
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.print("    ");
            mTarget.print(context);
            mTarget.println(" ctxt = context.Owner;");
            mTarget.println();
        }

        // Output transition to debug stream.
        if (mDebugLevel >= DEBUG_LEVEL_0)
        {
            mTarget.println();
            mTarget.println("#if TRACE");
            mTarget.print(mIndent);
            mTarget.println("    Trace.WriteLine(");
            mTarget.print(mIndent);
            mTarget.print("        \"LEAVING STATE   : ");
            mTarget.print(mapName);
            mTarget.print('.');
            mTarget.print(stateName);
            mTarget.println("\");");
            mTarget.println("#endif");
            mTarget.println();
        }

        // Loop through the guards and print each one.
        mGuardIndex = 0;
        mGuardCount = guards.size();
        while (git.hasNext())
        {
            guard = git.next();

            // Output the no condition guard *after* all other
            // guarded transitions.
            if (guard.getCondition().isEmpty())
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
        // transition. Pass all arguments into the default
        // transition.
        else if (mGuardIndex > 0)
        {
            // If there was only one guard, then we need to close
            // of its body.
            if (mGuardCount == 1)
            {
                mTarget.print(mIndent);
                mTarget.println("    }");
            }

            mTarget.print(mIndent);
            mTarget.println("    else");
            mTarget.print(mIndent);
            mTarget.println("    {");

            // Call the super class' transition method using
            // the "base" keyword and not the class name.
            mTarget.print(mIndent);
            mTarget.print("        base.");
            mTarget.print(transName);
            mTarget.print("(context");
            for (SmcParameter param: parameters)
            {
                mTarget.print(", ");
                passParameter(param);
            }
            mTarget.println(");");

            mTarget.print(mIndent);
            mTarget.println("    }");
            mTarget.println();
        }

        // End of transition.
        mTarget.println();
        mTarget.print(mIndent);
        mTarget.println("    return;");
        mTarget.print(mIndent);
        mTarget.println("}");

        return;
    } // end of visit(SmcTransition)

    /**
     * Emits C# code for this FSM transition guard.
     * @param guard emits C# code for this transition guard.
     */
    @Override
    public void visit(final SmcGuard guard)
    {
        SmcTransition transition = guard.getTransition();
        SmcState state = transition.getState();
        SmcMap map = state.getMap();
        String context = map.getFSM().getContext();
        String mapName = map.getName();
        String stateName = state.getClassName();
        String transName = transition.getName();
        TransType transType = guard.getTransType();
        boolean loopbackFlag;
        String indent2;
        String indent3;
        String indent4;
        String endStateName = guard.getEndState();
        String fqEndStateName = "";
        String pushStateName = guard.getPushState();
        String condition = guard.getCondition();
        List<SmcAction> actions = guard.getActions();
        boolean hasActions = !(actions.isEmpty());

        // If this guard's end state is not of the form
        // "map::state", then prepend the map name to the
        // state name.
        // DON'T DO THIS IF THIS IS A POP TRANSITION!
        // The "state" is actually a transition name.
        if (transType != TransType.TRANS_POP &&
            endStateName.length () > 0 &&
            endStateName.equals(SmcElement.NIL_STATE) == false)
        {
            endStateName = scopeStateName(endStateName, mapName);
        }

        // Qualify the state and push state names as well.
        stateName = scopeStateName(stateName, mapName);
        pushStateName = scopeStateName(pushStateName, mapName);

        // Is this an *internal* loopback?
        loopbackFlag = isLoopback(transType, endStateName);

        // The guard code generation is a bit tricky. The first
        // question is how many guards are there? If there are
        // more than one, then we will need to generate the
        // proper "if-then-else" code.
        if (mGuardCount > 1)
        {
            indent2 = mIndent + "        ";

            // There are multiple guards.
            // Is this the first guard?
            if (mGuardIndex == 0 && condition.length() > 0)
            {
                // Yes, this is the first. This means an "if"
                // should be used.
                mTarget.print(mIndent);
                mTarget.print("    if (");
                mTarget.print(condition);
                mTarget.println(")");
                mTarget.print(mIndent);
                mTarget.println("    {");
            }
            else if (condition.length() > 0)
            {
                // No, this is not the first transition but it
                // does have a condition. Use an "else if".
                mTarget.println();
                mTarget.print(mIndent);
                mTarget.print("    else if (");
                mTarget.print(condition);
                mTarget.println(")");
                mTarget.print(mIndent);
                mTarget.println("    {");
            }
            else
            {
                // This is not the first transition and it has
                // no condition.
                mTarget.println();
                mTarget.print(mIndent);
                mTarget.println("    else");
                mTarget.print(mIndent);
                mTarget.println("    {");
            }
        }
        // There is only one guard. Does this guard have
        // a condition?
        else if (condition.length() == 0)
        {
            // No. This is a plain, old. vanilla transition.
            indent2 = mIndent + "    ";
        }
        else
        {
            // Yes there is a condition.
            indent2 = mIndent + "        ";

            mTarget.print(mIndent);
            mTarget.print("    if (");
            mTarget.print(condition);
            mTarget.println(")");
            mTarget.print(mIndent);
            mTarget.println("    {");
        }

        // Now that the necessary conditions are in place, it's
        // time to dump out the transition's actions. First, do
        // the proper handling of the state change. If this
        // transition has no actions, then set the end state
        // immediately. Otherwise, unset the current state so
        // that if an action tries to issue a transition, it will
        // fail.
        if (hasActions == false && endStateName.length() != 0)
        {
            fqEndStateName = endStateName;
        }
        else if (hasActions)
        {
            // Save away the current state if this is a loopback
            // transition. Storing current state allows the
            // current state to be cleared before any actions are
            // executed. Remember: actions are not allowed to
            // issue transitions and clearing the current state
            // prevents them from doing do.
            if (loopbackFlag)
            {
                fqEndStateName = "endState";

                mTarget.print(indent2);
                mTarget.print(context);
                mTarget.print("State ");
                mTarget.print(fqEndStateName);
                mTarget.println(" = context.State;");
            }
            else
            {
                fqEndStateName = endStateName;
            }
        }

        mTarget.println();

        // Dump out the exit actions if
        // 1) this is a standard, non-loopback transition or
        // 2) a pop transition.
        if (transType == TransType.TRANS_POP ||
            loopbackFlag == false)
        {
            if (mDebugLevel >= DEBUG_LEVEL_1)
            {
                mTarget.println("#if TRACE");
                mTarget.print(indent2);
                mTarget.println("Trace.WriteLine(");
                mTarget.print(indent2);
                mTarget.print("    \"BEFORE EXIT     : ");
                mTarget.print(stateName);
                mTarget.println(".Exit(context)\");");
                mTarget.println("#endif");
                mTarget.println();
            }

            mTarget.print(indent2);
            mTarget.println("context.State.Exit(context);");

            if (mDebugLevel >= DEBUG_LEVEL_1)
            {
                mTarget.println("#if TRACE");
                mTarget.print(indent2);
                mTarget.println("Trace.WriteLine(");
                mTarget.print(indent2);
                mTarget.print("    \"AFTER EXIT      : ");
                mTarget.print(stateName);
                mTarget.println(".Exit(context)\");");
                mTarget.println("#endif");
                mTarget.println();
            }
        }

        if (mDebugLevel >= DEBUG_LEVEL_0)
        {
            List<SmcParameter> parameters =
                transition.getParameters();

            mTarget.println("#if TRACE");
            mTarget.print(indent2);
            mTarget.println("Trace.WriteLine(");
            mTarget.print(indent2);
            mTarget.print("    \"ENTER TRANSITION: ");
            mTarget.print(mapName);
            mTarget.print('.');
            mTarget.print(stateName);
            mTarget.print(".");
            mTarget.print(transName);

            // Output the transition parameters.
            mTarget.print("(");
            for (SmcParameter param: parameters)
            {
                mTarget.print(", ");
                param.accept(this);
            }
            mTarget.print(")");
            mTarget.println("\");");
            mTarget.println("#endif");
            mTarget.println();
        }

        // Dump out this transition's actions.
        if (hasActions == false)
        {
            if (condition.length() > 0)
            {
                mTarget.print(indent2);
                mTarget.println("// No actions.");
            }

            indent3 = indent2;
        }
        else
        {
            // Now that we are in the transition, clear the
            // current state.
            mTarget.print(indent2);
            mTarget.println("context.ClearState();");

            // v. 2.0.0: Place the actions inside a try/finally
            // block. This way the state will be set before an
            // exception leaves the transition method.
            // v. 2.2.0: Check if the user has turned off this
            // feature first.
            if (mNoCatchFlag == false)
            {
                mTarget.println();
                mTarget.print(indent2);
                mTarget.println("try");
                mTarget.print(indent2);
                mTarget.println("{");

                indent3 = indent2 + "    ";
            }
            else
            {
                indent3 = indent2;
            }

            indent4 = mIndent;
            mIndent = indent3;
            for (SmcAction action: actions)
            {
                action.accept(this);
            }
            mIndent = indent4;

            // v. 2.2.0: Check if the user has turned off this
            // feature first.
            if (mNoCatchFlag == false)
            {
                mTarget.print(indent2);
                mTarget.println("}");
                mTarget.print(indent2);
                mTarget.println("finally");
                mTarget.print(indent2);
                mTarget.println("{");
            }
        }

        if (mDebugLevel >= DEBUG_LEVEL_0)
        {
            List<SmcParameter> parameters =
                transition.getParameters();

            mTarget.println("#if TRACE");
            mTarget.print(indent3);
            mTarget.println("Trace.WriteLine(");
            mTarget.print(indent3);
            mTarget.print("    \"EXIT TRANSITION : ");
            mTarget.print(mapName);
            mTarget.print('.');
            mTarget.print(stateName);
            mTarget.print(".");
            mTarget.print(transName);

            // Output the transition parameters.
            mTarget.print("(");
            for (SmcParameter param: parameters)
            {
                mTarget.print(", ");
                param.accept(this);
            }
            mTarget.print(")");
            mTarget.println("\");");
            mTarget.println("#endif");
            mTarget.println();
        }

        // Print the state assignment if necessary. Do NOT
        // generate the state assignment if:
        // 1. The transition has no actions AND is a loopback OR
        // 2. This is a push or pop transition.
        if (transType == TransType.TRANS_SET &&
            (hasActions || loopbackFlag == false))
        {
            mTarget.print(indent3);
            mTarget.print("context.State = ");
            mTarget.print(fqEndStateName);
            mTarget.println(";");
        }
        else if (transType == TransType.TRANS_PUSH)
        {
            // Set the next state so this it can be pushed
            // onto the state stack. But only do so if a clear
            // state was done.
            // v. 4.3.0: If the full-qualified end state is
            // "nil", then don't need to do anything.
            if ((loopbackFlag == false || hasActions) &&
                fqEndStateName.equals(SmcElement.NIL_STATE) == false)
            {
                mTarget.print(indent3);
                mTarget.print("context.State = ");
                mTarget.print(fqEndStateName);
                mTarget.println(";");
            }

            // Before doing the push, execute the end state's
            // entry actions (if any) if this is not a loopback.
            if (loopbackFlag == false)
            {
                if (mDebugLevel >= DEBUG_LEVEL_1)
                {
                    mTarget.println("#if TRACE");
                    mTarget.print(indent3);
                    mTarget.println("Trace.WriteLine(");
                    mTarget.print(indent3);
                    mTarget.print("    \"BEFORE ENTRY    : ");
                    mTarget.print(mapName);
                    mTarget.print('.');
                    mTarget.print(stateName);
                    mTarget.println(".Exit(context)\");");
                    mTarget.println("#endif");
                    mTarget.println();
                }

                mTarget.print(indent3);
                mTarget.println("context.State.Entry(context);");

                if (mDebugLevel >= DEBUG_LEVEL_1)
                {
                    mTarget.println("#if TRACE");
                    mTarget.print(indent3);
                    mTarget.println("Trace.WriteLine(");
                    mTarget.print(indent3);
                    mTarget.print("    \"AFTER ENTRY     : ");
                    mTarget.print(mapName);
                    mTarget.print('.');
                    mTarget.print(stateName);
                    mTarget.println(".Exit(context)\");");
                    mTarget.println("#endif");
                    mTarget.println();
                }
            }

            mTarget.print(indent3);
            mTarget.print("context.PushState(");
            mTarget.print(pushStateName);
            mTarget.println(");");
        }
        else if (transType == TransType.TRANS_POP)
        {
            mTarget.print(indent3);
            mTarget.println("context.PopState();");
        }

        // Perform the new state's enty actions if:
        // 1) this is a standard, non-loopback transition or
        // 2) a push transition.
        if ((transType == TransType.TRANS_SET &&
             loopbackFlag == false) ||
             transType == TransType.TRANS_PUSH)
        {
            if (mDebugLevel >= DEBUG_LEVEL_1)
            {
                mTarget.println("#if TRACE");
                mTarget.print(indent3);
                mTarget.println("Trace.WriteLine(");
                mTarget.print(indent3);
                mTarget.print("    \"BEFORE ENTRY    : ");
                mTarget.print(mapName);
                mTarget.print('.');
                mTarget.print(fqEndStateName);
                mTarget.println(".Exit(context)\");");
                mTarget.println("#endif");
                mTarget.println();
            }

            mTarget.print(indent3);
            mTarget.println("context.State.Entry(context);");

            if (mDebugLevel >= DEBUG_LEVEL_1)
            {
                mTarget.println("#if TRACE");
                mTarget.print(indent3);
                mTarget.println("Trace.WriteLine(");
                mTarget.print(indent3);
                mTarget.print("    \"AFTER ENTRY     : ");
                mTarget.print(mapName);
                mTarget.print('.');
                mTarget.print(fqEndStateName);
                mTarget.println(".Exit(context)\");");
                mTarget.println("#endif");
                mTarget.println();
            }
        }

        // If there was a try/finally, then put the closing
        // brace on the finally block.
        // v. 2.2.0: Check if the user has turned off this
        // feature first.
        if (hasActions && mNoCatchFlag == false)
        {
            mTarget.print(indent2);
            mTarget.println("}");
            mTarget.println();
        }

        // If there is a transition associated with the pop, then
        // issue that transition here.
        if (transType == TransType.TRANS_POP &&
            endStateName.equals(SmcElement.NIL_STATE) == false &&
            endStateName.length() > 0)
        {
            String popArgs = guard.getPopArgs();

            mTarget.println();
            mTarget.print(indent2);
            mTarget.print("context.");
            mTarget.print(endStateName);
            mTarget.print("(");

            // Output any and all pop arguments.
            if (popArgs.length() > 0)
            {
                mTarget.print(popArgs);
            }
            mTarget.println(");");
        }

        // If this is a guarded transition, it will be necessary
        // to close off the "if" body. DON'T PRINT A NEW LINE!
        // Why? Because an "else" or "else if" may follow and we
        // won't know until we go back to the transition target
        // generator whether all clauses have been done.
        if (mGuardCount > 1)
        {
            mTarget.print(mIndent);
            mTarget.print("    }");
        }

        return;
    } // end of visit(SmcGuard)

    /**
     * Emits C# code for this FSM action.
     * @param action emits C# code for this action.
     */
    @Override
    public void visit(SmcAction action)
    {
        String name = action.getName();
        List<String> arguments = action.getArguments();
        Iterator<String> it;
        String sep;

        mTarget.print(mIndent);

        // Need to distinguish between FSMContext actions and
        // application class actions. If the action is
        // "emptyStateStack", then pass it to the context.
        // Otherwise, let the application class handle it.
        if ( action.isEmptyStateStack())
        {
            mTarget.println("context.EmptyStateStack();");
        }
        else
        {
        	if ( action.isStatic() == false )
        	{
	            mTarget.print("ctxt.");
        	}
            mTarget.print(name);
	        if (action.isProperty())
	        {
	            String arg = arguments.get(0);

	            mTarget.print(" = ");
	            mTarget.print(arg);
	            mTarget.println(";");
	        }
	        else
	        {
	            mTarget.print("(");

	            for (it = arguments.iterator(), sep = "";
	                 it.hasNext();
	                 sep = ", ")
	            {
	                mTarget.print(sep);
	                mTarget.print(it.next());
	            }

	            mTarget.println(");");
	        }
	        }

        return;
    } // end of visit(SmcAction)

    /**
     * Emits C# code for this transition parameter.
     * @param parameter emits C# code for this transition parameter.
     */
    @Override
    public void visit(final SmcParameter parameter)
    {
        mTarget.print(parameter.getType());
        mTarget.print(" ");
        mTarget.print(parameter.getName());

        return;
    } // end of visit(SmcParameter)

    /**
     * Emits the unmodifiable comment.
     */
    private void outputHeader()
    {
        mTarget.println("/*");
        mTarget.println(" * ex: set ro:");
        mTarget.println(" * DO NOT EDIT.");
        mTarget.println(" * generated by smc (http://smc.sourceforge.net/)");
        mTarget.print(" * from file : ");
        mTarget.print(mSrcfileBase);
        mTarget.println(".sm");
        mTarget.println(" */");
        mTarget.println();

        return;
    } // end of outputHeader()

    /**
     * Emits the ending unmodifiable comment.
     */
    private void outputFooter()
    {
        mTarget.println();
        mTarget.println("/*");
        mTarget.println(" * Local variables:");
        mTarget.println(" *  buffer-read-only: t");
        mTarget.println(" * End:");
        mTarget.println(" */");

        return;
    } // end of outputFooter()

	/**
	 * Emits C# code for passing the transition parameter to
     * another method.
     * @param params emit these parameters.
	 */
	private void passParameter(final SmcParameter param)
	{
        String paramType=param.getType().trim();

        if ( paramType.startsWith( "ref " ) )
        {
        	mTarget.print( "ref ");
        } else if (paramType.startsWith( "out " ) )
        {
        	mTarget.print( "out ");
        }
        mTarget.print(param.getName());
	}

    //
    // end of SmcVisitor Abstract Method Impelementation.
    //-----------------------------------------------------------
} // end of class SmcCSharpGenerator

//
// CHANGE LOG
// $Log: SmcCSharpGenerator.java,v $
// Revision 1.14  2015/08/02 19:44:36  cwrapp
// Release 6.6.0 commit.
//
// Revision 1.13  2013/07/14 14:32:38  cwrapp
// check in for release 6.2.0
//
// Revision 1.12  2011/11/20 14:58:33  cwrapp
// Check in for SMC v. 6.1.0
//
// Revision 1.11  2009/12/17 19:51:43  cwrapp
// Testing complete.
//
// Revision 1.10  2009/11/25 22:30:19  cwrapp
// Fixed problem between %fsmclass and sm file names.
//
// Revision 1.9  2009/11/25 15:09:45  cwrapp
// Corrected getStates.
//
// Revision 1.8  2009/11/24 20:42:39  cwrapp
// v. 6.0.1 update
//
// Revision 1.7  2009/10/06 15:31:59  kgreg99
// 1. Started implementation of feature request #2718920.
//     1.1 Added method boolean isStatic() to SmcAction class. It returns false now, but is handled in following language generators: C#, C++, java, php, VB. Instance identificator is not added in case it is set to true.
// 2. Resolved confusion in "emtyStateStack" keyword handling. This keyword was not handled in the same way in all the generators. I added method boolean isEmptyStateStack() to SmcAction class. This method is used instead of different string comparisons here and there. Also the generated method name is fixed, not to depend on name supplied in the input sm file.
//
// Revision 1.6  2009/10/05 13:54:45  kgreg99
// Feature request #2865719 implemented.
// Added method "passParameter" to SmcCSharpGenerator class. It shall be used to generate C# code if a transaction parameter shall be passed to another method. It preserves "ref" and "out" modifiers.
//
// Revision 1.5  2009/09/12 21:44:49  kgreg99
// Implemented feature req. #2718941 - user defined generated class name.
// A new statement was added to the syntax: %fsmclass class_name
// It is optional. If not used, generated class is called as before "XxxContext" where Xxx is context class name as entered via %class statement.
// If used, generated class is called asrequested.
// Following language generators are touched:
// c, c++, java, c#, objc, lua, groovy, scala, tcl, VB
// This feature is not tested yet !
// Maybe it will be necessary to modify also the output file name.
//
// Revision 1.4  2009/09/05 15:39:20  cwrapp
// Checking in fixes for 1944542, 1983929, 2731415, 2803547 and feature 2797126.
//
// Revision 1.3  2009/03/30 21:23:47  kgreg99
// 1. Patch for bug #2679204. Source code was compared to SmcJavaGenerator. At the end of function Visit(SmcGuard ... ) the condition to emit Entry() code was changed. Notice: there are other disimilarities in checking conditions in that function !
//
// Revision 1.2  2009/03/03 17:28:53  kgreg99
// 1. Bugs resolved:
// #2657779 - modified SmcParser.sm and SmcParserContext.java
// #2648516 - modified SmcCSharpGenerator.java
// #2648472 - modified SmcSyntaxChecker.java
// #2648469 - modified SmcMap.java
//
// Revision 1.1  2009/03/01 18:20:42  cwrapp
// Preliminary v. 6.0.0 commit.
//
// Revision 1.12  2008/03/21 14:03:16  fperrad
// refactor : move from the main file Smc.java to each language generator the following data :
//  - the default file name suffix,
//  - the file name format for the generated SMC files
//
// Revision 1.11  2008/01/14 19:59:23  cwrapp
// Release 5.0.2 check-in.
//
// Revision 1.10  2007/02/21 13:54:15  cwrapp
// Moved Java code to release 1.5.0
//
// Revision 1.9  2007/01/15 00:23:50  cwrapp
// Release 4.4.0 initial commit.
//
// Revision 1.8  2006/09/16 15:04:28  cwrapp
// Initial v. 4.3.3 check-in.
//
// Revision 1.7  2006/06/03 19:39:25  cwrapp
// Final v. 4.3.1 check in.
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
// Revision 1.5  2005/09/19 15:20:03  cwrapp
// Changes in release 4.2.2:
// New features:
//
// None.
//
// Fixed the following bugs:
//
// + (C#) -csharp not generating finally block closing brace.
//
// Revision 1.4  2005/09/14 01:51:33  cwrapp
// Changes in release 4.2.0:
// New features:
//
// None.
//
// Fixed the following bugs:
//
// + (Java) -java broken due to an untested minor change.
//
// Revision 1.3  2005/08/26 15:21:34  cwrapp
// Final commit for release 4.2.0. See README.txt for more information.
//
// Revision 1.2  2005/06/30 10:44:23  cwrapp
// Added %access keyword which allows developers to set the generate Context
// class' accessibility level in Java and C#.
//
// Revision 1.1  2005/05/28 19:28:42  cwrapp
// Moved to visitor pattern.
//
// Revision 1.2  2005/02/21 15:34:38  charlesr
// Added Francois Perrad to Contributors section for Python work.
//
// Revision 1.1  2005/02/21 15:10:36  charlesr
// Modified isLoopback() to new signature.
//
// Revision 1.0  2005/02/03 17:10:08  charlesr
// Initial revision
//
