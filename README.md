# gradle-dependency-export [![Build Status](https://travis-ci.org/uklance/gradle-dependency-export.svg?branch=master)](https://travis-ci.org/uklance/gradle-dependency-export) <a href='https://coveralls.io/github/uklance/gradle-dependency-export?branch=master'><img src='https://coveralls.io/repos/github/uklance/gradle-dependency-export/badge.svg?branch=master' alt='Coverage Status' /></a>

Export maven dependencies from a gradle project to the file system

## Usage (plugin DSL)
```
plugins {
  id "com.lazan.dependency-export" version "0.2"
}
```

## Usage (legacy)
```
buildscript {
  repositories {
    maven {
      url "https://plugins.gradle.org/m2/"
    }
  }
  dependencies {
    classpath "com.lazan:gradle-dependency-export:0.2"
  }
}

apply plugin: "com.lazan.dependency-export"
```

## mavenDependencyExport task

### Properties

|Name|Type|Default Value|
|----|----|-------------|
|configurations|Collection<Configuration>|buildscript.configurations + project.configurations|
|systemProperties|Map<String, Object>|System.getProperties()|

### Sample task customisation
```
plugins {
  id "com.lazan.dependency-export" version "0.2"
}
mavenDependencyExport {
  systemProperties = ['java.version': '1.9']
  configuration 'foo'
  configuration buildscript.configurations.classpath
}
```
