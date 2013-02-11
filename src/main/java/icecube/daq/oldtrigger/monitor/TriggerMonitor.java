/*
 * class: TriggerMonitor
 *
 * Version $Id: TriggerMonitor.java 14204 2013-02-11 19:52:57Z dglo $
 *
 * Date: August 31 2005
 *
 * (c) 2005 IceCube Collaboration
 */

package icecube.daq.oldtrigger.monitor;

import icecube.icebucket.monitor.ScalarFlowMonitor;

/**
 * This class is a mess.
 *
 * @version $Id: TriggerMonitor.java 14204 2013-02-11 19:52:57Z dglo $
 * @author pat
 */
public class TriggerMonitor
{

    private static final float[] NULL_HISTORY = new float[]{0};

    private static final ScalarFlowMonitor NULL_FLOW = new ScalarFlowMonitor() {
        public void dispose() {}
        public long getTotal() { return 0; }
        public float[] getHistory() { return NULL_HISTORY; }
        public void measure(int count) {}
        public void reset() {}
        public float getRate() { return 0; }
    };

    private final ScalarFlowMonitor triggerCountFlow;

    private final ScalarFlowMonitor triggerByteFlow;

    public TriggerMonitor(ScalarFlowMonitor triggerCountFlow, ScalarFlowMonitor triggerByteFlow) {
        if (null == triggerCountFlow) {
            this.triggerCountFlow = NULL_FLOW;
        } else {
            this.triggerCountFlow = triggerCountFlow;
        }
        if (null == triggerByteFlow) {
            this.triggerByteFlow = NULL_FLOW;
        } else {
            this.triggerByteFlow = triggerByteFlow;
        }
    }

    public float[] getTriggerCountHistory() {
        return triggerCountFlow.getHistory();
    }

    public float getTriggerCountRate() {
        return triggerCountFlow.getRate();
    }

    public long getTriggerCountTotal() {
        return triggerCountFlow.getTotal();
    }

    public float[] getTriggerByteHistory() {
        return triggerByteFlow.getHistory();
    }

    public float getTriggerByteRate() {
        return triggerByteFlow.getRate();
    }

    public long getTriggerByteTotal() {
        return triggerByteFlow.getTotal();
    }

}
