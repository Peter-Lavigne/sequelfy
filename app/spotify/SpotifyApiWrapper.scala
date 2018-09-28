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

  // TODO generate and validate this state
  // https://developer.spotify.com/documentation/general/guides/authorization-guide/#authorization-code-flow
  val state = "x4xkmn9pu3j6ukrs8n"

  // create a spotifyApi object to use the API
  def createSpotifyApi(redirectUrl: String = "https://sequelfy.com/select-playlist/"): SpotifyApi =
    new SpotifyApi.Builder()
      .setClientId(clientId)
      .setClientSecret(clientSecret)
      .setRedirectUri(SpotifyHttpManager.makeUri(redirectUrl))
      .build

  /** Create a link to authorize a spotify account for a given scope for selecting a playlist
    *
    * @param scope the authorization scopes needed for the user, in the form (scope1,scope2,...).
    *              see https://developer.spotify.com/documentation/general/guides/scopes/
    */
  def authorizationCodeUriSelectPlaylist(scope: String): URI = createSpotifyApi()
    .authorizationCodeUri
    .state(state)
    .scope(scope)
    .build
    .execute

  /** Create a link to authorize a spotify account for a given scope for creating a playlist
    *
    * @param scope the authorization scopes needed for the user, in the form (scope1,scope2,...).
    *              see https://developer.spotify.com/documentation/general/guides/scopes/
    */
  def authorizationCodeUriCreatePlaylist(scope: String, playlistId: String): URI =
    createSpotifyApi(redirectUrl = "https://sequelfy.com/create-playlist/")
      .authorizationCodeUri
      .state(playlistId)
      .scope(scope)
      .build
      .execute

  /** Create a SpotifyApi object using the authorization code flow.
    * See https://developer.spotify.com/documentation/general/guides/authorization-guide/#authorization-code-flow
    *
    * @param code the authorization code received from following the link given by authorizationCodeUri
    */
  def spotifyApiUserAuthentication(code: String,
                                   redirectUrl: String = "https://sequelfy.com/select-playlist/"): SpotifyApi = {
    val spotifyApi: SpotifyApi = createSpotifyApi(redirectUrl = redirectUrl)
    // create a request for the access and refresh tokens
    val authorizationCodeRequest: AuthorizationCodeRequest = spotifyApi.authorizationCode(code).build

    val authorizationCodeCredentials = authorizationCodeRequest.execute
    // set access and refresh tokens to use continue using spotifyApiUserAuthentication with credentials
    spotifyApi.setAccessToken(authorizationCodeCredentials.getAccessToken)
    spotifyApi.setRefreshToken(authorizationCodeCredentials.getRefreshToken)
    spotifyApi
  }

}
