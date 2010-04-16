package platform.client.logics;

import platform.client.form.ClientForm;
import platform.client.form.PropertyEditorComponent;
import platform.interop.form.RemoteFormInterface;
import platform.interop.navigator.RemoteNavigatorInterface;
import platform.base.BaseUtils;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Collection;
import java.rmi.RemoteException;

public class ClientObjectView extends ClientCellView {

    public ClientObjectImplementView object;

    public final boolean show;

    public ClientObjectView(DataInputStream inStream, Collection<ClientContainerView> containers, ClientObjectImplementView iObject) throws IOException, ClassNotFoundException {
        super(inStream, containers);
        object = iObject;

        show = inStream.readBoolean();
    }

    public int getID() {
        return object.getID();
    }

    public ClientGroupObjectImplementView getGroupObject() {
        return object.groupObject;
    }

    public int getMaximumWidth() {
        return getPreferredWidth();
    }

    public PropertyEditorComponent getEditorComponent(ClientForm form, Object value) throws IOException, ClassNotFoundException {

        if (form.switchClassView(object.groupObject))
            return null;
        else
            return baseType.getEditorComponent(form, this, value, getFormat());
    }

    public PropertyEditorComponent getClassComponent(ClientForm form, Object value) throws IOException, ClassNotFoundException {
        return baseType.getClassComponent(form, this, value, getFormat());
    }

    public RemoteFormInterface createForm(RemoteNavigatorInterface navigator) throws RemoteException {
        return navigator.createObjectForm(object.getID());
    }

    public RemoteFormInterface createClassForm(RemoteNavigatorInterface navigator, Integer value) throws RemoteException {
        return navigator.createObjectForm(object.getID(), BaseUtils.objectToInt(value));
    }
}
