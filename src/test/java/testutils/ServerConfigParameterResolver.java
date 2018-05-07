package testutils;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import runwar.options.ServerOptionsImpl;

public class ServerConfigParameterResolver implements ParameterResolver {

        @Override
        public boolean supportsParameter(ParameterContext parameterContext,
                                         ExtensionContext extensionContext) throws ParameterResolutionException {
            System.out.println("PARAM@TEST");
            return parameterContext.getParameter().getType()
                    .equals(ServerOptionsImpl.class);
        }

        @Override
        public Object resolveParameter(ParameterContext parameterContext,
                                       ExtensionContext extensionContext) throws ParameterResolutionException {
            System.out.println("PARAM@TEST");
            return new ServerOptionsImpl();
        }
    }
