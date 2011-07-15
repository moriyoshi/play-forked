package play.mvc;

import java.util.EnumSet;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.net.URI;
import java.net.URISyntaxException;
import play.vfs.VirtualFile;
import play.exceptions.UnexpectedException;
import play.exceptions.NoRouteFoundException;
import play.Play;

public abstract class Router {
    public abstract Iterable<Route> getRoutes();

    public abstract Map<String, String> route(Http.Verb method, String path, String headers, String host);

    public abstract Route route(Http.Request request);

    public abstract void routeOnlyStatic(Http.Request request);

    public abstract String reverse(VirtualFile file, boolean absolute);

    public abstract AbstractActionDefinition reverse(String action, Http.Verb verb, Map<String, Object> args);

    /**
     * Add a route at the given position
     */
    public abstract void addRoute(int position, EnumSet<Http.Verb> methods, String path, String action, String params, String headers);

    public abstract void addRoute(EnumSet<Http.Verb> methods, String path, String action, String params, String headers);

    public AbstractActionDefinition reverse(String action, Map<String, Object> args) {
        return reverse(action, null, args);
    }

    public Map<String, String> route(Http.Verb method, String path) {
        return route(method, path, null, null);
    }

    public Map<String, String> route(Http.Verb method, String path, String headers) {
        return route(method, path, headers, null);
    }

    public String reverse(VirtualFile file) {
        return reverse(file, false);
    }

    public AbstractActionDefinition reverse(String action, Http.Verb verb) {
        // Note the map is not <code>Collections.EMPTY_MAP</code> because it will be copied and changed.
        return reverse(action, verb, new HashMap<String, Object>());
    }

    public String reverseWithCheck(String name, VirtualFile file, boolean absolute) {
        if (file == null || !file.exists()) {
            throw new NoRouteFoundException(name + " (file not found)");
        }
        return reverse(file, absolute);
    }

    public static URI absolutize(URI url) {
        return absolutize(url, false);
    }

    public static URI absolutize(URI uri, boolean websocket) {
        if (uri.getScheme() != null) {
            // already an absolute URI
            return uri;
        }
        String scheme = null;
        if (websocket) {
            scheme = "ws";
        } else {
            if (Http.Request.current() != null && Http.Request.current().secure) {
                scheme = "https";
            } else {
                scheme = "http";
            }
        }
        String hostPart = uri.getHost();
        String domain = Http.Request.current() == null ? "" : Http.Request.current().get().domain;
        int port = Http.Request.current() == null ? 80 : Http.Request.current().get().port;
        // ~
        if (hostPart == null) {
            hostPart = domain;
        } else {
            if (hostPart.contains("{_}")) {
                java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("([-_a-z0-9A-Z]+([.][-_a-z0-9A-Z]+)?)$").matcher(domain);
                if (matcher.find()) {
                    hostPart = hostPart.replace("{_}", matcher.group(1));
                }
            }
        }
        try {
            return new URI(
                Http.Request.current() == null ?
                    Play.configuration.getProperty(
                        "application.baseUrl", "application.baseUrl"):
                    Http.Request.current().getBase())
                .resolve(new URI(
                    scheme, null, hostPart, port, uri.getPath(),
                    uri.getQuery(), uri.getFragment()));
        } catch (URISyntaxException e) {
            throw new UnexpectedException(e);
        }
    }

    public static URI securize(URI uri) {
        if (!uri.isAbsolute())
            uri = absolutize(uri, false);
        try {
            return new URI("https", uri.getUserInfo(), uri.getHost(), uri.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment());
        } catch (URISyntaxException e) {
            throw new UnexpectedException(e);
        }
    }


    public String getFullUrl(String action, Http.Verb method, Map<String, Object> args) {
        return absolutize(reverse(action, method, args).getUri()).toString();
    }

    public String getFullUrl(String action, Http.Verb method) {
        // Note the map is not <code>Collections.EMPTY_MAP</code> because it will be copied and changed.
        return getFullUrl(action, method, new HashMap<String, Object>(16));
    }

