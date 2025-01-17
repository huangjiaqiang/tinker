import com.tencent.tinker.build.gradle.extension.TinkerArkHotExtension
import com.tencent.tinker.build.gradle.extension.TinkerBuildConfigExtension
import com.tencent.tinker.build.gradle.extension.TinkerDexExtension
import com.tencent.tinker.build.gradle.extension.TinkerLibExtension
import com.tencent.tinker.build.gradle.extension.TinkerPackageConfigExtension
import com.tencent.tinker.build.gradle.extension.TinkerResourceExtension
import com.tencent.tinker.build.gradle.extension.TinkerSevenZipExtension
import org.gradle.internal.declarativedsl.parsing.main
import java.text.SimpleDateFormat
import java.util.Date

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

fun getTinkerId():String{
    return "${android.defaultConfig.versionName}.${android.defaultConfig.versionCode}"
}

android {
    namespace = "tinker.sample.android"
    compileSdk = 34

    buildFeatures{
        buildConfig = true
    }

    sourceSets {
        getByName("main"){
            java.srcDirs("../../tinker-sample-android/app/src/main/java")
            res.srcDirs("../../tinker-sample-android/app/src/main/res")
            manifest.srcFile("../../tinker-sample-android/app/src/main/AndroidManifest.xml")
        }
    }

    defaultConfig {
        applicationId = "tinker.sample.android2"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        multiDexKeepProguard = file("tinker_multidexkeep.pro")

        buildConfigField("String", "MESSAGE", "\"I am the base apk\"")
        buildConfigField("String", "TINKER_ID", "\"${getTinkerId()}\"")
        buildConfigField("String", "PLATFORM", "\"all\"")
    }

    signingConfigs {
        create("release") {
            try {
                storeFile = file("./keystore/release.keystore")
                storePassword = "testres"
                keyAlias = "testres"
                keyPassword = "testres"
            } catch (ex: Exception) {
                throw InvalidUserDataException(ex.toString())
            }
        }
//        create("debug") {
//            storeFile = file("./keystore/debug.keystore")
//        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                file("proguard-rules.pro")
            )
        }
        getByName("debug") {
            isDebuggable = true
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("libs")
        }
    }

}
val TINKER_VERSION = project.findProperty("TINKER_VERSION")

dependencies {

    implementation("androidx.appcompat:appcompat:1.1.0")
    api("com.tencent.tinker:tinker-android-lib:${TINKER_VERSION}") {
        isChanging = true
    }

    implementation("com.tencent.tinker:tinker-android-loader:${TINKER_VERSION}") {
        isChanging = true
    }
}

val bakPath = file("${buildDir}/bakApk/")

val runTaskName = gradle.startParameter.taskNames.firstOrNull() ?: ""

// Extension properties
ext {
    set("tinkerEnabled", true)
    set("ignoreWarning", true)
    set("isProtectedApp", false)

    set("apkFileName", when {
        runTaskName.toLowerCase().contains("debug") -> "app-debug-old"
        runTaskName.toLowerCase().contains("release") -> "app-release-old"
        else -> ""
    })

    // For normal build
    set("tinkerOldApkPath", "${bakPath}/${ext["apkFileName"]}.apk")
    set("tinkerApplyMappingPath", "${bakPath}/${ext["apkFileName"]}-mapping.txt")
    set("tinkerApplyResourcePath", "${bakPath}/${ext["apkFileName"]}-R.txt")
    set("tinkerBuildFlavorDirectory", "${bakPath}/${ext["apkFileName"]}")
}

fun getOldApkPath(): String {
    return if (hasProperty("OLD_APK")) property("OLD_APK").toString() else ext["tinkerOldApkPath"].toString()
}

fun getApplyMappingPath(): String {
    return if (hasProperty("APPLY_MAPPING")) property("APPLY_MAPPING").toString() else ext["tinkerApplyMappingPath"].toString()
}

fun getApplyResourceMappingPath(): String {
    return if (hasProperty("APPLY_RESOURCE")) property("APPLY_RESOURCE").toString() else ext["tinkerApplyResourcePath"].toString()
}

fun getTinkerIdValue(): String {
    return if (hasProperty("TINKER_ID")) property("TINKER_ID").toString() else getTinkerId()
}

fun buildWithTinker(): Boolean {
    return if (hasProperty("TINKER_ENABLE")) property("TINKER_ENABLE").toString().toBoolean() else ext["tinkerEnabled"] as Boolean
}

fun getTinkerBuildFlavorDirectory(): String {
    return ext["tinkerBuildFlavorDirectory"].toString()
}

