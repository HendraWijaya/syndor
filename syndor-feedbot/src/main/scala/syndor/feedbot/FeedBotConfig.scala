package syndor.feedbot

import syndor.EnvironmentSupport

object FeedBotConfig extends EnvironmentSupport {
  var fetcherTimeoutInMillis: Int = _
  
  def dev() {
    fetcherTimeoutInMillis = 20000
  }
  
  def test() {
    fetcherTimeoutInMillis = 1000
  }
  
  def prod() {
  }
}