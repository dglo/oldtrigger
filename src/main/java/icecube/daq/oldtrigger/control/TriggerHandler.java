/*
 * class: TriggerHandler
 *
 * Version $Id: TriggerHandler.java 14647 2013-10-14 21:35:55Z dglo $
 *
 * Date: October 25 2004
 *
 * (c) 2004 IceCube Collaboration
 */

package icecube.daq.oldtrigger.control;

import icecube.daq.io.DAQComponentOutputProcess;
import icecube.daq.io.OutputChannel;
import icecube.daq.juggler.alert.Alerter;
import icecube.daq.oldpayload.PayloadInterfaceRegistry;
import icecube.daq.oldpayload.impl.Payload;
import icecube.daq.oldpayload.impl.TriggerRequestPayloadFactory;
import icecube.daq.oldtrigger.algorithm.ITrigger;
import icecube.daq.oldtrigger.config.DomSetFactory;
import icecube.daq.oldtrigger.monitor.PayloadBagMonitor;
import icecube.daq.oldtrigger.monitor.TriggerHandlerMonitor;
import icecube.daq.payload.IByteBufferCache;
import icecube.daq.payload.IHitPayload;
import icecube.daq.payload.ILoadablePayload;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.ISourceID;
import icecube.daq.payload.ITriggerRequestPayload;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.IWriteablePayload;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.payload.impl.SourceID;
import icecube.daq.payload.impl.UTCTime;
import icecube.daq.trigger.common.ITriggerAlgorithm;
import icecube.daq.trigger.common.ITriggerManager;
import icecube.daq.trigger.exceptions.TriggerException;
import icecube.daq.util.DOMRegistry;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class provides the analysis framework for the inice trigger.
 *
 * @version $Id: TriggerHandler.java 14647 2013-10-14 21:35:55Z dglo $
 * @author pat
 */
