package play.mvc;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.URISyntaxException;

import play.utils.Default;
import play.exceptions.UnexpectedException;

public class ActionDefinition extends AbstractActionDefinition {
    final Router router;
    final Route route;
    final String action;
    final Map<String, Object> args;
    URI uriCache;

    public Router getRouter() {
        return router;
    }

    public Route getRoute() {
        return route;
    }

    public String getAction() {
        return action;
    }

    public Map<String, Object> getArgs() {
        return args;
    }

    public URI getUri(String encoding) {
        if (uriCache != null)
            return uriCache;
        StringBuilder queryString = new StringBuilder();
        String path = route.getPath();
        String host = route.getHost() != null ? route.getHost().replaceAll("\\{", "").replaceAll("\\}", ""): null;
        if (path.endsWith("/?")) {
            path = path.substring(0, path.length() - 2);
        }

        List<String> inPathArgs = new ArrayList<String>(16);
        // les noms de parametres matchent ils ?
        for (Route.Arg arg : route.getArgs()) {
            inPathArgs.add(arg.name);
            Object value = args.get(arg.name);
            if (value == null) {
                // This is a hack for reverting on hostname that are a regex expression.
                // See [#344] for more into. This is not optimal and should retough. However,
                // it allows us to do things like {(.*}}.domain.com
                if (host != null && (host.equals(arg.name) || host.matches(arg.name))) {
                    args.remove(arg.name);
                    host = Http.Request.current() == null ? "" : Http.Request.current().domain;
                    break;
                }
            } else {
                if (value instanceof List<?>) {
                    @SuppressWarnings("unchecked")
                    List<Object> l = (List<Object>) value;
                    value = l.get(0);
                }
                if (!value.toString().startsWith(":") && !arg.constraint.matches(value.toString())) {
                    return null;
                }
            }
        }

        for (Map.Entry<String, Object> entry : args.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (inPathArgs.contains(key) && value != null) {
                if (List.class.isAssignableFrom(value.getClass())) {
                    @SuppressWarnings("unchecked")
                    List<Object> vals = (List<Object>) value;
                    try {
                        path = path.replaceAll("\\{(<[^>]+>)?" + key + "\\}", URLEncoder.encode(vals.get(0).toString().replace("$", "\\$"), encoding));
                    } catch (UnsupportedEncodingException e) {
                        throw new UnexpectedException(e);
                    }
                } else {
                    try {
                        path = path.replaceAll("\\{(<[^>]+>)?" + key + "\\}", URLEncoder.encode(value.toString().replace("$", "\\$"), encoding).replace("%3A", ":").replace("%40", "@"));
                        if (host != null)
                            host = host.replaceAll("\\{(<[^>]+>)?" + key + "\\}", URLEncoder.encode(value.toString().replace("$", "\\$"), encoding).replace("%3A", ":").replace("%40", "@"));
                    } catch (UnsupportedEncodingException e) {
                        throw new UnexpectedException(e);
                    }
                }
            } else if (route.getStaticArgs().containsKey(key)) {
                // Do nothing -> The key is static
            } else if (Scope.RouteArgs.current() != null && Scope.RouteArgs.current().data.containsKey(key)) {
                // Do nothing -> The key is provided in RouteArgs and not used (see #447)
            } else if (value != null) {
                if (List.class.isAssignableFrom(value.getClass())) {
                    @SuppressWarnings("unchecked")
                    List<Object> vals = (List<Object>) value;
                    for (Object object : vals) {
                        try {
                            queryString.append(URLEncoder.encode(key, encoding));
                            queryString.append("=");
                            if (object.toString().startsWith(":")) {
                                queryString.append(object.toString());
                            } else {
                                queryString.append(URLEncoder.encode(object.toString() + "", encoding));
                            }
                            queryString.append("&");
                        } catch (UnsupportedEncodingException ex) {
                        }
                    }
                } else if (value.getClass().equals(Default.class)) {
                    // Skip defaults in queryString
                } else {
                    try {
                        queryString.append(URLEncoder.encode(key, encoding));
                        queryString.append("=");
                        if (value.toString().startsWith(":")) {
                            queryString.append(value.toString());
                        } else {
                            queryString.append(URLEncoder.encode(value.toString() + "", encoding));
                        }
                        queryString.append("&");
                    } catch (UnsupportedEncodingException ex) {
                    }
                }
            }
        }
        String qs = queryString.toString();
        if (qs.endsWith("&")) {
            qs = qs.substring(0, qs.length() - 1);
        }
        try {
            uriCache = new URI(null, host, path, qs, null);
            return uriCache;
        } catch (URISyntaxException e) {
            throw new UnexpectedException(e);
        }
    }

    @Override
    public String toString() {
        return getUri().toString();
    }

    public ActionDefinition(Router router, Route route, String action, Map<String, Object> args) {
        this.router = router;
        this.route = route;
        this.action = action;
        this.args = args;
    }

    public ActionDefinition() {
        this.router = null;
        this.route = null;
        this.action = null;
        this.args = null;
    }
}

