package lsfusion.server.logics.classes.user.set;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.physics.dev.id.name.ClassCanonicalNameUtils;

public class ResolveUpClassSet extends AUpClassSet<ResolveUpClassSet> implements ResolveClassSet {
    public ResolveUpClassSet(CustomClass[] customClasses) {
        super(customClasses);
    }

    public ResolveUpClassSet(CustomClass cls) {
        super(cls);
    }

    protected ResolveUpClassSet createThis(CustomClass[] wheres) {
        return new ResolveUpClassSet(wheres);
    }

    protected CustomClass add(CustomClass addWhere, CustomClass[] wheres, int numWheres, CustomClass[] proceeded, int numProceeded) {
        return null;
    }

    public static final ResolveUpClassSet FALSE = new ResolveUpClassSet(new CustomClass[0]);
    protected ResolveUpClassSet FALSETHIS() {
        return FALSE;
    }

    public boolean containsAll(ResolveClassSet set, boolean implicitCast) {
        if(set instanceof ResolveOrObjectClassSet) {
            ResolveOrObjectClassSet orSet = (ResolveOrObjectClassSet)set;
            if(!containsAll(orSet.up, implicitCast))
                return false;
            for(ConcreteCustomClass customClass : orSet.set) {
                if(!has(customClass))
                    return false;
            }
            return true;
//            return new ResolveOrObjectClassSet(this, SetFact.<ConcreteCustomClass>EMPTY()).containsAll(set, implicitCast);
        }
        
        if(!(set instanceof ResolveUpClassSet))
            return false;
        
        ResolveUpClassSet upSet = ((ResolveUpClassSet)set);
        for(CustomClass upWhere : upSet.wheres)
            if(!has(upWhere))
                return false;
        return true;
    }

    public ValueClass getCommonClass() {
        return OrObjectClassSet.getCommonClass(SetFact.toSet(wheres));
    }

    public ResolveClassSet and(ResolveClassSet set) {
        if(set instanceof ResolveOrObjectClassSet)
            return set.and(this);

        if(!(set instanceof ResolveUpClassSet))
            return ResolveUpClassSet.FALSE;
        return and((ResolveUpClassSet)set);
    }

    public ResolveClassSet or(ResolveClassSet set) {
        return or((ResolveUpClassSet) set);
    }

    // мост между логикой вычислений и логикой infer / resolve

    // надо же куда то положить
    // может быть null
    public static ResolveClassSet toResolve(AndClassSet set) {
        if(set == null) return null;
        
        // будем assert'ить что сюда попадет, только то что используется в toAnd 
        return set.toResolve();
    }
    // может быть null
    private static AndClassSet toAnd(ResolveClassSet set) {
        if(set == null) return null;            
        return set.toAnd();
    }

    // могут быть null
    public static <T> ImMap<T, AndClassSet> toAnd(ImMap<T, ResolveClassSet> set) {
        return set.mapValues(set1 -> toAnd(set1));
    }

    public static <T> ImMap<T, ResolveClassSet> toResolve(ImMap<T, AndClassSet> set) {
        return set.mapValues(set1 -> toResolve(set1));
    }

    public UpClassSet toAnd() {
        return new UpClassSet(wheres);
    }

    @Override
    public String getCanonicalName() {
        return ClassCanonicalNameUtils.createName(this);
    }

    public CustomClass[] getCommonClasses() {
        return wheres;
    }

    @Override
    public boolean equalsCompatible(ResolveClassSet set) {
        return BaseUtils.hashEquals(this, set);
    }
}
