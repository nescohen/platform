package platform.gwt.base.server.spring;

import org.springframework.dao.DataAccessException;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import platform.base.ExceptionUtils;
import platform.interop.exceptions.LoginException;
import platform.interop.remote.UserInfo;

import java.rmi.RemoteException;
import java.util.ArrayList;

public class GwtLogicsUserDetailsService implements UserDetailsService {

    private BusinessLogicsProvider businessLogicProvider;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException, DataAccessException {
        try {
            UserInfo info = businessLogicProvider.getLogics().getUserInfo(username);

            ArrayList<GrantedAuthorityImpl> authorities = new ArrayList<GrantedAuthorityImpl>();
            for (String role : info.roles) {
                authorities.add(new GrantedAuthorityImpl(role));
            }

            return new User(info.username, info.password, true, true, true, true, authorities);
        } catch (LoginException le) {
            throw new UsernameNotFoundException(le.getMessage());
        } catch (RemoteException e) {
            if (!ExceptionUtils.isRecoverableRemoteException(e)) {
                businessLogicProvider.invalidate();
            }

            throw new RuntimeException("Ошибка во время чтения данных о пользователе.", e);
        }
    }

    public void setBusinessLogicProvider(BusinessLogicsProvider businessLogicProvider) {
        this.businessLogicProvider = businessLogicProvider;
    }
}
