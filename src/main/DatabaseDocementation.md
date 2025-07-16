# Документация базы данных для InRussian

## Архитектура курсов

### Иерархическая структура

Система использует четырёхуровневую иерархию контента:
```
Курс → Раздел → Тема → Задание
```
#### Курсы (Courses)
Верхний уровень образовательного контента. Каждый курс:
- Создаётся модераторами контента
- Имеет автора и описание
- Может быть опубликован или находиться в разработке
- Привязан к конкретному языку

#### Разделы (Sections)
Логические группы внутри курса:
- Поддерживают строгую последовательность (`Order`)
- Позволяют структурировать обучение по этапам
- Каскадно удаляются при удалении курса

#### Темы (Themes)
Тематические блоки внутри разделов:
- Сохраняют порядок, заданный модератором контента
- Группируют связанные задания
- Служат основной единицей для организации заданий

#### Задания (Tasks)

##### Основные обучающие элементы с пятью типами:

1. **LISTEN_AND_CHOOSE** - прослушивание аудио и выбор ответа
2. **READ_AND_CHOOSE** - чтение текста и выбор ответа
3. **LOOK_AND_CHOOSE** - просмотр изображений и выбор ответа
4. **MATCH_AUDIO_TEXT** - сопоставление аудио и текста
5. **MATCH_TEXT_TEXT** - сопоставление текста с текстом
#### Система ответов и проверки
##### Варианты ответов (TaskAnswerOptions)
   Поддерживает различные форматы:

- Текстовые варианты (OptionText)
- Аудио варианты (OptionAudioID)
- Комбинированные варианты (текст + аудио)
Правильность отмечается булевым полем IsCorrect
##### Типы интерфейсов ответов (AnswerType)
- MULTIPLE_CHOICE_SHORT/LONG - множественный выбор
- SINGLE_CHOICE_SHORT/LONG - одиночный выбор
- TEXT_INPUT - ввод текста
- WORD_ORDER - упорядочивание слов
- WORD_SELECTION - выбор правильных слов
#### Правильные ответы (TaskAnswers)
JSON-формат для гибкости:
```json
// Для обычных заданий
{
  "answer": "правильный ответ"
}

// Для заданий сопоставления
{
  "pairs": [
    {"audio_id": "audio_123", "text": "соответствующий текст"},
    {"left": "левый элемент", "right": "правый элемент"}
  ]
}
```

#### Система прогресса и повторений
#### Отслеживание прогресса (UserTaskProgress)
#### Статусы заданий:

- NOT_STARTED - не начато
- IN_PROGRESS - в процессе выполнения
- COMPLETED - завершено
- PENDING_RETRY - ожидает повторного выполнения
#### Ключевые поля:

- AttemptCount - количество попыток
- IsCorrect - результат последней попытки
- ShouldRetryAfterTasks - через сколько заданий показать повтор
#### Логика повторения неправильных ответов
#### Принцип работы:

При неправильном ответе задание помечается как PENDING_RETRY
- В UserTaskQueue добавляется запись с IsRetryTask = true
- Задание вставляется в очередь через 3-5 позиций от текущей
- Порядок вставки может быть случайным в заданном диапазоне

#### Таблица очереди (UserTaskQueue):
```
UserID | TaskID | QueuePosition | IsOriginalTask | IsRetryTask | OriginalTaskID
user1  | task1  | 1            | true           | false       | null
user1  | task2  | 2            | true           | false       | null  
user1  | task3  | 3            | true           | false       | null
user1  | task1  | 6            | false          | true        | task1  -- повтор
```

#### Алгоритм генерации очереди
1. Начальная загрузка: Все задания темы добавляются в порядке модератора
2. При неправильном ответе:
 - Вычисляется позиция вставки (текущая + 3-5) 
 - Создаётся запись повтора 
 - Обновляются позиции последующих заданий
3. При правильном ответе: Задание отмечается как завершённое

### Система регистрации пользователей
#### Записи на курсы (UserCourseEnrollments)
##### Отслеживание текущего положения:

- CurrentSectionID - текущий раздел пользователя
- CurrentThemeID - текущая тема пользователя
- Автоматическое продвижение по курсу
#### Завершение курса:

