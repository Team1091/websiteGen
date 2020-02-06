package com.team1091.websiteGen

import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import java.io.File
import java.io.IOException

// Uploads to the website
object Uploader {

    private val host: String = System.getenv("HOST")
    private val user: String = System.getenv("USER")
    private val pwd: String = System.getenv("PASS")

    @JvmStatic
    fun main() {

        build()

        val ftp = FTPClient()

        println("Connecting")
        ftp.connect(host)


        val reply = ftp.replyCode
        if (!FTPReply.isPositiveCompletion(reply)) {
            ftp.disconnect()
            throw Exception("Exception in connecting to FTP Server")
        }

        println("Logging in")
        if (!ftp.login(user, pwd)) {
            throw Exception("Log in fail")
        }

        ftp.setFileType(FTP.BINARY_FILE_TYPE)
        ftp.enterLocalPassiveMode()

        // clear old stuff
        FTPUtil.removeDirectory(ftp, "/domains/team1091.com/public_html", "")

        // Do upload of www directory
        ftp.changeWorkingDirectory("/domains/team1091.com/public_html")

        File("www").listFiles().forEach {
            FTPUtil.upload(it, ftp)
        }

        // Disconnect
        println("Logging out")
        ftp.logout()
        println("Disconnecting")
        ftp.disconnect()
    }
}

object FTPUtil {

    /**
     * Removes a non-empty directory by delete all its sub files and
     * sub directories recursively. And finally remove the directory.
     */
    @Throws(IOException::class)
    fun removeDirectory(ftpClient: FTPClient, parentDir: String, currentDir: String) {
        var dirToList = parentDir
        if (currentDir != "") {
            dirToList += "/$currentDir"
        }

        val subFiles = ftpClient.listFiles(dirToList)

        if (subFiles != null && subFiles.isNotEmpty()) {
            for (aFile in subFiles) {
                val currentFileName = aFile.name
                if (currentFileName == "." || currentFileName == "..") {
                    // skip parent directory and the directory itself
                    continue
                }
                var filePath = (parentDir + "/" + currentDir + "/"
                        + currentFileName)
                if (currentDir == "") {
                    filePath = "$parentDir/$currentFileName"
                }

                if (aFile.isDirectory) {
                    // remove the sub directory
                    removeDirectory(ftpClient, dirToList, currentFileName)
                } else {
                    // delete the file
                    val deleted = ftpClient.deleteFile(filePath)
                    if (deleted) {
                        println("DELETED the file: $filePath")
                    } else {
                        println("CANNOT delete the file: $filePath")
                    }
                }
            }

            // finally, remove the directory itself
            val removed = ftpClient.removeDirectory(dirToList)
            if (removed) {
                println("REMOVED the directory: $dirToList")
            } else {
                println("CANNOT remove the directory: $dirToList")
            }
        }
    }

    @Throws(IOException::class)
    fun upload(src: File, ftp: FTPClient) {
        if (src.isDirectory) {
            ftp.makeDirectory(src.name)
            ftp.changeWorkingDirectory(src.name)
            for (file in src.listFiles()) {
                upload(file, ftp)
            }
            ftp.changeToParentDirectory()
        } else {
            src.toURI().toURL().openStream().use {
                ftp.storeFile(src.name, it)
            }
        }
    }
}