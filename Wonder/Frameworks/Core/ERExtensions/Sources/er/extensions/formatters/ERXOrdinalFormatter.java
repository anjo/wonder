package er.extensions.formatters;

import java.text.FieldPosition;
import java.text.ParsePosition;
import com.ibm.icu.text.RuleBasedNumberFormat;
import com.ibm.icu.util.ULocale;



/**
 * Formats numbers into 1st, 2nd, 3rd. etc.  Not localized (English only at present).
 *
 * @author chill
 */
public class ERXOrdinalFormatter extends java.text.Format {


	public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
		StringBuffer result = new StringBuffer();
		if (obj != null) {
			if ( !(obj instanceof Number)) {
		        throw new RuntimeException("Object of class " + obj.getClass().getCanonicalName() + " passed to ERXOrdinalFormatter");
			}
			
			RuleBasedNumberFormat rbnf = new RuleBasedNumberFormat(ULocale.ENGLISH,RuleBasedNumberFormat.ORDINAL);
			result.append(rbnf.format(obj));		 
		}

		return result;
	}

    /**
     * API conformance.
     */
    public Object parseObject(String source, ParsePosition pos)
    {
        throw new RuntimeException("Method not implemented");
    }

}
