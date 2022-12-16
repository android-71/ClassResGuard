package com.leos.superplugin.utils

import groovy.xml.XmlParser
import java.io.File
import java.util.regex.Pattern

/**
 * User: ljx
 * Date: 2022/3/22
 * Time: 10:31
 */

private val packagePattern = Pattern.compile("\\s*package\\s+(.*)")

//插入 import xx.xx.xx.R  import xx.xx.xx.BuildConfig    语句，
fun File.insertImportXxxIfAbsent(newPackage: String) {
    val text = readText()
    val importR = "import $newPackage.R"
    val importBuildConfig = "import $newPackage.BuildConfig"
    //如果使用引用了R类且没有导入，则需要导入
    val needImportR = text.findWord("R.") != -1 && text.findWord(importR) == -1
    //如果使用引用了BuildConfig类且没有导入，则需要导入
    val needImportBuildConfig = text.findWord("BuildConfig.") != -1 &&
            text.findWord(importBuildConfig) == -1
    if (!needImportR && !needImportBuildConfig) return
    val builder = StringBuilder()
    val matcher = packagePattern.matcher(text)
    if (matcher.find()) {
        val packageStatement = matcher.group()
        val packageIndex = text.indexOf(packageStatement)
        if (packageIndex > 0) {
            builder.append(text.substring(0, packageIndex))
        }
        builder.append(text.substring(packageIndex, packageStatement.length))
        builder.append("\n\n")
        if (needImportR) {
            //import xx.xx.xx.R
            builder.append(importR)
            if (name.endsWith(".java")) {
                builder.append(";")
            }
            builder.append("\n")
        }

        if (needImportBuildConfig) {
            //import xx.xx.xx.BuildConfig
            builder.append(importBuildConfig)
            if (name.endsWith(".java")) {
                builder.append(";")
            }
            builder.append("\n")
        }
        builder.append(text.substring(packageIndex + packageStatement.length))
    }
    writeText(builder.toString())
}

fun File.findPackage(): String? {
    val rootNode = XmlParser(false, false).parse(this)
    return rootNode.attribute("package")?.toString()
}