package com.lagradost.cloudstream3.yourname

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class DramaCoolProvider : MainAPI() {
    override var mainUrl = "https://dramacool.com.tr"
    override var name = "DramaCool"
    override val hasMainPage = true
    override val hasQuickSearch = true
    override val hasChromecastSupport = true

    override val supportedTypes = setOf(
        TvType.AsianDrama,
        TvType.Movie
    )

    companion object {
        fun getType(t: String): TvType {
            return if (t.contains("Movie", true)) TvType.Movie else TvType.AsianDrama
        }
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(mainUrl).document
        val home = ArrayList<HomePageList>()

        document.select("div.movie-list").forEach { block ->
            val header = block.selectFirst("h2.cat-heading")?.text()?.trim() ?: return@forEach
            val items = block.select("article.movie-item").mapNotNull {
                it.toSearchResult()
            }

            if (items.isNotEmpty()) {
                home.add(HomePageList(header, items))
            }
        }

        return HomePageResponse(home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h3.movie-title a")?.text()?.trim() ?: return null
        val href = fixUrl(this.selectFirst("a")?.attr("href") ?: return null)
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src") ?: this.selectFirst("img")?.attr("src"))
        val type = getType(href)

        return newAnimeSearchResponse(title, href) {
            this.posterUrl = posterUrl
            addType(type)
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/search.html?keyword=${query.encodeURI()}").document
        return document.select("article.movie-item").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document

        val title = document.selectFirst("h1.entry-title")?.text()?.trim() ?: throw ErrorLoadingException("No title found")
        val poster = fixUrlNull(document.selectFirst("div.movie-thumb img")?.attr("src"))
        val description = document.selectFirst("div.movie-detail div.entry-content")?.text()?.trim()
        val type = getType(url)

        val episodes = document.select("div.episode-list li").mapNotNull {
            val episodeNum = it.selectFirst("span.episode-number")?.text()?.trim()?.filter { it.isDigit() }?.toIntOrNull()
            val episodeUrl = fixUrl(it.selectFirst("a")?.attr("href") ?: return@mapNotNull null)
            val episodeTitle = it.selectFirst("span.episode-title")?.text()?.trim() ?: "Episode $episodeNum"

            Episode(episodeUrl, episodeTitle, episodeNum)
        }.reversed()

        return newTvSeriesLoadResponse(title, url, type, episodes) {
            this.posterUrl = poster
            this.plot = description
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        
        document.select("div.movie-play iframe").forEach { iframe ->
            val url = fixUrl(iframe.attr("src") ?: return@forEach)
            if (url.isNotBlank()) {
                loadExtractor(url, data, subtitleCallback, callback)
            }
        }
        
        return true
    }
}
