package com.lazan.dependency.export

import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

import org.gradle.api.Project;
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class MavenDependencyExportTest extends Specification {

	@Rule final TemporaryFolder tempFolder = new TemporaryFolder()

	def setup() {
		URL testkitPropsUrl = getResourceUrl("testkit-gradle.properties")
		testkitPropsUrl.withReader {
			writeFile('gradle.properties', it.text)
		}
	}

	URL getResourceUrl(String path) {
		URL url = getClass().classLoader.getResource(path)
		if (url == null) throw new RuntimeException("No such resource $path")
		return url
	}

	void writeFile(String path, String text) {
		File file = new File(tempFolder.root, path)
		file.parentFile.mkdirs()
		file.text = text
	}

	def "Test dependency export"() {
		given:
		File testkitDir = tempFolder.newFolder('testkit1')
		writeFile("build.gradle", '''
				plugins {
					id 'com.lazan.dependency-export'
					id 'java'
				}
				repositories {
					mavenCentral()
				}
				dependencies {
					compile 'org.hibernate:hibernate-core:3.5.4-Final'

				}
				task verifyExport {
					dependsOn mavenDependencyExport
					doLast {
						def paths = []
						fileTree("$buildDir/maven-dependency-export").visit {
							if (!it.file.directory) {
								paths << it.relativePath.pathString
							}
						}
						assert paths.contains('org/hibernate/hibernate-core/3.5.4-Final/hibernate-core-3.5.4-Final.pom')
						assert paths.contains('org/hibernate/hibernate-core/3.5.4-Final/hibernate-core-3.5.4-Final.jar')
						assert paths.contains('commons-collections/commons-collections/3.1/commons-collections-3.1.jar')
						assert paths.contains('commons-collections/commons-collections/3.1/commons-collections-3.1.pom')
						assert paths.contains('org/hibernate/hibernate-parent/3.5.4-Final/hibernate-parent-3.5.4-Final.pom')
					}
				} 
			''')
		when:
		def result = GradleRunner.create()
				.withProjectDir(tempFolder.root)
				.withTestKitDir(testkitDir)
				.withArguments('verifyExport', '--stacktrace')
				.withPluginClasspath()
				.build()
		then:
		result.task(":verifyExport").outcome == TaskOutcome.SUCCESS
	}
	
	def "Test custom configuration export"() {
		given:
		File testkitDir = tempFolder.newFolder('testkit2')
		writeFile("build.gradle", '''
				plugins {
					id 'com.lazan.dependency-export'
				}
				repositories {
					mavenCentral()
				}
				configurations { foo }
				dependencies {
					foo 'org.hibernate:hibernate-core:3.5.4-Final'
				}
				mavenDependencyExport {
					configuration 'foo'
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
						assert paths.contains('org/hibernate/hibernate-core/3.5.4-Final/hibernate-core-3.5.4-Final.pom')
						assert paths.contains('org/hibernate/hibernate-core/3.5.4-Final/hibernate-core-3.5.4-Final.jar')
						assert paths.contains('commons-collections/commons-collections/3.1/commons-collections-3.1.jar')
						assert paths.contains('commons-collections/commons-collections/3.1/commons-collections-3.1.pom')
						assert paths.contains('org/hibernate/hibernate-parent/3.5.4-Final/hibernate-parent-3.5.4-Final.pom')
					}
				} 
			''')
		when:
		def result = GradleRunner.create()
				.withProjectDir(tempFolder.root)
				.withTestKitDir(testkitDir)
				.withArguments('verifyExport', '--stacktrace')
				.withPluginClasspath()
				.build()
		then:
		result.task(":verifyExport").outcome == TaskOutcome.SUCCESS
	}
	
	def "Test custom configuration export with sources and javadoc"() {
		given:
		File testkitDir = tempFolder.newFolder('testkit3')
		writeFile("build.gradle", '''
				plugins {
					id 'com.lazan.dependency-export'
				}
				repositories {
					mavenCentral()
				}
				configurations { foo }
				dependencies {
					foo 'org.hibernate:hibernate-core:3.5.4-Final'
				}
				mavenDependencyExport {
					configuration 'foo'
					exportDir = file("$buildDir/offline-repo")
					exportSources = true
					exportJavadoc = true
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
						assert paths.contains('org/hibernate/hibernate-core/3.5.4-Final/hibernate-core-3.5.4-Final.pom')
						assert paths.contains('org/hibernate/hibernate-core/3.5.4-Final/hibernate-core-3.5.4-Final-sources.jar')
						assert paths.contains('org/hibernate/hibernate-core/3.5.4-Final/hibernate-core-3.5.4-Final.jar')
						assert paths.contains('commons-collections/commons-collections/3.1/commons-collections-3.1.jar')
						assert paths.contains('commons-collections/commons-collections/3.1/commons-collections-3.1-javadoc.jar')
						assert paths.contains('commons-collections/commons-collections/3.1/commons-collections-3.1.pom')
						assert paths.contains('org/hibernate/hibernate-parent/3.5.4-Final/hibernate-parent-3.5.4-Final.pom')
					}
				} 
			''')
		when:
		def result = GradleRunner.create()
				.withProjectDir(tempFolder.root)
				.withTestKitDir(testkitDir)
				.withArguments('verifyExport', '--stacktrace')
				.withPluginClasspath()
				.build()
		then:
		result.task(":verifyExport").outcome == TaskOutcome.SUCCESS
	}
}
