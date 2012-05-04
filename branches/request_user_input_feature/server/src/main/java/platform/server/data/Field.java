package platform.server.data;

import platform.base.TwinImmutableInterface;
import platform.base.TwinImmutableObject;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.type.Type;
import platform.server.data.type.TypeSerializer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

public abstract class Field extends TwinImmutableObject {
    public String name;
    public Type type;

    public static <F extends Field> Type.Getter<F> typeGetter() {
        return new Type.Getter<F> () {
            public Type getType(F key) {
                return key.type;
            }
        };
    }

    protected Field(String name,Type type) {
        this.name = name;
        this.type = type;
    }

    public static String getDeclare(Map<String, Type> map, SQLSyntax syntax) {
        String result = "";
        for(Map.Entry<String, Type> entry : map.entrySet())
            result = (result.length()==0?"":result + ',') + entry.getKey() + " " + entry.getValue().getDB(syntax);
        return result;
    }
    
    public String getDeclare(SQLSyntax syntax) {
        return name + " " + type.getDB(syntax);
    }

    public String toString() {
        return name;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeByte(getType());
        outStream.writeUTF(name);
        TypeSerializer.serializeType(outStream,type);
    }

    protected Field(DataInputStream inStream) throws IOException {
        name = inStream.readUTF();
        type = TypeSerializer.deserializeType(inStream);
    }

    public static Field deserialize(DataInputStream inStream) throws IOException {
        int type = inStream.readByte();
        if(type==0) return new KeyField(inStream);
        if(type==1) return new PropertyField(inStream);

        throw new IOException();
    }

    abstract byte getType();

    public boolean twins(TwinImmutableInterface o) {
        return name.equals(((Field)o).name) && type.equals(((Field)o).type);
    }

    public int immutableHashCode() {
        return (getClass().hashCode() * 31 + name.hashCode()) * 31 + type.hashCode();
    }
}
