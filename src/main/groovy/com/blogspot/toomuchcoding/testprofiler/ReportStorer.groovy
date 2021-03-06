package com.blogspot.toomuchcoding.testprofiler

import groovy.transform.PackageScope
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.gradle.api.Project

@PackageScope
@CompileStatic
@Slf4j
class ReportStorer {

    protected static final Closure<String> DEFAULT_ROW_FROM_REPORT_CONVERTER = { TestProfilerPluginExtension testProfilerPluginExtension , ReportRow reportRow ->
        "${reportRow.module}${testProfilerPluginExtension.separator}${reportRow.testExecutionResult.testClassName}${testProfilerPluginExtension.separator}${reportRow.testExecutionResult.testName}${testProfilerPluginExtension.separator}${reportRow.testExecutionResult.testExecutionTime}${testProfilerPluginExtension.separator}${reportRow.testClassExecutionTime}".toString()
    }

    private final TestProfilerPluginExtension testProfilerPluginExtension
    private final Project project
    private final File reportDir
    private final File mergedTestProfilingSummaryDir

    ReportStorer(TestProfilerPluginExtension testProfilerPluginExtension, Project project, File reportDir, File mergedTestProfilingSummaryDir) {
        this.testProfilerPluginExtension = testProfilerPluginExtension
        this.project = project
        this.reportDir = reportDir
        this.mergedTestProfilingSummaryDir = mergedTestProfilingSummaryDir
    }

    ReportStorer(TestProfilerPluginExtension testProfilerPluginExtension) {
        this.testProfilerPluginExtension = testProfilerPluginExtension
    }

    public void storeReport(Set<TestExecutionResult> testExecutionResults) {
        log.debug("All test execution results $testExecutionResults")
        File report = createNewReportFile()
        addHeadersToFile(report)
        Map<String, Double> classExecutionTime = calculateClassExecutionTime(testExecutionResults)
        log.debug("Calculated class execution time $classExecutionTime")
        String testExecutionResult = buildTestExecutionResult(classExecutionTime, testExecutionResults)
        log.debug("Test execution result [$testExecutionResult]")
        appendTestExecutionResultToFile(report, testExecutionResult)
        println "Your tests report is ready at [$report.absolutePath]"
        appendTestExecutionResultToMergedTestSummary(testExecutionResult)
    }

    private void appendTestExecutionResultToMergedTestSummary(String testExecutionResult) {
        mergedTestProfilingSummaryDir.mkdirs()
        File mergedTestProfilingSummary = new File(mergedTestProfilingSummaryDir, testProfilerPluginExtension.mergedSummaryFileName)
        mergedTestProfilingSummary << testExecutionResult << '\n'
        log.debug("Stored [$testExecutionResult] in [$mergedTestProfilingSummary]")
    }

    private File appendTestExecutionResultToFile(File report, String testExecutionResult) {
        return report << testExecutionResult
    }

    private File addHeadersToFile(File report) {
        return report << testProfilerPluginExtension.outputReportHeaders
    }

    private File createNewReportFile() {
        File reportDir = new File(project.buildDir.absolutePath, testProfilerPluginExtension.reportOutputDir)
        reportDir.mkdirs()
        File report = new File(reportDir, testProfilerPluginExtension.reportOutputCsvFilename)
        report.delete()
        return report
    }

    private String buildTestExecutionResult(Map<String, Double> classExecutionTime, Set<TestExecutionResult> testExecutionResults) {
        return testExecutionResults.collect {
            new ReportRow(project.path, it, classExecutionTime[it.testClassName])
        }.sort(testProfilerPluginExtension.comparator)
        .collect(rowFromReport()).join('\n')
    }

    private Map<String, Double> calculateClassExecutionTime(Set<TestExecutionResult> testExecutionResults) {
        return testExecutionResults.groupBy {
            TestExecutionResult testExecutionResult -> testExecutionResult.testClassName
        }.collectEntries {
            [it.key, (it.value.sum { TestExecutionResult testExecutionResult -> testExecutionResult.testExecutionTime } as Double).round(3)]
        } as Map<String, Double>
    }

    Closure<String> rowFromReport() {
        return testProfilerPluginExtension.rowFromReport.curry(testProfilerPluginExtension)
    }
}
