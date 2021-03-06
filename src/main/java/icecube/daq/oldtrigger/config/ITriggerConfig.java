/*
 * interface: ITrigger
 *
 * Version $Id: ITriggerConfig.java 14206 2013-02-11 22:15:22Z dglo $
 *
 * Date: January 3 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.oldtrigger.config;

import icecube.daq.payload.ISourceID;
import icecube.daq.trigger.exceptions.IllegalParameterValueException;
import icecube.daq.trigger.exceptions.UnknownParameterException;

import java.util.List;

/**
 * This interface defines the configuration aspect of a trigger.
 *
 * @version $Id: ITriggerConfig.java 14206 2013-02-11 22:15:22Z dglo $
 * @author pat
 */
public interface ITriggerConfig
{

    /**
     * Is the trigger configured?
     * @return true if it is
     */
    boolean isConfigured();

    /**
     * Get trigger type.
     * @return triggerType
     */
    int getTriggerType();

    /**
     * Set trigger type.
     * @param triggerType
     */
    void setTriggerType(int triggerType);

    /**
     * Get trigger configuration id.
     * @return triggerConfigId
     */
    int getTriggerConfigId();

    /**
     * Set trigger configuration id.
     * @param triggerConfigId
     */
    void setTriggerConfigId(int triggerConfigId);

    /**
     * Get source id.
     * @return sourceId
     */
    int getSourceId();

    /**
     * Get source id.
     * @return sourceId
     */
    ISourceID getSourceObject();

    /**
     * Set source id.
     * @param sourceId
     */
    void setSourceId(ISourceID sourceId);

    /**
     * Get trigger name.
     * @return triggerName
     */
    String getTriggerName();

    /**
     * Set trigger name.
     * @param triggerName
     */
    void setTriggerName(String triggerName);

    /**
     * Add a parameter.
     * @param parameter TriggerParameter object.
     * @throws UnknownParameterException
     */
    void addParameter(TriggerParameter parameter) throws UnknownParameterException, IllegalParameterValueException;

    /**
     * Get list of trigger parameters.
     * @return parameter list
     */
    List getParameterList();

    /**
     * Add a readout.
     * @param readout TriggerReadout object.
     */
    void addReadout(TriggerReadout readout);

    /**
     * Get list of trigger readouts.
     * @return readout list
     */
    List getReadoutList();

}
