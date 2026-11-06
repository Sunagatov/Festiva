package com.festiva.ai;

/** Falls back to the original calendar title without a paid request. */
public class BudgetedNameExtractor implements IcsNameExtractorService {
    private final IcsNameExtractorService delegate;
    private final AiUsageBudget budget;

    public BudgetedNameExtractor(IcsNameExtractorService delegate, AiUsageBudget budget) {
        this.delegate = delegate;
        this.budget = budget;
    }

    @Override
    public String extractName(String summary) {
        if (summary == null || summary.isBlank() || summary.length() > 200 || !budget.reserve()) return summary;
        return delegate.extractName(summary);
    }
}
