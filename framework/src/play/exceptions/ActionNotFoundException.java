package play.exceptions;

import play.mvc.ActionInvoker;

/**
 * Missing action
 */
public class ActionNotFoundException extends PlayException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private String action;
   
    public ActionNotFoundException(String action, Throwable cause) {
        super(String.format("Action %s not found", ActionInvoker.getActionName(action), cause));
        this.action = ActionInvoker.getActionName(action);
    }

    public String getAction() {
        return action;
    }

    @Override
    public String getErrorTitle() {
        return String.format("Action not found");
    }

    @Override
    public String getErrorDescription() {
        return String.format(
                "Action <strong>%s</strong> could not be found. Error raised is <strong>%s</strong>", 
                action, 
                getCause() instanceof ClassNotFoundException ? "ClassNotFound: "+getCause().getMessage() : getCause().getMessage()
        );
    }
}
