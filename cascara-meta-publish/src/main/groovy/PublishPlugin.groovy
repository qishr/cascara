// package io.github.qishr.cascara.meta

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication

class PublishPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

        project.pluginManager.apply("maven-publish")
        project.pluginManager.apply("signing")

        project.afterEvaluate {
            if (project.plugins.hasPlugin("maven-publish")) {
                project.publishing {
                    publications { pubs ->

                        project.plugins.withId("java-gradle-plugin") {
                            project.publishing.publications.withType(MavenPublication).configureEach { pub ->
                                configurePom(pub, project)
                            }
                        }


                        project.plugins.withId("java-library") {
                            project.publishing.publications.create("mavenJava", MavenPublication) { pub ->
                                pub.from(project.components.getByName("java"))
                                configurePom(pub, project)
                            }
                        }


                        project.plugins.withId("java-platform") {
                            if (!publishing.publications.names.contains("mavenBom")) {
                                project.publishing.publications.create("mavenBom", MavenPublication) { pub ->
                                    pub.from(project.components.getByName("javaPlatform"))
                                    configurePom(pub, project)
                                }
                            }
                        }

                    }
                }

                project.signing {
                    sign project.publishing.publications
                }

                // // Ensure each publish task depends on its matching sign task
                // project.publishing.publications.all { publication ->
                //     def pubName = publication.name.capitalize()

                //     def signTask = project.tasks.named("sign${pubName}Publication")

                //     project.tasks.matching { it.name == "publish${pubName}PublicationToMavenLocal" }.configureEach {
                //         dependsOn(signTask)
                //     }

                //     project.tasks.matching { it.name == "publish${pubName}PublicationToMavenRepository" }.configureEach {
                //         dependsOn(signTask)
                //     }
                // }

            }
        }
    }

    private static void configurePom(MavenPublication pub, Project project) {
        pub.pom {
            withXml {
                def deps = asNode().dependencies?.dependency
                if (deps) {
                    deps.each { dep ->
                        if (!dep.version) {
                            def group = dep.groupId.text()
                            def name  = dep.artifactId.text()
                            def v = project.configurations.runtimeClasspath
                                .resolvedConfiguration
                                .firstLevelModuleDependencies
                                .find { it.moduleGroup == group && it.moduleName == name }
                                ?.moduleVersion

                            if (v) {
                                dep.appendNode('version', v)
                            }
                        }
                    }
                }
            }

            name = project.title
            description = project.description
            url = project.url

            licenses {
                license {
                    if (project.hasProperty("overrideLicense") &&
                        project.property("overrideLicense") == "MIT") {

                        name = "MIT License"
                        url = "https://opensource.org/licenses/MIT"

                    } else {
                        name = "Apache License 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0"
                    }
                }
            }

            scm {
                url = project.scm_url
                connection = project.scm_conn
                developerConnection = project.scm_dev_conn
            }

            developers {
                developer {
                    id = "qishr"
                    name = "Qishr"
                }
            }
        }
    }
}
