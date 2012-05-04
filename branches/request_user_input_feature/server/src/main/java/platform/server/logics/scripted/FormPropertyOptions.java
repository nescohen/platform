package platform.server.logics.scripted;

import platform.interop.ClassViewType;
import platform.interop.PropertyEditType;
import platform.server.form.entity.GroupObjectEntity;
import platform.server.form.entity.PropertyObjectEntity;

import java.util.List;

import static platform.base.BaseUtils.nvl;

public class FormPropertyOptions {
    private PropertyEditType editType;
    private Boolean hintNoUpdate;
    private Boolean hintTable;
    private List<GroupObjectEntity> columns;
    private MappedProperty showIf;
    private PropertyObjectEntity readOnlyIf;
    private PropertyObjectEntity background;
    private PropertyObjectEntity foreground;
    private PropertyObjectEntity header;
    private PropertyObjectEntity footer;
    private ClassViewType forceViewType;
    private GroupObjectEntity toDraw;

    public PropertyEditType getEditType() {
        return editType;
    }

    public void setEditType(PropertyEditType editType) {
        this.editType = editType;
    }

    public List<GroupObjectEntity> getColumns() {
        return columns;
    }

    public void setColumns(List<GroupObjectEntity> columns) {
        this.columns = columns;
    }

    public void setShowIf(MappedProperty showIf) {
        this.showIf = showIf;
    }

    public MappedProperty getShowIf() {
        return showIf;
    }

    public PropertyObjectEntity getReadOnlyIf() {
        return readOnlyIf;
    }

    public void setReadOnlyIf(PropertyObjectEntity readOnlyIf) {
        this.readOnlyIf = readOnlyIf;
    }

    public PropertyObjectEntity getBackground() {
        return background;
    }

    public void setBackground(PropertyObjectEntity background) {
        this.background = background;
    }

    public PropertyObjectEntity getForeground() {
        return foreground;
    }

    public void setForeground(PropertyObjectEntity foreground) {
        this.foreground = foreground;
    }

    public PropertyObjectEntity getHeader() {
        return header;
    }

    public void setHeader(PropertyObjectEntity header) {
        this.header = header;
    }

    public PropertyObjectEntity getFooter() {
        return footer;
    }

    public void setFooter(PropertyObjectEntity footer) {
        this.footer = footer;
    }

    public void setForceViewType(ClassViewType forceViewType) {
        this.forceViewType = forceViewType;
    }

    public ClassViewType getForceViewType() {
        return forceViewType;
    }

    public void setToDraw(GroupObjectEntity toDraw) {
        this.toDraw = toDraw;
    }

    public GroupObjectEntity getToDraw() {
        return toDraw;
    }

    public Boolean getHintNoUpdate() {
        return hintNoUpdate;
    }

    public void setHintNoUpdate(Boolean hintNoUpdate) {
        this.hintNoUpdate = hintNoUpdate;
    }

    public Boolean getHintTable() {
        return hintTable;
    }

    public void setHintTable(Boolean hintTable) {
        this.hintTable = hintTable;
    }
    
    public FormPropertyOptions overrideWith(FormPropertyOptions overrides) {
        FormPropertyOptions merged = new FormPropertyOptions();

        merged.setEditType(nvl(overrides.getEditType(), editType));
        merged.setHintNoUpdate(nvl(overrides.getHintNoUpdate(), hintNoUpdate));
        merged.setHintTable(nvl(overrides.getHintTable(), hintTable));
        merged.setColumns(nvl(overrides.getColumns(), columns));
        merged.setShowIf(nvl(overrides.getShowIf(), showIf));
        merged.setReadOnlyIf(nvl(overrides.getReadOnlyIf(), readOnlyIf));
        merged.setBackground(nvl(overrides.getBackground(), background));
        merged.setForeground(nvl(overrides.getForeground(), foreground));
        merged.setHeader(nvl(overrides.getHeader(), header));
        merged.setFooter(nvl(overrides.getFooter(), footer));
        merged.setForceViewType(nvl(overrides.getForceViewType(), forceViewType));
        merged.setToDraw(nvl(overrides.getToDraw(), toDraw));

        return merged;
    }
}
