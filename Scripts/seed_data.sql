-- ============================================================
-- SamaySetu - Sample Seed Data for Development
-- MIT Academy of Engineering (MITAOE), Alandi, Pune, Maharashtra
--
-- RUN THIS IN: pgAdmin → Query Tool → paste → F5
-- PREREQUISITE: Start the backend app once so Hibernate creates tables.
--
-- PASSWORDS:
--   Admin + All Teachers: Staff@123
-- ============================================================

-- Clear any leftover aborted transaction from a previous failed run
ROLLBACK;

-- ============================================================
-- STEP 0: WIPE ALL DATA (single TRUNCATE handles FK order)
-- ============================================================
TRUNCATE TABLE
  timetable_entries,
  lab_session_groups,
  teacher_courses,
  user_availability,
  students,
  batches,
  time_slots,
  courses,
  classrooms,
  divisions,
  departments,
  users,
  academic_years
CASCADE;

-- ============================================================
-- 1. ACADEMIC YEARS
-- ============================================================
INSERT INTO academic_years (id, year_name, start_date, end_date, is_current, created_at) VALUES
  (1, '2024-25', '2024-06-15', '2025-05-31', FALSE, NOW()),
  (2, '2025-26', '2025-06-16', '2026-05-31', TRUE,  NOW());

-- ============================================================
-- 2. DEPARTMENTS
-- ============================================================
INSERT INTO departments (id, name, code, head_of_department, years, academic_year_id, created_at, updated_at) VALUES
  -- Current year 2025-26
  (1, 'Computer Engineering',             'COMP',  'Dr. Rajesh Deshmukh', '1,2,3,4', 2, NOW(), NOW()),
  (2, 'Electronics & Telecommunication',  'ENTC',  'Dr. Sneha Kulkarni',  '1,2,3,4', 2, NOW(), NOW()),
  (3, 'Mechanical Engineering',           'MECH',  'Dr. Anil Patil',      '1,2,3,4', 2, NOW(), NOW()),
  (4, 'Civil Engineering',                'CIVIL', 'Dr. Suresh Jadhav',   '1,2,3,4', 2, NOW(), NOW()),
  (5, 'Artificial Intelligence & DS',     'AIDS',  'Dr. Priya Joshi',     '1,2,3,4', 2, NOW(), NOW()),
  -- Previous year 2024-25 (historical)
  (6, 'Computer Engineering',             'COMP',  'Dr. Rajesh Deshmukh', '1,2,3,4', 1, NOW(), NOW()),
  (7, 'Electronics & Telecommunication',  'ENTC',  'Dr. Sneha Kulkarni',  '1,2,3,4', 1, NOW(), NOW()),
  (8, 'Mechanical Engineering',           'MECH',  'Dr. Anil Patil',      '1,2,3,4', 1, NOW(), NOW());

-- ============================================================
-- 3. DIVISIONS
-- ============================================================
INSERT INTO divisions (id, name, branch, year, total_students, time_slot_type, class_teacher, is_active, department_id, academic_year_id, created_at, updated_at) VALUES
  -- Computer Engineering (dept 1)
  ( 1, 'A', 'Computer Engineering', 2, 65, 'TYPE_1', 'Prof. Amol Gaikwad',    TRUE, 1, 2, NOW(), NOW()),
  ( 2, 'B', 'Computer Engineering', 2, 63, 'TYPE_1', 'Prof. Kavita Bhosale',  TRUE, 1, 2, NOW(), NOW()),
  ( 3, 'A', 'Computer Engineering', 3, 60, 'TYPE_1', 'Prof. Nitin Deshpande', TRUE, 1, 2, NOW(), NOW()),
  ( 4, 'B', 'Computer Engineering', 3, 58, 'TYPE_1', 'Prof. Swati More',      TRUE, 1, 2, NOW(), NOW()),
  ( 5, 'A', 'Computer Engineering', 4, 55, 'TYPE_1', 'Prof. Sachin Wagh',     TRUE, 1, 2, NOW(), NOW()),
  -- ENTC (dept 2)
  ( 6, 'A', 'Electronics & Telecommunication', 2, 60, 'TYPE_1', 'Prof. Rupali Mane',  TRUE, 2, 2, NOW(), NOW()),
  ( 7, 'A', 'Electronics & Telecommunication', 3, 55, 'TYPE_1', 'Prof. Manoj Shinde', TRUE, 2, 2, NOW(), NOW()),
  -- Mechanical (dept 3)
  ( 8, 'A', 'Mechanical Engineering', 2, 58, 'TYPE_1', 'Prof. Vikram Pawar',  TRUE, 3, 2, NOW(), NOW()),
  ( 9, 'A', 'Mechanical Engineering', 3, 50, 'TYPE_1', 'Prof. Deepak Chavan', TRUE, 3, 2, NOW(), NOW()),
  -- AIDS (dept 5)
  (10, 'A', 'Artificial Intelligence & DS', 2, 60, 'TYPE_1', 'Prof. Ashwini Kale', TRUE, 5, 2, NOW(), NOW());

