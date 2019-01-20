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
// Copyright (C) 2005, 2008. Charles W. Rapp.
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
// $Id: SmcTableGenerator.java,v 1.6 2010/03/05 21:29:53 fperrad Exp $
//
// CHANGE LOG
// (See the bottom of this file.)
//

package net.sf.smc.generator;

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
 * Visits the abstract syntax tree, emitting an HTML table.
 * @see SmcElement
 * @see SmcCodeGenerator
 * @see SmcVisitor
 * @see SmcOptions
 *
 * @author <a href="mailto:rapp@acm.org">Charles Rapp</a>
 */

public final class SmcTableGenerator
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
     * Creates a HTML table code generator for the given options.
     * @param options The target code generator options.
     */
    public SmcTableGenerator(final SmcOptions options)
    {
        super (options, TargetLanguage.TABLE.suffix());
    } // end of SmcTableGenerator(SmcOptions)

    //
    // end of Constructors.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // SmcVisitor Abstract Method Impelementation.
    //

    /**
     * Emits HTML table code for the finite state machine.
     * @param fsm emit HTML table code for this finite state machine.
     */
    @Override
    public void visit(final SmcFSM fsm)
    {
        Iterator<SmcMap> mit;
        String separator;

        // Output the top-of-page HTML.
        mTarget.println("<html>");
        mTarget.println("  <head>");
        mTarget.print("    <title>");
        mTarget.print(mSrcfileBase);
        mTarget.println("</title>");
        mTarget.println("  </head>");
        mTarget.println();
        mTarget.println("  <body>");

        // Have each map generate its HTML table.
        for (mit = fsm.getMaps().iterator(), separator = "";
             mit.hasNext();
             separator = "    <p>\n")
        {
            mTarget.print(separator);
            (mit.next()).accept(this);
        }

        // Output the end-of-page HTML.
        mTarget.println("  </body>");
        mTarget.println("</html>");

        return;
    } // end of visit(SmcFSM)

    /**
     * Emits HTML table code for the FSM map.
     * @param map emit HTML table code for this map.
     */
    @Override
    public void visit(final SmcMap map)
    {
        String mapName = map.getName();
        List<SmcTransition> transitions = map.getTransitions();
        List<SmcParameter> params;
        int transitionCount = transitions.size() + 1;
        Iterator<SmcParameter> it;
        SmcTransition defaultTransition = null;
        String transName;
        boolean firstFlag;

        // Output start of this map's table.
        mTarget.println(
            "    <table align=center border=3 cellspacing=2 cellpadding=2>");
        mTarget.println("      <caption align=\"top\">");
        mTarget.print("        ");
        mTarget.print(mapName);
        mTarget.println(" Finite State Machine");
        mTarget.println("      </caption>");

        // Output the table's header.
        mTarget.println("      <tr>");
        mTarget.println("        <th rowspan=2>");
        mTarget.println("          State");
        mTarget.println("        </th>");
        mTarget.println("        <th colspan=2>");
        mTarget.println("          Actions");
        mTarget.println("        </th>");
        mTarget.print("        <th colspan=");
        mTarget.print(transitionCount);
        mTarget.println(">");
        mTarget.println("          Transition");
        mTarget.println("        </th>");
        mTarget.println("      </tr>");
        mTarget.println("      <tr>");
        mTarget.println("        <th>");
        mTarget.println("          Entry");
        mTarget.println("        </th>");
        mTarget.println("        <th>");
        mTarget.println("         Exit");
        mTarget.println("        </th>");

        // Place each transition name into the header.
        for (SmcTransition transition: transitions)
        {
            transName = transition.getName();
            params = transition.getParameters();

            // Since we are placing the default transition at the
            // right-most column, don't output it here if it
            // should locally defined.
            if (transName.equals("Default") == false)
            {
                mTarget.println("        <th>");
                mTarget.print("          ");
                mTarget.println(transName);

                // If the transition has parameters, output
                // them now.
                if (params.isEmpty() == false)
                {
                    mTarget.println("          <br>");
                    mTarget.print("          (");

                    for (it = params.iterator(),
                             firstFlag = true;
                         it.hasNext();
                         firstFlag = false)
                    {
                        if (firstFlag == false)
                        {
                            mTarget.println(',');
                            mTarget.println("          <br>");
                            mTarget.print("          ");
                        }

                        (it.next()).accept(this);
                    }

                    mTarget.println(")");
                }

                mTarget.println("        </th>");
            }
        }

        // Also output the default transition.
        mTarget.println("        <th>");
        mTarget.println("          <b>Default</b>");
        mTarget.println("        </th>");
        mTarget.println("      </tr>");

        // The table header is finished. Now have each state
        // output its row.
        for (SmcState state: map.getStates())
        {
            // Output the row start.
            mTarget.println("      <tr>");

            // Note: the state outputs only its name and
            // entry/exit actions. It does not output its
            // transitions (see below).
            state.accept(this);

            // We need to generate transitions in the exact same
            // order as in the header. But the state object
            // does not store its transitions in any particular
            // order. Therefore we must output the state's
            // transitions for it.
            for (SmcTransition transition: transitions)
            {
                transName = transition.getName();
                params = transition.getParameters();

                // Since we are placing the default transition
                // at the right-most column, don't output it
                // here if it should locally defined.
                if (transName.equals("Default"))
                {
                    // If this state has a false transition,
                    // get it now and store it away for later.
                    defaultTransition =
                        state.findTransition(transName, params);
                }
                else
                {
                    mTarget.println("        <td>");

                    // We have the default transition definition
                    // in hand. We need the state's transition.
                    transition =
                        state.findTransition(transName, params);
                    if (transition != null)
                    {
                        // Place the transitions in preformatted
                        // sections. Don't add a new line - the
                        // transition will do that.
                        mTarget.print("          <pre>");
                        transition.accept(this);
                        mTarget.println("          </pre>");
                    }

                    mTarget.println("        </td>");
                }
            }

            // Now add the Default transition to the last column.
            mTarget.println("        <td>");
            if (defaultTransition != null)
            {
                // Place the transitions in preformatted
                // sections. Don't add a new line - the
                // transition will do that.
                mTarget.print("          <pre>");
                defaultTransition.accept(this);
                mTarget.println("          </pre>");
            }
            mTarget.println("        </td>");

            // Output the row end.
            mTarget.println("      </tr>");
        }

        // Output end of this map's table.
        mTarget.println("    </table>");

        return;
    } // end of visit(SmcMap)

    /**
     * Emits HTML table code for this FSM state.
     * @param state emits HTML table code for this state.
     */
    @Override
    public void visit(final SmcState state)
    {
        List<SmcAction> actions;

        // Output the row data. This consists of:
        // + the state name.
        // + the state entry actions.
        // + the state exit actions.
        // + Each of the transtions.
        mTarget.println("        <td>");
        mTarget.print("          ");
        mTarget.println(state.getInstanceName());
        mTarget.println("        </td>");

        mTarget.println("        <td>");
        actions = state.getEntryActions();
        if (actions != null && actions.isEmpty() == false)
        {
            mTarget.println("          <pre>");

            for (SmcAction action: actions)
            {
                action.accept(this);
            }
            mTarget.println("          </pre>");
        }
        mTarget.println("        </td>");

        mTarget.println("        <td>");
        actions = state.getExitActions();
        if (actions != null && actions.isEmpty() == false)
        {
            mTarget.println("          <pre>");
            for (SmcAction action: actions)
            {
                action.accept(this);
            }
            mTarget.println("          </pre>");
        }
        mTarget.println("        </td>");

        // Note: SmcMap generates our transitions for us in order
        //       to guarantee correct transition ordering.

        return;
    } // end of visit(SmcState)

    /**
     * Emits HTML table code for this FSM state transition.
     * @param transition emits HTML table code for this state
     * transition.
     */
    @Override
    public void visit(final SmcTransition transition)
    {
        for (SmcGuard guard: transition.getGuards())
        {
            mTarget.println();
            guard.accept(this);
        }

        return;
    } // end of visit(SmcTransition)

    /**
     * Emits HTML table code for this FSM transition guard.
     * @param guard emits HTML table code for this transition
     * guard.
     */
    @Override
    public void visit(final SmcGuard guard)
    {
        SmcTransition transition = guard.getTransition();
        SmcState state = transition.getState();
        SmcMap map = state.getMap();
        String mapName = map.getName();
        String stateName = state.getClassName();
        TransType transType = guard.getTransType();
        String condition = guard.getCondition();
        String endStateName = guard.getEndState();
        List<SmcAction> actions = guard.getActions();

        // Print out the guard (if there is one).
        if (condition.length() > 0)
        {
            mTarget.print('[');
            mTarget.print(condition);
            mTarget.println(']');
        }

        // If this is a pop transition, then print
        // out the pop transition and any arguments.
        if (transType == TransType.TRANS_POP)
        {
            mTarget.print("  pop(");

            // Is there a pop transition?
            if (endStateName.equals(
                    SmcElement.NIL_STATE) == false &&
                endStateName.length() > 0)
            {
                String popArgs = guard.getPopArgs();

                mTarget.print(endStateName);

                // Output any and all pop arguments.
                if (popArgs.length() > 0)
                {
                    mTarget.print(", ");
                    mTarget.print(popArgs.trim());
                }
            }

            mTarget.println(")");
        }
        else if (transType == TransType.TRANS_PUSH)
        {
            mTarget.print("  push(");

            // If the end state is nil, then replace it with the
            // current map and state.
            if (endStateName.equals(SmcElement.NIL_STATE))
            {
                mTarget.print(mapName);
                mTarget.print("::");
                mTarget.print(stateName);
            }
            else
            {
                mTarget.print(endStateName);
            }

            mTarget.println(")");
        }
        // Else this is a plain, old transition.
        else
        {
            // Print out the end state.
            mTarget.print("  ");

            // If the end state is nil, then replace it with the
            // current state's read name.
            if (endStateName.equals(SmcElement.NIL_STATE))
            {
                mTarget.println(stateName);
            }
            else
            {
                mTarget.println(endStateName);
            }
        }

        // Print out the actions (if there are any). Otherwise
        // output empty braces.
        if (actions.isEmpty())
        {
            mTarget.println("  {}");
        }
        else
        {
            mTarget.println("  {");

            mIndent = "    ";
            for (SmcAction action: actions)
            {
                action.accept(this);
            }

            mTarget.println("  }");
        }

        return;
    } // end of visit(SmcGuard)

    /**
     * Emits HTML table code for this FSM action.
     * @param action emits HTML table code for this action.
     */
    @Override
    public void visit(final SmcAction action)
    {
        List<String> arguments = action.getArguments();

        mTarget.print(mIndent);
        mTarget.print(action.getName());
        if (action.isProperty())
        {
            mTarget.print(" = ");
            mTarget.print(arguments.get(0).trim());
        }
        else
        {
            Iterator<String> it;
            String sep;

            mTarget.print("(");

            for (it = arguments.iterator(), sep = "";
                 it.hasNext();
                 sep = ", ")
            {
                mTarget.print(sep);
                mTarget.print((it.next()).trim());
            }

            mTarget.print(")");
        }
        mTarget.println(";");

        return;
    } // end of visit(SmcAction)

    /**
     * Emits HTML table code for this transition parameter.
     * @param parameter emits HTML table code for this transition
     * parameter.
     */
    @Override
    public void visit(final SmcParameter parameter)
    {
        mTarget.print(parameter.getType());
        return;
    } // end of visit(SmcParameter)

    //
    // end of SmcVisitor Abstract Method Impelementation.
    //-----------------------------------------------------------
} // end of class SmcTableGenerator

//
// CHANGE LOG
// $Log: SmcTableGenerator.java,v $
// Revision 1.6  2010/03/05 21:29:53  fperrad
// Allows property with Groovy, Lua, Perl, Python, Ruby & Scala
//
// Revision 1.5  2010/03/03 19:18:40  fperrad
// fix property with Graph & Table
//
// Revision 1.4  2009/11/25 22:30:19  cwrapp
// Fixed problem between %fsmclass and sm file names.
//
// Revision 1.3  2009/11/24 20:42:39  cwrapp
// v. 6.0.1 update
//
// Revision 1.2  2009/09/05 15:39:20  cwrapp
// Checking in fixes for 1944542, 1983929, 2731415, 2803547 and feature 2797126.
//
// Revision 1.1  2009/03/01 18:20:42  cwrapp
// Preliminary v. 6.0.0 commit.
//
//
