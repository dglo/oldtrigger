package icecube.daq.oldtrigger.control;

import icecube.daq.payload.IByteBufferCache;
import icecube.daq.splicer.Splicer;
import icecube.daq.splicer.SplicedAnalysis;
import icecube.daq.splicer.SplicerListener;
import icecube.daq.trigger.common.ITriggerManager;

public interface IOldManager
    extends ITriggerHandler, ITriggerManager, SplicedAnalysis, SplicerListener
{
    /**
     * Reset the handler for a new run.
     */
    void reset();

    /**
     * Set the outgoing payload buffer cache.
     * @param byte buffer cache manager
     */
    void setOutgoingBufferCache(IByteBufferCache cache);

    /**
     * setter for splicer
     * @param splicer splicer associated with this object
     */
    void setSplicer(Splicer splicer);
}
