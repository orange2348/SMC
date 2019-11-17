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
// $Id: SmcVBGenerator.java,v 1.9 2013/07/14 14:32:38 cwrapp Exp $
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
 * Visits the abstract syntax tree, emitting VB.Net code.
 * @see SmcElement
 * @see SmcCodeGenerator
 * @see SmcVisitor
 * @see SmcOptions
 *
 * @author <a href="mailto:rapp@acm.org">Charles Rapp</a>
 */

public final class SmcVBGenerator
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
     * Creates a VB code generator for the given options.
     * @param options The target code generator options.
     */
    public SmcVBGenerator(final SmcOptions options)
    {
        super (options, TargetLanguage.VB.suffix());
    } // end of SmcVBGenerator(SmcOptions)

    //
    // end of Constructors.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // SmcVisitor Abstract Method Impelementation.
    //

    /**
     * Emits VB code for the finite state machine.
     * @param fsm emit VB code for this finite state machine.
     */
    @Override
    public void visit(final SmcFSM fsm)
    {
        String rawSource = fsm.getSource();
        String packageName = fsm.getPackage();
        String context = fsm.getContext();
        String fsmClassName = fsm.getFsmClassName();
        String startState = fsm.getStartState();
        List<SmcMap> maps = fsm.getMaps();
        List<SmcTransition> transitions;
        List<SmcParameter> params;
        Iterator<SmcParameter> pit;
        String vbState;
        String separator;
        int index;
        String indent2;

        // Dump out the raw target code, if any.
        if (rawSource != null && rawSource.length () > 0)
        {
            mTarget.println(rawSource);
            mTarget.println();
        }

        // If reflection is on, then import the .Net collections
        // package.
        if (mReflectFlag)
        {
            if (mGenericFlag == false)
            {
                mTarget.println("Imports System.Collections");
            }
            else
            {
                mTarget.println("Imports System.Collections.Generic");
            }
        }

        // Do user-specified imports now.
        for (String imp: fsm.getImports())
        {
            mTarget.print("Imports ");
            mTarget.println(imp);
        }

        // If serialization is on, then import the .Net
        // serialization package.
        if (mSerialFlag)
        {
            mTarget.println(
                "Imports System.Runtime.Serialization");
        }
        mTarget.println();

        // If a package has been specified, generate the package
        // statement now and set the indent.
        if (packageName != null && packageName.length() > 0)
        {
            mTarget.print("Namespace ");
            mTarget.println(packageName);
            mTarget.println();
            mIndent = "    ";
        }

        // If -serial was specified, then prepend the serialize
        // attribute to the class declaration.
        if (mSerialFlag)
        {
            mTarget.print(mIndent);
            mTarget.print("<Serializable()> ");
        }

        // Now declare the context class.
        mTarget.print(mIndent);
        mTarget.print("Public NotInheritable Class ");
        mTarget.print(fsmClassName);
        mTarget.println("");
        mTarget.print(mIndent);
        mTarget.println("    Inherits statemap.FSMContext");

        // If -serial was specified, then we also implement the
        // ISerializable interface.
        if (mSerialFlag)
        {
            mTarget.print(mIndent);
            mTarget.println("    Implements ISerializable");
        }

        // Declare the associated application class as a data
        // member.
        mTarget.println();
        mTarget.print(mIndent);
        mTarget.println(
            "    '------------------------------------------------------------");
        mTarget.print(mIndent);
        mTarget.println("    ' Member data");
        mTarget.print(mIndent);
        mTarget.println("    '");
        mTarget.print(mIndent);
        mTarget.println();
        mTarget.print(mIndent);
        mTarget.println(
            "    ' The associated application class instance.");
        mTarget.print(mIndent);
        mTarget.print("    Private _owner As ");
        mTarget.println(context);
        mTarget.println();

        // If serialization is on, then the shared state array
        // must be generated.
        if (mSerialFlag || mReflectFlag)
        {
            String mapName;

            mTarget.print(mIndent);
            mTarget.println(
                "    '------------------------------------------------------------");
            mTarget.print(mIndent);
            mTarget.println("    ' Shared data");
            mTarget.print(mIndent);
            mTarget.println("    '");
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.print("    ' State instance array. ");
            mTarget.println("Used to deserialize.");
            mTarget.print(mIndent);
            mTarget.print(
                "    Private Shared ReadOnly _States() As ");
            mTarget.print(context);
            mTarget.println("State = _");
            mTarget.print(mIndent);
            mTarget.print("        {");

            // For each map, ...
            separator = " _";
            for (SmcMap map: maps)
            {
                mapName = map.getName();

                // and for each map state, ...
                for (SmcState state: map.getStates())
                {
                    // Add its singleton instance to the array.
                    mTarget.println(separator);
                    mTarget.print(mIndent);
                    mTarget.print("            ");
                    mTarget.print(mapName);
                    mTarget.print('.');
                    mTarget.print(state.getClassName());

                    separator = ", _";
                }
            }

            mTarget.println(" _");
            mTarget.print(mIndent);
            mTarget.println("        }");
            mTarget.println();
        }

        // Now declare the current state and owner properties.
        mTarget.print(mIndent);
        mTarget.println(
            "    '------------------------------------------------------------");
        mTarget.print(mIndent);
        mTarget.println("    ' Properties");
        mTarget.print(mIndent);
        mTarget.println("    '");
        mTarget.println();
        mTarget.print(mIndent);
        mTarget.print("    Public Property State() As ");
        mTarget.print(context);
        mTarget.println("State");
        mTarget.print(mIndent);
        mTarget.println("        Get");
        mTarget.print(mIndent);
        mTarget.println("            If state_ Is Nothing _");
        mTarget.print(mIndent);
        mTarget.println("            Then");
        mTarget.print(mIndent);
        mTarget.print("                Throw ");
        mTarget.println(
            "New statemap.StateUndefinedException()");
        mTarget.print(mIndent);
        mTarget.println("            End If");
        mTarget.println();
        mTarget.print(mIndent);
        mTarget.println("            Return state_");
        mTarget.print(mIndent);
        mTarget.println("        End Get");
        mTarget.println();
        mTarget.print(mIndent);
        mTarget.print("        Set(ByVal state As ");
        mTarget.print(context);
        mTarget.println("State)");
        mTarget.println();
        mTarget.print(mIndent);
        mTarget.println("            state_ = state");
        mTarget.print(mIndent);
        mTarget.println("        End Set");
        mTarget.print(mIndent);
        mTarget.println("    End Property");
        mTarget.println();

        // The Owner property.
        mTarget.print(mIndent);
        mTarget.print(
            "    Public Property Owner() As ");
        mTarget.println(context);
        mTarget.print(mIndent);
        mTarget.println("        Get");
        mTarget.print(mIndent);
        mTarget.println("            Return _owner");
        mTarget.print(mIndent);
        mTarget.println("        End Get");
        mTarget.print(mIndent);
        mTarget.print("        Set(ByVal owner As ");
        mTarget.print(context);
        mTarget.println(")");
        mTarget.println();
        mTarget.print(mIndent);
        mTarget.println("            If owner Is Nothing _");
        mTarget.print(mIndent);
        mTarget.println("            Then");
        mTarget.print(mIndent);
        mTarget.println(
            "                Throw New NullReferenceException");
        mTarget.print(mIndent);
        mTarget.println("            End If");
        mTarget.println();
        mTarget.print(mIndent);
        mTarget.println("            _owner = owner");
        mTarget.print(mIndent);
        mTarget.println("        End Set");
        mTarget.print(mIndent);
        mTarget.println("    End Property");
        mTarget.println();

        // The states property.
        if (mReflectFlag)
        {
            mTarget.print(mIndent);
            mTarget.print(
                "    Public ReadOnly Property States() As ");
            mTarget.print(context);
            mTarget.println("State()");
            mTarget.print(mIndent);
            mTarget.println("        Get");
            mTarget.print(mIndent);
            mTarget.println("            Return _States");
            mTarget.print(mIndent);
            mTarget.println("        End Get");
            mTarget.print(mIndent);
            mTarget.println("    End Property");
            mTarget.println();
        }

        // The state name "map::state" must be changed to
        // "map.state".
        if ((index = startState.indexOf("::")) >= 0)
        {
            vbState = startState.substring(0, index) +
                      "." +
                      startState.substring(index + 2);
        }
        else
        {
            vbState = startState;
        }

        // Generate the class member methods, starting with the
        // constructor.
        mTarget.print(mIndent);
        mTarget.println(
            "    '------------------------------------------------------------");
        mTarget.print(mIndent);
        mTarget.println("    ' Member methods");
        mTarget.print(mIndent);
        mTarget.println("    '");
        mTarget.println();
        mTarget.print(mIndent);
        mTarget.print("    Public Sub New(ByRef owner As ");
        mTarget.print(context);
        mTarget.println(")");
        mTarget.println();
        mTarget.print(mIndent);
        mTarget.print("        MyBase.New(");
        mTarget.print(vbState);
        mTarget.println(")");
        mTarget.print(mIndent);
        mTarget.println("        _owner = owner");
        mTarget.print(mIndent);
        mTarget.println("    End Sub");
        mTarget.println();

        // Execute the start state's entry actions.
        mTarget.print(mIndent);
        mTarget.println("    Public Overrides Sub EnterStartState()");
        mTarget.println();
        mTarget.print(mIndent);
        mTarget.println("        State.Entry(Me)");
        mTarget.print(mIndent);
        mTarget.println("    End Sub");
        mTarget.println();

        // Generate the transition methods. These methods are all
        // formatted: set transition name, call the current
        // state's transition method, clear the transition name.
        transitions = fsm.getTransitions();
        for (SmcTransition trans: transitions)
        {
            // Ignore the default transition.
            if (trans.getName().equals("Default") == false)
            {
                mTarget.print(mIndent);
                mTarget.print("    Public Sub ");
                mTarget.print(trans.getName());
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
                mTarget.println();

                // If the -sync flag was specified, then output
                // "SyncLock Me" to prevent multiple threads from
                // access this state machine simultaneously.
                if (mSyncFlag)
                {
                    mTarget.print(mIndent);
                    mTarget.println("        SyncLock Me");
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
                mTarget.print(trans.getName());
                mTarget.println("\"");

                mTarget.print(indent2);
                mTarget.print("State.");
                mTarget.print(trans.getName());
                mTarget.print("(Me");

                for (SmcParameter param: params)
                {
                    mTarget.print(", ");
                    mTarget.print(param.getName());
                }
                mTarget.println(")");
                mTarget.print(indent2);
                mTarget.println("transition_ = \"\"");

                // If the -sync flag was specified, then output
                // the "End SyncLock".
                if (mSyncFlag)
                {
                    mTarget.print(mIndent);
                    mTarget.println("        End SyncLock");
                }

                mTarget.print(mIndent);
                mTarget.println("    End Sub");
                mTarget.println();
            }
        }

        // If serialization is one, then output the GetObjectData
        // and deserialize constructor.
        if (mSerialFlag)
        {
            // Output the ValueOf method.
            mTarget.print(mIndent);
            mTarget.print("    Public Function ValueOf(");
            mTarget.print("ByVal stateId As Integer) As ");
            mTarget.print(context);
            mTarget.println("State");
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.println(
                "        Return _States(stateId)");
            mTarget.print(mIndent);
            mTarget.println("    End Function");
            mTarget.println();

            mTarget.print(mIndent);
            mTarget.print("    Private Sub GetObjectData(");
            mTarget.println(
                "ByVal info As SerializationInfo, _");
            mTarget.print(mIndent);
            mTarget.print("                              ");
            mTarget.println(
                "ByVal context As StreamingContext) _");
            mTarget.print(mIndent);
            mTarget.print("            ");
            mTarget.println(
                "Implements ISerializable.GetObjectData");
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.println(
                "        Dim stackSize As Integer = 0");
            mTarget.print(mIndent);
            mTarget.println("        Dim index As Integer");
            mTarget.print(mIndent);
            mTarget.println("        Dim it As IEnumerator");
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.println(
                "        If Not IsNothing(stateStack_) _");
            mTarget.print(mIndent);
            mTarget.println("        Then");
            mTarget.print(mIndent);
            mTarget.println(
                "            stackSize = stateStack_.Count");
            mTarget.print(mIndent);
            mTarget.println(
                "            it = stateStack_.GetEnumerator()");
            mTarget.print(mIndent);
            mTarget.println("        End If");
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.print("        ");
            mTarget.println(
                "info.AddValue(\"stackSize\", stackSize)");
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.println("        index = 0");
            mTarget.print(mIndent);
            mTarget.println("        While index < stackSize");
            mTarget.print(mIndent);
            mTarget.println("            it.MoveNext()");
            mTarget.print(mIndent);
            mTarget.println("            info.AddValue( _");
            mTarget.print(mIndent);
            mTarget.print("                ");
            mTarget.println(
                "String.Concat(\"stackItem\", index), _");
            mTarget.print(mIndent);
            mTarget.println(
                "                              it.Current.Id)");
            mTarget.print(mIndent);
            mTarget.println("            index += 1");
            mTarget.print(mIndent);
            mTarget.println("        End While");
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.println(
                "        info.AddValue(\"state\", state_.Id)");
            mTarget.print(mIndent);
            mTarget.println("    End Sub");
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.print("    Private Sub New(");
            mTarget.println(
                "ByVal info As SerializationInfo, _");
            mTarget.print(mIndent);
            mTarget.print("                    ");
            mTarget.println(
                "ByVal context As StreamingContext)");
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.println("        MyBase.New(Nothing)");
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.println("        Dim stackSize As Integer");
            mTarget.print(mIndent);
            mTarget.println("        Dim stateId As Integer");
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.print("        stackSize = ");
            mTarget.println("info.GetInt32(\"stackSize\")");
            mTarget.print(mIndent);
            mTarget.println("        If stackSize > 0 _");
            mTarget.print(mIndent);
            mTarget.println("        Then");
            mTarget.print(mIndent);
            mTarget.println("            Dim index As Integer");
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.println(
                "            stateStack_ = New Stack()");
            mTarget.print(mIndent);
            mTarget.println(
                "            index = stackSize - 1");
            mTarget.print(mIndent);
            mTarget.println(
                "            While index >= 0");
            mTarget.print(mIndent);
            mTarget.println(
                "                stateId = _");
            mTarget.print(mIndent);
            mTarget.println(
                "                    info.GetInt32( _");
            mTarget.print(mIndent);
            mTarget.print("                        ");
            mTarget.println(
                "String.Concat(\"stackItem\", index))");
            mTarget.print(mIndent);
            mTarget.print("                    ");
            mTarget.println(
                "stateStack_.Push(_States(stateId))");
            mTarget.print(mIndent);
            mTarget.println(
                "                    index -= 1");
            mTarget.print(mIndent);
            mTarget.println(
                "            End While");
            mTarget.print(mIndent);
            mTarget.println("        End If");
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.println(
                "        stateId = info.GetInt32(\"state\")");
            mTarget.print(mIndent);
            mTarget.println("        state_ = _States(stateId)");
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.println("    End Sub");
            mTarget.println();
        }

        // The context class declaration is complete.
        mTarget.print(mIndent);
        mTarget.println("End Class");
        mTarget.println();

        // Declare the root application state class.
        mTarget.print(mIndent);
        mTarget.print("Public MustInherit Class ");
        mTarget.print(context);
        mTarget.println("State");
        mTarget.print(mIndent);
        mTarget.println("    Inherits statemap.State");
        mTarget.println();

        // If reflection is on, then generate the abstract
        // Transitions property.
        if (mReflectFlag)
        {
            mTarget.print(mIndent);
            mTarget.println(
                "    '------------------------------------------------------------");
            mTarget.print(mIndent);
            mTarget.println("    ' Properties");
            mTarget.print(mIndent);
            mTarget.println("    '");
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.print("    Public MustOverride ReadOnly ");
            mTarget.print(
                "Property Transitions() As IDictionary");
            if (mGenericFlag)
            {
                mTarget.print("(Of String, Integer)");
            }
            mTarget.println();
            mTarget.println();
        }

        mTarget.print(mIndent);
        mTarget.println(
            "    '------------------------------------------------------------");
        mTarget.print(mIndent);
        mTarget.println("    ' Member methods");
        mTarget.print(mIndent);
        mTarget.println("    '");
        mTarget.println();
        mTarget.print(mIndent);
        mTarget.print("    Protected Sub New(");
        mTarget.println(
            "ByVal name As String, ByVal id As Integer)");
        mTarget.println();
        mTarget.print(mIndent);
        mTarget.println("        MyBase.New(name, id)");
        mTarget.print(mIndent);
        mTarget.println("    End Sub");
        mTarget.println();
        mTarget.print(mIndent);
        mTarget.print("    Public Overridable Sub Entry(");
        mTarget.print("ByRef context As ");
        mTarget.print(fsmClassName);
        mTarget.println(")");
        mTarget.print(mIndent);
        mTarget.println("    End Sub");
        mTarget.println();
        mTarget.print(mIndent);
        mTarget.print("    Public Overridable Sub Exit_(");
        mTarget.print("ByRef context As ");
        mTarget.print(fsmClassName);
        mTarget.println(")");
        mTarget.print(mIndent);
        mTarget.println("    End Sub");
        mTarget.println();

        // Generate the default transition definitions.
        for (SmcTransition trans: transitions)
        {
            // Don't generate the Default transition here.
            if (trans.getName().equals("Default") == false)
            {
                mTarget.print(mIndent);
                mTarget.print("    Public Overridable Sub ");
                mTarget.print(trans.getName());
                mTarget.print("(ByRef context As ");
                mTarget.print(fsmClassName);
                mTarget.print("");

                for (SmcParameter param: trans.getParameters())
                {
                    mTarget.print(", ");
                    param.accept(this);
                }

                mTarget.println(")");
                mTarget.println();

                // If this method is reached, that means that
                // this transition was passed to a state which
                // does not define the transition. Call the
                // state's default transition method.
                // Note: "Default" is a VB keyword, so use
                // "Default_" instead.
                mTarget.print(mIndent);
                mTarget.println("        Default_(context)");

                mTarget.print(mIndent);
                mTarget.println("    End Sub");
                mTarget.println();
            }
        }

        // Generate the overall Default transition for all maps.
        // Note: "Default" is a VB keyword, so use "Default_"
        // instead.
        mTarget.print(mIndent);
        mTarget.print("    Public Overridable Sub Default_(");
        mTarget.print("ByRef context As ");
        mTarget.print(fsmClassName);
        mTarget.println(")");
        mTarget.println();

        if (mDebugLevel >= DEBUG_LEVEL_0)
        {
            mTarget.println("#If TRACE Then");
            mTarget.print(mIndent);
            mTarget.print("        Trace.WriteLine(");
            mTarget.println("\"TRANSITION   : Default\")");
            mTarget.println("#End If");
            mTarget.println();
        }

        mTarget.print(mIndent);
        mTarget.print("        Throw ");
        mTarget.println(
            "New statemap.TransitionUndefinedException( _");
        mTarget.print(mIndent);
        mTarget.println(
            "            String.Concat(\"State: \", _");
        mTarget.print(mIndent);
        mTarget.println("               context.State.Name, _");
        mTarget.print(mIndent);
        mTarget.println("               \", Transition: \", _");
        mTarget.print(mIndent);
        mTarget.println(
            "               context.GetTransition()))");
        mTarget.print(mIndent);
        mTarget.println("    End Sub");

        // End of the application class' state class.
        mTarget.print(mIndent);
        mTarget.println("End Class");

        // Have each map print out its target code now.
        for (SmcMap map: maps)
        {
            map.accept(this);
        }

        // If a package has been specified, then generate
        // the End tag now.
        if (packageName != null && packageName.length() > 0)
        {
            mTarget.println();
            mTarget.println("End Namespace");
        }

        return;
    } // end of visit(SmcFSM)

    /**
     * Emits VB code for the FSM map.
     * @param map emit VB code for this map.
     */
    @Override
    public void visit(final SmcMap map)
    {
        List<SmcTransition> definedDefaultTransitions;
        SmcState defaultState = map.getDefaultState();
        String context = map.getFSM().getContext();
        String mapName = map.getName();
        List<SmcState> states = map.getStates();
        String stateName;

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

        // Declare the map class. Declare it as abstract to
        // prevent its instantiation.
        mTarget.println();
        mTarget.print(mIndent);
        mTarget.print("Public MustInherit Class ");
        mTarget.println(mapName);
        mTarget.println();
        mTarget.print(mIndent);
        mTarget.println(
            "    '------------------------------------------------------------");
        mTarget.print(mIndent);
        mTarget.println("    ' Shared data");
        mTarget.print(mIndent);
        mTarget.println("    '");
        mTarget.println();

        // Declare each of the state class member data.
        for (SmcState state: states)
        {
            stateName = state.getClassName();

            mTarget.print(mIndent);
            mTarget.print("    Public Shared ");
            mTarget.print(state.getInstanceName());
            mTarget.print(" As ");
            mTarget.print(mapName);
            mTarget.print('_');
            mTarget.print(stateName);
            mTarget.println(" = _");
            mTarget.print(mIndent);
            mTarget.print("        New ");
            mTarget.print(mapName);
            mTarget.print('_');
            mTarget.print(stateName);
            mTarget.print("(\"");
            mTarget.print(mapName);
            mTarget.print('.');
            mTarget.print(stateName);
            mTarget.print("\", ");
            mTarget.print(SmcMap.getNextStateId());
            mTarget.println(")");
        }

        // Create a default state as well.
        mTarget.print(mIndent);
        mTarget.print("    Private Shared Default_ As ");
        mTarget.print(mapName);
        mTarget.println("_Default = _");
        mTarget.print(mIndent);
        mTarget.print("        New ");
        mTarget.print(mapName);
        mTarget.print("_Default(\"");
        mTarget.print(mapName);
        mTarget.println(".Default\", -1)");
        mTarget.println();

        // End of the map class.
        mTarget.print(mIndent);
        mTarget.println("End Class");
        mTarget.println();

        // Declare the map default state class.
        mTarget.print(mIndent);
        mTarget.print("Public Class ");
        mTarget.print(mapName);
        mTarget.println("_Default");
        mTarget.print(mIndent);
        mTarget.print("    Inherits ");
        mTarget.print(context);
        mTarget.println("State");
        mTarget.println();

        // If reflection is on, generate the Transition property.
        if (mReflectFlag)
        {
            mTarget.print(mIndent);
            mTarget.println(
                "    '------------------------------------------------------------");
            mTarget.print(mIndent);
            mTarget.println("    ' Properties");
            mTarget.print(mIndent);
            mTarget.println("    '");
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.print("    Public Overrides ReadOnly ");
            mTarget.print(
                "Property Transitions() As IDictionary");
            if (mGenericFlag)
            {
                mTarget.print("(of String, Integer)");
            }
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.println("        Get");
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.println("            Return _transitions");
            mTarget.print(mIndent);
            mTarget.println("        End Get");
            mTarget.print(mIndent);
            mTarget.println("    End Property");
            mTarget.println();
        }

        mTarget.print(mIndent);
        mTarget.println(
            "    '------------------------------------------------------------");
        mTarget.print(mIndent);
        mTarget.println("    ' Member methods");
        mTarget.print(mIndent);
        mTarget.println("    '");
        mTarget.println();

        // Generate the constructor.
        mTarget.print(mIndent);
        mTarget.print("    Public Sub New(");
        mTarget.println(
            "ByVal name As String, ByVal id As Integer)");
        mTarget.println();
        mTarget.print(mIndent);
        mTarget.println("        MyBase.New(name, id)");
        mTarget.print(mIndent);
        mTarget.println("    End Sub");
        mTarget.println();

        // Declare the user-defined default transitions first.
        for (SmcTransition transition: definedDefaultTransitions)
        {
            transition.accept(this);
        }

        // If reflection is on, then define the transitions list.
        if (mReflectFlag)
        {
            List<SmcTransition> allTransitions =
                map.getFSM().getTransitions();
            String transName;
            int transDefinition;

            mTarget.println();
            mTarget.print(mIndent);
            mTarget.println(
                "    '------------------------------------------------------------");
            mTarget.print(mIndent);
            mTarget.println("    ' Shared data");
            mTarget.print(mIndent);
            mTarget.println("    '");
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.print("    ");
            mTarget.print(
                "Private Shared _transitions As IDictionary");
            if (mGenericFlag)
            {
                mTarget.print("(Of String, Integer)");
            }
            mTarget.println();
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.println("    Shared Sub New()");
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.print("        ");
            mTarget.print("_transitions = New ");
            if (mGenericFlag == false)
            {
                mTarget.print("Hashtable");
            }
            else
            {
                mTarget.print("Dictionary(Of String, Integer)");
            }
            mTarget.println("()");

            // Now place the transition names into the list.
            for (SmcTransition transition: allTransitions)
            {
                transName = transition.getName();

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

                mTarget.print(mIndent);
                mTarget.print("        ");
                mTarget.print("_transitions.Add(\"");
                mTarget.print(transName);
                mTarget.print("\", ");
                mTarget.print(transDefinition);
                mTarget.println(")");
            }
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.println("    End Sub");
        }

        mTarget.print(mIndent);
        mTarget.println("End Class");

        // Have each state now generate its code. Each state
        // class is an inner class.
        for (SmcState state: states)
        {
            state.accept(this);
        }

        return;
    } // end of visit(SmcMap)

    /**
     * Emits VB code for this FSM state.
     * @param state emits VB code for this state.
     */
    @Override
    public void visit(final SmcState state)
    {
        SmcMap map = state.getMap();
        String context = map.getFSM().getContext();
        String fsmClassName = map.getFSM().getFsmClassName();
        String mapName = map.getName();
        List<SmcAction> actions;

        // Declare the state class.
        mTarget.println();
        mTarget.print(mIndent);
        mTarget.print("Public NotInheritable Class ");
        mTarget.print(mapName);
        mTarget.print('_');
        mTarget.println(state.getClassName());
        mTarget.print(mIndent);
        mTarget.print("    Inherits ");
        mTarget.print(mapName);
        mTarget.println("_Default");
        mTarget.println();

        // Generate the Transitions property if reflection is on.
        if (mReflectFlag)
        {
            mTarget.print(mIndent);
            mTarget.println(
                "    '------------------------------------------------------------");
            mTarget.print(mIndent);
            mTarget.println("    ' Properties");
            mTarget.print(mIndent);
            mTarget.println("    '");
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.print("    Public Overrides ReadOnly ");
            mTarget.print(
                "Property Transitions() As IDictionary");
            if (mGenericFlag)
            {
                mTarget.print("(Of String, Integer)");
            }
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.println("        Get");
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.println("            Return _transitions");
            mTarget.print(mIndent);
            mTarget.println("        End Get");
            mTarget.print(mIndent);
            mTarget.println("    End Property");
            mTarget.println();
        }

        mTarget.print(mIndent);
        mTarget.println(
            "    '------------------------------------------------------------");
        mTarget.print(mIndent);
        mTarget.println("    ' Member methods");
        mTarget.print(mIndent);
        mTarget.println("    '");
        mTarget.println();

        // Add the constructor.
        mTarget.print(mIndent);
        mTarget.print("    Public Sub New(");
        mTarget.println(
            "ByVal name As String, ByVal id As Integer)");
        mTarget.println();
        mTarget.print(mIndent);
        mTarget.println("        MyBase.New(name, id)");
        mTarget.print(mIndent);
        mTarget.println("    End Sub");

        // Add the Entry() and Exit() member functions if this
        // state defines them.
        actions = state.getEntryActions();
        if (actions != null && actions.size() > 0)
        {
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.print("    Public Overrides Sub Entry(");
            mTarget.print("ByRef context As ");
            mTarget.print(fsmClassName);
            mTarget.println(")");
            mTarget.println();

            // Declare the "ctxt" local variable.
            mTarget.print(mIndent);
            mTarget.print("       Dim ctxt As ");
            mTarget.print(context);
            mTarget.println(" = context.Owner");
            mTarget.println();

            // Generate the actions associated with this code.
            for (SmcAction action: actions)
            {
                action.accept(this);
            }

            mTarget.print(mIndent);
            mTarget.println("    End Sub");
        }

        actions = state.getExitActions();
        if (actions != null && actions.size() > 0)
        {
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.print("    Public Overrides Sub Exit(");
            mTarget.print("ByRef context As ");
            mTarget.print(fsmClassName);
            mTarget.println(")");
            mTarget.println();

            // Declare the "ctxt" local variable.
            mTarget.print(mIndent);
            mTarget.print("        Dim ctxt As ");
            mTarget.print(context);
            mTarget.println(" = context.Owner");
            mTarget.println();

            // Generate the actions associated with this code.
            for (SmcAction action: actions)
            {
                action.accept(this);
            }

            mTarget.print(mIndent);
            mTarget.println("    End Sub");
        }

        // Have each transition generate its code.
        for (SmcTransition transition: state.getTransitions())
        {
            transition.accept(this);
        }

        // If reflection is on, then generate the transitions
        // list.
        if (mReflectFlag)
        {
            List<SmcTransition> allTransitions =
                map.getFSM().getTransitions();
            List<SmcTransition> stateTransitions =
                state.getTransitions();
            List<SmcTransition> defaultTransitions;
            SmcState defaultState = map.getDefaultState();
            String transName;
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
                "    '------------------------------------------------------------");
            mTarget.print(mIndent);
            mTarget.println("    ' Shared data");
            mTarget.print(mIndent);
            mTarget.println("    '");
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.print("    ");
            mTarget.print(
                "Private Shared _transitions As IDictionary");
            if (mGenericFlag)
            {
                mTarget.print("(Of String, Integer)");
            }
            mTarget.println();
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.println("    Shared Sub New()");
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.print("        ");
            mTarget.print("_transitions = New ");
            if (mGenericFlag == false)
            {
                mTarget.print("Hashtable");
            }
            else
            {
                mTarget.print("Dictionary(Of String, Integer)");
            }
            mTarget.println("()");

            // Now place the transition names into the list.
            for (SmcTransition transition: allTransitions)
            {
                transName = transition.getName();

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

                mTarget.print(mIndent);
                mTarget.print("        ");
                mTarget.print("_transitions.Add(\"");
                mTarget.print(transName);
                mTarget.print("\", ");
                mTarget.print(transDefinition);
                mTarget.println(")");
            }

            mTarget.println();
            mTarget.print(mIndent);
            mTarget.println("    End Sub");
        }

        mTarget.print(mIndent);
        mTarget.println("End Class");

        return;
    } // end of visit(SmcState)

    /**
     * Emits VB code for this FSM state transition.
     * @param transition emits VB code for this state transition.
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
        Iterator<SmcGuard> git = guards.iterator();
        SmcGuard guard;
        SmcGuard nullGuard = null;

        mTarget.println();
        mTarget.print(mIndent);
        mTarget.print("    Public Overrides Sub ");

        // If this is the Default transition, then change its
        // name to "Default_" because Default is a VB keyword.
        if (transName.equals("Default"))
        {
            mTarget.print("Default_");
        }
        else
        {
            mTarget.print(transName);
        }

        mTarget.print("(ByRef context As ");
        mTarget.print(fsmClassName);
        mTarget.print("");

        // Add user-defined parameters.
        for (SmcParameter param: parameters)
        {
            mTarget.print(", ");
            param.accept(this);
        }
        mTarget.println(")");
        mTarget.println();

        // Generate the ctxt local variable if needed.
        if (transition.hasCtxtReference())
        {
            mTarget.print(mIndent);
            mTarget.print("        Dim ctxt As ");
            mTarget.print(context);
            mTarget.println(" = context.Owner");
        }

        // Output transition to debug stream.
        if (mDebugLevel >= DEBUG_LEVEL_0)
        {
            mTarget.println("#If TRACE Then");
            mTarget.print(mIndent);
            mTarget.println("        Trace.WriteLine( _");
            mTarget.print(mIndent);
            mTarget.print("            \"LEAVING STATE   : ");
            mTarget.print(mapName);
            mTarget.print(".");
            mTarget.print(stateName);
            mTarget.println("\")");
            mTarget.println("#End If");
            mTarget.println();
        }

        // Loop through the guards and print each one.
        mGuardIndex = 0;
        mGuardCount = guards.size();
        while (git.hasNext())
        {
            guard = git.next();

            // Count up the guards with no condition.
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
            mTarget.println();
            mTarget.print(mIndent);
            mTarget.println("        Else");
            mTarget.print(mIndent);
            mTarget.print("            MyBase.");
            mTarget.print(transName);
            mTarget.print("(context");

            for (SmcParameter param: parameters)
            {
                mTarget.print(", ");
                mTarget.print(param.getName());
            }

            mTarget.println(")");
            mTarget.print(mIndent);
            mTarget.println("        End If");
        }
        // Need to add a final newline after a multiguard block.
        else if (mGuardCount > 1)
        {
            mTarget.print(mIndent);
            mTarget.println("        End If");
            mTarget.println();
        }

        mTarget.print(mIndent);
        mTarget.println("    End Sub");

        return;
    } // end of visit(SmcTransition)

    /**
     * Emits VB code for this FSM transition guard.
     * @param guard emits VB code for this transition guard.
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
        String indent1;
        String indent2;
        String endStateName = guard.getEndState();
        String fqEndStateName;
        String pushStateName = guard.getPushState();
        String condition = guard.getCondition();
        List<SmcAction> actions = guard.getActions();

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

        stateName = scopeStateName(stateName, mapName);
        pushStateName = scopeStateName(pushStateName, mapName);

        loopbackFlag = isLoopback(transType, endStateName);

        // The guard code generation is a bit tricky. The first
        // question is how many guards are there? If there are
        // more than one, then we will need to generate the
        // proper "if-then-else" code.
        if (mGuardCount > 1)
        {
            indent1 = mIndent + "            ";

            // There are multiple guards.
            // Is this the first guard?
            if (mGuardIndex == 0 && condition.length() > 0)
            {
                // Yes, this is the first. This means an "if"
                // should be used.
                mTarget.print(mIndent);
                mTarget.print("        If ");
                mTarget.print(condition);
                mTarget.println(" _");
                mTarget.print(mIndent);
                mTarget.println("        Then");
            }
            else if (condition.length() > 0)
            {
                // No, this is not the first transition but it
                // does have a condition. Use an "else if".
                mTarget.println();
                mTarget.print(mIndent);
                mTarget.print("        ElseIf ");
                mTarget.print(condition);
                mTarget.println(" _");
                mTarget.print(mIndent);
                mTarget.println("        Then");
            }
            else
            {
                // This is not the first transition and it has
                // no condition.
                mTarget.println();
                mTarget.print(mIndent);
                mTarget.println("        Else");
            }
        }
        else
        {
            // There is only one guard. Does this guard have
            // a condition?
            if (condition.length() == 0)
            {
                // No. This is a plain, old. vanilla transition.
                indent1 = mIndent + "        ";
            }
            else
            {
                // Yes there is a condition.
                indent1 = mIndent + "            ";
                mTarget.print(mIndent);
                mTarget.print("        If ");
                mTarget.print(condition);
                mTarget.println(" _");
                mTarget.print(mIndent);
                mTarget.println("        Then");
            }
        }

        // Now that the necessary conditions are in place, it's
        // time to dump out the transition's actions. First, do
        // the proper handling of the state change. If this
        // transition has no actions, then set the end state
        // immediately. Otherwise, unset the current state so
        // that if an action tries to issue a transition, it will
        // fail.
        if (actions.isEmpty())
        {
            fqEndStateName = endStateName;
        }
        // Save away the current state if this is a loopback
        // transition. Storing current state allows the
        // current state to be cleared before any actions are
        // executed. Remember: actions are not allowed to
        // issue transitions and clearing the current state
        // prevents them from doing do.
        else if (loopbackFlag)
        {
            fqEndStateName = "endState";
            mTarget.print(mIndent);
            mTarget.print("Dim ");
            mTarget.print(fqEndStateName);
            mTarget.print(" As ");
            mTarget.print(context);
            mTarget.println("State = context.State");
            mTarget.println();
        }
        else
        {
            fqEndStateName = endStateName;
        }

        // Dump out the exit actions - but only for the first
        // guard.
        // v. 1.0, beta 3: Not any more. The exit actions are
        // executed only if 1) this is a standard, non-loopback
        // transition or a pop transition.
        if (transType == TransType.TRANS_POP ||
            loopbackFlag == false)
        {
            if (mDebugLevel >= DEBUG_LEVEL_1)
            {
                mTarget.println("#If TRACE Then");
                mTarget.print(indent1);
                mTarget.println("Trace.WriteLine( _");
                mTarget.print(indent1);
                mTarget.print("    \"BEFORE EXIT     : ");
                mTarget.print(stateName);
                mTarget.println(".Exit_(context)\")");
                mTarget.println("#End If");
                mTarget.println();
            }

            mTarget.print(indent1);
            mTarget.println("context.State.Exit_(context)");

            if (mDebugLevel >= DEBUG_LEVEL_1)
            {
                mTarget.println("#If TRACE Then");
                mTarget.print(indent1);
                mTarget.println("Trace.WriteLine( _");
                mTarget.print(indent1);
                mTarget.print("    \"AFTER EXIT      : ");
                mTarget.print(stateName);
                mTarget.println(".Exit_(context)\")");
                mTarget.println("#End If");
                mTarget.println();
            }
        }

        if (mDebugLevel >= DEBUG_LEVEL_0)
        {
            List<SmcParameter> parameters =
                transition.getParameters();
            Iterator<SmcParameter> pit;
            String sep;

            mTarget.println("#If TRACE Then");
            mTarget.print(indent1);
            mTarget.println("Trace.WriteLine( _");
            mTarget.print(indent1);
            mTarget.print("    \"ENTER TRANSITION: ");
            mTarget.print(stateName);
            mTarget.print(".");
            mTarget.print(transName);
            mTarget.print("(");

            for (pit = parameters.iterator(), sep = "";
                 pit.hasNext();
                 sep = ", ")
            {
                mTarget.print(sep);
                (pit.next()).accept(this);
            }

            mTarget.println(")\")");
            mTarget.println("#End If");
            mTarget.println();
        }

        // Dump out this transition's actions.
        if (actions.isEmpty())
        {
            if (condition.length() > 0)
            {
                mTarget.print(indent1);
                mTarget.println("' No actions.");
            }

            indent2 = indent1;
        }
        else
        {
            String tempIndent;

            // Now that we are in the transition, clear the
            // current state.
            mTarget.print(indent1);
            mTarget.println("context.ClearState()");

            // v. 2.0.0: Place the actions inside a try/finally
            // block. This way the state will be set before an
            // exception leaves the transition method.
            // v. 2.2.0: Check if the user has turned off this
            // feature first.
            if (mNoCatchFlag == false)
            {
                mTarget.print(indent1);
                mTarget.println("Try");

                indent2 = indent1 + "    ";
            }
            else
            {
                indent2 = indent1;
            }

            tempIndent = mIndent;
            mIndent = indent2;
            for (SmcAction action: actions)
            {
                action.accept(this);
            }
            mIndent = tempIndent;

            // v. 2.2.0: Check if the user has turned off this
            // feature first.
            if (mNoCatchFlag == false)
            {
                mTarget.print(indent1);
                mTarget.println("Finally");
            }
        }

        if (mDebugLevel >= DEBUG_LEVEL_0)
        {
            List<SmcParameter> parameters =
                transition.getParameters();
            Iterator<SmcParameter> pit;
            String sep;

            mTarget.println("#If TRACE Then");
            mTarget.print(indent2);
            mTarget.println("Trace.WriteLine( _");
            mTarget.print(indent2);
            mTarget.print("    \"EXIT TRANSITION : ");
            mTarget.print(stateName);
            mTarget.print(".");
            mTarget.print(transName);
            mTarget.print("(");

            for (pit = parameters.iterator(), sep = "";
                 pit.hasNext();
                 sep = ", ")
            {
                mTarget.print(sep);
                (pit.next()).accept(this);
            }

            mTarget.println(")\")");
            mTarget.println("#End If");
            mTarget.println();
        }

        // Print the setState() call, if necessary. Do NOT
        // generate the set state it:
        // 1. The transition has no actions AND is a loopback OR
        // 2. This is a push or pop transition.
        if (transType == TransType.TRANS_SET &&
            (actions.size() > 0 || loopbackFlag == false))
        {
            mTarget.print(indent2);
            mTarget.print("context.State = ");
            mTarget.println(fqEndStateName);
        }
        else if (transType == TransType.TRANS_PUSH)
        {
            // Set the next state so this it can be pushed
            // onto the state stack. But only do so if a clear
            // state was done.
            if (loopbackFlag == false || actions.size() > 0)
            {
                mTarget.print(indent2);
                mTarget.print("context.State = ");
                mTarget.println(fqEndStateName);
            }

            // Before doing the push, execute the end state's
            // entry actions (if any) if this is not a loopback.
            if (loopbackFlag == false)
            {
                if (mDebugLevel >= DEBUG_LEVEL_1)
                {
                    mTarget.println("#If TRACE Then");
                    mTarget.print(indent1);
                    mTarget.println("Trace.WriteLine( _");
                    mTarget.print(indent1);
                    mTarget.print("    \"BEFORE ENTRY    : ");
                    mTarget.print(fqEndStateName);
                    mTarget.println(".Entry(context)\")");
                    mTarget.println("#End If");
                    mTarget.println();
                }

                mTarget.print(indent2);
                mTarget.println("context.State.Entry(context)");

                if (mDebugLevel >= DEBUG_LEVEL_1)
                {
                    mTarget.println("#If TRACE Then");
                    mTarget.print(indent1);
                    mTarget.println("Trace.WriteLine( _");
                    mTarget.print(indent1);
                    mTarget.print("    \"AFTER ENTRY     : ");
                    mTarget.print(fqEndStateName);
                    mTarget.println(".Entry(context)\")");
                    mTarget.println("#End If");
                    mTarget.println();
                }
            }

            mTarget.print(indent2);
            mTarget.print("context.PushState(");
            mTarget.print(pushStateName);
            mTarget.println(")");
        }
        else if (transType == TransType.TRANS_POP)
        {
            mTarget.print(indent1);
            mTarget.println("context.PopState()");
        }

        // Perform the new state's enty actions.
        // v. 1.0, beta 3: Not any more. The entry actions are
        // executed only if 1) this is a standard, non-loopback
        // transition or a push transition.
        if ((transType == TransType.TRANS_SET &&
             loopbackFlag == false) ||
             transType == TransType.TRANS_PUSH)
        {
            if (mDebugLevel >= DEBUG_LEVEL_1)
            {
                mTarget.println("#If TRACE Then");
                mTarget.print(indent2);
                mTarget.println("Trace.WriteLine( _");
                mTarget.print(indent2);
                mTarget.print("    \"BEFORE ENTRY    : ");
                mTarget.print(fqEndStateName);
                mTarget.println(".Entry(context)\")");
                mTarget.println("#End If");
                mTarget.println();
            }

            mTarget.print(indent2);
            mTarget.println("context.State.Entry(context)");

            if (mDebugLevel >= DEBUG_LEVEL_1)
            {
                mTarget.println("#If TRACE Then");
                mTarget.print(indent2);
                mTarget.println("Trace.WriteLine( _");
                mTarget.print(indent2);
                mTarget.print("    \"AFTER ENTRY     : ");
                mTarget.print(fqEndStateName);
                mTarget.println(".Entry(context)\")");
                mTarget.println("#End If");
                mTarget.println();
            }
        }

        // If there was a try/finally, then put the closing
        // brace on the finally block.
        // v. 2.2.0: Check if the user has turned off this
        // feature first.
        if (actions.size() > 0 && mNoCatchFlag == false)
        {
            mTarget.print(indent1);
            mTarget.println("End Try");
        }

        // If there is a transition associated with the pop, then
        // issue that transition here.
        if (transType == TransType.TRANS_POP &&
            endStateName.equals(SmcElement.NIL_STATE) == false &&
            endStateName.length() > 0)
        {
            String popArgs = guard.getPopArgs();

            mTarget.println();
            mTarget.print(indent1);
            mTarget.print("context.");
            mTarget.print(endStateName);
            mTarget.print("(");

            // Output any and all pop arguments.
            if (popArgs.length() > 0)
            {
                mTarget.print(popArgs);
            }
            mTarget.println(")");
        }

        return;
    } // end of visit(SmcGuard)

    /**
     * Emits VB code for this FSM action.
     * @param action emits VB code for this action.
     */
    @Override
    public void visit(final SmcAction action)
    {
        String name = action.getName();
        List<String> arguments = action.getArguments();
        Iterator<String> it;
        String sep;

        // Need to distinguish between FSMContext actions and
        // application class actions. If the action is
        // "emptyStateStack", then pass it to the context.
        // Otherwise, let the application class handle it.
        mTarget.print(mIndent);

        if (action.isEmptyStateStack())
        {
            mTarget.println("context.EmptyStateStack()");
        }
        // If this is a property assignment, then strip the
        // semicolon from the argument's end.
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
	            mTarget.println(arg.substring(0, arg.indexOf(';')));
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

	            mTarget.println(")");
	        }
        }

        return;
    } // end of visit(SmcAction)

    /**
     * Emits VB code for this transition parameter.
     * @param parameter emits VB code for this transition
     * parameter.
     */
    @Override
    public void visit(final SmcParameter parameter)
    {
        mTarget.print("ByVal ");
        mTarget.print(parameter.getName());
        mTarget.print(" As ");
        mTarget.print(parameter.getType());

        return;
    } // end of visit(SmcParameter)

    //
    // end of SmcVisitor Abstract Method Impelementation.
    //-----------------------------------------------------------
} // end of class SmcVBGenerator

