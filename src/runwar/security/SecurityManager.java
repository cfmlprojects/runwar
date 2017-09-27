package runwar.security;

import io.undertow.Undertow.Builder;
import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.AuthenticationMode;
import io.undertow.security.handlers.AuthenticationCallHandler;
import io.undertow.security.handlers.AuthenticationConstraintHandler;
import io.undertow.security.handlers.AuthenticationMechanismsHandler;
import io.undertow.security.handlers.SecurityInitialHandler;
import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.DigestCredential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.idm.PasswordCredential;
import io.undertow.security.impl.BasicAuthenticationMechanism;
import io.undertow.server.HttpHandler;
import io.undertow.servlet.api.AuthMethodConfig;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.LoginConfig;
import io.undertow.servlet.api.SecurityConstraint;
import io.undertow.servlet.api.WebResourceCollection;
import io.undertow.servlet.api.SecurityInfo;
import io.undertow.util.HexConverter;
import runwar.logging.Logger;
import runwar.options.ServerOptions;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class SecurityManager implements IdentityManager {

    private static final Charset UTF_8 = StandardCharsets.UTF_8;
    private final Map<String, UserAccount> users = new HashMap<>();
    private static Logger log = Logger.getLogger("SecurityManager");

    public void configureAuth(DeploymentInfo servletBuilder, ServerOptions serverOptions) {
        String realm = serverOptions.getServerName() + " Realm";
        log.debug("Enabling Basic Auth: " + realm);
        for(Entry<String,String> userNpass : serverOptions.getBasicAuth().entrySet()) {
            addUser(userNpass.getKey(), userNpass.getValue(), "role1");
            log.debug(String.format("User:%s password:%s",userNpass.getKey(),userNpass.getValue()));
        }
        LoginConfig loginConfig = new LoginConfig(realm);
        Map<String, String> props = new HashMap<>();
        props.put("charset", "ISO_8859_1");
        props.put("user-agent-charsets", "Chrome,UTF-8,OPR,UTF-8");
        loginConfig.addFirstAuthMethod(new AuthMethodConfig("BASIC", props));
        servletBuilder.setIdentityManager(this).setLoginConfig(loginConfig);
        // TODO: see if we can leverage this stuff
        //addConstraints(servletBuilder, serverOptions);

    }

    public void configureAuth(HttpHandler nextHandler, Builder serverBuilder, ServerOptions serverOptions) {
        String realm = serverOptions.getServerName() + " Realm";
        log.debug("Enabling Basic Auth: " + realm);
        final Map<String, String> users = new HashMap<>(2);
        for(Entry<String,String> userNpass : serverOptions.getBasicAuth().entrySet()) {
            users.put(userNpass.getKey(), userNpass.getValue());
            log.debug(String.format("User:%s password:%s",userNpass.getKey(),userNpass.getValue()));
        }
        serverBuilder.setHandler(addSecurity(nextHandler, users, realm));
    }

    public void addConstraints(DeploymentInfo servletBuilder, ServerOptions serverOptions) {
        servletBuilder.addSecurityConstraint(new SecurityConstraint()
                .addWebResourceCollection(new WebResourceCollection()
                        .addUrlPattern("*"))
                .addRoleAllowed("role1")
                .setEmptyRoleSemantic(SecurityInfo.EmptyRoleSemantic.DENY));
    }

    public HttpHandler addSecurity(final HttpHandler toWrap, final Map<String, String> users, String realm) {
        for(String userName : users.keySet()) {
            addUser(userName, users.get(userName), "role1");
        }
        HttpHandler handler = toWrap;
        handler = new AuthenticationCallHandler(handler);
        handler = new AuthenticationConstraintHandler(handler);
        final List<AuthenticationMechanism> mechanisms = Collections.<AuthenticationMechanism>singletonList(new BasicAuthenticationMechanism(realm));
        handler = new AuthenticationMechanismsHandler(handler, mechanisms);
        handler = new SecurityInitialHandler(AuthenticationMode.PRO_ACTIVE, this, handler);
        return handler;
    }

    public void addUser(final String name, final String password, final String... roles) {
        UserAccount user = new UserAccount();
        user.name = name;
        user.password = password.toCharArray();
        user.roles = new HashSet<>(Arrays.asList(roles));
        users.put(name, user);
    }

    @Override
    public Account verify(Account account) {
        // Just re-use the existing account.
        return account;
    }

    @Override
    public Account verify(String id, Credential credential) {
        Account account = users.get(id);
        if (account != null && verifyCredential(account, credential)) {
            return account;
        }

        return null;
    }

    @Override
    public Account verify(Credential credential) {
        return null;
    }

    private boolean verifyCredential(Account account, Credential credential) {
        // This approach should never be copied in a realm IdentityManager.
        if (account instanceof UserAccount) {
            if (credential instanceof PasswordCredential) {
                char[] expectedPassword = ((UserAccount) account).password;
                char[] suppliedPassword = ((PasswordCredential) credential).getPassword();

                return Arrays.equals(expectedPassword, suppliedPassword);
            } else if (credential instanceof DigestCredential) {
                DigestCredential digCred = (DigestCredential) credential;
                MessageDigest digest = null;
                try {
                    digest = digCred.getAlgorithm().getMessageDigest();

                    digest.update(account.getPrincipal().getName().getBytes(UTF_8));
                    digest.update((byte) ':');
                    digest.update(digCred.getRealm().getBytes(UTF_8));
                    digest.update((byte) ':');
                    char[] expectedPassword = ((UserAccount) account).password;
                    digest.update(new String(expectedPassword).getBytes(UTF_8));

                    return digCred.verifyHA1(HexConverter.convertToHexBytes(digest.digest()));
                } catch (NoSuchAlgorithmException e) {
                    throw new IllegalStateException("Unsupported Algorithm", e);
                } finally {
                    digest.reset();
                }
            }
        }
        return false;
    }

    private static class UserAccount implements Account {
        // In no way whatsoever should a class like this be considered a good idea for a real IdentityManager implementation,
        private static final long serialVersionUID = 8120665150347502722L;
        String name;
        char[] password;
        Set<String> roles;

        private final Principal principal = new Principal() {

            @Override
            public String getName() {
                return name;
            }
        };

        @Override
        public Principal getPrincipal() {
            return principal;
        }

        @Override
        public Set<String> getRoles() {
            return roles;
        }

    }

}
