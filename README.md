# gradle-dependency-export [![Build Status](https://travis-ci.org/uklance/gradle-dependency-export.svg?branch=master)](https://travis-ci.org/uklance/gradle-dependency-export) <a href='https://coveralls.io/github/uklance/gradle-dependency-export?branch=master'><img src='https://coveralls.io/repos/github/uklance/gradle-dependency-export/badge.svg?branch=master' alt='Coverage Status' /></a>

Export maven dependencies from a gradle project to the file system

## How to build and deploy to local maven Repository

    gradlew clean publishToMavenLocal

## How to include plugin

### settings.gradle
    
    pluginManagement {
        repositories {
            mavenLocal()
            maven {
                url 'http://your_own_repository'
            }
            gradlePluginPortal()
        }
    }

### build.gradle

    buildscript {
        dependencies {
            classpath group: 'com.lazan',
                    name: 'gradle-dependency-export',
                    version: '0.1-SNAPSHOT'
        }
    }

    ...

    apply plugin: 'com.lazan.dependency-export'

