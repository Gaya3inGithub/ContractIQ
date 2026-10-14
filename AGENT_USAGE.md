# ContractIQ — Agent Usage Documentation

## 1. Coding Agents and Tools Used

*   **Primary Agent:** Antigravity (Powered by Google DeepMind Gemini 3.8 Flash High)
*   **Agent Environment:** Windows 11 Shell (PowerShell), Java 21 LTS (`jdk-21.0.3`), Apache Maven 3.9.16, MySQL 8.0.39 Community Server, Apache Tomcat 10.1.31.
*   **Agent Tools Employed:**
    *   `run_command` (PowerShell command execution for audit, environment testing, Maven builds, database script execution, and live REST API verification)
    *   `write_to_file` & `replace_file_content` (Creation and refinement of Java classes, DAOs, Servlets, HTML/CSS/JS frontend files, SQL schemas, and configuration scripts)
    *   `view_file` (Inspection of compilation logs and file sections for debugging)
    *   `manage_task` (Management of background build tasks and Tomcat runtime daemon)

---

## 2. Representative Prompts & Invocations

1.  **Environment Audit:**
    > "Act as a senior Java full-stack engineer... Inspect the existing ContractIQ workspace and development environment first. Check OS, Java 21, Maven, Tomcat, MySQL. Create a realistic implementation plan, implement end-to-end, run tests, fix errors, and prepare deployment."
2.  **Implementation Architecture:**
    > "Use Jakarta Servlets 6.0, JDBC with PreparedStatement, MySQL 8+, Apache Tomcat 10.1, Maven WAR packaging, Java HttpClient for real LLM API requests, Apache PDFBox for PDF text extraction, Jackson for JSON processing, JUnit 5 for tests. Strictly prohibited: Spring Framework, React, Angular."
3.  **Mandatory Human Approval Workflow:**
    > "AI-generated information must never become approved merely because extraction succeeded. Approval must be enforced by the backend, not only by hiding or disabling a frontend button."

---

## 3. Delegated Work Breakdown

*   **Environment & Tooling Audit:** Detected Java 21 (`C:\Program Files\Java\jdk-21`), existing running `MySQL80` service with credentials (`root` / `accord`), VS Code's bundled Apache Maven (`oracle.oracle-java-27.0.0`), and portable Apache Tomcat 10.1.31 in Downloads. Created local `mvn.cmd` wrapper script to unify build execution.
*   **Database Schema & JDBC Persistence:** Designed 3 normalized tables (`contracts`, `obligations`, `activity_logs`) with foreign keys, ON DELETE CASCADE/SET NULL, timestamps, and indexes. Implemented thread-safe `DatabaseConfig` with `HikariCP` connection pool.
*   **PDF Ingestion & Text Extraction:** Engineered `PdfValidationService` checking `%PDF-` magic bytes, size limits, and filename sanitization against path traversal. Integrated Apache PDFBox 3.0.2 to extract text and per-page mappings.
*   **Configurable LLM Service:** Built `OpenAiCompatibleLlmService` with Java 21 `HttpClient`, structured JSON parsing, timeouts, retries, and fallback to `RuleBasedFallbackExtractor` when an API key is absent.
*   **Mandatory Human-in-the-Loop Review:** Implemented `ObligationService` enforcing that all AI extractions enter as `PENDING`. Decisions (`APPROVE` / `REJECT`) require explicit calls and update audit trails.
*   **Glassmorphism UI:** Built responsive dark/light dashboard in HTML5/CSS3/Vanilla JS featuring Lucide icons, Chart.js diagrams, drag-and-drop dropzone, and review modals.

---

## 4. Mistakes Caught and Resolved During Development

1.  **Constructor Parameter Arity in ActivityLog:**
    *   *Issue:* `ExtractionWorkflowService` called `new ActivityLog(contractId, actionType, description, actor, ipAddress)` with 5 arguments, while `ActivityLog` initially had only a 4-argument constructor.
    *   *Resolution:* Added the overloaded 5-argument constructor `ActivityLog(Long, String, String, String, String)` to support IP tracking.
2.  **Regex Sentence Boundary vs Corporate Suffixes:**
    *   *Issue:* In `RuleBasedFallbackExtractor`, corporate party names with periods like `"CloudScale Inc."` and `"Horizon Global LLC."` were either truncated or consumed trailing sentence words.
    *   *Resolution:* Refined regex pattern `(?i)(?:by and between|between)\s+([A-Z][A-Za-z0-9&.\s]{2,50}?)\s+(?:and|with)\s+([A-Z][A-Za-z0-9&.\s]{2,50}?)(?:[.\n\r]|\s*\(|\s*,\s*an?|$)` to cleanly separate parties while preserving legal suffixes.
3.  **Hyphen in Expiration Date Regex:**
    *   *Issue:* Character class `[A-Za-z0-9,\s]` in `extractExpirationDate` did not include the hyphen `-`, causing ISO format dates (`2026-03-01`) to return `null`.
    *   *Resolution:* Updated regex to include `\-`, properly extracting ISO dates.
4.  **PowerShell Redirection Operator:**
    *   *Issue:* Direct PowerShell command `mysql < schema.sql` failed because `<` is reserved in PowerShell.
    *   *Resolution:* Used `Get-Content schema.sql -Raw | mysql` to pipe SQL commands into the MySQL engine.

---

## 5. Verification and Real Testing Performed

*   **Automated Tests:**
    *   `PdfValidationServiceTest` (4 tests: valid PDF, empty file, non-PDF extension, corrupted magic bytes header)
    *   `PdfExtractionServiceTest` (1 test: PDF text extraction, page mapping, character counts)
    *   `DateUtilTest` (5 tests: ISO parsing, alternative formats, null-on-ambiguous dates, notice deadline calculations)
    *   `SecurityUtilTest` (5 tests: path traversal sanitization, Windows paths, SHA-256 hashing, HTML escaping, API key redaction)
    *   `LlmExtractionParsingTest` (2 tests: JSON schema deserialization, heuristic rule extraction)
    *   `ObligationApprovalRulesTest` (4 tests: PENDING initialization, human approval enforcement, rejection enforcement, field editing)
    *   **Result:** All 21 tests pass with 0 failures (`mvn test`).
*   **Live Deployment & Server Verification:**
    *   `ContractIQ.war` packaged successfully via Maven (`mvn package`).
    *   Deployed to Apache Tomcat 10.1.31 and verified running on port 8080.
    *   Live REST API checks executed:
        *   `GET /api/health` -> HTTP 200 `UP`, Database: `CONNECTED`.
        *   `GET /api/dashboard/stats` -> HTTP 200 with real database counts.
        *   `POST /api/contracts/upload` -> Successfully uploaded `synthetic_enterprise_saas_agreement.pdf`, extracted 3 obligations in `PENDING` state.
        *   `POST /api/obligations/{id}/approve` -> Approved obligation, recorded reviewer decision, verified real-time update in dashboard stats.
