package lsfusion.server.logics.property.actions.integration.importing.plain.table;

import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.GroupObjectEntity;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.actions.integration.importing.plain.ImportPlainActionProperty;
import lsfusion.server.logics.property.actions.integration.importing.plain.ImportPlainIterator;

import java.io.IOException;

public class ImportTableActionProperty extends ImportPlainActionProperty<ImportTableIterator> {

    public ImportTableActionProperty(int paramsCount, LCP<?> fileProperty, ImOrderSet<GroupObjectEntity> groupFiles, FormEntity formEntity) {
        super(paramsCount, fileProperty, groupFiles, formEntity);
    }

    @Override
    public ImportPlainIterator getIterator(byte[] file, ImOrderMap<String, Type> fieldTypes, ExecutionContext<PropertyInterface> context) throws IOException {
        return new ImportTableIterator(fieldTypes, file);
    }
}