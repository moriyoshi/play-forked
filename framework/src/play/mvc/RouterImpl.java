package play.mvc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.net.URI;
import java.net.URISyntaxException;

import jregex.Matcher;
import jregex.Pattern;
import jregex.REFlags;
import play.Logger;
import play.Play;
import play.vfs.VirtualFile;
import play.exceptions.NoRouteFoundException;
import play.exceptions.UnexpectedException;
import play.mvc.results.NotFound;
import play.mvc.results.RenderStatic;
import play.templates.TemplateLoader;

/**
 * The router matches HTTP requests to action invocations
 */
public class RouterImpl extends Router {

    static Pattern routePattern = new Pattern("^({method}GET|POST|PUT|DELETE|OPTIONS|HEAD|WS|\\*)[(]?({headers}[^)]*)(\\))?\\s+({path}.*/[^\\s]*)\\s+({action}[^\\s(]+)({params}.+)?(\\s*)$");
    /**
     * Pattern used to locate a method override instruction in request.querystring
     */
    static Pattern methodOverride = new Pattern("^.*x-http-method-override=({method}GET|PUT|POST|DELETE).*$");

    /**
     * Add a route
     */
    public void addRoute(String method, String path, String action, String params, String headers) {
        appendRoute(method, path, action, params, headers, null, 0);
    }

    public void addRoute(int position, String method, String path, String action, String params, String headers) {
        if (position > routes.size()) {
            position = routes.size();
        }
        routes.add(position, (RouteImpl)getRoute(method, path, action, params, headers));
    }

    /**
     * This is used internally when reading the route file. The order the routes are added matters and
     * we want the method to append the routes to the list.
     */
    void appendRoute(String method, String path, String action, String params, String headers, String sourceFile, int line) {
        routes.add((RouteImpl)getRoute(method, path, action, params, headers, sourceFile, line));
    }

    public Route getRoute(String method, String path, String action, String params, String headers, String sourceFile, int line) {
        RouteImpl route = new RouteImpl(this);
        route.method = method;
        route.path = path.replace("//", "/");
        route.action = action;
        route.routesFile = sourceFile;
        route.routesFileLine = line;
        route.addFormat(headers);
        route.addParams(params);
        route.compute();
        if (Logger.isTraceEnabled()) {
            Logger.trace("Adding [" + route.toString() + "] with params [" + params + "] and headers [" + headers + "]");
        }
        return route;
    }

    public void prependRoute(String method, String path, String action, String params, String headers) {
        routes.add(0, (RouteImpl)getRoute(method, path, action, params, headers));
    }

    /**
     * Parse a route file.
     * If an action starts with <i>"plugin:name"</i>, replace that route by the ones declared
     * in the plugin route file denoted by that <i>name</i>, if found.
     *
     * @param routeFile
     * @param prefix    The prefix that the path of all routes in this route file start with. This prefix should not
     *                  end with a '/' character.
     */
    void parse(VirtualFile routeFile, String prefix) {
        String fileAbsolutePath = routeFile.getRealFile().getAbsolutePath();
        String content = routeFile.contentAsString();
        if (content.indexOf("${") > -1 || content.indexOf("#{") > -1 || content.indexOf("%{") > -1) {
            // Mutable map needs to be passed in.
            content = TemplateLoader.load(routeFile).render(new HashMap<String, Object>(16));
        }
        parse(content, prefix, fileAbsolutePath);
    }

