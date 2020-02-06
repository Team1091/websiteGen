package com.team1091.websiteGen

import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import java.util.*


object Markdown {
    val parser: Parser
    val renderer: HtmlRenderer

    init {
        val options = MutableDataSet()
                .set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create(), StrikethroughExtension.create()))
                .toImmutable()

        // uncomment to convert soft-breaks to hard breaks
        //options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");

        parser = Parser.builder(options).build()
        renderer = HtmlRenderer.builder(options).build()

    }

}