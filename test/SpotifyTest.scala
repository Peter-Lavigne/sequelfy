import com.wrapper.spotify.SpotifyApi
import com.wrapper.spotify.model_objects.specification.Artist
import com.wrapper.spotify.requests.authorization.client_credentials.ClientCredentialsRequest
import com.wrapper.spotify.requests.data.artists.GetArtistRequest
import org.scalatest.FunSuite

class SpotifyTest extends FunSuite {

  test("API client credentials flow works") {

    // get client id and secret
    val clientId = sys.env("SPOTIFY_API_CLIENT_ID")
    val clientSecret = sys.env("SPOTIFY_API_CLIENT_SECRET")

    // create a spotifyApi object to use the API
    val spotifyApi: SpotifyApi =
      SpotifyApi.builder()
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

}