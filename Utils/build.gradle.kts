description = "CIFFIL Service Utils"

plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(24))
    }
}

tasks.test {
	useJUnitPlatform()
}

dependencies {

    implementation(libs.monetdbJdbc)

    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.junit.jupiterEngine)
    testRuntimeOnly(libs.junit.platformLauncher)
    implementation(libs.protobuf)
    implementation(libs.apache.commons.text)
}
