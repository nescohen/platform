package platform.server.form.view;

import platform.base.identity.IDGenerator;
import platform.interop.form.layout.AbstractGroupObject;
import platform.server.form.entity.GroupObjectEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.serialization.ServerIdentitySerializable;
import platform.server.serialization.ServerSerializationPool;

import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class GroupObjectView extends ArrayList<ObjectView> implements ServerIdentitySerializable, AbstractGroupObject<ComponentView> {

    public GroupObjectEntity entity;

    public Boolean needVerticalScroll = true;
    public Integer tableRowsCount;

    public GroupObjectView() {
    }

    public ObjectView getObjectView(ObjectEntity object) {
        for (ObjectView view : this) {
            if (view.entity.equals(object)) {
                return view;
            }
        }
        return null;
    }

    public GroupObjectView(IDGenerator idGen, GroupObjectEntity entity) {
        this.entity = entity;

        for (ObjectEntity object : this.entity.objects)
            add(new ObjectView(idGen, object, this));

        grid = new GridView(idGen.idShift(), this);
        showType = new ShowTypeView(idGen.idShift(), this);
    }

    public GroupObjectView(IDGenerator idGen, GroupObjectEntity entity, GridView grid, ShowTypeView showType) {
        this.entity = entity;

        for (ObjectEntity object : this.entity.objects)
            add(new ObjectView(idGen, object, this, false));

        this.grid = grid;
        this.grid.groupObject = this;
        this.showType = showType;
        this.showType.groupObject = this;

    }

    public GridView grid;
    public ShowTypeView showType;

    public String getCaption() {
        return get(0).getCaption();
    }

    public int getID() {
        return entity.getID();
    }

    public ComponentView getGrid() {
        return grid;
    }

    public ComponentView getShowType() {
        return showType;
    }

    int ID;

    public void setID(int iID) {
        ID = iID;
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.writeObject(outStream, entity.banClassView);
        pool.serializeCollection(outStream, this, serializationType);
        pool.serializeObject(outStream, pool.context.view.getTreeGroup(entity.treeGroup));

        pool.serializeObject(outStream, grid, serializationType);
        pool.serializeObject(outStream, showType, serializationType);
        pool.serializeObject(outStream, pool.context.view.getProperty(entity.filterProperty));

        outStream.writeBoolean(entity.isParent != null);

        boolean needVScroll;
        if (needVerticalScroll == null) {
            needVScroll = (entity.pageSize != null && entity.pageSize == 0);
        } else {
            needVScroll = needVerticalScroll;
        }
        pool.writeInt(outStream, entity.pageSize);
        outStream.writeBoolean(needVScroll);
        outStream.writeInt(tableRowsCount == null ? -1 : tableRowsCount);
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        entity = pool.context.entity.getGroupObject(ID);

        pool.deserializeCollection(this, inStream);

        grid = pool.deserializeObject(inStream);
        showType = pool.deserializeObject(inStream);
        needVerticalScroll = inStream.readBoolean();
        tableRowsCount = inStream.readInt();
    }

    public void setTableRowsCount(int tableRowsCount) {
        this.tableRowsCount = tableRowsCount;
        grid.constraints.fillVertical = 0.0;
    }
}
