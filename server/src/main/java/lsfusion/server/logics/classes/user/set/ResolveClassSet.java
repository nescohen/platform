package lsfusion.server.logics.classes.user.set;

import lsfusion.server.data.type.Type;
import lsfusion.server.logics.classes.ValueClass;

// множество классов, от AndClassSet и OrClassSet, тем что abstract'ы не считаются конкретными, нет множества конкретных классов и т.п.
// используется в resolve'инге и выводе типов
public interface ResolveClassSet {
    
    boolean containsAll(ResolveClassSet set, boolean implicitCast);
    
    ResolveClassSet and(ResolveClassSet set);

    ResolveClassSet or(ResolveClassSet set);

    boolean isEmpty();
    
    Type getType();

    ValueClass getCommonClass();
    
    AndClassSet toAnd();

    String getCanonicalName();

    boolean equalsCompatible(ResolveClassSet set); // для поиска по каноническому имени
}
