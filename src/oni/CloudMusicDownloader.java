package oni;

import com.mpatric.mp3agic.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.StringJoiner;
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

    private String[] songQuality = {"hMusic", "mMusic", "lMusic", "bMusic"};

    public class Quality {
        public final static int HIGH_QUALITY = 0;
        public final static int MEDIUM_QUALITY = 1;
        public final static int LOW_QUALITY = 2;
    }

    public class NamingRule {
        public final static String SONG_AND_ARTIST = "%s - %s";
        public final static String ARTIST_AND_SONG = "%2$s - %1$s";
        public final static String SONG_NAME_ONLY = "%s";
    }

    public void parseURL( String urlAddress, int quality,boolean isIncludeLyric, boolean isSetID3Tag,
                          String namingRule, int limit ) {
        String albumRegex = "http[s]?://music.163.com(?:/#)?/album\\?id=(\\d+)";
        String playlistRegex = "http[s]?://music.163.com(?:/#)?(?:/my/m/music)?/playlist\\?id=(\\d+)";
        String songRegex = "http[s]?://music.163.com(?:/#)?/song\\?id=(\\d+)";

        if ( urlAddress.matches(albumRegex) ) {
            Matcher matcher = Pattern.compile(albumRegex).matcher(urlAddress);
            if ( matcher.find() ) parseAlbum(matcher.group(1), quality, isIncludeLyric, isSetID3Tag, namingRule, limit);
        }
        else if ( urlAddress.matches(playlistRegex) ) {
            Matcher matcher = Pattern.compile(playlistRegex).matcher(urlAddress);
            if ( matcher.find() ) parsePlaylist(matcher.group(1), quality, isIncludeLyric, isSetID3Tag, namingRule,
                    limit);
        }
        else if ( urlAddress.matches(songRegex) ) {
            Matcher matcher = Pattern.compile(songRegex).matcher(urlAddress);
            if ( matcher.find() ) parseSong(matcher.group(1), quality, isIncludeLyric, isSetID3Tag, namingRule);
        }
        else {
            System.out.println("无效的Url：" + urlAddress);
            System.out.println("暂时支持只解析网易云音乐的歌单、专辑和单曲");
        }
    }

    private void parseAlbum( String albumID, int quality, boolean isIncludeLyric, boolean isSetID3Tag,
                             String namingRule, int limit ) {
        String json = readContent( "http://music.163.com/api/album/" + albumID );
        JSONObject jsonObject = new JSONObject(json);
        JSONObject album = jsonObject.getJSONObject("album");
        File dir = new File( album.getString("name") );
        if ( ( !dir.exists() && !dir.mkdir() ) || ( dir.exists() && dir.isFile() ) )
        {
            System.out.println( "不能创建目录：" + dir.getAbsolutePath() );
            return;
        }
        JSONArray songsJson = album.getJSONArray("songs");
        parseResult(songsJson, quality, dir, isIncludeLyric, isSetID3Tag, namingRule, limit);
    }
    private void parsePlaylist( String listID, int quality, boolean isIncludeLyric, boolean isSetID3Tag,
                                String namingRule, int limit ) {
        String json = readContent( "http://music.163.com/api/playlist/detail?id=" + listID );
        JSONObject jsonObject = new JSONObject(json);
        JSONObject result = jsonObject.getJSONObject("result");
        File dir = new File( result.getString("name") );
        if ( ( !dir.exists() && !dir.mkdir() ) || ( dir.exists() && dir.isFile() ) )
        {
            System.out.println( "不能创建目录：" + dir.getAbsolutePath() );
            return;
        }
        JSONArray tracksJson = result.getJSONArray("tracks");
        parseResult(tracksJson, quality, dir, isIncludeLyric, isSetID3Tag, namingRule, limit);
    }
    private void parseSong( String SongID, int quality, boolean isIncludeLyric, boolean isSetID3Tag,
                            String namingRule ) {
        String json = readContent( "http://music.163.com/api/song/detail?ids=[" + SongID + "]" );
        JSONObject jsonObject = new JSONObject(json);
        JSONArray songsJson = jsonObject.getJSONArray("songs");
        parseResult(songsJson, quality, null, isIncludeLyric, isSetID3Tag, namingRule, 1);
    }

    private void parseResult(JSONArray jsonResult, int quality, File dir, boolean isIncludeLyric, boolean isSetID3Tag,
                             String namingRule, int limit ) {

        Song[] songs = new Song[jsonResult.length()];

        for ( int i = 0; i < jsonResult.length(); ++i ) {
            JSONObject curSong = jsonResult.getJSONObject(i);
            int id = curSong.getInt("id");
            String name = curSong.getString("name");
            JSONArray artists = curSong.getJSONArray("artists");
            StringJoiner joiner = new StringJoiner(", ");
            for ( int j = 0; j < artists.length(); ++j ) {
                joiner.add( artists.getJSONObject(j).getString("name") );
            }
            String artist = joiner.toString();
            String album = curSong.getJSONObject("album").getString("name");

            String mp3Url = null;
            long dfsId = 0;
            String extension = null;
            int bitrate = 0;
            if ( !curSong.isNull("mp3Url") ) {
                mp3Url = curSong.getString("mp3Url");
                int curQuality = quality;
                while ( curQuality < songQuality.length && curSong.isNull(songQuality[curQuality]) ) {
                    ++curQuality;
                }
                JSONObject music = curSong.getJSONObject(songQuality[curQuality]);
                dfsId = music.getLong("dfsId");
                extension = music.getString("extension");
                bitrate = music.getInt("bitrate");
                if ( curQuality < songQuality.length ) {
                    mp3Url = mp3Url.substring(0, mp3Url.indexOf('/', "https://".length())) + "/" +
                            encryptedID("" + dfsId) + "/" + dfsId + "." + extension;
                }
            }

            Song song = new Song(id, name, artist, album, mp3Url, dfsId, extension, bitrate);
            songs[i] = song;
        }

        ExecutorService pool = Executors.newFixedThreadPool(limit);
        System.out.println("共：" + songs.length + "首歌曲\n正在下载...");
        for ( Song song : songs ) {
            if ( song.getMp3Url() == null ) {
                System.out.println("歌曲失效：" + String.format(namingRule, song.getName(), song.getArtist()));
                continue;
            }
            String songURL = song.getMp3Url();
            String songName = String.format(namingRule, song.getName(), song.getArtist()) + "." + song.getExtension();
            File file;
            if ( dir != null ) {
                file = new File( dir.getAbsolutePath() + File.separator + songName);
            }
            else {
                file = new File(songName);
            }

            pool.execute( new Thread( () -> {
                downloadFile(songURL, file);

                if ( isIncludeLyric ) {
                    downloadLyric(song, dir, namingRule);
                }
                if ( isSetID3Tag ) {
                    setID3Tag(song, file);
                }
                System.out.println( "下载完成：" + String.format(namingRule, song.getName(), song.getArtist()) +
                        " (" + song.getBitrate() / 1000 + " kbps)" );
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

    private boolean downloadLyric( Song song, File dir, String namingRule ) {
        boolean result = true;
        String jsonContent = readContent("http://music.163.com/api/song/media?id=" + song.getId());
        JSONObject lyricJson = new JSONObject(jsonContent);
        if ( lyricJson.has("lyric") ) {
            String lyric = lyricJson.getString("lyric");
            String lyricName;
            if (dir == null) {
                lyricName = String.format(namingRule, song.getName(), song.getArtist()) + ".lrc";
            } else {
                lyricName = dir.getAbsolutePath() + File.separator +
                        String.format(namingRule, song.getName(), song.getArtist()) + ".lrc";
            }
            try {
                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(lyricName));
                bufferedWriter.write(lyric);
                bufferedWriter.flush();
                bufferedWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            result = false;
        }

        return result;
    }

    private void setID3Tag(Song song, File file ) {
        try {
            Mp3File mp3File = new Mp3File(file);

            if ( mp3File.hasId3v1Tag() ) mp3File.removeId3v1Tag();
            if ( mp3File.hasCustomTag() ) mp3File.removeCustomTag();

            ID3v2 id3v2Tag = new ID3v24Tag();
            id3v2Tag.setTitle( song.getName() );
            id3v2Tag.setArtist( song.getArtist() );
            id3v2Tag.setAlbum( song.getAlbum() );
            mp3File.setId3v2Tag(id3v2Tag);

            File modifiedFile = new File( file.getAbsolutePath() + "_modified" );
            mp3File.save(modifiedFile.getAbsolutePath());
            if ( !file.delete() || !modifiedFile.renameTo(file) ) {
                System.out.println("修改ID3标签失败！");
            }
        } catch (IOException | UnsupportedTagException | InvalidDataException | NotSupportedException e) {
            e.printStackTrace();
        }
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
