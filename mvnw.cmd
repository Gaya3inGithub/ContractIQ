@echo off
set "JAVA_HOME=C:\Program Files\Java\jdk-21"
set "MAVEN_HOME=C:\Users\gayat\.vscode\extensions\oracle.oracle-java-27.0.0\nbcode\java\maven"
set "PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%"
"%MAVEN_HOME%\bin\mvn.cmd" %*
