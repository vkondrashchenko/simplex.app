package com.symplex.lambda.dto;

import java.util.List;

public record ParseResponse(int varCount, int monomialCount, List<String> variableNames) {}
