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

    public abstract boolean getStaticFile();

    public abstract Pattern getPattern();

    public abstract Pattern getHostPattern();

    public abstract List<Arg> getArgs();

    public abstract Map<String, String> getStaticArgs();

    public abstract List<String> getFormats();

    public abstract String getHost();

    public abstract Arg getHostArg();

    public abstract String getRoutesFile();

    /**
     * Check if the parts of a HTTP request equal this Route.
     *
     * @param method GET/POST/etc.
     * @param path   Part after domain and before query-string. Starts with a "/".
     * @param accept Format, e.g. html.
     * @param host   AKA the domain.
     * @return ???
     */
    public abstract Map<String, String> matches(String method, String path, String accept, String host);

    public Map<String, String> matches(String method, String path) {
        return matches(method, path, null, null);
    }

    public Map<String, String> matches(String method, String path, String accept) {
        return matches(method, path, accept, null);
    }
}
