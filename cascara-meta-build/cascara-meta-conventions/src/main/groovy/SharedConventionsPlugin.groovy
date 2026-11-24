import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.ZoneOffset
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.bundling.Jar
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.process.CommandLineArgumentProvider
import org.gradle.testing.jacoco.tasks.JacocoReport

class SharedConventionsPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

        project.pluginManager.apply 'jacoco'
        project.pluginManager.apply 'signing'
        project.pluginManager.apply 'maven-publish'

        // --- Java configuration ---
        project.extensions.configure(org.gradle.api.plugins.JavaPluginExtension) { java ->
            java.withJavadocJar()
            java.withSourcesJar()
            java.toolchain.languageVersion = JavaLanguageVersion.of(21)
        }

        project.base {
            archivesName =
                project.findProperty('maven_name') ?: project.name
        }

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

        def signing = project.extensions.create('cascaraSigning', CascaraSigningExtension)

        signing.keystore = project.findProperty('cascaraSigningKeystore')
        signing.storePass = project.findProperty('cascaraSigningStorePass')
        signing.keyPass = project.findProperty('cascaraSigningKeyPass')
        signing.alias = 'signing'

        def jarTask = project.tasks.named('jar', org.gradle.api.tasks.bundling.Jar)

        project.tasks.register('signJar', SignJarTask) { sign ->
            // Property binding automatically wires task dependencies
            sign.archiveFile.set(jarTask.flatMap { it.archiveFile })

            sign.keystore.set(project.providers.provider { signing.keystore })
            sign.storePass.set(project.providers.provider { signing.storePass })
            sign.keyPass.set(project.providers.provider { signing.keyPass })
            sign.alias.set(signing.alias)
        }

        // TODO: Get this working...
        // project.tasks.register('verifyJarSignature', Exec) {
        //     dependsOn 'signJar'

        //     commandLine(
        //         'jarsigner',
        //         '-keystore', signing.keystore,
        //         '-storepass', signing.storePass,
        //         '-verify',
        //         '-verbose',
        //         jarTask.archiveFile.get().asFile.absolutePath
        //     )
        // }

        project.tasks.named('jar') {
            it.finalizedBy 'signJar'
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

            def versionParts = project.cascara_version.toString().tokenize('.')
            def major = versionParts[0]
            def minor = versionParts[1]
            def defaultMinVersion = "${major}.${minor}.0"

            jar.manifest.attributes(
                'Implementation-Version': project.version,
                'Implementation-Vendor': project.group,
                'Implementation-Title': project.maven_name,
                'Module-Name': project.java_name,
                'Cascara-Version': project.cascara_version,
                'Min-Cascara-Version': project.findProperty('min_cascara_version') ?: defaultMinVersion,
                'Build-Date': isoDate
            )
        }
    }
}
