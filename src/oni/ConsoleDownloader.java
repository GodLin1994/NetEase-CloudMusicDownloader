package oni;

import org.apache.commons.cli.*;

public class ConsoleDownloader {
    private static CloudMusicDownloader cloudMusicDownloader = new CloudMusicDownloader();

    public static void main(String[] args) {
        Options options = new Options();

        final Option helpOption = Option.builder("h")
                .longOpt("help")
                .desc("打印帮助")
                .build();

        final Option urlOption = Option.builder("u")
                .longOpt("url")
                .desc("从指定URL下载歌曲")
                .hasArg()
                .argName("url")
                .required()
                .build();

        final Option qualityOption = Option.builder("q")
                .longOpt("quality")
                .desc("选择音频质量，高：h，中：m，低：l")
                .hasArg()
                .argName("quality")
                .build();

        final Option lyricOption = Option.builder("c")
                .longOpt("lrc")
                .desc("包含歌词")
                .build();

        final Option namingRuleOption = Option.builder("n")
                .longOpt("name_rule")
                .desc("设置歌曲命名格式。歌曲名+歌手：1，歌手+歌曲名：2，仅歌曲名：3")
                .hasArg()
                .argName("name rule")
                .build();

        final Option limitOption = Option.builder("m")
                .longOpt("limit")
                .desc("设置同时下载歌曲数")
                .hasArg()
                .argName("limit")
                .build();

        options
                .addOption(urlOption)
                .addOption(qualityOption)
                .addOption(lyricOption)
                .addOption(namingRuleOption)
                .addOption(limitOption)
                .addOption(helpOption);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter helpFormatter = new HelpFormatter();

        try {
            CommandLine cli = parser.parse(options, args);
            int quality = CloudMusicDownloader.Quality.HIGH_QUALITY;
            boolean isIncludeLyric = false;
            String namingRule = CloudMusicDownloader.NamingRule.SONG_AND_ARTIST;
            int limit = 5;

            if ( cli.hasOption("h") ) {
                helpFormatter.printHelp(cloudMusicDownloader.getClass().getSimpleName(), options);
            }

            if ( cli.hasOption("q") ) {
                char q = cli.getOptionValue("q").toCharArray()[0];
                switch ( q ) {
                    case 'h':
                        quality = CloudMusicDownloader.Quality.HIGH_QUALITY;
                        break;
                    case 'm':
                        quality = CloudMusicDownloader.Quality.MEDIUM_QUALITY;
                        break;
                    case 'l':
                        quality = CloudMusicDownloader.Quality.LOW_QUALITY;
                        break;
                    default:
                        printInvalidMsg(cli, "q");
                }
            }

            if ( cli.hasOption("c") ) {
                isIncludeLyric = true;
            }

            if ( cli.hasOption("n") ) {
                int n = 0;
                try {
                    n = Integer.parseInt( cli.getOptionValue("n") );
                    if ( n <= 0 ) {
                        printInvalidMsg(cli, "n");
                    }
                }
                catch ( NumberFormatException e ) {
                    printInvalidMsg(cli, "n");
                }
                switch ( n ) {
                    case 1:
                        namingRule = CloudMusicDownloader.NamingRule.SONG_AND_ARTIST;
                        break;
                    case 2:
                        namingRule = CloudMusicDownloader.NamingRule.ARTIST_AND_SONG;
                        break;
                    case 3:
                        namingRule = CloudMusicDownloader.NamingRule.SONG_NAME_ONLY;
                        break;
                    default:
                        printInvalidMsg(cli, "n");
                }
            }

            if ( cli.hasOption("m") ) {
                try {
                    limit = Integer.parseInt( cli.getOptionValue("m") );
                    if ( limit <= 0 ) {
                        printInvalidMsg(cli, "m");
                    }
                }
                catch ( NumberFormatException e ) {
                    printInvalidMsg(cli, "m");
                }
            }

            cloudMusicDownloader.parseURL(cli.getOptionValue("u"), quality, isIncludeLyric, namingRule, limit);
        } catch (ParseException e) {
            helpFormatter.printHelp(cloudMusicDownloader.getClass().getSimpleName(), options, true);
        }
    }

    private static void printInvalidMsg( CommandLine cli, String arg ) {
        System.out.println("Invalid arg: " + cli.getOptionValue(arg));
        System.exit(1);
    }
}
