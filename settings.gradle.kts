rootProject.name = "HMCLDL"
include(
    "HMCLDL",
    "HMCLDLCore",
    "HMCLDLBoot"
)

val minecraftLibraries = listOf("HMCLDLTransformerDiscoveryService", "HMCLDLMultiMCBootstrap")
include(minecraftLibraries)

for (library in minecraftLibraries) {
    project(":$library").projectDir = file("minecraft/libraries/$library")
}