-- ============================================================
-- 4. BATCHES (3 lab groups per division)
-- ============================================================
INSERT INTO batches (id, name, division_id, created_at, updated_at) VALUES
  -- COMP SY-A (div 1)
  ( 1, 'B1', 1, NOW(), NOW()), ( 2, 'B2', 1, NOW(), NOW()), ( 3, 'B3', 1, NOW(), NOW()),
  -- COMP SY-B (div 2)
  ( 4, 'B1', 2, NOW(), NOW()), ( 5, 'B2', 2, NOW(), NOW()), ( 6, 'B3', 2, NOW(), NOW()),
  -- COMP TY-A (div 3)
  ( 7, 'B1', 3, NOW(), NOW()), ( 8, 'B2', 3, NOW(), NOW()), ( 9, 'B3', 3, NOW(), NOW()),
  -- ENTC SY-A (div 6)
  (10, 'B1', 6, NOW(), NOW()), (11, 'B2', 6, NOW(), NOW()), (12, 'B3', 6, NOW(), NOW());

-- ============================================================
-- 5. COURSES
-- ============================================================
INSERT INTO courses (id, name, code, course_type, semester, year, credits, hours_per_week, description, is_active, department_id, created_at, updated_at) VALUES
  -- COMP SY Sem 3
  ( 1, 'Data Structures & Algorithms',  'COMP201', 'THEORY', 'SEM_3', 2, 4, 4, 'Arrays, linked lists, trees, graphs, sorting, searching',        TRUE, 1, NOW(), NOW()),
  ( 2, 'DSA Laboratory',                'COMP202', 'LAB',    'SEM_3', 2, 2, 2, 'Practical implementation of data structures using C/C++',         TRUE, 1, NOW(), NOW()),
  ( 3, 'Object Oriented Programming',   'COMP203', 'THEORY', 'SEM_3', 2, 3, 3, 'OOP using Java — classes, inheritance, polymorphism',             TRUE, 1, NOW(), NOW()),
  ( 4, 'OOP Laboratory',                'COMP204', 'LAB',    'SEM_3', 2, 2, 2, 'Java programming lab',                                           TRUE, 1, NOW(), NOW()),
  ( 5, 'Discrete Mathematics',          'COMP205', 'THEORY', 'SEM_3', 2, 3, 3, 'Sets, relations, graph theory, combinatorics, logic',             TRUE, 1, NOW(), NOW()),
  ( 6, 'Digital Logic Design',          'COMP206', 'THEORY', 'SEM_3', 2, 3, 3, 'Boolean algebra, combinational/sequential circuits',              TRUE, 1, NOW(), NOW()),
  ( 7, 'Digital Logic Design Lab',      'COMP207', 'LAB',    'SEM_3', 2, 1, 2, 'Hardware implementation of digital circuits',                     TRUE, 1, NOW(), NOW()),
  -- COMP SY Sem 4
  ( 8, 'Database Management Systems',   'COMP208', 'THEORY', 'SEM_4', 2, 4, 4, 'Relational model, SQL, normalization, transactions',              TRUE, 1, NOW(), NOW()),
  ( 9, 'DBMS Laboratory',               'COMP209', 'LAB',    'SEM_4', 2, 2, 2, 'SQL queries, PL/SQL, triggers using PostgreSQL',                  TRUE, 1, NOW(), NOW()),
  (10, 'Computer Networks',             'COMP210', 'THEORY', 'SEM_4', 2, 3, 3, 'OSI model, TCP/IP, routing, switching',                           TRUE, 1, NOW(), NOW()),
  (11, 'Operating Systems',             'COMP211', 'THEORY', 'SEM_4', 2, 3, 3, 'Process management, memory, file systems, scheduling',            TRUE, 1, NOW(), NOW()),
  (12, 'OS Laboratory',                 'COMP212', 'LAB',    'SEM_4', 2, 1, 2, 'Linux commands, shell scripting, process scheduling',             TRUE, 1, NOW(), NOW()),
  -- COMP TY Sem 5
  (13, 'Machine Learning',              'COMP301', 'THEORY', 'SEM_5', 3, 4, 4, 'Supervised/unsupervised learning, regression, neural networks',   TRUE, 1, NOW(), NOW()),
  (14, 'ML Laboratory',                 'COMP302', 'LAB',    'SEM_5', 3, 2, 2, 'Python ML — scikit-learn, TensorFlow',                            TRUE, 1, NOW(), NOW()),
  (15, 'Web Development',               'COMP303', 'THEORY', 'SEM_5', 3, 3, 3, 'HTML, CSS, JavaScript, React, Node.js, REST APIs',               TRUE, 1, NOW(), NOW()),
  (16, 'Web Dev Laboratory',            'COMP304', 'LAB',    'SEM_5', 3, 2, 2, 'Full-stack project using MERN/Spring Boot',                       TRUE, 1, NOW(), NOW()),
  (17, 'Software Engineering',          'COMP305', 'THEORY', 'SEM_5', 3, 3, 3, 'SDLC, Agile, testing, UML, design patterns',                     TRUE, 1, NOW(), NOW()),
  (18, 'Theory of Computation',         'COMP306', 'THEORY', 'SEM_5', 3, 3, 3, 'Automata, regular expressions, CFGs, Turing machines',            TRUE, 1, NOW(), NOW()),
  -- COMP BTech Sem 7
  (19, 'Cloud Computing',               'COMP401', 'THEORY', 'SEM_7', 4, 3, 3, 'AWS, Azure, containers, microservices',                           TRUE, 1, NOW(), NOW()),
  (20, 'Deep Learning',                 'COMP402', 'THEORY', 'SEM_7', 4, 3, 3, 'CNNs, RNNs, GANs, transformers, NLP',                            TRUE, 1, NOW(), NOW()),
  -- ENTC SY Sem 3
  (21, 'Signals & Systems',             'ENTC201', 'THEORY', 'SEM_3', 2, 4, 4, 'Continuous/discrete signals, Fourier, Laplace transforms',        TRUE, 2, NOW(), NOW()),
  (22, 'Electronic Devices & Circuits', 'ENTC202', 'THEORY', 'SEM_3', 2, 3, 3, 'Diodes, BJT, FET, amplifier circuits',                           TRUE, 2, NOW(), NOW()),
  (23, 'EDC Laboratory',                'ENTC203', 'LAB',    'SEM_3', 2, 2, 2, 'Circuits on breadboard and LTspice simulation',                   TRUE, 2, NOW(), NOW()),
  (24, 'Network Analysis',              'ENTC204', 'THEORY', 'SEM_3', 2, 3, 3, 'KVL, KCL, Thevenin/Norton theorems, AC circuits',                TRUE, 2, NOW(), NOW()),
  -- ENTC TY Sem 5
  (25, 'Microprocessors & Controllers', 'ENTC301', 'THEORY', 'SEM_5', 3, 4, 4, '8085, 8051, ARM architecture, embedded C',                       TRUE, 2, NOW(), NOW()),
  (26, 'Microprocessor Lab',            'ENTC302', 'LAB',    'SEM_5', 3, 2, 2, 'Assembly language, interfacing experiments',                      TRUE, 2, NOW(), NOW()),
  -- MECH SY Sem 3
  (27, 'Engineering Thermodynamics',    'MECH201', 'THEORY', 'SEM_3', 2, 4, 4, 'Laws of thermodynamics, entropy, Carnot cycle',                   TRUE, 3, NOW(), NOW()),
  (28, 'Strength of Materials',         'MECH202', 'THEORY', 'SEM_3', 2, 3, 3, 'Stress, strain, bending moments, shear force',                    TRUE, 3, NOW(), NOW()),
  (29, 'SOM Laboratory',                'MECH203', 'LAB',    'SEM_3', 2, 2, 2, 'Tensile, hardness, impact testing on UTM',                        TRUE, 3, NOW(), NOW()),
  (30, 'Fluid Mechanics',               'MECH204', 'THEORY', 'SEM_3', 2, 3, 3, 'Fluid properties, Bernoulli, pipe flow',                          TRUE, 3, NOW(), NOW()),
  (31, 'Manufacturing Processes',        'MECH205', 'THEORY', 'SEM_3', 2, 3, 3, 'Casting, welding, machining, forming, CNC',                      TRUE, 3, NOW(), NOW()),
  (32, 'Manufacturing Processes Lab',    'MECH206', 'LAB',    'SEM_3', 2, 1, 2, 'Lathe, milling, welding, fitting workshop',                      TRUE, 3, NOW(), NOW());

