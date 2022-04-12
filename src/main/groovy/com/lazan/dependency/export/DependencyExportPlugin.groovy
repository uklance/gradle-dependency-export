package com.lazan.dependency.export

import org.gradle.api.Plugin
import org.gradle.api.Project

class DependencyExportPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.tasks.create(name: 'mavenDependencyExport', type: MavenDependencyExport)
        project.extensions.create('mavenDependencyExport', MavenDependencyExportExtension)
    }
}
