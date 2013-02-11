package icecube.daq.oldtrigger.component;

import java.util.HashMap;

public interface AnalysisMBean
{
    HashMap getHitSources();
    long getIgnoredCount();
    int getNumHitsPerTrigger();
    long getTotalHitCount();
    int getTriggerCount();
}
