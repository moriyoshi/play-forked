package play.mvc;

import java.util.Map;
import java.util.HashMap;
import java.net.URI;
import java.net.URISyntaxException;
import play.vfs.VirtualFile;
import play.exceptions.UnexpectedException;
import play.exceptions.NoRouteFoundException;
import play.Play;

public abstract class Router {
    public abstract Iterable<Route> getRoutes();

    public abstract Route getRoute(String method, String path, String action, String params, String headers, String sourceFile, int line);

    public abstract Map<String, String> route(String method, String path, String headers, String host);

    public abstract Route route(Http.Request request);

    public abstract void routeOnlyStatic(Http.Request request);

    public abstract String reverse(VirtualFile file, boolean absolute);

    public abstract AbstractActionDefinition reverse(String action, Map<String, Object> args);

    /**
     * Add a route at the given position
     */
    public abstract void addRoute(int position, String method, String path, String action, String params, String headers);

    public abstract void addRoute(String method, String path, String action, String params, String headers);

    /**
     * Add a new route at the beginning of the route list
     */
    public abstract void prependRoute(String method, String path, String action, String params, String headers);

    public Route getRoute(String method, String path, String action, String params, String headers) {
        return getRoute(method, path, action, params, headers, null, 0);
    }

    public Map<String, String> route(String method, String path) {
        return route(method, path, null, null);
    }

    public Map<String, String> route(String method, String path, String headers) {
        return route(method, path, headers, null);
    }

    public String reverse(VirtualFile file) {
        return reverse(file, false);
    }

    public AbstractActionDefinition reverse(String action) {
        // Note the map is not <code>Collections.EMPTY_MAP</code> because it will be copied and changed.
        return reverse(action, new HashMap<String, Object>());
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


    public String getFullUrl(String action, Map<String, Object> args) {
        return absolutize(reverse(action, args).getUri()).toString();
    }

    public String getFullUrl(String action) {
        // Note the map is not <code>Collections.EMPTY_MAP</code> because it will be copied and changed.
        return getFullUrl(action, new HashMap<String, Object>(16));
    }

    /**
     * Add a route at the given position
     */
    public void addRoute(int position, String method, String path, String headers) {
        addRoute(position, method, path, null, null, headers);
    }

    /**
     * Add a route at the given position
     */
    public void addRoute(int position, String method, String path, String action, String headers) {
        addRoute(position, method, path, action, null, headers);
    }

    /**
     * Add a new route. Will be first in the route list
     */
    public void addRoute(String method, String path, String action) {
        addRoute(method, path, action, null, null);
    }

    /**
     * Add a route at the given position
     */
    public void addRoute(String method, String path, String action, String headers) {
        addRoute(method, path, action, null, headers);
    }

    /**
     * This one can be called to add new route. Last added is first in the route list.
     */
    public void prependRoute(String method, String path, String action, String headers) {
        prependRoute(method, path, action, null, headers);
    }

    /**
     * This one can be called to add new route. Last added is first in the route list.
     */
    public void prependRoute(String method, String path, String action) {
        prependRoute(method, path, action, null, null);
    }

    public static ThreadLocal<Router> current = new ThreadLocal<Router>();
}
