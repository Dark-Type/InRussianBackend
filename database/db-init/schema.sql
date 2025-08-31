--
-- PostgreSQL database dump
--

\restrict a2xyhP44loKpweYCbSFFDvmMTxLWrBVb0aqn6M5ML6prBQfKR6r8ZRgWk4AA2xj

-- Dumped from database version 16.4 (Postgres.app)
-- Dumped by pg_dump version 16.10 (Homebrew)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: uuid-ossp; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;


--
-- Name: EXTENSION "uuid-ossp"; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION "uuid-ossp" IS 'generate universally unique identifiers (UUIDs)';


--
-- Name: user_status; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.user_status AS ENUM (
    'ACTIVE',
    'SUSPENDED',
    'DEACTIVATED',
    'PENDING_VERIFICATION'
);


ALTER TYPE public.user_status OWNER TO postgres;

--
-- Name: check_staff_profile(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.check_staff_profile() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF (SELECT role FROM users WHERE id = NEW.user_id) NOT IN ('EXPERT', 'CONTENT_MODERATOR', 'ADMIN') THEN
        RAISE EXCEPTION 'Staff profile can only be created for users with EXPERT, CONTENT_MODERATOR, or ADMIN role';
    END IF;
    RETURN NEW;
END;
$$;


ALTER FUNCTION public.check_staff_profile() OWNER TO postgres;

--
-- Name: check_student_profile(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.check_student_profile() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF (SELECT role FROM users WHERE id = NEW.user_id) != 'STUDENT' THEN
        RAISE EXCEPTION 'User profile can only be created for users with STUDENT role';
    END IF;
    RETURN NEW;
END;
$$;


ALTER FUNCTION public.check_student_profile() OWNER TO postgres;

--
-- Name: create_course_statistics(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.create_course_statistics() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    INSERT INTO course_statistics(course_id)
    VALUES (NEW.id);
    RETURN NEW;
END;
$$;


ALTER FUNCTION public.create_course_statistics() OWNER TO postgres;

--
-- Name: create_user_statistics(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.create_user_statistics() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    INSERT INTO user_statistics(user_id)
    VALUES (NEW.id);
    RETURN NEW;
END;
$$;


ALTER FUNCTION public.create_user_statistics() OWNER TO postgres;

--
-- Name: decrement_course_enrollment_statistics(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.decrement_course_enrollment_statistics() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    -- Уменьшаем счетчик у пользователя
    UPDATE user_statistics
    SET courses_enrolled = courses_enrolled - 1,
        updated_at = CURRENT_TIMESTAMP
    WHERE user_id = OLD.user_id;

    -- Уменьшаем счетчик у курса
    UPDATE course_statistics
    SET students_enrolled = students_enrolled - 1,
        updated_at = CURRENT_TIMESTAMP
    WHERE course_id = OLD.course_id;

    RETURN OLD;
END;
$$;


ALTER FUNCTION public.decrement_course_enrollment_statistics() OWNER TO postgres;

--
-- Name: update_accumulated_statistics(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.update_accumulated_statistics() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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
$$;


ALTER FUNCTION public.update_accumulated_statistics() OWNER TO postgres;

--
-- Name: update_course_enrollment_statistics(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.update_course_enrollment_statistics() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    -- Увеличиваем счетчик у пользователя
    UPDATE user_statistics
    SET courses_enrolled = courses_enrolled + 1,
        updated_at = CURRENT_TIMESTAMP
    WHERE user_id = NEW.user_id;

    -- Увеличиваем счетчик у курса
    UPDATE course_statistics
    SET students_enrolled = students_enrolled + 1,
        updated_at = CURRENT_TIMESTAMP
    WHERE course_id = NEW.course_id;

    RETURN NEW;
END;
$$;


ALTER FUNCTION public.update_course_enrollment_statistics() OWNER TO postgres;

--
-- Name: update_updated_at_column(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.update_updated_at_column() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;


ALTER FUNCTION public.update_updated_at_column() OWNER TO postgres;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: badges; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.badges (
    id uuid NOT NULL,
    name character varying(255) NOT NULL,
    description text,
    image_id character varying(255) NOT NULL,
    badge_type character varying(30) NOT NULL,
    criteria jsonb NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.badges OWNER TO postgres;

--
-- Name: course_statistics; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.course_statistics (
    course_id uuid NOT NULL,
    students_enrolled integer DEFAULT 0 NOT NULL,
    students_completed integer DEFAULT 0 NOT NULL,
    students_active_last_7_days integer DEFAULT 0 NOT NULL,
    total_tasks_completed integer DEFAULT 0 NOT NULL,
    total_time_spent_seconds integer DEFAULT 0 NOT NULL,
    total_attempts integer DEFAULT 0 NOT NULL,
    total_correct_answers integer DEFAULT 0 NOT NULL,
    average_completion_time_seconds integer DEFAULT 0 NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.course_statistics OWNER TO postgres;

--
-- Name: courses; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.courses (
    id uuid NOT NULL,
    name character varying(255) NOT NULL,
    description text,
    author_id uuid NOT NULL,
    author_url character varying(500),
    language character varying(50) NOT NULL,
    is_published boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    poster_id uuid
);


ALTER TABLE public.courses OWNER TO postgres;

--
-- Name: media_files; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.media_files (
    id uuid NOT NULL,
    file_name character varying(500) NOT NULL,
    file_type character varying(20) NOT NULL,
    mime_type character varying(100) NOT NULL,
    file_size bigint NOT NULL,
    uploaded_by uuid,
    uploaded_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_active boolean DEFAULT true NOT NULL
);


ALTER TABLE public.media_files OWNER TO postgres;

--
-- Name: reports; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.reports (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    description character varying(1000) NOT NULL,
    task_id uuid NOT NULL,
    reporter_id uuid NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.reports OWNER TO postgres;

--
-- Name: sections; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.sections (
    id uuid NOT NULL,
    course_id uuid NOT NULL,
    name character varying(255) NOT NULL,
    description text,
    order_num integer NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.sections OWNER TO postgres;

--
-- Name: staff_profiles; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.staff_profiles (
    user_id uuid NOT NULL,
    name character varying(100) NOT NULL,
    surname character varying(100) NOT NULL,
    patronymic character varying(100)
);


ALTER TABLE public.staff_profiles OWNER TO postgres;

--
-- Name: task_answer_options; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.task_answer_options (
    id uuid NOT NULL,
    task_id uuid NOT NULL,
    option_text text,
    option_audio_id character varying(255),
    is_correct boolean DEFAULT false NOT NULL,
    order_num integer NOT NULL
);


ALTER TABLE public.task_answer_options OWNER TO postgres;

--
-- Name: task_answers; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.task_answers (
    id uuid NOT NULL,
    task_id uuid NOT NULL,
    answer_type character varying(30) NOT NULL,
    correct_answer jsonb NOT NULL
);


ALTER TABLE public.task_answers OWNER TO postgres;

--
-- Name: task_content; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.task_content (
    id uuid NOT NULL,
    task_id uuid NOT NULL,
    content_type character varying(20) NOT NULL,
    content_id character varying(255),
    description text,
    transcription text,
    translation text,
    order_num integer NOT NULL
);


ALTER TABLE public.task_content OWNER TO postgres;

--
-- Name: tasks; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tasks (
    id uuid NOT NULL,
    theme_id uuid NOT NULL,
    name character varying(255) NOT NULL,
    task_type character varying(50) NOT NULL,
    question text NOT NULL,
    instructions text,
    is_training boolean DEFAULT false NOT NULL,
    order_num integer NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.tasks OWNER TO postgres;

--
-- Name: themes; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.themes (
    id uuid NOT NULL,
    section_id uuid NOT NULL,
    name character varying(255) NOT NULL,
    description text,
    order_num integer NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.themes OWNER TO postgres;

--
-- Name: user_badges; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_badges (
    user_id uuid NOT NULL,
    badge_id uuid NOT NULL,
    earned_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    course_id uuid,
    theme_id uuid
);


ALTER TABLE public.user_badges OWNER TO postgres;

--
-- Name: user_course_enrollments; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_course_enrollments (
    user_id uuid NOT NULL,
    course_id uuid NOT NULL,
    enrolled_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    completed_at timestamp without time zone,
    current_section_id uuid,
    current_theme_id uuid,
    progress numeric(5,2) DEFAULT 0.0 NOT NULL
);


ALTER TABLE public.user_course_enrollments OWNER TO postgres;

--
-- Name: user_course_statistics; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_course_statistics (
    user_id uuid NOT NULL,
    course_id uuid NOT NULL,
    tasks_completed integer DEFAULT 0 NOT NULL,
    tasks_attempted integer DEFAULT 0 NOT NULL,
    tasks_total integer,
    time_spent_seconds integer DEFAULT 0 NOT NULL,
    correct_answers integer DEFAULT 0 NOT NULL,
    progress_percentage numeric(5,2) DEFAULT 0 NOT NULL,
    started_at timestamp without time zone,
    last_activity_at timestamp without time zone,
    completed_at timestamp without time zone,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.user_course_statistics OWNER TO postgres;

--
-- Name: user_language_skills; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_language_skills (
    user_id uuid NOT NULL,
    language character varying(50) NOT NULL,
    understands boolean DEFAULT false NOT NULL,
    speaks boolean DEFAULT false NOT NULL,
    reads boolean DEFAULT false NOT NULL,
    writes boolean DEFAULT false NOT NULL
);


ALTER TABLE public.user_language_skills OWNER TO postgres;

--
-- Name: user_profiles; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_profiles (
    user_id uuid NOT NULL,
    surname character varying(100) NOT NULL,
    name character varying(100) NOT NULL,
    patronymic character varying(100),
    gender character varying(10) NOT NULL,
    dob date NOT NULL,
    dor date NOT NULL,
    citizenship character varying(100),
    nationality character varying(100),
    country_of_residence character varying(100),
    city_of_residence character varying(100),
    country_during_education character varying(100),
    period_spent character varying(20),
    kind_of_activity character varying(255),
    education character varying(255),
    purpose_of_register character varying(255)
);


ALTER TABLE public.user_profiles OWNER TO postgres;

--
-- Name: user_statistics; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_statistics (
    user_id uuid NOT NULL,
    total_tasks_completed integer DEFAULT 0 NOT NULL,
    total_tasks_attempted integer DEFAULT 0 NOT NULL,
    total_time_spent_seconds integer DEFAULT 0 NOT NULL,
    total_correct_answers integer DEFAULT 0 NOT NULL,
    courses_enrolled integer DEFAULT 0 NOT NULL,
    courses_completed integer DEFAULT 0 NOT NULL,
    current_streak_days integer DEFAULT 0 NOT NULL,
    longest_streak_days integer DEFAULT 0 NOT NULL,
    last_activity_date date,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.user_statistics OWNER TO postgres;

--
-- Name: user_task_progress; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_task_progress (
    user_id uuid NOT NULL,
    task_id uuid NOT NULL,
    status character varying(20) DEFAULT 'NOT_STARTED'::character varying NOT NULL,
    attempt_count integer DEFAULT 0 NOT NULL,
    is_correct boolean,
    last_attempt_at timestamp without time zone,
    completed_at timestamp without time zone,
    should_retry_after_tasks integer
);


ALTER TABLE public.user_task_progress OWNER TO postgres;

--
-- Name: user_task_queue; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_task_queue (
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    task_id uuid NOT NULL,
    theme_id uuid NOT NULL,
    section_id uuid NOT NULL,
    queue_position integer NOT NULL,
    is_original_task boolean DEFAULT true NOT NULL,
    is_retry_task boolean DEFAULT false NOT NULL,
    original_task_id uuid,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.user_task_queue OWNER TO postgres;

--
-- Name: users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.users (
    id uuid NOT NULL,
    email character varying(255) NOT NULL,
    phone character varying(50),
    role character varying(20) NOT NULL,
    system_language character varying(20) NOT NULL,
    avatar_id character varying(255),
    last_activity_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    password_hash character varying(255) NOT NULL,
    status character varying(25) DEFAULT 'ACTIVE'::character varying NOT NULL
);


ALTER TABLE public.users OWNER TO postgres;

--
-- Name: badges badges_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.badges
    ADD CONSTRAINT badges_pkey PRIMARY KEY (id);


--
-- Name: course_statistics course_statistics_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.course_statistics
    ADD CONSTRAINT course_statistics_pkey PRIMARY KEY (course_id);


--
-- Name: courses courses_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.courses
    ADD CONSTRAINT courses_pkey PRIMARY KEY (id);


--
-- Name: media_files media_files_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.media_files
    ADD CONSTRAINT media_files_pkey PRIMARY KEY (id);


--
-- Name: user_badges pk_user_badges; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_badges
    ADD CONSTRAINT pk_user_badges PRIMARY KEY (user_id, badge_id);


--
-- Name: user_course_enrollments pk_user_course_enrollments; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_course_enrollments
    ADD CONSTRAINT pk_user_course_enrollments PRIMARY KEY (user_id, course_id);


--
-- Name: user_course_statistics pk_user_course_statistics; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_course_statistics
    ADD CONSTRAINT pk_user_course_statistics PRIMARY KEY (user_id, course_id);


--
-- Name: user_language_skills pk_user_language_skills; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_language_skills
    ADD CONSTRAINT pk_user_language_skills PRIMARY KEY (user_id, language);


--
-- Name: user_task_progress pk_user_task_progress; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_task_progress
    ADD CONSTRAINT pk_user_task_progress PRIMARY KEY (user_id, task_id);


--
-- Name: reports reports_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.reports
    ADD CONSTRAINT reports_pkey PRIMARY KEY (id);


--
-- Name: sections sections_course_id_order_num_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sections
    ADD CONSTRAINT sections_course_id_order_num_key UNIQUE (course_id, order_num);


--
-- Name: sections sections_course_id_order_num_unique; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sections
    ADD CONSTRAINT sections_course_id_order_num_unique UNIQUE (course_id, order_num);


--
-- Name: sections sections_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sections
    ADD CONSTRAINT sections_pkey PRIMARY KEY (id);


--
-- Name: staff_profiles staff_profiles_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.staff_profiles
    ADD CONSTRAINT staff_profiles_pkey PRIMARY KEY (user_id);


--
-- Name: task_answer_options task_answer_options_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.task_answer_options
    ADD CONSTRAINT task_answer_options_pkey PRIMARY KEY (id);


--
-- Name: task_answers task_answers_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.task_answers
    ADD CONSTRAINT task_answers_pkey PRIMARY KEY (id);


--
-- Name: task_answers task_answers_task_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.task_answers
    ADD CONSTRAINT task_answers_task_id_key UNIQUE (task_id);


--
-- Name: task_answers task_answers_task_id_unique; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.task_answers
    ADD CONSTRAINT task_answers_task_id_unique UNIQUE (task_id);


--
-- Name: task_content task_content_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.task_content
    ADD CONSTRAINT task_content_pkey PRIMARY KEY (id);


--
-- Name: tasks tasks_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tasks
    ADD CONSTRAINT tasks_pkey PRIMARY KEY (id);


--
-- Name: tasks tasks_theme_id_order_num_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tasks
    ADD CONSTRAINT tasks_theme_id_order_num_key UNIQUE (theme_id, order_num);


--
-- Name: tasks tasks_theme_id_order_num_unique; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tasks
    ADD CONSTRAINT tasks_theme_id_order_num_unique UNIQUE (theme_id, order_num);


--
-- Name: themes themes_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.themes
    ADD CONSTRAINT themes_pkey PRIMARY KEY (id);


--
-- Name: themes themes_section_id_order_num_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.themes
    ADD CONSTRAINT themes_section_id_order_num_key UNIQUE (section_id, order_num);


--
-- Name: themes themes_section_id_order_num_unique; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.themes
    ADD CONSTRAINT themes_section_id_order_num_unique UNIQUE (section_id, order_num);


--
-- Name: user_profiles user_profiles_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_profiles
    ADD CONSTRAINT user_profiles_pkey PRIMARY KEY (user_id);


--
-- Name: user_statistics user_statistics_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_statistics
    ADD CONSTRAINT user_statistics_pkey PRIMARY KEY (user_id);


--
-- Name: user_task_queue user_task_queue_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_task_queue
    ADD CONSTRAINT user_task_queue_pkey PRIMARY KEY (id);


--
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- Name: users users_email_unique; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_unique UNIQUE (email);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: idx_course_statistics_enrolled; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_course_statistics_enrolled ON public.course_statistics USING btree (students_enrolled);


--
-- Name: idx_media_files_type; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_media_files_type ON public.media_files USING btree (file_type);


--
-- Name: idx_media_files_uploaded_by; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_media_files_uploaded_by ON public.media_files USING btree (uploaded_by);


--
-- Name: idx_sections_course_order; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_sections_course_order ON public.sections USING btree (course_id, order_num);


--
-- Name: idx_task_answer_options_task_order; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_task_answer_options_task_order ON public.task_answer_options USING btree (task_id, order_num);


--
-- Name: idx_task_content_task_order; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_task_content_task_order ON public.task_content USING btree (task_id, order_num);


--
-- Name: idx_tasks_theme_order; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_tasks_theme_order ON public.tasks USING btree (theme_id, order_num);


--
-- Name: idx_themes_section_order; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_themes_section_order ON public.themes USING btree (section_id, order_num);


--
-- Name: idx_user_badges_user; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_user_badges_user ON public.user_badges USING btree (user_id);


--
-- Name: idx_user_course_enrollments_user; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_user_course_enrollments_user ON public.user_course_enrollments USING btree (user_id);


--
-- Name: idx_user_course_stats_activity; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_user_course_stats_activity ON public.user_course_statistics USING btree (last_activity_at);


--
-- Name: idx_user_course_stats_progress; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_user_course_stats_progress ON public.user_course_statistics USING btree (progress_percentage);


--
-- Name: idx_user_statistics_activity; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_user_statistics_activity ON public.user_statistics USING btree (last_activity_date);


--
-- Name: idx_user_task_progress_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_user_task_progress_status ON public.user_task_progress USING btree (status);


--
-- Name: idx_user_task_progress_user; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_user_task_progress_user ON public.user_task_progress USING btree (user_id);


--
-- Name: idx_user_task_queue_user_position; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_user_task_queue_user_position ON public.user_task_queue USING btree (user_id, queue_position);


--
-- Name: idx_users_email; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_email ON public.users USING btree (email);


--
-- Name: idx_users_role; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_role ON public.users USING btree (role);


--
-- Name: staff_profiles check_staff_profile_trigger; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER check_staff_profile_trigger BEFORE INSERT OR UPDATE ON public.staff_profiles FOR EACH ROW EXECUTE FUNCTION public.check_staff_profile();


--
-- Name: user_profiles check_student_profile_trigger; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER check_student_profile_trigger BEFORE INSERT OR UPDATE ON public.user_profiles FOR EACH ROW EXECUTE FUNCTION public.check_student_profile();


--
-- Name: courses create_course_statistics_trigger; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER create_course_statistics_trigger AFTER INSERT ON public.courses FOR EACH ROW EXECUTE FUNCTION public.create_course_statistics();


--
-- Name: user_course_enrollments trigger_decrement_enrollment_statistics; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trigger_decrement_enrollment_statistics AFTER DELETE ON public.user_course_enrollments FOR EACH ROW EXECUTE FUNCTION public.decrement_course_enrollment_statistics();


--
-- Name: user_task_progress trigger_update_accumulated_statistics; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trigger_update_accumulated_statistics AFTER INSERT OR UPDATE ON public.user_task_progress FOR EACH ROW EXECUTE FUNCTION public.update_accumulated_statistics();


--
-- Name: user_course_enrollments trigger_update_enrollment_statistics; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trigger_update_enrollment_statistics AFTER INSERT ON public.user_course_enrollments FOR EACH ROW EXECUTE FUNCTION public.update_course_enrollment_statistics();


--
-- Name: courses update_courses_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER update_courses_updated_at BEFORE UPDATE ON public.courses FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: users update_users_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON public.users FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: course_statistics course_statistics_course_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.course_statistics
    ADD CONSTRAINT course_statistics_course_id_fkey FOREIGN KEY (course_id) REFERENCES public.courses(id) ON DELETE CASCADE;


--
-- Name: courses courses_author_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.courses
    ADD CONSTRAINT courses_author_id_fkey FOREIGN KEY (author_id) REFERENCES public.users(id);


--
-- Name: courses courses_poster_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.courses
    ADD CONSTRAINT courses_poster_id_fkey FOREIGN KEY (poster_id) REFERENCES public.media_files(id) ON UPDATE RESTRICT ON DELETE SET NULL;


--
-- Name: course_statistics fk_course_statistics_course_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.course_statistics
    ADD CONSTRAINT fk_course_statistics_course_id__id FOREIGN KEY (course_id) REFERENCES public.courses(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: courses fk_courses_author_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.courses
    ADD CONSTRAINT fk_courses_author_id__id FOREIGN KEY (author_id) REFERENCES public.users(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: media_files fk_media_files_uploaded_by__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.media_files
    ADD CONSTRAINT fk_media_files_uploaded_by__id FOREIGN KEY (uploaded_by) REFERENCES public.users(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: sections fk_sections_course_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sections
    ADD CONSTRAINT fk_sections_course_id__id FOREIGN KEY (course_id) REFERENCES public.courses(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: staff_profiles fk_staff_profiles_user_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.staff_profiles
    ADD CONSTRAINT fk_staff_profiles_user_id__id FOREIGN KEY (user_id) REFERENCES public.users(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: task_answer_options fk_task_answer_options_task_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.task_answer_options
    ADD CONSTRAINT fk_task_answer_options_task_id__id FOREIGN KEY (task_id) REFERENCES public.tasks(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: task_answers fk_task_answers_task_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.task_answers
    ADD CONSTRAINT fk_task_answers_task_id__id FOREIGN KEY (task_id) REFERENCES public.tasks(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: task_content fk_task_content_task_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.task_content
    ADD CONSTRAINT fk_task_content_task_id__id FOREIGN KEY (task_id) REFERENCES public.tasks(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: tasks fk_tasks_theme_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tasks
    ADD CONSTRAINT fk_tasks_theme_id__id FOREIGN KEY (theme_id) REFERENCES public.themes(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: themes fk_themes_section_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.themes
    ADD CONSTRAINT fk_themes_section_id__id FOREIGN KEY (section_id) REFERENCES public.sections(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: user_badges fk_user_badges_badge_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_badges
    ADD CONSTRAINT fk_user_badges_badge_id__id FOREIGN KEY (badge_id) REFERENCES public.badges(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: user_badges fk_user_badges_course_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_badges
    ADD CONSTRAINT fk_user_badges_course_id__id FOREIGN KEY (course_id) REFERENCES public.courses(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: user_badges fk_user_badges_theme_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_badges
    ADD CONSTRAINT fk_user_badges_theme_id__id FOREIGN KEY (theme_id) REFERENCES public.themes(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: user_badges fk_user_badges_user_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_badges
    ADD CONSTRAINT fk_user_badges_user_id__id FOREIGN KEY (user_id) REFERENCES public.users(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: user_course_enrollments fk_user_course_enrollments_course_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_course_enrollments
    ADD CONSTRAINT fk_user_course_enrollments_course_id__id FOREIGN KEY (course_id) REFERENCES public.courses(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: user_course_enrollments fk_user_course_enrollments_current_section_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_course_enrollments
    ADD CONSTRAINT fk_user_course_enrollments_current_section_id__id FOREIGN KEY (current_section_id) REFERENCES public.sections(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: user_course_enrollments fk_user_course_enrollments_current_theme_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_course_enrollments
    ADD CONSTRAINT fk_user_course_enrollments_current_theme_id__id FOREIGN KEY (current_theme_id) REFERENCES public.themes(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: user_course_enrollments fk_user_course_enrollments_user_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_course_enrollments
    ADD CONSTRAINT fk_user_course_enrollments_user_id__id FOREIGN KEY (user_id) REFERENCES public.users(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: user_course_statistics fk_user_course_statistics_course_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_course_statistics
    ADD CONSTRAINT fk_user_course_statistics_course_id__id FOREIGN KEY (course_id) REFERENCES public.courses(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: user_course_statistics fk_user_course_statistics_user_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_course_statistics
    ADD CONSTRAINT fk_user_course_statistics_user_id__id FOREIGN KEY (user_id) REFERENCES public.users(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: user_language_skills fk_user_language_skills_user_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_language_skills
    ADD CONSTRAINT fk_user_language_skills_user_id__id FOREIGN KEY (user_id) REFERENCES public.users(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: user_profiles fk_user_profiles_user_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_profiles
    ADD CONSTRAINT fk_user_profiles_user_id__id FOREIGN KEY (user_id) REFERENCES public.users(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: user_statistics fk_user_statistics_user_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_statistics
    ADD CONSTRAINT fk_user_statistics_user_id__id FOREIGN KEY (user_id) REFERENCES public.users(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: user_task_progress fk_user_task_progress_task_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_task_progress
    ADD CONSTRAINT fk_user_task_progress_task_id__id FOREIGN KEY (task_id) REFERENCES public.tasks(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: user_task_progress fk_user_task_progress_user_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_task_progress
    ADD CONSTRAINT fk_user_task_progress_user_id__id FOREIGN KEY (user_id) REFERENCES public.users(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: user_task_queue fk_user_task_queue_original_task_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_task_queue
    ADD CONSTRAINT fk_user_task_queue_original_task_id__id FOREIGN KEY (original_task_id) REFERENCES public.tasks(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: user_task_queue fk_user_task_queue_section_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_task_queue
    ADD CONSTRAINT fk_user_task_queue_section_id__id FOREIGN KEY (section_id) REFERENCES public.sections(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: user_task_queue fk_user_task_queue_task_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_task_queue
    ADD CONSTRAINT fk_user_task_queue_task_id__id FOREIGN KEY (task_id) REFERENCES public.tasks(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: user_task_queue fk_user_task_queue_theme_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_task_queue
    ADD CONSTRAINT fk_user_task_queue_theme_id__id FOREIGN KEY (theme_id) REFERENCES public.themes(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: user_task_queue fk_user_task_queue_user_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_task_queue
    ADD CONSTRAINT fk_user_task_queue_user_id__id FOREIGN KEY (user_id) REFERENCES public.users(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: media_files media_files_uploaded_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.media_files
    ADD CONSTRAINT media_files_uploaded_by_fkey FOREIGN KEY (uploaded_by) REFERENCES public.users(id);


--
-- Name: reports reports_reporter_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.reports
    ADD CONSTRAINT reports_reporter_id_fkey FOREIGN KEY (reporter_id) REFERENCES public.users(id);


--
-- Name: sections sections_course_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sections
    ADD CONSTRAINT sections_course_id_fkey FOREIGN KEY (course_id) REFERENCES public.courses(id) ON DELETE CASCADE;


--
-- Name: staff_profiles staff_profiles_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.staff_profiles
    ADD CONSTRAINT staff_profiles_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: task_answer_options task_answer_options_task_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.task_answer_options
    ADD CONSTRAINT task_answer_options_task_id_fkey FOREIGN KEY (task_id) REFERENCES public.tasks(id) ON DELETE CASCADE;


--
-- Name: task_answers task_answers_task_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.task_answers
    ADD CONSTRAINT task_answers_task_id_fkey FOREIGN KEY (task_id) REFERENCES public.tasks(id) ON DELETE CASCADE;


--
-- Name: task_content task_content_task_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.task_content
    ADD CONSTRAINT task_content_task_id_fkey FOREIGN KEY (task_id) REFERENCES public.tasks(id) ON DELETE CASCADE;


--
-- Name: tasks tasks_theme_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tasks
    ADD CONSTRAINT tasks_theme_id_fkey FOREIGN KEY (theme_id) REFERENCES public.themes(id) ON DELETE CASCADE;


--
-- Name: themes themes_section_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.themes
    ADD CONSTRAINT themes_section_id_fkey FOREIGN KEY (section_id) REFERENCES public.sections(id) ON DELETE CASCADE;


--
-- Name: user_badges user_badges_badge_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_badges
    ADD CONSTRAINT user_badges_badge_id_fkey FOREIGN KEY (badge_id) REFERENCES public.badges(id) ON DELETE CASCADE;


--
-- Name: user_badges user_badges_course_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_badges
    ADD CONSTRAINT user_badges_course_id_fkey FOREIGN KEY (course_id) REFERENCES public.courses(id);


--
-- Name: user_badges user_badges_theme_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_badges
    ADD CONSTRAINT user_badges_theme_id_fkey FOREIGN KEY (theme_id) REFERENCES public.themes(id);


--
-- Name: user_badges user_badges_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_badges
    ADD CONSTRAINT user_badges_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: user_course_enrollments user_course_enrollments_course_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_course_enrollments
    ADD CONSTRAINT user_course_enrollments_course_id_fkey FOREIGN KEY (course_id) REFERENCES public.courses(id) ON DELETE CASCADE;


--
-- Name: user_course_enrollments user_course_enrollments_current_section_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_course_enrollments
    ADD CONSTRAINT user_course_enrollments_current_section_id_fkey FOREIGN KEY (current_section_id) REFERENCES public.sections(id);


--
-- Name: user_course_enrollments user_course_enrollments_current_theme_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_course_enrollments
    ADD CONSTRAINT user_course_enrollments_current_theme_id_fkey FOREIGN KEY (current_theme_id) REFERENCES public.themes(id);


--
-- Name: user_course_enrollments user_course_enrollments_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_course_enrollments
    ADD CONSTRAINT user_course_enrollments_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: user_course_statistics user_course_statistics_course_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_course_statistics
    ADD CONSTRAINT user_course_statistics_course_id_fkey FOREIGN KEY (course_id) REFERENCES public.courses(id) ON DELETE CASCADE;


--
-- Name: user_course_statistics user_course_statistics_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_course_statistics
    ADD CONSTRAINT user_course_statistics_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: user_language_skills user_language_skills_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_language_skills
    ADD CONSTRAINT user_language_skills_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: user_profiles user_profiles_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_profiles
    ADD CONSTRAINT user_profiles_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: user_statistics user_statistics_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_statistics
    ADD CONSTRAINT user_statistics_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: user_task_progress user_task_progress_task_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_task_progress
    ADD CONSTRAINT user_task_progress_task_id_fkey FOREIGN KEY (task_id) REFERENCES public.tasks(id) ON DELETE CASCADE;


--
-- Name: user_task_progress user_task_progress_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_task_progress
    ADD CONSTRAINT user_task_progress_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: user_task_queue user_task_queue_original_task_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_task_queue
    ADD CONSTRAINT user_task_queue_original_task_id_fkey FOREIGN KEY (original_task_id) REFERENCES public.tasks(id);


--
-- Name: user_task_queue user_task_queue_section_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_task_queue
    ADD CONSTRAINT user_task_queue_section_id_fkey FOREIGN KEY (section_id) REFERENCES public.sections(id) ON DELETE CASCADE NOT VALID;


--
-- Name: user_task_queue user_task_queue_task_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_task_queue
    ADD CONSTRAINT user_task_queue_task_id_fkey FOREIGN KEY (task_id) REFERENCES public.tasks(id) ON DELETE CASCADE;


--
-- Name: user_task_queue user_task_queue_theme_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_task_queue
    ADD CONSTRAINT user_task_queue_theme_id_fkey FOREIGN KEY (theme_id) REFERENCES public.themes(id) ON DELETE CASCADE;


--
-- Name: user_task_queue user_task_queue_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_task_queue
    ADD CONSTRAINT user_task_queue_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

\unrestrict a2xyhP44loKpweYCbSFFDvmMTxLWrBVb0aqn6M5ML6prBQfKR6r8ZRgWk4AA2xj

