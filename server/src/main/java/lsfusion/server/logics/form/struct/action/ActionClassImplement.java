package lsfusion.server.logics.form.struct.action;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.language.action.LA;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.form.struct.ValueClassWrapper;
import lsfusion.server.logics.form.struct.property.oraction.ActionOrPropertyClassImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class ActionClassImplement<P extends PropertyInterface> extends ActionOrPropertyClassImplement<P, Action<P>> {

    public ActionClassImplement(Action<P> property, ImOrderSet<ValueClassWrapper> classes, ImOrderSet<P> interfaces) {
        super(property, classes, interfaces);
    }

    public ActionClassImplement(Action<P> property, ImRevMap<P, ValueClassWrapper> mapping) {
        super(property, mapping);
    }

    public LA<P> createLP(ImOrderSet<ValueClassWrapper> listInterfaces, boolean prev) {
        return new LA<>(actionOrProperty, listInterfaces.mapOrder(mapping.reverse()));
    }

    public ActionClassImplement<P> map(ImRevMap<ValueClassWrapper, ValueClassWrapper> remap) {
        return new ActionClassImplement<>(actionOrProperty, mapping.join(remap));
    }
}
