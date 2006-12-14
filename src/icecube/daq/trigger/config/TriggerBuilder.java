/*
 * class: TriggerBuilder
 *
 * Version $Id: TriggerBuilder.java,v 1.18 2006/08/08 20:26:29 vav111 Exp $
 *
 * Date: August 18 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.trigger.config;

import icecube.daq.trigger.config.triggers.ActiveTriggers;
import icecube.daq.trigger.config.triggers.TriggerConfigType;
import icecube.daq.trigger.config.triggers.ParameterConfigType;
import icecube.daq.trigger.config.triggers.ReadoutConfigType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.InputStream;

import icecube.daq.trigger.exceptions.UnknownParameterException;
import icecube.daq.trigger.exceptions.IllegalParameterValueException;
import icecube.daq.payload.impl.SourceID4B;

/**
 * This class builds triggers from a trigger configuration object.
 *
 * @version $Id: TriggerBuilder.java,v 1.18 2006/08/08 20:26:29 vav111 Exp $
 * @author pat
 */
public class TriggerBuilder
{

    /**
     * Logging object for this class.
     */
    private static final Log log = LogFactory.getLog(TriggerBuilder.class);

    /**
     * Build and configure a trigger from a trigger configuration object.
     * @param triggerConfiguration TriggerConfigType
     * @return ITrigger
     */
    public static ITriggerConfig buildTrigger(TriggerConfigType triggerConfiguration) {

        // first get name of trigger and create instance of it
        String triggerName = triggerConfiguration.getTriggerName();
        String className = "icecube.daq.trigger.algorithm." + triggerName;
        if (log.isInfoEnabled()) {
            log.info("Building trigger: " + className);
        }
        ITriggerConfig trigger = null;
        try {
            trigger = (ITriggerConfig) Class.forName(className).newInstance();
        } catch (ClassNotFoundException cnfe) {
            log.error("Error building trigger " + className, cnfe);
        } catch (InstantiationException ie) {
            log.error("Error building trigger " + className, ie);
        } catch (IllegalAccessException iae) {
            log.error("Error building trigger " + className, iae);
        }

        // now configure trigger
        trigger.setTriggerType(triggerConfiguration.getTriggerType());
        trigger.setTriggerConfigId(triggerConfiguration.getTriggerConfigId());
        trigger.setSourceId(new SourceID4B(triggerConfiguration.getSourceId()));
        trigger.setTriggerName(triggerConfiguration.getTriggerName());

        Iterator paramIter = triggerConfiguration.getParameterConfig().iterator();
        while (paramIter.hasNext()) {
            ParameterConfigType paramConfig = (ParameterConfigType) paramIter.next();
            if (log.isInfoEnabled()) {
                log.info("Adding parameter " + paramConfig.getParameterName()
                         + " = " + paramConfig.getParameterValue());
            }
            TriggerParameter parameter = new TriggerParameter(paramConfig.getParameterName(),
                                                              paramConfig.getParameterValue());

            try {
                trigger.addParameter(parameter);
            } catch (UnknownParameterException e) {
                // there is an error, log it and make the trigger null
                log.error(e);
                trigger = null;
            } catch(IllegalParameterValueException e) {
                // there is an error, log it and make the trigger null
                log.error(e);
                trigger = null;
            }

        }

        Iterator readoutIter = triggerConfiguration.getReadoutConfig().iterator();
        while (readoutIter.hasNext()) {
            ReadoutConfigType readoutConfig = (ReadoutConfigType) readoutIter.next();
            TriggerReadout readout = new TriggerReadout(readoutConfig.getReadoutType(),
                                                        readoutConfig.getTimeOffset(),
                                                        readoutConfig.getTimeMinus(),
                                                        readoutConfig.getTimePlus());
            if (log.isInfoEnabled()) {
                log.info("Adding readout " + readoutConfig.getReadoutType()
                         + " = " + readoutConfig.getTimeOffset()
                         + ", " + readoutConfig.getTimeMinus() + ", " + readoutConfig.getTimePlus());
            }
            trigger.addReadout(readout);
        }

        return trigger;

    }

    /**
     * Build a list of triggers from an xml file on an input stream.
     * @param stream input stream tied to xml file
     * @return list of ITriggerConfig's
     */
    public static List buildTriggers(InputStream stream) {
        return buildTriggers(TriggerXMLParser.parse(stream));
    }

    /**
     * Build a list of triggers from an xml file given by file name.
     * @param fileName name of xml file
     * @return list of ITriggerConfig's
     */
    public static List buildTriggers(String fileName) {
        return buildTriggers(TriggerXMLParser.parse(fileName));
    }

    /**
     * Build a list of triggers from a trigger configuration object.
     * @param activeTriggers jaxb trigger configuration object
     * @return list of ITriggerConfig's
     */
    public static List buildTriggers(ActiveTriggers activeTriggers) {
        return buildTriggers(activeTriggers.getTriggerConfig());
    }

    /**
     * Build a list of triggers from a list of trigger configuration objects.
     * @param triggerConfigs list of TriggerConfigType's
     * @return list of ITriggerConfig's
     */
    public static List buildTriggers(List triggerConfigs) {
        List triggerList = new ArrayList();

        // loop over triggers
        Iterator triggerIter = triggerConfigs.iterator();
        while (triggerIter.hasNext()) {
            TriggerConfigType triggerConfiguration = (TriggerConfigType) triggerIter.next();
            ITriggerConfig trigger = buildTrigger(triggerConfiguration);

            // add configured trigger to list
            if (null != trigger) {
                triggerList.add(trigger);
            }

        }

        return triggerList;

    }
}
