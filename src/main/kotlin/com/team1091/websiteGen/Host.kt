package com.team1091.websiteGen

import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.options.MutableDataSet
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
import spark.kotlin.Http
import spark.kotlin.ignite
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDate
import java.util.*

fun main(args: Array<String>) {

    val port = 9000
    Builder.build()

    val http: Http = ignite()
    http.port(port)
    //http.staticFiles.location("/images")
    http.staticFiles.externalLocation("www")

    http.get("/rebuild") {
        Builder.build()
        "OK"
    }

    println("Go to http://localhost:$port/ in your browser")

}

data class Post(
        val date: LocalDate,
        val title: String,
        val url: String,
        val content: String,
        val outputDir: String
)

data class Page(
        val title: String,
        val url: String,
        val content: String,
        val outputDir: String
)

object Builder {


    fun build() {
        // Old layout for reference: https://web.archive.org/web/20180125183833/http://www.team1091.com/

        val outDir = File("www")
        FileUtils.deleteDirectory(outDir)
        outDir.mkdir()

        val cssFolder = File(outDir, "css")
        cssFolder.mkdir()

        val jsFolder = File(outDir, "js")
        jsFolder.mkdir()

        val blogFolder = File(outDir, "blog")
        blogFolder.mkdir()

        val imgFolder = File(outDir, "images")
        imgFolder.mkdir()

        // Load up data
        val posts = File("src/main/resources/blog/published").listFiles().flatMap { year ->
            year.listFiles().map {
                val header = it.readText().split("---")[1].lines().map { it.trim() }
                val title = header
                        .filter { it.startsWith("title:") }
                        .first()
                        .split(":")[1].trim()

                val date = header
                        .filter { it.startsWith("date:") }
                        .first()
                        .split(":")[1].trim()

                Post(
                        date = LocalDate.parse(date),
                        content = it.readText().split("---")[2],
                        title = title,
                        url = "/" + blogFolder.name + "/" + it.name.split('.')[0] + ".html",
                        outputDir = it.name.split('.')[0] + ".html"

                )
            }
        }

        val pages = File("src/main/resources/pages").listFiles().map {
            val header = it.readText().split("---")[1]
            val title = header.lines().map { it.trim() }
                    .filter { it.startsWith("title:") }
                    .first()
                    .split(":")[1].trim()

            Page(
                    content = it.readText().split("---")[2],
                    title = title,
                    url = "/" + it.name.split('.')[0] + ".html",
                    outputDir = it.name.split('.')[0] + ".html"
            )
        }

        var sidebarItems: MutableMap<String, List<Pair<String, String>>> = mutableMapOf()
        posts.sortedByDescending { it.date.year }
                .groupBy { it.date.year }
                .forEach { key: Int, value: List<Post> ->
                    sidebarItems[key.toString()] = value.map { Pair(it.title, it.url) }
                }

        var topMenuItems: List<Pair<String, String>> = pages
                .sortedBy { it.title }
                .map { Pair(it.title, it.url) }

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

        Files.copy(
                File("src/main/resources/favicon.ico").toPath(),
                File(outDir, "favicon.ico").toPath(),
                StandardCopyOption.REPLACE_EXISTING
        )

    }


    fun generatePage(content: (DIV) -> Unit,
                     title: String,
                     topMenuItems: List<Pair<String, String>>,
                     sidebarItems: Map<String, List<Pair<String, String>>>): String {
        // https://web.archive.org/web/20180125183833/http://www.team1091.com/
        // https://github.com/Kotlin/kotlinx.html
        return createHTMLDocument().html {
            head {
                title("${title} | Team 1091 | Oriole Assault")
                meta(content = "text/html", charset = "urt-8")
                meta(name = "viewport", content = "width=device-width, initial-scale=1")
                link(rel = "stylesheet", href = "http://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css")
                script(src = "https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/js/bootstrap.min.js") {}
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
                                    links.sortedBy { it.first }.forEach {
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


//    fun generateCss(): String {
//
//
//        val orange = 0xdd8500
//        val white = 0xffffff
//        val black = 0x0
//
//        // https://github.com/olegcherr/Aza-Kotlin-CSS
//        return Stylesheet {
//
//            footer {
//                position = "absolute";
//                bottom = 0;
//                width = 100.percent;
//                height = 60.px;
//                lineHeight = 60.px;
//                backgroundColor = "#f5f5f5";
//            }
//        }.render()
//    }

}

object Markdown {

    val options: MutableDataSet
    val parser: Parser
    val renderer: HtmlRenderer

    init {
        options = MutableDataSet()

        // uncomment to set optional extensions
        options.set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create(), StrikethroughExtension.create()));

        // uncomment to convert soft-breaks to hard breaks
        //options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");

        parser = Parser.builder(options).build()
        renderer = HtmlRenderer.builder(options).build()

    }

}
