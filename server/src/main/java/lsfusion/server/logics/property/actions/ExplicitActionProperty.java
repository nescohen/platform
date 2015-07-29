package lsfusion.server.logics.property.actions;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.actions.flow.ChangeFlowType;
import lsfusion.server.logics.property.actions.flow.FlowResult;
import lsfusion.server.logics.scripted.ScriptingActionProperty;

import java.sql.SQLException;

// с явным задание классов параметров (where определяется этими классами)
public abstract class ExplicitActionProperty extends BaseActionProperty<ClassPropertyInterface> {
    public boolean allowNullValue;

    protected ExplicitActionProperty(ValueClass... classes) {
        this("sys", classes);
    }

    protected ExplicitActionProperty(String caption, ValueClass[] classes) {
        super(caption, IsClassProperty.getInterfaces(classes));
    }

    protected abstract void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException;

    protected boolean allowNulls() {
        return false;
    }

    public final FlowResult aspectExecute(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        ImMap<ClassPropertyInterface, ? extends ObjectValue> keys = context.getKeys();
        ImMap<ClassPropertyInterface,DataObject> dataKeys = DataObject.filterDataObjects(keys);
        if(!allowNullValue && !allowNulls() && dataKeys.size() < keys.size())
            proceedNullException();
        else
            if(IsClassProperty.fitInterfaceClasses(context.getSession().getCurrentClasses(dataKeys))) { // если подходит по классам выполнем
                if (this instanceof ScriptingActionProperty)
                    ((ScriptingActionProperty) this).commonExecuteCustomDelegate(context);
                else
                    executeCustom(context);
            }
        return FlowResult.FINISH;
    }

    public CalcPropertyMapImplement<?, ClassPropertyInterface> calcWhereProperty() {
        return IsClassProperty.getProperty(interfaces);
    }

    protected boolean isVolatile() {
        return false;
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        return type == ChangeFlowType.VOLATILE && isVolatile();
    }
}
