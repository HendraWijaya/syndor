package syndor.feedbot

import java.net.URL
import akka.actor.Actor
import akka.config.Supervision.Permanent
import com.ning.http.client.HttpResponseBodyPart
import com.ning.http.client.AsyncHttpClientConfig
import akka.dispatch.MessageDispatcher
import com.ning.http.client.AsyncHttpClient
import akka.dispatch.DefaultCompletableFuture
import com.ning.http.client.HttpResponseHeaders
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.Executors
import akka.event.EventHandler
import com.ning.http.client.Response
import com.ning.http.client.HttpResponseStatus
import akka.dispatch.Future
import com.ning.http.client.AsyncHandler
import java.util.concurrent.TimeUnit
import java.io.InputStream

object UrlFetcher {
  sealed trait Request
  case class FetchUrlRequest(url: String, requestHeaders: Map[String, String]) extends Request

  sealed trait Response
  case class FetchUrlResponse(status: Int, headers: Map[String, String], stream: InputStream) extends Response

  // This field is just used for debug/logging/testing
  private val httpInFlight = new AtomicInteger(0)

  // note: an AsyncHttpClient is a heavy object with a thread
  // and connection pool associated with it, it's supposed to
  // be shared among lots of requests, not per-http-request
  private def buildHttpClient(implicit dispatcher: MessageDispatcher) = {
    val executor = Executors.newCachedThreadPool()

    val builder = new AsyncHttpClientConfig.Builder()
    val config = builder.setMaximumConnectionsTotal(1000)
      .setMaximumConnectionsPerHost(20)
      .setExecutorService(executor)
      .setFollowRedirects(true)
      .setCompressionEnabled(true)
      .setAllowPoolingConnection(true)
      .setRequestTimeoutInMs(FeedBotConfig.fetcherTimeoutInMillis)
      .build
    new AsyncHttpClient(config)
  }

  /*
  * This method fetches a url asynchronously by returning a future
  */
  private def fetchUrl(asyncHttpClient: AsyncHttpClient, url: String, requestHeaders: Map[String, String]): Future[FetchUrlResponse] = {
    val requestBuilder = asyncHttpClient.prepareGet(url)
    for ((key, value) <- requestHeaders)
      requestBuilder.addHeader(key, value)

    // timeout the Akka future 50ms after we'd have timed out the request anyhow,
    // gives us 50ms to parse the response
    val f = new DefaultCompletableFuture[FetchUrlResponse](asyncHttpClient.getConfig.getRequestTimeoutInMs + 50,
      TimeUnit.MILLISECONDS)

    val httpHandler = new AsyncHandler[Unit]() {
      httpInFlight.incrementAndGet()

      val responseBuilder =
        new Response.ResponseBuilder()

      var finished = false

      // We can have onThrowable called because onCompleted
      // throws, and other complex situations, so to handle everything
      // we use this
      private def finish(body: => Unit): Unit = {
        if (!finished) {
          try {
            body
          } catch {
            case t: Throwable => {
              EventHandler.debug(this, t.getMessage)
              f.completeWithException(t)
              throw t // rethrow for benefit of AsyncHttpClient
            }
          } finally {
            finished = true
            httpInFlight.decrementAndGet()
            assert(f.isCompleted)
          }
        }
      }

      // This is called if any of our other methods throws an exception,
      // including onCompleted.
      def onThrowable(t: Throwable) {
        finish { throw t }
      }

      def onBodyPartReceived(bodyPart: HttpResponseBodyPart) = {
        responseBuilder.accumulate(bodyPart)
        AsyncHandler.STATE.CONTINUE
      }

      def onStatusReceived(responseStatus: HttpResponseStatus) = {
        responseBuilder.accumulate(responseStatus)
        AsyncHandler.STATE.CONTINUE
      }

      def onHeadersReceived(responseHeaders: HttpResponseHeaders) = {
        responseBuilder.accumulate(responseHeaders)
        AsyncHandler.STATE.CONTINUE
      }

      def onCompleted() = {
        import scala.collection.JavaConverters._

        finish {
          val response = responseBuilder.build()

          val headersJavaMap = response.getHeaders()

          var headers = Map.empty[String, String]
          for (header <- headersJavaMap.keySet.asScala) {
            // sometimes getJoinedValue() would be more correct.
            headers += (header -> headersJavaMap.getFirstValue(header))
          }

          f.completeWithResult(FetchUrlResponse(response.getStatusCode(), headers, response.getResponseBodyAsStream()))
        }
      }
    }
    requestBuilder.execute(httpHandler)
    f
  }
}

class UrlFetcher extends Actor {
  self.lifeCycle = Permanent

  import UrlFetcher._

  private var asyncHttpClient: AsyncHttpClient = _

  override def receive = {
    case request: Request => {
      val f = request match {
        case FetchUrlRequest(url, requestHeaders) =>
          UrlFetcher.fetchUrl(asyncHttpClient, url, requestHeaders)
      }

      self.channel.replyWith(f)
    }
  }

  override def preStart = {
    asyncHttpClient = UrlFetcher.buildHttpClient
  }
  
  override def postStop = {
    asyncHttpClient.close()
  }

  override def preRestart(err: Throwable) = {
    postStop()
  }
}

