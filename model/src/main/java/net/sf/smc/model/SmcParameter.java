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
// Id: SmcParameter.java,v 1.1 2009/03/01 18:20:42 cwrapp Exp
//
// CHANGE LOG
// (See the bottom of this file.)
//

package net.sf.smc.model;

/**
 * This class contains a transition parameter definition. This
 * includes the parameter name and type.
 *
 * @see net.sf.smc.model.SmcTransition
 * @author <a href="mailto:rapp@acm.org">Charles Rapp</a>
 */

public final class SmcParameter
    extends SmcElement
    implements Comparable<SmcParameter>
{
//---------------------------------------------------------------
// Member data
//

    //-----------------------------------------------------------
    // Constants.
    //

    /**
     * Since Tcl is a typeless programming language, use this
     * "type" if the parameter is to be accessed by value.
     */
    public static final String TCL_VALUE_TYPE = "value";

    /**
     * Since Tcl is a typeless programming language, use this
     * "type" if the parameter is to be accessed by reference.
     */
    public static final String TCL_REFERENCE_TYPE = "reference";

    //-----------------------------------------------------------
    // Locals.
    //

    // A parameter has a name, a type and the line number where it
    // appears in the .sm file.
    private String mType;

//---------------------------------------------------------------
// Member methods
//

    //-----------------------------------------------------------
    // Constructors.
    //

    /**
     * Creates a parameter instance with the given name, type and
     * .sm file line number location.
     * @param name the parameter name.
     * @param lineNumber where the parameter is defined in the
     * .sm file.
     * @param type the parameter type.
     */
    public SmcParameter(final String name,
                        final int lineNumber,
                        final String type)
    {
        super (name, lineNumber);

        mType = type;
    } // end of SmcParameter(String, int, String)

    //
    // end of Constructors.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // SmcElement Abstract Methods.
    //

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
    // Comparable Interface Implementation.
    //

    /**
     * Returns an integer value &lt;, equal to or &gt; zero
     * depending on whether {@code this} parameter is &lt;,
     * equal to or &gt; {@code param}. The comparison is based
     * on the name and type.
     * @param param compare with this parameter instance.
     * @return an integer value &lt;, equal to or &gt; zero
     * depending on whether {@code this} parameter is &lt;,
     * equal to or &gt; {@code param}.
     */
    @Override
    public int compareTo(final SmcParameter param)
        throws ClassCastException
    {
        int retval = mName.compareTo(param.mName);

        if (retval == 0)
        {
            retval = mType.compareTo(param.mType);
        }

        return (retval);
    } // end of compareTo(SmcParameter)

    //
    // end of Comparable Interface Implementation.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // Object Method Overrides.
    //

    /**
     * Returns {@code true} if {@code obj} is a non-{@code null}
     * parameter instance with the same name and type;
     * {@code false} otherwise.
     * @param obj compare with this object.
     * @return {@code true} if {@code obj} is a non-{@code null}
     * parameter instance with the same name and type;
     * {@code false} otherwise.
     */
    @Override
    public boolean equals(final Object obj)
    {
        boolean retval = (this == obj);

        if (!retval && obj instanceof SmcParameter)
        {
            SmcParameter parameter = (SmcParameter) obj;

            retval = mName.equals(parameter.mName) &&
                     mType.equals(parameter.mType);
        }

        return (retval);
    } // end of equals(Object)

    /**
     * Returns the hash code based on the parameter name and
     * type.
     * @return the hash code based on the parameter name and
     * type.
     */
    @Override
    public int hashCode()
    {
        return ((this.toString()).hashCode());
    } // end of hashCode()

    /**
     * Returns the parameter text representation. The format is
     * either <i>name</i> if type is an empty string and
     * <i>name</i>[: <i>type</i>] if type is defined.
     * @return the parameter text representation.
     */
    @Override
    public String toString()
    {
        StringBuilder retval = new StringBuilder();

        retval.append(mName);
        if (!mType.isEmpty())
        {
            retval.append(": ");
            retval.append(mType);
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
     * Returns the parameter type.
     * @return the parameter type.
     */
    public String getType()
    {
        return (mType);
    } // end of getType()

    //
    // end of Get Methods.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // Set Methods.
    //

    /**
     * Sets the parameter type.
     * @param typeName the parameter type text.
     */
    public void setType(final String typeName)
    {
        // Trim away whitespace since the type was
        // read in verbatim.
        mType = typeName.trim();

        return;
    } // end of setType(String)

    //
    // end of Set Methods.
    //-----------------------------------------------------------
} // end of class SmcParamter

//
// CHANGE LOG
// Log: SmcParameter.java,v
// Revision 1.1  2009/03/01 18:20:42  cwrapp
// Preliminary v. 6.0.0 commit.
//
// Revision 1.7  2007/07/16 06:28:06  fperrad
// + Added Groovy generator.
//
// Revision 1.6  2007/01/15 00:23:51  cwrapp
// Release 4.4.0 initial commit.
//
// Revision 1.5  2006/09/16 15:04:29  cwrapp
// Initial v. 4.3.3 check-in.
//
// Revision 1.4  2005/05/28 19:28:42  cwrapp
// Moved to visitor pattern.
//
// Revision 1.5  2005/02/21 15:37:35  charlesr
// Added Francois Perrad to Contributors section for Python work.
//
// Revision 1.4  2005/02/03 16:48:51  charlesr
// In implementing the Visitor pattern, the generateCode()
// methods have been moved to the appropriate Visitor
// subclasses (e.g. SmcJavaGenerator). This class now extends
// SmcElement.
//
// Revision 1.3  2004/10/30 16:06:38  charlesr
// Added Graphviz DOT file generation.
//
// Revision 1.2  2004/09/06 16:40:41  charlesr
// Added C# support.
//
// Revision 1.1  2004/05/31 13:55:36  charlesr
// Added support for VB.net code generation.
//
// Revision 1.0  2003/12/14 21:04:32  charlesr
// Initial revision
//
