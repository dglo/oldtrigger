package icecube.daq.oldtrigger.control;

import icecube.daq.oldpayload.impl.TriggerRequestPayloadFactory;
import icecube.daq.oldtrigger.test.MockAppender;
import icecube.daq.oldtrigger.test.MockHit;
import icecube.daq.oldtrigger.test.MockOutputChannel;
import icecube.daq.oldtrigger.test.MockOutputProcess;
import icecube.daq.oldtrigger.test.MockPayload;
import icecube.daq.oldtrigger.test.MockSourceID;
import icecube.daq.oldtrigger.test.MockTrigger;
import icecube.daq.oldtrigger.test.MockTriggerRequest;
import icecube.daq.payload.ILoadablePayload;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.trigger.exceptions.TriggerException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.log4j.BasicConfigurator;

public class DummyTriggerHandlerTest
    extends TestCase
{
    private static final MockAppender appender = new MockAppender();

    public DummyTriggerHandlerTest(String name)
    {
        super(name);
    }

    protected void setUp()
        throws Exception
    {
        super.setUp();

        appender.clear();

        BasicConfigurator.resetConfiguration();
        BasicConfigurator.configure(appender);
    }

    public static Test suite()
    {
        return new TestSuite(DummyTriggerHandlerTest.class);
    }

    protected void tearDown()
        throws Exception
    {
        assertEquals("Bad number of log messages",
                     0, appender.getNumberOfMessages());

        super.tearDown();
    }

    public void testCreate()
    {
        TriggerRequestPayloadFactory factory =
            new TriggerRequestPayloadFactory();

        MockSourceID srcId =
            new MockSourceID(SourceIdRegistry.INICE_TRIGGER_SOURCE_ID);

        DummyTriggerHandler trigMgr = new DummyTriggerHandler(srcId, factory);
        assertNull("Monitor should be null", trigMgr.getMonitor());
        assertEquals("Count should be zero", 0, trigMgr.getTotalProcessed());
        assertEquals("Unexpected source ID",
                     srcId.getSourceID(), trigMgr.getSourceId());
    }

    public void testAddTrigger()
    {
        DummyTriggerHandler trigMgr = new DummyTriggerHandler();
        trigMgr.addTrigger(new MockTrigger());
    }

    public void testAddTriggers()
    {
        DummyTriggerHandler trigMgr = new DummyTriggerHandler();

        ArrayList list = new ArrayList();
        list.add(new MockTrigger());
        list.add(new MockTrigger());

        trigMgr.addTriggers(list);
    }

    public void testClearTriggers()
    {
        DummyTriggerHandler trigMgr = new DummyTriggerHandler();

        trigMgr.clearTriggers();
    }

    public void testIssueNoDest()
    {
        DummyTriggerHandler trigMgr = new DummyTriggerHandler();

        try {
            trigMgr.issueTriggers();
            fail("issueTriggers() should not work without PayloadDestination");
        } catch (RuntimeException rte) {
            // expect this to fail
        }
    }

    public void testIssueEmpty()
    {
        DummyTriggerHandler trigMgr = new DummyTriggerHandler();

        MockOutputProcess outProc = new MockOutputProcess();
        outProc.setOutputChannel(new MockOutputChannel());

        trigMgr.setOutputEngine(outProc);

        trigMgr.issueTriggers();
        assertEquals("Bad number of payloads written",
                    0, outProc.getNumberWritten());
    }

    public void testIssueOne()
    {
        DummyTriggerHandler trigMgr = new DummyTriggerHandler();

        MockOutputProcess outProc = new MockOutputProcess();
        outProc.setOutputChannel(new MockOutputChannel());

        trigMgr.setOutputEngine(outProc);

        trigMgr.addToTriggerBag(new MockTriggerRequest(100000L, 111111L, 0, 0));

        trigMgr.issueTriggers();
        assertEquals("Bad number of payloads written",
                     1, outProc.getNumberWritten());
    }

    public void testFlushEmpty()
    {
        DummyTriggerHandler trigMgr = new DummyTriggerHandler();

        trigMgr.flush();
    }

    public void testFlushOne()
    {
        DummyTriggerHandler trigMgr = new DummyTriggerHandler();

        MockOutputProcess outProc = new MockOutputProcess();
        outProc.setOutputChannel(new MockOutputChannel());

        trigMgr.setOutputEngine(outProc);

        trigMgr.addToTriggerBag(new MockTriggerRequest(100000L, 111111L, 0, 0));

        trigMgr.flush();
    }

    public void testProcessHit()
    {
        DummyTriggerHandler trigMgr = new DummyTriggerHandler();

        MockOutputProcess outProc = new MockOutputProcess();
        outProc.setOutputChannel(new MockOutputChannel());

        trigMgr.setOutputEngine(outProc);

        MockHit hit = new MockHit(345678L);

        assertEquals("Bad number of input payloads",
                     0, trigMgr.getTotalProcessed());

        trigMgr.process(hit);
        assertEquals("Bad number of input payloads",
                     1, trigMgr.getTotalProcessed());
        assertEquals("Bad number of triggers written",
                     1, outProc.getNumberWritten());
    }

    public void testProcessManyHitsAndReset()
    {
        DummyTriggerHandler trigMgr = new DummyTriggerHandler();

        MockOutputProcess outProc = new MockOutputProcess();
        outProc.setOutputChannel(new MockOutputChannel());

        trigMgr.setOutputEngine(outProc);

        final int numHitsPerTrigger = 6;
        trigMgr.setNumHitsPerTrigger(numHitsPerTrigger);

        assertEquals("Bad number of input payloads",
                     0, trigMgr.getTotalProcessed());

        for (int i = 0; i < numHitsPerTrigger * 4; i++) {
            MockHit hit = new MockHit(100000L + (10000 * i));

            trigMgr.process(hit);
            assertEquals("Bad number of input payloads",
                         i + 1, trigMgr.getTotalProcessed());
            assertEquals("Bad number of triggers written (" + (i + 1) +
                         " hits)",
                         (i / numHitsPerTrigger) + 1,
                         outProc.getNumberWritten());
        }

        trigMgr.reset();
        assertEquals("Bad number of input payloads",
                     0, trigMgr.getTotalProcessed());
    }

    public static void main(String[] args)
    {
        TestRunner.run(suite());
    }
}
