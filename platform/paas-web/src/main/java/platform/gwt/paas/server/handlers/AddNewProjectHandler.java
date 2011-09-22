package platform.gwt.paas.server.handlers;

import com.gwtplatform.dispatch.server.ExecutionContext;
import com.gwtplatform.dispatch.shared.ActionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import paas.api.remote.PaasRemoteInterface;
import platform.gwt.base.server.spring.BusinessLogicsProvider;
import platform.gwt.paas.shared.actions.AddNewProjectAction;
import platform.gwt.paas.shared.actions.GetProjectsResult;

import java.rmi.RemoteException;

@Component
public class AddNewProjectHandler extends SimpleActionHandlerEx<AddNewProjectAction, GetProjectsResult> {

    @Autowired
    private BusinessLogicsProvider<PaasRemoteInterface> blProvider;

    @Override
    public GetProjectsResult executeEx(final AddNewProjectAction action, final ExecutionContext context) throws ActionException, RemoteException {
        return new GetProjectsResult(
                blProvider.getLogics().addNewProject(
                        getAuthentication().getName(), action.project));
    }
}