if (buildWithTinker()) {
    apply(plugin = "com.tencent.tinker.patch")
    configure<com.tencent.tinker.build.gradle.extension.TinkerPatchExtension> {
        oldApk = getOldApkPath()
        ignoreWarning = ext["ignoreWarning"] as Boolean
        useSign = true
        tinkerEnable = buildWithTinker()


        buildConfig = TinkerBuildConfigExtension(project).apply {
            applyMapping = getApplyMappingPath()
            applyResourceMapping = getApplyResourceMappingPath()
            tinkerId = getTinkerIdValue()
            keepDexApply = false
            isProtectedApp = ext["isProtectedApp"] as Boolean
            supportHotplugComponent = false
        }

        dex = TinkerDexExtension(project).apply {
            dexMode = "raw"
            pattern = mutableSetOf("classes*.dex", "assets/secondary-dex-?.jar")
            loader = mutableSetOf("tinker.sample.android.app.BaseBuildInfo")
        }

        lib = TinkerLibExtension().apply{
            pattern = mutableSetOf("lib/*/*.so")
        }


        res = TinkerResourceExtension().apply{
            pattern = mutableSetOf("res/*", "assets/*", "resources.arsc", "AndroidManifest.xml")
            ignoreChange = mutableSetOf("assets/sample_meta.txt")
            largeModSize = 100
        }

        arkHot = TinkerArkHotExtension()
        packageConfig = TinkerPackageConfigExtension(project).apply {
            configField("patchMessage", "tinker is sample to use")
            configField("platform", "all")
            configField("patchVersion", "1.0")
        }

        sevenZip = TinkerSevenZipExtension(project).apply {
            path = "/usr/local/bin/7za"
        }
    }



    val flavors = android.productFlavors.map { it.name }
    val hasFlavors = flavors.isNotEmpty()

    android.applicationVariants.all {
        val variant = this
        val taskName = variant.name
        val runTask = gradle.startParameter.taskNames.firstOrNull() ?: ""

        if (!runTask.endsWith("assemble${taskName.capitalize()}")) {
            return@all
        }

        tasks.all {
            if ("assemble${taskName.capitalize()}" == name) {
                doLast {
                    val fileNamePrefix = "${project.name}-${variant.baseName}"
                    val newFileNamePrefix = if (hasFlavors) fileNamePrefix else "${fileNamePrefix}-old"
                    val destPath = if (hasFlavors) {
                        file("${bakPath}/${project.name}-${SimpleDateFormat("MMdd-HH-mm-ss").format(Date())}/${variant.flavorName}")
                    } else {
                        bakPath
                    }

                    copy {
                        val packageAndroidArtifact = variant.packageApplicationProvider.get()
                        from(File(packageAndroidArtifact.outputDirectory.asFile.get(), variant.outputs.first().outputFile.name))

                        into(destPath)
                        rename { fileName ->
                            fileName.replace("${fileNamePrefix}.apk", "${newFileNamePrefix}.apk")
                        }
                    }

                    copy {
                        val dirName = if (hasFlavors) taskName else variant.dirName
                        from("${buildDir}/outputs/mapping/${dirName}/mapping.txt")
                        into(destPath)
                        rename { fileName ->
                            fileName.replace("mapping.txt", "${newFileNamePrefix}-mapping.txt")
                        }
                    }

                    copy {
                        val dirName = if (hasFlavors) taskName else variant.dirName
                        from("${buildDir}/intermediates/symbols/${dirName}/R.txt")
                        from("${buildDir}/intermediates/symbol_list/${dirName}/R.txt")
                        from("${buildDir}/intermediates/runtime_symbol_list/${dirName}/R.txt")
                        from("${buildDir}/intermediates/runtime_symbol_list/${dirName}/process${dirName}Resources/R.txt")
                        into(destPath)
                        rename { fileName ->
                            fileName.replace("R.txt", "${newFileNamePrefix}-R.txt")
                        }
                    }
                }
            }
        }
    }

    if (hasFlavors) {
        tasks.register("tinkerPatchAllFlavorRelease") {
            group = "tinker"
            val originOldPath = getTinkerBuildFlavorDirectory()

            flavors.forEach { flavor ->
                dependsOn("tinkerPatch${flavor.capitalize()}Release")

                tasks.named("process${flavor.capitalize()}ReleaseManifest") {
                    doFirst {
                        val flavorName = name.substring(7, 8).toLowerCase() + name.substring(8, name.length - 15)
                        project.extensions.configure<com.tencent.tinker.build.gradle.extension.TinkerPatchExtension> {
                            oldApk = "${originOldPath}/${flavorName}/${project.name}-${flavorName}-release.apk"
                            val buildConfig = project.extensions.getByType(com.tencent.tinker.build.gradle.extension.TinkerBuildConfigExtension::class.java)
                            buildConfig.applyMapping = "${originOldPath}/${flavorName}/${project.name}-${flavorName}-release-mapping.txt"
                            buildConfig.applyResourceMapping = "${originOldPath}/${flavorName}/${project.name}-${flavorName}-release-R.txt"
                        }
                    }
                }
            }
        }

        tasks.register("tinkerPatchAllFlavorDebug") {
            group = "tinker"
            val originOldPath = getTinkerBuildFlavorDirectory()

            flavors.forEach { flavor ->
                dependsOn("tinkerPatch${flavor.capitalize()}Debug")

                tasks.named("process${flavor.capitalize()}DebugManifest") {
                    doFirst {
                        val flavorName = name.substring(7, 8).toLowerCase() + name.substring(8, name.length - 13)
                        project.extensions.configure<com.tencent.tinker.build.gradle.extension.TinkerPatchExtension> {
                            oldApk = "${originOldPath}/${flavorName}/${project.name}-${flavorName}-debug.apk"
                            val buildConfig = project.extensions.getByType(com.tencent.tinker.build.gradle.extension.TinkerBuildConfigExtension::class.java)
                            buildConfig.applyMapping = "${originOldPath}/${flavorName}/${project.name}-${flavorName}-debug-mapping.txt"
                            buildConfig.applyResourceMapping = "${originOldPath}/${flavorName}/${project.name}-${flavorName}-debug-R.txt"
                        }
                    }
                }
            }
        }
    }
}

tasks.register("sortPublicTxt") {
    doLast {
        val originalFile = project.file("public.txt")
        val sortedFile = project.file("public_sort.txt")
        val sortedLines = mutableListOf<String>()

        originalFile.readLines().forEach {
            sortedLines.add(it)
        }
        sortedLines.sort()

        sortedFile.delete()
        sortedFile.writeText(sortedLines.joinToString("\n"))
    }
}