    public String getFullUrl(String action, Map<String, Object> args) {
        // Note the map is not <code>Collections.EMPTY_MAP</code> because it will be copied and changed.
        return getFullUrl(action, Http.Verb.GET, args);
    }

    public String getFullUrl(String action) {
        // Note the map is not <code>Collections.EMPTY_MAP</code> because it will be copied and changed.
        return getFullUrl(action, Http.Verb.GET);
    }

    /**
     * Add a route at the given position
     */
    public void addRoute(int position, EnumSet<Http.Verb> methods, String path, String headers) {
        addRoute(position, methods, path, null, null, headers);
    }

    /**
     * Add a route at the given position
     */
    public void addRoute(int position, String methods, String path, String headers) {
        addRoute(position, toVerbSet(methods), path, headers);
    }

    /**
     * Add a route at the given position
     */
    public void addRoute(int position, EnumSet<Http.Verb> methods, String path, String action, String headers) {
        addRoute(position, methods, path, action, null, headers);
    }

    /**
     * Add a route at the given position
     */
    public void addRoute(int position, String methods, String path, String action, String headers) {
        addRoute(position, toVerbSet(methods), path, action, headers);
    }

    /**
     * Add a new route. Will be first in the route list
     */
    public void addRoute(EnumSet<Http.Verb> methods, String path, String action) {
        addRoute(methods, path, action, null, null);
    }

    /**
     * Add a new route. Will be first in the route list
     */
    public void addRoute(String methods, String path, String action) {
        addRoute(toVerbSet(methods), path, action);
    }

    /**
     * Add a route at the given position
     */
    public void addRoute(EnumSet<Http.Verb> methods, String path, String action, String headers) {
        addRoute(methods, path, action, null, headers);
    }

    /**
     * Add a route at the given position
     */
    public void addRoute(String methods, String path, String action, String headers) {
        addRoute(toVerbSet(methods), path, action, headers);
    }

    /**
     * Add a new route at the beginning of the route list
     */
    public void prependRoute(EnumSet<Http.Verb> methods, String path, String action, String params, String headers) {
        addRoute(0, methods, path, action, params, headers);
    }

    /**
     * Add a new route at the beginning of the route list
     */
    public void prependRoute(String methods, String path, String action, String params, String headers) {
        prependRoute(toVerbSet(methods), path, action, params, headers);
    }

    /**
     * This one can be called to add new route. Last added is first in the route list.
     */
    public void prependRoute(EnumSet<Http.Verb> methods, String path, String action, String headers) {
        prependRoute(methods, path, action, null, headers);
    }

    /**
     * This one can be called to add new route. Last added is first in the route list.
     */
    public void prependRoute(String methods, String path, String action, String headers) {
        prependRoute(toVerbSet(methods), path, action, headers);
    }

    /**
     * This one can be called to add new route. Last added is first in the route list.
     */
    public void prependRoute(EnumSet<Http.Verb> methods, String path, String action) {
        prependRoute(methods, path, action, null, null);
    }

    /**
     * This one can be called to add new route. Last added is first in the route list.
     */
    public void prependRoute(String methods, String path, String action) {
        prependRoute(toVerbSet(methods), path, action, null, null);
    }

    public static EnumSet<Http.Verb> toVerbSet(String methods) {
        if (methods.equals("*"))
            return EnumSet.allOf(Http.Verb.class);
        EnumSet<Http.Verb> retval = EnumSet.noneOf(Http.Verb.class);
        for (String method: methods.split("\\s*,\\s*")) {
            Http.Verb verb = Http.Verb.valueOf(method);
            if (verb == null)
                throw new IllegalArgumentException("Unsupported HTTP verb: " + method);
            retval.add(verb);
        }
        return retval;
    }

    public static ThreadLocal<Router> current = new ThreadLocal<Router>();
}
