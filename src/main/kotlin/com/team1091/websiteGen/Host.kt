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
import kotlinx.html.classes
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
import kotlinx.html.iframe
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
        val year: String,
        val header: String,
        val content: String,
        val title: String,
        val url: String,
        val outputDir: String)

object Builder {

    var sponsors = listOf(
            "Lions Club",
            "Ace"
    )

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
                val header = it.readText().split("---")[1]
                val title = header.lines().map { it.trim() }
                        .filter { it.startsWith("title:") }
                        .first()
                        .split(":")[1].trim()

                Post(
                        year = it.name.split('-')[0],
                        header = header,
                        content = it.readText().split("---")[2],
                        title = title,
                        url = "/" + blogFolder.name + "/" + it.name.split('.')[0] + ".html",
                        outputDir = it.name.split('.')[0] + ".html"

                )
            }
        }

        var sidebarItems: MutableMap<String, List<Pair<String, String>>> = mutableMapOf()
        posts.sortedBy { it.year }.reversed().groupBy { it.year }.forEach { key: String, value ->
            sidebarItems[key] = value.map { Pair(it.title, it.url) }
        }

        // Generate child blog pages
        posts.forEach {
            File(blogFolder, it.outputDir).writeText(
                    generatePage(markdownToHtml(it.content), sidebarItems)
            )
        }

        // Generate main page
        File(outDir, "index.html").writeText(
                generatePage(generateMain(posts), sidebarItems)
        )

        File(outDir, "calendar.html").writeText(
                generatePage(generateCalendarPageContent, sidebarItems)
        )

        File(outDir, "sponsor.html").writeText(
                generatePage(generateSponsorPage, sidebarItems)
        )


        // look into https://github.com/scireum/server-sass
        File(cssFolder, "main.css").writeText(
                File("src/main/resources/style/style.scss").readText()
        )

        File("src/main/resources/images").listFiles().forEach {
            Files.copy(
                    it.toPath(),
                    File(imgFolder, it.name).toPath(), StandardCopyOption.REPLACE_EXISTING)
        }

    }


    fun generatePage(content: (DIV) -> Unit, sidebarItems: Map<String, List<Pair<String, String>>>): String {
        // https://web.archive.org/web/20180125183833/http://www.team1091.com/
        // https://github.com/Kotlin/kotlinx.html
        return createHTMLDocument().html {
            head {
                title("Team 1091 | Oriole Assault")
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
                                li(classes = "nav-item") {
                                    a(classes = "nav-link", href = "/calendar.html") { +"Calendar" }
                                }
                                li(classes = "nav-item") {
                                    a(classes = "nav-link", href = "/sponsor.html") { +"Sponsors" }
                                }
                                li(classes = "nav-item") {
                                    a(classes = "nav-link", href = "#") {}
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
        }.serialize(true)
    }

    var generateMain: (posts: List<Post>) -> (DIV) -> Unit = {

        val html = Markdown.renderer.render(
                Markdown.parser.parse(
                        File("src/main/resources/home.md").readText()
                )
        )


        val inner: (DIV) -> Unit = { it.div { unsafe { raw(html) } } }
        inner
    }


    val markdownToHtml: (String) -> (DIV) -> Unit = {

        val document = Markdown.parser.parse(it)
        val html = Markdown.renderer.render(document)

        val inner: (DIV) -> Unit = { it.div { unsafe { raw(html) } } }
        inner

    }


    val generateCalendarPageContent: (DIV) -> Unit = {
        // TODO: https://support.google.com/calendar/answer/41207?hl=en
        it.div {
            it.iframe() {
                classes = setOf("calendar")
                src = "https://calendar.google.com/calendar/embed?src=frcteam1091%40gmail.com&ctz=America%2FChicago"
                width = "800px"
                height = "600px"
            }
        }
    }


    // Load sponsors
    val generateSponsorPage: (DIV) -> Unit = {
        it.ul("sponsors") {
            sponsors.forEach { li { +it } }
        }
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
