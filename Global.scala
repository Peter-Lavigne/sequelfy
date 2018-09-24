import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import scala.concurrent.Future

object Global extends GlobalSettings {

  override def onError(request: RequestHeader, ex: Throwable) = {
    println(ex.toString)
    Future.successful(InternalServerError(
      views.html.errors.error()
    ))
  }

  override def onHandlerNotFound(request: RequestHeader) = {
    Future.successful(NotFound(
      views.html.errors.notFound()
    ))
  }

  override def onBadRequest(request: RequestHeader, error: String) = {
    println(error.toString)
    Future.successful(BadRequest(
      views.html.errors.badRequest()
    ))
  }

}
