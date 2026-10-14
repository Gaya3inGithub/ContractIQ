# ContractIQ — AI Contract Obligation & Renewal Assistant

[![Java 21](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://www.oracle.com/java/)
[![Jakarta Servlet 6.0](https://img.shields.io/badge/Jakarta%20Servlet-6.0-blue.svg)](https://jakarta.ee/)
[![Apache Tomcat 10.1](https://img.shields.io/badge/Tomcat-10.1-yellow.svg)](https://tomcat.apache.org/)
[![MySQL 8+](https://img.shields.io/badge/MySQL-8.0%2B-blue.svg)](https://www.mysql.com/)
[![JUnit 5](https://img.shields.io/badge/JUnit-5.10-green.svg)](https://junit.org/junit5/)
[![Glassmorphism UI](https://img.shields.io/badge/UI-Glassmorphism%20Design-purple.svg)]()

ContractIQ is an enterprise-grade AI-powered web application that automates contract ingestion, PDF text extraction, obligation parsing, renewal deadline tracking, and mandatory human-in-the-loop review.

---

## 1. Mandatory Architecture & Technology Stack

The project strictly complies with all technical constraints:

*   **Backend:** Java 21 LTS
*   **Servlet Engine:** Jakarta Servlets 6.0 (Apache Tomcat 10.1 compatible)
*   **Persistence:** Direct JDBC with `PreparedStatement` and `HikariCP` connection pooling
*   **Database:** MySQL 8.0+
*   **Packaging:** Maven WAR packaging
*   **AI Integration:** Java 21 `java.net.http.HttpClient` with configurable LLM endpoints (OpenAI, Groq, Ollama, OpenRouter, Google Gemini OpenAI-compatible) and intelligent rule-based offline fallback
*   **PDF Extraction:** Apache PDFBox 3.0+
*   **JSON Serialization:** Jackson 2.17+ with JavaTimeModule
*   **Logging:** SLF4J with Logback rolling file and console appenders
*   **Testing:** JUnit 5 and Mockito 5 (deterministic offline tests)
*   **Frontend:** HTML5, CSS3 Glassmorphism System, Vanilla JavaScript (ES6+), Fetch API, Chart.js, Lucide Icons
*   **Strict Prohibitions Adhered to:** Zero Spring Framework, Zero Spring Boot, Zero Spring Data, Zero React, Zero Angular, Zero hardcoded credentials.

---

## 2. Layered Package Architecture

```
com.contractiq
├── config/
│   ├── AppConfig.java              # Application settings provider
│   ├── DatabaseConfig.java         # HikariCP JDBC connection pool manager
│   └── AppContextListener.java     # Servlet container lifecycle listener
├── controller/
│   ├── BaseServlet.java            # Standard JSON response & exception handler
│   ├── HealthServlet.java          # Diagnostics & system health endpoint
│   ├── DashboardServlet.java       # Real-data dashboard metrics endpoint
│   ├── ContractUploadServlet.java  # Multipart PDF ingestion & extraction trigger
│   ├── ContractServlet.java        # Contract repository and details endpoint
│   ├── ObligationServlet.java      # Obligation updates, human approval & rejection
│   ├── RenewalServlet.java         # Expiration & renewal notice deadline tracker
│   └── ActivityServlet.java        # Immutable audit history endpoint
├── service/
│   ├── ExtractionWorkflowService.java # End-to-end ingestion orchestrator
│   ├── ContractService.java        # Contract business logic
│   ├── ObligationService.java      # Mandatory human approval logic & contract sync
│   └── DashboardService.java       # Aggregated metrics & chart data service
├── dao/
│   ├── ContractDao.java            # Parameterized JDBC DAO for contracts
│   ├── ObligationDao.java          # Batch transactional DAO for obligations
│   └── ActivityDao.java            # Audit log DAO
├── model/
│   ├── Contract.java               # Contract entity
│   ├── Obligation.java             # Obligation entity with approval status
│   └── ActivityLog.java            # Audit record entity
├── dto/
│   ├── ApiResponse.java            # Uniform JSON response envelope
│   ├── DashboardStatsDto.java      # Metrics DTO for counter cards & Chart.js
│   ├── ContractExtractionDto.java  # Structured contract extraction schema
│   ├── ObligationExtractionDto.java# Obligation extraction schema
│   ├── ObligationUpdateRequest.java# Obligation editing DTO
│   └── ApprovalDecisionRequest.java# Reviewer decision & audit comment DTO
├── ai/
│   ├── LlmService.java             # Pluggable AI service interface
│   ├── OpenAiCompatibleLlmService.java # Real LLM integration via HttpClient
│   ├── RuleBasedFallbackExtractor.java # Heuristic offline/demo extraction engine
│   └── LlmExtractionResult.java    # AI execution metadata and audit timings
├── pdf/
│   ├── PdfValidationService.java   # Magic bytes (%PDF-), size & header validation
│   ├── PdfExtractionService.java   # Apache PDFBox full text & page mapper
│   └── ExtractedPdfContent.java    # PDF text and per-page content wrapper
├── util/
│   ├── DateUtil.java               # Strict ISO YYYY-MM-DD parsing & notice calculation
│   ├── SecurityUtil.java           # Path traversal prevention, SHA-256, HTML escaping
│   ├── JsonUtil.java               # Jackson object mapper helper
│   └── EnvLoader.java              # .env and system environment loader
└── exception/
    ├── ValidationException.java    # 400 Bad Request
    ├── ResourceNotFoundException.java # 404 Not Found
    ├── StorageException.java       # 500 Storage Failure
    └── LlmException.java           # External AI service failure
```

---

## 3. End-to-End Workflow & Mandatory Human Review Rule

1.  **Ingestion:** The user drops a PDF agreement onto the upload zone.
2.  **Validation:** `PdfValidationService` validates the `%PDF-` magic bytes header, `.pdf` extension, and ensures the file is under the 15MB limit.
3.  **Integrity:** A SHA-256 checksum is calculated and stored with the document.
4.  **Text Extraction:** Apache PDFBox extracts full contract text and builds a page-by-page mapping.
5.  **AI Analysis:** The contract text is sent to the configured LLM API (or offline fallback rule engine if no API key is provided).
6.  **MANDATORY HUMAN APPROVAL RULE:**
    *   AI extraction suggestions are **STRICTLY PENDING** by default.
    *   No obligation becomes approved automatically.
    *   Parent contract review status is set to `NEEDS_REVIEW`.
7.  **Persistence:** The contract and obligations are saved in MySQL via transactional JDBC.
8.  **Human Review:** In the UI, the reviewer can inspect the source clause excerpt, page number, AI confidence score, edit any field, and explicitly click **Approve** or **Reject**.
9.  **Decision Auditing:** Approvals and rejections record the reviewer's identity, timestamp, and audit comment in `activity_logs`.
10. **Reactive Dashboard:** Counter cards, Chart.js diagrams, renewal milestones, and activity feeds immediately reflect live database records.

---

## 4. Environment Variables Configuration

Copy `.env.example` to `.env` in the project root:

```bash
# Database Configuration (MySQL 8+)
DB_URL=jdbc:mysql://localhost:3306/contractiq_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8
DB_USER=root
DB_PASSWORD=your_password
DB_POOL_SIZE=10

# Storage
UPLOAD_DIR=E:/ContractIQ/uploads
MAX_FILE_SIZE_MB=15

# Real LLM API Configuration (OpenAI, Groq, Ollama, OpenRouter, Google Gemini)
# Leave blank to use ContractIQ's intelligent offline heuristic fallback engine
LLM_API_KEY=
LLM_API_URL=https://api.openai.com/v1/chat/completions
LLM_MODEL=gpt-4o-mini
LLM_TIMEOUT_SECONDS=60
DEMO_MODE=false
```

---

## 5. Local Setup and Build Instructions

### Prerequisites
*   Java JDK 21+ (`java -version` and `javac -version`)
*   Apache Maven 3.8+ (or use the included `mvn.cmd` wrapper)
*   MySQL 8.0+ running locally on port 3306
*   Apache Tomcat 10.1+

### Step 1: Initialize MySQL Database
Execute the database initialization script:
```powershell
Get-Content schema.sql -Raw | & mysql -u root -p
```
This creates the `contractiq_db` database, tables (`contracts`, `obligations`, `activity_logs`), indexes, foreign keys, and synthetic seed data.

### Step 2: Compile & Run Automated Tests
```powershell
.\mvn.cmd test
```
All 21 automated unit and integration tests run deterministically without requiring paid API credits.

### Step 3: Package WAR
```powershell
.\mvn.cmd package
```
Generates `target/ContractIQ.war` (~11MB) bundling all backend classes, libraries, and frontend assets.

### Step 4: Deploy to Apache Tomcat 10.1
Copy the generated WAR into your Tomcat `webapps` directory:
```powershell
Copy-Item target/ContractIQ.war "C:\path\to\apache-tomcat-10.1\webapps\ContractIQ.war"
```
Start Tomcat:
```powershell
& "C:\path\to\apache-tomcat-10.1\bin\catalina.bat" run
```

Access the application in your browser:
*   **Web Dashboard:** `http://localhost:8080/ContractIQ/`
*   **Health Diagnostics:** `http://localhost:8080/ContractIQ/api/health`

---

## 6. Docker Container Deployment

You can deploy the complete stack (Tomcat 10.1 + MySQL 8.0) using Docker Compose:

```bash
# Build and launch both containers
docker-compose up --build -d

# Check service health
docker-compose ps

# View application logs
docker-compose logs -f app
```

Once started, access `http://localhost:8080/` in your browser.

---

## 7. REST API Reference

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/health` | Diagnostics, JVM info, MySQL connection status, and LLM mode |
| `GET` | `/api/dashboard/stats` | Aggregated metrics, chart datasets, renewals, and activity |
| `POST`| `/api/contracts/upload`| Multipart upload of PDF contract with automatic AI extraction |
| `GET` | `/api/contracts` | List contracts with optional `?status=` and `?search=` filters |
| `GET` | `/api/contracts/{id}` | Complete contract details with associated obligations |
| `GET` | `/api/contracts/{id}/obligations` | Obligations list for a specific contract |
| `DELETE`| `/api/contracts/{id}` | Delete contract and cascade-delete obligations |
| `GET` | `/api/obligations` | List obligations with `?status=`, `?category=`, `?search=` |
| `GET` | `/api/obligations/{id}` | Get single obligation details |
| `PUT` | `/api/obligations/{id}` | Edit obligation title, description, party, deadline, category |
| `POST`| `/api/obligations/{id}/approve` | **Approve obligation** (records reviewer name & comment) |
| `POST`| `/api/obligations/{id}/reject` | **Reject obligation** (records reviewer name & comment) |
| `GET` | `/api/renewals` | Upcoming contract renewal milestones and notice deadlines |
| `GET` | `/api/activity` | Full immutable audit trail with `?limit=` and `?contractId=` |

---

## 8. Automated Test Suite Results

Test execution verified via `mvn test`:

```
-------------------------------------------------------
 T E S T S
-------------------------------------------------------
Running com.contractiq.test.DateUtilTest
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.517 s
Running com.contractiq.test.LlmExtractionParsingTest
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.497 s
Running com.contractiq.test.ObligationApprovalRulesTest
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.114 s
Running com.contractiq.test.PdfExtractionServiceTest
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.907 s
Running com.contractiq.test.PdfValidationServiceTest
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.192 s
Running com.contractiq.test.SecurityUtilTest
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.052 s

Results:
Tests run: 21, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## 9. Reviewer Testing & Synthetic Sample Document

To test the application immediately:
1. Open `http://localhost:8080/ContractIQ/` in your browser.
2. Click **"Upload Contract"** in the top navigation or dashboard hero.
3. Drag and drop the included synthetic contract:
   `sample/synthetic_enterprise_saas_agreement.pdf`
4. Watch the progress pipeline extract the text, analyze clauses, and create obligations.
5. In the **Obligations Review** screen:
   *   Note that all extracted obligations start as **PENDING** (human approval enforcement).
   *   Click **Approve** or **Reject** on an obligation.
   *   Click **Edit** to modify terms before approval.
6. Check the **Dashboard** and **Activity Log** to verify real-time MySQL persistence.
