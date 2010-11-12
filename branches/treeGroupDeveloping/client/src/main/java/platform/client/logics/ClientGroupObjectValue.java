package platform.client.logics;

import platform.base.OrderedMap;
import platform.client.form.GroupObjectController;
import platform.interop.ClassViewType;

import java.io.*;
import java.util.Map;
import java.util.Set;

import static platform.base.BaseUtils.*;

public class ClientGroupObjectValue extends OrderedMap<ClientObject, Object>
        implements Serializable {
    public ClientGroupObjectValue(ClientGroupObjectValue... clones) {
        super();
        for (ClientGroupObjectValue clone : clones) {
            putAll(clone);
        }
    }

    public ClientGroupObjectValue(DataInputStream inStream, ClientPropertyRead clientPropertyDraw, Set<ClientPropertyDraw> panelProperties, Map<ClientGroupObject, ClassViewType> classViews, Map<ClientGroupObject, GroupObjectController> controllers) throws IOException {
        for (ClientObject clientObject : clientPropertyDraw.getDeserializeList(panelProperties, classViews, controllers)) {
            put(clientObject, deserializeObject(inStream));
        }
    }

    public ClientGroupObjectValue(DataInputStream inStream, ClientGroupObject clientGroupObject) throws IOException {
        for (ClientObject clientObject : clientGroupObject) {
            put(clientObject, deserializeObject(inStream));
        }
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        for (Map.Entry<ClientObject, Object> objectValue : entrySet()) {
            serializeObject(outStream, objectValue.getValue());
        }
    }

    public void serialize(ClientPropertyDraw propertyDraw, DataOutputStream outStream) throws IOException {
        for (ClientGroupObject group : propertyDraw.columnGroupObjects) {
            for (ClientObject clientObject : group) {
                serializeObject(outStream, get(clientObject));
            }
        }
    }

    public byte[] serialize() throws IOException {
        return serialize((ClientPropertyDraw) null);
    }

    public byte[] serialize(ClientTreeGroup treeGroup) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream outStream = new DataOutputStream(byteStream);
        for (ClientGroupObject group : treeGroup.groups) {
            for (ClientObject clientObject : group) {
                //пока так
                if (containsKey(clientObject)) {
                    serializeObject(outStream, get(clientObject));
                }
            }
        }
        return byteStream.toByteArray();
    }

    public byte[] serialize(ClientPropertyDraw propertyDraw) throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        if (propertyDraw != null) {
            serialize(propertyDraw, new DataOutputStream(outStream));
        } else {
            serialize(new DataOutputStream(outStream));
        }
        return outStream.toByteArray();
    }

    public boolean contains(ClientGroupObjectValue other) {
        for (Map.Entry<ClientObject, Object> entry : other.entrySet()) {
            Object key = entry.getKey();
            if (!this.containsKey(key)) {
                return false;
            }

            if (!nullEquals(entry.getValue(), this.get(key))) {
                return false;
            }
        }

        return true;
    }

    public boolean contentEquals(ClientGroupObjectValue other) {
        return other != null && other.size() == size() && contains(other);
    }
}
