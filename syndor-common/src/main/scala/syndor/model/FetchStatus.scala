package syndor.model

case class FetchStatus(
    fetching: Boolean = false,
    fetchStart: Long = 0,
    fetchEnd: Long = 0,
    success: Boolean = true,
    message: Option[String] = None)