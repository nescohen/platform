package platform.server.data;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.server.caches.AbstractValuesContext;
import platform.server.classes.BaseClass;
import platform.server.data.expr.Expr;
import platform.server.data.query.Join;
import platform.server.data.query.Query;
import platform.server.data.where.classes.ClassWhere;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;

import java.sql.SQLException;
import java.util.*;

public abstract class SessionDataInterface<T extends SessionDataInterface<T>> extends AbstractValuesContext<T> {

    public abstract List<KeyField> getKeys();
    public abstract Set<PropertyField> getProperties();

    public abstract Join<PropertyField> join(final Map<KeyField, ? extends Expr> joinImplement);

    public abstract void drop(SQLSession session, Object owner) throws SQLException;

    public abstract boolean used(Query<?, ?> query);

    public abstract SessionDataInterface insertRecord(SQLSession session, Map<KeyField, DataObject> keyFields, Map<PropertyField, ObjectValue> propFields, boolean update, boolean groupLast, Object owner) throws SQLException;

    public abstract SessionDataInterface deleteRecords(SQLSession session, Map<KeyField,DataObject> keys) throws SQLException;

    public abstract SessionDataInterface deleteKey(SQLSession session, KeyField mapField, DataObject object) throws SQLException;

    public abstract SessionDataInterface deleteProperty(SQLSession session, PropertyField property, DataObject object) throws SQLException;

    public abstract void out(SQLSession session) throws SQLException;

    public abstract ClassWhere<KeyField> getClassWhere();
    public abstract ClassWhere<Field> getClassWhere(PropertyField property);

    public abstract boolean isEmpty();

    private static SessionDataInterface write(final SQLSession session, List<KeyField> keys, Set<PropertyField> properties, Query<KeyField, PropertyField> query, BaseClass baseClass, final QueryEnvironment env, Object owner) throws SQLException {

        assert properties.equals(query.properties.keySet());

        Map<KeyField, Expr> keyExprValues = new HashMap<KeyField, Expr>();
        Map<PropertyField, Expr> propExprValues = new HashMap<PropertyField, Expr>();
        final Query<KeyField, PropertyField> pullQuery = query.pullValues(keyExprValues, propExprValues);

        Map<KeyField, DataObject> keyValues = new HashMap<KeyField, DataObject>();
        for(Map.Entry<KeyField, ObjectValue> keyValue : Expr.readValues(session, baseClass, keyExprValues, env).entrySet())
            if(keyValue.getValue() instanceof DataObject)
                keyValues.put(keyValue.getKey(), (DataObject) keyValue.getValue());
            else
                return new SessionRows(keys, properties); // если null в ключах можно валить
        Map<PropertyField, ObjectValue> propValues = Expr.readValues(session, baseClass, propExprValues, env);

        // читаем классы не считывая данные
        Map<PropertyField,ClassWhere<Field>> insertClasses = new HashMap<PropertyField, ClassWhere<Field>>();
        for(PropertyField field : pullQuery.properties.keySet())
            insertClasses.put(field,pullQuery.<Field>getClassWhere(Collections.singleton(field)));

        SessionTable table = new SessionTable(session, BaseUtils.filterList(keys, pullQuery.mapKeys.keySet()), pullQuery.properties.keySet(), null, new FillTemporaryTable() {
            public Integer fill(String name) throws SQLException {
                return session.insertSessionSelect(name, pullQuery, env);
            }
        }, pullQuery.<KeyField>getClassWhere(new ArrayList<PropertyField>()), insertClasses, owner);
        // нужно прочитать то что записано
        if(table.count > SessionRows.MAX_ROWS)
            return new SessionDataTable(table, keys, keyValues, propValues);
        else {
            OrderedMap<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> readRows = table.read(session, baseClass);

            table.drop(session, owner); // выкидываем таблицу

            // надо бы batch update сделать, то есть зная уже сколько запискй
            SessionRows sessionRows = new SessionRows(keys, properties);
            for (Iterator<Map.Entry<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>>> iterator = readRows.entrySet().iterator(); iterator.hasNext();) {
                Map.Entry<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> writeRow = iterator.next();
                sessionRows = (SessionRows) sessionRows.insertRecord(session, BaseUtils.merge(writeRow.getKey(), keyValues), BaseUtils.merge(writeRow.getValue(), propValues), false, !iterator.hasNext(), owner);
            }
            return sessionRows;
        }
    }

    public SessionDataInterface rewrite(SQLSession session, Query<KeyField, PropertyField> query, BaseClass baseClass, QueryEnvironment env, Object owner) throws SQLException {
        boolean used = used(query);
        if(!used)
            drop(session, owner);

        SessionDataInterface result = write(session, getKeys(), getProperties(), query, baseClass, env, owner);

        if(used)
            drop(session, owner);
        return result;
    }

    // "обновляет" ключи в таблице
    public SessionDataInterface rewrite(SQLSession session, Collection<Map<KeyField, DataObject>> writeRows, Object owner) throws SQLException {
        assert getProperties().isEmpty();
        drop(session, owner);

        Map<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> data = BaseUtils.toMap(writeRows, (Map<PropertyField, ObjectValue>) new HashMap<PropertyField, ObjectValue>());

        if(writeRows.size()> SessionRows.MAX_ROWS)
            return new SessionDataTable(session, getKeys(), new HashSet<PropertyField>(), data, true, owner);
        else
            return new SessionRows(getKeys(), new HashSet<PropertyField>(), data);
    }

}
