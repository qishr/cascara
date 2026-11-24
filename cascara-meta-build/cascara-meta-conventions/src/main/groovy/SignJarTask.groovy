import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations

import javax.inject.Inject
import java.io.ByteArrayOutputStream

abstract class SignJarTask extends DefaultTask {

    @InputFile
    abstract RegularFileProperty getArchiveFile()

    @Input
    @Optional
    abstract Property<String> getKeystore()

    @Input
    @Optional
    abstract Property<String> getStorePass()

    @Input
    @Optional
    abstract Property<String> getKeyPass()

    @Input
    abstract Property<String> getAlias()

    @Inject
    abstract ExecOperations getExecOperations()

    @TaskAction
    void sign() {
        if (!getKeystore().isPresent() || !getStorePass().isPresent() || !getKeyPass().isPresent()) {
            logger.lifecycle("Skipping signing for ${getName()}: missing keystore credentials.")
            return
        }

        def stdout = new ByteArrayOutputStream()
        def stderr = new ByteArrayOutputStream()

        def result = getExecOperations().exec { spec ->
            spec.commandLine(
                'jarsigner',
                '-keystore', getKeystore().get(),
                '-storepass', getStorePass().get(),
                '-keypass', getKeyPass().get(),
                '-tsa', 'http://timestamp.digicert.com',
                getArchiveFile().get().asFile.absolutePath,
                getAlias().get()
            )
            spec.standardOutput = stdout
            spec.errorOutput = stderr
            spec.ignoreExitValue = true
        }

        if (result.exitValue != 0) {
            logger.error(stdout.toString())
            logger.error(stderr.toString())
            throw new GradleException("jarsigner failed with exit code ${result.exitValue}")
        }
    }
}