rootProject.name = "CIFF Tooling"

include("App", "App")
include("Utils", "Utils")

dependencyResolutionManagement {
repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = uri("https://repo.clojars.org/")
    }
}

	versionCatalogs {
		create("libs") {
			library("apache-jena", "org.apache.jena:apache-jena-libs:4.+")
            library("apache-commons-text", "org.apache.commons:commons-text:1.14.+")
			library("junit-jupiterEngine", "org.junit.jupiter:junit-jupiter-engine:5.10.2")
			library("junit-jupiterParams", "org.junit.jupiter:junit-jupiter-params:5.10.2")
			library("junit-platformLauncher", "org.junit.platform:junit-platform-launcher:1.10.2")
			library("log4j-api", "org.apache.logging.log4j:log4j-api:[2.17.2,3.0)")
			library("log4j-core", "org.apache.logging.log4j:log4j-core:[2.17.2,3.0)")
			library("log4j-slf4jImpl", "org.apache.logging.log4j:log4j-slf4j2-impl:[2.17.2,3.0)")
			library("log4j-web", "org.apache.logging.log4j:log4j-jakarta-web:[2.17.2,3.0)")
			library("mariadbJavaClient", "org.mariadb.jdbc:mariadb-java-client:2+")
			library("monetdbJdbc", "monetdb:monetdb-jdbc:12.0")
			library("okhttp", "com.squareup.okhttp3:okhttp:4.+")
			library("picocli", "info.picocli:picocli:4.+")
			library("postgresql", "org.postgresql:postgresql:42+")
			library("protobuf", "com.google.protobuf:protobuf-java:4.28.+")
			library("slf4jApi", "org.slf4j:slf4j-api:1.7.+")
			library("zstdJni", "com.github.luben:zstd-jni:1+")
			bundle("log4j", listOf("log4j-api", "log4j-core", "slf4jApi"))
		}
	}
}
