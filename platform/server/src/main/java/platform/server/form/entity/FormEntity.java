package platform.server.form.entity;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.base.Subsets;
import platform.base.identity.DefaultIDGenerator;
import platform.base.identity.IDGenerator;
import platform.interop.Constants;
import platform.interop.action.ClientResultAction;
import platform.server.classes.ValueClass;
import platform.server.form.entity.filter.FilterEntity;
import platform.server.form.entity.filter.RegularFilterEntity;
import platform.server.form.entity.filter.RegularFilterGroupEntity;
import platform.server.form.instance.FormInstance;
import platform.server.form.navigator.NavigatorElement;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.FormView;
import platform.server.logics.BusinessLogics;
import platform.server.logics.linear.LP;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyClassImplement;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.ValueClassWrapper;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.logics.property.group.AbstractNode;
import platform.server.serialization.ServerContext;
import platform.server.serialization.ServerIdentitySerializable;
import platform.server.serialization.ServerSerializationPool;

import javax.swing.*;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class FormEntity<T extends BusinessLogics<T>> extends NavigatorElement<T> implements ServerIdentitySerializable {
    private final static Logger logger = Logger.getLogger(FormEntity.class.getName());

    public boolean isReadOnly() {
        return false;
    }

    public boolean shouldProceedDefaultDraw() {
        return true;
    }

    public List<GroupObjectEntity> groups = new ArrayList<GroupObjectEntity>();
    public List<TreeGroupEntity> treeGroups = new ArrayList<TreeGroupEntity>();
    public List<PropertyDrawEntity> propertyDraws = new ArrayList<PropertyDrawEntity>();
    public Set<FilterEntity> fixedFilters = new HashSet<FilterEntity>();
    public List<RegularFilterGroupEntity> regularFilterGroups = new ArrayList<RegularFilterGroupEntity>();

    public OrderedMap<OrderEntity<?>, Boolean> fixedOrders = new OrderedMap<OrderEntity<?>, Boolean>();

    public boolean isPrintForm;

    public FormEntity() {
    }

    protected FormEntity(int ID, String caption) {
        this(ID, caption, false);
    }

    FormEntity(int iID, String caption, boolean iisPrintForm) {
        this(null, iID, caption, iisPrintForm);
    }

    protected FormEntity(NavigatorElement parent, int iID, String caption) {
        this(parent, iID, caption, false);
    }

    protected FormEntity(NavigatorElement parent, int iID, String caption, boolean iisPrintForm) {
        super(parent, iID, caption);
        logger.info("Initializing form " + caption + "...");

        isPrintForm = iisPrintForm;
    }

    public void addFixedFilter(FilterEntity filter) {
        fixedFilters.add(filter);
    }

    public void addFixedOrder(OrderEntity order, boolean descending) {
        fixedOrders.put(order, descending);
    }

    public void addRegularFilterGroup(RegularFilterGroupEntity group) {
        regularFilterGroups.add(group);
    }

    protected RegularFilterGroupEntity addSingleRegularFilterGroup(FilterEntity ifilter, String iname, KeyStroke ikey) {

        RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
        filterGroup.addFilter(new RegularFilterEntity(genID(), ifilter, iname, ikey));
        addRegularFilterGroup(filterGroup);

        return filterGroup;
    }

    // счетчик идентификаторов
    private IDGenerator idGenerator = new DefaultIDGenerator();

    public IDGenerator getIDGenerator() {
        return idGenerator;
    }

    public int genID() {
        return idGenerator.idShift();
    }

    public GroupObjectEntity getGroupObject(int id) {
        for (GroupObjectEntity group : groups) {
            if (group.getID() == id) {
                return group;
            }
        }

        return null;
    }

    public TreeGroupEntity getTreeGroup(int id) {
        for (TreeGroupEntity treeGroup : treeGroups) {
            if (treeGroup.getID() == id) {
                return treeGroup;
            }
        }

        return null;
    }

    public ObjectEntity getObject(int id) {
        for (GroupObjectEntity group : groups) {
            for (ObjectEntity object : group.objects) {
                if (object.getID() == id) {
                    return object;
                }
            }
        }
        return null;
    }

    public RegularFilterGroupEntity getRegularFilterGroup(int id) {
        for (RegularFilterGroupEntity filterGroup : regularFilterGroups) {
            if (filterGroup.getID() == id) {
                return filterGroup;
            }
        }

        return null;
    }

    public RegularFilterEntity getRegularFilter(int id) {
        for (RegularFilterGroupEntity filterGroup : regularFilterGroups) {
            for (RegularFilterEntity filter : filterGroup.filters) {
                if (filter.getID() == id) {
                    return filter;
                }
            }
        }

        return null;
    }

    protected ObjectEntity addSingleGroupObject(ValueClass baseClass, String caption, Object... groups) {

        GroupObjectEntity groupObject = new GroupObjectEntity(genID());
        ObjectEntity object = new ObjectEntity(genID(), baseClass, caption);
        groupObject.add(object);
        addGroup(groupObject);

        addPropertyDraw(groups, false, object);

        return object;
    }

    protected ObjectEntity addSingleGroupObject(ValueClass baseClass, Object... groups) {
        return addSingleGroupObject(baseClass, null, groups);
    }

    protected void addTreeGroupObject(GroupObjectEntity... tGroups) {
        TreeGroupEntity treeGroup = new TreeGroupEntity();
        for (GroupObjectEntity group : tGroups) {
            if (!groups.contains(group)) {
                groups.add(group);
            }
            treeGroup.add(group);
        }

        treeGroups.add(treeGroup);
    }

    protected void addGroup(GroupObjectEntity group) {
        groups.add(group);
    }

    protected void addPropertyDraw(ObjectEntity object, Object... groups) {
        addPropertyDraw(groups, false, object);
    }

    protected void addPropertyDraw(ObjectEntity object1, ObjectEntity object2, Object... groups) {
        addPropertyDraw(groups, false, object1, object2);
    }

    protected void addPropertyDraw(ObjectEntity object1, ObjectEntity object2, ObjectEntity object3, Object... groups) {
        addPropertyDraw(groups, false, object1, object2, object3);
    }

    protected void addPropertyDraw(ObjectEntity object1, ObjectEntity object2, ObjectEntity object3, ObjectEntity object4, Object... groups) {
        addPropertyDraw(groups, false, object1, object2, object3, object4);
    }

    protected void addPropertyDraw(ObjectEntity object, boolean useObjSubsets, Object... groups) {
        addPropertyDraw(groups, useObjSubsets, object);
    }

    protected void addPropertyDraw(ObjectEntity object1, ObjectEntity object2, boolean useObjSubsets, Object... groups) {
        addPropertyDraw(groups, useObjSubsets, object1, object2);
    }

    protected void addPropertyDraw(ObjectEntity object1, ObjectEntity object2, ObjectEntity object3, boolean useObjSubsets, Object... groups) {
        addPropertyDraw(groups, useObjSubsets, object1, object2, object3);
    }

    protected void addPropertyDraw(ObjectEntity object1, ObjectEntity object2, ObjectEntity object3, ObjectEntity object4, boolean useObjSubsets, Object... groups) {
        addPropertyDraw(groups, useObjSubsets, object1, object2, object3, object4);
    }

    private void addPropertyDraw(Object[] groups, boolean useObjSubsets, ObjectEntity... objects) {

        for (int i = 0; i < groups.length; i++) {

            Object group = groups[i];
            if (group instanceof Boolean) {
//                continue;
            } else if (group instanceof AbstractNode) {
                boolean upClasses = false;
                if ((i + 1) < groups.length && groups[i + 1] instanceof Boolean) {
                    upClasses = (Boolean) groups[i + 1];
                }
                addPropertyDraw((AbstractNode) group, upClasses, useObjSubsets, objects);
            } else if (group instanceof LP) {
                this.addPropertyDraw((LP) group, objects);
            }
        }
    }

    protected void addPropertyDraw(AbstractNode group, boolean upClasses, ObjectEntity... objects) {
        addPropertyDraw(group, upClasses, null, false, objects);
    }

    protected void addPropertyDraw(AbstractNode group, boolean upClasses, boolean useObjSubsets, ObjectEntity... objects) {
        addPropertyDraw(group, upClasses, null, useObjSubsets, objects);
    }

    protected void addPropertyDraw(AbstractNode group, boolean upClasses, GroupObjectEntity groupObject, ObjectEntity... objects) {
        addPropertyDraw(group, upClasses, groupObject, false, objects);
    }

    protected void addPropertyDraw(AbstractNode group, boolean upClasses, GroupObjectEntity groupObject, boolean useObjSubsets, ObjectEntity... objects) {
        List<ValueClassWrapper> valueClasses = new ArrayList<ValueClassWrapper>();
        Map<ObjectEntity, ValueClassWrapper> objectToClass = new HashMap<ObjectEntity, ValueClassWrapper>();
        for (ObjectEntity object : objects) {
            ValueClassWrapper wrapper = new ValueClassWrapper(object.baseClass);
            valueClasses.add(wrapper);
            objectToClass.put(object, wrapper);
        }

        List<List<ValueClassWrapper>> classSubsets;
        if (useObjSubsets) {
            classSubsets = new ArrayList<List<ValueClassWrapper>>();
            for (Set<ValueClassWrapper> set : new Subsets<ValueClassWrapper>(valueClasses)) {
                List<ValueClassWrapper> objectList = new ArrayList<ValueClassWrapper>(set);
                if (!objectList.isEmpty()) {
                    classSubsets.add(objectList);
                }
            }
        } else {
            classSubsets = Collections.singletonList(valueClasses);
        }

        for (PropertyClassImplement implement : group.getProperties(classSubsets, upClasses)) {
            List<PropertyInterface> interfaces = new ArrayList<PropertyInterface>();
            Map<ObjectEntity, PropertyInterface> objectToInterface =
                    BaseUtils.<ObjectEntity, ValueClassWrapper, PropertyInterface>join(objectToClass, BaseUtils.reverse(implement.mapping));
            for (ObjectEntity object : objects) {
                interfaces.add(objectToInterface.get(object));
            }
            addPropertyDraw(new LP(implement.property, interfaces), groupObject, objects);
        }
    }

    public PropertyDrawEntity addPropertyDraw(LP property, ObjectEntity... objects) {
        return addPropertyDraw(property, null, objects);
    }

    public void addPropertyDraw(LP[] properties, ObjectEntity... objects) {
        Map<ValueClass, ObjectEntity> classToObject = new HashMap<ValueClass, ObjectEntity>();
        for (ObjectEntity object : objects) {
            assert classToObject.put(object.baseClass, object) == null; // ValueClass объектов не должны совпадать
        }

        for (LP property : properties) {
            List<ObjectEntity> orderedObjects =
                    BaseUtils.mapList(property.listInterfaces, BaseUtils.join(property.property.getMapClasses(), classToObject));
            addPropertyDraw(property, null, orderedObjects.toArray(new ObjectEntity[1]));
        }
    }

    protected <P extends PropertyInterface> PropertyDrawEntity addPropertyDraw(LP<P> property, GroupObjectEntity groupObject, ObjectEntity... objects) {

        return addPropertyDraw(groupObject, new PropertyObjectEntity<P>(property, objects));
    }

    public GroupObjectEntity getApplyObject(Collection<ObjectEntity> objects) {
        GroupObjectEntity result = null;
        for (GroupObjectEntity group : groups) {
            for (ObjectEntity object : group.objects) {
                if (objects.contains(object)) {
                    result = group;
                    break;
                }
            }
        }
        return result;
    }

    <P extends PropertyInterface> PropertyDrawEntity<P> addPropertyDraw(GroupObjectEntity groupObject, PropertyObjectEntity<P> propertyImplement) {

        PropertyDrawEntity<P> propertyDraw = new PropertyDrawEntity<P>(genID(), propertyImplement, groupObject);
        if (shouldProceedDefaultDraw()) {
            propertyImplement.property.proceedDefaultDraw(propertyDraw, this);
        }

        if (propertyImplement.property.sID != null) {

            // придется поискать есть ли еще такие sID, чтобы добиться уникальности sID
            boolean foundSID = false;
            for (PropertyDrawEntity property : propertyDraws) {
                if (BaseUtils.nullEquals(property.getSID(), propertyImplement.property.sID)) {
                    foundSID = true;
                    break;
                }
            }
            propertyDraw.setSID(propertyImplement.property.sID + ((foundSID) ? propertyDraw.getID() : ""));
        }


        int count = 0;
        for (PropertyDrawEntity property : propertyDraws) {
            if (property.shouldBeLast) {
                propertyDraws.add(count, propertyDraw);
                count = -1;
                break;
            }
            count++;
        }

        if (count >= 0) {
            propertyDraws.add(propertyDraw);
        }


        assert richDesign == null;

        return propertyDraw;
    }

    protected <P extends PropertyInterface> void removePropertyDraw(LP<P> property) {
        removePropertyDraw(property.property);
    }

    protected <P extends PropertyInterface> void removePropertyDraw(Property<P> property) {
        Iterator<PropertyDrawEntity> it = propertyDraws.iterator();
        while (it.hasNext()) {
            if (property.equals(it.next().propertyObject.property)) {
                it.remove();
            }
        }
    }

    protected void removePropertyDraw(AbstractGroup group) {
        Iterator<PropertyDrawEntity> it = propertyDraws.iterator();
        while (it.hasNext()) {
            if (group.hasChild(it.next().propertyObject.property)) {
                it.remove();
            }
        }
    }

    public PropertyObjectEntity addPropertyObject(LP property, PropertyObjectInterfaceEntity... objects) {

        return new PropertyObjectEntity(property, objects);
    }

    public PropertyDrawEntity<?> getPropertyDraw(int iID) {
        for (PropertyDrawEntity propertyDraw : propertyDraws) {
            if (propertyDraw.getID() == iID) {
                return propertyDraw;
            }
        }

        return null;
    }

    protected PropertyObjectEntity getPropertyObject(LP<?> lp) {
        return getPropertyDraw(lp).propertyObject;
    }

    public PropertyDrawEntity<?> getPropertyDraw(LP<?> lp) {
        return getPropertyDraw(lp.property);
    }

    protected PropertyObjectEntity getPropertyObject(LP<?> lp, ObjectEntity object) {
        return getPropertyDraw(lp, object).propertyObject;
    }

    protected PropertyObjectEntity getPropertyObject(LP<?> lp, GroupObjectEntity groupObject) {
        return getPropertyDraw(lp, groupObject).propertyObject;
    }

    protected PropertyDrawEntity<?> getPropertyDraw(LP<?> lp, ObjectEntity object) {
        return getPropertyDraw(lp.property, object.groupTo);
    }

    protected PropertyDrawEntity<?> getPropertyDraw(LP<?> lp, GroupObjectEntity groupObject) {
        return getPropertyDraw(lp.property, groupObject);
    }

    protected PropertyDrawEntity getPropertyDraw(PropertyObjectEntity property) {

        PropertyDrawEntity resultPropertyDraw = null;
        for (PropertyDrawEntity propertyDraw : propertyDraws) {
            if (propertyDraw.propertyObject.equals(property)) {
                resultPropertyDraw = propertyDraw;
            }
        }

        return resultPropertyDraw;
    }

    public PropertyObjectEntity getPropertyObject(Property property) {
        return getPropertyDraw(property).propertyObject;
    }

    protected PropertyDrawEntity getPropertyDraw(Property property) {

        PropertyDrawEntity resultPropertyDraw = null;
        for (PropertyDrawEntity<?> propertyDraw : propertyDraws) {
            if (propertyDraw.propertyObject.property == property) {
                resultPropertyDraw = propertyDraw;
            }
        }

        return resultPropertyDraw;
    }

    public PropertyDrawEntity getPropertyDraw(AbstractNode group, ObjectEntity object) {
        return getPropertyDraw(group, object.groupTo);
    }

    public PropertyDrawEntity getPropertyDraw(AbstractNode group, GroupObjectEntity groupObject) {

        PropertyDrawEntity resultPropertyDraw = null;
        for (PropertyDrawEntity propertyDraw : propertyDraws) {
            if (group.hasChild(propertyDraw.propertyObject.property) && groupObject.equals(propertyDraw.getToDraw(this))) {
                resultPropertyDraw = propertyDraw;
            }
        }

        return resultPropertyDraw;
    }

    public void addHintsNoUpdate(AbstractGroup group) {
        for (Property property : group.getProperties()) {
            addHintsNoUpdate(property);
        }
    }

    public Collection<Property> hintsNoUpdate = new HashSet<Property>();

    protected void addHintsNoUpdate(LP<?> prop) {
        addHintsNoUpdate(prop.property);
    }

    protected void addHintsNoUpdate(Property prop) {
        hintsNoUpdate.add(prop);
    }

    @Override
    public String getSID() {
        return Constants.getDefaultFormSID(ID);
    }

    public Map<PropertyDrawEntity, GroupObjectEntity> forceDefaultDraw = new HashMap<PropertyDrawEntity, GroupObjectEntity>();

    public FormView createDefaultRichDesign() {
        return new DefaultFormView(this);
    }

    public FormView richDesign;

    public FormView getRichDesign() {
        if (richDesign == null) {
            return createDefaultRichDesign();
        } else {
            return richDesign;
        }
    }

    protected GroupObjectHierarchy groupHierarchy;

    public GroupObjectHierarchy.ReportHierarchy getReportHierarchy() {
        if (groupHierarchy == null) {
            FormGroupHierarchyCreator creator = new FormGroupHierarchyCreator(this);
            groupHierarchy = creator.createHierarchy();
        }
        return groupHierarchy.createReportHierarchy();
    }

    public GroupObjectHierarchy getGroupHierarchy() {
        return groupHierarchy;
    }

    public ArrayList<NavigatorElement> relevantElements = new ArrayList<NavigatorElement>();

    public byte getTypeID() {
        return 0;
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.writeString(outStream, caption);
        outStream.writeBoolean(isPrintForm);

        pool.serializeCollection(outStream, groups);
        pool.serializeCollection(outStream, treeGroups);
        pool.serializeCollection(outStream, propertyDraws);
        pool.serializeCollection(outStream, fixedFilters);
        pool.serializeCollection(outStream, regularFilterGroups);
        pool.serializeMap(outStream, forceDefaultDraw);

        outStream.writeInt(autoActions.size());
        for (Map.Entry<ObjectEntity, List<PropertyObjectEntity>> entry : autoActions.entrySet()) {
            pool.serializeObject(outStream, entry.getKey());
            pool.serializeCollection(outStream, entry.getValue());
        }
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        caption = pool.readString(inStream);
        isPrintForm = inStream.readBoolean();

        groups = pool.deserializeList(inStream);
        treeGroups = pool.deserializeList(inStream);
        propertyDraws = pool.deserializeList(inStream);
        fixedFilters = pool.deserializeSet(inStream);
        regularFilterGroups = pool.deserializeList(inStream);
        forceDefaultDraw = pool.deserializeMap(inStream);

        autoActions = new HashMap<ObjectEntity, List<PropertyObjectEntity>>();
        int length = inStream.readInt();
        for (int i = 0; i < length; ++i) {
            ObjectEntity object = pool.deserializeObject(inStream);
            List<PropertyObjectEntity> actions = pool.deserializeList(inStream);
            autoActions.put(object, actions);
        }
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);
        outStream.writeBoolean(isPrintForm);
    }

    public Map<ObjectEntity, List<PropertyObjectEntity>> autoActions = new HashMap<ObjectEntity, List<PropertyObjectEntity>>();

    public void addAutoAction(ObjectEntity object, PropertyObjectEntity... actions) {
        addAutoAction(object, false, actions);
    }

    public void addAutoAction(ObjectEntity object, boolean drop, PropertyObjectEntity... actions) {
        List<PropertyObjectEntity> propertyActions = autoActions.get(object);
        if (propertyActions == null || drop) {
            propertyActions = new ArrayList<PropertyObjectEntity>();
            autoActions.put(object, propertyActions);
        }

        propertyActions.addAll(Arrays.asList(actions));
    }

    public boolean hasClientApply() {
        return false;
    }

    public ClientResultAction getClientApply(FormInstance<T> form) {
        return null; // будем возвращать именно null, чтобы меньше данных передавалось        
    }

    public String checkClientApply(Object result) {
        return null;
    }

    public static FormEntity<?> deserialize(BusinessLogics BL, byte[] formState) {
        return deserialize(BL, new DataInputStream(new ByteArrayInputStream(formState)));
    }

    public static FormEntity<?> deserialize(BusinessLogics BL, DataInputStream inStream) {
        try {
            FormEntity form = new ServerSerializationPool(new ServerContext(BL)).deserializeObject(inStream);
            form.richDesign = new ServerSerializationPool(new ServerContext(BL, form)).deserializeObject(inStream);

            return form;
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при десериализации формы на сервере", e);
        }
    }
}
