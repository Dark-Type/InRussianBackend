package com.example.models.task

import kotlinx.serialization.Serializable

@Serializable
enum class AnswerType {
    MULTIPLE_CHOICE_SHORT,
    MULTIPLE_CHOICE_LONG,
    SINGLE_CHOICE_SHORT,
    SINGLE_CHOICE_LONG,
    TEXT_INPUT,
    WORD_ORDER,
    WORD_SELECTION
}