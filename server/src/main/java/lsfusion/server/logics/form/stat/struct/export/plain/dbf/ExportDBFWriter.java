package lsfusion.server.logics.form.stat.struct.export.plain.dbf;

import com.google.common.base.Throwables;
import com.hexiong.jdbf.DBFWriter;
import com.hexiong.jdbf.JDBFException;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.form.stat.struct.export.plain.ExportFilePlainWriter;

import java.io.IOException;

public class ExportDBFWriter extends ExportFilePlainWriter {

    private final DBFWriter writer; 

    public ExportDBFWriter(ImOrderMap<String, Type> fieldTypes, String charset) throws IOException, JDBFException {
        super(fieldTypes);

        OverJDBField[] fields = getFields();
        if(fields.length == 0) // dbf format (with 13 terminator) just does not support no fields  
            fields = new OverJDBField[] {new OverJDBField("dumb", 'N', 1, 0) };
        writer = new DBFWriter(file.getAbsolutePath(), fields, charset);
    }

    public void writeLine(ImMap<String, Object> row) {
        try {
            Object[] record = fieldTypes.keyOrderSet().mapList(row).toArray(new Object[row.size()]);
            if(record.length == 0)
                record = new Object[] {0};
            writer.addRecord(record);
        } catch (JDBFException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    protected void closeWriter() {
        try {
            writer.close();
        } catch (JDBFException e) {
            throw Throwables.propagate(e);
        }
    }

    private OverJDBField[] getFields() {
        ImOrderSet<OverJDBField> fields = fieldTypes.mapOrderSetValues((key, value) -> {
            try {
                return value.formatDBF(key);
            } catch (JDBFException e) {
                throw new RuntimeException(e);
            }
        });
        return fields.toArray(new OverJDBField[fields.size()]);
    }
}