package oni;

public class Song {
    private int id;
    private String name;
    private String artist;
    private String mp3Url;
    private long dfsId;
    private String extension;

    public Song(){}

    public Song(int id, String name, String artist, String mp3Url, long dfsId, String extension) {
        this.id = id;
        this.name = name;
        this.artist = artist;
        this.mp3Url = mp3Url;
        this.dfsId = dfsId;
        this.extension = extension;
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
}