-- ============================================================
-- 6. CLASSROOMS
-- ============================================================
INSERT INTO classrooms (id, name, room_number, building_wing, capacity, room_type, has_projector, has_ac, equipment, is_active, department_id, created_at, updated_at) VALUES
  -- A Wing — Lecture halls
  ( 1, 'Lecture Hall A101',      'A101', 'A', 120, 'CLASSROOM',  TRUE,  TRUE,  'Smart board, mic system',           TRUE, NULL, NOW(), NOW()),
  ( 2, 'Lecture Hall A102',      'A102', 'A',  80, 'CLASSROOM',  TRUE,  TRUE,  'Projector, whiteboard',             TRUE, NULL, NOW(), NOW()),
  ( 3, 'Classroom A201',         'A201', 'A',  65, 'CLASSROOM',  TRUE,  FALSE, 'Projector, whiteboard',             TRUE, NULL, NOW(), NOW()),
  ( 4, 'Classroom A202',         'A202', 'A',  65, 'CLASSROOM',  TRUE,  FALSE, 'Projector, whiteboard',             TRUE, NULL, NOW(), NOW()),
  ( 5, 'Classroom A203',         'A203', 'A',  65, 'CLASSROOM',  TRUE,  FALSE, 'Projector, whiteboard',             TRUE, NULL, NOW(), NOW()),
  ( 6, 'Classroom A301',         'A301', 'A',  60, 'CLASSROOM',  TRUE,  FALSE, 'Projector, whiteboard',             TRUE, NULL, NOW(), NOW()),
  ( 7, 'Classroom A302',         'A302', 'A',  60, 'CLASSROOM',  TRUE,  FALSE, 'Projector, whiteboard',             TRUE, NULL, NOW(), NOW()),
  -- B Wing — COMP / AIDS labs
  ( 8, 'Computer Lab B101',      'B101', 'B',  40, 'LAB', TRUE, TRUE, '40 PCs, i5 12th Gen, 16GB RAM',    TRUE, 1, NOW(), NOW()),
  ( 9, 'Computer Lab B102',      'B102', 'B',  40, 'LAB', TRUE, TRUE, '40 PCs, i5 12th Gen, 16GB RAM',    TRUE, 1, NOW(), NOW()),
  (10, 'AI/ML Lab B201',         'B201', 'B',  35, 'LAB', TRUE, TRUE, '35 PCs, RTX 3060, 32GB RAM',       TRUE, 5, NOW(), NOW()),
  (11, 'Network Lab B202',       'B202', 'B',  30, 'LAB', TRUE, TRUE, 'Cisco routers, managed switches',   TRUE, 1, NOW(), NOW()),
  -- C Wing — ENTC labs
  (12, 'Electronics Lab C101',   'C101', 'C',  35, 'LAB', TRUE, FALSE, 'CRO, function generators, DSO',    TRUE, 2, NOW(), NOW()),
  (13, 'Microprocessor Lab C102','C102', 'C',  30, 'LAB', TRUE, FALSE, '8085/8051 kits, ARM boards',       TRUE, 2, NOW(), NOW()),
  -- D Wing — MECH + Auditorium
  (14, 'Workshop D001',          'D001', 'D',  40, 'LAB',        FALSE, FALSE, 'Lathe, milling, drill press, CNC', TRUE, 3, NOW(), NOW()),
  (15, 'Thermal Lab D101',       'D101', 'D',  30, 'LAB',        TRUE,  FALSE, 'IC engine setups, calorimeters',   TRUE, 3, NOW(), NOW()),
  (16, 'Seminar Hall',           'D201', 'D', 200, 'AUDITORIUM', TRUE,  TRUE,  'Stage, PA system, video conf',     TRUE, NULL, NOW(), NOW());

