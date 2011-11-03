package syndor.source

import syndor.model.Feed
import syndor.model.Source

object TempoSource extends SourceDef {
  protected val source = Source(name = "Tempo", url = "http://www.tempointeraktif.com")

  protected val feeds =
    makeFeed(
        title = "Tempo Headline",
        url = "http://rss.tempointeraktif.com/index.xml") ::
    makeFeed(
        title = "Tempo Fokus",
        url = "http://rss.tempointeraktif.com/fokus.xml") :: 
    makeFeed(
        title = "Tempo Nasional",
        url = "http://rss.tempointeraktif.com/nasional.xml") ::
    makeFeed(
        title = "Tempo Internasional",
        url = "http://rss.tempointeraktif.com/internasional.xml") ::
    makeFeed(
        title = "Tempo Seni Hiburan",
        url = "http://rss.tempointeraktif.com/senihiburan.xml") ::
    makeFeed(
        title = "Tempo Teknologi",
        url = "http://rss.tempointeraktif.com/teknologi.xml") ::
    makeFeed(
        title = "Tempo Olahraga",
        url = "http://rss.tempointeraktif.com/olahraga.xml") ::
    Nil
}