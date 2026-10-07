-- Education Database Schema
CREATE TABLE IF NOT EXISTS students (
    student_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    birth_date DATE NOT NULL,
    country VARCHAR(100) NOT NULL,
    city VARCHAR(100) NOT NULL,
    document_type VARCHAR(20) NOT NULL,
    document_number VARCHAR(20) UNIQUE NOT NULL
    );

-- Create indexes for performance optimization
CREATE INDEX IF NOT EXISTS idx_students_email ON students(email);
CREATE INDEX IF NOT EXISTS idx_students_document_number ON students(document_number);
CREATE INDEX IF NOT EXISTS idx_students_country ON students(country);
CREATE INDEX IF NOT EXISTS idx_students_city ON students(city);

-- Phase 3: Courses & Curriculum DDL
CREATE TABLE IF NOT EXISTS courses (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'draft',
    difficulty VARCHAR(20) NOT NULL DEFAULT 'beginner',
    tags VARCHAR(500),
    thumbnail_url VARCHAR(500),
    instructor_name VARCHAR(200),
    institution_id VARCHAR(100) NOT NULL,
    total_duration INT NOT NULL DEFAULT 0,
    total_lessons INT NOT NULL DEFAULT 0,
    enrolled_count INT NOT NULL DEFAULT 0,
    completion_rate INT NOT NULL DEFAULT 0,
    average_rating NUMERIC(2,1),
    rating_count INT NOT NULL DEFAULT 0,
    published_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS modules (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    order_index INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_course FOREIGN KEY(course_id) REFERENCES courses(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS lessons (
    id BIGSERIAL PRIMARY KEY,
    module_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    order_index INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_module FOREIGN KEY(module_id) REFERENCES modules(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS contents (
    id BIGSERIAL PRIMARY KEY,
    lesson_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    value TEXT,
    order_index INT NOT NULL,
    CONSTRAINT fk_lesson FOREIGN KEY(lesson_id) REFERENCES lessons(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_courses_institution ON courses(institution_id);
CREATE INDEX IF NOT EXISTS idx_modules_course ON modules(course_id);
CREATE INDEX IF NOT EXISTS idx_lessons_module ON lessons(module_id);
CREATE INDEX IF NOT EXISTS idx_contents_lesson ON contents(lesson_id);

-- Phase 4: Quizzes, LearningPaths, Enrollments DDL
CREATE TABLE IF NOT EXISTS quizzes (
    id BIGSERIAL PRIMARY KEY,
    lesson_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    passing_score INT NOT NULL DEFAULT 60,
    CONSTRAINT fk_lesson_quiz FOREIGN KEY(lesson_id) REFERENCES lessons(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS questions (
    id BIGSERIAL PRIMARY KEY,
    quiz_id BIGINT NOT NULL,
    text TEXT NOT NULL,
    options TEXT NOT NULL, -- Semi-colon separated options
    correct_option VARCHAR(255) NOT NULL,
    CONSTRAINT fk_quiz FOREIGN KEY(quiz_id) REFERENCES quizzes(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS learning_paths (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    institution_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS learning_path_courses (
    learning_path_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    order_index INT NOT NULL,
    PRIMARY KEY(learning_path_id, course_id),
    CONSTRAINT fk_lp FOREIGN KEY(learning_path_id) REFERENCES learning_paths(id) ON DELETE CASCADE,
    CONSTRAINT fk_c FOREIGN KEY(course_id) REFERENCES courses(id) ON DELETE CASCADE
);

-- student_id holds the ms-auth user id of the enrolled student. No FK to the local
-- `students` table: that table is a separate, legacy student-profile registry (name,
-- birth date, document, etc.) that has nothing to do with the ms-auth user account
-- actually doing the enrolling, and requiring a row there before enrolling would force
-- a redundant registration flow the frontend never does.
CREATE TABLE IF NOT EXISTS enrollments (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    enrolled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    progress INT DEFAULT 0,
    completed_at TIMESTAMP,
    CONSTRAINT fk_course_enroll FOREIGN KEY(course_id) REFERENCES courses(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_quizzes_lesson ON quizzes(lesson_id);
CREATE INDEX IF NOT EXISTS idx_questions_quiz ON questions(quiz_id);
CREATE INDEX IF NOT EXISTS idx_learning_paths_institution ON learning_paths(institution_id);
CREATE INDEX IF NOT EXISTS idx_enrollments_student ON enrollments(student_id);
CREATE INDEX IF NOT EXISTS idx_enrollments_course ON enrollments(course_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_enrollments_student_course ON enrollments(student_id, course_id);

-- Detailed per-student progress (JSON) added after the initial schema.
ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS progress_data TEXT;

-- What students do inside a course (graded server-side).
CREATE TABLE IF NOT EXISTS quiz_attempts (
    id BIGSERIAL PRIMARY KEY,
    enrollment_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    block_id BIGINT NOT NULL,
    lesson_id BIGINT,
    attempt_number INT NOT NULL,
    answers TEXT NOT NULL,
    score INT NOT NULL,
    passed BOOLEAN NOT NULL,
    feedback TEXT,
    completed_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_attempt_enrollment FOREIGN KEY(enrollment_id) REFERENCES enrollments(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS assignment_submissions (
    id BIGSERIAL PRIMARY KEY,
    enrollment_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    block_id BIGINT NOT NULL,
    lesson_id BIGINT,
    text_content TEXT,
    file_urls TEXT,
    submitted_at TIMESTAMP NOT NULL,
    grade INT,
    feedback TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    CONSTRAINT fk_submission_enrollment FOREIGN KEY(enrollment_id) REFERENCES enrollments(id) ON DELETE CASCADE,
    CONSTRAINT uq_submission_block UNIQUE (enrollment_id, block_id)
);

CREATE INDEX IF NOT EXISTS idx_quiz_attempts_student ON quiz_attempts(student_id);
CREATE INDEX IF NOT EXISTS idx_quiz_attempts_enrollment_block ON quiz_attempts(enrollment_id, block_id);
-- Dos envíos simultáneos leen el mismo número de intentos previos; solo uno puede guardarse con ese número.
CREATE UNIQUE INDEX IF NOT EXISTS uq_quiz_attempts_number ON quiz_attempts(enrollment_id, block_id, attempt_number);
CREATE INDEX IF NOT EXISTS idx_submissions_student ON assignment_submissions(student_id);
CREATE INDEX IF NOT EXISTS idx_submissions_course ON assignment_submissions(course_id);

-- Cohorts. Member, course and path ids are kept as arrays: a group is always read and
-- written whole, and student/instructor ids live in ms-auth (no FK possible).
CREATE TABLE IF NOT EXISTS student_groups (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    institution_id VARCHAR(100) NOT NULL,
    instructor_id BIGINT,
    student_ids BIGINT[] NOT NULL DEFAULT '{}',
    course_ids BIGINT[] NOT NULL DEFAULT '{}',
    path_ids BIGINT[] NOT NULL DEFAULT '{}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_student_groups_institution ON student_groups(institution_id);

-- Learning path details added after the initial schema. Paths that existed before the
-- status column were visible to everyone, so they start as published.
ALTER TABLE learning_paths ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'published';
ALTER TABLE learning_paths ADD COLUMN IF NOT EXISTS tags VARCHAR(500);
ALTER TABLE learning_paths ADD COLUMN IF NOT EXISTS thumbnail_url VARCHAR(500);
ALTER TABLE learning_paths ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
ALTER TABLE learning_path_courses ADD COLUMN IF NOT EXISTS is_required BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE learning_path_courses ADD COLUMN IF NOT EXISTS minimum_score INT;

CREATE TABLE IF NOT EXISTS path_enrollments (
    id BIGSERIAL PRIMARY KEY,
    learning_path_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    enrolled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    UNIQUE (learning_path_id, student_id),
    CONSTRAINT fk_path_enrollment_path FOREIGN KEY(learning_path_id) REFERENCES learning_paths(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_path_enrollments_student ON path_enrollments(student_id);

-- Course feedback: one survey per course, one response and one review per student.
CREATE TABLE IF NOT EXISTS course_surveys (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL UNIQUE REFERENCES courses(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    sections TEXT NOT NULL DEFAULT '[]',
    published BOOLEAN NOT NULL DEFAULT false,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS survey_responses (
    id BIGSERIAL PRIMARY KEY,
    survey_id BIGINT NOT NULL REFERENCES course_surveys(id) ON DELETE CASCADE,
    course_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    answers TEXT NOT NULL DEFAULT '[]',
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (survey_id, student_id)
);

CREATE TABLE IF NOT EXISTS course_reviews (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    student_id BIGINT NOT NULL,
    rating SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (course_id, student_id)
);

-- In-app notifications. recipient_user_id NULL = for the staff of institution_id.
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    institution_id VARCHAR(100),
    recipient_user_id BIGINT,
    type VARCHAR(30) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    course_id BIGINT,
    reference_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_notifications_recipient ON notifications(recipient_user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_institution ON notifications(institution_id);

CREATE TABLE IF NOT EXISTS notification_reads (
    notification_id BIGINT NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL,
    read_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (notification_id, user_id)
);

-- Uploaded files (bytes in the configured storage, metadata here).
CREATE TABLE IF NOT EXISTS stored_files (
    id VARCHAR(36) PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    institution_id VARCHAR(100),
    scope VARCHAR(10) NOT NULL,
    name VARCHAR(255) NOT NULL,
    content_type VARCHAR(150) NOT NULL,
    size_bytes BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Module and lesson details added after the initial schema.
ALTER TABLE modules ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE lessons ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE lessons ADD COLUMN IF NOT EXISTS is_free BOOLEAN NOT NULL DEFAULT false;

-- Durable, publicly verifiable credentials. Names and titles are snapshots so an issued
-- certificate remains stable if the account or learning content changes later.
CREATE TABLE IF NOT EXISTS certificates (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    student_id BIGINT NOT NULL,
    student_name VARCHAR(200) NOT NULL,
    institution_id VARCHAR(100) NOT NULL,
    resource_type VARCHAR(30) NOT NULL CHECK (resource_type IN ('COURSE', 'LEARNING_PATH')),
    resource_id BIGINT NOT NULL,
    resource_title VARCHAR(300) NOT NULL,
    instructor_name VARCHAR(200),
    completed_at TIMESTAMP NOT NULL,
    issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP,
    UNIQUE (student_id, resource_type, resource_id)
);
CREATE INDEX IF NOT EXISTS idx_certificates_student ON certificates(student_id, issued_at DESC);

-- Gradebook: rubric scores of a graded submission (JSON [{criterionId, score, comment}]) and the
-- weight of each gradable block in the course grade (missing = 1).
ALTER TABLE assignment_submissions ADD COLUMN IF NOT EXISTS rubric_scores TEXT;
CREATE TABLE IF NOT EXISTS gradebook_weights (
    course_id BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    block_id BIGINT NOT NULL,
    weight INT NOT NULL CHECK (weight >= 0),
    PRIMARY KEY (course_id, block_id)
);
CREATE INDEX IF NOT EXISTS idx_quiz_attempts_course ON quiz_attempts(course_id);

-- Question bank: reusable questions per institution, grouped by category. payload is the question
-- JSON the web client writes in quiz blocks (type, question, options, correctAnswers...).
CREATE TABLE IF NOT EXISTS question_bank (
    id BIGSERIAL PRIMARY KEY,
    institution_id VARCHAR(100) NOT NULL,
    category VARCHAR(120) NOT NULL,
    type VARCHAR(30) NOT NULL,
    payload TEXT NOT NULL,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_question_bank_category ON question_bank(institution_id, category);

-- A quiz attempt in progress: the questions drawn and ordered for it (with answer keys, never sent
-- to the student) and its deadline. Submitting closes it; one open session per enrollment and block.
CREATE TABLE IF NOT EXISTS quiz_sessions (
    id BIGSERIAL PRIMARY KEY,
    enrollment_id BIGINT NOT NULL REFERENCES enrollments(id) ON DELETE CASCADE,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    block_id BIGINT NOT NULL,
    questions TEXT NOT NULL,
    student_questions TEXT NOT NULL,
    passing_score INT NOT NULL,
    started_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP,
    submitted_at TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_quiz_sessions_open ON quiz_sessions(enrollment_id, block_id) WHERE submitted_at IS NULL;

-- Course announcements, written by the course staff and pushed to enrolled students as notifications.
CREATE TABLE IF NOT EXISTS course_announcements (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    author_id BIGINT NOT NULL,
    author_name VARCHAR(200) NOT NULL,
    title VARCHAR(200) NOT NULL,
    body TEXT NOT NULL,
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_announcements_course ON course_announcements(course_id, created_at DESC);

-- Course forum: threads and their replies. Author names are copied when writing (users live in ms-auth).
CREATE TABLE IF NOT EXISTS forum_threads (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    author_id BIGINT NOT NULL,
    author_name VARCHAR(200) NOT NULL,
    author_role VARCHAR(30) NOT NULL,
    title VARCHAR(200) NOT NULL,
    body TEXT NOT NULL,
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    locked BOOLEAN NOT NULL DEFAULT FALSE,
    reply_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_activity_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_forum_threads_course ON forum_threads(course_id, pinned DESC, last_activity_at DESC);

CREATE TABLE IF NOT EXISTS forum_posts (
    id BIGSERIAL PRIMARY KEY,
    thread_id BIGINT NOT NULL REFERENCES forum_threads(id) ON DELETE CASCADE,
    author_id BIGINT NOT NULL,
    author_name VARCHAR(200) NOT NULL,
    author_role VARCHAR(30) NOT NULL,
    body TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_forum_posts_thread ON forum_posts(thread_id, created_at);

-- Academic periods of an institution (terms, semesters, cohorts in time).
CREATE TABLE IF NOT EXISTS academic_periods (
    id BIGSERIAL PRIMARY KEY,
    institution_id VARCHAR(100) NOT NULL,
    name VARCHAR(120) NOT NULL,
    starts_on DATE NOT NULL,
    ends_on DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (ends_on >= starts_on)
);
CREATE INDEX IF NOT EXISTS idx_academic_periods_institution ON academic_periods(institution_id, starts_on DESC);
-- Cierre del período: congela las notas finales (period_grade_records) y bloquea la actividad.
ALTER TABLE academic_periods ADD COLUMN IF NOT EXISTS closed_at TIMESTAMP;
ALTER TABLE academic_periods ADD COLUMN IF NOT EXISTS closed_by BIGINT;

CREATE TABLE IF NOT EXISTS period_grade_records (
    period_id BIGINT NOT NULL REFERENCES academic_periods(id) ON DELETE CASCADE,
    course_id BIGINT NOT NULL,
    course_title VARCHAR(255) NOT NULL,
    student_id BIGINT NOT NULL,
    enrollment_id BIGINT,
    final_grade DOUBLE PRECISION,
    current_grade DOUBLE PRECISION,
    progress INT,
    passed BOOLEAN NOT NULL DEFAULT FALSE,
    recorded_at TIMESTAMP NOT NULL,
    PRIMARY KEY (period_id, course_id, student_id)
);

-- Enrollment rules of a course; a course without a row has none. Window bounds are inclusive; when
-- closes_at is null and the course has a period, enrollment closes when the period ends.
CREATE TABLE IF NOT EXISTS course_enrollment_rules (
    course_id BIGINT PRIMARY KEY REFERENCES courses(id) ON DELETE CASCADE,
    period_id BIGINT REFERENCES academic_periods(id) ON DELETE SET NULL,
    opens_at TIMESTAMP,
    closes_at TIMESTAMP,
    capacity INT CHECK (capacity IS NULL OR capacity > 0),
    self_enrollment BOOLEAN NOT NULL DEFAULT TRUE,
    prerequisite_ids BIGINT[] NOT NULL DEFAULT '{}',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Course counters are derived from enrollments: recomputed on every enrollment change and,
-- here, once per startup so counters written by older versions are corrected.
UPDATE courses SET
    enrolled_count = (SELECT COUNT(*) FROM enrollments e WHERE e.course_id = courses.id),
    completion_rate = COALESCE((SELECT ROUND(100.0 * COUNT(*) FILTER (WHERE e.status = 'completed') / NULLIF(COUNT(*), 0))
                                FROM enrollments e WHERE e.course_id = courses.id), 0);

-- Gamificación: días con actividad de aprendizaje (rachas y meta semanal).
CREATE TABLE IF NOT EXISTS student_activity_days (
    student_id BIGINT NOT NULL,
    day DATE NOT NULL,
    actions INT NOT NULL DEFAULT 1,
    PRIMARY KEY (student_id, day)
);
-- Días de actividad anteriores a la tabla, a partir de intentos y entregas (idempotente).
INSERT INTO student_activity_days(student_id, day, actions)
SELECT student_id, CAST(completed_at AS DATE), COUNT(*) FROM quiz_attempts WHERE completed_at IS NOT NULL
GROUP BY student_id, CAST(completed_at AS DATE)
ON CONFLICT (student_id, day) DO NOTHING;
INSERT INTO student_activity_days(student_id, day, actions)
SELECT student_id, CAST(submitted_at AS DATE), COUNT(*) FROM assignment_submissions WHERE submitted_at IS NOT NULL
GROUP BY student_id, CAST(submitted_at AS DATE)
ON CONFLICT (student_id, day) DO NOTHING;

-- Comentarios de retroalimentación reutilizables de cada docente.
CREATE TABLE IF NOT EXISTS feedback_snippets (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    text VARCHAR(2000) NOT NULL,
    uses INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_feedback_snippets_text ON feedback_snippets(owner_id, md5(text));

-- Instituciones que piden su propio espacio desde la página pública.
CREATE TABLE IF NOT EXISTS institution_requests (
    id BIGSERIAL PRIMARY KEY,
    institution_name VARCHAR(160) NOT NULL,
    contact_name VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL,
    phone VARCHAR(40),
    role VARCHAR(60),
    students INT,
    message VARCHAR(2000),
    status VARCHAR(20) NOT NULL DEFAULT 'new',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
