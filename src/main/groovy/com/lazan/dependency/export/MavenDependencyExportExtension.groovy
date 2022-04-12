package com.lazan.dependency.export

abstract class MavenDependencyExportExtension {
    boolean exportSources
    boolean exportJavadoc

    boolean getExportSources() {
        return exportSources
    }

    void setExportSources(boolean exportSources) {
        this.exportSources = exportSources
    }

    boolean getExportJavadoc() {
        return exportJavadoc
    }

    void setExportJavadoc(boolean exportJavadoc) {
        this.exportJavadoc = exportJavadoc
    }
}
