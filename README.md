# ContractIQ — AI Contract Obligation & Renewal Assistant

[![Java 21](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://www.oracle.com/java/)
[![Jakarta Servlet 6.0](https://img.shields.io/badge/Jakarta%20Servlet-6.0-blue.svg)](https://jakarta.ee/)
[![Apache Tomcat 10.1](https://img.shields.io/badge/Tomcat-10.1-yellow.svg)](https://tomcat.apache.org/)
[![MySQL 8+](https://img.shields.io/badge/MySQL-8.0%2B-blue.svg)](https://www.mysql.com/)
[![JUnit 5](https://img.shields.io/badge/JUnit-5.10-green.svg)](https://junit.org/junit5/)
[![Glassmorphism UI](https://img.shields.io/badge/UI-Glassmorphism%20Design-purple.svg)]()

ContractIQ is an enterprise-grade AI-powered web application that automates contract ingestion, PDF text extraction, obligation parsing, renewal deadline tracking, and mandatory human-in-the-loop review.

## Completed scope

- Verified Java 21 + Maven 3 build pipeline and a packaging-ready WAR
- Verified local MySQL 8 connectivity and a synthetic PDF workflow through JDBC persistence
- Verified mandatory approval and rejection persistence on a synthetic contract
- Verified mandatory human approval enforcement for AI-extracted obligations
- Preserved fallback/offline behavior when no LLM key is configured

## Excluded or external scope

- Public hosting is intentionally left for a provider environment with valid credentials and HTTPS capability.
- Real LLM usage requires a valid API key and provider endpoint; otherwise the app remains in offline heuristic mode.
- Production deployment credentials are not stored in the repository and must be supplied through environment variables or a host secret store.

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
*   Apache Tomcat 10.1+ (required for the live servlet application; it is not bundled with this repository)

### Step 1: Initialize MySQL Database
Execute the database initialization script:
```powershell
Get-Content schema.sql -Raw | & mysql -u root -p
```
This creates the `contractiq_db` database, tables (`contracts`, `obligations`, `activity_logs`), indexes, and foreign keys. It does not drop tables or insert seed data.

### Step 2: Compile & Run Automated Tests
```powershell
.\mvn.cmd test
```
All automated tests run deterministically without requiring paid API credits.

### Step 3: Package WAR
```powershell
.\mvn.cmd package
```
Generates `target/ContractIQ.war` (~11MB) bundling all backend classes, libraries, and frontend assets.

### Step 4: Deploy to Apache Tomcat 10.1
Before launching Tomcat, set `DB_URL`, `DB_USER`, `DB_PASSWORD`, `UPLOAD_DIR`, and the other applicable values from `.env` as environment variables for the Tomcat process (or configure them in the Tomcat service). A Tomcat installation launched from its own `bin` directory does not automatically read this repository's `.env` file.

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

## 7. Vercel Container Deployment (Optional)

`Dockerfile.vercel` preserves the existing Java 21/Tomcat architecture and listens on Vercel's supplied `$PORT`. Vercel supports HTTP servers packaged in a `Dockerfile.vercel`, so this is technically viable for a small personal demonstration; it has **not** been deployed from this repository.

Before deploying, provide an external MySQL 8+ database and set these Vercel environment variables (never commit their values):

```text
DB_URL=jdbc:mysql://<host>:3306/<database>?useSSL=true&requireSSL=true&serverTimezone=UTC&characterEncoding=UTF-8
DB_USER=<least-privilege-application-user>
DB_PASSWORD=<secret>
DB_POOL_SIZE=3
# Vercel's 4.5 MB request-body limit includes multipart overhead.
MAX_FILE_SIZE_MB=4
APP_ENV=production
APP_BASIC_AUTH_USERNAME=<reviewer-login-name>
APP_BASIC_AUTH_PASSWORD=<secret>
LLM_API_KEY=
LLM_API_URL=https://api.openai.com/v1/chat/completions
LLM_MODEL=gpt-4o-mini
LLM_TIMEOUT_SECONDS=60
DEMO_MODE=false
```

Important deployment constraints:

* Do not run MySQL in the Vercel container. The database must be external, use TLS, and allow the deployment's outbound connectivity.
* The current application stores PDFs on its local file system. `Dockerfile.vercel` deliberately uses `/tmp/contractiq-uploads`, so source PDFs are lost when an instance restarts or scales. Persistent uploaded-document retrieval requires an object-storage integration, which is not implemented.
* PDF extraction is synchronous. Keep demo PDFs small and text-based. Vercel's current container/function duration depends on the plan and configuration, so test the real upload path on the selected plan before sharing a deployment.
* Vercel rejects request bodies larger than 4.5 MB before Tomcat receives them. `Dockerfile.vercel` therefore defaults the application upload limit to 4 MB; do not advertise the local 15 MB limit for this deployment.
* Vercel Hobby is for personal, non-commercial use and has finite included compute. Treat it as a demo tier, not permanent hosting. Review the current [Docker guidance](https://vercel.com/kb/docker), [Hobby limits](https://vercel.com/docs/plans/hobby), and [platform limits](https://vercel.com/docs/limits) before deployment.
* In production, ContractIQ now fails closed until `APP_BASIC_AUTH_USERNAME` and `APP_BASIC_AUTH_PASSWORD` are configured. The built-in HTTP Basic authentication protects every route, including contract upload and reviewer actions. Do not place either value in source control. Vercel Deployment Protection is an optional additional layer.

To deploy, import the intended Git repository into Vercel, confirm it selects `Dockerfile.vercel`, set the variables above in the Vercel dashboard, then deploy. Test the returned URL's `/api/health`, upload, review, and persistence behavior before sharing it.

---

## 8. REST API Reference

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

## 9. Automated Test Suite Results

Most recent verification command:

```powershell
.\mvn.cmd -B clean test package
```

```
-------------------------------------------------------
 T E S T S
-------------------------------------------------------
Results:
Tests run: 27, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## 10. Reviewer Testing & Synthetic Sample Document

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

### Known limitations

* Image-only or scanned PDFs are rejected because OCR is not implemented.
* With no LLM key, the UI/API correctly reports heuristic/offline extraction; it does not claim a real AI provider was used.
* Email reminders and scheduled notification delivery are not implemented.
* Local Tomcat must be installed and started before browser/API acceptance testing can be performed.
* Vercel containers are stateless. Without an object-storage integration, uploaded source PDFs are not durable after an instance restart or scale-out.
