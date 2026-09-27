plugins {
    id("maafw.kotlin.jvm")
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit5"))
}

tasks.test {
    useJUnitPlatform()
}
