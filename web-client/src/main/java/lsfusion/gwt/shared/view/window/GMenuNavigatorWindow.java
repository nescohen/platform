package lsfusion.gwt.shared.view.window;

import lsfusion.gwt.client.navigator.GINavigatorController;
import lsfusion.gwt.client.navigator.GMenuNavigatorView;
import lsfusion.gwt.client.navigator.GNavigatorView;

public class GMenuNavigatorWindow extends GNavigatorWindow {
    public int showLevel;
    public int orientation;

    @Override
    public GNavigatorView createView(GINavigatorController navigatorController) {
        return new GMenuNavigatorView(this, navigatorController);
    }
}
