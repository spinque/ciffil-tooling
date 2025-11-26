description = "CIFFIL Service Utils"

plugins {
        `java-library`
        application
}


java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(24))
  }
}

application {
        mainClass.set("com.spinque.ciff.app.Launcher")
}

// Application name is specified here and in Picocli annotation of Launcher class
application.applicationName = "ciff-cli"

dependencies {
	api(project(":Utils"))
        testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	    implementation(libs.okhttp)
        implementation(libs.picocli)

        api(libs.bundles.log4j)
        implementation(libs.log4j.slf4jImpl)
        testImplementation(libs.log4j.slf4jImpl)

        testImplementation(libs.junit.jupiterParams)
}
