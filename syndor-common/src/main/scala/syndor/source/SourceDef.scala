package syndor.source

import syndor.model.Feed
import syndor.model.Source

trait SourceDef {
  protected val source: Source
  protected val feeds: List[Feed]

  def load() {
    Source.insert(source)

    for (feed <- feeds) {
      Feed.insert(feed)
    }
  }
  
  def makeFeed(title: String, url: String) = {
    Feed(
        sourceId = source.id,
        title = title,
        url = url)
  }
}