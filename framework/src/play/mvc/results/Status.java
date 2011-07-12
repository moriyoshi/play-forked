package play.mvc.results;

import play.mvc.Http.Request;
import play.mvc.Http.Response;

public class Status extends Result {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    int code;

    public Status(int code) {
        super(code+"");
        this.code = code;
    }

    public void apply(Request request, Response response) {
        response.status = code;
    }
}