    void parse(String content, String prefix, String fileAbsolutePath) {
        int lineNumber = 0;
        for (String line : content.split("\n")) {
            lineNumber++;
            line = line.trim().replaceAll("\\s+", " ");
            if (line.length() == 0 || line.startsWith("#")) {
                continue;
            }
            Matcher matcher = routePattern.matcher(line);
            if (matcher.matches()) {
                String action = matcher.group("action");
                // module:
                if (action.startsWith("module:")) {
                    String moduleName = action.substring("module:".length());
                    String newPrefix = prefix + matcher.group("path");
                    if (newPrefix.length() > 1 && newPrefix.endsWith("/")) {
                        newPrefix = newPrefix.substring(0, newPrefix.length() - 1);
                    }
                    if (moduleName.equals("*")) {
                        for (String p : Play.modulesRoutes.keySet()) {
                            parse(Play.modulesRoutes.get(p), newPrefix + p);
                        }
                    } else if (Play.modulesRoutes.containsKey(moduleName)) {
                        parse(Play.modulesRoutes.get(moduleName), newPrefix);
                    } else {
                        Logger.error("Cannot include routes for module %s (not found)", moduleName);
                    }
                } else {
                    String method = matcher.group("method");
                    String path = prefix + matcher.group("path");
                    String params = matcher.group("params");
                    String headers = matcher.group("headers");
                    appendRoute(method, path, action, params, headers, fileAbsolutePath, lineNumber);
                }
            } else {
                Logger.error("Invalid route definition : %s", line);
            }
        }
    }

    /**
     * All the loaded routes.
     */
    List<RouteImpl> routes = new ArrayList<RouteImpl>(500);

    @SuppressWarnings("unchecked")
    public Iterable<Route> getRoutes() {
        return (Iterable<Route>)(List<?>)routes;
    }

    public void routeOnlyStatic(Http.Request request) {
        for (RouteImpl route : routes) {
            try {
                String format = request.format;
                String host = request.host;
                if (route.matches(request.method, request.path, format, host) != null) {
                    break;
                }
            } catch (Throwable t) {
                if (t instanceof RenderStatic) {
                    throw (RenderStatic) t;
                }
                if (t instanceof NotFound) {
                    throw (NotFound) t;
                }
            }
        }
    }

    public Route route(Http.Request request) {
        if (Logger.isTraceEnabled()) {
            Logger.trace("Route: " + request.path + " - " + request.querystring);
        }
        // request method may be overriden if a x-http-method-override parameter is given
        if (request.querystring != null && methodOverride.matches(request.querystring)) {
            Matcher matcher = methodOverride.matcher(request.querystring);
            if (matcher.matches()) {
                if (Logger.isTraceEnabled()) {
                    Logger.trace("request method %s overriden to %s ", request.method, matcher.group("method"));
                }
                request.method = matcher.group("method");
            }
        }
        for (RouteImpl route : routes) {
            String format = request.format;
            String host = request.host;
            Map<String, String> args = route.matches(request.method, request.path, format, host);
            if (args != null) {
                request.routeArgs = args;
                request.action = route.getAction();
                if (args.containsKey("format")) {
                    request.format = args.get("format");
                }
                if (request.action.indexOf("{") > -1) { // more optimization ?
                    for (String arg : request.routeArgs.keySet()) {
                        request.action = request.action.replace("{" + arg + "}", request.routeArgs.get(arg));
                    }
                }
                if (request.action.equals("404")) {
                    throw new NotFound(route.getPath());
                }
                return route;
            }
        }
        // Not found - if the request was a HEAD, let's see if we can find a corresponding GET
        if (request.method.equalsIgnoreCase("head")) {
            request.method = "GET";
            Route route = route(request);
            request.method = "HEAD";
            if (route != null) {
                return route;
            }
        }
        throw new NotFound(request.method, request.path);
    }

    public Map<String, String> route(String method, String path, String headers, String host) {
        for (RouteImpl route : routes) {
            Map<String, String> args = route.matches(method, path, headers, host);
            if (args != null) {
                args.put("action", route.getAction());
                return args;
            }
        }
        return new HashMap<String, String>(16);
    }