-- ============================================================
-- 7. TIME SLOTS
-- ============================================================
INSERT INTO time_slots (id, start_time, end_time, duration_minutes, slot_name, type, is_break, is_active, created_at) VALUES
  ( 1, '09:00', '10:00', 60,  'Period 1',    'TYPE_1', FALSE, TRUE, NOW()),
  ( 2, '10:00', '11:00', 60,  'Period 2',    'TYPE_1', FALSE, TRUE, NOW()),
  ( 3, '11:00', '11:15', 15,  'Tea Break',   'TYPE_1', TRUE,  TRUE, NOW()),
  ( 4, '11:15', '12:15', 60,  'Period 3',    'TYPE_1', FALSE, TRUE, NOW()),
  ( 5, '12:15', '13:15', 60,  'Period 4',    'TYPE_1', FALSE, TRUE, NOW()),
  ( 6, '13:15', '14:00', 45,  'Lunch Break', 'TYPE_1', TRUE,  TRUE, NOW()),
  ( 7, '14:00', '15:00', 60,  'Period 5',    'TYPE_1', FALSE, TRUE, NOW()),
  ( 8, '15:00', '16:00', 60,  'Period 6',    'TYPE_1', FALSE, TRUE, NOW()),
  ( 9, '16:00', '16:15', 15,  'Tea Break',   'TYPE_1', TRUE,  TRUE, NOW()),
  (10, '16:15', '17:15', 60,  'Period 7',    'TYPE_1', FALSE, TRUE, NOW()),
  ( 11, '09:00', '10:00', 60,  'Period 1',    'TYPE_2', FALSE, TRUE, NOW()),
  ( 12, '10:00', '11:00', 60,  'Period 2',    'TYPE_2', FALSE, TRUE, NOW()),
  ( 13, '11:00', '11:15', 15,  'Tea Break',   'TYPE_2', TRUE,  TRUE, NOW()),
  ( 14, '11:15', '12:15', 60,  'Period 3',    'TYPE_2', FALSE, TRUE, NOW()),
  ( 15, '12:15', '13:15', 60,  'Period 4',    'TYPE_2', FALSE, TRUE, NOW()),
  ( 16, '13:15', '14:00', 45,  'Lunch Break', 'TYPE_2', TRUE,  TRUE, NOW()),
  ( 17, '14:00', '15:00', 60,  'Period 5',    'TYPE_2', FALSE, TRUE, NOW()),
  ( 18, '15:00', '16:00', 60,  'Period 6',    'TYPE_2', FALSE, TRUE, NOW()),
  ( 19, '16:00', '16:15', 15,  'Tea Break',   'TYPE_2', TRUE,  TRUE, NOW()),
  (20, '16:15', '17:15', 60,  'Period 7',    'TYPE_2', FALSE, TRUE, NOW());

