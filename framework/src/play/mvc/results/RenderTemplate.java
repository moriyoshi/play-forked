package play.mvc.results;

import java.util.Map;

import play.exceptions.UnexpectedException;
import play.libs.MimeTypes;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.templates.Template;

/**
 * 200 OK with a template rendering
 */
public class RenderTemplate extends Result {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private final String name;
    private final String content;
    private final String contentType;

    public RenderTemplate(Template template, Map<String, Object> args) {
        this(template, args, MimeTypes.getContentType(template.name, "text/plain"));
    }

    public RenderTemplate(Template template, Map<String, Object> args, String contentType) {
        this.name = template.name;
        if (args.containsKey("out")) {
            throw new RuntimeException("Assertion failed! args shouldn't contain out");
        }
        this.content = template.render(args);
        this.contentType = contentType;
    }

    public void apply(Request request, Response response) {
        try {
            response.out.write(content.getBytes(getEncoding()));
            setContentTypeIfNotSet(response, contentType);
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    public String getName() {
        return name;
    }

    public String getContentType() {
        return contentType;
    }

    public String getContent() {
        return content;
    }
}
