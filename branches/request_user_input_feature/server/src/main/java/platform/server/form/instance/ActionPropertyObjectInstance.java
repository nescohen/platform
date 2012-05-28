package platform.server.form.instance;

import platform.interop.action.ClientAction;
import platform.server.caches.IdentityLazy;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.*;
import platform.server.logics.property.actions.FormEnvironment;
import platform.server.logics.property.actions.flow.FlowResult;
import platform.server.session.ExecutionEnvironment;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class ActionPropertyObjectInstance<P extends PropertyInterface> extends PropertyObjectInstance<P, ActionProperty<P>> {

    public ActionPropertyObjectInstance(ActionProperty<P> property, Map<P, ? extends PropertyObjectInterfaceInstance> mapping) {
        super(property, mapping);
    }

    @IdentityLazy
    public ActionPropertyObjectInstance<P> getRemappedPropertyObject(Map<? extends PropertyObjectInterfaceInstance, DataObject> mapKeyValues) {
        return new ActionPropertyObjectInstance<P>(property, remap(mapKeyValues));
    }

    public FlowResult execute(ExecutionEnvironment env, List<ClientAction> actions) throws SQLException {
        return execute(env, null, null, actions);
    }

    public FlowResult execute(ExecutionEnvironment env, ObjectValue requestValue, PropertyDrawInstance propertyDraw, List<ClientAction> actions) throws SQLException {
        return env.execute(property, getInterfaceValues(), new FormEnvironment<P>(mapping, propertyDraw), requestValue, actions);
    }

    public CalcPropertyObjectInstance<?> getDrawProperty() {
        return property.getWhereProperty().mapObjects(mapping);
    }
}
