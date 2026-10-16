import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication

class PublishPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.pluginManager.apply("maven-publish")
        project.pluginManager.apply("signing")

        // Extract configuration-phase properties as plain strings to avoid capturing 'project'
        def projName = project.name
        def mavenName = project.hasProperty("maven_name") ? project.property("maven_name").toString() : projName
        def projTitle = project.hasProperty("title") ? project.property("title").toString() : projName
        def projDesc = project.description ?: ""
        def projUrl = project.hasProperty("url") ? project.property("url").toString() : null
        def overrideLicense = project.hasProperty("overrideLicense") ? project.property("overrideLicense").toString() : null
        def scmUrl = project.hasProperty("scm_url") ? project.property("scm_url").toString() : null
        def scmConn = project.hasProperty("scm_conn") ? project.property("scm_conn").toString() : null
        def scmDevConn = project.hasProperty("scm_dev_conn") ? project.property("scm_dev_conn").toString() : null

        project.extensions.configure('publishing') { publishing ->
            publishing.publications { pubs ->

                project.plugins.withId("java-gradle-plugin") {
                    pubs.withType(MavenPublication).configureEach { pub ->
                        configurePom(pub, mavenName, projTitle, projDesc, projUrl, overrideLicense, scmUrl, scmConn, scmDevConn)
                    }
                }

                project.plugins.withId("java-library") {
                    pubs.create("mavenJava", MavenPublication) { pub ->
                        pub.from(project.components.getByName("java"))
                        configurePom(pub, mavenName, projTitle, projDesc, projUrl, overrideLicense, scmUrl, scmConn, scmDevConn)
                    }
                }

                project.plugins.withId("java-platform") {
                    if (!pubs.names.contains("mavenBom")) {
                        pubs.create("mavenBom", MavenPublication) { pub ->
                            pub.from(project.components.getByName("javaPlatform"))
                            configurePom(pub, mavenName, projTitle, projDesc, projUrl, overrideLicense, scmUrl, scmConn, scmDevConn)

                            pub.suppressPomMetadataWarningsFor("apiElements")
                            pub.suppressPomMetadataWarningsFor("runtimeElements")
                        }
                    }
                }
            }
        }
    }

    private static void configurePom(MavenPublication pub,
                                     String mavenName,
                                     String projTitle,
                                     String projDesc,
                                     String projUrl,
                                     String overrideLicense,
                                     String scmUrl,
                                     String scmConn,
                                     String scmDevConn) {

        pub.artifactId = mavenName

        pub.pom {
            name = projTitle
            description = projDesc
            url = projUrl

            licenses {
                license {
                    if (overrideLicense == "MIT") {
                        name = "MIT License"
                        url = "https://opensource.org/licenses/MIT"
                    } else {
                        name = "GPL 3.0"
                        url = "https://opensource.org/license/gpl-3.0"
                    }
                }
            }

            scm {
                url = scmUrl
                connection = scmConn
                developerConnection = scmDevConn
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
