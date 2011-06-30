package platform.server.logics.property;

import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.Changes;
import platform.server.session.Modifier;

import java.util.List;
import java.util.Map;

abstract public class FormulaProperty<T extends PropertyInterface> extends NoIncrementProperty<T> {

    protected FormulaProperty(String sID, String caption, List<T> interfaces) {
        super(sID, caption, interfaces);
    }

    @Override
    public boolean check() {
        return true;
    }
}
