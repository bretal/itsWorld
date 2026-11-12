package com.itsworld.assistant.service;

import com.itsworld.common.exception.BusinessException;

public final class AssistantErrors {
    private AssistantErrors() {}
    public static BusinessException error(String category) {
        return new BusinessException(1500, category);
    }
}
