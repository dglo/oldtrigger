package icecube.daq.oldtrigger.control;

import icecube.daq.io.DAQOutputChannelManager;
import icecube.daq.oldpayload.impl.MasterPayloadFactory;

/**
 * Created by IntelliJ IDEA.
 * User: toale
 * Date: Mar 30, 2007
 * Time: 1:29:30 PM
 */
public interface IStringTriggerHandler
        extends DAQOutputChannelManager, ITriggerHandler
{

    void setMasterPayloadFactory(MasterPayloadFactory factory);


}
