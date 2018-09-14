package controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, Controller}

/**
 * This controller handles requests to the home page.
 */
@Singleton
class HomeController @Inject() extends Controller {

  /**
   * Actions (such as this one) are what handle requests. Controllers simply generate Actions.
   */
  def index = Action {
    Ok(views.html.index())
  }

}
