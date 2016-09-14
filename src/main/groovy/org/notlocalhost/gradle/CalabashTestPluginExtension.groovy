package org.notlocalhost.gradle

/**
 * Created by zan on 25/04/2014.
 */
class CalabashTestPluginExtension {
    String[] featuresPaths
    String[] pathsRequired
    String profile
    boolean verbose
    String[] tags
    boolean showProgress
    String[] formats
    Map<String, String> environmentVariables
    String preRun
    boolean ignoreExitValue
}
