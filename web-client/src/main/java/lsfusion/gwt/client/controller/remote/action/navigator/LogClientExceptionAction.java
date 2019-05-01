package lsfusion.gwt.client.controller.remote.action.navigator;

import com.google.gwt.core.shared.SerializableThrowable;
import lsfusion.gwt.client.base.exception.GExceptionManager;
import lsfusion.gwt.client.base.exception.NonFatalHandledException;
import lsfusion.gwt.client.base.exception.StackedException;
import lsfusion.gwt.client.base.result.VoidResult;
import net.customware.gwt.dispatch.shared.DispatchException;

public class LogClientExceptionAction extends NavigatorAction<VoidResult> {
    public Throwable throwable;

    public LogClientExceptionAction() {
    }

    // result throwable class should exist on web-server
    private static Throwable fromWebClientToWebServer(Throwable t) {
        if(t instanceof DispatchException) // because that exception came from server, it will definitely be able to go back to server
            return t;
        if(t instanceof NonFatalHandledException || t instanceof StackedException) // this exception are shared
            return t;
        
        Throwable webServerException = new SerializableThrowable("", GExceptionManager.copyMessage(t));
        GExceptionManager.copyStackTraces(t, webServerException);
        return webServerException;
    }

    public LogClientExceptionAction(Throwable throwable) {
        this.throwable = fromWebClientToWebServer(throwable);
    }

    @Override
    public boolean logRemoteException() {
        return false;
    }
}
