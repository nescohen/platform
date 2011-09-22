package platform.gwt.paas.client;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootPanel;
import com.gwtplatform.mvp.client.DelayedBindRegistry;
import platform.gwt.paas.client.gin.PaasGinjector;
import platform.gwt.paas.client.login.LoginAuthenticatedEvent;

public class Paas implements EntryPoint {

    interface GlobalResources extends ClientBundle {
        @CssResource.NotStrict
        @Source("Paas.css")
        CssResource css();
    }

//    private static PaasConstants constants;

    public final static PaasGinjector ginjector = GWT.create(PaasGinjector.class);

    private long startTimeMillis;

    public void onModuleLoad() {
        Log.setUncaughtExceptionHandler();

        // Defer all application initialisation code to defferedOnModuleLoad() so that the
        // UncaughtExceptionHandler can catch any unexpected exceptions.
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                defferedOnModuleLoad();
            }
        });
    }

    private void defferedOnModuleLoad() {
        try {
            if (Log.isDebugEnabled()) {
                startTimeMillis = System.currentTimeMillis();
            }

            // inject global styles
            GWT.<GlobalResources>create(GlobalResources.class).css().ensureInjected();

            // load constants
//            constants = (PaasConstants) GWT.create(PaasConstants.class);

            // this is required by gwt-platform proxy's generator
            DelayedBindRegistry.bind(ginjector);

            initializeLoggedInUser();

            ginjector.getPlaceManager().revealCurrentPlace();

            // hide the animated 'loading.gif'
            RootPanel loading = RootPanel.get("loading");
            if (loading != null) {
                loading.setVisible(false);
            }

            if (Log.isDebugEnabled()) {
                long endTimeMillis = System.currentTimeMillis();
                float durationSeconds = (endTimeMillis - startTimeMillis) / 1000F;
                Log.debug("Duration: " + durationSeconds + " seconds");
            }
        } catch (Exception e) {
            Log.error("e: " + e);
            e.printStackTrace();
            Window.alert(e.getMessage());
        }
    }

    public String initializeLoggedInUser() {
        try {
            Dictionary dict = Dictionary.getDictionary("parameters");
            if (dict != null) {
                String username = dict.get("username");
                if (username != null) {
                    LoginAuthenticatedEvent.fire(ginjector.getEventBus(), username);
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }


//    public static PaasConstants getConstants() {
//        return constants;
//    }
}
