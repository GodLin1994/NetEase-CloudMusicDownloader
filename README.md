# 网易云音乐下载器
从网易云音乐下载歌单、专辑和单曲

使用方法：
```
usage: CloudMusicDownloader [-c] [-h] [-m <limit>] [-n <name rule>] [-q <quality>] -u <url>
 -c,--lrc                     包含歌词
 -h,--help                    打印帮助
 -m,--limit <limit>           设置同时下载歌曲数
 -n,--name_rule <name rule>   设置歌曲命名格式。歌曲名+歌手：1，歌手+歌曲名：2，仅歌曲名：3
 -q,--quality <quality>       选择音频质量，高：h，中：m，低：l
 -u,--url <url>               从指定URL下载歌曲
```

例子：
```
java -jar NetEase-CloudMusicDownloader.jar -u http://music.163.com/\#/song\?id\=28302100 -c
java -jar NetEase-CloudMusicDownloader.jar -u http://music.163.com/\#/playlist\?id\=141663003 -q l -n 2 -m 1
```
