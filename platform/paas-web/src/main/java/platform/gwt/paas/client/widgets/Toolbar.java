package platform.gwt.paas.client.widgets;

import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.toolbar.ToolStrip;
import com.smartgwt.client.widgets.toolbar.ToolStripButton;
import platform.gwt.paas.client.PaasPlaceManager;
import platform.gwt.paas.client.login.LogoffAuthenticatedEvent;

public class Toolbar extends ToolStrip {
    @Inject
    private EventBus eventBus;
    @Inject
    private PaasPlaceManager placeManager;

    public Toolbar() {
        setWidth100();
    }

    public ToolStripButton addToolStripButton(String iconPath, String toolTip, ClickHandler clickHandler) {
        return addToolStripButton("", iconPath, toolTip, clickHandler);
    }

    public ToolStripButton addToolStripButton(String title, String iconPath, String toolTip, ClickHandler clickHandler) {
        ToolStripButton button = new ToolStripButton(title, "toolbar/" + iconPath);
        button.setTooltip(toolTip);
        button.addClickHandler(clickHandler);

        addButton(button);

        return button;
    }

    public void addLogoffButton() {
        addToolStripButton("Log off", "door.png", "Logoff", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                LogoffAuthenticatedEvent.fire(eventBus);
            }
        });
    }

    public void addHomeButton() {
        addToolStripButton("", "home.png", "Go to start page", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                placeManager.revealDefaultPlace();
            }
        });
    }
}
