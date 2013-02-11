/*
 * interface: IPayloadProducer
 *
 * Version $Id: IPayloadProducer.java 14204 2013-02-11 19:52:57Z dglo $
 *
 * Date: October 19 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.oldtrigger.control;

import icecube.daq.io.DAQComponentOutputProcess;

/**
 * This interface provides methods for setting payload destinations.
 *
 * @version $Id: IPayloadProducer.java 14204 2013-02-11 19:52:57Z dglo $
 * @author pat
 */
public interface IPayloadProducer
{

    void setPayloadOutput(DAQComponentOutputProcess outProc);

}
