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
        /**
         * necessary，default 'null'
         * the old apk path, use to diff with the new apk to build
         * add apk from the build/bakApk
         */
        oldApk = getOldApkPath()
        oldApk = getOldApkPath()
        /**
         * optional，default 'false'
         * there are some cases we may get some warnings
         * if ignoreWarning is true, we would just assert the patch process
         * case 1: minSdkVersion is below 14, but you are using dexMode with raw.
         *         it must be crash when load.
         * case 2: newly added Android Component in AndroidManifest.xml,
         *         it must be crash when load.
         * case 3: loader classes in dex.loader{} are not keep in the main dex,
         *         it must be let tinker not work.
         * case 4: loader classes in dex.loader{} changes,
         *         loader classes is ues to load patch dex. it is useless to change them.
         *         it won't crash, but these changes can't effect. you may ignore it
         * case 5: resources.arsc has changed, but we don't use applyResourceMapping to build
         */
        ignoreWarning = ext["ignoreWarning"] as Boolean

        /**
         * optional，default 'true'
         * whether sign the patch file
         * if not, you must do yourself. otherwise it can't check success during the patch loading
         * we will use the sign config with your build type
         */
        useSign = true

        /**
         * optional，default 'true'
         * whether use tinker to build
         */
        tinkerEnable = buildWithTinker()

        /**
         * Warning, applyMapping will affect the normal android build!
         */
        buildConfig{

            /**
             * optional，default 'null'
             * if we use tinkerPatch to build the patch apk, you'd better to apply the old
             * apk mapping file if minifyEnabled is enable!
             * Warning:
             * you must be careful that it will affect the normal assemble build!
             */
            applyMapping = getApplyMappingPath()

            /**
             * optional，default 'null'
             * It is nice to keep the resource id from R.txt file to reduce java changes
             */
            applyResourceMapping = getApplyResourceMappingPath()

            /**
             * necessary，default 'null'
             * because we don't want to check the base apk with md5 in the runtime(it is slow)
             * tinkerId is use to identify the unique base apk when the patch is tried to apply.
             * we can use git rev, svn rev or simply versionCode.
             * we will gen the tinkerId in your manifest automatic
             */
            tinkerId = getTinkerIdValue()

            /**
             * if keepDexApply is true, class in which dex refer to the old apk.
             * open this can reduce the dex diff file size.
             */
            keepDexApply = false

            /**
             * optional, default 'false'
             * Whether tinker should treat the base apk as the one being protected by app
             * protection tools.
             * If this attribute is true, the generated patch package will contain a
             * dex including all changed classes instead of any dexdiff patch-info files.
             */
            isProtectedApp = ext["isProtectedApp"] as Boolean

            /**
             * optional, default 'false'
             * Whether tinker should support component hotplug (add new component dynamically).
             * If this attribute is true, the component added in new apk will be available after
             * patch is successfully loaded. Otherwise an error would be announced when generating patch
             * on compile-time.
             *
             * <b>Notice that currently this feature is incubating and only support NON-EXPORTED Activity</b>
             */
            supportHotplugComponent = false
        }


        dex{
            /**
             * optional，default 'jar'
             * only can be 'raw' or 'jar'. for raw, we would keep its original format
             * for jar, we would repack dexes with zip format.
             * if you want to support below 14, you must use jar
             * or you want to save rom or check quicker, you can use raw mode also
             */
            dexMode = "raw"

            /**
             * necessary，default '[]'
             * what dexes in apk are expected to deal with tinkerPatch
             * it support * or ? pattern.
             */
            pattern = mutableSetOf("classes*.dex", "assets/secondary-dex-?.jar")

            /**
             * necessary，default '[]'
             * Warning, it is very very important, loader classes can't change with patch.
             * thus, they will be removed from patch dexes.
             * you must put the following class into main dex.
             * Simply, you should add your own application {@code tinker.sample.android.SampleApplication}
             * own tinkerLoader, and the classes you use in them
             *
             */
            loader = mutableSetOf("tinker.sample.android.app.BaseBuildInfo")
        }


        lib{
            /**
             * optional，default '[]'
             * what library in apk are expected to deal with tinkerPatch
             * it support * or ? pattern.
             * for library in assets, we would just recover them in the patch directory
             * you can get them in TinkerLoadResult with Tinker
             */
            pattern = mutableSetOf("lib/*/*.so")
        }


        res{
            /**
             * optional，default '[]'
             * what resource in apk are expected to deal with tinkerPatch
             * it support * or ? pattern.
             * you must include all your resources in apk here,
             * otherwise, they won't repack in the new apk resources.
             */
            pattern = mutableSetOf("res/*", "assets/*", "resources.arsc", "AndroidManifest.xml")

            /**
             * optional，default '[]'
             * the resource file exclude patterns, ignore add, delete or modify resource change
             * it support * or ? pattern.
             * Warning, we can only use for files no relative with resources.arsc
             */
            ignoreChange = mutableSetOf("assets/sample_meta.txt")

            /**
             * default 100kb
             * for modify resource, if it is larger than 'largeModSize'
             * we would like to use bsdiff algorithm to reduce patch file size
             */
            largeModSize = 100
        }

        packageConfig{

            /**
             * optional，default 'TINKER_ID, TINKER_ID_VALUE' 'NEW_TINKER_ID, NEW_TINKER_ID_VALUE'
             * package meta file gen. path is assets/package_meta.txt in patch file
             * you can use securityCheck.getPackageProperties() in your ownPackageCheck method
             * or TinkerLoadResult.getPackageConfigByName
             * we will get the TINKER_ID from the old apk manifest for you automatic,
             * other config files (such as patchMessage below)is not necessary
             */
            configField("patchMessage", "tinker is sample to use")

            /**
             * just a sample case, you can use such as sdkVersion, brand, channel...
             * you can parse it in the SamplePatchListener.
             * Then you can use patch conditional!
             */
            configField("platform", "all")

            /**
             * patch version via packageConfig
             */
            configField("patchVersion", "1.0")
        }

        //or you can add config filed outside, or get meta value from old apk
        //project.tinkerPatch.packageConfig.configField("test1", project.tinkerPatch.packageConfig.getMetaDataFromOldApk("Test"))
        //project.tinkerPatch.packageConfig.configField("test2", "sample")

        /**
         * if you don't use zipArtifact or path, we just use 7za to try
         */
        sevenZip{

            /**
             * optional，default '7za'
             * the 7zip artifact path, it will use the right 7za with your platform
             */
//            zipArtifact = "com.tencent.mm:SevenZip:1.1.10"
            /**
             * optional，default '7za'
             * you can specify the 7za path yourself, it will overwrite the zipArtifact value
             */
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