//
// CHANGE LOG
// $Log: SmcVBGenerator.java,v $
// Revision 1.9  2013/07/14 14:32:38  cwrapp
// check in for release 6.2.0
//
// Revision 1.8  2011/11/20 14:58:33  cwrapp
// Check in for SMC v. 6.1.0
//
// Revision 1.7  2009/12/17 19:51:43  cwrapp
// Testing complete.
//
// Revision 1.6  2009/11/25 22:30:19  cwrapp
// Fixed problem between %fsmclass and sm file names.
//
// Revision 1.5  2009/11/24 20:42:39  cwrapp
// v. 6.0.1 update
//
// Revision 1.4  2009/10/06 15:31:59  kgreg99
// 1. Started implementation of feature request #2718920.
//     1.1 Added method boolean isStatic() to SmcAction class. It returns false now, but is handled in following language generators: C#, C++, java, php, VB. Instance identificator is not added in case it is set to true.
// 2. Resolved confusion in "emtyStateStack" keyword handling. This keyword was not handled in the same way in all the generators. I added method boolean isEmptyStateStack() to SmcAction class. This method is used instead of different string comparisons here and there. Also the generated method name is fixed, not to depend on name supplied in the input sm file.
//
// Revision 1.3  2009/09/12 21:44:49  kgreg99
// Implemented feature req. #2718941 - user defined generated class name.
// A new statement was added to the syntax: %fsmclass class_name
// It is optional. If not used, generated class is called as before "XxxContext" where Xxx is context class name as entered via %class statement.
// If used, generated class is called asrequested.
// Following language generators are touched:
// c, c++, java, c#, objc, lua, groovy, scala, tcl, VB
// This feature is not tested yet !
// Maybe it will be necessary to modify also the output file name.
//
// Revision 1.2  2009/09/05 15:39:20  cwrapp
// Checking in fixes for 1944542, 1983929, 2731415, 2803547 and feature 2797126.
//
// Revision 1.1  2009/03/01 18:20:42  cwrapp
// Preliminary v. 6.0.0 commit.
//
// Revision 1.10  2008/03/21 14:03:17  fperrad
// refactor : move from the main file Smc.java to each language generator the following data :
//  - the default file name suffix,
//  - the file name format for the generated SMC files
//
// Revision 1.9  2007/02/21 13:57:07  cwrapp
// Moved Java code to release 1.5.0
//
// Revision 1.8  2007/01/15 00:23:52  cwrapp
// Release 4.4.0 initial commit.
//
// Revision 1.7  2006/09/23 14:28:19  cwrapp
// Final SMC, v. 4.3.3 check-in.
//
// Revision 1.6  2006/09/16 15:04:29  cwrapp
// Initial v. 4.3.3 check-in.
//
// Revision 1.5  2006/07/11 18:18:37  cwrapp
// Corrected owner property.
//
// Revision 1.4  2006/06/03 19:39:25  cwrapp
// Final v. 4.3.1 check in.
//
// Revision 1.3  2005/11/07 19:34:54  cwrapp
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
// Revision 1.2  2005/08/26 15:21:34  cwrapp
// Final commit for release 4.2.0. See README.txt for more information.
//
// Revision 1.1  2005/05/28 19:28:43  cwrapp
// Moved to visitor pattern.
//
// Revision 1.2  2005/02/21 15:38:51  charlesr
// Added Francois Perrad to Contributors section for Python work.
//
// Revision 1.1  2005/02/21 15:23:02  charlesr
// Modified isLoopback() method call to new signature due to moving
// the method from SmcGuard to SmcCodeGenerator.
//
// Revision 1.0  2005/02/03 17:12:57  charlesr
// Initial revision
//
