package platform.server.data.expr;

import platform.base.TwinImmutableInterface;
import platform.server.caches.hash.HashContext;
import platform.server.classes.DataClass;
import platform.server.data.query.CompileSource;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;

import java.util.Collection;
import java.util.Map;

public class FormulaUnionExpr extends UnionExpr {

    private final String formula;
    private final DataClass dataClass;
    private final Map<String, Expr> params;

    public FormulaUnionExpr(String formula, DataClass dataClass, Map<String, Expr> params) {
        this.formula = formula;
        this.dataClass = dataClass;
        this.params = params;
    }

    public DataClass getStaticClass() {
        return dataClass;
    }

    protected Collection<Expr> getParams() {
        return params.values();
    }

    protected BaseExpr translate(MapTranslate translator) {
        return new FormulaUnionExpr(formula, dataClass, translator.translate(params));
    }

    public Expr translateQuery(QueryTranslator translator) {
        return new FormulaUnionExpr(formula, dataClass, translator.translate(params));
    }

    public String getSource(CompileSource compile) {
        return FormulaExpr.getSource(formula, params, compile);
    }

    protected int hash(HashContext hashContext) {
        return (formula.hashCode() * 31 + dataClass.hashCode()) * 31 + hashOuter(params, hashContext);
    }

    public boolean twins(TwinImmutableInterface o) {
        return formula.equals(((FormulaUnionExpr)o).formula) && dataClass.equals(((FormulaUnionExpr)o).dataClass) && params.equals(((FormulaUnionExpr)o).params);
    }
}
