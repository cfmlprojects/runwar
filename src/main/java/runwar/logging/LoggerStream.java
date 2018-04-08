package runwar.logging;

import java.io.IOException;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import org.apache.log4j.Level;
//import org.apache.log4j.Logger;
import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;

public class LoggerStream extends OutputStream {
    private final Logger logger;
    private final Level logLevel;
    private static Matcher ignoreLineMatcher;

    public LoggerStream(Logger logger, Level logLevel, String ignoreLineRegex) {
        super();
        this.logger = logger;
        this.logLevel = logLevel;
        ignoreLineMatcher = Pattern.compile(ignoreLineRegex).matcher("");
    }

    public LoggerStream(Logger logger, Level logLevel) {
        super();
        this.logger = logger;
        this.logLevel = logLevel;
    }

    @Override
    public void write(byte[] b) throws IOException {
        String string = new String(b);
        if (shouldLog(string))
            logger.log(logLevel, string.trim());
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        String string = new String(b, off, len);
        if (shouldLog(string))
            logger.log(logLevel, string.trim());
    }

    @Override
    public void write(int b) throws IOException {
        String string = String.valueOf((char) b);
        if (shouldLog(string))
            logger.log(logLevel, string.trim());
    }

    private static boolean shouldLog(String string) {
        if (ignoreLineMatcher != null) {
            if (!ignoreLineMatcher.reset(string).matches() && !string.trim().isEmpty()) {
                return true;
            }
        } else {
            if (!string.trim().isEmpty())
                return true;
        }
        return false;
    }
}
