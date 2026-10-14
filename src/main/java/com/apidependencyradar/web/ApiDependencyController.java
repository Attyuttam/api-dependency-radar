package com.apidependencyradar.web;

import com.apidependencyradar.application.ApiDependencyService;
import com.apidependencyradar.web.dto.ApiChangeResponse;
import com.apidependencyradar.web.dto.ApiDependencyResponse;
import com.apidependencyradar.web.dto.CheckResultResponse;
import com.apidependencyradar.web.dto.CreateApiDependencyRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/dependencies")
public class ApiDependencyController {

    private final ApiDependencyService service;

    public ApiDependencyController(ApiDependencyService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiDependencyResponse> create(@Valid @RequestBody CreateApiDependencyRequest request) {
        ApiDependencyResponse created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public List<ApiDependencyResponse> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public ApiDependencyResponse get(@PathVariable String id) {
        return service.get(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/changes")
    public List<ApiChangeResponse> changes(@PathVariable String id) {
        return service.listChanges(id);
    }

    @PostMapping("/{id}/check")
    public CheckResultResponse checkNow(@PathVariable String id) {
        return service.checkNow(id);
    }
}
