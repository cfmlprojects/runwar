package runwar.util;

import runwar.logging.RunwarLogger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutionException;

import static runwar.util.Reflection.invoke;
import static runwar.util.Reflection.method;
import static runwar.util.Reflection.load;

public class FusionReactor {

    boolean hasFusionReactor = false;

    private Class<?> frapi;
    private Method getAppMethod;
    private Object appInstance;
    private Method setTransactionName;
    private Method setTransactionApplicationName;
    private ClassLoader classLoader;

    public FusionReactor(ClassLoader classLoader) {
        this.classLoader = classLoader;
        try {
            frapi = load(classLoader,"com.intergral.fusionreactor.api.FRAPI");
            hasFusionReactor = true;
        } catch (Exception e) {
            hasFusionReactor = false;
            return;
        }
        try {
            getAppMethod = method(frapi, "getInstance");
            setTransactionName = method(frapi,"setTransactionName", java.lang.String.class);
            setTransactionApplicationName = method(frapi,"setTransactionApplicationName", java.lang.String.class);
        } catch (Exception e) {
            e.printStackTrace();
            hasFusionReactor = false;
        }
    }


    public boolean hasFusionReactor() {
        return hasFusionReactor;
    }

    public void setFusionReactorInfo(String transactionName, String transactionApplicationName) {
        if (!hasFusionReactor) {
            RunwarLogger.CONTEXT_LOG.warn("Trying to set FusionReactor options without fusion reactor in the class path");
            return;
        }
        try {
            appInstance = invoke(getAppMethod,frapi);
            invoke(setTransactionName,appInstance, transactionName);
            invoke(setTransactionApplicationName, appInstance, transactionApplicationName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
