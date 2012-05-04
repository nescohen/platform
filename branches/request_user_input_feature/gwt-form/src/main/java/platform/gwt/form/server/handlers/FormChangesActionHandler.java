package platform.gwt.form.server.handlers;

import net.customware.gwt.dispatch.shared.Action;
import platform.client.logics.ClientFormChanges;
import platform.gwt.base.server.FormSessionObject;
import platform.gwt.form.server.RemoteFormServiceImpl;
import platform.gwt.form.shared.actions.form.FormChangesResult;
import platform.interop.form.RemoteChanges;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public abstract class FormChangesActionHandler<A extends Action<FormChangesResult>> extends FormServiceActionHandler<A, FormChangesResult> {
    protected FormChangesActionHandler(RemoteFormServiceImpl servlet) {
        super(servlet);
    }

    protected FormChangesResult getRemoteChanges(FormSessionObject form) throws IOException {
        return getRemoteChanges(form, form.remoteForm.getRemoteChanges());
    }

    protected FormChangesResult getRemoteChanges(FormSessionObject form, RemoteChanges remoteChanges) throws IOException {
        ClientFormChanges clientChanges =
                new ClientFormChanges(new DataInputStream(new ByteArrayInputStream(remoteChanges.form)), form.clientForm, null);
        return new FormChangesResult(clientChanges.getGwtFormChangesDTO());
    }
}
