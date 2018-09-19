package controllers

import com.wrapper.spotify.SpotifyApi
import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, Controller}
import spotify.SpotifyUtils

/**
 * This controller handles requests to the home page.
 */
@Singleton
class MainController @Inject() extends Controller {

  // show the home page
  def index = Action {
    Ok(views.html.index(SpotifyUtils.authorizationCodeUri("playlist-read-private,playlist-modify-public").toString))
  }

  // show the "select playlist" page if the user has authenticated
  def selectPlaylist(codeOption: Option[String]): Action[AnyContent] = codeOption.map(code =>
    Action {
      val spotifyApi: SpotifyApi = SpotifyUtils.spotifyApiUserAuthentication(code)
      Ok(views.html.selectPlaylist(
        SpotifyUtils.getPlaylistsFromUser(spotifyApi),
        spotifyApi.getAccessToken
      ))
    }).getOrElse(index)

  def createPlaylist(accessToken: Option[String], playlistId: Option[String]): Action[AnyContent] =
    (accessToken, playlistId) match {
      case (Some(token), Some(id)) => Action { Ok(views.html.createPlaylist(SpotifyUtils.createPlaylist(token, id))) }
      case _ => index
    }

}
