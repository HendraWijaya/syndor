package syndor.source

import syndor.model.Feed
import syndor.model.Source

object ReutersSource extends SourceDef {
  protected val source = Source(name = "Reuters", url = "http://www.reuters.com")
    
  protected val feeds =
    makeFeed(
        title = "All",
        url = "http://feeds.reuters.com/reuters/topNews") ::
    Nil
}