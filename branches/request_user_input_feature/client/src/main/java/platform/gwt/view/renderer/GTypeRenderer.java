package platform.gwt.view.renderer;

import com.smartgwt.client.widgets.Canvas;

import java.io.Serializable;

public interface GTypeRenderer extends Serializable {
    Canvas getComponent();
    void setValue(Object value);
    void setChangedHandler(PropertyChangedHandler handler);
}
