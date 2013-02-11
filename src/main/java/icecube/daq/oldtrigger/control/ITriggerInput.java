/*
 * interface: ITriggerInput
 *
 * Version $Id: ITriggerInput.java 14204 2013-02-11 19:52:57Z dglo $
 *
 * Date: May 2 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.oldtrigger.control;

import icecube.daq.payload.ILoadablePayload;

/**
 * This interface defines the functionality of a trigger input module
 *
 * @version $Id: ITriggerInput.java 14204 2013-02-11 19:52:57Z dglo $
 * @author pat
 */
public interface ITriggerInput
{

    void addPayload(ILoadablePayload payload);

    void flush();

    boolean hasNext();

    ILoadablePayload next();

    int size();

}
