package lsfusion.server.logics.property;

import lsfusion.base.FunctionSet;
import lsfusion.base.Pair;
import lsfusion.base.SFunctionSet;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.server.caches.IdentityInstanceLazy;
import lsfusion.server.caches.IdentityLazy;
import lsfusion.server.classes.ActionClass;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.actions.BaseEvent;
import lsfusion.server.logics.property.actions.FormEnvironment;
import lsfusion.server.logics.property.actions.SessionEnvEvent;
import lsfusion.server.logics.property.actions.SystemEvent;
import lsfusion.server.logics.property.actions.edit.GroupChangeActionProperty;
import lsfusion.server.logics.property.actions.flow.ChangeFlowType;
import lsfusion.server.logics.property.actions.flow.FlowResult;
import lsfusion.server.logics.property.actions.flow.ListCaseActionProperty;
import lsfusion.server.session.ExecutionEnvironment;

import java.sql.SQLException;

public abstract class ActionProperty<P extends PropertyInterface> extends Property<P> {

    public ActionProperty(String sID, String caption, ImOrderSet<P> interfaces) {
        super(sID, caption, interfaces);
    }

    public final static AddValue<CalcProperty, Boolean> addValue = new SymmAddValue<CalcProperty, Boolean>() {
        public Boolean addValue(CalcProperty key, Boolean prevValue, Boolean newValue) {
            return prevValue && newValue;
        }
    };

    // assert что возвращает только DataProperty и Set(IsClassProperty), Drop(IsClassProperty), IsClassProperty, для использования в лексикографике (calculateLinks)
    public ImMap<CalcProperty, Boolean> getChangeExtProps() {
        ActionPropertyMapImplement<?, P> compile = compile();
        if(compile!=null)
            return compile.property.getChangeExtProps();

        return aspectChangeExtProps();
    }

    // убирает Set и Drop, так как с depends будет использоваться
    public ImSet<CalcProperty> getChangeProps() {
        ImMap<CalcProperty, Boolean> changeExtProps = getChangeExtProps();
        int size = changeExtProps.size(); 
        MSet<CalcProperty> mResult = SetFact.mSetMax(size);
        for(int i=0;i<size;i++) {
            CalcProperty property = changeExtProps.getKey(i);
            if(property instanceof ChangedProperty)
                mResult.add((IsClassProperty)((ChangedProperty)property).property);
            else {
                assert property instanceof DataProperty || property instanceof ObjectClassProperty;
                mResult.add(property);
            }
        }

        return mResult.immutable();
    }
    // схема с аспектом сделана из-за того что getChangeProps для ChangeClassAction не инвариантен (меняется после компиляции), тоже самое и For с addObject'ом
    @IdentityLazy
    protected ImMap<CalcProperty, Boolean> aspectChangeExtProps() {
        MMap<CalcProperty, Boolean> result = MapFact.mMap(addValue);
        for(ActionProperty<?> dependAction : getDependActions())
            result.addAll(dependAction.getChangeExtProps());
        return result.immutable();
    }

    protected void markRecursions(ListCaseActionProperty recursiveAction) {
        for(ActionProperty action : getDependActions())
            action.markRecursions(recursiveAction);
    }

    public ImMap<CalcProperty, Boolean> getUsedExtProps() {
        ActionPropertyMapImplement<?, P> compile = compile();
        if(compile!=null)
            return compile.property.getUsedExtProps();

        return aspectUsedExtProps();
    }

    @IdentityLazy
    protected ImMap<CalcProperty, Boolean> aspectUsedExtProps() {
        MMap<CalcProperty, Boolean> result = MapFact.mMap(addValue);
        for(ActionProperty<?> dependAction : getDependActions())
            result.addAll(dependAction.getUsedExtProps());
        return result.immutable();
    }

    public ImSet<CalcProperty> getUsedProps() {
        return getUsedExtProps().keys();
    }

    protected static ImMap<CalcProperty, Boolean> getChangeProps(CalcProperty... props) {
        MMap<CalcProperty, Boolean> result = MapFact.mMap(addValue);
        for(CalcProperty element : props)
            result.addAll(element.getChangeProps().toMap(false));
        return result.immutable();
    }
    protected static <T extends PropertyInterface> ImMap<CalcProperty, Boolean> getUsedProps(CalcPropertyInterfaceImplement<T>... props) {
        return getUsedProps(SetFact.<CalcPropertyInterfaceImplement<T>>EMPTY(), props);
    }
    protected static <T extends PropertyInterface> ImMap<CalcProperty, Boolean> getUsedProps(ImCol<? extends CalcPropertyInterfaceImplement<T>> col, CalcPropertyInterfaceImplement<T>... props) {
        MSet<CalcProperty> mResult = SetFact.mSet();
        for(CalcPropertyInterfaceImplement<T> element : col)
            element.mapFillDepends(mResult);
        for(CalcPropertyInterfaceImplement<T> element : props)
            element.mapFillDepends(mResult);
        return mResult.immutable().toMap(false);
    }
    
