package icecube.daq.oldtrigger.component;

import icecube.daq.io.DAQComponentOutputProcess;
import icecube.daq.io.OutputChannel;
import icecube.daq.io.SimpleOutputEngine;
import icecube.daq.io.SpliceablePayloadReader;
import icecube.daq.juggler.component.DAQCompException;
import icecube.daq.juggler.component.DAQCompServer;
import icecube.daq.juggler.component.DAQComponent;
import icecube.daq.juggler.component.DAQConnector;
import icecube.daq.juggler.mbean.MemoryStatistics;
import icecube.daq.juggler.mbean.SystemStatistics;
import icecube.daq.oldpayload.impl.HitPayload;
import icecube.daq.oldpayload.impl.MasterPayloadFactory;
import icecube.daq.oldpayload.impl.TriggerRequestPayloadFactory;
import icecube.daq.payload.IByteBufferCache;
import icecube.daq.payload.IReadoutRequest;
import icecube.daq.payload.IReadoutRequestElement;
import icecube.daq.payload.ISourceID;
import icecube.daq.payload.ITriggerRequestPayload;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.IWriteablePayload;
import icecube.daq.payload.PayloadRegistry;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.payload.impl.SourceID;
import icecube.daq.payload.impl.UTCTime;
import icecube.daq.payload.impl.VitreousBufferCache;
import icecube.daq.splicer.HKN1Splicer;
import icecube.daq.splicer.Spliceable;
import icecube.daq.splicer.SplicedAnalysis;
import icecube.daq.splicer.Splicer;
import icecube.daq.splicer.SplicerChangedEvent;
import icecube.daq.splicer.SplicerListener;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class Analysis
    implements SplicedAnalysis, SplicerListener, AnalysisMBean
{
    class Counter
    {
        private int val;

        Counter()
        {
            this(0);
        }

        Counter(int val)
        {
            this.val = val;
        }

        synchronized int getValue()
        {
            return val;
        }

        synchronized void inc()
        {
            val++;
        }
    }

    private static final int DEFAULT_HIT_INTERVAL = 350;

    private static final Log LOG = LogFactory.getLog(Analysis.class);

    private static final ISourceID SOURCE_ID =
        new SourceID(SourceIdRegistry.INICE_TRIGGER_SOURCE_ID);

    private static final long TIME_WINDOW = 10000000L;

    private MasterPayloadFactory factory;
    private TriggerRequestPayloadFactory trigReqFactory;

    private DAQComponentOutputProcess trigOut;
    private OutputChannel trigChan;

    private Splicer splicer;
    private int listOffset;

    private HashMap hitSrcs = new HashMap();

    private int numHitsPerTrigger = DEFAULT_HIT_INTERVAL;

    private int hitCount;
    private long totHitCount;
    private int triggerCount;
    private long prevTime;
    private long ignoredCount;

    Analysis(MasterPayloadFactory factory)
    {
        this.factory = factory;

        final int type = PayloadRegistry.PAYLOAD_ID_TRIGGER_REQUEST;
        trigReqFactory =
            (TriggerRequestPayloadFactory) factory.getPayloadFactory(type);
    }

    public void disposed(SplicerChangedEvent evt)
    {
        // ignored
    }

    public void execute(List splicedObjects, int decrement)
    {
        final int listLen = splicedObjects.size();
        for (int i = listOffset; i < listLen; i++)
        {
/*
            HitPayload hit = (HitPayload) splicedObjects.get(i);

            ISourceID srcId = hit.getSourceID();
            if (!hitSrcs.containsKey(srcId)) {
                hitSrcs.put(srcId, new Counter(1));
            } else {
                ((Counter) hitSrcs.get(srcId)).inc();
            }
*/

            totHitCount++;
            if (++hitCount >= numHitsPerTrigger) {
                HitPayload hit = (HitPayload) splicedObjects.get(i);
                sendRequest(hit);
                hitCount -= numHitsPerTrigger;
            }
        }

        if (listLen > 0)
        {
            if (listLen >= listOffset) listOffset = listLen;
            splicer.truncate((Spliceable) splicedObjects.get(listLen - 1));
        }
    }

    public void failed(SplicerChangedEvent evt)
    {
        // ignored
    }

    public HashMap getHitSources()
    {
        HashMap strSrcs = new HashMap();

        Iterator iter = hitSrcs.keySet().iterator();
        while (iter.hasNext()) {
            ISourceID srcObj = (ISourceID) iter.next();
            Counter cnt = (Counter) hitSrcs.get(srcObj);

            int srcId = srcObj.getSourceID();
            String srcStr = SourceIdRegistry.getDAQNameFromSourceID(srcId) +
                "#" + SourceIdRegistry.getDAQIdFromSourceID(srcId);

            strSrcs.put(srcStr, new Integer(cnt.getValue()));
        }

        return strSrcs;
    }

    public long getIgnoredCount()
    {
        return ignoredCount;
    }

    public int getNumHitsPerTrigger()
    {
        return numHitsPerTrigger;
    }

    public long getTotalHitCount()
    {
        return totHitCount;
    }

    public int getTriggerCount()
    {
        return triggerCount;
    }

    private void sendRequest(HitPayload hit)
    {
        final long offset = TIME_WINDOW / 20L;

        if (trigChan == null) {
            if (trigOut == null) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Trigger output has not been set");
                }
		//TODO: Is this what you want to do?
		return;
            }

            trigChan = trigOut.getChannel();
        }

        final IUTCTime time = hit.getHitTimeUTC();

        long minTime = time.longValue() - offset;
        if (minTime < prevTime) {
            minTime = prevTime;
        }

        final long maxTime = time.longValue() + offset;
        if (maxTime <= minTime) {
            ignoredCount++;
        } else {
            triggerCount++;

            prevTime = maxTime;

            IUTCTime minObj = new UTCTime(minTime);
            IUTCTime maxObj = new UTCTime(maxTime);

            IReadoutRequestElement rrElem =
                TriggerRequestPayloadFactory.createReadoutRequestElement
                (IReadoutRequestElement.READOUT_TYPE_GLOBAL, minObj, maxObj,
                 null, null);

            Vector elems = new Vector();
            elems.add(rrElem);

            IReadoutRequest rReq =
                TriggerRequestPayloadFactory.createReadoutRequest
                (SOURCE_ID, triggerCount, elems);

            Vector hits = new Vector();
            hits.add(hit);

            final int trigType = 1;
            final int trigCfgId = 1;

            ITriggerRequestPayload trigReq =
                (ITriggerRequestPayload) trigReqFactory.createPayload
                (triggerCount, trigType, trigCfgId, SOURCE_ID, minObj, maxObj,
                 hits, rReq);

            ByteBuffer trigBuf =
                ByteBuffer.allocate(trigReq.length());
            try {
                ((IWriteablePayload) trigReq).writePayload(false, 0, trigBuf);
            } catch (IOException ioe) {
                LOG.error("Couldn't create payload", ioe);
                trigBuf = null;
            }

            if (trigBuf != null) {
                trigChan.receiveByteBuffer(trigBuf);
            }

            trigReq.recycle();
        }
    }

    public void setNumHitsPerTrigger(int numHitsPerTrigger)
    {
        this.numHitsPerTrigger = numHitsPerTrigger;
    }

    void setOutput(DAQComponentOutputProcess trigOut)
    {
        this.trigOut = trigOut;
        trigChan = null;
    }

    void setSplicer(Splicer splicer)
    {
        this.splicer = splicer;
    }

    public void started(SplicerChangedEvent evt)
    {
        // ignored
    }

    public void starting(SplicerChangedEvent evt)
    {
        LOG.error("Rate is " + numHitsPerTrigger + " hits/trigger");
    }

    public void stopped(SplicerChangedEvent evt)
    {
        // ignored
    }

    public void stopping(SplicerChangedEvent evt)
    {
        // ignored
    }

    public void truncated(SplicerChangedEvent evt)
    {
        if (evt.getSpliceable() == Splicer.LAST_POSSIBLE_SPLICEABLE) {
            listOffset = 0;
        } else {
            Iterator iter = evt.getAllSpliceables().iterator();
            while (iter.hasNext()) {
                HitPayload payload = (HitPayload) iter.next();
                payload.recycle();
            }

            listOffset -= evt.getAllSpliceables().size();
        }
    }
}

