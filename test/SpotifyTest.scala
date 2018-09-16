import java.net.URI

import com.wrapper.spotify.exceptions.SpotifyWebApiException
import com.wrapper.spotify.model_objects.specification.Artist
import com.wrapper.spotify.requests.authorization.authorization_code.{AuthorizationCodeRequest, AuthorizationCodeUriRequest}
import com.wrapper.spotify.requests.authorization.client_credentials.ClientCredentialsRequest
import com.wrapper.spotify.requests.data.artists.GetArtistRequest
import com.wrapper.spotify.{SpotifyApi, SpotifyHttpManager}
import java.io.IOException
import org.scalatest.FunSuite

class SpotifyTest extends FunSuite {

  test("API client credentials flow works") {

    // get client id and secret
    val clientId = sys.env("SPOTIFY_API_CLIENT_ID")
    val clientSecret = sys.env("SPOTIFY_API_CLIENT_SECRET")

    // create a spotifyApi object to use the API
    val spotifyApi: SpotifyApi = SpotifyApi.builder()
      .setClientId(clientId)
      .setClientSecret(clientSecret)
      .build()

    // set access tokens to continue using spotifyApi
    val clientCredentialsRequest: ClientCredentialsRequest = spotifyApi.clientCredentials().build()
    spotifyApi.setAccessToken(clientCredentialsRequest.execute().getAccessToken)

    // get the artist "Dive In" by their ID
    val getArtistRequest: GetArtistRequest = spotifyApi.getArtist("3MUHvC1BahPXrKbKZjkTwC").build()
    val artist: Artist = getArtistRequest.execute()

    // confirm we got the right artist
    assert(artist.getName == "Dive In")
  }

  test("API authorization code flow: generate an authorization code") {
    // get client id and secret
    val clientId = sys.env("SPOTIFY_API_CLIENT_ID")
    val clientSecret = sys.env("SPOTIFY_API_CLIENT_SECRET")

    // the URL to redirect to along with a parameter containing the authorization code
    val redirectUri: URI = SpotifyHttpManager.makeUri("https://sequelfy.com")

    // create a spotifyApi object to use the API
    val spotifyApi: SpotifyApi = new SpotifyApi.Builder()
      .setClientId(clientId)
      .setClientSecret(clientSecret)
      .setRedirectUri(redirectUri)
      .build

    // TODO generate and validate this state
    // https://developer.spotify.com/documentation/general/guides/authorization-guide/#authorization-code-flow
    val state = "x4xkmn9pu3j6ukrs8n"

    // create and execute a request for an authorization code
    val authorizationCodeUriRequest: AuthorizationCodeUriRequest = spotifyApi
      .authorizationCodeUri
      .state(state)
      .scope("user-read-birthdate,user-read-email")
      .show_dialog(true) // this should be false in production
      .build
    val uri: URI = authorizationCodeUriRequest.execute

    assert(uri.toString.contains("spotify.com"))
    System.out.println("URI: " + uri.toString)
  }

  test("API authorization code flow: use a code to generate a access and refresh tokens") {
    // get client id and secret
    val clientId = sys.env("SPOTIFY_API_CLIENT_ID")
    val clientSecret = sys.env("SPOTIFY_API_CLIENT_SECRET")

    // the URL to redirect to along with a parameter containing the authorization code
    val redirectUri: URI = SpotifyHttpManager.makeUri("https://sequelfy.com")

    // manually enter a code (use the previous test to generate a code)
    val code: String = sys.env("SPOTIFY_TESTING_AUTHORIZATION_CODE")

    // create a spotifyApi object to use the API
    val spotifyApi: SpotifyApi = new SpotifyApi.Builder()
      .setClientId(clientId)
      .setClientSecret(clientSecret)
      .setRedirectUri(redirectUri)
      .build

    // create a request for the access and refresh tokens
    val authorizationCodeRequest: AuthorizationCodeRequest = spotifyApi.authorizationCode(code).build

    try {
      val authorizationCodeCredentials = authorizationCodeRequest.execute
      
      // set access and refresh tokens to use spotifyApi with credentials
      spotifyApi.setAccessToken(authorizationCodeCredentials.getAccessToken)
      spotifyApi.setRefreshToken(authorizationCodeCredentials.getRefreshToken)

      val expirationTime: Int = authorizationCodeCredentials.getExpiresIn
      assert(expirationTime == 3600) // the credentials should expire after one hour
    } catch {
      case e@(_: IOException | _: SpotifyWebApiException) => println("Error: " + e.getMessage)
    }
  }

}