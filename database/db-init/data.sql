--
-- PostgreSQL database dump
--

\restrict cfcFE9Xzd790nd3RLax6GjD66w0SPiYeaZWibtHfRxodEhocdCiXGcfGShK8VqY

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
-- Data for Name: badges; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.badges (id, name, description, image_id, badge_type, criteria, created_at) FROM stdin;
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.users (id, email, phone, role, system_language, avatar_id, last_activity_at, created_at, updated_at, password_hash, status) FROM stdin;
810f49d6-2d3e-4514-a7bd-4d80b98ad073	stud@mail.ru	000000	STUDENT	RUSSIAN	\N	\N	2025-08-09 21:31:45.581802	2025-08-12 19:02:35.392161	$2a$12$Zc7PMhUKJPCdnIRiZXGAnOa1u0H6qsnTaNlJkxk1i8hrsSN	ACTIVE
9359be3c-28d7-4292-a316-ede48f1fca0e	test@test.ru	88005553535	ADMIN	RUSSIAN	52e7e951-8a20-4337-a3a2-e188197e7cc0	\N	2025-08-11 00:13:27.293161	2025-08-14 17:41:23.040143	$2a$12$PiLGhGf1K3Z4K/Fi9VmyuejdLWOjCOayYZerCZSI1VSY3QvQGWgDe	ACTIVE
9003269c-0bc0-460a-aaa0-84fbe0e80765	a@mail.ru	89131231212	ADMIN	RUSSIAN		2025-08-14 17:41:05.69771	2025-08-09 14:14:51.91135	2025-08-14 17:43:52.198219	$2a$12$vyF1ib9aM0sVQAy746OMsOAA7DdDnBbM4vj2AMl9OU6nD0VG3FxTi	ACTIVE
91a69e63-73ca-4169-bba8-ac400b8cc760	expert@mail.ru		EXPERT	RUSSIAN	\N	2025-08-14 18:11:58.091578	2025-08-09 16:16:11.843068	2025-08-14 18:11:58.093049	$2a$12$Zc7PMhUKJPCdnIRiZXGAnOa1u0H6qsnTaNlJkxk1i8hrsSN.MvLge	ACTIVE
28d1cfda-7675-4752-b4f7-402af5fd2cb4	admin@example.com	1489	ADMIN	RUSSIAN	e232189f-e26e-4e03-933e-d960537e50ca	2025-08-24 15:16:17.325251	2025-08-09 14:22:51.687287	2025-08-24 15:16:17.32811	$2a$12$CqRH7Fu4rzNyJD7yzouGfONsJwcCyIxfiuiefqXED/VFrNzC89ria	ACTIVE
78bb65b3-b284-424a-8e43-a9beedb14fec	cm@mail.ru	8800123123	CONTENT_MODERATOR	RUSSIAN	a1e4a7f1-f9f3-40f3-9d63-0b473fd3b295	2025-08-24 15:17:49.305337	2025-08-09 16:54:54.563295	2025-08-24 15:17:49.30578	$2a$12$HlChVVJQe6Tdhw9Bu9ypq.di4vyoM6kdeBjlg4YYk.5IMf35g9jk2	ACTIVE
\.


