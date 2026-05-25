# SamaySetu Major Project Report — Update Log

This folder contains the revised version of the Major Project Report LaTeX package.

## Files Changed (all `.tex`)

| File | Status | Summary of changes |
|------|--------|---------------------|
| `TY_Major_Report.tex` | Updated | Title now reads *"SamaySetu — College Timetable Management System"* (was "Samay Setu – Smart Timetable Generation System"). Project advisor title changed from **Mr.** to **Dr.** Vijaykumar P. Mantri. |
| `abstract.tex` | Rewritten | Removes claims of OptaPlanner / auto-generation (not implemented). Replaces MySQL with PostgreSQL 17. Adds Redis, JWT auth, BCrypt, role-based access (ADMIN/HOD/TIMETABLE_COORDINATOR/TEACHER), drag-and-drop, lab wizard, pre-publish validation, AWS deployment. |
| `acknowledgement.tex` | Rewritten | More polished prose, tighter paragraph structure. Guide is now **Dr. Vijaykumar P. Mantri**. |
| `chapter1.tex` | Rewritten | Honest framing: system is a manual builder with intelligent conflict prevention, not opaque auto-generation. Cleaner introduction, motivation, project idea, proposed solution, and chapter summary. |
| `chapter2.tex` | Rewritten | Same literature references retained. Professional tone throughout. Added a fair limitations-of-SOTA section and positions SamaySetu as complementary to metaheuristic research, not competing with it. |
| `chapter3.tex` | Rewritten | Hardware/software requirements now match the actual stack (PostgreSQL 17, Redis, React 18, TypeScript, Vite, Tailwind, Spring Boot 3.5.5, Java 17, OpenPDF, Apache POI, @dnd-kit, etc). No more OptaPlanner or iText references. |
| `chapter4.tex` | Rewritten | Fixed corrupted Unicode characters (`��`, orphan `}`) from the original. Removed OptaPlanner references. Reflects real architecture with real controllers, services, filters, configuration, and external integrations. Updated project plan to match actual phases. |
| `chapter5.tex` | Rewritten | Renamed from "Proposed Methodology" to "System Design, Implementation, and Results". Documents the real 8-point conflict detection algorithm formally, the lab session atomic creation flow, pre-publish validation, caching strategy, security model, testing outcomes, and development approach. |
| `chapter6.tex` | Rewritten | Conclusion updated to reflect what the system actually does. Future scope organised into near-term / medium-term / strategic tiers. Removed claims about AI-based auto-generation already being built. |
| `references_UG.bib` | Extended | Original references retained + added Spring Boot, React, PostgreSQL, and JJWT official docs, plus Fowler and Evans books for general engineering foundations. |
| `bibliography.tex` | Unchanged | — |
| `appendix.tex` | Unchanged | — |
| `appendix1.tex` | Rewritten | Data dictionary fixed (column `user_id` not `teacher_id`; column `classroom_id` not `room_id`). Added tables for departments, divisions, courses, classrooms, time_slots, users, lab_session_groups, user_availability. Expanded API spec with actual endpoint paths. Added deployment configuration summary. |
| `listoffigures.tex` | Unchanged | — |
| `listoftables.tex` | Unchanged | — |

## Files Unchanged (copied across verbatim)

- `mitaoeminorreportty.cls` — the MITAOE report class definition
- `maelogo.jpg`, `image.png`, `image2.png`, `planning.png`, `schema_sql.png`, `block-cropped.pdf` — figures

## Key Corrections to Factual Claims

1. **Backend**: was **MySQL** → now **PostgreSQL 17**
2. **Scheduling engine**: was **OptaPlanner** (never existed in code) → now **8-point conflict detection engine** (honest)
3. **PDF library**: was **iText** → now **OpenPDF** (correct — iText went commercial; OpenPDF is the LGPL fork)
4. **Roles**: was mentioning only Admin / Teacher / Student → now **ADMIN / HOD / TIMETABLE_COORDINATOR / TEACHER** (four real roles)
5. **Auto-generation claim**: removed from conclusion; mentioned only as future scope
6. **Tech stack additions**: Redis caching, JWT, Spring Security, React 18, TypeScript, Vite, Tailwind, @dnd-kit, AWS deployment — all now present in the report
7. **Database schema (appendix)**: column names corrected to match the real Hibernate mapping

## Compile Instructions

The report is a standard LaTeX project using the MITAOE custom class.

### Option 1 — Overleaf (recommended)

1. Create a new project in Overleaf → Upload Project.
2. Zip this folder and upload.
3. Set `TY_Major_Report.tex` as the main document.
4. Compile with `pdflatex` + `bibtex` + `pdflatex` + `pdflatex`.

### Option 2 — Local MiKTeX / TeX Live

```
pdflatex TY_Major_Report.tex
bibtex   TY_Major_Report
pdflatex TY_Major_Report.tex
pdflatex TY_Major_Report.tex
```

The double-`pdflatex` at the end is required for the table of contents, list of figures, list of tables, and bibliography references to resolve correctly.

## Word count (approximate)

- Abstract: ~400 words
- Chapter 1: ~2,200 words
- Chapter 2: ~1,600 words
- Chapter 3: ~1,800 words
- Chapter 4: ~2,300 words
- Chapter 5: ~2,500 words
- Chapter 6: ~1,400 words

Total body text: approximately **12,000 words** across ~50-60 printed pages.
