import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.protoc
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val gradleWrapperVersion = "7.3.3"
val kotlinVersion = "1.6.10"
val protobufVersion = "3.19.4"
val junitVersion = "5.8.2"
val kotestVersion = "5.1.0"
val mockkVersion = "1.12.2"

plugins {
    val kotlinPluginVersion = "1.6.10"
    val kotestPluginVersion = "0.3.8"
    val protobufPluginVersion = "0.8.18"

    // kotlin.
    kotlin("jvm").version(kotlinPluginVersion)

    // protobuf.
    id("com.google.protobuf").version(protobufPluginVersion)

    // kotest.
    id("io.kotest").version(kotestPluginVersion)
}

repositories {
    mavenCentral()
}

dependencies {
    // kotlin.
    implementation(kotlin("stdlib", kotlinVersion))
    implementation(kotlin("stdlib-jdk7", kotlinVersion))
    implementation(kotlin("stdlib-jdk8", kotlinVersion))

    // protobuf.
    implementation("com.google.protobuf:protobuf-java:$protobufVersion")
    implementation("com.google.protobuf:protobuf-java-util:$protobufVersion")
}

dependencies {
    // kotest.
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-property:$kotestVersion")

    // mockk.
    testImplementation("io.mockk:mockk:$mockkVersion")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
    }

    withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "failed", "skipped")
            setExceptionFormat("full")
            showExceptions = true
            showCauses = true
            showStackTraces = false
            debug { events("started", "passed", "failed", "skipped", "standard_error", "standard_out") }
            info { events = debug.events }
        }
        outputs.upToDateWhen { false }
    }

    withType<Wrapper> {
        gradleVersion = gradleWrapperVersion
        distributionType = Wrapper.DistributionType.ALL
    }
}

protobuf {
    protobuf.protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }

    protobuf.generateProtoTasks {
        // remove cached build output to force regeneration of java code.
        // see: https://github.com/google/protobuf-gradle-plugin/issues/331#issuecomment-543333726
        all().forEach { task ->
            task.doFirst {
                delete(task.outputs)
            }
        }
    }
}
