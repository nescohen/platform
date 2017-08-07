package lsfusion.client.form.tree;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.sun.java.swing.plaf.windows.WindowsTreeUI;
import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.client.ClientResourceBundle;
import lsfusion.client.SwingUtils;
import lsfusion.client.form.*;
import lsfusion.client.form.cell.CellTableInterface;
import lsfusion.client.form.cell.ClientAbstractCellEditor;
import lsfusion.client.form.cell.ClientAbstractCellRenderer;
import lsfusion.client.form.dispatch.EditPropertyDispatcher;
import lsfusion.client.form.grid.GridPropertyTable;
import lsfusion.client.form.sort.MultiLineHeaderRenderer;
import lsfusion.client.form.sort.TableSortableHeaderManager;
import lsfusion.client.logics.ClientGroupObject;
import lsfusion.client.logics.ClientGroupObjectValue;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.client.logics.ClientTreeGroup;
import lsfusion.client.logics.classes.ClientStringClass;
import lsfusion.client.logics.classes.ClientType;
import lsfusion.interop.Order;
import lsfusion.interop.form.ColorPreferences;
import org.jdesktop.swingx.JXTableHeader;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.table.TableColumnExt;
import org.jdesktop.swingx.treetable.TreeTableNode;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.text.JTextComponent;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.util.*;
import java.util.List;

import static java.lang.Math.max;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static lsfusion.client.form.ClientFormController.PasteData;
import static lsfusion.client.form.EditBindingMap.getPropertyEditActionSID;
import static lsfusion.client.form.EditBindingMap.isEditableAwareEditEvent;
import static lsfusion.client.form.grid.GridTable.DEFAULT_HEADER_HEIGHT;
import static lsfusion.client.form.grid.GridTable.DEFAULT_PREFERRED_SIZE;

public class TreeGroupTable extends ClientFormTreeTable implements CellTableInterface, EditPropertyHandler {
    private final EditPropertyDispatcher editDispatcher;

    private final EditBindingMap editBindingMap = new EditBindingMap();

    private final CellTableContextMenuHandler contextMenuHandler = new CellTableContextMenuHandler(this);

    private final int HIERARCHICAL_COLUMN_MIN_WIDTH = 50;
    private final int HIERARCHICAL_COLUMN_MAX_WIDTH = 100000;

    protected EventObject editEvent;
    protected int editRow;
    protected int editCol;
    protected ClientType currentEditType;
    protected Object currentEditValue;
    protected boolean editPerformed;
    protected boolean commitingValue;

    private final TreeGroupNode rootNode;
    public final ClientFormController form;
    private final ClientTreeGroup treeGroup;

    public GroupTreeTableModel model;

    private boolean synchronize = false;

    private Action moveToNextCellAction = null;

    private ClientGroupObjectValue currentPath;
    public TreePath currentTreePath;
    private Set<TreePath> expandedPathes;

    boolean plainTreeMode = false;
    boolean manualExpand;

    private TableSortableHeaderManager<ClientPropertyDraw> sortableHeaderManager;

    private WeakReference<TableCellRenderer> defaultHeaderRendererRef;
    private TableCellRenderer wrapperHeaderRenderer;

