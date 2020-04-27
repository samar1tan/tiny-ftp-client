# Tiny FTP Client

Complying with a limited subset of RFC [959](http://www.rfcreader.com/#rfc959) & [3659](http://www.rfcreader.com/#rfc3659), a multithreading (i.e., [deadlock-prone](https://github.com/samar1tan/tiny-ftp-client/issues/1)) FTP client written in Java socket programming and [JavaFX 11](https://openjfx.io/) GUI framework, which supports the resuming of interrupted downloading / uploading and parallel transfers.

We develop this with pride for our course — *Application Design of Computer Networks, Wuhan University*.

## Install

Before you start, please ensure your JDK (or JRE alone) has been installed, path variable (`JAVA_HOME`) has been appropriately set, and the current version is `11` by running `java -version` on the command line.

> Why JDK 11?
>
> JDK 8 and 11 are the only two [LTS (Long-Term Support) versions](https://en.wikipedia.org/wiki/Java_version_history) for now, which means better stability and continuous fixes, and 8 is older. 

**If you haven't installed JDK / JRE / "Java" before**, you can visit [this site](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html) to download Oracle JDK installer of your platform, or [OpenJDK's version](https://jdk.java.net/java-se-ri/11) if you prefer open sources.

You can check your setting of path variable from command line by

- Linux Shell

```sh
echo $JAVA_HOME
```

- Windows CMD

```cmd
echo %JAVA_HOME%
```

**If you have installed JDK / JRE / "Java" from Oracle on Windows before**,  its installer will add extra contents (pointing to `java.exe`) to environment variable `PATH`, which is prior to `JAVA_HOME` and lead a mismatching when doubly clicking`tiny-ftp-client-specific_ver.exe`. Thus, if you need to upgrade/downgrade your JDK / JRE / “Java”, please remember to remove those contents subsequently.

Once done, do sanity check by running

```bash
java -version
```

You should see prompts like

```
java version "11.0.7" 2020-04-14 LTS
```

Finally, please download artifacts of your platform from [our release page](https://github.com/samar1tan/tiny-ftp-client/releases)

## Usage

- EXE: just doubly click it.
- JAR: launch it from command line:

```bash
java -jar tiny-ftp-client-specific_ver.jar
```

Enjoy it!

**If the application fails to start because of dependencies,** you can clone this repo and use Maven or IntelliJ+Maven (recommended) to build and run.

**Please notice we haven't support [vsFTPd](https://security.appspot.com/vsftpd.html), which is widely deployed on Linux FTP servers**, because it lacks FTP command `MLSD`. You can use another outstanding, free software **[FileZilla Server](https://filezilla-project.org/download.php?type=server)**.

## Contributing

PRs accepted and welcomed. *Although we believe no one else will have an interest. :P*

## License

GPLv3 © FxxkMultiThreading Group

## See Also

**For more information about the project**, dependencies, and configuration details used by Maven to build the project, please see [pom.xml](pom.xml)

**For Javadoc**, please click [here](https://samaritan.cn/javadoc/tiny-ftp-client/index.html) for online reading.