- Устанавливается CompletedAt при завершении всех заданий
- Может триггерить выдачу значков
### Система значков (Badges)
#### Типы значков
- COURSE_COMPLETION - за завершение курса
- SECTION_COMPLETION - за завершение секции
- STREAK - за серии правильных ответов
- ACHIEVEMENT - за достижения (которые могут в будущем понадобиться системе)
### Критерии получения
Хранятся в JSON-формате для гибкости:
```
{
  "type": "course_completion",
  "course_id": "uuid",
  "required_score": 80
}
```
### Масштабируемость и производительность
#### Индексы
- Композитные индексы для частых запросов (курс + порядок)
- Индексы по пользователям для быстрого поиска прогресса
- Индексы по статусам для фильтрации заданий
#### Каскадные удаления
- При удалении курса удаляются все связанные данные
- При удалении пользователя очищается весь его прогресс
- Сохранение целостности данных
#### Триггеры
- Автоматическое обновление UpdatedAt
- Валидация ролей пользователей
- Контроль целостности профилей


# Система накопительной статистики 

## Обзор архитектуры

Система статистики построена на принципе предвычисленных агрегированных данных, которые автоматически обновляются через триггеры базы данных. Это обеспечивает максимальную производительность GET-запросов за счет распределения вычислительной нагрузки на POST-операции.

## Принципы работы

### 1. Автоматическое обновление триггерами
- Все статистические данные обновляются автоматически при изменениях в `user_task_progress` и `user_course_enrollments`
- Исключает необходимость в дополнительной бизнес-логике на уровне приложения
- Гарантирует консистентность данных

### 2. Хранение сумм вместо средних значений
- Вместо вычисления и хранения средних значений сохраняются суммы и количества
- Позволяет легко вычислять средние на любом уровне агрегации
- Обеспечивает гибкость для различных типов аналитики

### 3. Трёхуровневая система агрегации
```
user_statistics (общая статистика пользователя)
    ↓
user_course_statistics (статистика по курсам)
    ↓  
course_statistics (общая статистика курса)
```

## Структура статистических таблиц

### user_statistics
**Назначение**: Общая накопленная статистика пользователя по всем курсам

**Ключевые поля**:
- `total_tasks_completed` - общее количество выполненных заданий
- `total_tasks_attempted` - общее количество попыток выполнения
- `total_time_spent_seconds` - суммарное время обучения в секундах
- `total_correct_answers` - количество правильных ответов
- `courses_enrolled` / `courses_completed` - счетчики курсов
- `current_streak_days` / `longest_streak_days` - данные о "стриках"

**Вычисляемые метрики**:
```sql
-- Среднее время на задание
SELECT total_time_spent_seconds / NULLIF(total_tasks_completed, 0) as avg_time_per_task
FROM user_statistics WHERE user_id = ?;

-- Процент правильных ответов
SELECT (total_correct_answers::float / NULLIF(total_tasks_attempted, 0)) * 100 as success_rate
FROM user_statistics WHERE user_id = ?;
```

### user_course_statistics
**Назначение**: Детализированная статистика пользователя по каждому курсу

**Ключевые поля**:
- `progress_percentage` - автоматически вычисляемый прогресс (tasks_completed/tasks_total*100)
- `time_spent_seconds` - время, потраченное на конкретный курс
- `tasks_total` - общее количество заданий в курсе (заполняется при записи)
- `started_at` / `completed_at` - временные метки прохождения курса

**Особенности**:
- Создается автоматически при записи пользователя на курс
- `completed_at` устанавливается автоматически при достижении 100% прогресса
- Поле `tasks_total` заполняется один раз при создании записи

### course_statistics
**Назначение**: Агрегированная статистика по курсу для всех студентов

**Ключевые поля**:
- `students_enrolled` / `students_completed` - счетчики студентов
- `students_active_last_7_days` - активные студенты (обновляется отдельно)
- `total_time_spent_seconds` - суммарное время всех студентов
- `average_completion_time_seconds` - среднее время завершения курса

**Вычисляемые метрики**:
```sql
-- Средний прогресс всех студентов
SELECT AVG(progress_percentage) as avg_progress
FROM user_course_statistics WHERE course_id = ?;

-- Процент завершения курса
SELECT (students_completed::float / NULLIF(students_enrolled, 0)) * 100 as completion_rate
FROM course_statistics WHERE course_id = ?;
```

## Логика работы триггеров

### Триггер обновления статистики заданий
**Срабатывает**: При INSERT/UPDATE в `user_task_progress`

**Условия обновления**:
1. **При завершении задания** (`status` = 'COMPLETED'):
    - Обновляются все три уровня статистики
    - Пересчитывается прогресс курса
    - При достижении 100% прогресса обновляются счетчики завершенных курсов

2. **При увеличении количества попыток**:
    - Обновляются только счетчики попыток
    - Время выполнения не пересчитывается

### Триггер записи на курс
**Срабатывает**: При INSERT в `user_course_enrollments`

