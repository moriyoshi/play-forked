package play.mvc.results;


import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.Response;

/**
 * 200 OK
 */
public class Ok extends Result {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public Ok() {
        super("OK");
    }

    public void apply(Request request, Response response) {
        response.status = Http.StatusCode.OK;
    }
}