-- ============================================================
-- 8. USERS (Admin + 20 Teachers)
--    Password for ALL: Staff@123
-- ============================================================
INSERT INTO users (id, name, employee_id, email, phone, password, role, specialization,
                   min_weekly_hours, max_weekly_hours, is_active, is_approved,
                   is_email_verified, is_first_login, department_id, created_at, updated_at) VALUES
  -- Admin
  ( 1, 'Admin User',             'EMP00005', 'suryankadmin@mitaoe.ac.in',     NULL,
    '$2a$10$BUwvTms.A0op8W6dH2AOXujLrVaavO22cTu0agZs8p5NaSZH1Va.e', 'ADMIN',
    NULL, 10, 40, TRUE, TRUE, TRUE, FALSE, NULL, NOW(), NOW()),
  -- COMP (dept 1)
  ( 2, 'Dr. Rajesh Deshmukh',    'COMP001', 'rajesh.deshmukh@mitaoe.ac.in',  '9823456701',
    '$2a$10$BUwvTms.A0op8W6dH2AOXujLrVaavO22cTu0agZs8p5NaSZH1Va.e', 'TEACHER',
    'Data Structures, Algorithms, Competitive Programming',       12, 20, TRUE, TRUE, TRUE, FALSE, 1, NOW(), NOW()),
  ( 3, 'Prof. Amol Gaikwad',     'COMP002', 'amol.gaikwad@mitaoe.ac.in',     '9823456702',
    '$2a$10$BUwvTms.A0op8W6dH2AOXujLrVaavO22cTu0agZs8p5NaSZH1Va.e', 'TEACHER',
    'Object Oriented Programming, Java, Design Patterns',         14, 22, TRUE, TRUE, TRUE, FALSE, 1, NOW(), NOW()),
  ( 4, 'Prof. Kavita Bhosale',   'COMP003', 'kavita.bhosale@mitaoe.ac.in',   '9823456703',
    '$2a$10$BUwvTms.A0op8W6dH2AOXujLrVaavO22cTu0agZs8p5NaSZH1Va.e', 'TEACHER',
    'Database Systems, SQL, Data Warehousing',                    12, 20, TRUE, TRUE, TRUE, FALSE, 1, NOW(), NOW()),
  ( 5, 'Prof. Nitin Deshpande',  'COMP004', 'nitin.deshpande@mitaoe.ac.in',  '9823456704',
    '$2a$10$BUwvTms.A0op8W6dH2AOXujLrVaavO22cTu0agZs8p5NaSZH1Va.e', 'TEACHER',
    'Machine Learning, Deep Learning, Computer Vision',           10, 18, TRUE, TRUE, TRUE, FALSE, 1, NOW(), NOW()),
  ( 6, 'Prof. Swati More',       'COMP005', 'swati.more@mitaoe.ac.in',       '9823456705',
    '$2a$10$BUwvTms.A0op8W6dH2AOXujLrVaavO22cTu0agZs8p5NaSZH1Va.e', 'TEACHER',
    'Web Technologies, React, Node.js, Cloud Computing',          14, 22, TRUE, TRUE, TRUE, FALSE, 1, NOW(), NOW()),
  ( 7, 'Prof. Sachin Wagh',      'COMP006', 'sachin.wagh@mitaoe.ac.in',      '9823456706',
    '$2a$10$BUwvTms.A0op8W6dH2AOXujLrVaavO22cTu0agZs8p5NaSZH1Va.e', 'TEACHER',
    'Computer Networks, Network Security, IoT',                   12, 20, TRUE, TRUE, TRUE, FALSE, 1, NOW(), NOW()),
  ( 8, 'Prof. Prashant Kulkarni','COMP007', 'prashant.kulkarni@mitaoe.ac.in','9823456707',
    '$2a$10$BUwvTms.A0op8W6dH2AOXujLrVaavO22cTu0agZs8p5NaSZH1Va.e', 'TEACHER',
    'Operating Systems, Linux, System Programming',               14, 24, TRUE, TRUE, TRUE, FALSE, 1, NOW(), NOW()),
  ( 9, 'Prof. Manisha Pawar',    'COMP008', 'manisha.pawar@mitaoe.ac.in',    '9823456708',
    '$2a$10$BUwvTms.A0op8W6dH2AOXujLrVaavO22cTu0agZs8p5NaSZH1Va.e', 'TEACHER',
    'Discrete Mathematics, Theory of Computation, Automata',      12, 20, TRUE, TRUE, TRUE, FALSE, 1, NOW(), NOW()),
  (10, 'Prof. Ganesh Thombre',   'COMP009', 'ganesh.thombre@mitaoe.ac.in',   '9823456709',
    '$2a$10$BUwvTms.A0op8W6dH2AOXujLrVaavO22cTu0agZs8p5NaSZH1Va.e', 'TEACHER',
    'Software Engineering, Agile, DevOps',                        10, 18, TRUE, TRUE, TRUE, FALSE, 1, NOW(), NOW()),
  (11, 'Prof. Ashwini Kale',     'COMP010', 'ashwini.kale@mitaoe.ac.in',     '9823456710',
    '$2a$10$BUwvTms.A0op8W6dH2AOXujLrVaavO22cTu0agZs8p5NaSZH1Va.e', 'TEACHER',
    'Digital Logic Design, Computer Architecture, VLSI',          14, 22, TRUE, TRUE, TRUE, FALSE, 1, NOW(), NOW()),
  -- ENTC (dept 2)
  (12, 'Dr. Sneha Kulkarni',     'ENTC001', 'sneha.kulkarni@mitaoe.ac.in',   '9823456711',
    '$2a$10$BUwvTms.A0op8W6dH2AOXujLrVaavO22cTu0agZs8p5NaSZH1Va.e', 'TEACHER',
    'Signals & Systems, DSP, Communication Systems',              10, 18, TRUE, TRUE, TRUE, FALSE, 2, NOW(), NOW()),
  (13, 'Prof. Rupali Mane',      'ENTC002', 'rupali.mane@mitaoe.ac.in',      '9823456712',
    '$2a$10$BUwvTms.A0op8W6dH2AOXujLrVaavO22cTu0agZs8p5NaSZH1Va.e', 'TEACHER',
    'Electronic Devices, Analog Circuits, PCB Design',            14, 22, TRUE, TRUE, TRUE, FALSE, 2, NOW(), NOW()),
  (14, 'Prof. Manoj Shinde',     'ENTC003', 'manoj.shinde@mitaoe.ac.in',     '9823456713',
    '$2a$10$BUwvTms.A0op8W6dH2AOXujLrVaavO22cTu0agZs8p5NaSZH1Va.e', 'TEACHER',
    'Microprocessors, Embedded Systems, ARM Programming',         12, 20, TRUE, TRUE, TRUE, FALSE, 2, NOW(), NOW()),
  (15, 'Prof. Anjali Raut',      'ENTC004', 'anjali.raut@mitaoe.ac.in',      '9823456714',
    '$2a$10$BUwvTms.A0op8W6dH2AOXujLrVaavO22cTu0agZs8p5NaSZH1Va.e', 'TEACHER',
    'Network Analysis, Control Systems',                          14, 22, TRUE, TRUE, TRUE, FALSE, 2, NOW(), NOW()),
  -- MECH (dept 3)
  (16, 'Dr. Anil Patil',         'MECH001', 'anil.patil@mitaoe.ac.in',       '9823456715',
    '$2a$10$BUwvTms.A0op8W6dH2AOXujLrVaavO22cTu0agZs8p5NaSZH1Va.e', 'TEACHER',
    'Thermodynamics, Heat Transfer, IC Engines',                  10, 18, TRUE, TRUE, TRUE, FALSE, 3, NOW(), NOW()),
  (17, 'Prof. Vikram Pawar',     'MECH002', 'vikram.pawar@mitaoe.ac.in',     '9823456716',
    '$2a$10$BUwvTms.A0op8W6dH2AOXujLrVaavO22cTu0agZs8p5NaSZH1Va.e', 'TEACHER',
    'Strength of Materials, Structural Analysis, FEA',            14, 22, TRUE, TRUE, TRUE, FALSE, 3, NOW(), NOW()),
  (18, 'Prof. Deepak Chavan',    'MECH003', 'deepak.chavan@mitaoe.ac.in',    '9823456717',
    '$2a$10$BUwvTms.A0op8W6dH2AOXujLrVaavO22cTu0agZs8p5NaSZH1Va.e', 'TEACHER',
    'Fluid Mechanics, Hydraulics, Turbomachinery',                12, 20, TRUE, TRUE, TRUE, FALSE, 3, NOW(), NOW()),
  (19, 'Prof. Sunil Londhe',     'MECH004', 'sunil.londhe@mitaoe.ac.in',     '9823456718',
    '$2a$10$BUwvTms.A0op8W6dH2AOXujLrVaavO22cTu0agZs8p5NaSZH1Va.e', 'TEACHER',
    'Manufacturing Processes, CNC, CAD/CAM, 3D Printing',         14, 24, TRUE, TRUE, TRUE, FALSE, 3, NOW(), NOW()),
  -- AIDS (dept 5)
  (20, 'Dr. Priya Joshi',        'AIDS001', 'priya.joshi@mitaoe.ac.in',      '9823456719',
    '$2a$10$BUwvTms.A0op8W6dH2AOXujLrVaavO22cTu0agZs8p5NaSZH1Va.e', 'TEACHER',
    'Artificial Intelligence, NLP, Data Science, Python',         10, 18, TRUE, TRUE, TRUE, FALSE, 5, NOW(), NOW()),
  -- Pending approval (test the approval flow)
  (21, 'Prof. Rohit Bhagat',     'COMP011', 'rohit.bhagat@mitaoe.ac.in',     '9823456720',
    '$2a$10$BUwvTms.A0op8W6dH2AOXujLrVaavO22cTu0agZs8p5NaSZH1Va.e', 'TEACHER',
    'Cybersecurity, Ethical Hacking, Blockchain',                 12, 20, TRUE, FALSE, TRUE, TRUE, 1, NOW(), NOW());

