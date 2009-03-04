package er.indexing.attributes;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParseException;
import java.text.ParsePosition;

import com.webobjects.foundation.NSForwardException;
import com.webobjects.foundation.NSTimestamp;
import com.webobjects.foundation.NSTimestampFormatter;
import com.webobjects.foundation._NSFoundationCollection;

import er.extensions.eof.ERXConstant;
import er.extensions.foundation.ERXRuntimeUtilities;

public class ERIValueType extends ERXConstant.NumberConstant {

    private static class IdentityFormat extends Format {

        @Override
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            return toAppendTo.append(obj);
        }

        @Override
        public Object parseObject(String source, ParsePosition pos) {
            if(source != null) {
                return source.toString();
            }
            return null;
        }
    }
    
    private static class BooleanFormat extends Format {

        @Override
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            return toAppendTo.append(obj);
        }

        @Override
        public Object parseObject(String source, ParsePosition pos) {
            if(source != null) {
                return Boolean.valueOf(source);
            }
            return null;
        }
    }
    
    public static ERIValueType STRING = new ERIValueType(1, "ERIValueTypeString", new IdentityFormat());
    public static ERIValueType INTEGER = new ERIValueType(2, "ERIValueTypeInteger", new DecimalFormat("0"));
    public static ERIValueType DECIMAL = new ERIValueType(3, "ERIValueTypeDecimal", new DecimalFormat("0.00"));
    public static ERIValueType DATE = new ERIValueType(4, "ERIValueTypeDate", new NSTimestampFormatter());
    public static ERIValueType BOOLEAN = new ERIValueType(5, "ERIValueTypeBoolean", new BooleanFormat());

    private Format _format;
    
    protected ERIValueType(int value, String name, Format format) {
        super(value, name);
        _format = format;
    }
    
    public static ERIValueType valueType(int key) {
        return (ERIValueType) constantForClassNamed(key, ERIValueType.class.getName());
    }

    public Format formatterForFormat(String format) {
        return _format;
    }
}
