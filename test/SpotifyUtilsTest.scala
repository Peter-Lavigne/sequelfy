import java.net.URI

import com.wrapper.spotify.model_objects.specification.Track
import com.wrapper.spotify.SpotifyApi
import org.scalatest.FunSuite
import spotify.{SpotifyApiWrapper, SpotifyUtils}
import org.scalatest._
import Matchers._

class SpotifyUtilsTest extends FunSuite {

  test("API authorization code flow: generate an authorization code") {
    val uri: URI = SpotifyApiWrapper.authorizationCodeUriSelectPlaylist("user-read-email")
    assert(uri.toString.contains("spotify.com"))
    println("URI: " + uri.toString)
  }

  test("API authorization code flow: use a code to generate a access and refresh tokens") {
    // manually enter a code (use the previous test to generate a code)
    val code: String = sys.env("SPOTIFY_TESTING_AUTHORIZATION_CODE")

    // create a spotifyApi object to use the API
    val spotifyApi: SpotifyApi = SpotifyApiWrapper.spotifyApiSelectPlaylist(code)

    assert(spotifyApi.getCurrentUsersProfile.build().execute().getEmail contains "@gmail.com")
  }

  test("Getting genres from tracks") {
    val code: String = sys.env("SPOTIFY_TESTING_AUTHORIZATION_CODE")

    val spotifyApi: SpotifyApi = SpotifyApiWrapper.spotifyApiSelectPlaylist(code)
    val track1: Track = spotifyApi
      .getTrack("0rgfnZB9zSh7T4hVnqBtnX") // "Can't Hold Me Down" by Dive In
      .build()
      .execute()
    val track2: Track = spotifyApi
      .getTrack("2AHLyqdUKMvvtYZrvM7Uf6") // "Sugar" by PAWWS
      .build()
      .execute()

    val genres: Seq[Seq[String]] = SpotifyUtils.getGenresFromTracks(Seq(track1, track2), spotifyApi)
    genres should contain allOf (Seq("gauze pop", "metropopolis"), Seq("gauze pop", "vapor pop"))
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
    val spotifyApi: SpotifyApi = SpotifyApiWrapper.spotifyApiSelectPlaylist(code)

    val playlist = SpotifyUtils.getSoundOfPlaylistForGenre("gauze pop", spotifyApi)

    assert(playlist.map(_.getName).getOrElse("") == "The Sound of Gauze Pop")
    assert(playlist.map(_.getOwner.getDisplayName).getOrElse("") == "The Sounds of Spotify")
  }

  test("Getting random tracks from playlist") {
    // manually enter a code (use the previous test to generate a code)
    val code: String = sys.env("SPOTIFY_TESTING_AUTHORIZATION_CODE")

    // create a spotifyApi object to use the API
    val spotifyApi: SpotifyApi = SpotifyApiWrapper.spotifyApiSelectPlaylist(code)

    val playlist = SpotifyUtils.getSoundOfPlaylistForGenre("gauze pop", spotifyApi)
    assert(playlist.isDefined)

    val tracks = SpotifyUtils.getRandomTracksFromPlaylist(playlist.get, 10)

    assert(tracks.size == 10)
    for (track <- tracks) {
      assert(tracks.count(_ == track) == 1)
    }
  }

  test("Sequel titles") {
    assert(SpotifyUtils.sequelName("ABC", Seq("ABC", "DEF", "GHI")) == "ABC 2")
    assert(SpotifyUtils.sequelName("ABC", Seq("ABC", "ABC 2", "DEF", "GHI")) == "ABC 3")
    assert(SpotifyUtils.sequelName("ABC", Seq("ABC", "ABC 2", "ABC 3", "DEF", "GHI")) == "ABC 4")

    assert(SpotifyUtils.sequelName("ABC 2", Seq("ABC", "ABC 2", "ABC 3", "DEF", "GHI")) == "ABC 4")
  }

}