-- ============================================================
-- 9. TEACHER-COURSE ASSIGNMENTS
-- ============================================================
INSERT INTO teacher_courses (user_id, course_id) VALUES
  (2,1),(2,2),     -- Deshmukh → DSA + DSA Lab
  (3,3),(3,4),     -- Gaikwad → OOP + OOP Lab
  (4,8),(4,9),     -- Bhosale → DBMS + DBMS Lab
  (5,13),(5,14),(5,20), -- Deshpande → ML + ML Lab + Deep Learning
  (6,15),(6,16),(6,19), -- More → Web Dev + Lab + Cloud
  (7,10),          -- Wagh → Computer Networks
  (8,11),(8,12),   -- Kulkarni → OS + OS Lab
  (9,5),(9,18),    -- Pawar → Discrete Maths + Theory of Computation
  (10,17),         -- Thombre → Software Engineering
  (11,6),(11,7),   -- Kale → DLD + DLD Lab
  (12,21),         -- Sneha → Signals & Systems
  (13,22),(13,23), -- Mane → EDC + EDC Lab
  (14,25),(14,26), -- Shinde → Microprocessors + Lab
  (15,24),         -- Raut → Network Analysis
  (16,27),         -- Patil → Thermodynamics
  (17,28),(17,29), -- Pawar → SOM + SOM Lab
  (18,30),         -- Chavan → Fluid Mechanics
  (19,31),(19,32); -- Londhe → Manufacturing + Lab

