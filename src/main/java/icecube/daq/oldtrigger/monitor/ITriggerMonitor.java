/*
 * interface: ITriggerMonitor
 *
 * Version $Id: ITriggerMonitor.java 14204 2013-02-11 19:52:57Z dglo $
 *
 * Date: August 22 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.oldtrigger.monitor;

import java.util.Map;

/**
 * This interface defines the control aspect of a trigger.
 *
 * @version $Id: ITriggerMonitor.java 14204 2013-02-11 19:52:57Z dglo $
 * @author pat
 */
public interface ITriggerMonitor
{

    String toString();

    int getTriggerCounter();

    boolean isOnTrigger();

    TriggerMonitor getTriggerMonitor();

    Map<String, Object> getTriggerMonitorMap();
}