public class TriggerHandler
        implements ITriggerHandler
{

    /**
     * Log object for this class
     */
    private static final Log log = LogFactory.getLog(TriggerHandler.class);

    static final DummyPayload FLUSH_PAYLOAD = new DummyPayload(new UTCTime(0));

    private static final boolean USE_THREAD = true;

    /**
     * List of defined triggers
     */
    private List<ITriggerAlgorithm> triggerList;

    /**
     * Bag of triggers to issue
     */
    private ITriggerBag triggerBag;

    /**
     * counts the number of processed primitives
     */
    private int count;

    /**
     * output process
     */
    private DAQComponentOutputProcess payloadOutput;

    /**
     * output channel
     */
    private OutputChannel outChan;

    /**
     * earliest thing of interest to the analysis
     */
    private IPayload earliestPayloadOfInterest;

    /**
     * source of last hit, used for monitoring
     */
    private ISourceID srcOfLastHit;

    /**
     * time of last hit, used for monitoring
     */
    private IUTCTime timeOfLastHit;

    /**
     * input handler
     */
    private ITriggerInput inputHandler;

    /**
     * Default output factory
     */
    private TriggerRequestPayloadFactory outputFactory;

    /**
     * SourceId of this TriggerHandler.
     */
    private ISourceID sourceId;

    /**
     * Monitor object.
     */
    private TriggerHandlerMonitor monitor;

    /**
     * DOMRegistry
     */
    private DOMRegistry domRegistry;

    /** Outgoing byte buffer cache. */
    private IByteBufferCache outCache;

    /** Main trigger thread */
    private MainThread mainThread;

    /** Splicer adds payloads to this queue -- ACCESS MUST BE SYNCHRONIZED. */
    private LinkedList<ILoadablePayload> inputQueue =
        new LinkedList<ILoadablePayload>();

    /** Output thread. */
    private OutputThread outputThread;

    /** Output queue  -- ACCESS MUST BE SYNCHRONIZED. */
    private List<ByteBuffer> outputQueue =
        new LinkedList<ByteBuffer>();

    /**
     * Default constructor
     */
    public TriggerHandler() {
        this(new SourceID(SourceIdRegistry.INICE_TRIGGER_SOURCE_ID));
    }

    public TriggerHandler(ISourceID sourceId) {
        this(sourceId, new TriggerRequestPayloadFactory());
    }

    public TriggerHandler(ISourceID sourceId, TriggerRequestPayloadFactory outputFactory) {
        this.sourceId = sourceId;
        this.outputFactory = outputFactory;
        init();
    }

    protected void init() {

        count = 0;
        earliestPayloadOfInterest = null;
        srcOfLastHit = null;
        timeOfLastHit = null;
        inputHandler = new TriggerInput();
        triggerList = new ArrayList<ITriggerAlgorithm>();

        triggerBag = createTriggerBag();

        monitor = new TriggerHandlerMonitor();
        PayloadBagMonitor triggerBagMonitor = new PayloadBagMonitor();
        triggerBag.setMonitor(triggerBagMonitor);
        monitor.setTriggerBagMonitor(triggerBagMonitor);

        outChan = null;

        if (inputQueue.size() > 0) {
            if (log.isErrorEnabled()) {
                log.error("Unhandled input payloads queued at init");
            }

            synchronized (inputQueue) {
                inputQueue.clear();
            }
        }

        if (mainThread == null) {
            mainThread = new MainThread("Main-" + sourceId);
            mainThread.start();
        }

        if (outputQueue.size() > 0) {
            if (log.isErrorEnabled()) {
                log.error("Unwritten triggers queued at reset");
            }

            synchronized (outputQueue) {
                outputQueue.clear();
            }
        }

        if (outputThread == null) {
            outputThread = new OutputThread("Output-" + sourceId);
            outputThread.start();
        }
    }

    protected ITriggerBag createTriggerBag()
    {
        ITriggerBag bag = new TriggerBag(sourceId);
        if (outputFactory != null) {
            bag.setPayloadFactory(outputFactory);
        }
        return bag;
    }

    /**
     * add a new trigger payload to the bag
     * @param triggerPayload new trigger to add
     */
    public void addToTriggerBag(ILoadablePayload triggerPayload) {
        triggerBag.add(triggerPayload);
    }

    /**
     * method for adding triggers to the trigger list
     * @param trigger trigger to be added
     */
    public void addTrigger(ITriggerAlgorithm trigger) {

        // check for duplicates
        boolean good = true;
        for (ITriggerAlgorithm existing : triggerList) {
            if ( (trigger.getTriggerType() == existing.getTriggerType()) &&
                 (trigger.getTriggerConfigId() == existing.getTriggerConfigId()) &&
                 (trigger.getSourceId() == existing.getSourceId()) ) {
                log.error("Attempt to add duplicate trigger to trigger list!");
                good = false;
            }
        }

        if (good) {
            log.info("Setting OutputFactory of Trigger");
            ((ITrigger) trigger).setTriggerFactory(outputFactory);
            log.info("Adding Trigger to TriggerList");
            triggerList.add(trigger);
            trigger.setTriggerManager((ITriggerManager) this);
        }
    }

    /**
     * add a list of triggers
     *
     * @param triggers
     */
    public void addTriggers(List<ITriggerAlgorithm> triggers) {
        clearTriggers();
        triggerList.addAll(triggers);
    }

    /**
     * clear list of triggers
     */
    public void clearTriggers() {
        triggerList.clear();
    }

    public Map<String, Long> getTriggerCounts()
    {
        HashMap<String, Long> map = new HashMap<String, Long>();

        for (ITriggerAlgorithm trigger : triggerList) {
            map.put(trigger.getTriggerName(),
                    new Long(trigger.getTriggerCounter()));
        }

        return map;
    }

    public Map<String, Object> getTriggerMonitorMap()
    {
        HashMap<String, Object> map = new HashMap<String, Object>();

        for (ITriggerAlgorithm trigger : triggerList) {
            Map<String, Object> moniMap = trigger.getTriggerMonitorMap();
            if (moniMap != null && moniMap.size() > 0) {
                String trigName = trigger.getTriggerName() + "-" +
                    trigger.getTriggerConfigId();
		for (Map.Entry entry: moniMap.entrySet()) {
		    map.put(trigName + "-" + entry.getKey(),
			    entry.getValue());
		}
            }
        }

        return map;
    }

    /**
     * flush the handler
     * including the input buffer, all triggers, and the output bag
     */
    public void flush() {
        // flush the input handler
        if (log.isInfoEnabled()) {
            log.info("Flushing InputHandler: size = " + inputHandler.size());
        }
        inputHandler.flush();

        // then call process with a null payload to drain everything from
        // the input handler
        process(null);
        while (mainThread != null && !mainThread.sawFlush()) {
            Thread.yield();
//try { Thread.sleep(10); } catch (Exception ex) { }
        }

        // now flush the triggers, this should prompt them to send any
        // known triggers to the bag
        if (log.isInfoEnabled()) {
            log.info("Flushing Triggers");
        }
        for (ITriggerAlgorithm trigger : triggerList) {
            ((ITrigger) trigger).flush();
            if (log.isInfoEnabled()) {
                log.info("Trigger count for " + trigger.getTriggerName() +
                         " is " + trigger.getTriggerCounter());
            }
        }

        // finally flush the trigger bag
        if (log.isInfoEnabled()) {
            log.info("Flushing TriggerBag");
        }
        triggerBag.flush();

        // one last call to process to check the bag
        process(null);
        while (mainThread != null && !mainThread.sawFlush()) {
            Thread.yield();
        }

    }

    /**
     * sets payload output
     * @param payloadOutput destination of payloads
     */
    public void setOutputEngine(DAQComponentOutputProcess payloadOutput) {
        this.payloadOutput = payloadOutput;
    }

    public DAQComponentOutputProcess getPayloadOutput()
    {
        return payloadOutput;
    }

    public boolean hasUnprocessedPayloads()
    {
        return inputQueue.size() > 0;
    }

    public boolean isMerged()
    {
        throw new Error("Unimplemented");
    }

    /**
     * Method to process payloads, assumes that they are time ordered.
     * @param payload payload to process
     */
    public void process(ILoadablePayload payload) {

        if (!USE_THREAD) {
            if (payload != null) {
                reprocess(payload);
            } else {
                reprocess(FLUSH_PAYLOAD);
            }
        } else {
            synchronized (inputQueue) {
                if (mainThread == null) {
                    if (payload != null) {
                        log.error("Attempting to queue " + payload +
                                  " without input thread");
                    }
                } else if (payload != null) {
                    inputQueue.add(payload);
                } else {
                    inputQueue.add(FLUSH_PAYLOAD);
                }
                inputQueue.notify();
            }
        }
    }

    void reprocess(ILoadablePayload payload) {
        // add payload to input handler
        if (payload != FLUSH_PAYLOAD) {
            inputHandler.addPayload(payload);
        }

        // now loop over payloads available from input handler
        while (inputHandler.hasNext()) {
            IPayload nextPayload = inputHandler.next();

            int interfaceType = nextPayload.getPayloadInterfaceType();

            // make sure we have hit payloads (or hit data payloads)
            if ((interfaceType == PayloadInterfaceRegistry.I_HIT_PAYLOAD) ||
                (interfaceType == PayloadInterfaceRegistry.I_HIT_DATA_PAYLOAD)) {

                IHitPayload hit = (IHitPayload) nextPayload;
                if (hit.getHitTimeUTC() == null) {
                    Payload pay = (Payload) hit;
                    log.error("Bad hit buf " + pay.getPayloadBacking() +
                              " off " + pay.getPayloadOffset() + " len " +
                              pay.length() + " type " +
                              pay.getPayloadType() + " utc " +
                              pay.getPayloadTimeUTC());
                    continue;
                }

                // Calculate time since last hit
                double timeDiff;
                if (timeOfLastHit == null) {
                    timeDiff = 0.0;
                } else {
                    timeDiff = hit.getHitTimeUTC().timeDiff_ns(timeOfLastHit);
                }

                // check to see if timeDiff is reasonable, if not ignore it
                if (timeDiff < 0.0) {
                    log.error("Hit from " + hit.getSourceID() +
                              " out of order! This time - Last time = " +
                              timeDiff + (srcOfLastHit == null ? "" :
                                          ", src of last hit = " +
                                          srcOfLastHit));
                    continue;
                } else {
                    timeOfLastHit = hit.getHitTimeUTC();
                    srcOfLastHit = hit.getSourceID();
                    count++;
                }

                // loop over triggers
                for (ITriggerAlgorithm trigger : triggerList) {
                    try {
                        trigger.runTrigger(hit);
                    } catch (TriggerException e) {
                        log.error("Exception while running trigger", e);
                    }
                }

            } else if(interfaceType == PayloadInterfaceRegistry.I_TRIGGER_REQUEST_PAYLOAD){
                try {
                    ((ILoadablePayload) nextPayload).loadPayload();
                } catch (IOException e) {
                    log.error("Couldn't load payload", e);
                } catch (DataFormatException e) {
                    log.error("Couldn't load payload", e);
                }
                ITriggerRequestPayload tPayload = (ITriggerRequestPayload) nextPayload;
                int srcId;
                if (tPayload.getSourceID() != null) {
                    srcId = tPayload.getSourceID().getSourceID();
                } else {
                    if (tPayload.length() == 0 &&
                        tPayload.getPayloadTimeUTC() == null &&
                        ((IPayload) tPayload).getPayloadBacking() == null)
                    {
                        log.error("Ignoring recycled payload");
                    } else {
                        log.error("Unexpected null SourceID in payload (len=" +
                                  tPayload.length() + ", time=" +
                                  (tPayload.getPayloadTimeUTC() == null ?
                                   "null" : "" + tPayload.getPayloadTimeUTC()) +
                                   ", buf=" +
                                  ((IPayload) tPayload).getPayloadBacking() +
                                  ")");
                    }

                    srcId = -1;
                }
                if(srcId == SourceIdRegistry.AMANDA_TRIGGER_SOURCE_ID
                    || srcId == SourceIdRegistry.STRINGPROCESSOR_SOURCE_ID){
                    count++;

                    // loop over triggers
                    for (ITriggerAlgorithm trigger : triggerList) {
                        try {
                            trigger.runTrigger(tPayload);
                        } catch (TriggerException e) {
                            log.error("Exception while running trigger", e);
                        }
                    }
                }else if (srcId != -1) {

                    log.error("SourceID " + srcId + " should not send TriggerRequestPayloads!");
                }
            }else{
                    log.warn("TriggerHandler only knows about either hitPayloads or TriggerRequestPayloads!");
            }

            Thread.yield();
        }

        // Check triggerBag and issue triggers
        issueTriggers();

    }

    /**
     * Reset the handler for a new run.
     */
    public void reset() {
        init();
    }

    /**
     * Get the monitor object.
     *
     * @return a TriggerHandlerMonitor
     */
    public TriggerHandlerMonitor getMonitor() {
        return monitor;
    }

    /**
     * Get the input handler
     *
     * @return a trigger input handler
     */
    public ITriggerInput getInputHandler()
    {
        return inputHandler;
    }

    /**
     * getter for count
     * @return count
     * @deprecated use getTotalProcessed()
     */
    public long getCount() {
        return getTotalProcessed();
    }

    /**
     * getter for total number of payloads processed
     * @return count
     */
    public long getTotalProcessed()
    {
        return count;
    }

    /**
     * getter for triggerList
     * @return trigger list
     */
    public List<ITriggerAlgorithm> getTriggerList() {
        return triggerList;
    }

    /**
     * getter for SourceID
     * @return sourceID
     */
    public ISourceID getSourceObject() {
        return sourceId;
    }

    /**
     * getter for SourceID
     * @return sourceID
     */
    public int getSourceId() {
        if (sourceId == null) {
            throw new Error("Source ID has not been set");
        }

        return sourceId.getSourceID();
    }

    void complainAboutNonTrigger(IWriteablePayload payload)
    {
        log.error("Issuing non-request " + payload);
    }

    /**
     * check triggerBag and issue triggers if possible
     *   any triggers that are earlier than the earliestPayloadOfInterest are selected
     *   if any of those overlap, they are merged
     */
    public void issueTriggers() {
        if (null == payloadOutput) {
            throw new RuntimeException("PayloadOutput has not been set!");
        }


        if (log.isDebugEnabled()) {
            log.debug("Trigger Bag contains " + triggerBag.size() +
                      " triggers");
        }

        // update earliest time of interest
        setEarliestTime();

        while (triggerBag.hasNext()) {
            IWriteablePayload payload = (IWriteablePayload) triggerBag.next();

            if (payload == null) {
                log.error("TriggerBag returned null next payload");
                break;
            }

            if (payload.getPayloadInterfaceType() !=
                PayloadInterfaceRegistry.I_TRIGGER_REQUEST_PAYLOAD)
            {
                complainAboutNonTrigger(payload);
            } else if (log.isDebugEnabled()) {
                ITriggerRequestPayload trigger = (ITriggerRequestPayload) payload;

                IUTCTime firstTime = trigger.getFirstTimeUTC();
                IUTCTime lastTime = trigger.getLastTimeUTC();

                int nSubPayloads = 0;
                try {
                    nSubPayloads = trigger.getPayloads().size();
                } catch (Exception e) {
                    log.error("Couldn't get number of subpayloads", e);
                }

                String trType;
                if (0 > trigger.getTriggerType()) {
                    trType = "triggers";
                } else {
                    trType = "hits";
                }

                log.debug("Issue trigger: extended event time = " +
                          firstTime + " to " + lastTime +
                          " and contains " + nSubPayloads + " " +
                          trType);
            }

            int bufLen = payload.length();

            // allocate ByteBuffer
            ByteBuffer trigBuf;
            if (outCache != null) {
                trigBuf = outCache.acquireBuffer(bufLen);
//System.err.println("Alloc "+trigBuf.capacity()+" bytes from "+outCache);
            } else {
                trigBuf = ByteBuffer.allocate(bufLen);
//System.err.println("Unattached "+SourceIdRegistry.getDAQNameFromISourceID(sourceId)+" "+trigBuf.capacity()+" bytes");
            }

            // write trigger to a ByteBuffer
            try {
                ((IWriteablePayload) payload).writePayload(false, 0, trigBuf);
            } catch (IOException ioe) {
                log.error("Couldn't create payload", ioe);
                trigBuf = null;
            }

            if (trigBuf != null) {
                synchronized (outputQueue) {
                    outputQueue.add(trigBuf);
                    outputQueue.notify();
                }
            }

            // now recycle it
            payload.recycle();

        }

    }

    public IPayload getEarliestPayloadOfInterest() {
        return earliestPayloadOfInterest;
    }

    public void setEarliestPayloadOfInterest(IPayload earliest) {
        earliestPayloadOfInterest = earliest;
    }

    /**
     * sets the earliest overall time of interest by inspecting each trigger
     */
    private void setEarliestTime() {

        IUTCTime earliestTimeOverall = new UTCTime(Long.MAX_VALUE);
        IPayload earliestPayloadOverall = null;

        // loop over triggers and find earliest time of interest
        for (ITriggerAlgorithm trigger : triggerList) {
            IPayload earliestPayload = trigger.getEarliestPayloadOfInterest();
            if (earliestPayload != null) {
                // if payload < earliest
                if (earliestTimeOverall.compareTo(earliestPayload.getPayloadTimeUTC()) > 0) {
                    earliestTimeOverall = earliestPayload.getPayloadTimeUTC();
                    earliestPayloadOverall = earliestPayload;
                }
            }
        }

        if (earliestPayloadOverall != null) {
            earliestPayloadOfInterest = earliestPayloadOverall;

            // set timeGate in triggerBag
            triggerBag.setTimeGate(earliestPayloadOfInterest.getPayloadTimeUTC());
            // set earliestPayload
            setEarliestPayloadOfInterest(earliestPayloadOfInterest);
        }

    }

    public void setDOMRegistry(DOMRegistry registry) {
        domRegistry = registry;
        DomSetFactory.setDomRegistry(registry);
    }

    public DOMRegistry getDOMRegistry() {
        return domRegistry;
    }

    public void setOutputFactory(TriggerRequestPayloadFactory factory)
    {
        outputFactory = factory;
        if (outputFactory != null) {
            triggerBag.setPayloadFactory(outputFactory);
        }
    }

    /**
     * Set the outgoing payload buffer cache.
     * @param byte buffer cache manager
     */
    public void setOutgoingBufferCache(IByteBufferCache cache)
    {
        outCache = cache;
    }

    /**
     * Stop the threads
     */
    public void stopThread()
    {
        if (mainThread != null) {
            mainThread = null;

            synchronized (inputQueue) {
                inputQueue.notify();
            }
        }

        if (outputThread != null) {
            outputThread = null;

            synchronized (outputQueue) {
                outputQueue.notify();
            }
        }
    }

    public boolean isMainThreadWaiting()
    {
        if (mainThread == null) {
            return false;
        }

        return mainThread.isWaiting();
    }

    public boolean isOutputThreadWaiting()
    {
        if (outputThread == null) {
            return false;
        }

        return outputThread.isWaiting();
    }

    /**
     * Get number of triggers queued for input.
     *
     * @return number of triggers queued
     */
    public int getNumInputsQueued()
    {
        return inputQueue.size();
    }

    /**
     * Get number of triggers queued for output.
     *
     * @return number of triggers queued
     */
    public int getNumOutputsQueued()
    {
        return outputQueue.size();
    }

    public void switchToNewRun(Alerter alerter, int runNumber)
    {
        // does nothing
    }

    class MainThread
        implements Runnable
    {
        private Thread thread;
        private boolean sawFlush;
        private boolean waiting;

        MainThread(String name)
        {
            thread = new Thread(this);
            thread.setName(name);
        }

        void addedFlush()
        {
            sawFlush = false;
        }

        boolean isWaiting()
        {
            return waiting;
        }

        /**
         * Main payload processing loop.
         */
        public void run()
        {
            // wait for initial assignment to complete
            while (mainThread == null) {
                Thread.yield();
            }

            ILoadablePayload payload;
            while (mainThread != null) {
                synchronized (inputQueue) {
                    if (inputQueue.size() == 0) {
                        try {
                            waiting = true;
                            inputQueue.wait();
                        } catch (InterruptedException ie) {
                            log.error("Interrupt while waiting for input queue",
                                      ie);
                        }
                        waiting = false;
                    }

                    if (inputQueue.size() == 0) {
                        payload = null;
                    } else {
                        payload = inputQueue.remove(0);
                    }
                }

                if (payload != null) {
                    try {
                        reprocess(payload);
                    } catch (Throwable thr) {
                        log.error("Caught exception while processing " +
                                  payload, thr);
                        // should probably break out of the thread after
                        //  some number of consecutive failures
                    }
                    if (payload == FLUSH_PAYLOAD) {
                        sawFlush = true;
                        break;
                    }
                }
            }
        }

        boolean sawFlush()
        {
            return sawFlush;
        }

        void start()
        {
            thread.start();
        }
    }

    /**
     * Class which writes triggers to output channel.
     */
    class OutputThread
        implements Runnable
    {
        private Thread thread;
        private boolean waiting;

        /**
         * Create and start output thread.
         *
         * @param name thread name
         */
        OutputThread(String name)
        {
            thread = new Thread(this);
            thread.setName(name);
        }

        boolean isWaiting()
        {
            return waiting;
        }

        /**
         * Main output loop.
         */
        public void run()
        {
//System.err.println("OTtop");
            ByteBuffer trigBuf;
            while (outputThread != null || outputQueue.size() > 0) {
//System.err.println("OTloop");
                synchronized (outputQueue) {
//System.err.println("OTq="+outputQueue.size());
                    if (outputQueue.size() == 0) {
                        try {
                            waiting = true;
//System.err.println("OTwait");
                            outputQueue.wait();
                        } catch (InterruptedException ie) {
                            log.error("Interrupt while waiting for output queue",
                                      ie);
                        }
                        waiting = false;
//System.err.println("OTawake");
                    }

                    if (outputQueue.size() == 0) {
//System.err.println("OTempty");
                        trigBuf = null;
                    } else {
                        trigBuf = outputQueue.remove(0);
//System.err.println("OTgotBuf");
                    }
                }

                if (trigBuf == null) {
//System.err.println("OTcont");
                    continue;
                }

                // if we haven't already, get the output channel
                if (outChan == null) {
                    if (payloadOutput == null) {
//System.err.println("OT!!noOut!!");
                        log.error("Trigger destination has not been set");
			throw new Error("Trigger destination not set");
                    }

		    outChan = payloadOutput.getChannel();
		    if (outChan == null) {
			//System.err.println("OT!!noChan!!");
			throw new Error("Output channel has not been set" +
					" in " + payloadOutput);
		    }
                }

                //--ship the trigger to its destination
//System.err.println("OTsend");
                outChan.receiveByteBuffer(trigBuf);
            }
//System.err.println("OTexit");

            if (outChan != null) {
                outChan.sendLastAndStop();
            }
        }

        void start()
        {
            thread.start();
        }
    }
}
