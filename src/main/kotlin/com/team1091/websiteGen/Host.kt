package com.team1091.websiteGen

import io.javalin.Javalin
import io.javalin.http.staticfiles.Location
import kotlinx.html.*
import kotlinx.html.dom.createHTMLDocument
import kotlinx.html.dom.serialize
import org.apache.commons.io.FileUtils

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDate


// This is the main function, its the starting point to the whole application
fun main() {

    // This converts our markdown into webpages
    build()


    // This creates a small webserver using javalin
    val portNo = 9000
    val app = Javalin.create { config ->
        config.addStaticFiles("www", Location.EXTERNAL)
    }.start(portNo)

    // we can hit a endpoint called rebuild to remake the site
    app.get("/rebuild") { ctx ->
        build()
        ctx.result("OK")
    }


    println("Go to http://localhost:$portNo/ in your browser")

}

/**
 * This takes in some markdown and generates a website from it
 */

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

    val imgFolder = File(outDir, "images")
    imgFolder.mkdir()

    val fileFolder = File(outDir, "files")
    fileFolder.mkdir()

    // Load up data
    val posts = File("src/main/resources/blog/published").listFiles().flatMap { year ->
        year.listFiles().map { post ->
            val header = post.readText().split("---")[1].lines().map { it.trim() }
            val title = header
                    .first { it.startsWith("title:") }
                    .split(":")[1].trim()

            val date = header
                    .first { it.startsWith("date:") }
                    .split(":")[1].trim()

            val hide = header.any { it.startsWith("hide:") }

            Post(
                    date = LocalDate.parse(date),
                    content = post.readText().split("---")[2],
                    title = title,
                    url = title.replace(" ", "-"),
                    outputDir = post.name.split('.')[0] + ".html",
                    hidden = hide
            )
        }
    }

    val sidebarItems: Map<String, List<Pair<String, String>>> = posts.asSequence()
            .sortedByDescending { it.date.year }
            .groupBy { it.date.year }
            .map { (year: Int, posts: List<Post>) ->
                Pair(year.toString(), posts.sortedByDescending { it.date }.map { Pair(it.title, it.url) })
            }
            .toMap()


    val pages = File("src/main/resources/pages").listFiles().map { file ->
        val header = file.readText().split("---")[1].lines().map { it.trim() }
        val order = (header.firstOrNull { it.startsWith("order:") } ?: ":99")
                .split(":")[1].trim()

        val title = (header.firstOrNull { it.startsWith("title:") } ?: ":Untitled")
                .split(":")[1].trim()

        val pageName = file.name.split('.')[0]

        val hide = header.any { it.startsWith("hide:") }

        Page(
                content = file.readText().split("---")[2],
                title = title,
                url = if (pageName == "index") "/" else "$pageName.html",
                outputDir = "$pageName.html",
                order = Integer.parseInt(order),
                hidden = hide
        )
    } + Page(
            title = "Blog",
            url = "blog.html",
            content = posts.sortedByDescending { it.date }.map { it.content }.joinToString("\n\n"),
            outputDir = "blog.html",
            order = 3,
            sidebarItems = sidebarItems,
            hidden = false
    )


    val topMenuItems: List<Pair<String, String>> = pages
            .filterNot { it.hidden }
            .sortedBy { it.order }
            .map { Pair(it.title, it.url) }
            .toList()


    // Generate Main Pages
    pages.forEach {
        File(outDir, it.outputDir).writeText(
                generatePage(markdownToHtml(it.content), it.title, topMenuItems, it.sidebarItems)
        )
    }


    // look into https://github.com/scireum/server-sass
    File(cssFolder, "main.css").writeText(
            File("src/main/resources/style/style.css").readText()
    )

    File(jsFolder, "main.js").writeText(
            File("src/main/resources/script/script.js").readText()
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


private const val bootstrapVersion = "4.4.1"
private fun generatePage(
        content: (DIV) -> Unit,
        title: String,
        topMenuItems: List<Pair<String, String>>,
        sidebarItems: Map<String, List<Pair<String, String>>>?
): String {
    // https://web.archive.org/web/20180125183833/http://www.team1091.com/
    // https://github.com/Kotlin/kotlinx.html
    return createHTMLDocument().html {
        lang = "en"
        head {
            title("$title | Team 1091 | Oriole Assault")
            meta(content = "text/html", charset = "utf-8")
            meta(name = "viewport", content = "width=device-width, initial-scale=1")
            link(rel = "stylesheet", href = "http://maxcdn.bootstrapcdn.com/bootstrap/$bootstrapVersion/css/bootstrap.min.css")
            link(rel = "stylesheet", href = "/css/main.css")
            link(rel = "stylesheet", href = "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.12.1/css/all.min.css")

            script(src = "https://code.jquery.com/jquery-3.4.1.slim.min.js") {
                //                integrity = "sha384-J6qa4849blE2+poT4WnyKhv5vZF5SrPo0iEjwBvKU7imGFAV0wwj1yYfoRSJoZ+n"
//                crossorigin="anonymous"
            }
            script(src = "https://cdn.jsdelivr.net/npm/popper.js@1.16.0/dist/umd/popper.min.js") {
                //                integrity = "sha384-Q6E9RHvbIyZFJoft+2mJbHaEWldlvI9IOYy5n3zV9zzTtmI3UksdQRVvoxMfooAo"
//                crossorigin="anonymous"
            }

            script(src = "https://maxcdn.bootstrapcdn.com/bootstrap/$bootstrapVersion/js/bootstrap.min.js") {}
            script(src = "/js/main.js") {}
        }
        body {

            div("jumbotron jumbotron-fluid") {
                div("container") {
                    h1("display-4") { +"Team 1091" }
                    p("lead") { +"Hartford Union Highschool First Robotics Team" }
                }

            }

            header {
                nav("navbar navbar-expand-md navbar-dark bg-dark") {
                    //                    a(href = "/", classes = "navbar-brand") { +"Home" }
                    div("collapse navbar-collapse") {
                        ul("navbar-nav nav-fill w-100") {

                            topMenuItems.forEach {
                                li(classes = "nav-item") {
                                    a(classes = "nav-link", href = it.second) { +it.first }
                                }
                            }
                        }
                    }
                }
            }

            div("container") {


                div("row") {
                    if (sidebarItems != null) {
                        aside("col-3") {
                            h2 { +"Blog Posts" }
                            sidebarItems.forEach { (year, links) ->
                                h3 { +year }
                                ul {
                                    links.forEach {
                                        li {
                                            button {
                                                a(it.second) {
                                                    +it.first
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        div("col-9") {
                            content(this)
                        }
                    } else {
                        div("col-12") {
                            content(this)
                        }
                    }

                }

            }
            footer("footer bg-dark") {
                div("container") {
                    div(classes = "row") {
                        p(classes = "col-10") {
                            +"Team 1091 Oriole Assault"
                        }

                        div(classes = "col-2") {
                            a(href = "https://facebook.com/frcteam1091", classes = "fab fa-facebook-f") {}
                            a(href = "https://twitter.com/HUHS_Robotics", classes = "fab fa-twitter-square") {}
                            a(href = "https://instagram.com/frcteam1091", classes = "fab fa-instagram") {}
                        }
                    }
                }
            }
        }
    }
            .serialize(true)
            .replace( // This is to insert the calendar, for some reason the XML parser did not like the URL
                    "{{calendar}}",
                    "<iframe class=\"calendar\" width=\"100%\" height=\"600px\" src=\"https://calendar.google.com/calendar/embed?src=frcteam1091%40gmail.com&ctz=America%2FChicago\"/>")
}


// Convert a markdown file into html
val markdownToHtml: (String) -> (DIV) -> Unit = { markdown ->
    val html = Markdown.renderer.render(Markdown.parser.parse(markdown))

    val inner: (DIV) -> Unit = { it.div { unsafe { raw(html) } } }
    inner
}



