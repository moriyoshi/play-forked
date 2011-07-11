package play.mvc;

import java.net.URI;
import java.util.Map;

public abstract class AbstractActionDefinition {
    /**
     * The router
     */
    public abstract Router getRouter();

    /**
     * Corresponding route
     */
    public abstract Route getRoute();

    /**
     * The HTTP method, e.g. "GET".
     */
    public abstract String getMethod();

    /**
     * Whether the route contains an astericks *.
     */
    public abstract boolean isContainingStar();

    /**
     * @todo - what is this? does it include the class and package?
     */
    public abstract String getAction();

    /**
     * @todo - are these the required args in the routing file, or the query string in a request?
     */
    public abstract Map<String, Object> getArgs();

    public abstract URI getUri(String encoding);

    public URI getUri() {
        return getUri(Http.Response.current() == null ? "utf-8" : Http.Response.current().encoding);
    }

    public URI absolute() {
        return Router.absolutize(getUri());
    }

    public URI secure() {
        return Router.securize(Router.absolutize(getUri()));
    }
}

