package org.notlocalhost.gradle;

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.Exec

class CalabashTestPlugin implements Plugin<Project> {
    private static final String TEST_TASK_NAME = 'calabash'

    private Project project

    void apply(Project project) {
        this.project = project;
        project.extensions.create("calabashTest", CalabashTestPluginExtension)

        def hasAppPlugin = project.plugins.hasPlugin AppPlugin
        def hasLibraryPlugin = project.plugins.hasPlugin LibraryPlugin

        // Ensure the Android plugin has been added in app or library form, but not both.
        if (!hasAppPlugin && !hasLibraryPlugin) {
            throw new IllegalStateException("The 'android' or 'android-library' plugin is required.")
        } else if (hasAppPlugin && hasLibraryPlugin) {
            throw new IllegalStateException(
                    "Having both 'android' and 'android-library' plugin is not supported.")
        }

        project.afterEvaluate {

            def variants = hasAppPlugin ? project.android.applicationVariants :
                    project.android.libraryVariants

            variants.all { variant ->

                def apkFile = variant.outputs.outputFile.first().absolutePath

                project.logger.debug "==========================="
                project.logger.debug "$apkFile"
                project.logger.debug "${project.getPath()}"
                project.logger.debug "==========================="

                def variationName = variant.name.capitalize()

                //def outFile = new File(project.file("${project.buildDir}/reports/calabash/${variationName}"), "report.html")
                def outFileDir = project.file("${project.buildDir}/reports/calabash/${variationName}")
                def format = project.calabashTest.format ?: "html"
                def outFile = new File(outFileDir, "report.${getOutputFormat()}")

                def taskRunName = "$TEST_TASK_NAME$variationName"
                def testRunTask = project.tasks.create(taskRunName, Exec)
                testRunTask.dependsOn project["assemble${variationName}"]
                testRunTask.description = "Run Calabash Tests for '$variationName'."
                testRunTask.group = JavaBasePlugin.VERIFICATION_GROUP

                testRunTask.workingDir "${project.rootDir}/"
                def os = System.getProperty("os.name").toLowerCase()

                Iterable commandArguments = constructCommandLineArguments(project, apkFile, outFile)

                if (!os.contains("windows")) { // assume Linux
                    testRunTask.environment("SCREENSHOT_PATH", "${outFileDir}/")
                }

                testRunTask.commandLine commandArguments

                testRunTask.doFirst {
                    if(!outFileDir.exists()) {
                        project.logger.debug "Making dir path $outFileDir.canonicalPath"
                        if(!outFileDir.mkdirs()) {
                            throw new IllegalStateException("Could not create reporting directories")
                        }
                    }
                }
                
                testRunTask.doLast {
                    println "\r\nCalabash Report: file://$outFile.canonicalPath"
                }
            }
        }
    }

    String getOutputFormat() {
        return project.calabashTest.format ?: "html"
    }

    Iterable constructCommandLineArguments(Project project, String apkFile, File outFile) {
        def os = System.getProperty("os.name").toLowerCase()

        java.util.ArrayList<String> commandArguments = new ArrayList<String>()

        if (os.contains("windows")) {
            // you start commands in Windows by kicking off a cmd shell
            commandArguments.add("cmd")
            commandArguments.add("/c")
        }

        commandArguments.add("calabash-android")
        commandArguments.add("run")
        commandArguments.add(apkFile)

        def calabash = project.calabashTest

        String featuresPath = calabash.featuresPath
        if (featuresPath != null) {
            commandArguments.add(featuresPath)
        }

        if (calabash.profile != null) {
            commandArguments.add("--profile")
            commandArguments.add(calabash.profile)
        }

        commandArguments.add("--format")
        commandArguments.add(getOutputFormat())

        commandArguments.add("--out")
        commandArguments.add(outFile.canonicalPath)

        if (calabash.showProgress) {
            commandArguments.add("--format progress")
        }

        if (calabash.verbose) {
            commandArguments.add("-v")
        }

        calabash.tags?.each() {
            commandArguments.add("--tags ${it}")
        }

        return commandArguments;
    }
}
