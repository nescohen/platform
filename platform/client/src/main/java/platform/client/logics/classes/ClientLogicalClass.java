package platform.client.logics.classes;

import platform.client.form.*;
import platform.client.form.renderer.LogicalPropertyRenderer;
import platform.client.form.editor.LogicalPropertyEditor;

import java.io.DataInputStream;
import java.io.IOException;
import java.text.Format;
import java.awt.*;

public class ClientLogicalClass extends ClientDataClass {

    public ClientLogicalClass(DataInputStream inStream) throws IOException {
        super(inStream);
    }

    @Override
    public int getMinimumWidth(FontMetrics fontMetrics) {
        return getPreferredWidth(fontMetrics);
    }


    public int getPreferredWidth(FontMetrics fontMetrics) {
        return 25;
    }

    public String getPreferredMask() {
        return "";
    }

    public Format getDefaultFormat() {
        return null;
    }

    public PropertyRendererComponent getRendererComponent(Format format, String caption, Font font) { return new LogicalPropertyRenderer(); }
    public PropertyEditorComponent getComponent(Object value, Format format, Font font) { return new LogicalPropertyEditor(value); }
}