    public String reverse(VirtualFile file, boolean absolute) {
        if (file == null || !file.exists()) {
            throw new NoRouteFoundException("File not found (" + file + ")");
        }
        String path = file.relativePath();
        path = path.substring(path.indexOf("}") + 1);
        for (RouteImpl route : routes) {
            String staticDir = route.getStaticDir();
            if (staticDir != null) {
                if (!staticDir.startsWith("/")) {
                    staticDir = "/" + staticDir;
                }
                if (!staticDir.equals("/") && !staticDir.endsWith("/")) {
                    staticDir = staticDir + "/";
                }
                if (path.startsWith(staticDir)) {
                    String to = route.getPath() + path.substring(staticDir.length());
                    if (to.endsWith("/index.html")) {
                        to = to.substring(0, to.length() - "/index.html".length() + 1);
                    }
                    if (absolute) {
                        try {
                            to = absolutize(new URI(null, route.getHost(), to, null, null)).toString();
                        } catch (URISyntaxException e) {
                            throw new UnexpectedException(e);
                        }
                    }
                    return to;
                }
            }
        }
        throw new NoRouteFoundException(file.relativePath());
    }

    public ActionDefinition reverse(String action, Map<String, Object> args) {
        if (action.startsWith("controllers.")) {
            action = action.substring(12);
        }
        // Add routeArgs
        if (Scope.RouteArgs.current() != null) {
            for (String key : Scope.RouteArgs.current().data.keySet()) {
                if (!args.containsKey(key)) {
                    args.put(key, Scope.RouteArgs.current().data.get(key));
                }
            }
        }
        for (RouteImpl route : routes) {
            ActionDefinition actionDef = route.reverse(action, args);
            if (actionDef != null) {
                return actionDef;
            }
        }
        throw new NoRouteFoundException(action, args);
    }

    public RouterImpl(VirtualFile routes, String prefix) {
        parse(routes, prefix);
    }

    public static class RouteImpl extends Route {
        RouterImpl router;
        /**
         * HTTP method, e.g. "GET".
         */
        String method;
        String path;

        /**
         * @todo - what is this?
         */
        String action;
        Pattern actionPattern;
        List<String> actionArgs = new ArrayList<String>(3);
        String staticDir;
        String staticFile;
        Pattern pattern;
        Pattern hostPattern;
        List<Arg> args = new ArrayList<Arg>(3);
        Map<String, String> staticArgs = new HashMap<String, String>(3);
        List<String> formats = new ArrayList<String>(1);
        String host;
        Arg hostArg = null;
        int routesFileLine;
        String routesFile;

        static Pattern customRegexPattern = new Pattern("\\{([a-zA-Z_][a-zA-Z_0-9]*)\\}");
        static Pattern argsPattern = new Pattern("\\{<([^>]+)>([a-zA-Z_0-9]+)\\}");
        static Pattern paramPattern = new Pattern("([a-zA-Z_0-9]+):'(.*)'");

        public String getMethod() {
            return method;
        }

        public String getPath() {
            return path;
        }

        public String getAction() {
            return action;
        }

        public Pattern getActionPattern() {
            return actionPattern;
        }

        public List<String> getActionArgs() {
            return actionArgs;
        }

        public String getStaticDir() {
            return staticDir;
        }

        public String getStaticFile() {
            return staticFile;
        }

        public Pattern getPattern() {
            return pattern;
        }

        public Pattern getHostPattern() {
            return hostPattern;
        }
 
        public List<Arg> getArgs() {
            return args;
        }

        public Map<String, String> getStaticArgs() {
            return staticArgs;
        }

        public List<String> getFormats() {
            return formats;
        }

        public String getHost() {
            return host;
        }

        public Arg getHostArg() {
            return hostArg;
        }

        public String getRoutesFile() {
            return routesFile;
        }

