package com.foogaro.hotel.workflow;

import com.foogaro.hotel.common.dto.BookWorkflowRequest;
import com.foogaro.hotel.common.model.Workflow;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/workflows")
@Tag(name = "Workflows", description = "Book and cancel orchestration across hotel, room, booking and check-in")
public class WorkflowController {

    private final WorkflowOrchestrator orchestrator;

    public WorkflowController(WorkflowOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/book")
    @ResponseStatus(HttpStatus.CREATED)
    public Workflow book(@Valid @RequestBody BookWorkflowRequest request) {
        return orchestrator.book(request);
    }

    @PostMapping("/{id}/cancel")
    public Workflow cancel(@PathVariable String id) {
        return orchestrator.cancel(id);
    }

    @GetMapping("/{id}")
    public Workflow get(@PathVariable String id) {
        return orchestrator.get(id);
    }

    @GetMapping
    public List<Workflow> list() {
        return orchestrator.listForSession();
    }
}