--
-- Data for Name: media_files; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.media_files (id, file_name, file_type, mime_type, file_size, uploaded_by, uploaded_at, is_active) FROM stdin;
6f2a1364-0b91-48ab-b6c7-94278d87d41c	собака.jpg	AVATAR	image/jpeg	108717	28d1cfda-7675-4752-b4f7-402af5fd2cb4	2025-08-09 15:38:21.147136	f
4276ef46-c72f-4138-9452-3d462233adf7	ИЛЬЯ.jpg	AVATAR	image/jpeg	46545	28d1cfda-7675-4752-b4f7-402af5fd2cb4	2025-08-09 15:40:19.051436	f
441f3e66-c91d-4b52-b87e-3edd3050e76f	ИЛЬЯ.jpg	AVATAR	image/jpeg	46545	28d1cfda-7675-4752-b4f7-402af5fd2cb4	2025-08-09 15:40:25.32489	f
7df1893d-105e-41be-b12d-973fcc1468f9	собака.jpg	AVATAR	image/jpeg	108717	28d1cfda-7675-4752-b4f7-402af5fd2cb4	2025-08-09 15:41:50.803877	f
a1e4a7f1-f9f3-40f3-9d63-0b473fd3b295	ИЛЬЯ.jpg	AVATAR	image/jpeg	46545	78bb65b3-b284-424a-8e43-a9beedb14fec	2025-08-09 16:55:14.924504	t
52e7e951-8a20-4337-a3a2-e188197e7cc0	inRussian.png	AVATAR	image/png	10474	9359be3c-28d7-4292-a316-ede48f1fca0e	2025-08-11 00:13:49.094088	t
7836265e-5373-4d9e-916f-dbb6e30c91e6	собака.jpg	AVATAR	image/jpeg	108717	28d1cfda-7675-4752-b4f7-402af5fd2cb4	2025-08-09 15:42:02.85221	f
e232189f-e26e-4e03-933e-d960537e50ca	players_club.jpg	AVATAR	image/jpeg	157592	28d1cfda-7675-4752-b4f7-402af5fd2cb4	2025-08-12 17:09:32.221853	t
21947f30-9f8a-4f79-a9ec-df365eed5efe	BOOM.mp3	AUDIO	audio/mpeg	21230	78bb65b3-b284-424a-8e43-a9beedb14fec	2025-08-14 17:45:19.45065	t
b4abeae3-9568-4332-ab51-00b21b758a96	BOOM.mp3	AUDIO	audio/mpeg	21230	78bb65b3-b284-424a-8e43-a9beedb14fec	2025-08-14 17:51:03.223537	t
\.


--
-- Data for Name: courses; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.courses (id, name, description, author_id, author_url, language, is_published, created_at, updated_at, poster_id) FROM stdin;
ad696a5e-7f76-466b-b52c-95a7a2469d23	Математика	Курс по математике для младших классов	78bb65b3-b284-424a-8e43-a9beedb14fec	78bb65b3-b284-424a-8e43-a9beedb14fec	RUSSIAN	f	2025-08-09 16:55:44.308312	2025-08-09 16:55:44.308312	\N
5ec405ed-1baf-4aaa-9343-dd988e7c8a24	Русский язык	для русских	78bb65b3-b284-424a-8e43-a9beedb14fec	78bb65b3-b284-424a-8e43-a9beedb14fec	RUSSIAN	f	2025-08-10 22:31:31.942675	2025-08-10 22:31:31.942675	\N
f3c9f222-4432-4b2a-97a7-77e5adf7271b	Тест		78bb65b3-b284-424a-8e43-a9beedb14fec	0070ecd4-fa1f-4007-bde7-a5399b789fe1	RUSSIAN	f	2025-08-11 16:37:21.880827	2025-08-11 17:32:28.6083	\N
\.


--
-- Data for Name: course_statistics; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.course_statistics (course_id, students_enrolled, students_completed, students_active_last_7_days, total_tasks_completed, total_time_spent_seconds, total_attempts, total_correct_answers, average_completion_time_seconds, updated_at) FROM stdin;
\.


--
-- Data for Name: password_recovery_tokens; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.password_recovery_tokens (email, token_hash, expires_at, created_at) FROM stdin;
\.


--
-- Data for Name: reports; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.reports (id, description, task_id, reporter_id, created_at) FROM stdin;
1dbee0bb-2eb3-4920-8034-f5c09edc42d6	Привычки играют ключевую роль в нашей жизни и во многом определяют наш успех. Именно ежедневные небольшие действия складываются в большие достижения со временем. Формирование полезных привычек помогает дисциплинировать себя, экономить энергию и принимать правильные решения автоматически, без постоянных раздумий. Например, привычка читать хотя бы несколько страниц книги каждый день постепенно расширяет кругозор и улучшает мышление. Регулярные занятия спортом поддерживают здоровье и повышают уровень энергии. В то же время вредные привычки могут замедлять развитие и мешать достигать целей\nПсихология говорит о том, что формирование новой привычки занимает от 21 до 66 дней постоянных повторений. Важно начать с маленьких и реалистичных шагов, чтобы не перегрузить себя и сохранить мотивацию. Полезно также использовать напоминания и поощрения, чтобы закрепить новое поведение. Постепенно эти действия станут частью вашего образа жизни и принесут долгосрочные положительные результаты	df8ae630-53de-4880-b864-4bc6a2f533b8	28d1cfda-7675-4752-b4f7-402af5fd2cb4	2025-08-08 21:21:03
\.


