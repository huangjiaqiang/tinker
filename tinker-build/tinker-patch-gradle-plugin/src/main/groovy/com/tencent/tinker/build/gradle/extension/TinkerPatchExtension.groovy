/*
 * Tencent is pleased to support the open source community by making Tinker available.
 *
 * Copyright (C) 2016 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.tinker.build.gradle.extension

import com.android.build.gradle.api.AndroidSourceSet
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project;

/**
 * The configuration properties.
 *
 * @author zhangshaowen
 */

public class TinkerPatchExtension {
    /**
     * Specifies the old apk path to diff with the new apk
     */
    String oldApk

    /**
     * Specify a folder for the outputs where place the tinker patch results.
     */
    String outputFolder

    /**
     * Specify the new apk path instead of running assemble task again.
     */
    String newApk;

    /**
     * If there is loader class changes,
     * or Activity, Service, Receiver, Provider change, it will terminal
     * if ignoreWarning is false
     * default: false
     */
    boolean ignoreWarning

    /**
     *
     * Allow loader class existence in any class loader.
     *
     * This will suppress the exception like:
     * <pre>
     * loader classes are found in old secondary dex. Found classes: ...
     * loader classes are found in new secondary dex. Found classes: ...
     * </pre>
     *
     * <p>Since Android Gradle Plugin 3.3.0, there is no simply way to keep all loader classes in
     * the primary dex file if your application's min sdk version is 21 or above. In this situation, you
     * can turn the {@link #removeLoaderForAllDex} and {@link #allowLoaderInAnyDex} to true and
     * tolerate the loader classes to exists in any dex file.
     *
     * <p>default: false
     */
    boolean allowLoaderInAnyDex

    /**
     * Whether to remove loader class for every dex file. When false, we will assume the loader
     * class only exists in the main dex(classes.dex).
     *
     * <p>If the loader class may exists in any dex, you must set this to true. Otherwise, you patch will
     * cause tinker runtime load failed. But this will cause a little increment on the size
     * of the patch file.
     *
     * <p>Since Android Gradle Plugin 3.3.0, there is no simply way to keep all loader classes in
     * the primary dex file if your application's min sdk version is 21 or above. In this situation, you
     * can turn the {@link #removeLoaderForAllDex} and {@link #allowLoaderInAnyDex} to true and
     * tolerate the loader classes to exists in any dex file.
     *
     * <p>default: false
     */
    boolean removeLoaderForAllDex

    /**
     * If sign the patch file with the android signConfig
     * default: true
     */
    boolean useSign

    /**
     * whether use tinker
     * default: true
     */
    boolean tinkerEnable

    /**
     * customDiffPath
     * default: null
     */
    String customPath

    /**
     * customDiffPathArgs
     * default: null
     */
    String customDiffPathArgs

    TinkerBuildConfigExtension buildConfig = new TinkerBuildConfigExtension(this.project)
    TinkerDexExtension dex = new TinkerDexExtension(this.project)
    TinkerLibExtension lib = new TinkerLibExtension()
    TinkerResourceExtension res = new TinkerResourceExtension()
    TinkerArkHotExtension arkHot = new TinkerArkHotExtension()
    TinkerPackageConfigExtension packageConfig = new TinkerPackageConfigExtension(this.project)
    TinkerSevenZipExtension sevenZip = new TinkerSevenZipExtension(this.project)

    public void buildConfig(Action<TinkerBuildConfigExtension> action){
        action.execute(buildConfig)
    }

    public void dex(Action<TinkerDexExtension> action){
        action.execute(dex)
    }

    public void lib(Action<TinkerLibExtension> action){
        action.execute(lib)
    }

    public void res(Action<TinkerResourceExtension> action){
        action.execute(res)
    }

    public void arkHot(Action<TinkerArkHotExtension> action){
        action.execute(arkHot)
    }

    public void packageConfig(Action<TinkerPackageConfigExtension> action){
        action.execute(packageConfig)
    }

    public void sevenZip(Action<TinkerSevenZipExtension> action){
        action.execute(sevenZip)
    }

    private Project project

    public TinkerPatchExtension(Project project) {
        this.project = project
        oldApk = ""
        outputFolder = ""
        newApk = ""
        ignoreWarning = false
        allowLoaderInAnyDex = false
        removeLoaderForAllDex = false
        useSign = true
        tinkerEnable = true
        customPath = null
        customDiffPathArgs = null
    }

    void checkParameter() {
        if (oldApk == null) {
            throw new GradleException("old apk is null, you must set the correct old apk value!")
        }
        File apk = new File(oldApk)
        if (!apk.exists()) {
            throw new GradleException("old apk ${oldApk} is not exist, you must set the correct old apk value!")
        }
    }

    @Override
    public String toString() {
        """| oldApk = ${oldApk}
           | outputFolder = ${outputFolder}
           | newApk = ${newApk}
           | ignoreWarning = ${ignoreWarning}
           | removeLoaderForAllDex = ${removeLoaderForAllDex}
           | tinkerEnable = ${tinkerEnable}
           | useSign = ${useSign}
        """.stripMargin()
    }
}