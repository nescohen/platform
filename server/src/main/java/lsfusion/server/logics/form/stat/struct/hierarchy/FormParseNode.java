package lsfusion.server.logics.form.stat.struct.hierarchy;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.logics.form.struct.object.ObjectEntity;

public class FormParseNode extends GroupParseNode {

    public FormParseNode(ImOrderSet<ParseNode> children) {
        super(children);
    }

    @Override
    public <T extends Node<T>> void importNode(T node, ImMap<ObjectEntity, Object> upValues, ImportData importData) {
        importChildrenNodes(node, upValues, importData);
    }

    @Override
    public <T extends Node<T>> boolean exportNode(T node, ImMap<ObjectEntity, Object> upValues, ExportData exportData) {
        return exportChildrenNodes(node, upValues, exportData);
    }
}
