package platform.client.logics;

import platform.client.serialization.ClientSerializationPool;
import platform.base.context.ApplicationContext;
import platform.interop.form.layout.SimplexConstraints;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClientRegularFilterGroup extends ClientComponent {

    public List<ClientRegularFilter> filters = new ArrayList<ClientRegularFilter>();

    public int defaultFilter = -1;

    public ClientRegularFilterGroup() {

    }

    public ClientRegularFilterGroup(int ID, ApplicationContext context) {
        super(ID, context);
    }

    @Override
    public SimplexConstraints<ClientComponent> getDefaultConstraints() {
        return SimplexConstraints.getRegularFilterGroupDefaultConstraints(super.getDefaultConstraints());
    }

    @Override
    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);

        pool.serializeCollection(outStream, filters);
    }

    @Override
    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        filters = pool.deserializeList(inStream);

        defaultFilter = inStream.readInt();
    }

    @Override
    public String getCaption() {
        return "Фильтр";
    }

    @Override
    public String toString() {
        return filters.toString();
    }

    @Override
    public String getCodeConstructor(String name) {
        return "RegularFilterGroupView " + name + " = createRegularFilterGroup(" + getID() + ")";
    }
}