        void compute() {
            this.host = null;
            this.hostPattern = new Pattern(".*");
            final boolean actionIsStaticDir = action.startsWith("staticDir:");
            final boolean actionIsStaticFile = action.startsWith("staticFile:");
            if (actionIsStaticDir || actionIsStaticFile) {
                // Is there is a host argument, append it.
                if (!path.startsWith("/")) {
                    String p = this.path;
                    this.path = p.substring(p.indexOf("/"));
                    this.host = p.substring(0, p.indexOf("/"));
                    if (this.host.contains("{")) {
                        Logger.warn("Static route cannot have a dynamic host name");
                        return;
                    }
                }
                if (!method.equalsIgnoreCase("*") && !method.equalsIgnoreCase("GET")) {
                    Logger.warn("Static route only support GET method");
                    return;
                }
                // staticDir
                if (actionIsStaticDir) {
                    if (!this.path.endsWith("/") && !this.path.equals("/")) {
                        Logger.warn("The path for a staticDir route must end with / (%s)", this);
                        this.path += "/";
                    }
                    this.pattern = new Pattern("^" + path + "({resource}.*)$");
                    this.staticDir = action.substring("staticDir:".length());
                } else if (actionIsStaticFile) {
                    this.pattern = new Pattern("^" + path + "$");
                    this.staticFile = action.substring("staticFile:".length());
                }
            } else {
                // URL pattern
                // Is there is a host argument, append it.
                if (!path.startsWith("/")) {
                    String p = this.path;
                    this.path = p.substring(p.indexOf("/"));
                    this.host = p.substring(0, p.indexOf("/"));
                    String pattern = host.replaceAll("\\.", "\\\\.").replaceAll("\\{.*\\}", "(.*)");

                    if (Logger.isTraceEnabled()) {
                        Logger.trace("pattern [" + pattern + "]");
                        Logger.trace("host [" + host + "]");
                    }

                    Matcher m = new Pattern(pattern).matcher(host);
                    this.hostPattern = new Pattern(pattern);

                    if (m.matches()) {
                        if (this.host.contains("{")) {
                            String name = m.group(1).replace("{", "").replace("}", "");
                            if (!name.equals("_")) {
                                hostArg = new Arg();
                                hostArg.name = name;
                                if (Logger.isTraceEnabled()) {
                                    Logger.trace("hostArg name [" + name + "]");
                                }
                                // The default value contains the route version of the host ie {client}.bla.com
                                // It is temporary and it indicates it is an url route.
                                // TODO Check that default value is actually used for other cases.
                                hostArg.defaultValue = host;
                                hostArg.constraint = new Pattern(".*");

                                if (Logger.isTraceEnabled()) {
                                    Logger.trace("adding hostArg [" + hostArg + "]");
                                }

                                args.add(hostArg);
                            }
                        }
                    }


                }
                String patternString = path;
                patternString = customRegexPattern.replacer("\\{<[^/]+>$1\\}").replace(patternString);
                Matcher matcher = argsPattern.matcher(patternString);
                while (matcher.find()) {
                    Arg arg = new Arg();
                    arg.name = matcher.group(2);
                    arg.constraint = new Pattern(matcher.group(1));
                    args.add(arg);
                }

                patternString = argsPattern.replacer("({$2}$1)").replace(patternString);
                this.pattern = new Pattern(patternString);
                // Action pattern
                patternString = action;
                patternString = patternString.replace(".", "[.]");
                for (Arg arg : args) {
                    if (patternString.contains("{" + arg.name + "}")) {
                        patternString = patternString.replace("{" + arg.name + "}", "({" + arg.name + "}" + arg.constraint.toString() + ")");
                        actionArgs.add(arg.name);
                    }
                }
                actionPattern = new Pattern(patternString, REFlags.IGNORE_CASE);
            }
        }

        void addParams(String params) {
            if (params == null || params.length() < 1) {
                return;
            }
            params = params.substring(1, params.length() - 1);
            for (String param : params.split(",")) {
                Matcher matcher = paramPattern.matcher(param);
                if (matcher.matches()) {
                    staticArgs.put(matcher.group(1), matcher.group(2));
                } else {
                    Logger.warn("Ignoring %s (static params must be specified as key:'value',...)", params);
                }
            }
        }

        // TODO: Add args names
        void addFormat(String params) {
            if (params == null || params.length() < 1) {
                return;
            }
            params = params.trim();
            formats.addAll(Arrays.asList(params.split(",")));
        }

        private boolean contains(String accept) {
            boolean contains = (accept == null);
            if (accept != null) {
                if (this.formats.isEmpty()) {
                    return true;
                }
                for (String format : this.formats) {
                    contains = format.startsWith(accept);
                    if (contains) {
                        break;
                    }
                }
            }
            return contains;
        }

