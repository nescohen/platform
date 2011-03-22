package platform.client.form.tree;

import org.jdesktop.swingx.treetable.DefaultTreeTableModel;
import org.jdesktop.swingx.treetable.MutableTreeTableNode;
import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.client.form.ClientFormController;
import platform.client.logics.ClientGroupObject;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;

import javax.swing.tree.TreePath;
import java.io.IOException;
import java.util.*;

class GroupTreeTableModel extends DefaultTreeTableModel {
    private final Map<ClientGroupObject, Set<TreeGroupNode>> groupNodes = new HashMap<ClientGroupObject, Set<TreeGroupNode>>();
    public final List<ClientPropertyDraw> properties = new ArrayList<ClientPropertyDraw>();
    public final List<ClientPropertyDraw> columnProperties = new ArrayList<ClientPropertyDraw>();
    public final Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> values = new HashMap<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>>();
    public final Map<ClientGroupObject, List<ClientPropertyDraw>> groupPropsMap = new HashMap<ClientGroupObject, List<ClientPropertyDraw>>();

    private final ClientFormController form;
    private final boolean plainTreeMode;

    public GroupTreeTableModel(ClientFormController form, boolean plainTreeMode) {
        super();
        this.form = form;
        this.plainTreeMode = plainTreeMode;
        root = new TreeGroupNode(this);
    }

    @Override
    public TreeGroupNode getRoot() {
        return (TreeGroupNode) super.getRoot();
    }

    @Override
    public int getColumnCount() {
        return plainTreeMode ? 1 : 1 + columnProperties.size();
    }

    @Override
    public String getColumnName(int column) {
        if (column == 0) {
            return "Дерево";
        }

        return getColumnProperty(column).getFullCaption();
    }

    @Override
    public Object getValueAt(Object node, int column) {
        if (column == 0) {
            return plainTreeMode ? node.toString() : "";
        }

        if (node instanceof TreeGroupNode) {
            ClientPropertyDraw property = getProperty(node, column);

            if (property == null) {
                return null;
            }

            Object o = values.get(property).get(((TreeGroupNode) node).key);
            return o instanceof String ? BaseUtils.rtrim((String) o) : o;
        }
        return node.toString();
    }

    @Override
    public void setValueAt(Object value, Object node, int column) {
        ClientPropertyDraw property = getProperty(node, column);
        if (property != null) {
            try {
                form.changePropertyDraw(property, value, false, ((TreeGroupNode) node).key);
            } catch (IOException e) {
                throw new RuntimeException("Ошибка при изменении значения свойства", e);
            }
        }
    }

    @Override
    public boolean isCellEditable(Object node, int column) {
        ClientPropertyDraw property = getProperty(node, column);
        return column != 0 && property != null && !property.readOnly;
    }

    public Object getPropertyValue(Object node, ClientPropertyDraw property) {
        if (node instanceof TreeGroupNode) {
            Map<ClientGroupObjectValue, Object> properties = values.get(property);
            if (properties != null) {
                return properties.get(((TreeGroupNode) node).key);
            }
        }
        return null;
    }

    public boolean isCellFocusable(Object node, int column) {
        if (column == 0) {
            return true;
        }
        ClientPropertyDraw property = getProperty(node, column);
        if (property == null) {
            return false;
        }

        Boolean focusable = property.focusable;
        return focusable == null || focusable;
    }

    public ClientPropertyDraw getColumnProperty(int col) {
        return columnProperties.get(col - 1);
    }

    public ClientPropertyDraw getProperty(Object node, int column) {
        if (node instanceof TreeGroupNode) {
            List<ClientPropertyDraw> groupProperties = groupPropsMap.get(((TreeGroupNode) node).group);
            if (groupProperties == null || column == 0 || column > groupProperties.size()) {
                return null;
            }

            return groupProperties.get(column - 1);
        }
        return null;
    }

    public Set<TreeGroupNode> getGroupNodes(ClientGroupObject group) { // так как mutable надо аккуратно пользоваться а то можно на concurrent нарваться
        if (group == null) {
            return Collections.singleton(getRoot());
        }

        Set<TreeGroupNode> nodes = groupNodes.get(group);
        if (nodes == null) {
            nodes = new HashSet<TreeGroupNode>();
            groupNodes.put(group, nodes);
        }
        return nodes;
    }

