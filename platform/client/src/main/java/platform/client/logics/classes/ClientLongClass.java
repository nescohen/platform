package platform.client.logics.classes;

import platform.client.ClientResourceBundle;
import platform.gwt.view.classes.GLongType;
import platform.gwt.view.classes.GType;
import platform.interop.Data;

import java.text.ParseException;

public class ClientLongClass extends ClientIntegralClass implements ClientTypeClass {

    public final static ClientLongClass instance = new ClientLongClass();

    private final String sID = "LongClass";

    @Override
    public String getSID() {
        return sID;
    }

    public Class getJavaClass() {
        return Long.class;
    }

    public byte getTypeId() {
        return Data.LONG;
    }

    public Object parseString(String s) throws ParseException {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException nfe) {
            throw new ParseException(s + ClientResourceBundle.getString("logics.classes.can.not.be.converted.to.long"), 0);
        }
    }

    @Override
    public String formatString(Object obj) {
        return obj.toString();
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.long");
    }

    @Override
    public GType getGwtType() {
        return GLongType.instance;
    }
}
