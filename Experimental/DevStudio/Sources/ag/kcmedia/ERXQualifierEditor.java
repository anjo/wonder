//
// ERXQualifierEditor.java: Class file for WO Component 'ERXQualifierEditor'
// Project RuleEditor
//
// Created by ak on Thu Jun 20 2002
//
package ag.kcmedia;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;

public class ERXQualifierEditor extends WOComponent {

    public ERXQualifierEditor(WOContext context) {
        super(context);
    }

    public boolean isStateless() { return true;}
    public boolean synchronizesVariablesWithBindings() { return false;}

    public int index;
    public EOQualifier qualifier;
    public EOQualifier currentQualifier;

    public void reset() {
        super.reset();
        qualifier = null;
    }

    public EOQualifier qualifier() {
        if(qualifier == null) {
            qualifier = (EOQualifier)valueForBinding("qualifier");
        }
        return qualifier;
    }

    public boolean isArrayQualifier() {
        if (qualifier() instanceof EOAndQualifier)
            return true;
        if (qualifier() instanceof EOOrQualifier)
            return true;
        return false;
    }

    public boolean isNegateQualifier() {
        if (qualifier() instanceof EONotQualifier)
            return true;
        return false;
    }

    public boolean isSimpleQualifier() {
        if (isArrayQualifier() || isNegateQualifier() )
            return false;
        return true;
    }

    public boolean isFirstRow() {
        return index == 0;
    }

    public String qualifierClass() {
        if (qualifier() instanceof EOAndQualifier)
            return "and";
        if (qualifier() instanceof EOOrQualifier)
            return "or";
        if (qualifier() instanceof EONotQualifier)
            return "not";
        return "error";
    }
    public String qualifierKind() {
        if (qualifier() instanceof EOAndQualifier)
            return "A<br>N<br>D";
        if (qualifier() instanceof EOOrQualifier)
            return "O<br>R";
        if (qualifier() instanceof EONotQualifier)
            return "NOT";
        return "ERROR";
    }
}
