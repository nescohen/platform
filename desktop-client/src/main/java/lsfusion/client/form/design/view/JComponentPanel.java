package lsfusion.client.form.design.view;

import lsfusion.interop.base.view.FlexLayout;
import lsfusion.interop.form.design.Alignment;

import javax.swing.*;
import java.awt.*;

import static lsfusion.client.base.SwingUtils.overrideSize;

// выполняет роль FlexPanel в web
public class JComponentPanel extends JPanel {

    public JComponentPanel(boolean vertical, Alignment alignment) {
        super(null);
        setLayout(new FlexLayout(this, vertical, alignment));
    }
    public JComponentPanel() {
        this(new BorderLayout());
    }
    public JComponentPanel(LayoutManager layout) {
        super(layout);
    }

    private Dimension componentSize;

    public void setComponentSize(Dimension componentSize) {
        this.componentSize = componentSize;
    }

    @Override
    public Dimension getPreferredSize() {
        return overrideSize(super.getPreferredSize(), componentSize);
    }

    public Dimension getMaxPreferredSize() {
        return getPreferredSize();
    }
}
