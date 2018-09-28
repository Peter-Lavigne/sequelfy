package controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, Controller}
import spotify.{SpotifyApiCreator, SpotifyUtils}

/**
 * This controller handles requests to the home page.
 */
@Singleton
class MainController @Inject() extends Controller {

  // show the home page
  def index = Action {
    Ok(views.html.index(SpotifyApiCreator.authUriSelectPlaylist))
  }

  // show the "select playlist" page if the user has authenticated
  def selectPlaylist(code: String): Action[AnyContent] = Action {
    Ok(views.html.selectPlaylist(
      new SpotifyUtils(SpotifyApiCreator.spotifyApiSelectPlaylist(code)).getPlaylistsFromUser
    ))
  }

  // creates a playlist
  // the "state" parameter is the playlist id, but the Spotify API doesn't allow query parameters on redirect URLs
  def createPlaylist(code: String, state: String): Action[AnyContent] = Action {
    Ok(views.html.createPlaylist(
      new SpotifyUtils(SpotifyApiCreator.spotifyApiCreatePlaylist(code)).createPlaylistSequel(state)
    ))
  }

}
