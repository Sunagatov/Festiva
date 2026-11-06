package com.festiva.ai;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class BudgetedNameExtractorTest {
    @Test
    void exhaustionSkipsProviderAndKeepsOriginalTitle() {
        var budget = mock(AiUsageBudget.class);
        var delegate = mock(IcsNameExtractorService.class);
        assertThat(new BudgetedNameExtractor(delegate, budget).extractName("Birthday of Alice"))
                .isEqualTo("Birthday of Alice");
        verifyNoInteractions(delegate);
    }

    @Test
    void inputBoundIsCheckedBeforeBudgetAndApprovedCallIsMadeOnce() {
        var budget = mock(AiUsageBudget.class);
        var delegate = mock(IcsNameExtractorService.class);
        var extractor = new BudgetedNameExtractor(delegate, budget);
        assertThat(extractor.extractName("a".repeat(201))).hasSize(201);
        verifyNoInteractions(budget, delegate);
        when(budget.reserve()).thenReturn(true);
        when(delegate.extractName("Birthday of Alice")).thenReturn("Alice");
        assertThat(extractor.extractName("Birthday of Alice")).isEqualTo("Alice");
        verify(budget).reserve();
        verify(delegate).extractName("Birthday of Alice");
    }
}
