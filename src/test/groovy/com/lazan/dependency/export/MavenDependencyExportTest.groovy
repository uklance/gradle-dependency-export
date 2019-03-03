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
	int increment = 0
	@Rule final TemporaryFolder tempFolder = new TemporaryFolder()
	File testKitDir

	def setup() {
		URL globalProps = getClass().classLoader.getResource('testkit-gradle.properties')
		File testProps = tempFolder.newFile('gradle.properties')
		testProps.text = globalProps.text
		testKitDir = tempFolder.newFolder("testKit${increment++}")
	}
	
	def buildFile(String snippet) {
		File buildFile = tempFolder.newFile('build.gradle')
		buildFile.text = 
"""
plugins {
	id 'com.lazan.dependency-export'
	id 'java'
}
repositories {
	mavenCentral()
}
test {
	testLogging.showStandardStreams = true
}
$snippet
"""
	}

	def "Test dependency export"() {
		given:
		buildFile('''
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
'''
		)
		when:
		def result = GradleRunner.create()
				.withProjectDir(tempFolder.root)
				.withTestKitDir(testKitDir)
				.withArguments('verifyExport', '-i', '--stacktrace')
				.withPluginClasspath()
				.build()
		then:
		System.out.println result.output
		result.task(":verifyExport").outcome == TaskOutcome.SUCCESS
	}
	
	def "Test custom configuration export"() {
		given:
		buildFile('''
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
'''
		)
		when:
		def result = GradleRunner.create()
				.withProjectDir(tempFolder.root)
				.withTestKitDir(testKitDir)
				.withArguments('verifyExport', '-i', '--stacktrace')
				.withPluginClasspath()
				.build()
		then:
		System.out.println result.output
		result.task(":verifyExport").outcome == TaskOutcome.SUCCESS
	}
	
	def "Test custom configuration export with sources and javadoc"() {
		given:
		buildFile('''
configurations { foo }
dependencies {
	foo 'org.hibernate:hibernate-core:3.5.4-Final'
}
mavenDependencyExport {
	configuration 'foo'
	exportSources = true
	exportJavadoc = true
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
		assert paths.contains('org/hibernate/hibernate-core/3.5.4-Final/hibernate-core-3.5.4-Final-sources.jar')
		assert paths.contains('org/hibernate/hibernate-core/3.5.4-Final/hibernate-core-3.5.4-Final.jar')
		assert paths.contains('commons-collections/commons-collections/3.1/commons-collections-3.1.jar')
		assert paths.contains('commons-collections/commons-collections/3.1/commons-collections-3.1-javadoc.jar')
		assert paths.contains('commons-collections/commons-collections/3.1/commons-collections-3.1.pom')
		assert paths.contains('org/hibernate/hibernate-parent/3.5.4-Final/hibernate-parent-3.5.4-Final.pom')
	}
} 
'''
		)
		when:
		def result = GradleRunner.create()
				.withProjectDir(tempFolder.root)
				.withTestKitDir(testKitDir)
				.withArguments('verifyExport', '-i', '--stacktrace')
				.withPluginClasspath()
				.build()
		then:
		System.out.println result.output
		result.task(":verifyExport").outcome == TaskOutcome.SUCCESS
	}
}