    public void updateKeys(ClientGroupObject group, List<ClientGroupObjectValue> keys, List<ClientGroupObjectValue> parents) {
        // приводим переданную структуры в нормальную - child -> parent
        OrderedMap<ClientGroupObjectValue, ClientGroupObjectValue> parentTree = new OrderedMap<ClientGroupObjectValue, ClientGroupObjectValue>();
        for (int i = 0; i < keys.size(); i++) {
            ClientGroupObjectValue key = keys.get(i);

            ClientGroupObjectValue parentPath = new ClientGroupObjectValue(key); // значение для непосредственного родителя
            parentPath.removeAll(group.objects); // удаляем значение ключа самого groupObject, чтобы получить путь к нему из "родителей"
            parentPath.putAll(parents.get(i)); //рекурсивный случай - просто перезаписываем значения для ObjectInstance'ов
            parentTree.put(key, parentPath);
        }

        Map<ClientGroupObjectValue, List<ClientGroupObjectValue>> childTree = BaseUtils.groupList(parentTree);
        for (TreeGroupNode groupNode : getGroupNodes(group.getUpTreeGroup())) {
            synchronize(groupNode, group, childTree);
        }
    }

    void synchronize(TreeGroupNode parent, ClientGroupObject syncGroup, Map<ClientGroupObjectValue, List<ClientGroupObjectValue>> tree) {
        final List<ClientGroupObjectValue> syncChilds = tree.containsKey(parent.key)
                                                        ? tree.get(parent.key)
                                                        : new ArrayList<ClientGroupObjectValue>();

        if (parent.hasOnlyExpandningNodeAsChild()) {
            // убираем +
            parent.removeFirstChild();
        }

        List<TreeGroupNode> allChildren = new ArrayList<TreeGroupNode>();
        TreeGroupNode[] thisGroupChildren = new TreeGroupNode[syncChilds.size()];

        for (TreeGroupNode child : BaseUtils.<TreeGroupNode>copyTreeChildren(parent.getChildren())) {
            // бежим по node'ам
            if (child.group.equals(syncGroup)) {
                int index = syncChilds.indexOf(child.key);
                if (index == -1) {
                    parent.removeChild(child);
                    removeFromGroupNodes(syncGroup, child);
                } else { // помечаем что был, и рекурсивно синхронизируем child
                    thisGroupChildren[index] = child;
                    synchronize(child, syncGroup, tree);
                }
            } else {
                allChildren.add(child);
            }
        }

        for (int i = 0; i < syncChilds.size(); ++i) {
            if (thisGroupChildren[i] == null) {
                TreeGroupNode newNode = new TreeGroupNode(this, syncGroup, syncChilds.get(i));
                thisGroupChildren[i] = newNode;
                parent.addChild(newNode);

                getGroupNodes(syncGroup).add(newNode);

                if (syncGroup.mayHaveChildren()) {
                    newNode.addChild(new ExpandingTreeTableNode());
                }
            }
        }

        if (parent.group == syncGroup) {
            allChildren.addAll(0, Arrays.asList(thisGroupChildren));
        } else {
            allChildren.addAll(Arrays.asList(thisGroupChildren));
        }

        parent.removeAllChildren();

        for (TreeGroupNode child : allChildren) {
            parent.addChild(child);
        }

        if (parent.getChildCount() == 0) {
            if (parent.group != null && parent.group.mayHaveChildren()) {
                parent.addChild(new ExpandingTreeTableNode());
            }
        }
    }

    private void removeFromGroupNodes(ClientGroupObject syncGroup, TreeGroupNode node) {
        getGroupNodes(syncGroup).remove(node);

        for (MutableTreeTableNode child : Collections.list(node.children())) {
            if (child instanceof TreeGroupNode) {
                removeFromGroupNodes(syncGroup.getDownGroup(), (TreeGroupNode) child);
            }
        }
    }

    public void updateDrawPropertyValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> ivalues) {
        values.put(property, ivalues);
    }

    public int addDrawProperty(ClientFormController form, ClientGroupObject group, ClientPropertyDraw property) {
        if (properties.indexOf(property) == -1) {
            int ins = BaseUtils.relativePosition(property, form.getPropertyDraws(), properties);
            properties.add(ins, property);

            List<ClientPropertyDraw> groupProperties = groupPropsMap.get(group);
            if (groupProperties == null) {
                groupProperties = new ArrayList<ClientPropertyDraw>();
                groupPropsMap.put(group, groupProperties);
            }
            int gins = BaseUtils.relativePosition(property, properties, groupProperties);
            groupProperties.add(gins, property);

            if (group.isLastGroupInTree()) {
                int tins = BaseUtils.relativePosition(property, properties, columnProperties);
                columnProperties.add(tins, property);
                return tins + 1;
            }
        }
        return -1;
    }

    public int removeProperty(ClientGroupObject group, ClientPropertyDraw property) {
        properties.remove(property);
        groupPropsMap.get(group).remove(property);

        int ind = columnProperties.indexOf(property);
        if (ind != -1) {
            columnProperties.remove(property);
        }
        return ind + 1;
    }

    public void firePathChanged(TreePath nodePath) {
        modelSupport.firePathChanged(nodePath);
    }
}
