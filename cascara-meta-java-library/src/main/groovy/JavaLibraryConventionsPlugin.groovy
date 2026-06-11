import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.toolchain.JavaLanguageVersion

class JavaLibraryConventionsPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.pluginManager.apply("java-library")
        project.pluginManager.apply("io.github.qishr.cascara-meta.shared-conventions")

        // --- Java configuration ---
        project.extensions.configure(org.gradle.api.plugins.JavaPluginExtension) { java ->
            java.withJavadocJar()
            java.withSourcesJar()
            java.toolchain.languageVersion = JavaLanguageVersion.of(25)
        }

    }
}