--
-- Data for Name: sections; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.sections (id, course_id, name, description, order_num, created_at) FROM stdin;
e708a6b4-d720-4f7c-8f2e-2837c33b4be8	ad696a5e-7f76-466b-b52c-95a7a2469d23	Умножение	уроки умножения	1	2025-08-09 16:56:50.571017
42fb8c64-8ee4-4c16-ab61-6fa79e2e8829	ad696a5e-7f76-466b-b52c-95a7a2469d23	Деление	деление в столбик	2	2025-08-09 16:57:17.406957
bccbdd35-69d4-478e-9a64-8728a9215c92	5ec405ed-1baf-4aaa-9343-dd988e7c8a24	Крутой прикол		1	2025-08-10 22:38:48.978996
894f31b7-a4ad-42a8-bc4d-32be9c389c1b	f3c9f222-4432-4b2a-97a7-77e5adf7271b	Секция Тест 1		1	2025-08-11 17:32:49.819339
\.


--
-- Data for Name: staff_profiles; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.staff_profiles (user_id, name, surname, patronymic) FROM stdin;
91a69e63-73ca-4169-bba8-ac400b8cc760	тест	эксперт	эксперт
28d1cfda-7675-4752-b4f7-402af5fd2cb4	test	test	
9359be3c-28d7-4292-a316-ede48f1fca0e	Test	Test	
78bb65b3-b284-424a-8e43-a9beedb14fec	cm	cm	
9003269c-0bc0-460a-aaa0-84fbe0e80765	Админ	Админович	 
\.


--
-- Data for Name: themes; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.themes (id, section_id, name, description, order_num, created_at) FROM stdin;
46a8ac91-be83-4968-b468-9a19205e0e3e	42fb8c64-8ee4-4c16-ab61-6fa79e2e8829	Дроби	базовые понятия о дробях	1	2025-08-09 16:57:52.855378
bc048345-964e-4159-9134-ea10398ddade	42fb8c64-8ee4-4c16-ab61-6fa79e2e8829	Прикольность		2	2025-08-09 20:05:29.357554
cd5a79d6-09a3-4e82-b12a-3f81153f33d1	bccbdd35-69d4-478e-9a64-8728a9215c92	Крутая тема 1	Описание темы 1	1	2025-08-10 22:40:18.001899
0a8a6f41-b71c-4737-a8f5-afb509650109	894f31b7-a4ad-42a8-bc4d-32be9c389c1b	Тема Тест	Описание темы Тест 1	1	2025-08-11 17:33:28.839803
\.


--
-- Data for Name: tasks; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.tasks (id, theme_id, name, task_type, question, instructions, is_training, order_num, created_at) FROM stdin;
fb6e6abf-08e6-41c4-b9c3-591ab4cd5e33	46a8ac91-be83-4968-b468-9a19205e0e3e	Читать и выбирать	READ_AND_CHOOSE	Почему		t	1	2025-08-09 17:03:54.85571
df8ae630-53de-4880-b864-4bc6a2f533b8	bc048345-964e-4159-9134-ea10398ddade	Почему	MATCH_TEXT_TEXT	Ура		t	1	2025-08-09 20:06:17.811592
8e445c47-976a-419e-bbef-7969b1ee761d	bc048345-964e-4159-9134-ea10398ddade	Почему	MATCH_TEXT_TEXT	Ура		t	2	2025-08-09 20:07:28.601159
4558277c-4484-42b7-b810-b72462bd30db	bc048345-964e-4159-9134-ea10398ddade	Почему	MATCH_TEXT_TEXT	Ура		t	3	2025-08-09 21:14:01.439736
9203c8c1-be59-43a8-ae11-bc3c5fdc182d	0a8a6f41-b71c-4737-a8f5-afb509650109	123	LISTEN_AND_CHOOSE	123		f	1	2025-08-11 19:17:30.607107
ca7933b9-8bdf-4cd3-abd6-4b483f17fbb7	0a8a6f41-b71c-4737-a8f5-afb509650109	Моя задача?	LISTEN_AND_CHOOSE	Моя задача норм?		f	2	2025-08-14 17:50:29.230909
\.


--
-- Data for Name: task_answer_options; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.task_answer_options (id, task_id, option_text, option_audio_id, is_correct, order_num) FROM stdin;
8d52aba5-240d-462f-a4c7-8f4bd069826e	9203c8c1-be59-43a8-ae11-bc3c5fdc182d	1	\N	f	0
02b99108-a5a5-4231-b172-470966db9ad5	9203c8c1-be59-43a8-ae11-bc3c5fdc182d	2	\N	t	1
8dc597c1-43dc-4fae-8240-1b69272da805	9203c8c1-be59-43a8-ae11-bc3c5fdc182d	3	\N	f	2
60e5581d-b29e-4ed5-9d44-a25d9627d665	ca7933b9-8bdf-4cd3-abd6-4b483f17fbb7	Да	\N	t	0
cb4ef62d-a41d-482e-935a-79b9bee20758	ca7933b9-8bdf-4cd3-abd6-4b483f17fbb7	Может быть	\N	f	2
6a435c19-fc44-4615-a8ff-317fc43ef984	ca7933b9-8bdf-4cd3-abd6-4b483f17fbb7	Нет	\N	t	1
\.


