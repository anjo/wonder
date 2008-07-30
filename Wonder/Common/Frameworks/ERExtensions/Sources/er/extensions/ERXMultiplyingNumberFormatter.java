package er.extensions;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.FieldPosition;

import com.webobjects.foundation.NSNumberFormatter;

/**
 * @author david
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ERXMultiplyingNumberFormatter extends NSNumberFormatter {

    private float factor;
    /**
     * 
     */
    public ERXMultiplyingNumberFormatter() {
        super();
    }

    /**
     * @param arg0
     */
    public ERXMultiplyingNumberFormatter(String arg0) {
        super(arg0);
    }

    
    /* (non-Javadoc)
     * @see com.webobjects.foundation.NSNumberFormatter#pattern()
     */
    public String pattern() {
        String pattern = super.pattern();
        return pattern;
    }
    /* (non-Javadoc)
     * @see com.webobjects.foundation.NSNumberFormatter#setPattern(java.lang.String)
     */
    public void setPattern(String pattern) {
        
        try {
            if (pattern.indexOf("=)") == -1) {
                super.setPattern(pattern);
                return;
            }
            
            pattern = pattern.substring(pattern.indexOf("=)"+2));
            String f = pattern.substring(1, pattern.indexOf("=)"));
            try {
                factor = Float.parseFloat(f);
            } catch (NumberFormatException e1) {
                throw new IllegalArgumentException("ERXMultiplyingNumberFormatter must have a pattern like '(1024=)0.00',"+
                " where 1024 is the factor.");
            }
            
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("ERXMultiplyingNumberFormatter must have a pattern like '(1024=)0.00',"+
            " where 1024 is the factor.");
        }
        super.setPattern(pattern);
    }
    /* (non-Javadoc)
     * @see java.text.Format#format(java.lang.Object, java.lang.StringBuffer, java.text.FieldPosition)
     */
    public StringBuffer format(Object arg0, StringBuffer arg1,
            FieldPosition arg2) {
        if (!(arg0 instanceof Number)) {
            return super.format(arg0, arg1, arg2);
        }
        Number n = (Number)arg0;
        if (arg0 instanceof BigDecimal) {
            BigDecimal b = (BigDecimal)arg0;
            b = b.multiply(new BigDecimal(factor));
            return super.format(b, arg1, arg2);
        } else if (arg0 instanceof BigInteger) {
            BigInteger b = (BigInteger)arg0;
            b = b.multiply(new BigInteger(""+factor));
            return super.format(b, arg1, arg2);
        } else {
            double d = n.doubleValue();
            d *= (double)factor;
            return super.format(new Double(d), arg1, arg2);
        }
    }
}
