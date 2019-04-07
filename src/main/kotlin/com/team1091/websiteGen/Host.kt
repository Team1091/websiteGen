package com.team1091.websiteGen

import kotlinx.html.DIV
import kotlinx.html.a
import kotlinx.html.aside
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.dom.createHTMLDocument
import kotlinx.html.dom.serialize
import kotlinx.html.footer
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.h3
import kotlinx.html.head
import kotlinx.html.header
import kotlinx.html.html
import kotlinx.html.li
import kotlinx.html.link
import kotlinx.html.meta
import kotlinx.html.nav
import kotlinx.html.p
import kotlinx.html.script
import kotlinx.html.title
import kotlinx.html.ul
import kotlinx.html.unsafe
import org.apache.commons.io.FileUtils
import spark.Spark.get
import spark.Spark.port
import spark.Spark.staticFiles
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDate

// This is the main function, its the starting point to the whole application
fun main(args: Array<String>) {

    val portNo = 9000

    // This converts our markdown into webpages
    Builder.build()

    // This creates a small webserver using http://sparkjava.com/
    port(portNo)
    staticFiles.externalLocation("www")

    // we can hit a endpoint called rebuild to remake the site
    get("/rebuild") { req, resp ->
        Builder.build()
        "OK"
    }

    println("Go to http://localhost:$portNo/ in your browser")

}

/**
 * This takes in some markdown and generates a website from it
 */
object Builder {


    fun build() {
        // Old layout for reference: https://web.archive.org/web/20180125183833/http://www.team1091.com/

        // This deletes and recreates the web folder
        val outDir = File("www")
        FileUtils.deleteDirectory(outDir)
        outDir.mkdir()

        // Make some dirs for assets
        val cssFolder = File(outDir, "css")
        cssFolder.mkdir()

        val jsFolder = File(outDir, "js")
        jsFolder.mkdir()

        val blogFolder = File(outDir, "blog")
        blogFolder.mkdir()

        val imgFolder = File(outDir, "images")
        imgFolder.mkdir()

        val fileFolder = File(outDir, "files")
        fileFolder.mkdir()

        // Load up data
        val posts = File("src/main/resources/blog/published").listFiles().flatMap { year ->
            year.listFiles().map { post ->
                val header = post.readText().split("---")[1].lines().map { it.trim() }
                val title = header.asSequence()
                        .first { it.startsWith("title:") }
                        .split(":")[1].trim()

                val date = header.asSequence()
                        .first { it.startsWith("date:") }
                        .split(":")[1].trim()

                Post(
                        date = LocalDate.parse(date),
                        content = post.readText().split("---")[2],
                        title = title,
                        url = "/" + blogFolder.name + "/" + post.name.split('.')[0] + ".html",
                        outputDir = post.name.split('.')[0] + ".html"
                )
            }
        }

        val pages = File("src/main/resources/pages").listFiles().map { file ->
            val header = file.readText().split("---")[1]
            val title = header.lines().asSequence()
                    .map { it.trim() }
                    .first { it.startsWith("title:") }
                    .split(":")[1].trim()
            val pageName = file.name.split('.')[0]

            Page(
                    content = file.readText().split("---")[2],
                    title = title,
                    url = "/${pageName}.html",
                    outputDir = "${pageName}.html"
            )
        }

        val sidebarItems: MutableMap<String, List<Pair<String, String>>> = mutableMapOf()
        posts.asSequence()
                .sortedByDescending { it.date.year }
                .groupBy { it.date.year }
                .forEach { key: Int, value: List<Post> ->
                    sidebarItems[key.toString()] = value.sortedByDescending { it.date }.map { Pair(it.title, it.url) }
                }

        val topMenuItems: List<Pair<String, String>> = pages.asSequence()
                .sortedBy { it.title }
                .map { Pair(it.title, it.url) }
                .toList()

        // Generate Blog pages
        posts.forEach {
            File(blogFolder, it.outputDir).writeText(
                    generatePage(markdownToHtml(it.content), it.title, topMenuItems, sidebarItems)
            )
        }

        // Generate Main Pages
        pages.forEach {
            File(outDir, it.outputDir).writeText(
                    generatePage(markdownToHtml(it.content), it.title, topMenuItems, sidebarItems)
            )
        }


        // look into https://github.com/scireum/server-sass
        File(cssFolder, "main.css").writeText(
                File("src/main/resources/style/style.scss").readText()
        )

        File("src/main/resources/images").listFiles().forEach {
            Files.copy(
                    it.toPath(),
                    File(imgFolder, it.name).toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            )
        }

        File("src/main/resources/files/").listFiles().forEach {
            Files.copy(
                    it.toPath(),
                    File(fileFolder, it.name).toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            )
        }

        Files.copy(
                File("src/main/resources/favicon.ico").toPath(),
                File(outDir, "favicon.ico").toPath(),
                StandardCopyOption.REPLACE_EXISTING
        )

    }


