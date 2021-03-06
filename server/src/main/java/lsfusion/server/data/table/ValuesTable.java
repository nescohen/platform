package lsfusion.server.data.table;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.data.query.compile.CompileSource;
import lsfusion.server.data.stat.DistinctKeys;
import lsfusion.server.data.stat.PropStat;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.stat.TableStatKeys;

import java.util.function.Function;

public class ValuesTable extends Table {

    private final SessionRows rows;
    public ValuesTable(SessionRows rows) {
        super(rows.getOrderKeys(), rows.getProperties(), rows.getClassWhere(), rows.getPropertyClasses());
        this.rows = rows;
        
        assert keys.size() == 1 && properties.isEmpty(); // пока реализуем для частного случая
    }

    @IdentityLazy
    @Override
    public TableStatKeys getTableStatKeys() {
        final Stat stat = new Stat(rows.getCount());
        return new TableStatKeys(stat, new DistinctKeys<>(keys.getSet().mapValues(new Function<KeyField, Stat>() {
            @Override
            public Stat apply(KeyField value) {
                return stat;
            }
        })));
    }

    @IdentityLazy
    @Override
    public ImMap<PropertyField, PropStat> getStatProps() {
        return properties.toMap(new PropStat(new Stat(rows.getCount())));
    }

    @Override
    public String getQuerySource(CompileSource source) {
        ImOrderSet<Field> fields = SetFact.addOrderExcl(keys, properties.toOrderSet());
        String values = rows.getQuerySource(source.syntax, fields);

        String fieldNames = fields.toString(Field.nameGetter(), ",");
        return "(SELECT " + fieldNames + " FROM (VALUES " + values + ") t (" + fields + "))";
    }

    public String toString() {
        return rows.toString();
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return rows.equals(((ValuesTable)o).rows) && super.calcTwins(o);
    }

    public int immutableHashCode() {
        return 31 * super.immutableHashCode() + rows.hashCode();
    }
    
}
