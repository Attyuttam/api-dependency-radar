package com.apidependencyradar.domain.detector;

import com.apidependencyradar.domain.model.ApiChange;

import java.util.List;

public interface ApiChangeDetector {

    List<ApiChange> detect(String oldSpec, String newSpec);
}