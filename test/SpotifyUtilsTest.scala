import java.net.URI

import org.scalatest.FunSuite
import spotify.SpotifyApiCreator
import org.scalatest._
import Matchers._

class SpotifyUtilsTest extends FunSuite {

  test("API authorization code flow: generate an authorization code") {
    val uri: URI = SpotifyApiCreator.authUriSelectPlaylist
    assert(uri.toString.contains("spotify.com"))
    println("URI: " + uri.toString)
  }

//  test("Getting frequencies") {
//    val testData = Seq(Seq(1), Seq(2, 3), Seq(1, 2, 4))
//    val frequencies = SpotifyUtils.getFrequencies(testData)
//    frequencies should equal (Map(
//      1 -> ((1.0 / 3.0) + (1.0 / 3.0 / 3.0)),
//      2 -> ((1.0 / 2.0 / 3.0) + (1.0 / 3.0 / 3.0)),
//      3 -> (1.0 / 2.0 / 3.0),
//      4 -> (1.0 / 3.0 / 3.0)
//    ))
//  }
//
//  test("Sequel titles") {
//    assert(SpotifyUtils.sequelName("ABC", Seq("ABC", "DEF", "GHI")) == "ABC 2")
//    assert(SpotifyUtils.sequelName("ABC", Seq("ABC", "ABC 2", "DEF", "GHI")) == "ABC 3")
//    assert(SpotifyUtils.sequelName("ABC", Seq("ABC", "ABC 2", "ABC 3", "DEF", "GHI")) == "ABC 4")
//
//    assert(SpotifyUtils.sequelName("ABC 2", Seq("ABC", "ABC 2", "ABC 3", "DEF", "GHI")) == "ABC 4")
//  }

}