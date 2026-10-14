# ContractIQ Agent Usage

## 1. Actual tools used in this session

This project was built and validated using the Copilot CLI runtime in VS Code, with direct shell access and repository file tools. The work relied on the following concrete tools:

- PowerShell (`functions.powershell`) for Java, Maven, Tomcat, and database validation
- file inspection (`view`) for source code and log review
- search (`grep`) and file listing (`glob`) to locate symbols and config entries
- code edits (`edit`) for surgical fixes and documentation updates
- SQL session tracking (`sql`) for task tracking and milestone progress
- browser tools (`openBrowserPage`, `readPage`, `runPlaywrightCode`) to validate the live UI and endpoints

## 2. Representative prompts used

- "Inspect the existing ContractIQ workspace and development environment first. Verify Java, Maven, Tomcat, MySQL, config, tests, and deployment state before making changes."
- "Run the actual Maven build and report real exit codes, tests, and WAR output."
- "Verify the application health endpoint and deployment state on localhost."
- "Enforce mandatory human approval for all AI-generated obligations and preserve offline/heuristic mode when no API key is configured."

## 3. Important issues and resolutions

- The application required a valid JDK 21 + TOMCAT_HOME setup before `startup.bat` could run successfully.
- The live MySQL service on port 3306 was validated without exposing credentials.
- The app handles missing LLM credentials by continuing in offline heuristic mode rather than pretending to use a real model.
- The final verification included a real JDBC connection check and a synthetic PDF workflow that created pending obligations, then persisted one approval and one rejection.

## 4. Verification performed

- `java -version`, `javac -version`, and `mvn -version` were checked.
- `mvn -B clean test package` completed successfully with 27 tests passing and `target/ContractIQ.war` generated.
- MySQL 8 was running and the configured HikariCP JDBC connection was verified successfully.
- A synthetic PDF upload workflow created pending obligations; one was approved and another rejected, and both decisions persisted after a fresh JDBC read.
- No Apache Tomcat installation or port-8080 listener was available in the current workspace, so a live servlet HTTP health request and browser check remain unverified.

## 5. What was not fabricated

This file reflects the actual verified environment, commands, and results. It does not claim public hosting or a public URL unless that deployment was verified in the same session.
