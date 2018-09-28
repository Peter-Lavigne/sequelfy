package spotify

import java.net.URI

import com.wrapper.spotify.{SpotifyApi, SpotifyHttpManager}
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest

/**
  * A wrapper around the Spotify API https://github.com/thelinmichael/spotify-web-api-java
  *
  * This object simplifies the API to be more Scala-friendly (removing some of the builder patterns).
  * Since most API calls need a logged-in user, all SpotifyApi objects use Spotify's authorization code flow.
  * See https://developer.spotify.com/documentation/general/guides/authorization-guide/#authorization-code-flow
  */
object SpotifyApiWrapper {

  // spotify client id and secret
  val clientId = sys.env("SPOTIFY_API_CLIENT_ID")
  val clientSecret = sys.env("SPOTIFY_API_CLIENT_SECRET")

  // redirect urls
  val SELECT_PLAYLIST_REDIRECT_URL = "https://sequelfy.com/select-playlist/"
  val CREATE_PLAYLIST_REDIRECT_URL = "https://sequelfy.com/create-playlist/"

  // authorization scopes. see https://developer.spotify.com/documentation/general/guides/scopes/
  val SELECT_PLAYLIST_SCOPES = "playlist-read-private"
  val CREATE_PLAYLIST_SCOPES = "playlist-read-private,playlist-modify-public"

  /**
    * Create a basic spotifyApi object to use the API. User authorizations must be added separately.
    *
    * @param redirectUrl the URL that this object will return to (if creating an authentication URI)
    *                    or authenticate against (if authenticating a code)
    */
  // create a spotifyApi object to use the API
  private def createSpotifyApi(redirectUrl: String): SpotifyApi =
    new SpotifyApi.Builder()
      .setClientId(clientId)
      .setClientSecret(clientSecret)
      .setRedirectUri(SpotifyHttpManager.makeUri(redirectUrl))
      .build

  /**
    * Create a link that will authorize a spotify account.
    *
    * @param redirectUrl the URL to redirect the user to after authenticating
    * @param scope       Spotify scopes to authorize the user for.
    *                    See https://developer.spotify.com/documentation/general/guides/scopes/
    * @param state       the state to pass to the redirect url, this information will be available for the
    *                    next request to use
    */
  private def authUri(redirectUrl: String, scope: String, state: String = ""): URI =
    createSpotifyApi(redirectUrl)
      .authorizationCodeUri
      .state(state)
      .scope(scope)
      .build
      .execute

  /**
    * Create a link that will authorize a spotify account for selecting a playlist.
    */
  def authUriSelectPlaylist: URI = authUri(SELECT_PLAYLIST_REDIRECT_URL, SELECT_PLAYLIST_SCOPES)

  /**
    * Create a link that will authorize a spotify account for creating a playlist.
    *
    * @param playlistId the Spotify id for the playlist to create a sequel for
    */
  def authUriCreatePlaylist(playlistId: String): URI =
    authUri(CREATE_PLAYLIST_REDIRECT_URL, CREATE_PLAYLIST_SCOPES,  playlistId)

  /**
    * Create a SpotifyApi object using the authorization code flow.
    * See https://developer.spotify.com/documentation/general/guides/authorization-guide/#authorization-code-flow
    *
    * @param code        the authorization code returned from following an authorization URI
    * @param redirectUrl the redirect url that was linked to by Spotify along with the code
    */
  private def spotifyApiUserAuthentication(code: String, redirectUrl: String): SpotifyApi = {
    val spotifyApi: SpotifyApi = createSpotifyApi(redirectUrl = redirectUrl)
    // create a request for the access and refresh tokens
    val authorizationCodeRequest: AuthorizationCodeRequest = spotifyApi.authorizationCode(code).build

    val authorizationCodeCredentials = authorizationCodeRequest.execute
    // set access and refresh tokens to use continue using spotifyApiUserAuthentication with credentials
    spotifyApi.setAccessToken(authorizationCodeCredentials.getAccessToken)
    spotifyApi.setRefreshToken(authorizationCodeCredentials.getRefreshToken)

    spotifyApi
  }

  /**
    * Create a SpotifyApi object for selecting a playlist.
    * The user must have authenticated and been redirected to the select playlist page.
    *
    * @param code the authorization code returned from following an authorization URI
    */
  def spotifyApiSelectPlaylist(code: String): SpotifyApi = {
    spotifyApiUserAuthentication(code, SELECT_PLAYLIST_REDIRECT_URL)
  }

  /**
    * Create a SpotifyApi object for creating a playlist.
    * The user must have authenticated and been redirected to the create playlist page.
    *
    * @param code the authorization code returned from following an authorization URI
    */
  def spotifyApiCreatePlaylist(code: String): SpotifyApi = {
    spotifyApiUserAuthentication(code, CREATE_PLAYLIST_REDIRECT_URL)
  }

}