    val bootstrapVersion = "4.3.1"
    fun generatePage(content: (DIV) -> Unit,
                     title: String,
                     topMenuItems: List<Pair<String, String>>,
                     sidebarItems: Map<String, List<Pair<String, String>>>): String {
        // https://web.archive.org/web/20180125183833/http://www.team1091.com/
        // https://github.com/Kotlin/kotlinx.html
        return createHTMLDocument().html {
            head {
                title("$title | Team 1091 | Oriole Assault")
                meta(content = "text/html", charset = "urt-8")
                meta(name = "viewport", content = "width=device-width, initial-scale=1")
                link(rel = "stylesheet", href = "http://maxcdn.bootstrapcdn.com/bootstrap/${bootstrapVersion}/css/bootstrap.min.css")
                script(src = "https://maxcdn.bootstrapcdn.com/bootstrap/${bootstrapVersion}/js/bootstrap.min.js") {}
                link(rel = "stylesheet", href = "/css/main.css")
            }
            body {

                header {
                    nav("navbar navbar-expand-md navbar-dark fixed-top bg-dark") {
                        a(href = "/", classes = "navbar-brand") { +"Home" }
                        div("collapse navbar-collapse") {
                            ul("navbar-nav mr-auto") {

                                topMenuItems.filter { it.first != "Home" }.forEach {
                                    li(classes = "nav-item") {
                                        a(classes = "nav-link", href = it.second) { +it.first }
                                    }
                                }
                            }
                        }
                    }
                }

                div("container") {
                    div("row jumbotron") {
                        header {
                            h1 { +"Team 1091" }
                            p { +"Hartford Union Highschool First Robotics Team" }
                        }

                    }

                    div("row") {
                        aside("col-sm-3") {
                            h2 { +"Blog Posts" }
                            sidebarItems.forEach { year, links ->
                                h3 { +year }
                                ul {
                                    links.forEach {
                                        li {
                                            a(it.second) {
                                                +it.first
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        div("col-sm-8") {
                            content(this)
                        }

                    }

                }
                footer("footer") {
                    div("container") {
                        p {
                            +"Team 1091 Oriole Assault"
                        }
                    }
                }
            }
        }
                .serialize(true)
                .replace( // This is to insert the calendar, for some reason the XML parser did not like the URL
                        "{{calendar}}",
                        "<iframe class=\"calendar\" width=\"800px\" height=\"600px\" src=\"https://calendar.google.com/calendar/embed?src=frcteam1091%40gmail.com&ctz=America%2FChicago\"/>")
    }


    // Convert a markdown file into html
    val markdownToHtml: (String) -> (DIV) -> Unit = {
        val html = Markdown.renderer.render(Markdown.parser.parse(it))

        val inner: (DIV) -> Unit = { it.div { unsafe { raw(html) } } }
        inner
    }

}