**Действия**:
- Увеличивает счетчик `courses_enrolled` в `user_statistics`
- Создает запись в `user_course_statistics` с заполненным `tasks_total`
- Увеличивает счетчик `students_enrolled` в `course_statistics`

## Производительность и оптимизация

### Индексы
```sql
-- Для быстрого поиска статистики пользователя
CREATE INDEX idx_user_statistics_activity ON user_statistics(last_activity_date);

-- Для фильтрации по прогрессу
CREATE INDEX idx_user_course_stats_progress ON user_course_statistics(progress_percentage);

-- Для поиска активных пользователей
CREATE INDEX idx_user_course_stats_activity ON user_course_statistics(last_activity_at);

-- Для сортировки курсов по популярности
CREATE INDEX idx_course_statistics_enrolled ON course_statistics(students_enrolled);
```

### Типичные запросы и их производительность

**Топ студентов по активности** (O(log n)):
```sql
SELECT u.email, us.total_tasks_completed, us.last_activity_date
FROM user_statistics us
JOIN users u ON us.user_id = u.id
WHERE u.role = 'STUDENT'
ORDER BY us.total_tasks_completed DESC
LIMIT 10;
```

**Прогресс студента по всем курсам** (O(1) для каждого курса):
```sql
SELECT c.name, ucs.progress_percentage, ucs.time_spent_seconds
FROM user_course_statistics ucs
JOIN courses c ON ucs.course_id = c.id
WHERE ucs.user_id = ?
ORDER BY ucs.last_activity_at DESC;
```

**Статистика курса для экспертов** (O(1)):
```sql
SELECT 
    cs.*,
    (cs.students_completed::float / NULLIF(cs.students_enrolled, 0)) * 100 as completion_rate,
    cs.total_time_spent_seconds / NULLIF(cs.students_enrolled, 0) as avg_time_per_student
FROM course_statistics cs
WHERE cs.course_id = ?;
```

## Примеры использования в API

### Дашборд администратора
```sql
-- Общая статистика системы
SELECT 
    COUNT(*) as total_users,
    SUM(courses_enrolled) as total_enrollments,
    AVG(total_tasks_completed) as avg_tasks_per_user
FROM user_statistics us
JOIN users u ON us.user_id = u.id
WHERE u.role = 'STUDENT';
```

### Дашборд эксперта
```sql
-- Топ курсов по количеству студентов
SELECT c.name, cs.students_enrolled, cs.students_completed
FROM course_statistics cs
JOIN courses c ON cs.course_id = c.id
ORDER BY cs.students_enrolled DESC
LIMIT 5;
```

### Профиль студента
```sql
-- Полная статистика студента
SELECT 
    us.*,
    (us.total_correct_answers::float / NULLIF(us.total_tasks_attempted, 0)) * 100 as success_rate,
    us.total_time_spent_seconds / NULLIF(us.total_tasks_completed, 0) as avg_time_per_task
FROM user_statistics us
WHERE us.user_id = ?;
```

## Масштабирование и обслуживание(на будущее)

### Партицирование (для больших объёмов)
```sql
-- Партицирование user_course_statistics по course_id
ALTER TABLE user_course_statistics PARTITION BY HASH (course_id);
```

### Архивирование старых данных
```sql
-- Перенос завершённых курсов старше года в архивную таблицу
CREATE TABLE user_course_statistics_archive (LIKE user_course_statistics);

INSERT INTO user_course_statistics_archive
SELECT * FROM user_course_statistics 
WHERE completed_at < NOW() - INTERVAL '1 year';
```

### Мониторинг производительности
```sql
-- Проверка размера статистических таблиц
SELECT 
    schemaname,
    tablename,
    attname,
    n_distinct,
    correlation
FROM pg_stats 
WHERE tablename IN ('user_statistics', 'user_course_statistics', 'course_statistics');
```

## Обработка ошибок и консистентность

### Механизмы защиты
1. **UPSERT операции** - используется `ON CONFLICT DO UPDATE` для предотвращения дублирования
2. **Проверка деления на ноль** - используется `NULLIF()` во всех вычислениях
3. **Транзакционность** - все операции в триггерах выполняются в одной транзакции
4. **Валидация данных** - проверка существования связанных записей

### Восстановление статистики
```sql
-- Процедура полного пересчёта статистики (для критических случаев)
CREATE OR REPLACE FUNCTION recalculate_all_statistics()
RETURNS VOID AS $$
BEGIN
    -- Очистка существующих данных
    TRUNCATE user_statistics, user_course_statistics, course_statistics;
    
    -- Пересчёт на основе user_task_progress и user_course_enrollments
    -- (детальная реализация зависит от требований к downtime)
END;
$$ LANGUAGE plpgsql;
```