/**
 * Simple trigger component.
 */
public class SimpleTrigger
    extends DAQComponent
{
    /** Component name. */
    private static final String COMPONENT_NAME = "simpleTrigger";

    private SpliceablePayloadReader hitReader;
    private SimpleOutputEngine gtWriter;
    private Analysis analysis;

    /**
     * Create event builder component.
     */
    public SimpleTrigger()
    {
        super(COMPONENT_NAME, 0);

        final int compId = 0;

        IByteBufferCache hitMgr = new VitreousBufferCache("SimpTrigHit");
        addCache(DAQConnector.TYPE_STRING_HIT, hitMgr);
        MasterPayloadFactory hitFactory = new MasterPayloadFactory(hitMgr);

        IByteBufferCache trigMgr = new VitreousBufferCache("SimpTrig");
        addCache(DAQConnector.TYPE_TRIGGER, trigMgr);

        addMBean("jvm", new MemoryStatistics());
        addMBean("system", new SystemStatistics());

        analysis = new Analysis(hitFactory);
        Splicer splicer = new HKN1Splicer(analysis);
        splicer.addSplicerListener(analysis);
        analysis.setSplicer(splicer);
        addSplicer(splicer);
        addMBean("analysis", analysis);

        try {
            hitReader = new SpliceablePayloadReader(COMPONENT_NAME, splicer,
                                                    hitFactory);
        } catch (IOException ioe) {
            throw new Error("Couldn't create hit reader", ioe);
        }
        addMonitoredEngine(DAQConnector.TYPE_STRING_HIT, hitReader);

        gtWriter = new SimpleOutputEngine(COMPONENT_NAME, compId, "gtOutput");
        addMonitoredEngine(DAQConnector.TYPE_TRIGGER, gtWriter, true);

        //gtWriter.registerBufferManager(trigMgr);
        analysis.setOutput(gtWriter);
    }

    public String getVersionInfo()
    {
        return "$Id$";
    }

    /**
     * Handle a command-line option.
     *
     * @param arg0 first argument string
     * @param arg1 second argument string (or <tt>null</tt> if none available)
     *
     * @return 0 if the argument string was not used
     *         1 if only the first argument string was used
     *         2 if both argument strings were used
     */
    public int handleOption(String arg0, String arg1)
    {
        int used = 0;
        if (arg0.startsWith("-h")) {
            String numStr;
            if (arg0.length() > 2) {
                numStr = arg0.substring(2);
                used = 1;
            } else {
                if (arg1 == null) {
                    System.err.println("No argument to -h");
                    return -1;
                }

                numStr = arg1;
                used = 2;
            }

            int val;
            try {
                val = Integer.parseInt(numStr);
            } catch (NumberFormatException nfe) {
                System.err.println("Bad number of hits \"" + numStr + "\"");
                return -2;
            }

            analysis.setNumHitsPerTrigger(val);
        }

        return used;
    }

    /**
     * Run a DAQ component server.
     *
     * @param args command-line arguments
     *
     * @throws DAQCompException if there is a problem
     */
    public static void main(String[] args)
        throws DAQCompException
    {
        DAQCompServer srvr;
        try {
            srvr = new DAQCompServer(new SimpleTrigger(), args);
        } catch (IllegalArgumentException ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
            return; // without this, compiler whines about uninitialized 'srvr'
        }
        srvr.startServing();
    }
}
