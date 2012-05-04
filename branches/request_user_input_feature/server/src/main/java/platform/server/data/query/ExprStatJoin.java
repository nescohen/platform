package platform.server.data.query;

import platform.base.QuickSet;
import platform.base.Result;
import platform.base.TwinImmutableInterface;
import platform.server.caches.AbstractOuterContext;
import platform.server.caches.OuterContext;
import platform.server.caches.hash.HashContext;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.InnerExpr;
import platform.server.data.expr.NotNullExpr;
import platform.server.data.expr.NotNullExprSet;
import platform.server.data.expr.query.Stat;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.query.stat.WhereJoin;
import platform.server.data.translator.MapTranslate;
import platform.server.data.where.Where;

import java.util.Collections;
import java.util.Map;

public class ExprStatJoin extends ExprJoin<ExprStatJoin> {

    private final Stat stat;

    @Override
    public String toString() {
        return baseExpr + " - " + stat.toString();
    }

    protected Stat getStat() {
        return stat;
    }

    public ExprStatJoin(BaseExpr baseExpr, Stat stat) {
        super(baseExpr);
        this.stat = stat;
    }

    public StatKeys<Integer> getStatKeys(KeyStat keyStat) {
        return new StatKeys<Integer>(Collections.singleton(0), getStat());
    }

    protected int hash(HashContext hashContext) {
        return 31 * (31 * super.hash(hashContext) + stat.hashCode()) + 5;
    }

    protected ExprStatJoin translate(MapTranslate translator) {
        return new ExprStatJoin(baseExpr.translateOuter(translator), stat);
    }

    public boolean twins(TwinImmutableInterface o) {
        return super.twins(o) && stat.equals(((ExprStatJoin)o).stat);
    }
}
