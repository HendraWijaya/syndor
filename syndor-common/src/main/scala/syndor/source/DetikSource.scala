package syndor.source

import syndor.model.Feed
import syndor.model.Source

object DetikSource extends SourceDef {
  protected val source = Source(name = "Detik", url = "http://www.detik.com")

  protected val feeds =
    makeFeed(
        title = "Detik Surabaya",
        url = "http://feeds.feedburner.com/detik/Zgvz?format=xml") ::
    makeFeed(
        title = "Detik",
        url = "http://rss.detik.com/") :: 
    Nil
    
  
}