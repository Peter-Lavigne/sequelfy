package controllers

import com.wrapper.spotify.SpotifyApi
import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, Controller}
import spotify.{SpotifyApiWrapper, SpotifyUtils}

/**
 * This controller handles requests to the home page.
 */
@Singleton
class MainController @Inject() extends Controller {

  // show the home page
  def index = Action {
    Ok(views.html.index(SpotifyApiWrapper.authUriSelectPlaylist))
  }

  // show the "select playlist" page if the user has authenticated
  def selectPlaylist(code: String): Action[AnyContent] = Action {
    val spotifyApi: SpotifyApi = SpotifyApiWrapper.spotifyApiSelectPlaylist(code)
    Ok(views.html.selectPlaylist(
      SpotifyUtils.getPlaylistsFromUser(spotifyApi)
    ))
  }

  // creates a playlist
  // the "state" parameter is the playlist id, but the Spotify API doesn't allow query parameters on redirect URLs
  def createPlaylist(code: String, state: String): Action[AnyContent] = Action {
    Ok(views.html.createPlaylist(SpotifyUtils.createPlaylistSequel(code, state)))
  }

}
