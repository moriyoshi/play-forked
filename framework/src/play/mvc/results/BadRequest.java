package play.mvc.results;

import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.Response;

/**
 * 400 Bad Request
 */
public class BadRequest extends Result {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public void apply(Request request, Response response) {
        response.status = Http.StatusCode.BAD_REQUEST;
    }

}
