import java.net.URI

import com.wrapper.spotify.model_objects.specification.{Artist, Track}
import com.wrapper.spotify.requests.data.artists.GetArtistRequest
import com.wrapper.spotify.SpotifyApi
import org.scalatest.FunSuite
import spotify.SpotifyUtils
import org.scalatest._
import Matchers._

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

  test("Getting genres from a track") {
    val spotifyApi: SpotifyApi = SpotifyUtils.spotifyApiClientCredentials
    val track: Track = spotifyApi
      .getTrack("0rgfnZB9zSh7T4hVnqBtnX") // "Can't Hold Me Down" by Dive In
      .build()
      .execute()
    val genres: Seq[String] = SpotifyUtils.getGenresFromTrack(track)
    genres should contain allOf ("gauze pop", "metropopolis")
  }

  test("Getting frequencies") {
    val testData = Seq(Seq(1), Seq(2, 3), Seq(1, 2, 4))
    val frequencies = SpotifyUtils.getFrequencies(testData)
    frequencies should equal (Map(
      1 -> ((1.0 / 3.0) + (1.0 / 3.0 / 3.0)),
      2 -> ((1.0 / 2.0 / 3.0) + (1.0 / 3.0 / 3.0)),
      3 -> (1.0 / 2.0 / 3.0),
      4 -> (1.0 / 3.0 / 3.0)
    ))
  }

  test("Retrieving the The Sound Of Genre playlist") {
    // manually enter a code (use the previous test to generate a code)
    val code: String = sys.env("SPOTIFY_TESTING_AUTHORIZATION_CODE")

    // create a spotifyApi object to use the API
    val spotifyApi: SpotifyApi = SpotifyUtils.spotifyApiUserAuthentication(code)

    val playlist = SpotifyUtils.getSoundOfPlaylistForGenre("gauze pop", spotifyApi)

    assert(playlist.getName == "The Sound of Gauze Pop")
    assert(playlist.getOwner.getDisplayName == "The Sounds of Spotify")
  }

  test("Getting random tracks from playlist") {
    // manually enter a token
    val accessToken: String = sys.env("SPOTIFY_TESTING_ACCESS_TOKEN")

    // create a spotifyApi object to use the API
    val spotifyApi: SpotifyApi = SpotifyUtils.spotifyApiFromAccessToken(accessToken)

    val playlist = SpotifyUtils.getSoundOfPlaylistForGenre("gauze pop", spotifyApi)

    val tracks = SpotifyUtils.getRandomTracksFromPlaylist(playlist, 10)

    assert(tracks.size == 10)
    for (track <- tracks) {
      assert(tracks.count(_ == track) == 1)
    }
  }

}