/*
 * class: CoincidenceTrigger
 *
 * Version $Id: CoincidenceTrigger.java 14205 2013-02-11 20:36:28Z dglo $
 *
 * Date: September 2 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.oldtrigger.algorithm;

import icecube.daq.oldpayload.PayloadInterfaceRegistry;
import icecube.daq.oldtrigger.control.DummyPayload;
import icecube.daq.oldtrigger.control.Sorter;
import icecube.daq.payload.ILoadablePayload;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.IReadoutRequest;
import icecube.daq.payload.ITriggerRequestPayload;
import icecube.daq.payload.IUTCTime;
import icecube.daq.trigger.exceptions.TriggerException;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class is to provide methods common to all coincidence triggers.
 *
 * @version $Id: CoincidenceTrigger.java 14205 2013-02-11 20:36:28Z dglo $
 * @author shseo
 */
public abstract class CoincidenceTrigger
        extends AbstractGlobalTrigger
{
    /**
    * Log object for this class
    */
    private static final Log log = LogFactory.getLog(CoincidenceTrigger.class);
    private Sorter mtSorter = new Sorter();

    private int miNumIncomingConfiguredTriggers;

    /**
     * Create an instance of this class.
     * Default constructor is declared, but private, to stop accidental
     * creation of an instance of the class.
     */
    public CoincidenceTrigger()
    {
        super();
    }
    /**
     * This is the main method.
     *
     * @param payload
     * @throws icecube.daq.trigger.exceptions.TriggerException
     */
    public void runTrigger(IPayload payload) throws TriggerException {

       //--updates the timeGate in coincidenceTriggerBag w/ any incoming trigger.
        DummyPayload dummy_one = new DummyPayload(((ITriggerRequestPayload) payload).getFirstTimeUTC());
        try {
            setEarliestTimeInConditionalTrigBag(dummy_one);
        } catch (Exception e) {
            log.error("Couldn't set earliest time", e);
        }

        if(!isConfigured()) {
            log.error("Trigger was NOT properly configured at addParameter()!");
        }

        //--add a configured trigger to a coincidenceTriggerBag
        if(isConfiguredTrigger((ITriggerRequestPayload) payload))
        {
            miNumIncomingConfiguredTriggers++;
            if (log.isDebugEnabled()) {
                log.debug("Total number of incoming configured triggers so far = " + miNumIncomingConfiguredTriggers);
            }

            mtConditionalTriggerBag.add((ILoadablePayload) payload);

            if (log.isDebugEnabled()) {
                log.debug("coincidence Trigger Bag contains " + mtConditionalTriggerBag.size() + " triggers");
            }

        }else
        {
            log.debug("This is an UnConfigured Trigger.");
            List payloadsInConditionalTrigBag = mtConditionalTriggerBag.getPayloadsInConditonalTriggerBag();
            if(payloadsInConditionalTrigBag.size() == 0){
                DummyPayload dummy = new DummyPayload(payload.getPayloadTimeUTC());
                setEarliestPayloadOfInterest(dummy);
            }
        }
        while (mtConditionalTriggerBag.hasNext())
        {
            ILoadablePayload tPayload = mtConditionalTriggerBag.next();
            //--update earliestPayloadOfInterest when there is a coincidenceTrigger.
            DummyPayload dummy = new DummyPayload(((ITriggerRequestPayload) tPayload).getFirstTimeUTC());
            setEarliestPayloadOfInterest(dummy);

            //--every wrapped trigger should be reported to GlobalTrigBag.
            reportTrigger(tPayload);
        }
        cleanConditionalTriggerBag();
    }

    public void cleanConditionalTriggerBag()
    {
        //--todo: need to update earliestPyalod (to flush) if conditionalBag contains only invalidtriggers
        List payloadsInConditionalTrigBag = mtConditionalTriggerBag.getPayloadsInConditonalTriggerBag();
        int iNumCaididateGTeventsInBag = payloadsInConditionalTrigBag.size();
        if( iNumCaididateGTeventsInBag == 0 && mtConditionalTriggerBag.needUpdate())
        {
            DummyPayload dummy = new DummyPayload(mtConditionalTriggerBag.getUpdater().getPayloadTimeUTC());
            setEarliestPayloadOfInterest(dummy);
        }

   /*     List listUnqualifiedTirggers  = mtConditionalTriggerBag.getListUnqualifiedTriggers();
        Iterator iter = listUnqualifiedTirggers.iterator();
        while(iter.hasNext())
        {
            iter.remove();
            ((ILoadablePayload)iter.next()).recycle();
        }*/
    }
    public abstract List getConfiguredTriggerIDs();

    public abstract boolean isConfiguredTrigger(ITriggerRequestPayload tPayload);

    //public abstract List selectCoincidentTrigger(List payloadList);

    //public abstract boolean isCoincidentTrigger(TriggerRequestPayload tPayload_1, TriggerRequestPayload tPayload_2);
    /**
     * This method checks two triggers overlap in time or not.
     *
     * @param payload1
     * @param payload2
     * @return
     */
    public boolean isTimeOverlap(ITriggerRequestPayload payload1, ITriggerRequestPayload payload2)
    {
        boolean bIsOverlap = false;

        IReadoutRequest tReadoutRequest_payload1 = ((ITriggerRequestPayload) payload1).getReadoutRequest();
        IReadoutRequest tReadoutRequest_payload2 = ((ITriggerRequestPayload) payload2).getReadoutRequest();

        IUTCTime tStartTime_payload1 = null;
        IUTCTime tEndTime_payload1 = null;
        IUTCTime tStartTime_payload2 = null;
        IUTCTime tEndTime_payload2 = null;

        int type1 = payload1.getPayloadInterfaceType();
        if (type1 == PayloadInterfaceRegistry.I_TRIGGER_REQUEST_PAYLOAD) {
            try{
                List elems = tReadoutRequest_payload1.getReadoutRequestElements();
                tStartTime_payload1 = mtSorter.getUTCTimeEarliest(elems, false);
                tEndTime_payload1 = mtSorter.getUTCTimeLatest(elems, false);

            }catch(NullPointerException e)
            {
                log.error("ReadoutRequest should not be null in CoincidenceTrigger!");
                return false;
            }
        } else {
            log.error("Unexpected payload type passed to CoincidenceTrigger");
            return false;
        }

        int type2 = payload2.getPayloadInterfaceType();
        if (type2 == PayloadInterfaceRegistry.I_TRIGGER_REQUEST_PAYLOAD) {
            try{
                List elems = tReadoutRequest_payload2.getReadoutRequestElements();
                tStartTime_payload2 = mtSorter.getUTCTimeEarliest(elems, false);
                tEndTime_payload2 = mtSorter.getUTCTimeLatest(elems, false);

            }catch(NullPointerException e)
            {
                log.error("ReadoutRequest should not be null in CoincidenceTrigger!");
                return false;
            }
        } else {
            log.error("Unexpected payload type passed to CoincidenceTrigger");
            return false;
        }

        double diff_1 = tStartTime_payload1.compareTo(tEndTime_payload2);
        double diff_2 = tStartTime_payload2.compareTo(tEndTime_payload1);

        if(diff_1 > 0 || diff_2 > 0)
        {
            bIsOverlap = false;
            log.debug("No Time-Overlap in CoincidenceTrigger!");
        }else
        {
            bIsOverlap = true;
            log.debug("Time-Overlap in CoincidenceTrigger!!!");
        }

        return bIsOverlap;
    }
    /**
     * This method checks two triggers are coincident based on time-overlap and triggerID.
     *
     * @param tPayload_1
     * @param tPayload_2
     * @return
     */
    public boolean isCoincidentTrigger(ITriggerRequestPayload tPayload_1, ITriggerRequestPayload tPayload_2)
    {
        boolean bIsTimeOverlap = false;
        boolean bIsDifferentTriggerId = false;

        bIsTimeOverlap = isTimeOverlap(tPayload_1, tPayload_2);
        bIsDifferentTriggerId = isDifferentTriggerId(tPayload_1, tPayload_2);

        if(bIsTimeOverlap && bIsDifferentTriggerId)
        {
            return true;
        }
        return false;
    }
    /**
     * This method checks two triggers have different triggerIDs.
     *
     * @param tPayload_1
     * @param tPayload_2
     * @return
     */
    public boolean isDifferentTriggerId(ITriggerRequestPayload tPayload_1, ITriggerRequestPayload tPayload_2)
    {
        if(getTriggerId(tPayload_1) != getTriggerId(tPayload_2))
        {
            return true;
        }

        return false;
    }

    public abstract int getTriggerId(ITriggerRequestPayload tPayload);

    /**
     * This method sets the earliestPayload for the coincidence TriggerBag.
     *
     * @throws Exception
     */
    protected void setEarliestTimeInConditionalTrigBag(IPayload tEarliestPayloadOfInterest) throws Exception
    {
        if (tEarliestPayloadOfInterest != null)
        {
            mtConditionalTriggerBag.setTimeGate(((IPayload) tEarliestPayloadOfInterest).getPayloadTimeUTC());
        }else
        {
            log.error("There is no earliestPayloadOverall.....");
            throw new NullPointerException("earliestPayloadOverall is NULL...!");
        }
    }

    public int getTotalNumberOfIncomingConfiguredTrigger()
    {
        return miNumIncomingConfiguredTriggers;
    }
    /**
     * Flush all things in the coincidenceTriggerBag.
     * This method works for any kind of coincidenceTrigger algorithm.
     */
    public void flush()
    {
        mtConditionalTriggerBag.flush();
        while(mtConditionalTriggerBag.hasNext()){
            ILoadablePayload tPayload = mtConditionalTriggerBag.next();
            reportTrigger(tPayload);
        }
    }

}