-- ============================================================
-- 10. SAMPLE STUDENTS (COMP SY-A and SY-B)
-- ============================================================
INSERT INTO students (id, name, roll_number, email, phone, admission_year, is_active, division_id, created_at, updated_at) VALUES
  ( 1, 'Aarav Sharma',      'COMP2025001', 'aarav.sharma@mitaoe.ac.in',    '9876543201', 2024, TRUE, 1, NOW(), NOW()),
  ( 2, 'Saanvi Patil',      'COMP2025002', 'saanvi.patil@mitaoe.ac.in',    '9876543202', 2024, TRUE, 1, NOW(), NOW()),
  ( 3, 'Vihaan Deshmukh',   'COMP2025003', 'vihaan.deshmukh@mitaoe.ac.in', '9876543203', 2024, TRUE, 1, NOW(), NOW()),
  ( 4, 'Ananya Kulkarni',   'COMP2025004', 'ananya.kulkarni@mitaoe.ac.in', '9876543204', 2024, TRUE, 1, NOW(), NOW()),
  ( 5, 'Arjun Jadhav',      'COMP2025005', 'arjun.jadhav@mitaoe.ac.in',    '9876543205', 2024, TRUE, 1, NOW(), NOW()),
  ( 6, 'Diya Bhosale',      'COMP2025006', 'diya.bhosale@mitaoe.ac.in',    '9876543206', 2024, TRUE, 1, NOW(), NOW()),
  ( 7, 'Reyansh Gaikwad',   'COMP2025007', 'reyansh.gaikwad@mitaoe.ac.in', '9876543207', 2024, TRUE, 1, NOW(), NOW()),
  ( 8, 'Ishita Wagh',       'COMP2025008', 'ishita.wagh@mitaoe.ac.in',     '9876543208', 2024, TRUE, 1, NOW(), NOW()),
  ( 9, 'Aditya Thorat',     'COMP2025009', 'aditya.thorat@mitaoe.ac.in',   '9876543209', 2024, TRUE, 2, NOW(), NOW()),
  (10, 'Kavya Shinde',      'COMP2025010', 'kavya.shinde@mitaoe.ac.in',    '9876543210', 2024, TRUE, 2, NOW(), NOW());

