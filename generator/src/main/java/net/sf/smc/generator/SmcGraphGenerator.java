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
// $Id: SmcGraphGenerator.java,v 1.9 2010/05/27 10:15:36 fperrad Exp $
//
// CHANGE LOG
// (See the bottom of this file.)
//

package net.sf.smc.generator;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
 * Visits the abstract syntax tree, emitting a Graphviz diagram.
 * @see SmcElement
 * @see SmcCodeGenerator
 * @see SmcVisitor
 * @see SmcOptions
 *
 * @author <a href="mailto:rapp@acm.org">Charles Rapp</a>
 */

public final class SmcGraphGenerator
    extends SmcCodeGenerator
{
//---------------------------------------------------------------
// Member data
//

    //-----------------------------------------------------------
    // Locals.
    //

    private final String mIndentAction;

    private SmcState mState;

//---------------------------------------------------------------
// Member methods
//

    //-----------------------------------------------------------
    // Constructors.
    //

    /**
     * Creates a GraphViz code generator for the given options.
     * @param options The target code generator options.
     */
    public SmcGraphGenerator(final SmcOptions options)
    {
        super (options, TargetLanguage.GRAPH.suffix());

        mIndentAction = "&nbsp;&nbsp;&nbsp;";
    } // end of SmcGraphGenerator(SmcOptions)

    //
    // end of Constructors.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // SmcVisitor Abstract Method Impelementation.
    //

    /**
     * Emits GraphViz code for the finite state machine.
     * @param fsm emit GraphViz code for this finite state machine.
     */
    @Override
    public void visit(final SmcFSM fsm)
    {
        // Create one overall graph and place each map in a
        // subgraph.
        mTarget.print("digraph ");
        mTarget.print(mSrcfileBase);
        mTarget.println(" {");
        mTarget.println();
        mTarget.println("    node");
        mTarget.println("        [shape=Mrecord width=1.5];");
        mTarget.println();

        // Have each map generate its subgraph.
        for (SmcMap map: fsm.getMaps())
        {
            String mapName = map.getName();

            mTarget.print("    subgraph cluster_");
            mTarget.print(mapName);
            mTarget.println(" {");
            mTarget.println();
            mTarget.print("        label=\"");
            mTarget.print(mapName);
            mTarget.println("\";");
            mTarget.println();

            map.accept(this);

            // Output the subgraph's closing brace.
            mTarget.println("    }");
            mTarget.println();
        }

        // Output the digraph's closing brace.
        mTarget.println("}");

        return;
    } // end of visit(SmcFSM)

    /**
     * Emits GraphViz code for the FSM map.
     * @param map emit GraphViz code for this map.
     */
    public void visit(final SmcMap map)
    {
        String mapName = map.getName();
        SmcState defaultState = map.getDefaultState();
        String startStateName = map.getFSM().getStartState();
        Map<String, String> pushEntryMap = new HashMap<>();
        Map<String, String> popTransMap = new HashMap<>();
        Map<String, String> pushStateMap = new HashMap<>();
        boolean needEnd = false;
        String startMapName = startStateName.substring(0, startStateName.indexOf(":"));

        mTarget.println("        //");
        mTarget.println("        // States (Nodes)");
        mTarget.println("        //");
        mTarget.println();

        // Output the state names first.
        for (SmcState state: map.getStates())
        {
            state.accept(this);
        }

        for (SmcState state: map.getStates())
        {
            for (SmcTransition transition: state.getTransitions())
            {
                for (SmcGuard guard: transition.getGuards())
                {
                    String endStateName = guard.getEndState();
                    TransType transType = guard.getTransType();

                    if (transType == TransType.TRANS_PUSH)
                    {
                        String pushStateName = guard.getPushState();
                        String pushMapName;
                        int index;

                        if (endStateName.equals(SmcElement.NIL_STATE))
                        {
                             endStateName = state.getInstanceName();
                        }

                        if ((index = pushStateName.indexOf("::")) >= 0)
                        {
                            pushMapName = pushStateName.substring(0, index);
                        }
                        else
                        {
                            pushMapName = mapName;
                        }

                        pushStateMap.put(mapName + "::" + endStateName + "::" + pushMapName, pushMapName);
                    }
                    else if (transType == TransType.TRANS_POP)
                    {
                        String popKey = endStateName;
                        String popVal = endStateName;
                        String popArgs;

                        if (mGraphLevel == GRAPH_LEVEL_2 &&
                            (popArgs = guard.getPopArgs()) != null &&
                            popArgs.length() > 0)
                        {
                            popKey += ", ";
                            popVal += ", ";
                            popKey += doEscape(normalize(popArgs));
                            // If the argument contains line separators,
                            // then replace them with a "\n" so Graphviz knows
                            // about the line separation.
                            popVal += doEscape(popArgs).replaceAll(
                                "\\n", "\\\\\\l");
                        }
                        popTransMap.put(popKey, popVal);
                        needEnd = true;
                    }
                }
            }
            if (defaultState != null)
            {
                for (SmcTransition transition: defaultState.getTransitions())
                {
                    String transName = transition.getName();
                    if (state.callDefault(transName))
                    {
                        for (SmcGuard guard: transition.getGuards())
                        {
                            if (state.findGuard(transName, guard.getCondition()) == null)
                            {
                                String endStateName = guard.getEndState();
                                TransType transType = guard.getTransType();

                                if (transType == TransType.TRANS_PUSH)
                                {
                                    String pushStateName = guard.getPushState();
                                    String pushMapName;
                                    int index;

                                    if (endStateName.equals(SmcElement.NIL_STATE))
                                    {
                                        endStateName = state.getInstanceName();
                                    }

                                    if ((index = pushStateName.indexOf("::")) >= 0)
                                    {
                                        pushMapName = pushStateName.substring(0, index);
                                    }
                                    else
                                    {
                                        pushMapName = mapName;
                                    }

                                    pushStateMap.put(mapName + "::" + endStateName + "::" + pushMapName, pushMapName);
                                }
                                else if (transType == TransType.TRANS_POP)
                                {
                                    String popKey = endStateName;
                                    String popVal = endStateName;
                                    String popArgs;

                                    if (mGraphLevel == GRAPH_LEVEL_2 &&
                                        (popArgs = guard.getPopArgs()) != null &&
                                         popArgs.length() > 0)
                                    {
                                        popKey += ", ";
                                        popVal += ", ";
                                        popKey += doEscape(normalize(popArgs));
                                        // If the argument contains line separators,
                                        // then replace them with a "\n" so Graphviz knows
                                        // about the line separation.
                                        popVal += doEscape(popArgs).replaceAll(
                                            "\\n", "\\\\\\l");
                                    }
                                    popTransMap.put(popKey, popVal);
                                    needEnd = true;
                                }
                            }
                        }
                    }
                }
            }
        }

        // Now output the pop transitions as "nodes".
        for (String pname: popTransMap.keySet())
        {
            mTarget.print("        \"");
            mTarget.print(mapName);
            mTarget.print("::pop(");
            mTarget.print(pname);
            mTarget.println(")\"");
            mTarget.println("            [label=\"\" width=1]");
            mTarget.println();
        }

        if (needEnd)
        {
            // Output the end node
            mTarget.print("        \"");
            mTarget.print(mapName);
            mTarget.println("::%end\"");
            mTarget.println(
                "            [label=\"\" shape=doublecircle style=filled fillcolor=black width=0.15];");
            mTarget.println();
        }

        // Now output the push composite state.
        for (String pname: pushStateMap.keySet())
        {
            mTarget.print("        \"");
            mTarget.print(pname);
            mTarget.println("\"");
            mTarget.print("            [label=\"{");
            mTarget.print(pushStateMap.get(pname));
            mTarget.println("|O-O\\r}\"]");
            mTarget.println();
        }

        if (startMapName.equals(mapName))
        {
            // Output the start node only in the right map
            mTarget.println("        \"%start\"");
            mTarget.println("            [label=\"\" shape=circle style=filled fillcolor=black width=0.25];");
            mTarget.println();
        }

        // Now output the push actions as "nodes".
        for (SmcMap map2: map.getFSM().getMaps())
        {
            for (SmcState state: map2.getAllStates())
            {
                for (SmcTransition transition: state.getTransitions())
                {
                    for (SmcGuard guard: transition.getGuards())
                    {
                        if (guard.getTransType() == TransType.TRANS_PUSH)
                        {
                            String pushStateName = guard.getPushState();

                            if (pushStateName.indexOf(mapName) == 0)
                            {
                                pushEntryMap.put(pushStateName, "");
                            }
                        }
                    }
                }
            }
        }
        for (String pname: pushEntryMap.keySet())
        {
            // Output the push action.
            mTarget.print("        \"push(");
            mTarget.print(pname);
            mTarget.println(")\"");
            mTarget.println("            [label=\"\" shape=plaintext];");
            mTarget.println();
        }

        mTarget.println("        //");
        mTarget.println("        // Transitions (Edges)");
        mTarget.println("        //");

        // For each state, output its transitions.
        for (SmcState state: map.getStates())
        {
            mState = state;
            for (SmcTransition transition: state.getTransitions())
            {
                transition.accept(this);
            }
            if (defaultState != null)
            {
                for (SmcTransition transition: defaultState.getTransitions())
                {
                    String transName = transition.getName();
                    if (state.callDefault(transName))
                    {
                        for (SmcGuard guard: transition.getGuards())
                        {
                            if (state.findGuard(transName, guard.getCondition()) == null)
                            {
                                guard.accept(this);
                            }
                        }
                    }
                }
            }
        }

        // Now output the pop transitions.
        for (String pname: popTransMap.keySet())
        {
            mTarget.println();
            mTarget.print("        \"");
            mTarget.print(mapName);
            mTarget.print("::pop(");
            mTarget.print(pname);
            mTarget.print(")\" -> \"");
            mTarget.print(mapName);
            mTarget.println("::%end\"");
            mTarget.print("            [label=\"pop(");
            mTarget.print(popTransMap.get(pname));
            mTarget.println(");\\l\"];");
        }

        // Now output the composite state transition.
        for (String pname: pushStateMap.keySet())
        {
            mTarget.println();
            mTarget.print("        \"");
            mTarget.print(pname);
            mTarget.print("\" -> \"");
            mTarget.print(pname.substring(0, pname.lastIndexOf("::")));
            mTarget.println("\"");
            mTarget.println("            [label=\"pop/\"]");
        }

        if (startMapName.equals(mapName))
        {
            // Output the start transition only in the right map
            mTarget.println();
            mTarget.print("        \"%start\" -> \"");
            mTarget.print(startStateName);
            mTarget.println("\"");
        }

        // Now output the push actions as entry "transition".
        for (String pname: pushEntryMap.keySet())
        {
            mTarget.println();
            mTarget.print("        \"push(");
            mTarget.print(pname);
            mTarget.print(")\" -> \"");
            mTarget.print(pname);
            mTarget.println("\"");
            mTarget.println("            [arrowtail=odot];");
        }

        return;
    } // end of visit(SmcMap)

    /**
     * Emits GraphViz code for this FSM state.
     * @param state emits GraphViz code for this state.
     */
    @Override
    public void visit(final SmcState state)
    {
        SmcMap map = state.getMap();
        SmcState defaultState = map.getDefaultState();
        String mapName = map.getName();
        String instanceName = state.getInstanceName();

        // The state name must be fully-qualified because
        // Graphviz does not allow subgraphs to share node names.
        // Place the node name in quotes.
        mTarget.print("        \"");
        mTarget.print(mapName);
        mTarget.print("::");
        mTarget.print(instanceName);
        mTarget.println("\"");

        mTarget.print("            [label=\"{");
        mTarget.print(instanceName);

        // For graph level 1 & 2, output entry and exit actions.
        if (mGraphLevel >= GRAPH_LEVEL_1)
        {
            List<SmcAction> actions;
            Iterator<SmcAction> it;
            boolean empty = true;

            actions = state.getEntryActions();
            if (actions == null && defaultState != null)
            {
                actions = defaultState.getEntryActions();
            }
            if (actions != null)
            {
                if (empty)
                {
                    mTarget.print("|");
                    empty = false;
                }
                mTarget.print("Entry/\\l");

                // Output the entry actions, one per line.
                for (SmcAction action: actions)
                {
                    mTarget.print(mIndentAction);
                    action.accept(this);
                }
            }

            actions = state.getExitActions();
            if (actions == null && defaultState != null)
            {
                actions = defaultState.getExitActions();
            }
            if (actions != null)
            {
                if (empty)
                {
                    mTarget.print("|");
                }
                mTarget.print("Exit/\\l");

                // Output the exit actions, one per line.
                for (SmcAction action: actions)
                {
                    mTarget.print(mIndentAction);
                    action.accept(this);
                }
            }

            // Starts a new compartment for internal events
            empty = true;
            for (SmcTransition transition: state.getTransitions())
            {
                for (SmcGuard guard: transition.getGuards())
                {
                    String endStateName = guard.getEndState();
                    TransType transType = guard.getTransType();

                    if (isLoopback(transType, endStateName) &&
                        transType != TransType.TRANS_PUSH)
                    {
                        String transName = transition.getName();
                        String condition = guard.getCondition();
                        String pushStateName = guard.getPushState();
                        actions = guard.getActions();

                        if (empty)
                        {
                            mTarget.print("|");
                            empty = false;
                        }
                        mTarget.print(transName);

                        // Graph Level 2: Output the transition parameters.
                        if (mGraphLevel == GRAPH_LEVEL_2)
                        {
                            List<SmcParameter> parameters = transition.getParameters();
                            Iterator<SmcParameter> pit;
                            String sep;

                            mTarget.print("(");
                            for (pit = parameters.iterator(), sep = "";
                                 pit.hasNext();
                                 sep = ", ")
                            {
                                mTarget.print(sep);
                                (pit.next()).accept(this);
                            }
                            mTarget.print(")");
                        }

                        // Output the guard.
                        if (condition != null && condition.length() > 0)
                        {
                            String tmp = doEscape(condition);

                            // If the condition contains line separators,
                            // then replace them with a "\n" so Graphviz knows
                            // about the line separation.
                            tmp = tmp.replaceAll("\\n", "\\\\\\l");

                            // Not needed when label in edge !!
                            tmp = tmp.replaceAll(">", "\\\\>");
                            tmp = tmp.replaceAll("<", "\\\\<");
                            tmp = tmp.replaceAll("\\|", "\\\\|");

                            mTarget.print("\\l\\[");
                            mTarget.print(tmp);
                            mTarget.print("\\]");
                        }

                        mTarget.print("/\\l");

                        if (actions != null)
                        {
                            // Output the actions, one per line.
                            for (SmcAction action: actions)
                            {
                                mTarget.print(mIndentAction);
                                action.accept(this);
                            }
                        }

                        if (transType == TransType.TRANS_PUSH)
                        {
                            mTarget.print(mIndentAction);
                            mTarget.print("push(");
                            mTarget.print(pushStateName);
                            mTarget.print(")\\l");
                        }
                    }
                }
            }
            if (defaultState != null)
            {
                for (SmcTransition transition: defaultState.getTransitions())
                {
                    String transName = transition.getName();
                    if (state.callDefault(transName))
                    {
                        for (SmcGuard guard: transition.getGuards())
                        {
                            if (state.findGuard(transName, guard.getCondition()) == null)
                            {
                                String endStateName = guard.getEndState();
                                TransType transType = guard.getTransType();

                                if (isLoopback(transType, endStateName) &&
                                    transType != TransType.TRANS_PUSH)
                                {
                                    transName = transition.getName();
                                    String condition = guard.getCondition();
                                    String pushStateName = guard.getPushState();
                                    actions = guard.getActions();

                                    if (empty)
                                    {
                                        mTarget.print("|");
                                        empty = false;
                                    }
                                    mTarget.print(transName);

                                    // Graph Level 2: Output the transition parameters.
                                    if (mGraphLevel == GRAPH_LEVEL_2)
                                    {
                                        List<SmcParameter> parameters = transition.getParameters();
                                        Iterator<SmcParameter> pit;
                                        String sep;

                                        mTarget.print("(");
                                        for (pit = parameters.iterator(), sep = "";
                                             pit.hasNext();
                                             sep = ", ")
                                        {
                                            mTarget.print(sep);
                                            (pit.next()).accept(this);
                                        }
                                        mTarget.print(")");
                                    }

                                    // Output the guard.
                                    if (condition != null && condition.length() > 0)
                                    {
                                        String tmp = doEscape(condition);

                                        // If the condition contains line separators,
                                        // then replace them with a "\n" so Graphviz knows
                                        // about the line separation.
                                        tmp = tmp.replaceAll("\\n", "\\\\\\l");

                                        // Not needed when label in edge !!
                                        tmp = tmp.replaceAll(">", "\\\\>");
                                        tmp = tmp.replaceAll("<", "\\\\<");
                                        tmp = tmp.replaceAll("\\|", "\\\\|");

                                        mTarget.print("\\l\\[");
                                        mTarget.print(tmp);
                                        mTarget.print("\\]");
                                    }

                                    mTarget.print("/\\l");

                                    if (actions != null)
                                    {
                                        // Output the actions, one per line.
                                        for (SmcAction action: actions)
                                        {
                                            mTarget.print(mIndentAction);
                                            action.accept(this);
                                        }
                                    }

                                    if (transType == TransType.TRANS_PUSH)
                                    {
                                        mTarget.print(mIndentAction);
                                        mTarget.print("push(");
                                        mTarget.print(pushStateName);
                                        mTarget.print(")\\l");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        mTarget.println("}\"];");
        mTarget.println();

        return;
    } // end of visit(SmcState)

    /**
     * Emits GraphViz code for this FSM transition.
     * @param transition emits GraphViz code for this transition.
     */
    @Override
    public void visit(final SmcTransition transition)
    {
        for (SmcGuard guard: transition.getGuards())
        {
            guard.accept(this);
        }

        return;
    } // end of visit(SmcTransition)

    /**
     * Emits GraphViz code for this FSM transition guard.
     * @param guard emits GraphViz code for this transition guard.
     */
    @Override
    public void visit(final SmcGuard guard)
    {
        SmcTransition transition = guard.getTransition();
        SmcMap map = mState.getMap();
        String mapName = map.getName();
        String stateName = mState.getInstanceName();
        String transName = transition.getName();
        TransType transType = guard.getTransType();
        String endStateName = guard.getEndState();
        String pushStateName = guard.getPushState();
        String condition = guard.getCondition();
        List<SmcAction> actions = guard.getActions();

        // Loopback are added in the state
        if (isLoopback(transType, endStateName) &&
            transType != TransType.TRANS_PUSH)
        {
            return;
        }

        mTarget.println();
        mTarget.print("        \"");
        mTarget.print(mapName);
        mTarget.print("::");
        mTarget.print(stateName);
        mTarget.print("\" -> ");

        if (transType != TransType.TRANS_POP)
        {
            if (endStateName.equals(SmcElement.NIL_STATE))
            {
                endStateName = stateName;
            }

            if (!endStateName.contains("::"))
            {
                endStateName = mapName + "::" + endStateName;
            }

            mTarget.print("\"");
            mTarget.print(endStateName);

            if (transType == TransType.TRANS_PUSH)
            {
                int index = pushStateName.indexOf("::");

                mTarget.print("::");

                if (index < 0)
                {
                    mTarget.print(mapName);
                }
                else
                {
                    mTarget.print(pushStateName.substring(0, pushStateName.indexOf("::")));
                }
            }

            mTarget.println("\"");
        }
        else
        {
            String popArgs = guard.getPopArgs();

            mTarget.print("\"");
            mTarget.print(mapName);
            mTarget.print("::pop(");
            mTarget.print(endStateName);

            if (mGraphLevel == GRAPH_LEVEL_2 &&
                popArgs != null &&
                popArgs.length() > 0)
            {
                mTarget.print(", ");
                mTarget.print(doEscape(normalize(popArgs)));
            }

            mTarget.println(")\"");
        }

        mTarget.print("            [label=\"");
        mTarget.print(transName);

        // Graph Level 2: Output the transition parameters.
        if (mGraphLevel == GRAPH_LEVEL_2)
        {
            List<SmcParameter> parameters =
                transition.getParameters();
            Iterator<SmcParameter> pit;
            String sep;

            mTarget.print("(");
            for (pit = parameters.iterator(), sep = "";
                 pit.hasNext();
                 sep = ", ")
            {
                mTarget.print(sep);
                (pit.next()).accept(this);
            }
            mTarget.print(")");
        }

        // Graph Level 1, 2: Output the guard.
        if (mGraphLevel > GRAPH_LEVEL_0 &&
            condition != null &&
            condition.length() > 0)
        {
            mTarget.print("\\l\\[");

            // If the condition contains line separators,
            // then replace them with a "\n" so Graphviz knows
            // about the line separation.
            // 4.3.0: First escape the condition then replace the
            //        line separators.
            mTarget.print(
                doEscape(condition).replaceAll(
                    "\\n", "\\\\\\l"));

            mTarget.print("\\]");
        }
        mTarget.print("/\\l");

        // Graph Level 1, 2: output actions.
        if (mGraphLevel > GRAPH_LEVEL_0 &&
            actions != null)
        {
            for (SmcAction action: actions)
            {
                action.accept(this);
            }
        }

        if (transType == TransType.TRANS_PUSH)
        {
            mTarget.print("push(");
            mTarget.print(pushStateName);
            mTarget.print(")\\l");
        }

        mTarget.println("\"];");

        return;
    } // end of visit(SmcGuard)

    /**
     * Emits GraphViz code for this FSM action.
     * @param action emits GraphViz code for this action.
     */
    @Override
    public void visit(final SmcAction action)
    {
        // Actions are only reported for graph levels 1 and 2.
        // Graph level 1: only the action name, no arguments.
        // Graph level 2: action name and arguments.
        //
        // Note: do not output an end-of-line.
        if (mGraphLevel >= GRAPH_LEVEL_1)
        {
            mTarget.print(action.getName());

            if (mGraphLevel == GRAPH_LEVEL_2)
            {
                List<String> arguments = action.getArguments();
                String arg;

                if (action.isProperty())
                {
                    mTarget.print(" = ");

                    arg = arguments.get(0).trim();

                    // If the argument is a quoted string, then
                    // the quotes must be escaped.
                    // First, replace all backslashes with two
                    // backslashes.
                    arg = arg.replaceAll("\\\\", "\\\\\\\\");

                    // Then replace all double quotes with
                    // a backslash double qoute.
                    mTarget.print(
                        arg.replaceAll("\"", "\\\\\""));
                }
                else
                {
                    Iterator<String> it;
                    String sep;

                    mTarget.print("(");

                    // Now output the arguments.
                    for (it = arguments.iterator(),
                             sep = "";
                         it.hasNext();
                         sep = ", ")
                    {
                        arg = (it.next()).trim();

                        mTarget.print(sep);

                        // If the argument is a quoted string, then
                        // the quotes must be escaped.
                        // First, replace all backslashes with two
                        // backslashes.
                        arg = arg.replaceAll("\\\\", "\\\\\\\\");

                        // Then replace all double quotes with
                        // a backslash double qoute.
                        mTarget.print(
                            arg.replaceAll("\"", "\\\\\""));
                    }

                    mTarget.print(")");
                }
            }

            mTarget.print(";\\l");
        }

        return;
    } // end of visit(SmcAction)

    /**
     * Emits GraphViz code for this transition parameter.
     * @param parameter emits GraphViz code for this transition parameter.
     */
    @Override
    public void visit(final SmcParameter parameter)
    {
        // Graph Level 2
        mTarget.print(parameter.getName());
        if (parameter.getType().equals("") == false)
        {
            mTarget.print(": ");
            mTarget.print(parameter.getType());
        }

        return;
    } // end of visit(SmcParameter)

    //
    // end of SmcVisitor Abstract Method Impelementation.
    //-----------------------------------------------------------

    // Place a backslash escape character in front of backslashes
    // and doublequotes.
    private static String doEscape(final String s)
    {
        String retval;

        if (s.indexOf('\\') < 0 && s.indexOf('"') < 0)
        {
            retval = s;
        }
        else
        {
            StringBuilder buffer =
                new StringBuilder(s.length() * 2);
            int index;
            int length = s.length();
            char c;

            for (index = 0; index < length; ++index)
            {
                c = s.charAt(index);
                if (c == '\\' || c == '"')
                {
                    buffer.append('\\');
                }

                buffer.append(c);
            }

            retval = buffer.toString();
        }

        return (retval);
    }

    private static String normalize(final String s)
    {
        int index;
        int length = s.length();
        char c;
        boolean space = false;
        StringBuilder buffer =
            new StringBuilder(length);

        for (index = 0; index < length; ++index)
        {
            c = s.charAt(index);
            if (space)
            {
                if (c != ' ' && c != '\t' && c != '\n')
                {
                    buffer.append(c);
                    space = false;
                }
            }
            else
            {
                if (c == ' ' || c == '\t' || c == '\n')
                {
                    buffer.append(' ');
                    space = true;
                }
                else
                {
                    buffer.append(c);
                }
            }
        }

        return (buffer.toString().trim());
    }

    // Outputs a list of warning and error messages.
} // end of class SmcGraphGenerator

//
// CHANGE LOG
// $Log: SmcGraphGenerator.java,v $
// Revision 1.9  2010/05/27 10:15:36  fperrad
// fix #3007678
//
// Revision 1.8  2010/03/08 17:02:40  fperrad
// New representation of the Default state. The result is full UML.
//
// Revision 1.7  2010/03/05 21:29:53  fperrad
// Allows property with Groovy, Lua, Perl, Python, Ruby & Scala
//
// Revision 1.6  2010/03/03 19:18:40  fperrad
// fix property with Graph & Table
//
// Revision 1.5  2009/11/25 22:30:19  cwrapp
// Fixed problem between %fsmclass and sm file names.
//
// Revision 1.4  2009/11/24 20:42:39  cwrapp
// v. 6.0.1 update
//
// Revision 1.3  2009/09/05 15:39:20  cwrapp
// Checking in fixes for 1944542, 1983929, 2731415, 2803547 and feature 2797126.
//
// Revision 1.2  2009/03/27 09:41:47  cwrapp
// Added F. Perrad changes back in.
//
// Revision 1.1  2009/03/01 18:20:42  cwrapp
// Preliminary v. 6.0.0 commit.
//
//
