/*
 * class: Sorter
 *
 * Version $Id: Sorter.java 14204 2013-02-11 19:52:57Z dglo $
 *
 * Date: July 15 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.oldtrigger.control;

import icecube.daq.payload.IReadoutRequestElement;
import icecube.daq.payload.ITriggerRequestPayload;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.impl.UTCTime;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class is to provide the earliest/latest UTC-timeStamp.
 *
 * @version $Id: Sorter.java 14204 2013-02-11 19:52:57Z dglo $
 * @author shseo
 */
public class Sorter
{
    /**
     * Create an instance of this class.
     * Default constructor is declared, but private, to stop accidental
     * creation of an instance of the class.
     */
    public Sorter()
    {
    }

    //--todo: include excetion
    /**
     * This method is to provide time-ordered list of ReadoutElements.
     * NOTE: This method destroys the original list
     *
     * @param listReadoutElementsUTCTimeUnsorted
     * @return
     */
    public List getReadoutElementsUTCTimeSorted(List listReadoutElementsUTCTimeUnsorted)
    {
        List listReadoutElementsUTCTimeSorted = new ArrayList();

        IReadoutRequestElement element = null;
        IReadoutRequestElement earliestElement = null;
        IUTCTime earliestUTCTime = new UTCTime(Long.MAX_VALUE);
        IUTCTime tStartUTCTime = null;

        int iSizeUnsortedList = listReadoutElementsUTCTimeUnsorted.size();
        int index = -1;

        // XXX YIKES!  This needs to be changed to use Collection.sort()

        if(iSizeUnsortedList > 1)
        {
            //boolean bStop = false;
            //while(!bStop){
            while(listReadoutElementsUTCTimeSorted.size() < iSizeUnsortedList-1)
            {
                for(int i=0; i < listReadoutElementsUTCTimeUnsorted.size(); i++)
                {
                    element = (IReadoutRequestElement) listReadoutElementsUTCTimeUnsorted.get(i);

                    tStartUTCTime = element.getFirstTimeUTC();

                    if(earliestUTCTime.compareTo(tStartUTCTime) >= 0)
                    {
                        earliestElement = element;
                        //index is updated to indicate the position of the earliest UTCTime element.
                        index = i;
                        earliestUTCTime = tStartUTCTime;
                    }

                }

                listReadoutElementsUTCTimeSorted.add(earliestElement);
                listReadoutElementsUTCTimeUnsorted.remove(index);
                earliestUTCTime = new UTCTime(Long.MAX_VALUE);

            }

            listReadoutElementsUTCTimeSorted.add(listReadoutElementsUTCTimeUnsorted.get(0));

        } else
        {
            listReadoutElementsUTCTimeSorted = listReadoutElementsUTCTimeUnsorted;
        }

        return listReadoutElementsUTCTimeSorted;
    }
    /**
     * This method is to give earliest readoutTime_start in a listReadoutElements.
     *
     * @param listReadoutElements
     * @return
     */
    public IUTCTime getUTCTimeEarliest(List listReadoutElements)
    {
        return getUTCTimeEarliest(listReadoutElements, false);
    }
    public IUTCTime getUTCTimeEarliest(List listObjects, boolean isPayloadObjects)
    {
        IUTCTime UTCTime_earliest = new UTCTime(Long.MAX_VALUE);
        IUTCTime UTCTime_start = null;

        Iterator iterElements = listObjects.iterator();

        while(iterElements.hasNext())
        {
            if(isPayloadObjects)
            {
                ITriggerRequestPayload tPayload = (ITriggerRequestPayload) iterElements.next();
                UTCTime_start = tPayload.getFirstTimeUTC();

            }else{//ReadoutElement
                IReadoutRequestElement element = (IReadoutRequestElement) iterElements.next();
                UTCTime_start = element.getFirstTimeUTC();
            }

            if(UTCTime_earliest.compareTo(UTCTime_start) > 0)
            {
                UTCTime_earliest = UTCTime_start;
            }
        }
        return UTCTime_earliest;
    }
    public IUTCTime getUTCTimeLatest(List listObjects, boolean isPayloadObjects)
    {
        IUTCTime UTCTime_latest = new UTCTime(Long.MIN_VALUE);
        IUTCTime UTCTime_end = null;

        Iterator iterElements = listObjects.iterator();
        while(iterElements.hasNext())
        {
            if(isPayloadObjects)
            {
                ITriggerRequestPayload tPayload = (ITriggerRequestPayload) iterElements.next();
                UTCTime_end = tPayload.getLastTimeUTC();

            }else{

                IReadoutRequestElement element = (IReadoutRequestElement) iterElements.next();
                UTCTime_end = element.getLastTimeUTC();
            }

            if(UTCTime_latest.compareTo(UTCTime_end) < 0)
            {
                UTCTime_latest = UTCTime_end;
            }
        }

        return UTCTime_latest;
    }

    /**
     * This method is to give latest readoutTime_start in a listReadoutElements.
     *
     * @param listReadoutElements
     * @return
     */
    public IUTCTime getUTCTimeLatest(List listReadoutElements)
    {
        return getUTCTimeLatest(listReadoutElements, false);
    }

}
