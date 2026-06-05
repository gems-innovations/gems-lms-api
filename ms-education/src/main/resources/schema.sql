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
