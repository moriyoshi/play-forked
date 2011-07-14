package play.mvc;

import java.util.List;
import java.util.Map;

import jregex.Pattern;

public abstract class Route {
    public static class Arg {
        String name;
        Pattern constraint;
        String defaultValue;
        Boolean optional = false;
    }
    /**
     * HTTP method, e.g. "GET".
     */
    public abstract String getMethod();

    public abstract String getPath();

    public abstract String getAction();

    public abstract Pattern getActionPattern();

    public abstract List<String> getActionArgs();

    public abstract String getStaticDir();

    public abstract String getStaticFile();

    public abstract Pattern getPattern();

    public abstract Pattern getHostPattern();

    public abstract List<Arg> getArgs();

    public abstract Map<String, String> getStaticArgs();

    public abstract List<String> getFormats();

    public abstract String getHost();

    public abstract Arg getHostArg();

    public abstract String getRoutesFile();
}
