plugins {
    id("dev.prism")
}

group = "com.norwood"
version = "1.0.0"

prism {
    metadata {
        modId = "sbw_mavic_extender"
        name = "[SBW] Mavic drone extender"
        description = "A Minecraft mod."
        license = "GPL-3.0"
    }

    curseMaven()
    maven("kotlinforforge", "https://thedarkcolour.github.io/KotlinForForge/")
    maven("theillusivec4", "https://maven.theillusivec4.top/")
    maven("modrinth", "https://api.modrinth.com/maven")

    version("1.20.1") {
        forge {
            loaderVersion = "47.4.18"
            loaderVersionRange = "[47,)"

            dependencies {
                modImplementation("curse.maven:superb-warfare-1218165:8104849")
                modImplementation("curse.maven:geckolib-388172:8285794")
                runtimeOnly("thedarkcolour:kotlinforforge:4.11.0")
                modRuntimeOnly("top.theillusivec4.curios:curios-forge:5.14.1+1.20.1")
                modRuntimeOnly("maven.modrinth:embeddium:0.3.31+mc1.20.1")
            }
        }
    }

    version("1.21.1") {
        neoforge {
            loaderVersion = "21.1.222"
            loaderVersionRange = "[4,)"

            dependencies {
                modImplementation("curse.maven:superb-warfare-1218165:8104860")
                modImplementation("curse.maven:geckolib-388172:8350073")
                runtimeOnly("thedarkcolour:kotlinforforge-neoforge:5.6.0")
            }
        }
    }

}
