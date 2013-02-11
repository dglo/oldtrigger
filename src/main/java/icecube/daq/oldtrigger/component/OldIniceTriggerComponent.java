package icecube.daq.oldtrigger.component;

import icecube.daq.common.DAQCmdInterface;
import icecube.daq.juggler.component.DAQCompException;
import icecube.daq.juggler.component.DAQCompServer;

public class OldIniceTriggerComponent
    extends OldTriggerComponent
{

    private static final String COMPONENT_NAME = DAQCmdInterface.DAQ_INICE_TRIGGER;
    private static final int COMPONENT_ID = 0;

    public OldIniceTriggerComponent() {
        super(COMPONENT_NAME, COMPONENT_ID);
    }

    public static void main(String[] args) throws DAQCompException {
        DAQCompServer srvr;
        try {
            srvr = new DAQCompServer(new OldIniceTriggerComponent(), args);
        } catch (IllegalArgumentException ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
            return; // without this, compiler whines about uninitialized 'srvr'
        }
        srvr.startServing();
    }

}
