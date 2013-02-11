package icecube.daq.oldtrigger.algorithm;

import icecube.daq.oldtrigger.config.ITriggerConfig;
import icecube.daq.oldtrigger.control.ITriggerControl;
import icecube.daq.oldtrigger.monitor.ITriggerMonitor;

public interface ITrigger
    extends ITriggerConfig, ITriggerControl, ITriggerMonitor
{
}
