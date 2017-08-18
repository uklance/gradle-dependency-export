package com.lazan.dependency.export

import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

import org.gradle.api.Project;
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.util.regex.*
import static org.junit.Assert.*


import spock.lang.Specification

class MavenDependencyExportTest extends Specification {

    @Rule final TemporaryFolder testProjectDir = new TemporaryFolder()

    def setup() {
        writeFile('gradle.properties', getResourceUrl("testkit-gradle.properties").text)
    }

    URL getResourceUrl(String path) {
        URL url = getClass().classLoader.getResource(path)
        if (url == null) throw new RuntimeException("No such resource $path")
        return url
    }

    void writeFile(String path, String text) {
        File file = new File(testProjectDir.root, path)
        file.parentFile.mkdirs()
        file.text = text
    }

    def "Test dependency export"() {
        given:
        writeFile("build.gradle", '''
				plugins {
					id 'com.lazan.dependency-export'
					id 'java'
				}
				repositories {
					mavenCentral()
				}
				dependencies {
				    compile 'org.springframework:spring-core:4.3.10.RELEASE'

				}
				mavenDependencyExport {
				    configuration 'compile'
				    exportDir = file("$buildDir/offline-repo")
                }
                task verifyExport {
                    dependsOn mavenDependencyExport
                    doLast {
                        def paths = []
                        fileTree("$buildDir/offline-repo").visit {
                            if (!it.file.directory) {
                                paths << it.relativePath.pathString
                            }
                        }
                        assert paths.contains('org/springframework/spring-core/4.3.10.RELEASE/spring-core-4.3.10.RELEASE.jar')
                        assert paths.contains('org/springframework/spring-core/4.3.10.RELEASE/spring-core-4.3.10.RELEASE.pom')
                        assert paths.contains('commons-logging/commons-logging/1.2/commons-logging-1.2.jar')
                        assert paths.contains('commons-logging/commons-logging/1.2/commons-logging-1.2.pom')
                    }
                } 
			''')
        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('verifyExport', '--stacktrace')
                .withPluginClasspath()
                .build()
        then:
        result.task(":verifyExport").outcome == TaskOutcome.SUCCESS
    }
}