--
-- Data for Name: task_answers; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.task_answers (id, task_id, answer_type, correct_answer) FROM stdin;
85ea0f39-57f2-401e-a502-69a96aaa9ca8	9203c8c1-be59-43a8-ae11-bc3c5fdc182d	SINGLE_CHOICE_SHORT	{"optionId": "tmp_1754914626595"}
365c8ce9-c6a2-451b-aa0d-2633709d24c4	ca7933b9-8bdf-4cd3-abd6-4b483f17fbb7	SINGLE_CHOICE_SHORT	{"optionId": "tmp_1755168598495"}
\.


--
-- Data for Name: task_content; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.task_content (id, task_id, content_type, content_id, description, transcription, translation, order_num) FROM stdin;
aadaa52e-bf1a-45b4-96ba-d833f5b9a1f4	fb6e6abf-08e6-41c4-b9c3-591ab4cd5e33	TEXT	\N		[hui]	хуй	0
4d2f2cf0-2883-41d1-bee5-695f2a017da5	ca7933b9-8bdf-4cd3-abd6-4b483f17fbb7	AUDIO	21947f30-9f8a-4f79-a9ec-df365eed5efe	BOOM	boom	бум	0
\.


--
-- Data for Name: task_entity; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.task_entity (id, course_id, task_body, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: task_types; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.task_types (name) FROM stdin;
\.


--
-- Data for Name: task_to_types; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.task_to_types (task_id, type_name) FROM stdin;
\.


--
-- Data for Name: user_badges; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.user_badges (user_id, badge_id, earned_at, course_id, theme_id) FROM stdin;
\.


--
-- Data for Name: user_course_enrollments; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.user_course_enrollments (user_id, course_id, enrolled_at, completed_at, current_section_id, current_theme_id, progress) FROM stdin;
\.


--
-- Data for Name: user_course_statistics; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.user_course_statistics (user_id, course_id, tasks_completed, tasks_attempted, tasks_total, time_spent_seconds, correct_answers, progress_percentage, started_at, last_activity_at, completed_at, updated_at) FROM stdin;
\.


--
-- Data for Name: user_language_skills; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.user_language_skills (user_id, language, understands, speaks, reads, writes) FROM stdin;
810f49d6-2d3e-4514-a7bd-4d80b98ad073	russian	t	f	t	t
\.


--
-- Data for Name: user_profiles; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.user_profiles (user_id, surname, name, patronymic, gender, dob, dor, citizenship, nationality, country_of_residence, city_of_residence, country_during_education, period_spent, kind_of_activity, education, purpose_of_register) FROM stdin;
810f49d6-2d3e-4514-a7bd-4d80b98ad073	Kizaru	Oleg	Алексеевич	MALE	2025-08-14	2025-08-14	\N	Чурка	Дагестан	\N	Румыния	\N	\N	\N	\N
\.


--
-- Data for Name: user_statistics; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.user_statistics (user_id, total_tasks_completed, total_tasks_attempted, total_time_spent_seconds, total_correct_answers, courses_enrolled, courses_completed, current_streak_days, longest_streak_days, last_activity_date, updated_at) FROM stdin;
810f49d6-2d3e-4514-a7bd-4d80b98ad073	2	5	12344	1	2	1	1	1	2025-08-05	2025-08-12 18:04:27
\.


--
-- Data for Name: user_task_progress; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.user_task_progress (user_id, task_id, status, attempt_count, is_correct, last_attempt_at, completed_at, should_retry_after_tasks) FROM stdin;
\.


--
-- Data for Name: user_task_queue; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.user_task_queue (id, user_id, task_id, theme_id, section_id, queue_position, is_original_task, is_retry_task, original_task_id, created_at) FROM stdin;
\.


--
-- PostgreSQL database dump complete
--

\unrestrict cfcFE9Xzd790nd3RLax6GjD66w0SPiYeaZWibtHfRxodEhocdCiXGcfGShK8VqY

