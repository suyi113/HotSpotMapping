plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.jar {
    manifest {
        attributes(
            "Premain-Class" to "net.hotspot.cplus.agent.AgentMain",
            "Agent-Class" to "net.mcreator.warden.Agent.AgentMain",
            "Can-Redefine-Classes" to "true",
            "Can-Retransform-Classes" to "true",
            "Implementation-Title" to "My Java Agent",
            "Implementation-Version" to project.version.toString()
        )
    }
}

tasks.test {
    useJUnitPlatform()
}