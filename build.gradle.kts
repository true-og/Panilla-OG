import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmVendorSpec

plugins {
    id("eclipse")
}

val hasBootstrapRepo = System.getProperty("SELF_MAVEN_LOCAL_REPO")
    ?.takeIf { it.isNotBlank() }
    ?.let { custom ->
        val resolvedPath = if (custom.startsWith("~")) {
            custom.replaceFirst("^~".toRegex(), System.getProperty("user.home"))
        } else {
            custom
        }
        file(resolvedPath).isDirectory
    }
    ?: false

allprojects {
    group = "com.ruinscraft"
    version = "1.9.0"
}

subprojects {
    apply(plugin = "java")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
            vendor.set(JvmVendorSpec.GRAAL_VM)
        }
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    if (!hasBootstrapRepo) {
        configurations.configureEach {
            resolutionStrategy.dependencySubstitution {
                all {
                    val requestedModule = requested as? ModuleComponentSelector ?: return@all
                    if (requestedModule.group == "org.spigotmc" && requestedModule.module == "spigot") {
                        useTarget(
                            "org.bukkit:craftbukkit:${requestedModule.version}",
                            "TrueOG Bootstrap is unavailable, so fall back to mirrored CraftBukkit artifacts for server internals."
                        )
                    }
                }
            }
        }
    }

    configurations.configureEach {
        resolutionStrategy.dependencySubstitution {
            substitute(module("com.google.code.gson:gson:2.8.8"))
                .using(module("com.google.code.gson:gson:2.8.9"))
                .because("gson 2.8.8 jar is missing from the bootstrap maven repo; 2.8.9 is API-compatible and is the version already used by spigot-api 1.18.2.")
            substitute(module("com.google.code.gson:gson:2.10"))
                .using(module("com.google.code.gson:gson:2.10.1"))
                .because("gson 2.10 jar is missing from the bootstrap maven repo; 2.10.1 is API-compatible and is the version already used by spigot-api 1.20.4.")
            if (hasBootstrapRepo) {
                substitute(module("org.spigotmc:spigot:1.18.2-R0.1-SNAPSHOT"))
                    .using(module("org.bukkit:craftbukkit:1.18.2-R0.1-SNAPSHOT"))
                    .because("spigot 1.18.2 jar is missing from the bootstrap maven repo; the mirrored CraftBukkit 1.18.2 artifact exposes the same v1_18_R2 server internals needed to compile this module.")
                substitute(module("org.spigotmc:spigot:1.19.3-R0.1-SNAPSHOT"))
                    .using(module("org.bukkit:craftbukkit:1.19.3-R0.1-SNAPSHOT"))
                    .because("spigot 1.19.3 jar is missing from the bootstrap maven repo; the mirrored CraftBukkit 1.19.3 artifact exposes the same v1_19_R2 server internals needed to compile this module.")
                substitute(module("org.spigotmc:spigot:1.19.4-R0.1-SNAPSHOT"))
                    .using(module("org.bukkit:craftbukkit:1.19.4-R0.1-SNAPSHOT"))
                    .because("spigot 1.19.4 jar is missing from the bootstrap maven repo; the mirrored CraftBukkit 1.19.4 artifact exposes the same v1_19_R3 server internals needed to compile this module.")
            }
        }
    }
}

tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
    delete("target")
}
