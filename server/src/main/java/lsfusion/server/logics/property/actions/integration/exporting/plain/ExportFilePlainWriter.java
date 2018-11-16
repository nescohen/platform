package lsfusion.server.logics.property.actions.integration.exporting.plain;

import lsfusion.base.IOUtils;
import lsfusion.base.RawFileData;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.server.data.type.Type;

import java.io.File;
import java.io.IOException;

public abstract class ExportFilePlainWriter extends ExportPlainWriter {
    
    protected final File file;

    public ExportFilePlainWriter(ImOrderMap<String, Type> fieldTypes) throws IOException {
        super(fieldTypes);
        file = File.createTempFile("file", ".exp");
    }

    public RawFileData release() throws IOException {
        RawFileData result;
        try {
            closeWriter();
        } finally {
            result = new RawFileData(file);
            if(!file.delete())
                file.deleteOnExit();
        }
        return result;
    }

    protected abstract void closeWriter() throws IOException;
}
