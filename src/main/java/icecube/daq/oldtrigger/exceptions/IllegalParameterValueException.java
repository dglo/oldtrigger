/*
 * class: TimeOutOfOrderException
 *
 * Version $Id: IllegalParameterValueException.java 14204 2013-02-11 19:52:57Z dglo $
 *
 * Date: March 31 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.oldtrigger.exceptions;


/**
 * This class provides a specific exception
 *
 * @version $Id: IllegalParameterValueException.java 14204 2013-02-11 19:52:57Z dglo $
 * @author pat
 */
public class IllegalParameterValueException
        extends TriggerException
{

    /**
     * default constructor
     */
    IllegalParameterValueException() {
    }

    /**
     * constructor taking a message
     * @param message message associated with this exception
     */
    public IllegalParameterValueException(String message) {
        super(message);
    }

    /**
     * constructor taking an exception
     * @param exception the exception
     */
    public IllegalParameterValueException(Exception exception) {
        super(exception);
    }

    /**
     * constructor taking a message and an exception
     * @param message message associated with this exception
     * @param exception the exception
     */
    public IllegalParameterValueException(String message, Exception exception) {
        super(message, exception);
    }

}