package icecube.daq.oldtrigger.component;

import icecube.daq.common.DAQCmdInterface;
import icecube.daq.juggler.component.DAQCompException;
import icecube.daq.juggler.component.DAQCompServer;
import icecube.daq.oldtrigger.algorithm.ITrigger;
import icecube.daq.oldtrigger.config.TriggerBuilder;
import icecube.daq.oldtrigger.config.TriggerReadout;
import icecube.daq.oldtrigger.control.GlobalTriggerManager;
import icecube.daq.payload.ISourceID;
import icecube.daq.payload.SourceIdRegistry;
import icecube.daq.trigger.common.ITriggerAlgorithm;
import icecube.daq.trigger.exceptions.TriggerException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class OldGlobalTriggerComponent
    extends OldTriggerComponent
{

    private static final String COMPONENT_NAME = DAQCmdInterface.DAQ_GLOBAL_TRIGGER;
    private static final int COMPONENT_ID = 0;

    public OldGlobalTriggerComponent() {
        super(COMPONENT_NAME, COMPONENT_ID);
    }

    /**
     * Configure a component using the specified configuration name.
     *
     * @param configName configuration name
     *
     * @throws icecube.daq.juggler.component.DAQCompException
     *          if there is a problem configuring
     */
    public void configuring(String configName) throws DAQCompException {
        super.configuring(configName);

        // Now get the maximum readout length
        List readouts = new ArrayList();

        ISourceID iniceSourceId = SourceIdRegistry.getISourceIDFromNameAndId(DAQCmdInterface.DAQ_INICE_TRIGGER, 0);
        readouts.addAll(getReadouts(iniceSourceId));

        ISourceID icetopSourceId = SourceIdRegistry.getISourceIDFromNameAndId(DAQCmdInterface.DAQ_ICETOP_TRIGGER, 0);
        readouts.addAll(getReadouts(icetopSourceId));

        ISourceID amandaSourceId = SourceIdRegistry.getISourceIDFromNameAndId(DAQCmdInterface.DAQ_AMANDA_TRIGGER, 0);
        readouts.addAll(getReadouts(amandaSourceId));

        ((GlobalTriggerManager) getTriggerManager()).setMaxTimeGateWindow(getMaxReadoutTimeEarliest(readouts));

    }

    private List getReadouts(ISourceID sourceId)
        throws DAQCompException
    {
        List<ITriggerAlgorithm> list;
        try {
            list =
                TriggerBuilder.buildTriggers(getTriggerConfigFile(), sourceId);
        } catch (TriggerException te) {
            throw new DAQCompException("Cannot get readouts from \"" +
                                       getTriggerConfigFile() + "\" for " +
                                       sourceId, te);
        }

        List readouts = new ArrayList();
        for (ITriggerAlgorithm trigger : list) {
            readouts.addAll(((ITrigger) trigger).getReadoutList());
        }
        return readouts;
    }

    /**
     * Find maximumReadoutTimeEarliest among configured readoutTimeWindows.
     *
     * @param readoutList list of active subdetector trigger readouts
     * @return maximum readout extent into past
     */
    private int getMaxReadoutTimeEarliest(List readoutList) {

        int maxPastOverall = Integer.MAX_VALUE;

        // loop over triggers
        Iterator readoutIter = readoutList.iterator();
        while (readoutIter.hasNext()) {
            TriggerReadout readout = (TriggerReadout) readoutIter.next();

            // check this readout against the overall earliest
            int maxPast = TriggerReadout.getMaxReadoutPast(readout);
            if (maxPast < maxPastOverall) {
                maxPastOverall = maxPast;
            }
        }

        // todo: Agree on the sign of the returned int.
        return maxPastOverall;
    }

    public static void main(String[] args) throws DAQCompException {
        DAQCompServer srvr;
        try {
            srvr = new DAQCompServer(new OldGlobalTriggerComponent(), args);
        } catch (IllegalArgumentException ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
            return; // without this, compiler whines about uninitialized 'srvr'
        }
        srvr.startServing();
    }

}
