package syndor.feedbot

import org.scalatest.BeforeAndAfterAll
import syndor.TestHttpServer
import org.scalatest.Suite

trait TestHttpServerSupport extends BeforeAndAfterAll { this: Suite =>
  protected var httpServer: TestHttpServer = null

  override def beforeAll() {
    httpServer = new TestHttpServer(Some(this.getClass))
    httpServer.start()
    super.beforeAll()
  }

  override def afterAll() {
    try {
      super.afterAll()
    } finally {
       httpServer.stop()
       httpServer = null
    }
  }
}