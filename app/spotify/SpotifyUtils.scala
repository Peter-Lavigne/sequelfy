package spotify

import java.io.IOException
import java.net.URI

import com.wrapper.spotify.exceptions.SpotifyWebApiException
import com.wrapper.spotify.model_objects.specification.PlaylistSimplified
import com.wrapper.spotify.{SpotifyApi, SpotifyHttpManager}
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest
import com.wrapper.spotify.requests.authorization.client_credentials.ClientCredentialsRequest

object SpotifyUtils {

  // spotify client id and secret
  val clientId = sys.env("SPOTIFY_API_CLIENT_ID")
  val clientSecret = sys.env("SPOTIFY_API_CLIENT_SECRET")

  // the URL to redirect to along with a parameter containing the authorization code
  val redirectUri: URI = SpotifyHttpManager.makeUri("https://sequelfy.com/select-playlist/")

  // TODO generate and validate this state
  // https://developer.spotify.com/documentation/general/guides/authorization-guide/#authorization-code-flow
  val state = "x4xkmn9pu3j6ukrs8n"

  // create a spotifyApi object to use the API
  def spotifyApi: SpotifyApi = new SpotifyApi.Builder()
    .setClientId(clientId)
    .setClientSecret(clientSecret)
    .setRedirectUri(redirectUri)
    .build

  // create a SpotifyApi object using the client credentials workflow. no user authentication is required
  def spotifyApiClientCredentials: SpotifyApi = {
    val spotifyApiClientCredentials: SpotifyApi = spotifyApi
    val clientCredentialsRequest: ClientCredentialsRequest = spotifyApiClientCredentials.clientCredentials().build()
    // set the access token to continue using the spotifyApi object
    spotifyApiClientCredentials.setAccessToken(clientCredentialsRequest.execute().getAccessToken)
    spotifyApiClientCredentials
  }

  /** Create a link to authorize a spotify account for a given scope
    *
    * @param scope the authorization scopes needed for the user, in the form (scope1,scope2,...).
    *              see https://developer.spotify.com/documentation/general/guides/scopes/
    */
  def authorizationCodeUri(scope: String): URI = spotifyApi
    .authorizationCodeUri
    .state(state)
    .scope(scope)
    .build
    .execute

  /** Create a SpotifyApi object using the authorization code flow.
    * See https://developer.spotify.com/documentation/general/guides/authorization-guide/#authorization-code-flow
    *
    * @param code the authorization code received from following the link given by authorizationCodeUri
    */
  def spotifyApiUserAuthentication(code: String): SpotifyApi = {
    val spotifyApiUserAuthentication: SpotifyApi = spotifyApi
    // create a request for the access and refresh tokens
    val authorizationCodeRequest: AuthorizationCodeRequest = spotifyApiUserAuthentication.authorizationCode(code).build

    try {
      val authorizationCodeCredentials = authorizationCodeRequest.execute
      // set access and refresh tokens to use continue using spotifyApiUserAuthentication with credentials
      spotifyApiUserAuthentication.setAccessToken(authorizationCodeCredentials.getAccessToken)
      spotifyApiUserAuthentication.setRefreshToken(authorizationCodeCredentials.getRefreshToken)
      spotifyApiUserAuthentication
    } catch {
      case e@(_: IOException | _: SpotifyWebApiException) =>
        println("Error: " + e.getMessage)
        throw e
    }
  }

  def getPlaylistsFromUser(code: String): Array[PlaylistSimplified] = {
    spotifyApiUserAuthentication(code)
      .getListOfCurrentUsersPlaylists
      .limit(50) // TODO add pagination, the max is 50
      .build()
      .execute()
      .getItems
  }

}
