/*
 * class: ITriggerBag
 *
 * Version $Id: ITriggerBag.java 14204 2013-02-11 19:52:57Z dglo $
 *
 * Date: March 16 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.oldtrigger.control;

import icecube.daq.oldpayload.impl.PayloadFactory;
import icecube.daq.oldtrigger.monitor.PayloadBagMonitor;
import icecube.daq.payload.ILoadablePayload;
import icecube.daq.payload.IUTCTime;

/**
 *
 * Interface for trigger bag
 *
 * @version $Id: ITriggerBag.java 14204 2013-02-11 19:52:57Z dglo $
 * @author pat
 */
public interface ITriggerBag
{

    /**
     * add another payload to the bag
     * @param payload payload to add
     */
    void add(ILoadablePayload payload);

    /**
     * method to flush the bag, allow all payloads to go free
     */
    void flush();

    /**
     * test to see if there is a trigger available for release
     * @return true if there is a trigger to release
     */
    boolean hasNext();

    /**
     * get next payload from bag
     * @return next available payload
     */
    ILoadablePayload next();

    /**
     * get the current time of the bag
     * @return current UTC time
     */
    IUTCTime getTimeGate();

    /**
     * set the current time of the bag
     * @param time time to set
     */
    void setTimeGate(IUTCTime time);

    /**
     * get size of bag
     * @return number of triggers in bag
     */
    int size();

    /**
     * set the factory for the bag
     * @param payloadFactory payload factory
     */
    void setPayloadFactory(PayloadFactory payloadFactory);

    /**
     * Get the monitor object.
     * @return a PayloadBagMonitor
     */
    PayloadBagMonitor getMonitor();

    /**
     * Set the monitor object.
     * @param monitor
     */
    void setMonitor(PayloadBagMonitor monitor);

    /**
     * Reset the trigger UID for a new run.
     */
    void resetUID();
}
