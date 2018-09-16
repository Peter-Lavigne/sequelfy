import java.net.URI

import com.wrapper.spotify.model_objects.specification.Artist
import com.wrapper.spotify.requests.data.artists.GetArtistRequest
import com.wrapper.spotify.SpotifyApi

import org.scalatest.FunSuite
import spotify.SpotifyUtils

class SpotifyUtilsTest extends FunSuite {

  test("API client credentials flow works") {
    // create a spotifyApi object to use the API
    val spotifyApi: SpotifyApi = SpotifyUtils.spotifyApiClientCredentials

    // get the artist "Dive In" by their ID
    val getArtistRequest: GetArtistRequest = spotifyApi.getArtist("3MUHvC1BahPXrKbKZjkTwC").build()
    val artist: Artist = getArtistRequest.execute()

    // confirm we got the right artist
    assert(artist.getName == "Dive In")
  }

  test("API authorization code flow: generate an authorization code") {
    val uri: URI = SpotifyUtils.authorizationCodeUri("user-read-email")
    assert(uri.toString.contains("spotify.com"))
    println("URI: " + uri.toString)
  }

  test("API authorization code flow: use a code to generate a access and refresh tokens") {
    // manually enter a code (use the previous test to generate a code)
    val code: String = sys.env("SPOTIFY_TESTING_AUTHORIZATION_CODE")

    // create a spotifyApi object to use the API
    val spotifyApi: SpotifyApi = SpotifyUtils.spotifyApiUserAuthentication(code)

    assert(spotifyApi.getCurrentUsersProfile.build().execute().getEmail contains "@gmail.com")
  }

}