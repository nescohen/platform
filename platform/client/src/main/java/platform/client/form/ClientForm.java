/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platform.client.form;

import platform.base.BaseUtils;
import platform.base.DefaultIDGenerator;
import platform.base.IDGenerator;
import platform.client.Log;
import platform.client.Main;
import platform.client.SwingUtils;
import platform.client.logics.*;
import platform.client.logics.classes.ClientConcreteClass;
import platform.client.logics.classes.ClientObjectClass;
import platform.client.logics.filter.ClientPropertyFilter;
import platform.client.navigator.ClientNavigator;
import platform.interop.CompressingInputStream;
import platform.interop.Order;
import platform.interop.Scroll;
import platform.interop.action.ClientAction;
import platform.interop.action.ClientApply;
import platform.interop.action.CheckFailed;
import platform.interop.form.RemoteFormInterface;
import platform.interop.form.RemoteChanges;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class ClientForm {

    private final ClientFormView formView;

    public final RemoteFormInterface remoteForm;
    public final ClientNavigator clientNavigator;
    public final ClientFormActionDispatcher actionDispatcher;

    public boolean isDialogMode() {
        return false;
    }

    public boolean isReadOnlyMode() {
        return formView.readOnly;
    }

    private static IDGenerator idGenerator = new DefaultIDGenerator();
    private int ID;
    public int getID() {
        return ID;
    }

    public KeyStroke getKeyStroke() {
        return formView.keyStroke;
    }

    public String getCaption() {
        return  formView.caption;
    }

    public String getFullCaption() {
        return  formView.getFullCaption();
    }

    private static final Map<Integer, ClientFormView> cacheClientFormView = new HashMap<Integer, ClientFormView>();

    private static ClientFormView cacheClientFormView(RemoteFormInterface remoteForm) throws IOException, ClassNotFoundException {

        int ID = remoteForm.getID();

        if (!cacheClientFormView.containsKey(ID)) {

            byte[] state = remoteForm.getRichDesignByteArray();
            Log.incrementBytesReceived(state.length);

            cacheClientFormView.put(ID, new ClientFormView(new DataInputStream(new CompressingInputStream(new ByteArrayInputStream(state)))));
        }

        return cacheClientFormView.get(ID);
    }

    public ClientForm(RemoteFormInterface remoteForm, ClientNavigator clientNavigator) throws IOException, ClassNotFoundException {

        ID = idGenerator.idShift();

        // Форма нужна, чтобы с ней общаться по поводу данных и прочих
        this.remoteForm = remoteForm;

        // Навигатор нужен, чтобы уведомлять его об изменениях активных объектов, чтобы он мог себя переобновлять
        this.clientNavigator = clientNavigator;

        actionDispatcher = new ClientFormActionDispatcher(this.clientNavigator);

        formView = cacheClientFormView(remoteForm);

        initializeForm();
    }

    // ------------------------------------------------------------------------------------ //
    // ----------------------------------- Инициализация ---------------------------------- //
    // ------------------------------------------------------------------------------------ //

    private ClientFormLayout formLayout;

    private Map<ClientGroupObjectImplementView, GroupObjectController> controllers;

    private JButton buttonApply;
    private JButton buttonCancel;

    public ClientFormLayout getComponent() {
        return formLayout;
    }

    void initializeForm() throws IOException {

        formLayout = new ClientFormLayout(formView.containers) {

            @Override
            protected void gainedFocus() {

                try {
                    remoteForm.gainedFocus();
                    clientNavigator.currentFormChanged();

                    // если вдруг изменились данные в сессии
                    ClientExternalScreen.invalidate(getID());
                    applyRemoteChanges();
                } catch (IOException e) {
                    throw new RuntimeException("Ошибка при активации формы", e);
                }
            }
        };

//        setContentPane(formLayout.getComponent());
//        setComponent(formLayout.getComponent());

        initializeGroupObjects();

        initializeRegularFilters();

        initializeButtons();

        initializeOrders();

        applyRemoteChanges();
    }

    // здесь хранится список всех GroupObjects плюс при необходимости null
    private List<ClientGroupObjectImplementView> groupObjects;
    public List<ClientGroupObjectImplementView> getGroupObjects() {
        return groupObjects;
    }


    private void initializeGroupObjects() throws IOException {

        controllers = new HashMap<ClientGroupObjectImplementView, GroupObjectController>();
        groupObjects = new ArrayList<ClientGroupObjectImplementView>();

        for (ClientGroupObjectImplementView groupObject : formView.groupObjects) {
            groupObjects.add(groupObject);
            GroupObjectController controller = new GroupObjectController(groupObject, formView, this, formLayout);
            controllers.put(groupObject, controller);
        }

        for (ClientPropertyView properties : formView.getProperties()) {
            if (properties.groupObject == null) {
                groupObjects.add(null);
                GroupObjectController controller = new GroupObjectController(null, formView, this, formLayout);
                controllers.put(null, controller);
                break;
            }
        }
    }

    private void initializeRegularFilters() {

        // Проинициализируем регулярные фильтры

        for (final ClientRegularFilterGroupView filterGroup : formView.regularFilters) {

            if (filterGroup.filters.size() == 1) {

                final ClientRegularFilterView singleFilter = filterGroup.filters.get(0);

                final JCheckBox checkBox = new JCheckBox(singleFilter.toString());
                checkBox.addItemListener(new ItemListener() {

                    public void itemStateChanged(ItemEvent ie) {
                        try {
                            if (ie.getStateChange() == ItemEvent.SELECTED)
                                setRegularFilter(filterGroup, singleFilter);
                            else
                                setRegularFilter(filterGroup, null);
                        } catch (IOException e) {
                            throw new RuntimeException("Ошибка при изменении регулярного фильтра", e);
                        }
                    }
                });
                formLayout.add(filterGroup, checkBox);
                formLayout.addBinding(singleFilter.key, "regularFilter" + filterGroup.ID + singleFilter.ID, new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        checkBox.setSelected(!checkBox.isSelected());
                    }
                });

                if(filterGroup.defaultFilter >= 0) {
                    checkBox.setSelected(true);
                    try {
                        setRegularFilter(filterGroup, singleFilter);
                    } catch (IOException e) {
                        throw new RuntimeException("Ошибка при инициализации регулярного фильтра", e);
                    }
                }
            } else {

                final JComboBox comboBox = new JComboBox(
                        BaseUtils.mergeList(Collections.singletonList("(Все)"),filterGroup.filters).toArray());
                comboBox.addItemListener(new ItemListener() {

                    public void itemStateChanged(ItemEvent ie) {
                        try {
                            setRegularFilter(filterGroup,
                                    ie.getItem() instanceof ClientRegularFilterView?(ClientRegularFilterView)ie.getItem():null);
                        } catch (IOException e) {
                            throw new RuntimeException("Ошибка при изменении регулярного фильтра", e);
                        }
                    }
                });
                formLayout.add(filterGroup, comboBox);

                for (final ClientRegularFilterView singleFilter : filterGroup.filters) {
                    formLayout.addBinding(singleFilter.key, "regularFilter" + filterGroup.ID + singleFilter.ID, new AbstractAction() {
                        public void actionPerformed(ActionEvent e) {
                            comboBox.setSelectedItem(singleFilter);
                        }
                    });
                }

                if(filterGroup.defaultFilter >= 0) {
                    ClientRegularFilterView defaultFilter = filterGroup.filters.get(filterGroup.defaultFilter);
                    comboBox.setSelectedItem(defaultFilter);
                    try {
                        setRegularFilter(filterGroup, defaultFilter);
                    } catch (IOException e) {
                        throw new RuntimeException("Ошибка при инициализации регулярного фильтра", e);
                    }
                }
            }

        }
    }

    private void initializeButtons() {

        KeyStroke altP = KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.ALT_DOWN_MASK);
        KeyStroke altX = KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.ALT_DOWN_MASK);
        KeyStroke altDel = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.ALT_DOWN_MASK);
        KeyStroke altR = KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.ALT_DOWN_MASK);
        KeyStroke altEnter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, (isDialogMode() && isReadOnlyMode()) ? 0 : InputEvent.ALT_DOWN_MASK);
        KeyStroke escape = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);

        // Добавляем стандартные кнопки

        if(Main.module.isFull()) {
            AbstractAction printAction = new AbstractAction("Печать (" + SwingUtils.getKeyStrokeCaption(altP) + ")") {

                public void actionPerformed(ActionEvent ae) {
                    print();
                }
            };
            formLayout.addBinding(altP, "altPPressed", printAction);

            JButton buttonPrint = new JButton(printAction);
            buttonPrint.setFocusable(false);

            AbstractAction xlsAction = new AbstractAction("Excel (" + SwingUtils.getKeyStrokeCaption(altX) + ")") {

                public void actionPerformed(ActionEvent ae) {
                    Main.module.runExcel(remoteForm);
                }
            };
            formLayout.addBinding(altX, "altXPressed", xlsAction);

            JButton buttonXls = new JButton(xlsAction);
            buttonXls.setFocusable(false);

            if (!isDialogMode()) {
                formLayout.add(formView.printView, buttonPrint);
                formLayout.add(formView.xlsView, buttonXls);
            }            
        }

        AbstractAction nullAction = new AbstractAction("Сбросить (" + SwingUtils.getKeyStrokeCaption(altDel) + ")") {

            public void actionPerformed(ActionEvent ae) {
                nullPressed();
            }
        };
        JButton buttonNull = new JButton(nullAction);
        buttonNull.setFocusable(false);

        AbstractAction refreshAction = new AbstractAction("Обновить (" + SwingUtils.getKeyStrokeCaption(altR) + ")") {

            public void actionPerformed(ActionEvent ae) {
                refreshData();
            }
        };
        JButton buttonRefresh = new JButton(refreshAction);
        buttonRefresh.setFocusable(false);

        AbstractAction applyAction = new AbstractAction("Применить (" + SwingUtils.getKeyStrokeCaption(altEnter) + ")") {

            public void actionPerformed(ActionEvent ae) {
                applyChanges();
            }
        };
        buttonApply = new JButton(applyAction);
        buttonApply.setFocusable(false);

        AbstractAction cancelAction = new AbstractAction("Отменить (" + SwingUtils.getKeyStrokeCaption(escape) + ")") {

            public void actionPerformed(ActionEvent ae) {
                cancelChanges();
            }
        };
        buttonCancel = new JButton(cancelAction);
        buttonCancel.setFocusable(false);

        AbstractAction okAction = new AbstractAction("OK (" + SwingUtils.getKeyStrokeCaption(altEnter) + ")") {

            public void actionPerformed(ActionEvent ae) {
                okPressed();
            }
        };
        JButton buttonOK = new JButton(okAction);
        buttonOK.setFocusable(false);

        AbstractAction closeAction = new AbstractAction("Закрыть (" + SwingUtils.getKeyStrokeCaption(escape) + ")") {

            public void actionPerformed(ActionEvent ae) {
                closePressed();
            }
        };
        JButton buttonClose = new JButton(closeAction);
        buttonClose.setFocusable(false);

        formLayout.addBinding(altR, "altRPressed", refreshAction);
        formLayout.add(formView.refreshView, buttonRefresh);
        
        if (!isDialogMode()) {

            formLayout.addBinding(altEnter, "enterPressed", applyAction);
            formLayout.add(formView.applyView, buttonApply);

            formLayout.addBinding(escape, "escapePressed", cancelAction);
            formLayout.add(formView.cancelView, buttonCancel);

        } else {

            formLayout.addBinding(altDel, "altDelPressed", nullAction);
            formLayout.add(formView.nullView, buttonNull);

            formLayout.addBinding(altEnter, "enterPressed", okAction);
            formLayout.add(formView.okView, buttonOK);

            formLayout.addBinding(escape, "escapePressed", closeAction);
            formLayout.add(formView.closeView, buttonClose);
        }
    }

    private void initializeOrders() throws IOException {
        // Применяем порядки по умолчанию
        for (Map.Entry<ClientCellView, Boolean> entry : formView.defaultOrders.entrySet()) {
            controllers.get(entry.getKey().getGroupObject()).changeGridOrder(entry.getKey(), Order.ADD);
            if (!entry.getValue()) {
                controllers.get(entry.getKey().getGroupObject()).changeGridOrder(entry.getKey(), Order.DIR);
            }
        }
    }

    private void applyRemoteChanges() throws IOException {
        RemoteChanges remoteChanges = remoteForm.getRemoteChanges();

        for(ClientAction action : remoteChanges.actions)
            action.dispatch(actionDispatcher);

        Log.incrementBytesReceived(remoteChanges.form.length);
        applyFormChanges(new ClientFormChanges(new DataInputStream(new CompressingInputStream(new ByteArrayInputStream(remoteChanges.form))), formView));
        
        clientNavigator.changeCurrentClass(remoteChanges.classID);
    }

    private Color defaultApplyBackground;
    private boolean dataChanged;
    
    private void applyFormChanges(ClientFormChanges formChanges) {

        if(formChanges.dataChanged!=null && buttonApply!=null) {
            if (defaultApplyBackground == null)
                defaultApplyBackground = buttonApply.getBackground();

            dataChanged = formChanges.dataChanged;
            if (dataChanged) {
                buttonApply.setBackground(Color.green);
                buttonApply.setEnabled(true);
                buttonCancel.setEnabled(true);
            } else {
                buttonApply.setBackground(defaultApplyBackground);
                buttonApply.setEnabled(false);
                buttonCancel.setEnabled(false);
            }
        }

        // Сначала меняем виды объектов

        for (ClientPropertyView property : formChanges.panelProperties.keySet()) {
            if (property.shouldBeDrawn(this))
                controllers.get(property.groupObject).addPanelProperty(property);
        }

        for (ClientPropertyView property : formChanges.gridProperties.keySet()) {
            if (property.shouldBeDrawn(this))
                controllers.get(property.groupObject).addGridProperty(property);
        }

        for (ClientPropertyView property : formChanges.dropProperties) {
            controllers.get(property.groupObject).dropProperty(property);
        }


        // Затем подгружаем новые данные

        // Сначала новые объекты

        for (ClientGroupObjectImplementView groupObject : formChanges.gridObjects.keySet()) {
            controllers.get(groupObject).setGridObjects(formChanges.gridObjects.get(groupObject));
        }

        for (ClientGroupObjectImplementView groupObject : formChanges.gridClasses.keySet()) {
            controllers.get(groupObject).setGridClasses(formChanges.gridClasses.get(groupObject));
        }

        for (Map.Entry<ClientGroupObjectImplementView,ClientGroupObjectValue> groupObject : formChanges.objects.entrySet())
            controllers.get(groupObject.getKey()).setCurrentGroupObject(groupObject.getValue(),false);

        for (ClientGroupObjectImplementView groupObject : formChanges.classViews.keySet())
            controllers.get(groupObject).setClassView(formChanges.classViews.get(groupObject));

        for (Map.Entry<ClientGroupObjectImplementView,ClientGroupObjectClass> groupObject : formChanges.classes.entrySet())
            controllers.get(groupObject.getKey()).setCurrentGroupObjectClass(groupObject.getValue());

        // Затем их свойства

        for (ClientPropertyView property : formChanges.panelProperties.keySet()) {
            if (property.shouldBeDrawn(this))
                controllers.get(property.groupObject).setPanelPropertyValue(property, formChanges.panelProperties.get(property));
        }

        for (ClientPropertyView property : formChanges.gridProperties.keySet()) {
            if (property.shouldBeDrawn(this))
                controllers.get(property.groupObject).setGridPropertyValues(property, formChanges.gridProperties.get(property));
        }

        formLayout.getComponent().validate();
        ClientExternalScreen.repaintAll(getID());

        // выдадим сообщение если было от сервера
        if(formChanges.message.length()>0)
            Log.printFailedMessage(formChanges.message);        
    }

    public void changeGroupObject(ClientGroupObjectImplementView groupObject, ClientGroupObjectValue objectValue) throws IOException {

        ClientGroupObjectValue curObjectValue = controllers.get(groupObject).getCurrentObject();

        if (!objectValue.equals(curObjectValue)) {

            // приходится вот так возвращать класс, чтобы не было лишних запросов
            remoteForm.changeGroupObject(groupObject.getID(), Serializer.serializeClientGroupObjectValue(objectValue));
            controllers.get(groupObject).setCurrentGroupObject(objectValue,true);

            applyRemoteChanges();
        }

    }

    public void changeGroupObject(ClientGroupObjectImplementView groupObject, Scroll changeType) throws IOException {

        remoteForm.changeGroupObject(groupObject.getID(), changeType.serialize());

        applyRemoteChanges();
    }

    public void changeProperty(ClientCellView property, Object value, boolean all) throws IOException {

        if (property.getGroupObject() != null) // для глобальных свойств пока не может быть отложенных действий
            SwingUtils.stopSingleAction(property.getGroupObject().getActionID(), true);

        if (property instanceof ClientPropertyView) {

            remoteForm.changePropertyView(property.getID(), BaseUtils.serializeObject(value), all);
            applyRemoteChanges();

        } else {

            if (property instanceof ClientClassCellView) {
                changeClass(((ClientClassCellView)property).object, (ClientConcreteClass)value);
            } else {

                ClientObjectImplementView object = ((ClientObjectCellView)property).object;
                remoteForm.changeObject(object.getID(), value);
                controllers.get(property.getGroupObject()).setCurrentObject(object, value);
                applyRemoteChanges();
            }
        }

    }

    void addObject(ClientObjectImplementView object, ClientConcreteClass cls) throws IOException {
        
        remoteForm.addObject(object.getID(), cls.ID);
        applyRemoteChanges();
    }

    public void changeClass(ClientObjectImplementView object, ClientConcreteClass cls) throws IOException {

        SwingUtils.stopSingleAction(object.groupObject.getActionID(), true);

        remoteForm.changeClass(object.getID(), (cls == null) ? -1 : cls.ID);
        applyRemoteChanges();
    }

    public void changeGridClass(ClientObjectImplementView object, ClientObjectClass cls) throws IOException {

        remoteForm.changeGridClass(object.getID(), cls.ID);
        applyRemoteChanges();
    }

    public void switchClassView(ClientGroupObjectImplementView groupObject) throws IOException {

        SwingUtils.stopSingleAction(groupObject.getActionID(), true);

        remoteForm.switchClassView(groupObject.getID());
        
        applyRemoteChanges();
    }

    public void changeClassView(ClientGroupObjectImplementView groupObject, byte show) throws IOException {

        SwingUtils.stopSingleAction(groupObject.getActionID(), true);

        remoteForm.changeClassView(groupObject.getID(), show);

        applyRemoteChanges();
    }

    public void changeOrder(ClientCellView property, Order modiType) throws IOException {

        if(property instanceof ClientPropertyView)
            remoteForm.changePropertyOrder(property.getID(), modiType.serialize());
        else
            remoteForm.changeObjectOrder(property.getID(), modiType.serialize());

        applyRemoteChanges();
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void changeFind(List<ClientPropertyFilter> conditions) {
    }

    private final Map<ClientGroupObjectImplementView, List<ClientPropertyFilter>> currentFilters = new HashMap<ClientGroupObjectImplementView, List<ClientPropertyFilter>>();
    
    public void changeFilter(ClientGroupObjectImplementView groupObject, List<ClientPropertyFilter> conditions) throws IOException {

        currentFilters.put(groupObject, conditions);

        remoteForm.clearUserFilters();

        for (List<ClientPropertyFilter> listFilter : currentFilters.values())
            for (ClientPropertyFilter filter : listFilter) {
                remoteForm.addFilter(Serializer.serializeClientFilter(filter));
            }

        applyRemoteChanges();
    }

    private void setRegularFilter(ClientRegularFilterGroupView filterGroup, ClientRegularFilterView filter) throws IOException {

        remoteForm.setRegularFilter(filterGroup.ID, (filter == null) ? -1 : filter.ID);

        applyRemoteChanges();
    }

    public void changePageSize(ClientGroupObjectImplementView groupObject, int pageSize) throws IOException {

        remoteForm.changePageSize(groupObject.getID(), pageSize);

//        applyFormChanges();
    }

    void print() {

        try {
            Main.frame.runReport(clientNavigator, remoteForm);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при печати формы", e);
        }
    }

    void refreshData() {

        try {

            remoteForm.refreshData();

            applyRemoteChanges();

        } catch (IOException e) {
            throw new RuntimeException("Ошибка при обновлении формы", e);
        }
    }

    void applyChanges() {

        try {

            if (dataChanged) {

                String okMessage = "";
                for (ClientGroupObjectImplementView groupObject : formView.groupObjects) {
                    okMessage += controllers.get(groupObject).getSaveMessage();
                }

                if (!okMessage.isEmpty()) {
                    if (!(SwingUtils.showConfirmDialog(getComponent(), okMessage, null, JOptionPane.QUESTION_MESSAGE, SwingUtils.YES_BUTTON) == JOptionPane.YES_OPTION)) {
                        return;
                    }
                }

                if(remoteForm.hasClientApply()) {
                    ClientApply clientApply = remoteForm.getClientApply();
                    if(clientApply instanceof CheckFailed) // чтобы не делать лишний RMI вызов
                        Log.printFailedMessage(((CheckFailed)clientApply).message);
                    else {
                        remoteForm.applyClientChanges(((ClientAction)clientApply).dispatch(actionDispatcher));

                        applyRemoteChanges();
                    }
                } else {
                    remoteForm.applyChanges();

                    applyRemoteChanges();
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Ошибка при применении изменений", e);
        }
    }

    boolean cancelChanges() {

        try {

            if (dataChanged) {

                if (SwingUtils.showConfirmDialog(getComponent(), "Вы действительно хотите отменить сделанные изменения ?", null, JOptionPane.WARNING_MESSAGE, SwingUtils.NO_BUTTON) == JOptionPane.YES_OPTION) {
                    remoteForm.cancelChanges();

                    applyRemoteChanges();
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Ошибка при отмене изменений", e);
        }

        return true;
    }

    public void okPressed() {
        applyChanges();
    }

    boolean closePressed() {
        return cancelChanges();
    }

    boolean nullPressed() {
        return true;
    }

    public void dropLayoutCaches() {
        formLayout.dropCaches();
    }
}