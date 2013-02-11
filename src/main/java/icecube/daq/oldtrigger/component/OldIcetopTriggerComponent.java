package icecube.daq.oldtrigger.component;

import icecube.daq.common.DAQCmdInterface;
import icecube.daq.juggler.component.DAQCompException;
import icecube.daq.juggler.component.DAQCompServer;

public class OldIcetopTriggerComponent
    extends OldTriggerComponent
{

    private static final String COMPONENT_NAME = DAQCmdInterface.DAQ_ICETOP_TRIGGER;
    private static final int COMPONENT_ID = 0;

    public OldIcetopTriggerComponent() {
        super(COMPONENT_NAME, COMPONENT_ID);
    }

    public static void main(String[] args) throws DAQCompException {
        DAQCompServer srvr;
        try {
            srvr = new DAQCompServer(new OldIcetopTriggerComponent(), args);
        } catch (IllegalArgumentException ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
            return; // without this, compiler whines about uninitialized 'srvr'
        }
        srvr.startServing();
    }

}