-- ============================================================
-- 11. RESET SEQUENCES so next auto-ID is correct
-- ============================================================
SELECT setval('academic_years_id_seq', (SELECT MAX(id) FROM academic_years));
SELECT setval('departments_id_seq',    (SELECT MAX(id) FROM departments));
SELECT setval('divisions_id_seq',      (SELECT MAX(id) FROM divisions));
SELECT setval('batches_id_seq',        (SELECT MAX(id) FROM batches));
SELECT setval('courses_id_seq',        (SELECT MAX(id) FROM courses));
SELECT setval('classrooms_id_seq',     (SELECT MAX(id) FROM classrooms));
SELECT setval('time_slots_id_seq',     (SELECT MAX(id) FROM time_slots));
SELECT setval('users_id_seq',          (SELECT MAX(id) FROM users));
SELECT setval('students_id_seq',       (SELECT MAX(id) FROM students));

-- ============================================================
-- DONE! Verify with:
-- ============================================================
SELECT 'academic_years' AS tbl, COUNT(*) AS rows FROM academic_years
UNION ALL SELECT 'departments',    COUNT(*) FROM departments
UNION ALL SELECT 'divisions',      COUNT(*) FROM divisions
UNION ALL SELECT 'batches',        COUNT(*) FROM batches
UNION ALL SELECT 'courses',        COUNT(*) FROM courses
UNION ALL SELECT 'classrooms',     COUNT(*) FROM classrooms
UNION ALL SELECT 'time_slots',     COUNT(*) FROM time_slots
UNION ALL SELECT 'users',          COUNT(*) FROM users
UNION ALL SELECT 'teacher_courses',COUNT(*) FROM teacher_courses
UNION ALL SELECT 'students',       COUNT(*) FROM students
ORDER BY tbl;

-- ============================================================
-- LOGIN CREDENTIALS (all passwords: Staff@123)
-- ============================================================
-- ADMIN:   suryankadmin@mitaoe.ac.in
-- TEACHER: rajesh.deshmukh@mitaoe.ac.in
-- TEACHER: amol.gaikwad@mitaoe.ac.in
-- TEACHER: kavita.bhosale@mitaoe.ac.in
-- (all 20 teachers use Staff@123)
-- PENDING: rohit.bhagat@mitaoe.ac.in (is_approved=false)
-- ============================================================
