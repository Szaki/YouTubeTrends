import java.io.File
import java.time.LocalDate
import java.time.Period

val regions = listOf(
    "US", "CA", "MX", "DE", "FR", "GB", "RU", "IN", "JP", "KR"
)

val categories = mapOf(
    0 to "Film & Animation",
    1 to "Autos & Vehicles",
    2 to "Music",
    3 to "Pets & Animals",
    4 to "Sports",
    5 to "Short Movies",
    6 to "Travel & Events",
    7 to "Gaming",
    8 to "Videoblogging",
    9 to "People & Blogs",
    10 to "Comedy",
    11 to "Entertainment",
    12 to "News & Politics",
    13 to "Howto & Style",
    14 to "Education",
    15 to "Science & Technology",
    16 to "Movies",
    17 to "Anime/Animation",
    18 to "Action/Adventure",
    19 to "Classics",
    20 to "Comedy",
    21 to "Documentary",
    22 to "Drama",
    23 to "Family",
    24 to "Foreign",
    25 to "Horror",
    26 to "Sci-Fi/Fantasy",
    27 to "Thriller",
    28 to "Shorts",
    29 to "Shows",
    30 to "Trailers"
    )

class Video(region: String, date: String, val title: String, val channel: String, cat: Int, pub: String, var views: Int, var likes: Int, var dislikes: Int, var comments: Int, var comments_disabled: Boolean, var ratings_disabled: Boolean) {
    val dates = sortedMapOf<String, MutableSet<String>>()
    val regions = mutableMapOf<String, MutableSet<String>>()
    val category = categories[cat]
    val published = pub.substringBefore('T')

    init {
        storeDate(region, date)
    }

    fun update(region: String, date: String, views: Int, likes: String, dislikes: String, comments: String, comments_disabled: String, ratings_disabled: String) {
        storeDate(region, date)
        if (views > this.views) {
            this.views = views
            this.likes = likes.toInt()
            this.dislikes = dislikes.toInt()
            this.comments = comments.toInt()
            this.comments_disabled = comments_disabled.toBoolean()
            this.ratings_disabled = ratings_disabled.toBoolean()
        }
    }

    fun storeDate(region: String, date: String) {
        val (Y, D, M) = date.split('.')
        val d = "20$Y-$M-$D"
        if (dates[d]?.add(region) == null)
            dates[d] = mutableSetOf(region)
        if (regions[region]?.add(d) == null)
            regions[region] = mutableSetOf(d)
    }
}

fun String.parseCsv(): List<String> {
    if (this.isBlank() || this[0] == '\\')
        return listOf()
    val pcs = this.split(',')
    try {
        var dateIdx = 0
        for (i in pcs.indices)
            if (pcs[i].length == 24 && pcs[i][10] == 'T' && pcs[i][23] == 'Z') {
                dateIdx = i
                break
            }
        if (dateIdx == 0)
            return listOf()
        var thumbnailIdx = 0
        for (i in IntRange(dateIdx + 1, pcs.size))
            if ("ytimg" in pcs[i]) {
                thumbnailIdx = i
                break
            }
        if (thumbnailIdx == 0)
            return listOf()
        return mutableListOf(pcs[0], pcs[1], pcs.subList(2, dateIdx - 2).joinToString(",")).apply {
            addAll(pcs.subList(dateIdx - 2, dateIdx + 1))
            addAll(pcs.subList(thumbnailIdx - 4, thumbnailIdx))
            addAll(pcs.subList(thumbnailIdx + 1, thumbnailIdx + 3))
        }
    } catch (ex: Exception) {
        return listOf()
    }
}

fun processVideos(): List<Video> {
    val videos = mutableMapOf<String, Video>()
    regions.forEach { region ->
        File("res/${region}videos.csv").forEachLine { video ->
            val data = video.parseCsv()
            if (data.isNotEmpty())
                if (videos[data[0]]?.update(region, data[1], data[6].toInt(), data[7], data[8], data[9], data[10], data[11]) == null)
                    videos[data[0]] = Video(region, data[1], data[2], data[3], data[4].toInt(), data[5], data[6].toInt(),
                                                data[7].toInt(), data[8].toInt(), data[9].toInt(), data[10].toBoolean(), data[11].toBoolean())
        }
    }
    return videos.values.toList()
}

