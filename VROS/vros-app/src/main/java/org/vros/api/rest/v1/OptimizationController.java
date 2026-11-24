package org.vros.api.rest.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.vros.api.dto.request.SubmitOptimizationRequest;
import org.vros.api.dto.response.OptimizationJobResponse;
import org.vros.api.dto.response.OptimizationSolutionResponse;
import org.vros.api.mapper.OptimizationDtoMapper;
import org.vros.application.service.OptimizationService;
import org.vros.domain.model.OptimizationJobStatus;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/optimization", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Optimization", description = "Submit and track VRP optimization jobs")
public class OptimizationController {

    private final OptimizationService optimizationService;

    @PostMapping(value = "/jobs", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Submit a scenario for optimization")
    public ResponseEntity<OptimizationJobResponse> submit(@Valid @RequestBody SubmitOptimizationRequest request) {
        var job = optimizationService.submit(request.getScenarioId());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{jobId}").buildAndExpand(job.getId()).toUri();
        return ResponseEntity.accepted().location(location)
                .body(OptimizationDtoMapper.toResponse(job));
    }

    @GetMapping("/jobs/{jobId}")
    @Operation(summary = "Get the status of an optimization job")
    public OptimizationJobResponse getJob(@PathVariable UUID jobId) {
        return OptimizationDtoMapper.toResponse(optimizationService.getJob(jobId));
    }

    @GetMapping("/jobs/{jobId}/solution")
    @Operation(summary = "Get the solution of a completed optimization job")
    public OptimizationSolutionResponse getSolution(@PathVariable UUID jobId) {
        var solution = optimizationService.getSolution(jobId);
        var response = OptimizationDtoMapper.toResponse(solution);
        // scenarioId is not stored on the solution; fetch it from the job for the API response.
        response.setScenarioId(optimizationService.getJob(jobId).getScenarioId());
        return response;
    }

    @GetMapping("/jobs")
    @Operation(summary = "List optimization jobs, optionally filtered by scenario or status")
    public List<OptimizationJobResponse> list(
            @RequestParam(required = false) Long scenarioId,
            @RequestParam(required = false) OptimizationJobStatus status) {
        return optimizationService.list(scenarioId, status).stream()
                .map(OptimizationDtoMapper::toResponse)
                .toList();
    }
}
