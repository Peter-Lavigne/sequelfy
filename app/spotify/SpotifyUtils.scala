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

  // number of songs to sample from the source playlist
  val SOURCE_PLAYLIST_SAMPLE_SIZE = 30

  // the minimum frequency that genres should appear in the sampled source playlists
  // raising this value increases the variety of genres in sequel playlists, but increases creation time
  val SOURCE_PLAYLIST_MIN_GENRE_FREQUENCY = 0.04

  // the id of the authenticated user, used by several API calls
  val userId: String = spotifyApi.getCurrentUsersProfile.build().execute().getId

  /**
    * Returns all the playlists (public and private) from a user.
    */
  def getPlaylistsFromUser: Array[PlaylistSimplified] = {
    spotifyApi
      .getListOfCurrentUsersPlaylists
      .limit(50) // TODO add pagination, current max is 50
      .build()
      .execute()
      .getItems
  }

  /**
    * Takes a random sample (non-repeating) of Tracks from a Playlist.
    *
    * @param playlist   the playlist to sample from
    * @param sampleSize the number of Tracks to take
    */
  def samplePlaylist(playlist: Playlist, sampleSize: Int): Seq[Track] = {
    Random.shuffle(playlist.getTracks.getItems.toSeq).take(sampleSize).map(_.getTrack)
  }

  /**
    * Calculates the frequency at which each element occurs in a 2D Seq. Elements in the subsequences are
    * given value relative to how many elements are in that sublist.
    *
    * For example, getFrequencies(Seq(Seq("A"), Seq("B", "C")) returns Map("A" -> 0.5, "B" -> 0.25, "C" -> 0.25
    *
    * @param elements the 2D Seq containing the elements to count
    */
  def getFrequencies[A](elements: Seq[Seq[A]]): Seq[(A, Double)] = {
    val frequencies = mutable.Map[A, Double]()
    for (inner <- elements; a <- inner) {
      val current = frequencies.getOrElse(a, 0.0)
      val addition = 1.0 / inner.size / elements.size
      frequencies(a) = current + addition
    }
    frequencies.toSeq
  }

  /**
    * Returns the Artists of given Tracks.
    */
  def getArtistsFromTracks(tracks: Seq[Track]): Seq[Artist] = {
    spotifyApi
      .getSeveralArtists(tracks.flatMap(_.getArtists).map(_.getId): _*)
      .build()
      .execute()
  }

  /**
    * Returns the genres of a given collection of Tracks, using the genre of their artists.
    *
    * NOTE: I could have used the genre of the album they're on, but I'm not confident that
    * there will be always be data for album genres.
    *
    * @param tracks       the Tracks to compute the genres of
    * @param minFrequency the minimum frequency that a genre must appear within the tracks to be included in the result.
    *                     this should be a number in the range [0.0, 1.0], where 0.0 returns all genres, and 1.0 only
    *                     returns a result if every track is a single genre.
    */
  def getGenresFromTracks(tracks: Seq[Track], minFrequency: Double): Seq[String] = {
    val artists = getArtistsFromTracks(tracks)
    val artistIdToGenres: Map[String, Seq[String]] =
      artists.map(artist => artist.getId -> artist.getGenres.toSeq).toMap
    val tracksGenres: Seq[Seq[String]] = tracks.flatMap(_.getArtists.map(artist => artistIdToGenres(artist.getId)))
    getFrequencies(tracksGenres).filter(_._2 >= minFrequency).map(_._1)
  }

  // TODO some artists don't work, such as Angels and Airwaves. look into why that's the case
  // TODO local files cause an error, but there's currently no way to check for local files in the API
  // TODO find a way to avoid passing SpotifyApi objects around everywhere. use implicit parameters?

  // returns Spotify's "The Sound of ..." playlist for a given genre
  def getSoundOfPlaylistForGenre(genre: String): Option[Playlist] = {
    val userId: String = spotifyApi.getCurrentUsersProfile.build().execute().getId
    val soundOfPlaylist = spotifyApi.searchPlaylists(s"The Sound of $genre").build().execute().getItems.headOption
    soundOfPlaylist.map(playlist =>
      spotifyApi.getPlaylist(userId, playlist.getId).build().execute()
    )
  }

  // appends a digit to the end of 'name' (starting with 2) that doesn't conflict with any other names
  def sequelName(name: String, playlists: Seq[String]): String = {
    val prefix = if (name matches """.*\s\d+""") name.substring(0, name.lastIndexOf(" ")) else name
    prefix + " " + Stream.from(2).filter(n => !playlists.contains(s"$prefix ${n.toString}")).head.toString
  }

  /** Create a playlist based on the given playlist. This method will use the genres of the artists within
    * the playlist to create a new one.
    *
    * @param sourcePlaylistId the id of the playlist to create a sequel to
    * @return                 the playlist id of the new playlist
    */
  def createPlaylistSequel(sourcePlaylistId: String): String = {
    val playlist: Playlist = spotifyApi.getPlaylist(
      userId,
      sourcePlaylistId
    ).build().execute()

    val sourcePlaylistTracks: Seq[Track] = samplePlaylist(
      playlist,
      SOURCE_PLAYLIST_SAMPLE_SIZE
    )

    val sourcePlaylistGenres: Seq[String] = getGenresFromTracks(
      sourcePlaylistTracks,
      SOURCE_PLAYLIST_MIN_GENRE_FREQUENCY
    )

    val soundOfPlaylists: Seq[Playlist] = sourcePlaylistGenres.flatMap(getSoundOfPlaylistForGenre)

    val API_LIMIT = 90 // API caps at 100 songs added at a time
    // TODO debug and fix empty playlists causing an error
    val songsPerPlaylist = if (soundOfPlaylists.nonEmpty) API_LIMIT / soundOfPlaylists.size else 0

    val newPlaylistTracks: Seq[Track] = soundOfPlaylists.flatMap(
      samplePlaylist(_, songsPerPlaylist)
    ).take(API_LIMIT)

    val sequelTitle = sequelName(playlist.getName, getPlaylistsFromUser.map(_.getName))
    val newPlaylist = spotifyApi.createPlaylist(userId, sequelTitle)
      .public_(true)
      .description(s"A sequel to the playlist ${playlist.getName} made using Sequelfy.com")
      .build()
      .execute()

    spotifyApi
      .addTracksToPlaylist(userId, newPlaylist.getId, newPlaylistTracks.map(_.getUri).toArray)
      .position(0)
      .build()
      .execute()

    newPlaylist.getId
  }

}