inline fun timer(func: () -> Unit) {
    val t1 = System.currentTimeMillis()
    func()
    println("Time: ${System.currentTimeMillis() - t1} ms")
}

fun main() {
    val videos = processVideos()
    println("-------------------------------------------------------------")
    println("Which videos were trending for longest in each region? What about all regions?")
    println("-------------------------------------------------------------")
    regions.forEach { region ->
        println("$region:")
        videos.groupBy { it.regions[region]?.size ?: 0 }.maxBy { it.key }?.value?.forEach {
            println("${it.title} by ${it.channel} | ${it.regions[region]?.size} days")
        }
    }
    println("-------------------------------------------------------------")
    println("All regions:")
    videos.groupBy { it.dates.count { date -> date.value.size == 10 } }.maxBy { it.key }?.value?.forEach {
        println("${it.title} by ${it.channel} | ${it.dates.count {date -> date.value.size == 10}} days")
    }
    println("-------------------------------------------------------------")
    println("Which videos made viewers leave comments the most?")
    println("-------------------------------------------------------------")
    videos.sortedByDescending { it.comments.toDouble() / it.views.toDouble() }.take(10).forEach {
        println("${it.title} by ${it.channel} | Commented: ${(it.comments * 100.0 / it.views)}%")
    }
    println("-------------------------------------------------------------")
    println("Are there any videos that appeared in all regions except one?")
    println("-------------------------------------------------------------")
    videos.filter { it.regions.keys.size == 9 }.forEach {
        println("${it.title} by ${it.channel} | Didn't trend in: ${(regions - it.regions.keys)[0]}")
    }
    println("-------------------------------------------------------------")
    println("Which region had the most trending videos that didn't appear in other regions?")
    println("-------------------------------------------------------------")
    println(videos.asSequence().filter { it.regions.keys.size == 1 }.groupBy { it.regions.keys.first() }.maxBy { it.value.size }?.key)
    println("-------------------------------------------------------------")
    println("What's the correlation between all caps titles and categories?")
    println("-------------------------------------------------------------")
    val allcaps = videos.filter { it.title.toUpperCase() == it.title }
    allcaps.groupingBy { it.category }.eachCount().forEach {
        println("${it.key}: ${it.value} videos, ${it.value * 100.0 / allcaps.size}%")
    }
    println("-------------------------------------------------------------")
    println("Which videos started trending after a while that they were uploaded?")
    println("-------------------------------------------------------------")
    videos.sortedByDescending { Period.between(LocalDate.parse(it.published), LocalDate.parse(it.dates.firstKey())).toTotalMonths() }.take(10).forEach {
        println("${it.title} by ${it.channel} | ${it.dates.firstKey()}, ${it.published} | ${Period.between(LocalDate.parse(it.published), LocalDate.parse(it.dates.firstKey())).toTotalMonths()} months")
    }
    println("-------------------------------------------------------------")
    println("Did viewers dislike videos more that disabled comments?")
    println("-------------------------------------------------------------")
    videos.asSequence().filter { it.dislikes + it.likes > 0 }.groupBy { it.comments_disabled }.forEach {
        println("Disabled: ${it.key} | ${it.value.asSequence().map {video -> video.dislikes*100.0 / (video.dislikes + video.likes)}.average()}% disliked")
    }
    println("-------------------------------------------------------------")
    println("What were the most popular categories in each region?")
    println("-------------------------------------------------------------")
    regions.forEach { region ->
        println("$region: ${videos.asSequence().filter {it.regions[region] != null}.groupBy {it.category}.maxBy { it.value.size }?.key}")
    }
    println("-------------------------------------------------------------")
    println("Which channels were trending the most in different regions?")
    println("-------------------------------------------------------------")
    regions.forEach { region ->
        println("$region: ${videos.asSequence().filter {it.regions[region] != null}.groupBy {it.channel}.maxBy { it.value.asSequence().map {video -> video.regions[region]!!.size}.sum() }?.key}")
    }
}
