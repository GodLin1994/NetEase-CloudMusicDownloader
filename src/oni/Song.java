package oni;

public class Song {
    private int id;
    private String name;
    private String artist;
    private String album;
    private int no;
    private String mp3Url;
    private long dfsId;
    private String extension;
    private int bitrate;

    public Song(){}

    public Song(int id, String name, String artist, String album, int no, String mp3Url, long dfsId, String extension,
                int bitrate) {
        this.id = id;
        this.name = name;
        this.artist = artist;
        this.album = album;
        this.no = no;
        this.mp3Url = mp3Url;
        this.dfsId = dfsId;
        this.extension = extension;
        this.bitrate = bitrate;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public int getNo() {
        return no;
    }

    public void setNo(int no) {
        this.no = no;
    }

    public String getMp3Url() {
        return mp3Url;
    }

    public void setMp3Url(String mp3Url) {
        this.mp3Url = mp3Url;
    }

    public long getDfsId() {
        return dfsId;
    }

    public void setDfsId(long dfsId) {
        this.dfsId = dfsId;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public int getBitrate() {
        return bitrate;
    }

    public void setBitrate(int bitrate) {
        this.bitrate = bitrate;
    }
}