    private FunctionSet<CalcProperty> usedProps;
    public FunctionSet<CalcProperty> getDependsUsedProps() {
        if(usedProps==null)
            usedProps = CalcProperty.getDependsFromSet(getUsedProps());
        return usedProps;
    }

    @IdentityLazy
    public boolean hasFlow(ChangeFlowType type) {
        for(ActionProperty<?> dependAction : getDependActions())
            if(dependAction.hasFlow(type))
                return true;
        return false;
    }

    @IdentityLazy
    public ImSet<SessionCalcProperty> getSessionCalcDepends(boolean events) {
        MSet<SessionCalcProperty> mResult = SetFact.mSet();
        for(CalcProperty property : getUsedProps())
            mResult.addAll(property.getSessionCalcDepends(events));
        return mResult.immutable();
    }

    public ImSet<OldProperty> getParseOldDepends() {
        MSet<OldProperty> mResult = SetFact.mSet();
        for(CalcProperty property : getUsedProps())
            mResult.addAll(property.getParseOldDepends());
        return mResult.immutable();
    }

    public abstract ImSet<ActionProperty> getDependActions();

    public ImMap<P, ValueClass> getInterfaceClasses(ClassType type) {
        return getWhereProperty().mapInterfaceClasses(type);
    }
    public ClassWhere<P> getClassWhere(ClassType type) {
        return getWhereProperty().mapClassWhere(type);
    }

    public abstract CalcPropertyMapImplement<?, P> getWhereProperty();

    @Override
    protected ImCol<Pair<Property<?>, LinkType>> calculateLinks() {
        if(getEvents().isEmpty()) // вырежем Action'ы без Event'ов, они нигде не используются, а дают много компонент связности
            return SetFact.EMPTY();

        LinkType linkType = hasFlow(ChangeFlowType.NEWSESSION) ? LinkType.RECUSED : LinkType.USEDACTION;

        MCol<Pair<Property<?>, LinkType>> mResult = ListFact.mCol();
        ImMap<CalcProperty, Boolean> used = getUsedExtProps();
        for(int i=0,size=used.size();i<size;i++)
            mResult.add(new Pair<Property<?>, LinkType>(used.getKey(i), used.getValue(i) ? LinkType.RECUSED : LinkType.USEDACTION));
        mResult.add(new Pair<Property<?>, LinkType>(getWhereProperty().property, linkType));
        CalcProperty depend = getStrongUsed();
        if(depend!=null)
            mResult.add(new Pair<Property<?>, LinkType>(depend, LinkType.EVENTACTION));
        return mResult.immutableCol();
    }
    
    public CalcProperty strongUsed = null;
    public void setStrongUsed(CalcProperty property) { // чисто для лексикографики
        strongUsed = property;
    }
    public CalcProperty getStrongUsed() {
        return strongUsed;
    }

    public <V extends PropertyInterface> ActionPropertyMapImplement<P, V> getImplement(ImOrderSet<V> list) {
        return new ActionPropertyMapImplement<P, V>(this, getMapInterfaces(list));
    }

    public Object events = MapFact.mExclMap();
    public void addEvent(BaseEvent event, SessionEnvEvent forms) {
        ((MExclMap<BaseEvent, SessionEnvEvent>)events).exclAdd(event, forms);
    }
    @LongMutable
    public ImMap<BaseEvent, SessionEnvEvent> getEvents() {
        return (ImMap<BaseEvent, SessionEnvEvent>)events;
    }
    public SessionEnvEvent getSessionEnv(BaseEvent event) {
        return getEvents().get(event);
    }

    public boolean singleApply = false;
    public boolean resolve = false;
    public boolean hasResolve() {
        return getSessionEnv(SystemEvent.APPLY)==SessionEnvEvent.ALWAYS && resolve;
    }
    
    private Object beforeAspects = ListFact.mCol();
    public void addBeforeAspect(ActionPropertyMapImplement<?, P> action) {
        ((MCol<ActionPropertyMapImplement<?, P>>)beforeAspects).add(action);
    }
    @LongMutable
    public ImCol<ActionPropertyMapImplement<?, P>> getBeforeAspects() {
        return (ImCol<ActionPropertyMapImplement<?,P>>)beforeAspects;
    }
    private Object afterAspects = ListFact.mCol();
    public void addAfterAspect(ActionPropertyMapImplement<?, P> action) {
        ((MCol<ActionPropertyMapImplement<?, P>>)afterAspects).add(action);
    }
    @LongMutable
    public ImCol<ActionPropertyMapImplement<?, P>> getAfterAspects() {
        return (ImCol<ActionPropertyMapImplement<?,P>>)afterAspects;
    }

