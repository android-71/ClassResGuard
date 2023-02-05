package com.leos.superplugin.tasks

import com.leos.superplugin.entension.ConfigExtension
import com.leos.superplugin.entension.javaDir
import com.leos.superplugin.entension.replaceWords
import com.leos.superplugin.entension.resDir
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.*
import javax.inject.Inject

/**
 * @author: Leo
 * @time: 2022/11/17
 * @desc:
 */
open class RenameResGuardTask @Inject constructor(
    private val configExtension: ConfigExtension,
) : DefaultTask() {

    init {
        group = "guard"
    }

    private val random by lazy { Random() }

    @TaskAction
    fun execute() {
        handleResClass(project)
    }

    private fun handleResClass(project: Project) {
        val pathArray = configExtension.changeResDir
            ?: throw IllegalArgumentException("The changeXmlPkg has not been configured yet. Please configure the changeXmlPkg before running the task")
        pathArray.forEach {
            val resDir = project.resDir(it)
            renameFile(it, resDir)
        }
    }

    private fun renameFile(path: String, file: File) {
        val listFiles = file.listFiles()
        if (file.exists()) {
            listFiles?.forEach {
                val xmlPrefixNameArray = configExtension.resPrefixName
                if (xmlPrefixNameArray.isEmpty()) {
                    throw IllegalArgumentException("The xmlPrefixName has not been configured yet. Please configure the xmlPrefixName before running the task")
                }
                val xmlPrefixName = if (xmlPrefixNameArray.size == 1) {
                    xmlPrefixNameArray[0]
                } else {
                    xmlPrefixNameArray[random.nextInt(xmlPrefixNameArray.size)]
                }
                val newFileName = "${xmlPrefixName.lowercase()}_${it.name}"
                it.renameTo(File("${file.absolutePath}/${newFileName}"))
                if (path.startsWith("mipmap") || path.startsWith("drawable") || path.startsWith("layout")
                    || name.startsWith("navigation")
                ) {
                    val oldName = it.name.substring(0, it.name.indexOf("."))
                    val newName = newFileName.substring(0, newFileName.indexOf("."))
                    val resFileList = project.resDir().listFiles { _, name ->
                        name.startsWith("layout") || name.startsWith("navigation")
                                || name.startsWith("drawable")
                    }?.toMutableList() ?: return
                    project.files(resFileList).asFileTree.forEach { xmlFile ->
                        replaceXmlText(oldName, newName, xmlFile)
                    }
                    obfuscateAllClass(project.javaDir(), oldName, newName)
                }
            }
        }
    }

    private fun obfuscateAllClass(file: File, oldName: String, newName: String) {
        file.listFiles()?.forEach {
            if (it.isDirectory) {
                obfuscateAllClass(it, oldName, newName)
            } else {
                replaceClassText(it, oldName, newName)
            }
        }
    }

    private fun replaceClassText(file: File, oldName: String, newName: String) {
        val sb = StringBuilder()
        file.readLines().forEach {
            if (it.contains("R.layout")) {
                sb.append(it.replaceWords("R.layout.$oldName", "R.layout.$newName")).append("\n")
            } else if (it.contains("R.mipmap")) {
                sb.append(it.replaceWords("R.mipmap.$oldName", "R.mipmap.$newName")).append("\n")
            } else if (it.contains("R.drawable")) {
                sb.append(it.replaceWords("R.drawable.$oldName", "R.drawable.$newName")).append("\n")
            } else if (it.contains("R.navigation")) {
                sb.append(it.replaceWords("R.navigation.$oldName", "R.navigation.$newName")).append("\n")
            } else {
                sb.append(it).append("\n")
            }
        }
        file.writeText(sb.toString())
    }

    private fun replaceXmlText(oldName: String, newName: String, file: File) {
        val originalText = file.readText()
        val newText = originalText.replaceWords(oldName, newName)
        file.writeText(newText)
    }
}