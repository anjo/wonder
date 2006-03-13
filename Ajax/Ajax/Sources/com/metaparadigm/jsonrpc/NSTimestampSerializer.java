package com.metaparadigm.jsonrpc;

import org.json.JSONObject;

import com.webobjects.foundation.NSTimeZone;
import com.webobjects.foundation.NSTimestamp;

public class NSTimestampSerializer extends AbstractSerializer {
    private final static long serialVersionUID = 1;

    private static Class[] _serializableClasses = new Class[] { NSTimestamp.class };

    private static Class[] _JSONClasses = new Class[] { JSONObject.class };

    public Class[] getSerializableClasses() {
        return _serializableClasses;
    }

    public Class[] getJSONClasses() {
        return _JSONClasses;
    }

    public ObjectMatch tryUnmarshall(SerializerState state, Class clazz,
            Object o) throws UnmarshallException {
        JSONObject jso = (JSONObject) o;
        String java_class = jso.getString("javaClass");
        if (java_class == null)
            throw new UnmarshallException("no type hint");
        if (!(java_class.equals("com.webobjects.foundation.NSTimestamp")))
            throw new UnmarshallException("not a NSTimestamp");
        long time = jso.getLong("time");
        String tz = jso.getString("tz");
        return ObjectMatch.OKAY;
    }

    public Object unmarshall(SerializerState state, Class clazz, Object o)
            throws UnmarshallException {
        JSONObject jso = (JSONObject) o;
        long time = jso.getLong("time");
        String tz = jso.getString("tz");
        
        if (jso.has("javaClass")) {
            try {
                clazz = Class.forName(jso.getString("javaClass"));
            } catch (ClassNotFoundException cnfe) {
                throw new UnmarshallException(cnfe.getMessage());
            }
        }
        if (NSTimestamp.class.equals(clazz)) {
            return new NSTimestamp(time, NSTimeZone.getTimeZone(tz));
        }
        throw new UnmarshallException("invalid class " + clazz);
    }

    public Object marshall(SerializerState state, Object o)
            throws MarshallException {
        long time;
        String tz;

        if (o instanceof NSTimestamp) {
            time = ((NSTimestamp) o).getTime();
            tz = ((NSTimestamp)o).timeZone().getID();
        } else {
            throw new MarshallException("cannot marshall date using class "
                    + o.getClass());
        }
        JSONObject obj = new JSONObject();
        if (ser.getMarshallClassHints())
            obj.put("javaClass", o.getClass().getName());
        obj.put("time", time);
        obj.put("tz", tz);
        return obj;
    }

}
