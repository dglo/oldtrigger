/*
 * interface: ITriggerManager
 *
 * Version $Id: ITriggerManager.java 14204 2013-02-11 19:52:57Z dglo $
 *
 * Date: March 31 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.oldtrigger.control;

import icecube.daq.oldpayload.impl.TriggerRequestPayloadFactory;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.IUTCTime;
import icecube.daq.splicer.Splicer;

/**
 * This interface defines the behavior of a TriggerManager
 *
 * @version $Id: ITriggerManager.java 14204 2013-02-11 19:52:57Z dglo $
 * @author pat
 */
public interface ITriggerManager
    extends ITriggerHandler, AdvancedSplicedAnalysis
{

    Splicer getSplicer();

    IUTCTime getEarliestTime();

    IUTCTime getLatestTime();

    long getAverageProcessingTime();

    long getMinProcessingTime();

    long getMaxProcessingTime();

    int getNumOutputsQueued();

    int getProcessingCount();

    IPayload getEarliestPayloadOfInterest();

    void setOutputFactory(TriggerRequestPayloadFactory factory);
}
