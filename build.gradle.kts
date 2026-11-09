plugins {
    java
}

// 基本信息：后续想改包名/版本，改这里即可
group = "com.example.escui"
version = "2.0.0"

repositories {
    mavenCentral()
    // Paper 的仓库
    maven("https://repo.papermc.io/repository/maven-public/")
    // PlaceholderAPI（可选的软依赖，只在编译期用）
    maven("https://repo.extendedclip.com/releases/")
}

dependencies {
    // Paper API：服务端是 Paper 26.2（游戏版本 26.2），所以这里依赖 26.2 的 API。
    // compileOnly 表示只在编译期用，不会被打进 jar（服务端本身已经有这些类）。
    compileOnly("io.papermc.paper:paper-api:26.2.build.129-stable")

    // PlaceholderAPI：软依赖，服务端装了就用、没装也不影响运行。
    // 同样是 compileOnly，不会打进 jar。
    compileOnly("me.clip:placeholderapi:2.12.3")
}

java {
    // 用 JDK 25 编译（对应 MC 26.x 的运行时）
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
    // 打开过时 API 提示，方便及时发现 Paper 标记为待删除的方法
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.processResources {
    // 把 build.gradle.kts 里的 version 注入到 plugin.yml 的 ${version} 占位符
    val props = mapOf("version" to version.toString())
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching(listOf("plugin.yml", "paper-plugin.yml")) {
        expand(props)
    }
}

tasks.jar {
    // 生成的 jar 名字，例如 EscUI-1.0.0.jar
    archiveBaseName.set("EscUI")
}
