package runwar.logging;

import java.io.PrintStream;
import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;

public class LoggerPrintStream extends PrintStream {

    public LoggerPrintStream(Logger logger) {
        super(new LoggerStream(logger, org.jboss.logging.Logger.Level.INFO));
    }

    public LoggerPrintStream(Logger logger, org.jboss.logging.Logger.Level level) {
        super(new LoggerStream(logger, level));
    }

    public LoggerPrintStream(Logger logger, Level level, String filter) {
        super(new LoggerStream(logger, level, filter));
    }

}
