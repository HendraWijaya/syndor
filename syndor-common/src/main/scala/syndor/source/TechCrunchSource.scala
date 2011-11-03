package syndor.source

import syndor.model.Feed
import syndor.model.Source

object TechCrunchSource extends SourceDef {
  protected val source = Source(name = "TechCrunch", url = "http://www.techcrunch.com")
  
  protected val feeds =
    makeFeed(
        title = "All",
        url = "http://feeds.feedburner.com/TechCrunch") ::
    Nil
}