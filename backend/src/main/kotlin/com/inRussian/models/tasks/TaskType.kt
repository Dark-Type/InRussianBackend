package com.inRussian.models.tasks

import kotlinx.serialization.Serializable

@Serializable
enum class TaskType {
    WRITE, // Write.svg Пишите
    LISTEN, // Listen.svg Слушайте
    READ, // Read.svg Читайте
    SPEAK, // Speak.svg Говорите
    REPEAT, // Repeat.svg Повторяйте
    REMIND, // Remember.svg Запоминайте
    MARK, // PickRightWords.svg Выберите правильные слова
    FILL, // FillInTheBlanks.svg Заполните пропуски
    CONNECT_AUDIO, // ConnectAudioToTranslation.svg Соедините аудио с переводом
    CONNECT_IMAGE, // ConnectImageToText.svg Соедините изображение с текстом
    CONNECT_TRANSLATE, // ConnectTranslationToWord.svg Соедините перевод со словом
    SELECT, // ChooseRightVariant.svg Выберите правильный вариант
    TASK, // Task.svg Задание
    QUESTION, // Question.svg Что это?
    SET_THE_STRESS, // SetTheStress.svg
    CONTENT_BLOCKS // ContentBlocks.svg
}