        /**
         * Check if the parts of a HTTP request equal this Route.
         *
         * @param method GET/POST/etc.
         * @param path   Part after domain and before query-string. Starts with a "/".
         * @param accept Format, e.g. html.
         * @param host   AKA the domain.
         * @return ???
         */
        public Map<String, String> matches(String method, String path, String accept, String host) {
            // Normalize
            if (path.equals(Play.ctxPath)) {
                path = path + "/";
            }
            // If method is HEAD and we have a GET
            if (method == null || this.method.equals("*") || method.equalsIgnoreCase(this.method) || (method.equalsIgnoreCase("head") && ("get").equalsIgnoreCase(this.method))) {

                Matcher matcher = pattern.matcher(path);

                boolean hostMatches = (host == null);
                if (host != null) {

                    Matcher hostMatcher = hostPattern.matcher(host);
                    hostMatches = hostMatcher.matches();
                }
                // Extract the host variable
                if (matcher.matches() && contains(accept) && hostMatches) {
                    // 404
                    if (action.equals("404")) {
                        throw new NotFound(method, path);
                    }
                    // Static dir
                    if (staticDir != null) {
                        String resource = null;
                        try {
                            if (staticFile != null) {
                                throw new RenderStatic(staticFile);
                            } else {
                                resource = matcher.group("resource");
                                String root = new File(staticDir).getCanonicalPath();
                                String childResourceName = staticFile != null ? staticFile: staticDir + "/" + resource;
                                String child = new File(childResourceName).getCanonicalPath();
                                if (child.startsWith(root)) {
                                    throw new RenderStatic(childResourceName);
                                }
                            }
                        } catch (IOException e) {
                            Logger.warn("Inaccessible resource", e);
                        }
                        throw new NotFound(resource);
                    } else {
                        Map<String, String> localArgs = new HashMap<String, String>();
                        for (Arg arg : args) {
                            // FIXME: Careful with the arguments that are not matching as they are part of the hostname
                            // Defaultvalue indicates it is a one of these urls. This is a trick and should be changed.
                            if (arg.defaultValue == null) {
                                localArgs.put(arg.name, matcher.group(arg.name));
                            }
                        }
                        if (hostArg != null && host != null) {
                            // Parse the hostname and get only the part we are interested in
                            String routeValue = hostArg.defaultValue.replaceAll("\\{.*}", "");
                            host = host.replace(routeValue, "");
                            localArgs.put(hostArg.name, host);
                        }
                        localArgs.putAll(staticArgs);
                        return localArgs;
                    }
                }
            }
            return null;
        }

        public Map<String, String> matches(String method, String path) {
            return matches(method, path, null, null);
        }

        public Map<String, String> matches(String method, String path, String accept) {
            return matches(method, path, accept, null);
        }

        public ActionDefinition reverse(String action, Map<String, Object> args) {
            if (actionPattern == null)
                return null;

            final Matcher matcher = actionPattern.matcher(action);
            if (!matcher.matches())
                return null;

            final Map<String, Object> _args = new HashMap<String, Object>(args);
            for (String group : actionArgs) {
                String v = matcher.group(group);
                if (v == null) {
                    continue;
                }
                _args.put(group, v.toLowerCase());
            }

            // les parametres codes en dur dans la route matchent-ils ?
            for (String staticKey : staticArgs.keySet()) {
                if (staticKey.equals("format")) {
                    if (!(Http.Request.current() == null ? "" : Http.Request.current().format).equals(staticArgs.get("format"))) {
                        return null;
                    }
                    continue; // format is a special key
                }
                if (!_args.containsKey(staticKey) ||
                        (_args.get(staticKey) == null) ||
                        !_args.get(staticKey).toString().equals(
                            staticArgs.get(staticKey))) {
                    return null;
                }
            }
            return new ActionDefinition(router, this, action, _args);
        }

        @Override
        public String toString() {
            return method + " " + path + " -> " + action;
        }

        public RouteImpl(RouterImpl router) {
            this.router = router;
        }
    }
}
