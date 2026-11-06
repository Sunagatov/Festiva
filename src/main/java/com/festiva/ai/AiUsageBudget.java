package com.festiva.ai;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationUpdate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/** One atomic Mongo document; restarting or switching users cannot refill it. */
@Component
public class AiUsageBudget {
    static final String COLLECTION = "festiva_ai_usage";
    private final MongoTemplate mongo;
    private final long limit;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public AiUsageBudget(MongoTemplate mongo, @Value("${ai.daily-call-limit:100}") long limit) {
        this(mongo, limit, Clock.systemUTC());
    }

    AiUsageBudget(MongoTemplate mongo, long limit, Clock clock) {
        if (limit < 0) throw new IllegalArgumentException("AI daily allowance cannot be negative");
        this.mongo = mongo;
        this.limit = limit;
        this.clock = clock;
    }

    public boolean reserve() {
        if (limit == 0) return false;
        String day = LocalDate.now(clock).toString();
        var query = new BasicQuery(new Document("_id", "ics-name-extraction").append("$or", List.of(
                new Document("day", new Document("$ne", day)), new Document("used", new Document("$lt", limit)))));
        var used = new Document("$cond", List.of(new Document("$eq", List.of("$day", day)),
                new Document("$add", List.of("$used", 1)), 1));
        var update = AggregationUpdate.from(List.of((AggregationOperation) context ->
                new Document("$set", new Document("day", day).append("used", used))));
        try {
            if (mongo.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true),
                    Document.class, COLLECTION) != null) return true;
            try {
                mongo.insert(new Document("_id", "ics-name-extraction").append("day", day).append("used", 1), COLLECTION);
                return true;
            } catch (DuplicateKeyException ignored) { return false; }
        } catch (DataAccessException ignored) {
            return false; // Skip optional paid work when the allowance cannot be checked.
        }
    }
}