# SQL запрос для создания таблиц, диалект PostgreSQL
```sql
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Все пользователи
CREATE TABLE users (
                       id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                       email VARCHAR(255) UNIQUE NOT NULL,
                       phone VARCHAR(50),
                       role VARCHAR(20) NOT NULL CHECK (role IN ('STUDENT', 'EXPERT', 'CONTENT_MODERATOR', 'ADMIN')),
                       system_language VARCHAR(20) NOT NULL CHECK (system_language IN ('RUSSIAN', 'UZBEK', 'CHINESE', 'HINDI', 'TAJIK', 'ENGLISH')),
                       avatar_id VARCHAR(255),
                       last_activity_at TIMESTAMP WITH TIME ZONE,
                       created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Только для студентов
CREATE TABLE user_profiles (
                               user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
                               surname VARCHAR(100) NOT NULL,
                               name VARCHAR(100) NOT NULL,
                               patronymic VARCHAR(100),
                               gender VARCHAR(10) NOT NULL CHECK (gender IN ('MALE', 'FEMALE')),
                               dob DATE NOT NULL,
                               dor DATE NOT NULL,
                               citizenship VARCHAR(100),
                               nationality VARCHAR(100),
                               country_of_residence VARCHAR(100),
                               city_of_residence VARCHAR(100),
                               country_during_education VARCHAR(100),
                               period_spent VARCHAR(20) CHECK (period_spent IN ('MONTH-', '6MONTHS-', 'YEAR-', 'YEAR+', '5YEAR+', 'NEVER')),
                               kind_of_activity VARCHAR(255),
                               education VARCHAR(255),
                               purpose_of_register VARCHAR(255)
);

-- Только для сотрудников
CREATE TABLE staff_profiles (
                                user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
                                name VARCHAR(100) NOT NULL,
                                surname VARCHAR(100) NOT NULL,
                                patronymic VARCHAR(100)
);

-- Общая таблица для языковых навыков пользователей
CREATE TABLE user_language_skills (
                                      user_id UUID REFERENCES users(id) ON DELETE CASCADE,
                                      language VARCHAR(50) NOT NULL,
                                      understands BOOLEAN DEFAULT FALSE,
                                      speaks BOOLEAN DEFAULT FALSE,
                                      reads BOOLEAN DEFAULT FALSE,
                                      writes BOOLEAN DEFAULT FALSE,
                                      PRIMARY KEY (user_id, language)
);

-- Курсы, создаваемые модераторами 
CREATE TABLE courses (
                         id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                         name VARCHAR(255) NOT NULL,
                         description TEXT,
                         author_id UUID NOT NULL REFERENCES users(id),
                         author_url VARCHAR(500),
                         language VARCHAR(50) NOT NULL, -- язык курса
                         is_published BOOLEAN DEFAULT FALSE,
                         created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Секции внутри курсов
CREATE TABLE sections (
                          id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                          course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
                          name VARCHAR(255) NOT NULL,
                          description TEXT,
                          order_num INTEGER NOT NULL,
                          created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                          UNIQUE(course_id, order_num)
);

-- Темы внутри секций
CREATE TABLE themes (
                        id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                        section_id UUID NOT NULL REFERENCES sections(id) ON DELETE CASCADE,
                        name VARCHAR(255) NOT NULL,
                        description TEXT,
                        order_num INTEGER NOT NULL,
                        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                        UNIQUE(section_id, order_num)
);

-- Задания внутри тем
CREATE TABLE tasks (
                       id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                       theme_id UUID NOT NULL REFERENCES themes(id) ON DELETE CASCADE,
                       name VARCHAR(255) NOT NULL,
                       task_type VARCHAR(50) NOT NULL CHECK (task_type IN ('LISTEN_AND_CHOOSE', 'READ_AND_CHOOSE', 'LOOK_AND_CHOOSE', 'MATCH_AUDIO_TEXT', 'MATCH_TEXT_TEXT')),
                       question TEXT NOT NULL,
                       instructions TEXT,
                       is_training BOOLEAN DEFAULT FALSE,
                       order_num INTEGER NOT NULL,
                       created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                       UNIQUE(theme_id, order_num)
);

-- Контент для заданий, аудио, изображения, текст и видео
CREATE TABLE task_content (
                              id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                              task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
                              content_type VARCHAR(20) NOT NULL CHECK (content_type IN ('AUDIO', 'IMAGE', 'TEXT', 'VIDEO')),
                              content_id VARCHAR(255),
                              description TEXT,
                              transcription TEXT, -- для аудио и видео
                              translation TEXT, -- для аудио, видео и текста
                              order_num INTEGER NOT NULL
);

-- Варианты ответов для заданий
CREATE TABLE task_answer_options (
                                     id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                     task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
                                     option_text TEXT,
                                     option_audio_id VARCHAR(255),
                                     is_correct BOOLEAN NOT NULL DEFAULT FALSE,
                                     order_num INTEGER NOT NULL
);

-- Конфигурация правильных ответов для заданий
CREATE TABLE task_answers (
                              id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                              task_id UUID UNIQUE NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
                              answer_type VARCHAR(30) NOT NULL CHECK (answer_type IN ('MULTIPLE_CHOICE_SHORT', 'MULTIPLE_CHOICE_LONG', 'SINGLE_CHOICE_SHORT', 'SINGLE_CHOICE_LONG', 'TEXT_INPUT', 'WORD_ORDER', 'WORD_SELECTION')),
                              correct_answer JSONB NOT NULL
);

-- Запись пользователей на курсы
CREATE TABLE user_course_enrollments (
                                         user_id UUID REFERENCES users(id) ON DELETE CASCADE,
                                         course_id UUID REFERENCES courses(id) ON DELETE CASCADE,
                                         enrolled_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                         completed_at TIMESTAMP WITH TIME ZONE,
                                         current_section_id UUID REFERENCES sections(id),
                                         current_theme_id UUID REFERENCES themes(id),
                                         PRIMARY KEY (user_id, course_id)
);

-- Прогресс пользователей по курсам
CREATE TABLE user_task_progress (
                                    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
                                    task_id UUID REFERENCES tasks(id) ON DELETE CASCADE,
                                    status VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED' CHECK (status IN ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED', 'PENDING_RETRY')),
                                    attempt_count INTEGER DEFAULT 0,
                                    is_correct BOOLEAN, -- null если не начато
                                    last_attempt_at TIMESTAMP WITH TIME ZONE,
                                    completed_at TIMESTAMP WITH TIME ZONE,
                                    should_retry_after_tasks INTEGER, -- для системы повторов
                                    PRIMARY KEY (user_id, task_id)
);

-- Очередь заданий для пользователей
CREATE TABLE user_task_queue (
                                 id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                 user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                 task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
                                 theme_id UUID NOT NULL REFERENCES themes(id) ON DELETE CASCADE,
                                 queue_position INTEGER NOT NULL,
                                 is_original_task BOOLEAN DEFAULT TRUE,
                                 is_retry_task BOOLEAN DEFAULT FALSE,
                                 original_task_id UUID REFERENCES tasks(id), -- Отсылает к основному заданию, если это повтор
                                 created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Badges
CREATE TABLE badges (
                        id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                        name VARCHAR(255) NOT NULL,
                        description TEXT,
                        image_id VARCHAR(255) NOT NULL,
                        badge_type VARCHAR(30) NOT NULL CHECK (badge_type IN ('COURSE_COMPLETION', 'THEME_COMPLETION', 'STREAK', 'ACHIEVEMENT')),
                        criteria JSONB -- JSON формат для гибкости критериев получения значка
);

-- User badges
CREATE TABLE user_badges (
                             user_id UUID REFERENCES users(id) ON DELETE CASCADE,
                             badge_id UUID REFERENCES badges(id) ON DELETE CASCADE,
                             earned_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                             course_id UUID REFERENCES courses(id), -- nullable, если значок не привязан к курсу
                             theme_id UUID REFERENCES themes(id), -- nullable, если значок не привязан к теме
                             PRIMARY KEY (user_id, badge_id)
);

-- Медиафайлы, загружаемые пользователями
CREATE TABLE media_files (
                             id VARCHAR(255) PRIMARY KEY,
                             file_name VARCHAR(500) NOT NULL,
                             file_type VARCHAR(20) NOT NULL CHECK (file_type IN ('IMAGE', 'AUDIO', 'VIDEO')),
                             mime_type VARCHAR(100) NOT NULL,
                             file_size BIGINT NOT NULL,
                             uploaded_by UUID REFERENCES users(id),
                             uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                             is_active BOOLEAN DEFAULT TRUE
);
-- Накопительные статистические таблицы

-- Общая статистика пользователя (обновляется триггерами)
CREATE TABLE user_statistics (
                                user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
                                total_tasks_completed INTEGER DEFAULT 0,
                                total_tasks_attempted INTEGER DEFAULT 0,
                                total_time_spent_seconds INTEGER DEFAULT 0, -- сумма всего времени
                                total_correct_answers INTEGER DEFAULT 0,
                                courses_enrolled INTEGER DEFAULT 0,
                                courses_completed INTEGER DEFAULT 0,
                                current_streak_days INTEGER DEFAULT 0,
                                longest_streak_days INTEGER DEFAULT 0,
                                last_activity_date DATE,
                                updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Статистика пользователя по конкретному курсу (обновляется триггерами)  
CREATE TABLE user_course_statistics (
                                       user_id UUID REFERENCES users(id) ON DELETE CASCADE,
                                       course_id UUID REFERENCES courses(id) ON DELETE CASCADE,
                                       tasks_completed INTEGER DEFAULT 0,
                                       tasks_attempted INTEGER DEFAULT 0,
                                       tasks_total INTEGER, -- общее количество заданий в курсе (заполняется при записи)
                                       time_spent_seconds INTEGER DEFAULT 0, -- сумма времени на этот курс
                                       correct_answers INTEGER DEFAULT 0,
                                       progress_percentage DECIMAL(5,2) DEFAULT 0, -- вычисляется как tasks_completed/tasks_total*100
                                       started_at TIMESTAMP WITH TIME ZONE,
                                       last_activity_at TIMESTAMP WITH TIME ZONE,
                                       completed_at TIMESTAMP WITH TIME ZONE,
                                       updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                       PRIMARY KEY (user_id, course_id)
);

-- Общая статистика курса (обновляется триггерами)
CREATE TABLE course_statistics (
                                  course_id UUID PRIMARY KEY REFERENCES courses(id) ON DELETE CASCADE,
                                  students_enrolled INTEGER DEFAULT 0,
                                  students_completed INTEGER DEFAULT 0,
                                  students_active_last_7_days INTEGER DEFAULT 0,
                                  total_tasks_completed INTEGER DEFAULT 0,
                                  total_time_spent_seconds INTEGER DEFAULT 0, -- сумма времени всех студентов
                                  total_attempts INTEGER DEFAULT 0,
                                  total_correct_answers INTEGER DEFAULT 0,
                                  average_completion_time_seconds INTEGER DEFAULT 0, -- среднее время завершения курса
                                  updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Индексы для статистических таблиц
CREATE INDEX idx_user_statistics_activity ON user_statistics(last_activity_date);
CREATE INDEX idx_user_course_stats_progress ON user_course_statistics(progress_percentage);
CREATE INDEX idx_user_course_stats_activity ON user_course_statistics(last_activity_at);
CREATE INDEX idx_course_statistics_enrolled ON course_statistics(students_enrolled);


-- Индексы для лучшей производительности
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_sections_course_order ON sections(course_id, order_num);
CREATE INDEX idx_themes_section_order ON themes(section_id, order_num);
CREATE INDEX idx_tasks_theme_order ON tasks(theme_id, order_num);
CREATE INDEX idx_task_content_task_order ON task_content(task_id, order_num);
CREATE INDEX idx_task_answer_options_task_order ON task_answer_options(task_id, order_num);
CREATE INDEX idx_user_course_enrollments_user ON user_course_enrollments(user_id);
CREATE INDEX idx_user_task_progress_user ON user_task_progress(user_id);
CREATE INDEX idx_user_task_progress_status ON user_task_progress(status);
CREATE INDEX idx_user_task_queue_user_position ON user_task_queue(user_id, queue_position);
CREATE INDEX idx_user_badges_user ON user_badges(user_id);
CREATE INDEX idx_media_files_type ON media_files(file_type);
CREATE INDEX idx_media_files_uploaded_by ON media_files(uploaded_by);



-- Триггеры на обновления
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_courses_updated_at BEFORE UPDATE ON courses FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Функции для проверки ролей (вместо constraint с подзапросами)
CREATE OR REPLACE FUNCTION check_student_profile()
RETURNS TRIGGER AS $$
BEGIN
    IF (SELECT role FROM users WHERE id = NEW.user_id) != 'STUDENT' THEN
        RAISE EXCEPTION 'User profile can only be created for users with STUDENT role';
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION check_staff_profile()
RETURNS TRIGGER AS $$
BEGIN
    IF (SELECT role FROM users WHERE id = NEW.user_id) NOT IN ('EXPERT', 'CONTENT_MODERATOR', 'ADMIN') THEN
        RAISE EXCEPTION 'Staff profile can only be created for users with EXPERT, CONTENT_MODERATOR, or ADMIN role';
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Триггеры для проверки ролей
CREATE TRIGGER check_student_profile_trigger
    BEFORE INSERT OR UPDATE ON user_profiles
                         FOR EACH ROW EXECUTE FUNCTION check_student_profile();

CREATE TRIGGER check_staff_profile_trigger
    BEFORE INSERT OR UPDATE ON staff_profiles
                         FOR EACH ROW EXECUTE FUNCTION check_staff_profile();


-- Триггер для обновления накопительной статистики при завершении заданий
CREATE OR REPLACE FUNCTION update_accumulated_statistics()
RETURNS TRIGGER AS $$
DECLARE
course_uuid UUID;
    total_course_tasks INTEGER;
    user_completed_tasks INTEGER;
    new_progress DECIMAL(5,2);
    time_spent INTEGER;
    old_attempt_count INTEGER;
BEGIN
    -- Получаем course_id через JOIN'ы
SELECT c.id INTO course_uuid
FROM tasks t
        JOIN themes th ON t.theme_id = th.id
        JOIN sections s ON th.section_id = s.id
        JOIN courses c ON s.course_id = c.id
WHERE t.id = NEW.task_id;

-- Безопасное получение старого значения attempt_count
old_attempt_count := COALESCE(OLD.attempt_count, 0);

    -- Вычисляем время выполнения (разница между попытками)
    time_spent := COALESCE(
        EXTRACT(EPOCH FROM (NEW.last_attempt_at - COALESCE(OLD.last_attempt_at, NEW.last_attempt_at)))::INTEGER, 0
    );

    -- При завершении задания (только при переходе в COMPLETED)
    IF NEW.status = 'COMPLETED' AND (OLD IS NULL OR OLD.status != 'COMPLETED') THEN
        
        -- 1. Обновляем общую статистику пользователя
        INSERT INTO user_statistics (
            user_id, total_tasks_completed, total_tasks_attempted, 
            total_time_spent_seconds, total_correct_answers, 
            last_activity_date, updated_at
        )
        VALUES (
            NEW.user_id, 1, NEW.attempt_count, time_spent,
            CASE WHEN NEW.is_correct THEN 1 ELSE 0 END, 
            CURRENT_DATE, CURRENT_TIMESTAMP
        )
        ON CONFLICT (user_id) DO UPDATE SET
   total_tasks_completed = user_statistics.total_tasks_completed + 1,
                                                                            total_tasks_attempted = user_statistics.total_tasks_attempted + (NEW.attempt_count - old_attempt_count),
                                                                            total_time_spent_seconds = user_statistics.total_time_spent_seconds + time_spent,
                                                                            total_correct_answers = user_statistics.total_correct_answers +
                                                                            CASE WHEN NEW.is_correct THEN 1 ELSE 0 END,
            last_activity_date = CURRENT_DATE,
            updated_at = CURRENT_TIMESTAMP;

        -- 2. Получаем общее количество заданий в курсе
SELECT COUNT(*) INTO total_course_tasks
FROM tasks t
        JOIN themes th ON t.theme_id = th.id
        JOIN sections s ON th.section_id = s.id
WHERE s.course_id = course_uuid;

-- 3. Получаем количество завершенных заданий пользователем в этом курсе
SELECT COUNT(*) INTO user_completed_tasks
FROM user_task_progress utp
        JOIN tasks t ON utp.task_id = t.id
        JOIN themes th ON t.theme_id = th.id
        JOIN sections s ON th.section_id = s.id
WHERE s.course_id = course_uuid
  AND utp.user_id = NEW.user_id
  AND utp.status = 'COMPLETED';

-- Вычисляем прогресс
new_progress := CASE 
            WHEN total_course_tasks > 0 THEN 
                ROUND((user_completed_tasks::DECIMAL / total_course_tasks::DECIMAL) * 100, 2)
            ELSE 0
END;

        -- 4. Обновляем статистику пользователя по курсу
INSERT INTO user_course_statistics (
   user_id, course_id, tasks_completed, tasks_attempted,
   tasks_total, time_spent_seconds, correct_answers,
   progress_percentage, last_activity_at, updated_at
)
VALUES (
          NEW.user_id, course_uuid, 1, NEW.attempt_count, total_course_tasks,
          time_spent, CASE WHEN NEW.is_correct THEN 1 ELSE 0 END,
          new_progress, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
       )
   ON CONFLICT (user_id, course_id) DO UPDATE SET
   tasks_completed = user_course_statistics.tasks_completed + 1,
                                          tasks_attempted = user_course_statistics.tasks_attempted + (NEW.attempt_count - old_attempt_count),
                                          time_spent_seconds = user_course_statistics.time_spent_seconds + time_spent,
                                          correct_answers = user_course_statistics.correct_answers +
                                          CASE WHEN NEW.is_correct THEN 1 ELSE 0 END,
            progress_percentage = new_progress,
            last_activity_at = CURRENT_TIMESTAMP,
            updated_at = CURRENT_TIMESTAMP,
            completed_at = CASE WHEN new_progress >= 100 THEN CURRENT_TIMESTAMP 
                              ELSE user_course_statistics.completed_at END;

        -- 5. Обновляем общую статистику курса
INSERT INTO course_statistics (
   course_id, total_tasks_completed, total_attempts,
   total_time_spent_seconds, total_correct_answers, updated_at
)
VALUES (
          course_uuid, 1, NEW.attempt_count, time_spent,
          CASE WHEN NEW.is_correct THEN 1 ELSE 0 END, CURRENT_TIMESTAMP
       )
   ON CONFLICT (course_id) DO UPDATE SET
   total_tasks_completed = course_statistics.total_tasks_completed + 1,
                                 total_attempts = course_statistics.total_attempts + (NEW.attempt_count - old_attempt_count),
                                 total_time_spent_seconds = course_statistics.total_time_spent_seconds + time_spent,
                                 total_correct_answers = course_statistics.total_correct_answers +
                                 CASE WHEN NEW.is_correct THEN 1 ELSE 0 END,
            updated_at = CURRENT_TIMESTAMP;

        -- 6. Обновляем счетчики завершенных курсов при 100% прогрессе
        IF new_progress >= 100 THEN
UPDATE user_statistics
SET courses_completed = courses_completed + 1
WHERE user_id = NEW.user_id
  AND NOT EXISTS (
   SELECT 1 FROM user_course_statistics
   WHERE user_id = NEW.user_id AND course_id = course_uuid AND completed_at IS NOT NULL
);

UPDATE course_statistics
SET students_completed = students_completed + 1
WHERE course_id = course_uuid;
END IF;

    -- При обновлении attempt_count (но не завершении) - обновляем только попытки
    ELSIF OLD IS NOT NULL AND NEW.attempt_count > OLD.attempt_count THEN

UPDATE user_statistics
SET total_tasks_attempted = total_tasks_attempted + (NEW.attempt_count - OLD.attempt_count),
    updated_at = CURRENT_TIMESTAMP
WHERE user_id = NEW.user_id;

UPDATE user_course_statistics
SET tasks_attempted = tasks_attempted + (NEW.attempt_count - OLD.attempt_count),
    updated_at = CURRENT_TIMESTAMP
WHERE user_id = NEW.user_id AND course_id = course_uuid;

UPDATE course_statistics
SET total_attempts = total_attempts + (NEW.attempt_count - OLD.attempt_count),
    updated_at = CURRENT_TIMESTAMP
WHERE course_id = course_uuid;

END IF;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_accumulated_statistics
   AFTER INSERT OR UPDATE ON user_task_progress
                      FOR EACH ROW
                      EXECUTE FUNCTION update_accumulated_statistics();

-- Триггер для обновления статистики при записи на курс
CREATE OR REPLACE FUNCTION update_course_enrollment_statistics()
RETURNS TRIGGER AS $$
DECLARE
total_course_tasks INTEGER;
BEGIN
    -- Получаем общее количество заданий в курсе
SELECT COUNT(*) INTO total_course_tasks
FROM tasks t
        JOIN themes th ON t.theme_id = th.id
        JOIN sections s ON th.section_id = s.id
WHERE s.course_id = NEW.course_id;

-- Обновляем статистику пользователя
INSERT INTO user_statistics (user_id, courses_enrolled, updated_at)
VALUES (NEW.user_id, 1, CURRENT_TIMESTAMP)
   ON CONFLICT (user_id) DO UPDATE SET
   courses_enrolled = user_statistics.courses_enrolled + 1,
                               updated_at = CURRENT_TIMESTAMP;

-- Создаем запись статистики по курсу для пользователя
INSERT INTO user_course_statistics (
   user_id, course_id, tasks_total, started_at, updated_at
)
VALUES (NEW.user_id, NEW.course_id, total_course_tasks, NEW.enrolled_at, CURRENT_TIMESTAMP)
   ON CONFLICT (user_id, course_id) DO UPDATE SET
   started_at = COALESCE(user_course_statistics.started_at, NEW.enrolled_at),
                                          updated_at = CURRENT_TIMESTAMP;

-- Обновляем статистику курса
INSERT INTO course_statistics (course_id, students_enrolled, updated_at)
VALUES (NEW.course_id, 1, CURRENT_TIMESTAMP)
   ON CONFLICT (course_id) DO UPDATE SET
   students_enrolled = course_statistics.students_enrolled + 1,
                                 updated_at = CURRENT_TIMESTAMP;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_enrollment_statistics
   AFTER INSERT ON user_course_enrollments
   FOR EACH ROW
   EXECUTE FUNCTION update_course_enrollment_statistics();

```