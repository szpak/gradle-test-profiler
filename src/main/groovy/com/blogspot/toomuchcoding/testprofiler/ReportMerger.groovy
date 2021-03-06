package com.blogspot.toomuchcoding.testprofiler
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import groovy.util.logging.Slf4j
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction

@PackageScope
@CompileStatic
@Slf4j
class ReportMerger extends DefaultTask {

    TestProfilerPluginExtension testProfilerPluginExtension
    @InputDirectory File mergedTestProfilingSummaryDir

    @TaskAction
    void testsProfileSummaryReport() {
        File mergedTestProfilingSummary = new File(getMergedTestProfilingSummaryDir(), getTestProfilerPluginExtension().mergedSummaryFileName)
        if(!mergedTestProfilingSummary.exists()) {
            log.debug("No test data has been stored - skipping profile summary report creation")
            return
        }
        log.debug("Will store merged test profiling summary in [${mergedTestProfilingSummary}]")
        String fileContent = mergedTestProfilingSummary.text
        log.trace("Saving file [$mergedTestProfilingSummary] content [$fileContent]")
        mergedTestProfilingSummary.text = getTestProfilerPluginExtension().outputReportHeaders
        Set<ReportRow> reportRows = new TreeSet<ReportRow>(getTestProfilerPluginExtension().comparator as Comparator<ReportRow>)
        appendReportRow(fileContent, reportRows)
        mergedTestProfilingSummary << reportRows.collect(rowFromReport()).join('\n')
        println "Your combined report is available here [$mergedTestProfilingSummary]"
    }

    private void appendReportRow(String fileContent, Set<ReportRow> reportRows) {
        fileContent.split('\n')
                .each { String string ->
            String[] row = string.split(getTestProfilerPluginExtension().separator)
            log.debug("Converting row $row")
            try {
                reportRows << new ReportRow(row[0], new TestExecutionResult(row[1], row[2], row[3] as Double), row[4] as Double)
            } catch (NumberFormatException e) {
                log.warn("Exception occurred while trying to parse a report row", e)
            }
        }
    }

    Closure<String> rowFromReport() {
        return getTestProfilerPluginExtension().rowFromReport.curry(getTestProfilerPluginExtension())
    }
}
