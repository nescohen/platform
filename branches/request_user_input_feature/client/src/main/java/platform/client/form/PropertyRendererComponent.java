package platform.client.form;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public interface PropertyRendererComponent {
    Color SELECTED_ROW_BORDER_COLOR = new Color(175, 175, 255);
    Color SELECTED_ROW_BACKGROUND = new Color(249, 249, 255);
    Color SELECTED_CELL_BACKGROUND = new Color(237, 238, 244);
    Color FOCUSED_CELL_BORDER_COLOR = new Color(98, 98, 255);
    Color FOCUSED_CELL_BACKGROUND = new Color(232, 232, 255);
    Border SELECTED_ROW_BORDER = BorderFactory.createMatteBorder(1, 0, 1, 0, SELECTED_ROW_BORDER_COLOR);
    Border FOCUSED_CELL_BORDER = BorderFactory.createMatteBorder(1, 1, 1, 1, FOCUSED_CELL_BORDER_COLOR);

    JComponent getComponent();

    void setValue(Object value, boolean isSelected, boolean hasFocus);

    void rateSelected();
}