    public TreeGroupTable(ClientFormController iform, ClientTreeGroup itreeGroup) {
        form = iform;
        treeGroup = itreeGroup;
        plainTreeMode = itreeGroup.plainTreeMode;

        editDispatcher = new EditPropertyDispatcher(this, form.getDispatcherListener());

        contextMenuHandler.install();
        setAutoCreateColumnsFromModel(false);

        setTreeTableModel(model = new GroupTreeTableModel(form, plainTreeMode));
        
        addColumn(createColumn(0)); // одна колонка для дерева. создаём вручную, чтобы подставить renderer
        
        setupHierarhicalColumn();

        rootNode = model.getRoot();
        
        if (treeGroup.design.font != null) {
            setFont(treeGroup.design.getFont(this));
        }

        sortableHeaderManager = new TableSortableHeaderManager<ClientPropertyDraw>(this, true) {
            protected void orderChanged(final ClientPropertyDraw columnKey, final Order modiType) {
                RmiQueue.runAction(new Runnable() {
                    @Override
                    public void run() {
                        TreeGroupTable.this.orderChanged(columnKey, modiType);
                    }
                });
            }

            @Override
            protected void ordersCleared(final ClientGroupObject groupObject) {
                RmiQueue.runAction(new Runnable() {
                    @Override
                    public void run() {
                        TreeGroupTable.this.ordersCleared(groupObject);
                    }
                });
            }

            @Override
            protected ClientPropertyDraw getColumnKey(int column) {
                return model.getColumnProperty(column);
            }

            @Override
            protected ClientPropertyDraw getColumnProperty(int column) {
                return model.getColumnProperty(column);
            }
        };

        tableHeader.addMouseListener(sortableHeaderManager);

        if (plainTreeMode) {
            setTableHeader(null);
            setShowGrid(false, false);
        } else {
            //подсветка ячеек дерева
            ColorPreferences colorPreferences = getForm().getColorPreferences();
            setHighlighters(
                    new ColorHighlighter(
                            new HighlightPredicate.AndHighlightPredicate(
                                    HighlightPredicate.HAS_FOCUS,
                                    new HighlightPredicate.ColumnHighlightPredicate(0)
                            ), colorPreferences.getFocusedCellBackground(), Color.BLACK, colorPreferences.getFocusedCellBackground(), Color.BLACK
                    ),
                    new ColorHighlighter(
                            new HighlightPredicate.AndHighlightPredicate(
                                    new HighlightPredicate.NotHighlightPredicate(
                                            HighlightPredicate.HAS_FOCUS
                                    ),
                                    new HighlightPredicate.ColumnHighlightPredicate(0)
                            ), Color.WHITE, Color.BLACK, colorPreferences.getSelectedRowBackground(), Color.BLACK
                    )
            );

            setDefaultRenderer(Object.class, new ClientAbstractCellRenderer());
            setDefaultEditor(Object.class, new ClientAbstractCellEditor(this));
        }

        addTreeWillExpandListener(new TreeWillExpandListener() {
            public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
                final TreeGroupNode node = (TreeGroupNode) event.getPath().getLastPathComponent();
                if (!synchronize && !manualExpand) {
                    if (node.group != null) {
                        RmiQueue.runAction(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    form.expandGroupObject(node.group, node.key);
                                } catch (IOException e) {
                                    throw new RuntimeException(ClientResourceBundle.getString("form.tree.error.opening.treenode"), e);
                                }
                            }
                        });
                    }
                }
                if (node.hasOnlyExpandningNodeAsChild()) {
                    throw new ExpandVetoException(event);
                }
            }

            public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
                TreeGroupNode node = (TreeGroupNode) event.getPath().getLastPathComponent();
                if (!synchronize && !manualExpand) {
                    if (node.group != null) {
                        try {
                            form.collapseGroupObject(node.group, node.key);
                        } catch (IOException e) {
                            throw new RuntimeException(ClientResourceBundle.getString("form.tree.error.opening.treenode"), e);
                        }
                    }
                }
            }
        });

        getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (synchronize) {
                    return;
                }

                final TreePath path = getPathForRow(getSelectedRow());
                if (path != null) {
                    TreeGroupNode node = (TreeGroupNode) path.getLastPathComponent();
                    if (node.group != null && currentPath != node.key) {
                        currentPath = node.key;
                        currentTreePath = path;

                        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(new ChangeObjectEvent(node.group, node.key));
                    }
                }
            }
        });

        addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (!isEditing()) {
                    moveToFocusableCellIfNeeded();
                }
            }
        });

        addKeyListener(new TreeGroupQuickSearchHandler(this));

        if (form.isDialog()) {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() > 1) {
                        RmiQueue.runAction(new Runnable() {
                            @Override
                            public void run() {
                                form.okPressed();
                            }
                        });
                    }
                }
            });
        }
        
        if (treeGroup.expandOnClick) {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2 && !editPerformed) {
                        final TreePath path = getPathForRow(rowAtPoint(e.getPoint()));
                        if (path != null && !((TreeGroupTreeUI) getHierarhicalColumnRenderer().getUI()).isLocationInExpandControl(path, e.getX(), e.getY())) {
                            final TreeGroupNode node = (TreeGroupNode) path.getLastPathComponent();

                            if (node.isExpandable() && node.group != null) {
                                RmiQueue.runAction(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            if (!isExpanded(path)) {
                                                form.expandGroupObject(node.group, node.key);
                                            } else {
                                                form.collapseGroupObject(node.group, node.key);
                                            }
                                        } catch (IOException ex) {
                                            throw new RuntimeException(ClientResourceBundle.getString("form.tree.error.opening.treenode"), ex);
                                        }
                                    }
                                });
                            }
                        }
                    }
                }
            });
        }

        getHierarhicalColumnRenderer().setUI(new TreeGroupTreeUI());

        initializeActionMap();
        currentTreePath = new TreePath(rootNode);
    }

    private void orderChanged(ClientPropertyDraw columnKey, Order modiType) {
        try {
            form.changePropertyOrder(columnKey, modiType, ClientGroupObjectValue.EMPTY);
            tableHeader.resizeAndRepaint();
        } catch (IOException e) {
            throw new RuntimeException(ClientResourceBundle.getString("errors.error.changing.sorting"), e);
        }

        tableHeader.resizeAndRepaint();
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return gridPropertyTable.getScrollableTracksViewportWidth();
    }

    @Override
    public void doLayout() {
        gridPropertyTable.doLayout();
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return treeGroup.autoSize ? getPreferredSize() : DEFAULT_PREFERRED_SIZE;
    }

    private void ordersCleared(ClientGroupObject groupObject) {
        try {
            form.clearPropertyOrders(groupObject);
            tableHeader.resizeAndRepaint();
        } catch (IOException e) {
            throw new RuntimeException(ClientResourceBundle.getString("errors.error.changing.sorting"), e);
        }

        tableHeader.resizeAndRepaint();
    }

    private void initializeActionMap() {
        final Action nextColumnAction = new GoToNextCellAction(0, 1);
        final Action prevColumnAction = new GoToNextCellAction(0, -1);
        final Action nextRowAction = new GoToNextCellAction(1, 0);
        final Action prevRowAction = new GoToNextCellAction(-1, 0);

        final Action firstColumnAction = new GoToNextCellAction(0, -1, true);
        final Action lastColumnAction = new GoToNextCellAction(0, 1, true);

        final Action firstRowAction = new GoToNextCellAction(-1, 0, true);
        final Action lastRowAction = new GoToNextCellAction(1, 0, true);

        ActionMap actionMap = getActionMap();

        // вверх/вниз
        actionMap.put("selectNextRow", nextRowAction);
        actionMap.put("selectPreviousRow", prevRowAction);

        // вперёд/назад
        actionMap.put("selectNextColumn", new ExpandAction(true, nextColumnAction));
        actionMap.put("selectPreviousColumn", new ExpandAction(false, prevColumnAction));
        actionMap.put("selectNextColumnCell", nextColumnAction);
        actionMap.put("selectPreviousColumnCell", prevColumnAction);

        // в начало/конец ряда
        actionMap.put("selectFirstColumn", firstColumnAction);
        actionMap.put("selectLastColumn", lastColumnAction);

        // в начало/конец колонки
        actionMap.put("selectFirstRow", firstRowAction);
        actionMap.put("selectLastRow", lastRowAction);

        this.moveToNextCellAction = nextColumnAction;
    }

    private void moveToFocusableCellIfNeeded() {
        int row = getSelectionModel().getLeadSelectionIndex();
        int col = getColumnModel().getSelectionModel().getLeadSelectionIndex();
        if (!isCellFocusable(row, col)) {
            moveToNextCellAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ""));
        }
    }

    public void updateKeys(ClientGroupObject group, List<ClientGroupObjectValue> keys, List<ClientGroupObjectValue> parents, Map<ClientGroupObjectValue, Boolean> expandables) {
        model.updateKeys(group, keys, parents, expandables);
    }

    public void updateReadOnlyValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> readOnlyValues) {
        model.updateReadOnlyValues(property, readOnlyValues);
    }

    public void updateCellBackgroundValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> cellBackgroundValues) {
        model.updateCellBackgroundValues(property, cellBackgroundValues);
    }

    public void updateCellForegroundValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> cellForegroundValues) {
        model.updateCellForegroundValues(property, cellForegroundValues);
    }

    public void updateRowBackgroundValues(Map<ClientGroupObjectValue, Object> rowBackground) {
        model.updateRowBackgroundValues(rowBackground);
    }

    public void updateRowForegroundValues(Map<ClientGroupObjectValue, Object> rowForeground) {
        model.updateRowForegroundValues(rowForeground);
    }

    public void expandNew(ClientGroupObject group) {
        for (TreeGroupNode node : model.getGroupNodes(group)) {
            boolean inExpanded = false;
            for (TreePath expandedPath : expandedPathes)
                if (Arrays.asList(expandedPath.getPath()).contains(node))
                    inExpanded = true;
            if (!inExpanded && (node.getChildCount() > 1 || (node.getChildCount() == 1 && node.getChildAt(0) instanceof TreeGroupNode))) {
                TreePath path = new TreePath(getTreePath(node, new ArrayList<TreeTableNode>()).toArray());
                expandPathManually(path);
            }
        }
    }
    
    private List<TreeTableNode> getTreePath(TreeTableNode node, List<TreeTableNode> path) {
        path.add(0, node);
        if (node.getParent() != null) {
            return getTreePath(node.getParent(), path);
        } else {
            return path;
        }
    }
    
    public void expandPathManually(TreePath path) {
        manualExpand = true;
        expandPath(path);
        manualExpand = false;
    }

    public void updateDrawPropertyValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> ivalues, boolean update) {
        model.updateDrawPropertyValues(property, ivalues, update);
    }

    private Map<ClientPropertyDraw, TableColumn> columnsMap = MapFact.mAddRemoveMap();

    public boolean addDrawProperty(ClientGroupObject group, ClientPropertyDraw property) {
        int ind = model.addDrawProperty(form, group, property);
        if (ind > -1 && !plainTreeMode) {
            createPropertyColumn(property, ind);
        }
        return ind != -1;
    }

    public void removeProperty(ClientGroupObject group, ClientPropertyDraw property) {
        int ind = model.removeProperty(group, property);
        if (ind > 0 && !plainTreeMode) {
            removeColumn(getColumn(ind));
            //нужно поменять реальный индекс всех колонок после данной
            for (int i = ind; i < getColumnCount(); ++i) {
                getColumn(i).setModelIndex(i);
            }

            columnsMap.remove(property);
        }
        
        int rowHeight = 0;
        for (ClientPropertyDraw columnProperty : model.columnProperties) {
            rowHeight = max(rowHeight, columnProperty.getPreferredValueHeight(this));
        }
        if (rowHeight != getRowHeight() && rowHeight > 0) {
            setRowHeight(rowHeight);
        }
    }

    private int hierarchicalPreferredWidth;
    private TableColumnExt hierarchicalColumn;
    private void setupHierarhicalColumn() {
        TableColumnExt tableColumn = getColumnExt(0);

        int pref = treeGroup.calculatePreferredSize();
        hierarchicalColumn = tableColumn;
        hierarchicalPreferredWidth = pref;
//        setColumnSizes(tableColumn, pref, pref, pref);

        getColumnModel().getSelectionModel().setSelectionInterval(0, 0);
    }

    private TableColumnExt createColumn(int pos) {
        TableColumnExt tableColumn = new TableColumnExt(pos) {
            @Override
            public TableCellRenderer getHeaderRenderer() {
                TableCellRenderer defaultHeaderRenderer = tableHeader.getDefaultRenderer();
                if (defaultHeaderRendererRef == null || defaultHeaderRendererRef.get() != defaultHeaderRenderer) {
                    defaultHeaderRendererRef = new WeakReference<>(defaultHeaderRenderer);
                    wrapperHeaderRenderer = new MultiLineHeaderRenderer(tableHeader.getDefaultRenderer(), sortableHeaderManager) {
                        @Override
                        public Component getTableCellRendererComponent(JTable itable, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                            Component comp = super.getTableCellRendererComponent(itable, value, isSelected, hasFocus, row, column);
                            if (column > 0) {
                                model.getColumnProperty(column).design.designHeader(comp);
                            }
                            return comp;
                        }
                    };
                }
                return wrapperHeaderRenderer;
            }
        };
        getColumnFactory().configureTableColumn(getModel(), tableColumn);
        return tableColumn;
    } 

    private void createPropertyColumn(ClientPropertyDraw property, int pos) {
        TableColumnExt tableColumn = createColumn(pos);
        int rowHeight = getRowHeight();
        int currentSelectedColumn = getColumnModel().getSelectionModel().getLeadSelectionIndex();

        int min = property.getMinimumValueWidth(this);
        int max = property.getMaximumValueWidth(this);
        int pref = property.getPreferredValueWidth(this);

        setColumnSizes(tableColumn, min, max, pref);

        rowHeight = max(rowHeight, property.getPreferredValueHeight(this));

        addColumn(tableColumn);
        moveColumn(getColumnCount() - 1, pos);
        //нужно поменять реальный индекс всех колонок после данной
        for (int i = pos + 1; i < getColumnCount(); ++i) {
            getColumn(i).setModelIndex(i);
        }

        // moveColumn норовит выделить вновь добавленную колонку (при инициализации происходит скроллирование вправо). возвращаем выделение обратно
        if (currentSelectedColumn != -1) {
            getColumnModel().getSelectionModel().setLeadSelectionIndex(pos <= currentSelectedColumn ? currentSelectedColumn + 1 : currentSelectedColumn);
        }

        tableColumn.setToolTipText(property.getTooltipText(model.getColumnName(pos)));

        columnsMap.put(property, tableColumn);

        if (getRowHeight() != rowHeight && rowHeight > 0) {
            setRowHeight(rowHeight);
        }
    }

    private void setColumnSizes(TableColumnExt tableColumn, int min, int max, int pref) {
        // делаем так потому, что новые значения размеров
        // корректируется в зависимости от старых
        // и мы можем увидеть не то, что хотели
        if (min < tableColumn.getMinWidth()) {
            tableColumn.setMinWidth(min);
            tableColumn.setMaxWidth(max);
        } else {
            tableColumn.setMaxWidth(max);
            tableColumn.setMinWidth(min);
        }
        tableColumn.setPreferredWidth(getAutoResizeMode() == JTable.AUTO_RESIZE_OFF ? min : pref);
    }

    public void setCurrentPath(final ClientGroupObjectValue objects) {
        enumerateNodesDepthFirst(new NodeProccessor() {
            @Override
            public void processPath(TreePath nodePath) {
                Object node = nodePath.getLastPathComponent();
                if (node instanceof TreeGroupNode) {
                    TreeGroupNode groupNode = (TreeGroupNode) node;
                    if (groupNode.key.equals(objects)) {
                        currentPath = objects;
                        currentTreePath = nodePath;
                    }
                }
            }
        });
    }

    public void setSelectionPath(TreePath treePath) {
        getSelectionModel().setSelectionInterval(0, getRowForPath(treePath));
    }

    public void saveVisualState() {
        Enumeration paths = getExpandedDescendants(new TreePath(rootNode));
        expandedPathes = paths == null
                         ? new HashSet<TreePath>()
                         : new HashSet<TreePath>(Collections.list(paths));
    }

    public void restoreVisualState() {
        synchronize = true;
        enumerateNodesDepthFirst(new NodeProccessor() {
            @Override
            public void processPath(TreePath nodePath) {
                if (expandedPathes.contains(nodePath)) {
                    expandPath(nodePath);
                }
                model.firePathChanged(nodePath);
            }
        });

        for (ClientGroupObject group : treeGroup.groups)
            expandNew(group);

        moveToFocusableCellIfNeeded();

        setSelectionPath(currentTreePath);

        synchronize = false;
    }

    private boolean isCellFocusable(int row, int col) {
        TreePath path = getPathForRow(row);
        return path != null && model.isCellFocusable(path.getLastPathComponent(), col);
    }

    @Override
    public void changeSelection(int row, int col, boolean toggle, boolean extend) {
        if (toggle) {
            //чтобы нельзя было просто убрать выделение
            return;
        }

        if (isCellFocusable(row, col)) {
            super.changeSelection(row, col, toggle, extend);
        }
    }

    public boolean isPressed(int row, int column) {
        return false;
    }

    @Override
    public ClientPropertyDraw getProperty(int row, int column) {
        TreePath pathForRow = getPathForRow(row);
        if (pathForRow != null) {
            return model.getProperty(pathForRow.getLastPathComponent(), column);
        }
        return null;
    }

    public ClientGroupObjectValue getColumnKey(int row, int col) {
        TreePath pathForRow = getPathForRow(row);
        if (pathForRow != null) {
            Object node = pathForRow.getLastPathComponent();
            if (node instanceof TreeGroupNode)
                return ((TreeGroupNode) node).key;
        }
        return ClientGroupObjectValue.EMPTY;
    }

    @Override
    public boolean richTextSelected() {
        ClientPropertyDraw property = getCurrentProperty();
        return property != null && property.baseType instanceof ClientStringClass && ((ClientStringClass) property.baseType).rich;
    }

    public void pasteTable(List<List<String>> table) {
        //пока вставляем только одно значение

        int row = getSelectionModel().getLeadSelectionIndex();
        int column = getColumnModel().getSelectionModel().getLeadSelectionIndex();

        if (!isHierarchical(column) && !table.isEmpty() && !table.get(0).isEmpty()) {

            final ClientPropertyDraw property = getProperty(row, column);
            if (property != null) {
                try {
                    String value = table.get(0).get(0);
                    Object newValue = value == null ? null : property.parseChangeValueOrNull(value);
                    if (property.canUsePasteValueForRendering()) {
                        model.setValueAt(newValue, row, column);
                    }

                    form.pasteMulticellValue(
                            singletonMap(property, new PasteData(newValue, singletonList(currentPath), singletonList(getValueAt(row, column))))
                    );
                } catch (IOException e) {
                    throw Throwables.propagate(e);
                }
            }
        }
    }

    @Override
    public boolean isSelected(int row, int coulumn) {
        return false;
    }

    @Override
    public Color getBackgroundColor(int row, int column) {
        if (row < 0 || row > getRowCount()) {
            return null;
        }

        TreePath pathForRow = getPathForRow(row);
        if (pathForRow == null) {
            return null;
        }
        return model.getBackgroundColor(pathForRow.getLastPathComponent(), column);
    }

    @Override
    public Color getForegroundColor(int row, int column) {
        if (row < 0 || row > getRowCount()) {
            return null;
        }

        TreePath pathForRow = getPathForRow(row);
        if (pathForRow == null) {
            return null;
        }
        return model.getForegroundColor(pathForRow.getLastPathComponent(), column);
    }

    @Override
    public ClientFormController getForm() {
        return form;
    }

    public ClientPropertyDraw getCurrentProperty() {
        int column = getSelectedColumn();
        int row = getSelectedRow();

        ClientPropertyDraw selectedProperty = null;

        if (column == 0) {
            ++column;
        }

        if (column >= 0 && column < getColumnCount() && row >= 0 && row <= getRowCount()) {
            selectedProperty = getProperty(row, column);
        }

        return selectedProperty != null
               ? selectedProperty
               : model.getColumnCount() > 1
                 ? model.getColumnProperty(1)
                 : null;
    }

    public Object getSelectedValue(ClientPropertyDraw property) {
        int row = getSelectedRow();

        if (row < 0 || row > getRowCount()) {
            return null;
        }

        TreePath pathForRow = getPathForRow(row);
        if (pathForRow == null) {
            return null;
        }
        return model.getPropertyValue(pathForRow.getLastPathComponent(), property);
    }

    @Override
    public ClientType getCurrentEditType() {
        return currentEditType;
    }

    @Override
    public Object getCurrentEditValue() {
        return currentEditValue;
    }

    public boolean editCellAt(int row, int column, EventObject e){
        if (!form.commitCurrentEditing()) {
            return false;
        }

        if (e instanceof MouseEvent) {
            // чтобы не срабатывало редактирование при изменении ряда,
            // потому что всё равно будет апдейт
            int selRow = getSelectedRow();
            if (selRow == -1 || selRow != row) {
                return false;
            }
        }

        if (row < 0 || row >= getRowCount() || column < 0 || column >= getColumnCount()) {
            return false;
        }

        ClientPropertyDraw property = getProperty(row, column);
        if (property == null) {
            return false;
        }

        ClientGroupObjectValue columnKey = getColumnKey(row, column);
        if (columnKey == null) {
            return false;
        }

        String actionSID = getPropertyEditActionSID(e, property, editBindingMap);
        if (actionSID == null) {
            return false;
        }

        if (isEditableAwareEditEvent(actionSID) && !isCellEditable(row, column)) {
            return false;
        }

        if (isHierarchical(column)) {
            return false;
        }

        editRow = row;
        editCol = column;
        editEvent = e;
        commitingValue = false;

        //здесь немного запутанная схема...
        //executePropertyEditAction возвращает true, если редактирование произошло на сервере, необязательно с вводом значения...
        //но из этого editCellAt мы должны вернуть true, только если началось редактирование значения
        editPerformed = editDispatcher.executePropertyEditAction(property, columnKey, actionSID, getValueAt(row, column), editEvent);
        return editorComp != null;
    }

    public boolean requestValue(ClientType valueType, Object oldValue) {
        //пока чтение значения можно вызывать только один раз в одном изменении...
        //если получится безусловно задержать фокус, то это ограничение можно будет убрать
        Preconditions.checkState(!commitingValue, "You can request value only once per edit action.");

        currentEditType = valueType;
        currentEditValue = oldValue;

        if (!super.editCellAt(editRow, editCol, editEvent)) {
            return false;
        }

        if (editEvent instanceof KeyEvent) {
            prepareTextEditor();
        }

        editorComp.requestFocusInWindow();

        form.setCurrentEditingTable(this);

        return true;
    }

    void prepareTextEditor() {
        if (editorComp instanceof JTextComponent) {
            JTextComponent textEditor = (JTextComponent) editorComp;
            textEditor.selectAll();
            if (getProperty(editRow, editCol).clearText) {
                textEditor.setText("");
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public Component prepareEditor(TableCellEditor editor, int row, int column) {
        Component component = super.prepareEditor(editor, row, column);
        if (component instanceof JComponent) {
            JComponent jComponent = (JComponent)component;
            // у нас есть возможность редактировать нефокусную таблицу, и тогда после редактирования фокус теряется,
            // поэтому даём возможность FocusManager'у самому поставить фокус
            if (!isFocusable() && jComponent.getNextFocusableComponent() == this) {
                jComponent.setNextFocusableComponent(null);
                return component;
            }
        }
        return component;
    }

    @Override
    public void editingStopped(ChangeEvent e) {
        TableCellEditor editor = getCellEditor();
        if (editor != null) {
            Object value = editor.getCellEditorValue();
            internalRemoveEditor();
            commitValue(value);
        }
    }

    private void commitValue(Object value) {
        commitingValue = true;
        editDispatcher.commitValue(value);
        form.clearCurrentEditingTable(this);
    }

    @Override
    public void editingCanceled(ChangeEvent e) {
        internalRemoveEditor();
        editDispatcher.cancelEdit();
        form.clearCurrentEditingTable(this);
    }

    public void updateEditValue(Object value) {
        setValueAt(value, editRow, editCol);
    }

    private void internalRemoveEditor() {
        super.removeEditor();
    }

    @Override
    public void removeEditor() {
        // removeEditor иногда вызывается напрямую, поэтому вызываем cancelCellEditing сами
        TableCellEditor cellEditor = getCellEditor();
        if (cellEditor != null) {
            cellEditor.cancelCellEditing();
        }
    }

    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
        editPerformed = false;
        boolean consumed = super.processKeyBinding(ks, e, condition, pressed);
        return consumed || editPerformed;
    }

    public void changeOrder(ClientPropertyDraw property, Order modiType) throws IOException {
        int propertyIndex = model.getPropertyColumnIndex(property);
        if (propertyIndex > 0) {
            sortableHeaderManager.changeOrder(property, modiType);
        } else {
            //меняем напрямую для верхних groupObjects
            form.changePropertyOrder(property, modiType, ClientGroupObjectValue.EMPTY);
        }
    }

    public void clearOrders(ClientGroupObject groupObject) throws IOException {
            sortableHeaderManager.clearOrders(groupObject);
    }

    @Override
    protected JTableHeader createDefaultTableHeader() {
        JXTableHeader jxTableHeader = new JXTableHeader(columnModel) {
            @Override
            public Dimension getPreferredSize() {
                Dimension pref = super.getPreferredSize();
                return new Dimension(pref.width, DEFAULT_HEADER_HEIGHT);
            }
        };
        jxTableHeader.setReorderingAllowed(false);
        return jxTableHeader;
    }

    public void updateTable() {
        gridPropertyTable.updateLayoutWidth();
    }

    private class ChangeObjectEvent extends AWTEvent implements ActiveEvent {
        public static final int CHANGE_OBJECT_EVENT = AWTEvent.RESERVED_ID_MAX + 5555;
        private final ClientGroupObject group;
        private final ClientGroupObjectValue key;

        public ChangeObjectEvent(ClientGroupObject group, ClientGroupObjectValue key) {
            super(TreeGroupTable.this, CHANGE_OBJECT_EVENT);
            this.group = group;
            this.key = key;
        }

        @Override
        public void dispatch() {
            if (key == currentPath) {
                try {
                    form.changeGroupObject(group, key);
                } catch (IOException ioe) {
                    throw new RuntimeException(ClientResourceBundle.getString("form.tree.error.selecting.node"), ioe);
                }
            }
        }
    }

    public ClientGroupObjectValue getCurrentPath() {
        return currentPath;
    }

    private class GoToNextCellAction extends AbstractAction {
        private final int dc;
        private final int dr;
        private final boolean boundary;

        public GoToNextCellAction(int dr, int dc) {
            this(dr, dc, false);
        }

        public GoToNextCellAction(int dr, int dc, boolean boundary) {
            this.dr = dr;
            this.dc = dc;
            this.boundary = boundary;
        }

        public void actionPerformed(ActionEvent e) {
            if (!form.commitCurrentEditing()) {
                return;
            }
            if (getRowCount() <= 0 || getColumnCount() <= 0) {
                return;
            }

            int maxR = getRowCount() - 1;
            int maxC = getColumnCount() - 1;

            int row = getSelectionModel().getLeadSelectionIndex();
            int column = getColumnModel().getSelectionModel().getLeadSelectionIndex();

            boolean checkFirst = row == -1 || column == -1;
            row = row == -1 ? 0 : row;
            column = column == -1 ? 0 : column;

            if (boundary) {
                int startRow = dr != 0
                               ? (dr > 0 ? 0 : maxR)
                               : row;

                int startColumn = dc != 0
                                  ? (dc > 0 ? 0 : maxC)
                                  : column;

                int endRow = dr != 0
                        ? (dr > 0 ? maxR : 0)
                        : startRow;
                int endColumn = dc != 0
                        ? (dc > 0 ? maxC : 0)
                        : startColumn;

                int rc[] = moveBoundary(startRow, endRow, startColumn, endColumn);
                row = rc[0];
                column = rc[1];
            } else {
                if (!(checkFirst && isCellFocusable(row, column))) {
                    do {
                        int next[] = moveNext(row, column, maxR, maxC);
                        row = next[0];
                        column = next[1];
                    } while (row != -1 && !isCellFocusable(row, column));
                }
            }

            if (row >= 0 && column >= 0) {
                changeSelection(row, column, false, false);
            }
        }

        private int[] moveNext(int row, int column, int maxR, int maxC) {
            if (dc != 0) {
                column += dc;
                if (column > maxC) {
                    ++row;
                    if (row > maxR) {
                        // в крайнем положении - больше некуда двигаться
                        row = -1;
                    } else {
                        column = 0;
                    }
                } else if (column < 0) {
                    --row;
                    if (row < 0) {
                        // в крайнем положении - больше некуда двигаться
                        row = -1;
                    } else {
                        column = maxC;
                    }
                }
            } else {
                row += dr;
                if (row > maxR || row < 0) {
                    row = -1;
                }
            }
            return new int[]{row, column};
        }

        private int[] moveBoundary(int startRow, int endRow, int startColumn, int endColumn) {
            for (int r = endRow, c = endColumn; r != startRow || c != startColumn; r -= dr, c -= dc) {
                if (isCellFocusable(r, c)) {
                    return new int[] {r, c};
                }
            }
            if (isCellFocusable(startRow, startColumn)) {
                return new int[] {startRow, startColumn};
            }
            return new int[] {-1, -1};
        }
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        java.awt.Point p = e.getPoint();
        int rowIndex = rowAtPoint(p);
        int colIndex = columnAtPoint(p);

        if (rowIndex != -1 && colIndex != -1) {
            ClientPropertyDraw cellProperty = getProperty(rowIndex, colIndex);
            if (cellProperty!=null && !cellProperty.echoSymbols) {
                Object value = getValueAt(rowIndex, colIndex);
                if (value != null) {
                    if (value instanceof Double) {
                        value = (double) Math.round(((Double) value) * 1000) / 1000;
                    }

                    String formattedValue;
                    try {
                        formattedValue = cellProperty.baseType.formatString(value);
                    } catch (ParseException e1) {
                        formattedValue = String.valueOf(value);
                    }

                    if (!BaseUtils.isRedundantString(formattedValue)) {
                        return SwingUtils.toMultilineHtml(formattedValue, createToolTip().getFont());
                    }
                }
            }
        }
        return null;
    }
    
    class TreeGroupTreeUI extends WindowsTreeUI {
        @Override
        protected boolean isLocationInExpandControl(TreePath path, int mouseX, int mouseY) {
            return super.isLocationInExpandControl(path, mouseX, mouseY);
        }
    }

    private Integer hierarchicalUserWidth = null;
    private final Map<ClientPropertyDraw, Integer> userWidths = MapFact.mAddRemoveMap();
    private final GridPropertyTable gridPropertyTable = new GridPropertyTable() {
        public void setUserWidth(ClientPropertyDraw property, Integer value) {
            userWidths.put(property, value);
        }

        public Integer getUserWidth(ClientPropertyDraw property) {
            return userWidths.get(property);
        }

        @Override
        public ClientPropertyDraw getColumnPropertyDraw(int i) {
            assert i < getColumnsCount() - 1; // уже вычли фиксированную колонку
            return model.columnProperties.get(i);
        }

        @Override
        public TableColumn getColumnDraw(int i) {
            if(i == 0)
                return hierarchicalColumn;
            return columnsMap.get(getColumnPropertyDraw(i-1));
        }

        @Override
        public JTable getTable() {
            return TreeGroupTable.this;
        }

        public int getColumnsCount() {
            return 1 + model.properties.size();
        }

        @Override
        protected boolean isColumnFlex(int i) {
            if(i == 0)
                return true;
            return super.isColumnFlex(i - 1);
        }

        @Override
        protected void setUserWidth(int i, int width) {
            if(i==0) {
                hierarchicalUserWidth = width;
                return;
            }
            super.setUserWidth(i - 1, width);
        }

        @Override
        protected Integer getUserWidth(int i) {
            if(i==0)
                return hierarchicalUserWidth;
            return super.getUserWidth(i - 1);
        }

        @Override
        protected int getColumnBasePref(int i) {
            if(i==0)
                return hierarchicalPreferredWidth;
            return super.getColumnBasePref(i - 1);
        }
    };
}
