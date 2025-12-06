package com.inRussian.utils.validation

class ValidationException(val errors: List<FieldError>) : RuntimeException("validation_failed")