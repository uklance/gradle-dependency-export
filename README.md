# gradle-dependency-export [![Build Status](https://travis-ci.org/uklance/gradle-dependency-export.svg?branch=master)](https://travis-ci.org/uklance/gradle-dependency-export) <a href='https://coveralls.io/github/uklance/gradle-dependency-export?branch=master'><img src='https://img.shields.io/coveralls/github/uklance/gradle-dependency-export/master.svg' alt='Coverage Status' /></a>

Export maven dependencies from a gradle project to the file system

## Usage (plugin DSL)
```
plugins {
  id "com.lazan.dependency-export" version "0.3"
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
    classpath "com.lazan:gradle-dependency-export:0.3"
  }
}

apply plugin: "com.lazan.dependency-export"
```

## mavenDependencyExport task

### Properties

|Name|Type|Default Value|
|----|----|-------------|
|configurations|Collection\<Configuration\>|buildscript.configurations + project.configurations|
|systemProperties|Map\<String, Object\>|System.getProperties()|

### Sample task customisation
```
plugins {
  id "com.lazan.dependency-export" version "0.3"
}
configurations {
  foo
}
dependencies {
   foo 'x:y:1.0'
}
mavenDependencyExport {
  systemProperties = ['java.version': '1.8']
  configuration 'foo'
  configuration buildscript.configurations.classpath
}
```
