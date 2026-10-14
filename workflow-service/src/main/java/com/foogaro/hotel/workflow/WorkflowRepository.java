package com.foogaro.hotel.workflow;

import com.foogaro.hotel.common.model.Workflow;
import com.foogaro.hotel.common.redis.JsonRedisStore;
import com.foogaro.hotel.common.redis.RedisKeys;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class WorkflowRepository {

    private final JsonRedisStore store;

    public WorkflowRepository(JsonRedisStore store) {
        this.store = store;
    }

    public Workflow save(Workflow workflow) {
        store.save(RedisKeys.workflow(workflow.getId()), workflow);
        store.addToSet(RedisKeys.sessionWorkflows(workflow.getSessionId()), workflow.getId());
        return workflow;
    }

    public Optional<Workflow> findById(String id) {
        return store.find(RedisKeys.workflow(id), Workflow.class);
    }

    public Workflow getRequired(String id) {
        return store.getRequired(RedisKeys.workflow(id), Workflow.class, "Workflow not found: " + id);
    }

    public List<Workflow> findBySession(String sessionId) {
        return store.findAll(RedisKeys.sessionWorkflows(sessionId), RedisKeys::workflow, Workflow.class);
    }
}
