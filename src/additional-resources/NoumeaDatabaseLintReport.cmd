@echo off
java -classpath ../../_schemacrawler/lib/*;lib/* schemacrawler.Main -server=hsqldb -database=schemacrawler -user=sa -password= -infolevel=standard -command dblint -infolevel=maximum -sorttables=false -outputformat NoumeaDatabaseLintReportTemplate.thymeleaf -o NoumeaDatabaseLintReport.html
