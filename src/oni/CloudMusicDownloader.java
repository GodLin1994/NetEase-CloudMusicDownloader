package oni;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CloudMusicDownloader {

    /**
     *
     * 实例：
     * 专辑：http://music.163.com/#/album?id=2532058
     * 歌单：http://music.163.com/#/playlist?id=141663003
     * 歌单：http://music.163.com/#/my/m/music/playlist?id=141663003
     * 单曲：http://music.163.com/#/song?id=26598519
     *
     * 网易云音乐API
     * 专辑：http://music.163.com/api/album/141663003
     * 歌单：http://music.163.com/api/playlist/detail?id=2532058
     * 单曲：http://music.163.com/api/song/detail?ids=[33166563]
     * 歌词：http://music.163.com/api/song/media?id=33166563
     *
     */

    public class Quality {
        public final static String HIGH_QUALITY = "hMusic";
        public final static String MEDIUM_QUALITY = "mMusic";
        public final static String LOW_QUALITY = "lMusic";
    }

    public class NamingRule {
        public final static String SONG_AND_ARTIST = "%s - %s";
        public final static String ARTIST_AND_SONG = "%2$s - %1$s";
        public final static String SONG_NAME_ONLY = "%s";
    }

    public void parseURL( String urlAddress, String quality,boolean isIncludeLyric, String namingRule, int limit ) {
        String albumRegex = "http[s]?://music.163.com/#/album\\?id=(\\d+)";
        String playlistRegex = "http[s]?://music.163.com/#(?:/my/m/music)?/playlist\\?id=(\\d+)";
        String songRegex = "http[s]?://music.163.com/#/song\\?id=(\\d+)";

        if ( urlAddress.matches(albumRegex) ) {
            Matcher matcher = Pattern.compile(albumRegex).matcher(urlAddress);
            if ( matcher.find() ) parseAlbum(matcher.group(1), quality, isIncludeLyric, namingRule, limit);
        }
        else if ( urlAddress.matches(playlistRegex) ) {
            Matcher matcher = Pattern.compile(playlistRegex).matcher(urlAddress);
            if ( matcher.find() ) parsePlaylist(matcher.group(1), quality, isIncludeLyric, namingRule, limit);
        }
        else if ( urlAddress.matches(songRegex) ) {
            Matcher matcher = Pattern.compile(songRegex).matcher(urlAddress);
            if ( matcher.find() ) parseSong(matcher.group(1), quality, isIncludeLyric, namingRule);
        }
        else {
            System.out.println("无效的Url：" + urlAddress);
            System.out.println("暂时支持只解析网易云音乐的歌单、专辑和单曲");
        }
    }

    private void parseAlbum( String albumID, String quality, boolean isIncludeLyric, String namingRule, int limit ) {
        String json = readContent( "http://music.163.com/api/album/" + albumID );
        JSONObject jsonObject = new JSONObject(json);
        JSONObject album = jsonObject.getJSONObject("album");
        JSONArray songsJson = album.getJSONArray("songs");
        parseResult(songsJson, quality, isIncludeLyric, namingRule, limit);
    }
    private void parsePlaylist( String listID, String quality, boolean isIncludeLyric, String namingRule, int limit ) {
        String json = readContent( "http://music.163.com/api/playlist/detail?id=" + listID );
        JSONObject jsonObject = new JSONObject(json);
        JSONObject result = jsonObject.getJSONObject("result");
        JSONArray tracksJson = result.getJSONArray("tracks");
        parseResult(tracksJson, quality, isIncludeLyric, namingRule, limit);
    }
    private void parseSong( String SongID, String quality, boolean isIncludeLyric, String namingRule ) {
        String json = readContent( "http://music.163.com/api/song/detail?ids=[" + SongID + "]" );
        JSONObject jsonObject = new JSONObject(json);
        JSONArray songsJson = jsonObject.getJSONArray("songs");
        parseResult(songsJson, quality, isIncludeLyric, namingRule, 1);
    }

    private void parseResult(JSONArray jsonResult, String quality, boolean isIncludeLyric, String namingRule,
                             int limit ) {

        Song[] songs = new Song[jsonResult.length()];

        for ( int i = 0; i < jsonResult.length(); ++i ) {
            JSONObject curSong = jsonResult.getJSONObject(i);
            int id = curSong.getInt("id");
            String name = curSong.getString("name");
            JSONArray artists = curSong.getJSONArray("artists");
            String[] artistList = new String[artists.length()];
            for ( int j = 0; j < artists.length(); ++j ) {
                artistList[j] = artists.getJSONObject(j).getString("name");
            }
            String artist = String.join(", ", artistList);
            String mp3Url = curSong.getString("mp3Url");
            mp3Url = mp3Url.substring(0, mp3Url.indexOf('/', "https://".length()));
            long dfsId = curSong.getJSONObject(quality).getLong("dfsId");
            String extension = curSong.getJSONObject(quality).getString("extension");
            Song song = new Song(id, name, artist, mp3Url, dfsId, extension);
            songs[i] = song;
        }

        ExecutorService pool = Executors.newFixedThreadPool(limit);
        System.out.println("共：" + songs.length + "首歌曲");
        for ( Song song : songs ) {
            String songURL = song.getMp3Url() + "/" + encryptedID("" + song.getDfsId()) + "/" +
                    song.getDfsId() + "." + song.getExtension();
            String songName = String.format(namingRule, song.getName(), song.getArtist()) + "." + song.getExtension();
            File file = new File(songName);

            pool.execute( new Thread( () -> {
                System.out.println("正在下载：" + String.format(namingRule, song.getName(), song.getArtist()));
                downloadFile(songURL, file);
                if ( isIncludeLyric ) {
                    String jsonContent = readContent("http://music.163.com/api/song/media?id=" + song.getId());
                    JSONObject lyricJson = new JSONObject(jsonContent);
                    try {
                        String lyric = lyricJson.getString("lyric");
                        String lyricName = String.format(namingRule, song.getName(), song.getArtist()) + ".lrc";
                        try {
                            BufferedWriter bufferedWriter = new BufferedWriter( new FileWriter(lyricName) );
                            bufferedWriter.write(lyric);
                            bufferedWriter.flush();
                            bufferedWriter.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } catch ( JSONException e ){
                        //no lyric found
                    }
                }
            }) );
        }
        pool.shutdown();
    }

    private String encryptedID( String SongID ) {
        String result = "";

        byte[] secretKey = "3go8&$8*3*3h0k(2)2".getBytes();
        byte[] songId = SongID.getBytes();
        int keyLength = secretKey.length;
        for ( int i = 0; i < songId.length; ++i ) {
            songId[i] = (byte)(songId[i] ^ secretKey[i % keyLength]);
        }
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            result = Base64.getEncoder().encodeToString( md5.digest(songId) );
            result = result.replaceAll("/", "_");
            result = result.replaceAll("\\+", "-");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return result;
    }

    private String readContent( String urlAddress ) {
        String result = "";
        try {
            URL url = new URL(urlAddress);
            URLConnection urlConnection = url.openConnection();
            urlConnection.setRequestProperty("referer", "http://music.163.com");
            BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(urlConnection.getInputStream()) );
            final int BUFFER_SIZE = 1024;
            char[] buffer = new char[BUFFER_SIZE];
            int charsRead;
            StringBuilder stringBuilder = new StringBuilder();
            while ( ( charsRead = bufferedReader.read(buffer, 0, BUFFER_SIZE) ) != -1 ) {
                stringBuilder.append(buffer, 0, charsRead);
            }
            bufferedReader.close();
            result = stringBuilder.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    private long downloadFile(String urlAddress, File outFile ) {
        long fileSize = 0;
        try {
            URLConnection connection = new URL(urlAddress).openConnection();
            connection.connect();
            fileSize = connection.getContentLength();
            InputStream is = connection.getInputStream();
            final int BUFFER_SIZE = 1024;
            byte[] BUFFER = new byte[BUFFER_SIZE];
            int bytesRead;
            OutputStream os = new FileOutputStream(outFile);
            while ( ( bytesRead = is.read(BUFFER, 0, BUFFER_SIZE) ) != -1 ) {
                os.write(BUFFER, 0, bytesRead);
            }
            os.flush();
            os.close();
            is.close();
        }
        catch ( IOException e ) {
            e.printStackTrace();
        }
        return fileSize;
    }
}
