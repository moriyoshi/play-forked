package play.exceptions;

import java.util.Map;
import java.util.List;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.templates.Template;
import java.util.Arrays;

/**
 * Multiple routes found (during reverse routing)
 */
public class MultipleRoutesFoundException extends PlayException implements SourceAttachment {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    String action;
    Map<String, Object> args;
    String sourceFile;
    List<String> source;
    Integer line;

    public MultipleRoutesFoundException(String action, Map<String, Object> args) {
        super("Multiple routes found");
        this.action = action;
        this.args = args;
        if(this.action.startsWith("controllers.")) {
            this.action = this.action.substring(12);
        }
    } 
    
    public MultipleRoutesFoundException(String action, Map<String, Object> args, ApplicationClass<?> applicationClass, Integer line) {
        this(action, args);
        this.sourceFile = applicationClass.javaFile.relativePath();
        this.source = Arrays.asList(applicationClass.javaSource.split("\n"));
        this.line = line;
    }
    
    public MultipleRoutesFoundException(String action, Map<String, Object> args, Template template, Integer line) {
        this(action, args);
        this.sourceFile = template.name;
        this.source = Arrays.asList(template.source.split("\n"));
        this.line = line;
    }

    public String getAction() {
        return action;
    }

    public Map<String, Object> getArgs() {
        return args;
    }    

    @Override
    public String getErrorTitle() {
        return "Multiple routes found";
    }

    @Override
    public String getErrorDescription() {
        if(args == null) {
            return String.format("Multiple routes for invoking action <strong>%s</strong> were found.", action);
        }
        return String.format("Multiple routes for invoking action <strong>%s</strong> with arguments <strong>%s</strong> were found.", action, args);
    }
    
    @Override
    public boolean isSourceAvailable() {
        return source != null;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public List<String> getSource() {
        return source;
    }

    public Integer getLineNumber() {
        return line;
    }

}
