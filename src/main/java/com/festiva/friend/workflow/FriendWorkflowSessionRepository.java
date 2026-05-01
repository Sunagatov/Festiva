package com.festiva.friend.workflow;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface FriendWorkflowSessionRepository extends MongoRepository<FriendWorkflowSession, Long> {
}
