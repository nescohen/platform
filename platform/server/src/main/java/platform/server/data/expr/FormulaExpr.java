package platform.server.data.expr;

import platform.base.BaseUtils;
import platform.server.caches.IdentityLazy;
import platform.server.caches.ParamLazy;
import platform.server.caches.hash.HashContext;
import platform.server.classes.ConcreteValueClass;
import platform.server.data.expr.cases.CaseExpr;
import platform.server.data.expr.cases.ExprCaseList;
import platform.server.data.expr.cases.MapCase;
import platform.server.data.expr.where.MapWhere;
import platform.server.data.query.AbstractSourceJoin;
import platform.server.data.query.CompileSource;
import platform.server.data.query.ExprEnumerator;
import platform.server.data.query.JoinData;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.translator.TranslateExprLazy;
import platform.server.data.type.Type;
import platform.server.data.where.Where;

import java.util.Map;

@TranslateExprLazy
public class FormulaExpr extends StaticClassExpr {

    private final String formula;
    private final ConcreteValueClass valueClass;
    private final Map<String, BaseExpr> params;

    // этот конструктор напрямую можно использовать только заведомо зная что getClassWhere не null или через оболочку create 
    private FormulaExpr(String formula,Map<String, BaseExpr> params, ConcreteValueClass valueClass) {
        this.formula = formula;
        this.params = params;
        this.valueClass = valueClass;
    }

    public static Expr create(String formula, ConcreteValueClass value,Map<String,? extends Expr> params) {
        ExprCaseList result = new ExprCaseList();
        for(MapCase<String> mapCase : CaseExpr.pullCases(params))
            result.add(mapCase.where, BaseExpr.create(new FormulaExpr(formula, mapCase.data, value)));
        return result.getExpr();
    }

    public void enumDepends(ExprEnumerator enumerator) {
        enumerator.fill(params);
    }

    public void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        for(BaseExpr param : params.values())
            param.fillJoinWheres(joins, andWhere);
    }

    public String getSource(CompileSource compile) {
        String sourceString = formula;
        for(String prm : params.keySet())
            sourceString = sourceString.replace(prm, params.get(prm).getSource(compile));
         return "("+sourceString+")";
     }

    public Type getType(KeyType keyType) {
        return valueClass.getType();
    }

    @ParamLazy
    public Expr translateQuery(QueryTranslator translator) {
        return create(formula, valueClass, translator.translate(params));
    }

    @ParamLazy
    public BaseExpr translateOuter(MapTranslate translator) {
        return new FormulaExpr(formula,translator.translateDirect(params),valueClass);
    }

    @Override
    public Expr packFollowFalse(Where where) {
        Map<String, Expr> packParams = packFollowFalse(params, where);
        if(!BaseUtils.hashEquals(packParams, params)) 
            return create(formula, valueClass, packParams);
        else
            return this;
    }

    // возвращает Where без следствий
    public Where calculateWhere() {
        return getWhere(params);
    }

    public VariableExprSet calculateExprFollows() {
        return InnerExpr.getExprFollows(params);
    }

    public boolean twins(AbstractSourceJoin o) {
        return formula.equals(((FormulaExpr) o).formula) && params.equals(((FormulaExpr) o).params) && valueClass.equals(((FormulaExpr) o).valueClass);
    }

    @IdentityLazy
    public int hashOuter(HashContext hashContext) {
        int hash = 0;
        for(Map.Entry<String, BaseExpr> param : params.entrySet())
            hash += param.getKey().hashCode() ^ param.getValue().hashOuter(hashContext);
        return valueClass.hashCode()*31*31 + hash*31 + formula.hashCode();
    }

    public ConcreteValueClass getStaticClass() {
        return valueClass;
    }

    public long calculateComplexity() {
        return getComplexity(params.values());
    }
}

