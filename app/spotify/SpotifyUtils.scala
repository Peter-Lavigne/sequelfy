package spotify

import java.io.IOException
import java.net.URI

import com.wrapper.spotify.exceptions.SpotifyWebApiException
import com.wrapper.spotify.model_objects.specification._
import com.wrapper.spotify.{SpotifyApi, SpotifyHttpManager}
import com.wrapper.spotify.requests.authorization.authorization_code.{AuthorizationCodeRefreshRequest, AuthorizationCodeRequest}
import com.wrapper.spotify.requests.authorization.client_credentials.ClientCredentialsRequest

import scala.util.Random
import scala.collection.mutable

object SpotifyUtils {

  // TODO some artists don't work, such as Angels and Airwaves. look into why that's the case
  // TODO local files cause an error, but there's currently no way to check for local files in the API

  // spotify client id and secret
  val clientId = sys.env("SPOTIFY_API_CLIENT_ID")
  val clientSecret = sys.env("SPOTIFY_API_CLIENT_SECRET")

  // the URL to redirect to along with a parameter containing the authorization code
  val redirectUri: URI = SpotifyHttpManager.makeUri("https://sequelfy.com/select-playlist/")

  // TODO generate and validate this state
  // https://developer.spotify.com/documentation/general/guides/authorization-guide/#authorization-code-flow
  val state = "x4xkmn9pu3j6ukrs8n"

  // create a spotifyApi object to use the API
  def createSpotifyApi: SpotifyApi = new SpotifyApi.Builder()
    .setClientId(clientId)
    .setClientSecret(clientSecret)
    .setRedirectUri(redirectUri)
    .build

  // create a SpotifyApi object using the client credentials workflow. no user authentication is required
  def spotifyApiClientCredentials: SpotifyApi = {
    val spotifyApi: SpotifyApi = createSpotifyApi
    val clientCredentialsRequest: ClientCredentialsRequest = spotifyApi.clientCredentials().build()
    // set the access token to continue using the spotifyApi object
    spotifyApi.setAccessToken(clientCredentialsRequest.execute().getAccessToken)
    spotifyApi
  }

  /** Create a link to authorize a spotify account for a given scope
    *
    * @param scope the authorization scopes needed for the user, in the form (scope1,scope2,...).
    *              see https://developer.spotify.com/documentation/general/guides/scopes/
    */
  def authorizationCodeUri(scope: String): URI = createSpotifyApi
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
    val spotifyApi: SpotifyApi = createSpotifyApi
    // create a request for the access and refresh tokens
    val authorizationCodeRequest: AuthorizationCodeRequest = spotifyApi.authorizationCode(code).build

    val authorizationCodeCredentials = authorizationCodeRequest.execute
    // set access and refresh tokens to use continue using spotifyApiUserAuthentication with credentials
    spotifyApi.setAccessToken(authorizationCodeCredentials.getAccessToken)
    spotifyApi.setRefreshToken(authorizationCodeCredentials.getRefreshToken)
    spotifyApi
  }

  /** Returns all the playlists (public and private) from a user.
    *
    * @param spotifyApiUserAuth a SpotifyApi object obtained from calling spotifyApiUserAuthentication
    */
  def getPlaylistsFromUser(spotifyApiUserAuth: SpotifyApi): Array[PlaylistSimplified] = {
    spotifyApiUserAuth
      .getListOfCurrentUsersPlaylists
      .limit(50) // TODO add pagination, the max is 50
      .build()
      .execute()
      .getItems
  }

  /** Creates a SpotifyApi object from a refresh token.
    *
    * @param refreshToken the refresh token given by an authenticated SpotifyApi object
    */
  def spotifyApiFromRefreshToken(refreshToken: String): SpotifyApi = {
    val spotifyApi: SpotifyApi = createSpotifyApi
    spotifyApi.setRefreshToken(refreshToken)

    val authorizationCodeRefreshRequest: AuthorizationCodeRefreshRequest  = spotifyApi
      .authorizationCodeRefresh()
      .build()

    try {
      val authorizationCodeCredentials = authorizationCodeRefreshRequest.execute
      // set access and refresh tokens to use continue using spotifyApiUserAuthentication with credentials
      spotifyApi.setAccessToken(authorizationCodeCredentials.getAccessToken)
      spotifyApi.setRefreshToken(authorizationCodeCredentials.getRefreshToken)
      spotifyApi
    } catch {
      case e@(_: IOException | _: SpotifyWebApiException) =>
        println("Error: " + e.getMessage)
        throw e
    }
  }

  /** Creates a SpotifyApi object from an access token.
    *
    * @param accessToken the access token given by an authenticated SpotifyApi object
    */
  def spotifyApiFromAccessToken(accessToken: String): SpotifyApi = {
    val spotifyApi: SpotifyApi = createSpotifyApi
    spotifyApi.setAccessToken(accessToken)
    spotifyApi
  }

  // retrieve the genres from a track
  def getGenresFromTracks(tracks: Seq[Track]): Seq[Seq[String]] = {
    val artists = spotifyApiClientCredentials
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
  def getSoundOfPlaylistForGenre(genre: String, spotifyApi: SpotifyApi): Option[Playlist] = {
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
    * @param accessToken the token used to get a new access token.
    *                     see https://developer.spotify.com/documentation/general/guides/authorization-guide/#authorization-code-flow
    * @param playlistId the id of the playlist to base the new one off of
    * @return the playlist id of the new playlist
    */
  def createPlaylistSequel(accessToken: String, playlistId: String): String = {
    val spotifyApi: SpotifyApi = spotifyApiFromAccessToken(accessToken)
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

    val soundOfPlaylists: Seq[Playlist] = genres.flatMap(getSoundOfPlaylistForGenre(_, spotifyApi))

    val API_LIMIT = 90 // API caps at 100 songs added at a time
    val songsPerPlaylist = API_LIMIT / soundOfPlaylists.size

    val newPlaylistTracks: Seq[PlaylistTrack] = soundOfPlaylists.flatMap(
      getRandomTracksFromPlaylist(_, songsPerPlaylist)
    ).take(API_LIMIT)

    val sequelTitle = sequelName(playlist.getName, getPlaylistsFromUser(spotifyApi).map(_.getName))
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