    @Override
    public void finalizeAroundInit() {
        super.finalizeAroundInit();

        beforeAspects = ((MCol<ActionPropertyMapImplement<?, P>>)beforeAspects).immutableCol();
        afterAspects = ((MCol<ActionPropertyMapImplement<?, P>>)afterAspects).immutableCol();
        events = ((MMap<BaseEvent, SessionEnvEvent>)events).immutable();
    }

    public FlowResult execute(ExecutionContext<P> context) throws SQLException {
        for(ActionPropertyMapImplement<?, P> aspect : getBeforeAspects()) {
            FlowResult beforeResult = aspect.execute(context);
            if(beforeResult != FlowResult.FINISH)
                return beforeResult;
        }

        ActionPropertyMapImplement<?, P> compile = compile();
        if(compile!=null)
            return compile.execute(context);
        
        FlowResult result = aspectExecute(context);

        for(ActionPropertyMapImplement<?, P> aspect : getAfterAspects())
            aspect.execute(context);

        return result;
    }

    public void prereadCaches() {
        compile();
        getInterfaceClasses(ClassType.ASIS);
        getInterfaceClasses(ClassType.FULL);
    }

    protected abstract FlowResult aspectExecute(ExecutionContext<P> context) throws SQLException;

    public ActionPropertyMapImplement<P, P> getImplement() {
        return new ActionPropertyMapImplement<P, P>(this, getIdentityInterfaces());
    }

    public void execute(ExecutionEnvironment env) throws SQLException {
        assert interfaces.size()==0;
        execute(MapFact.<P, DataObject>EMPTY(), env, null);
    }

    public void execute(ImMap<P, ? extends ObjectValue> keys, ExecutionEnvironment env, FormEnvironment<P> formEnv) throws SQLException {
        env.execute(this, keys, formEnv, null, null);
    }

    public ValueClass getValueClass() {
        return ActionClass.instance;
    }

    @Override
    public ActionPropertyMapImplement<?, P> getDefaultEditAction(String editActionSID, CalcProperty filterProperty) {
        return getImplement();
    }

    // если этот action используется как действие для редактирования свойства, проверять ли это свойство на readOnly
    public boolean checkReadOnly = true;

    /**
     * возвращает тип для "простого" редактирования, когда этот action используется в качестве действия для редактирования </br>
     * assert, что тип будет DataClass, т.к. для остальных такое редактирование невозможно...
     * @param optimistic - если true, то если для некоторых случаев нельзя вывести тип, то эти случае будут игнорироваться
     */
    public Type getSimpleRequestInputType(boolean optimistic) {
        return null;
    }

    // по аналогии с верхним, assert что !hasChildren
    public CustomClass getSimpleAdd() {
        return null;
    }

    public P getSimpleDelete() {
        return null;
    }

    protected ActionPropertyClassImplement<P> createClassImplement(ImOrderSet<ValueClassWrapper> classes, ImOrderSet<P> mapping) {
        return new ActionPropertyClassImplement<P>(this, classes, mapping);
    }

    @IdentityInstanceLazy
    public ActionPropertyMapImplement<?, P> getGroupChange() {
        ActionPropertyMapImplement<P, P> changeImplement = getImplement();
        ImOrderSet<P> listInterfaces = getOrderInterfaces();

        GroupChangeActionProperty groupChangeActionProperty = new GroupChangeActionProperty("GCH" + getSID(), "sys", listInterfaces, changeImplement);
        return groupChangeActionProperty.getImplement(listInterfaces);
    }

    @IdentityLazy
    public ImSet<OldProperty> getSessionEventOldDepends() { // assert что OldProperty, при этом у которых Scope соответствующий локальному событию
        assert getSessionEnv(SystemEvent.SESSION)!=null;
        return getOldDepends().filterFn(new SFunctionSet<OldProperty>() {
            public boolean contains(OldProperty element) {
                return element.scope == PrevScope.EVENT;
            }});
    }

    public ActionPropertyMapImplement<?, P> compile() {
       return null;
    }

    public ImList<ActionPropertyMapImplement<?, P>> getList() {
        return ListFact.<ActionPropertyMapImplement<?, P>>singleton(getImplement());
    }
    public <T extends PropertyInterface, PW extends PropertyInterface> boolean hasPushFor(ImRevMap<P, T> mapping, ImSet<T> context, boolean ordersNotNull) {
        return false;
    }
    public <T extends PropertyInterface, PW extends PropertyInterface> CalcProperty getPushWhere(ImRevMap<P, T> mapping, ImSet<T> context, boolean ordersNotNull) {
        throw new RuntimeException("should not be");
    }
    public <T extends PropertyInterface, PW extends PropertyInterface> ActionPropertyMapImplement<?,T> pushFor(ImRevMap<P, T> mapping, ImSet<T> context, CalcPropertyMapImplement<PW, T> where, ImOrderMap<CalcPropertyInterfaceImplement<T>, Boolean> orders, boolean ordersNotNull) {
        throw new RuntimeException("should not be");
    }

    protected void proceedNullException() {
    }
}
