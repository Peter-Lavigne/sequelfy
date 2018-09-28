package spotify

import com.wrapper.spotify.model_objects.specification._
import com.wrapper.spotify.SpotifyApi

import scala.util.Random
import scala.collection.mutable

/**
  * Utility class for performing actions with a Spotify API. Some methods can only be used with SpotifyAPI objects
  * that have the "playlist-modify-public" scope enabled, and are marked as such.
  *
  * @param spotifyApi the API class retrieved from using SpotifyApiCreator's methods
  */
class SpotifyUtils(spotifyApi: SpotifyApi) {

  // TODO some artists don't work, such as Angels and Airwaves. look into why that's the case
  // TODO local files cause an error, but there's currently no way to check for local files in the API
  // TODO find a way to avoid passing SpotifyApi objects around everywhere. use implicit parameters?

  /** Returns all the playlists (public and private) from a user.
    *
    */
  def getPlaylistsFromUser: Array[PlaylistSimplified] = {
    spotifyApi
      .getListOfCurrentUsersPlaylists
      .limit(50) // TODO add pagination, the max is 50
      .build()
      .execute()
      .getItems
  }

  // retrieve the genres from a track
  def getGenresFromTracks(tracks: Seq[Track]): Seq[Seq[String]] = {
    val artists = spotifyApi
      .getSeveralArtists(tracks.flatMap(_.getArtists).map(_.getId): _*)
      .build()
      .execute()

    val idToGenres: Map[String, Seq[String]] = Map(artists.map(artist => artist.getId -> artist.getGenres.toSeq): _*)
    tracks.flatMap(_.getArtists.map(artist => idToGenres(artist.getId)))
  }

  // returns the frequency at which each element occurs. elements in the subsequences are given value relative to
  // how many elements are in that sublist.
  //
  // for example, getFrequencies(Seq(Seq("A"), Seq("B", "C")) returns Map("A" -> 0.5, "B" -> 0.25, "C" -> 0.25
  def getFrequencies[A](elements: Seq[Seq[A]]): Map[A, Double] = {
    val frequencies = mutable.Map[A, Double]()
    for (inner <- elements; a <- inner) {
      val current: Double = frequencies.getOrElse(a, 0.0)
      val addition: Double = 1.0 / inner.size / elements.size
      frequencies(a) = current + addition
    }
    frequencies.toMap
  }

  // returns Spotify's "The Sound of ..." playlist for a given genre
  def getSoundOfPlaylistForGenre(genre: String): Option[Playlist] = {
    val userId: String = spotifyApi.getCurrentUsersProfile.build().execute().getId
    val soundOfPlaylist = spotifyApi.searchPlaylists(s"The Sound of $genre").build().execute().getItems.headOption
    soundOfPlaylist.map(playlist =>
      spotifyApi.getPlaylist(userId, playlist.getId).build().execute()
    )
  }

  // returns n random tracks from a playlist (non-repeating)
  def getRandomTracksFromPlaylist(playlist: Playlist, n: Int): Seq[PlaylistTrack] = {
    Random.shuffle(playlist.getTracks.getItems.toSeq).take(n)
  }

  // appends a digit to the end of 'name' (starting with 2) that doesn't conflict with any other names
  def sequelName(name: String, playlists: Seq[String]): String = {
    val prefix = if (name matches """.*\s\d+""") name.substring(0, name.lastIndexOf(" ")) else name
    prefix + " " + Stream.from(2).filter(n => !playlists.contains(s"$prefix ${n.toString}")).head.toString
  }

  /** Create a playlist based on the given playlist. This method will use the genres of the artists within the playlist
    * to create a new one.
    *
    * @param playlistId the id of the playlist to base the new one off of
    * @return the playlist id of the new playlist
    */
  def createPlaylistSequel(playlistId: String): String = {
    val userId: String = spotifyApi.getCurrentUsersProfile.build().execute().getId

    val playlist = spotifyApi.getPlaylist(
      userId,
      playlistId
    ).build().execute()

    val SAMPLE_SIZE = 30
    val tracks: Seq[Track] = getRandomTracksFromPlaylist(playlist, SAMPLE_SIZE).map(_.getTrack)
    val genreCounts: Seq[Seq[String]] = getGenresFromTracks(tracks)

    val MIN_FREQUENCY = 0.04
    val genres: Seq[String] = getFrequencies(genreCounts).filter(_._2 >= MIN_FREQUENCY).keys.toSeq

    val soundOfPlaylists: Seq[Playlist] = genres.flatMap(getSoundOfPlaylistForGenre)

    val API_LIMIT = 90 // API caps at 100 songs added at a time
    // TODO debug and fix empty playlists causing an error
    val songsPerPlaylist = if (soundOfPlaylists.nonEmpty) API_LIMIT / soundOfPlaylists.size else 0

    val newPlaylistTracks: Seq[PlaylistTrack] = soundOfPlaylists.flatMap(
      getRandomTracksFromPlaylist(_, songsPerPlaylist)
    ).take(API_LIMIT)

    val sequelTitle = sequelName(playlist.getName, getPlaylistsFromUser.map(_.getName))
    val newPlaylist = spotifyApi.createPlaylist(userId, sequelTitle)
      .public_(true)
      .description(s"A sequel to the playlist ${playlist.getName} made using Sequelfy.com")
      .build()
      .execute()

    spotifyApi
      .addTracksToPlaylist(userId, newPlaylist.getId, newPlaylistTracks.map(_.getTrack.getUri).toArray)
      .position(0)
      .build()
      .execute()

    newPlaylist.getId
  }

}
