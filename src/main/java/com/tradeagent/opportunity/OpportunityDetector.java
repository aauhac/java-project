package com.tradeagent.opportunity;

import java.util.List;

public interface OpportunityDetector<T extends OpportunityCase> {

    List<T> detect(Long userId);
}
