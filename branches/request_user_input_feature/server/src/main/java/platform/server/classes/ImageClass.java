package platform.server.classes;

import platform.interop.Data;
import platform.server.logics.ServerResourceBundle;

public class ImageClass extends FileClass {

    public final static ImageClass instance = new ImageClass();
    private final static String sid = "ImageClass";
    static {
        DataClass.storeClass(sid, instance);
    }

    protected ImageClass() {}

    public String toString() {
        return ServerResourceBundle.getString("classes.image");
    }

    public DataClass getCompatible(DataClass compClass) {
        return compClass instanceof ImageClass ? this : null;
    }

    public byte getTypeID() {
        return Data.IMAGE;
    }

    public String getSID() {
        return sid;
    }

    public String getExtensions() {
        return "jpg, jpeg, bmp, png";
    }
}
