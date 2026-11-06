package com.festiva.ai;

import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.aggregation.AggregationUpdate;
import org.springframework.data.mongodb.core.query.Query;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AiUsageBudgetTest {
    @Test
    void unavailableMongoRefusesPaidWork() {
        var mongo = mock(MongoTemplate.class);
        when(mongo.findAndModify(any(Query.class), any(AggregationUpdate.class), any(FindAndModifyOptions.class),
                eq(org.bson.Document.class), eq(AiUsageBudget.COLLECTION)))
                .thenThrow(new org.springframework.dao.DataAccessResourceFailureException("unavailable"));
        assertThat(new AiUsageBudget(mongo, 100).reserve()).isFalse();
    }
}
