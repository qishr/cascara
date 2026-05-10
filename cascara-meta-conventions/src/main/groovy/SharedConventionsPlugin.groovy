// package io.github.qishr.cascara.meta

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.ZoneOffset
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Jar
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.testing.jacoco.tasks.JacocoReport

class SharedConventionsPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

        project.pluginManager.apply 'jacoco'
        project.pluginManager.apply 'signing'
        project.pluginManager.apply 'maven-publish'

        project.tasks.withType(JavaCompile).configureEach { compile ->
            compile.options.compilerArgs.addAll(["--module-version", "${project.version}"])
        }

        // --- runtimeModulePath task ---
        project.tasks.register('runtimeModulePath', Copy) { t ->
            t.dependsOn project.tasks.named('jar')
            t.from project.tasks.named('jar')
            t.from project.configurations.runtimeClasspath
            t.duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.EXCLUDE
            t.into project.layout.buildDirectory.dir('modulepath')
        }

        project.tasks.named('jar') {
            it.finalizedBy 'runtimeModulePath'
        }

        // --- Test configuration ---
        project.tasks.withType(org.gradle.api.tasks.testing.Test).configureEach { test ->
            test.dependsOn 'runtimeModulePath'
            test.modularity.inferModulePath = true
            test.useJUnitPlatform()

            test.testLogging {
                events TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED
                showStandardStreams = true
                exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
                showExceptions = true
                showCauses = true
                showStackTraces = true
            }

            test.testLogging {
                showStandardStreams = true
                events TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED
            }
            test.finalizedBy 'jacocoTestReport'
        }

        // --- Jacoco ---
        project.extensions.configure(org.gradle.testing.jacoco.plugins.JacocoPluginExtension) {
            it.toolVersion = '0.8.13'
        }

        project.tasks.withType(JacocoReport).configureEach { report ->
            report.dependsOn 'test'
            report.reports {
                xml.required = true
                html.required = true
            }
        }

        // --- Jar manifest ---
        project.tasks.withType(Jar).configureEach { jar ->
            def isoDate = Instant.now()
                .atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_INSTANT)

            jar.manifest.attributes(
                'Implementation-Title': project.providers.gradleProperty('maven_name'),
                'Implementation-Version': project.version,
                'Implementation-Vendor': project.group,
                'Build-Date': isoDate
            )
        }
    }
}
