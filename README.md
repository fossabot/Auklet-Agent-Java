<p align="center"><a href="https://auklet.io"><img src="https://s3.amazonaws.com/auklet/static/github_readme_java.png" alt="Auklet - Problem Solving Software for Java"></a></p>

# Auklet for Java
[![Maintainability](https://api.codeclimate.com/v1/badges/e1ced62442c7cf49d58a/maintainability)](https://codeclimate.com/github/aukletio/Auklet-Agent-Java/maintainability)
[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Fjoey-fossa%2FAuklet-Agent-Java.svg?type=shield)](https://app.fossa.com/projects/git%2Bgithub.com%2Fjoey-fossa%2FAuklet-Agent-Java?ref=badge_shield)

This is the Java agent for Auklet, officially supporting Java 7+ and Android Jelly Bean or higher (4.1.x+, API level 16+).

# Features

* Automatic report of unhandled exceptions.
* Location and system metrics identification for all issues.


# Quickstart
The Auklet Java agent is deployed to [JCenter](https://bintray.com/bintray/jcenter).

## Maven
```
<repository>
  <snapshots>
    <enabled>false</enabled>
  </snapshots>
  <id>central</id>
  <name>bintray</name>
  <url>http://jcenter.bintray.com</url>
</repository>

<dependency>
  <groupId>io.auklet</groupId>
  <artifactId>auklet-agent-java</artifactId>
  <version>...</version>
</dependency>
```

## Gradle (Java)
```
repositories {
    // other repositories
    jcenter()
}

implementation 'io.auklet:auklet-agent-java:<version>'
```

## Gradle (Android)
In the project's `build.gradle` file.
```
allprojects {
    repositories {
        // other repositories
        jcenter()
    }
}
```

In the module's `build.gradle` file.
```
implementation 'io.auklet:auklet-agent-java:<version>'
```

# Code

## Configuration/Authorization
To authorize your application you need to provide both an API key and app ID. These values are available in the connection settings of your application as well as during initial setup.

Unless you use the `Config` object to manually set your Auklet credentials, you will need to set one of the following on the JVM where you are running Auklet:
* The environment variables `AUKLET_APP_ID` and `AUKLET_API_KEY`
* JVM system properties `auklet.app.id` and `auklet.api.key`

On Android, you will need to add
`<uses-permission android:name="android.permission.INTERNET" />` to your manifest.

## Java
To configure Auklet agent using env vars/JVM sysprops, all you would need is
```
Auklet.init();
```

To configure the Auklet agent manually, you would need to set an app id and an api key.
```
Auklet.init(new Auklet.Config()
    .setAppId("...")
    .setApiKey("...")
    // ...set other options here
);
```

## Android
To configure Auklet agent using env vars/JVM sysprops, all you would need is
```
Auklet.init(new Auklet.Config().setAndroidContext(getApplicationContext()));
```

To configure the Auklet agent manually, you would need to set an app id and an api key.
```
Auklet.init(new Auklet.Config()
    .setAndroidContext(getApplicationContext())
    .setAppId("...")
    .setApiKey("...")
    // ...set other options here
);
```

## Sending Exceptions
By default, Auklet sets itself as the JVM's default uncaught exception handler; thus, any exceptions that bubble up out of any thread will be sent to Auklet. This can be disabled via any one of the following:
- `new Config().setUncaughtExceptionHandler(false)`
- Setting env var `AUKLET_UNCAUGHT_EXCEPTION_HANDLER` to `false`
- Setting JVM system property `auklet.uncaught.exception.handler` to `false`

If you are catching the exception and not letting it bubble up, you can use this syntax to send it to Auklet.
```
try {
    // code that can cause an exception
} catch (Exception e) {
    Auklet.send(e);
}
```

## Auto-Start (Java only)
Set the environment variable `AUKLET_AUTO_START` or the JVM system property `auklet.auto.start` to `true` to have the agent start alongside the JVM. In this configuration, the agent will only send to Auklet exceptions that are not caught within a thread or by a thread handler (see the previous section for details). If you want to explicitly catch and report some exceptions to Auklet, do not use this method.

## Security Policies (Java only)
If you have a security manager/security policy enabled on the JVM where the Auklet agent is running, you will need/want to grant certain permissions, as documented below:

In addition to what is listed below, you may need to grant permissions for whatever SLF4J logging backend you choose to implement.

```
grant {
    // *** Required permissions for Auklet config dir ***
    // You must have these permissions in your security policy or the Auklet agent will not work.
    // It is STRONGLY RECOMMENDED that you explicitly set the Auklet config dir to "/path/to/your/config/dir/" when using a security policy.
    permission java.io.FilePermission "/path/to/your/config/dir/.auklet","read,write,delete";
    permission java.io.FilePermission "/path/to/your/config/dir/.auklet/-","read,write,delete";

    // *** Required permissions for Auklet dependencies ***
    // You must have these permissions in your security policy or the Auklet agent will not work.
    permission java.lang.RuntimePermission "modifyThread";
    permission java.lang.reflect.ReflectPermission "suppressAccessChecks";
    permission java.net.NetPermission "getNetworkInformation";
    permission java.net.NetPermission "getProxySelector";
    permission java.net.SocketPermission "checkip.amazonaws.com","connect";
    permission java.net.SocketPermission "auklet-certs.s3.amazonaws.com","connect";
    permission java.util.PropertyPermission "okhttp.platform","read";
    permission java.util.PropertyPermission "java.runtime.name","read";
    permission java.util.PropertyPermission "com.google.appengine.runtime.version","read";
    permission java.util.PropertyPermission "msgpack.universal-buffer","read";
    // If explicitly setting the base URL, update these permissions accordingly.
    permission java.net.SocketPermission "api.auklet.io","connect";
    permission java.net.SocketPermission "mq.feeds.auklet.io","connect";

    // *** Recommended permissions ***
    // In typical deployments, you will want to grant these in order to use these features.
    permission java.lang.RuntimePermission "getenv.AUKLET_APP_ID";
    permission java.lang.RuntimePermission "getenv.AUKLET_API_KEY";
    permission java.lang.RuntimePermission "getenv.AUKLET_CONFIG_DIR";
    permission java.lang.RuntimePermission "getenv.AUKLET_LOUD_SECURITY_EXCEPTIONS";
    permission java.util.PropertyPermission "auklet.app.id","read";
    permission java.util.PropertyPermission "auklet.api.key","read";
    permission java.util.PropertyPermission "auklet.config.dir","read";
    permission java.util.PropertyPermission "auklet.loud.security.exceptions","read";
    // Needed to add JVM shutdown hooks and uncaught exception handler, which are enabled by default.
    permission java.lang.RuntimePermission "shutdownHooks";
    permission java.lang.RuntimePermission "setDefaultUncaughtExceptionHandler";
    // If you do not want to grant the above two permissions, you will need to disable those
    // functionalities, either by using a config object in your code or by adding two of the
    // four permissions below and setting the corresponding envvar/sysprop.
    permission java.lang.RuntimePermission "getenv.AUKLET_AUTO_SHUTDOWN";
    permission java.lang.RuntimePermission "getenv.AUKLET_UNCAUGHT_EXCEPTION_HANDLER";
    permission java.util.PropertyPermission "auklet.auto.shutdown","read";
    permission java.util.PropertyPermission "auklet.uncaught.exception.handler","read";

    // *** Optional permissions ***
    // In typical deployments, you can get by without these permissions.
    permission java.lang.RuntimePermission "getenv.AUKLET_AUTO_START";
    permission java.lang.RuntimePermission "getenv.AUKLET_BASE_URL";
    permission java.lang.RuntimePermission "getenv.AUKLET_SERIAL_PORT";
    permission java.lang.RuntimePermission "getenv.AUKLET_THREADS_MQTT";
    permission java.util.PropertyPermission "auklet.auto.start","read";
    permission java.util.PropertyPermission "auklet.base.url","read";
    permission java.util.PropertyPermission "auklet.serial.port","read";
    permission java.util.PropertyPermission "auklet.threads.mqtt","read";
    permission java.util.PropertyPermission "user.dir","read";
    permission java.util.PropertyPermission "user.home","read";
    permission java.util.PropertyPermission "java.io.tmpdir","read";
    // Most JVMs do not require these permissions. If you are sending performance data
    // to the Auklet cloud, you will need to enable loud logging of SecurityExceptions
    // to determine if you need these permissions.
    permission java.util.PropertyPermission "os.name","read";
    permission java.util.PropertyPermission "os.arch","read";
    permission java.util.PropertyPermission "os.version","read";
};
```

## HTTPS Certificates
If you are running the Auklet agent on a platform that does not trust the root CA certificates used to communicate with the Auklet cloud, you will experience exceptions like this:

```
io.auklet.AukletException: Error while making HTTP request.
        at io.auklet.net.Https.doRequest...
Caused by: javax.net.ssl.SSLHandshakeException: sun.security.validator.ValidatorException: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
        at sun.security.ssl.Alerts.getSSLException...
```

To fix this, you will need to pass the necessary certificates to the Auklet agent's config object. Please contact Auklet support to obtain the necessary certificate files; you will need to provide these files to the Auklet agent at runtime.

Below is some sample code you can use to do this. In this example, the certificate files are included as resources in your app's JAR file and accessed via the classloader. Do not retain any references to these streams, as they will eventually be closed by the Auklet agent.

```
import java.util.Arrays;
import io.auklet.Auklet;
import io.auklet.Config;

Auklet.init(new Config()
    .setAppId("your-app-id")
    .setApiKey("your-api-key")
    .setSslCertificates(Arrays.asList(
        YourApp.class.getResourceAsStream("/cert-1.pem"),
        YourApp.class.getResourceAsStream("/cert-2.pem")
    ))
);
```

## Note Regarding TLS Support
The auklet.io data pipeline uses TLS 1.2. According to [Android docs](https://developer.android.com/reference/javax/net/ssl/SSLSocket#protocols), TLS 1.2 is enabled by default starting with API level 20 (Android version 4.4W). If your application supports API levels 16 through 19, you may need to do additional work to ensure that TLS 1.2 is available.

If TLS 1.2 is not available on a device, the Auklet agent will fail to initialize and will log relevant messages to SLF4J, but this will not interrupt your application. Notably, since `Auklet.init()` never throws any exceptions, you will not be notified by the agent if it fails to initialize for this reason, so you should take care to ensure TLS 1.2 is available prior to invoking `Auklet.init()`.

You may find [this post](https://medium.com/tech-quizlet/working-with-tls-1-2-on-android-4-4-and-lower-f4f5205629a) relevant in adding TLS 1.2 compatibility for devices on API levels 16 through 19.

# Compiling/Running Locally
Auklet for Java uses Gradle 4 and requires Java 7+ to compile (Java 9+ recommended).

```
git clone git@github.com:aukletio/Auklet-Agent-Java.git
cd Auklet-Agent-Java
# This stores the Auklet agent in your local Maven repository with artifact version "local-build", which you can use per the instructions above.
./gradlew publishToMavenLocal
```

When compiling on Java 7, you will see warnings related to dependencies compiled for newer versions of Java; these warnings can be ignored.

When compiling on Java 8, use this syntax fix cross-compilation warnings (requires installing JDK 7): `JDK7_HOME=/path/to/java7/home ./gradlew publishToMavenLocal`

# Questions? Problems? Ideas?

To get support, report a bug or suggest future ideas for Auklet, go to https://help.auklet.io and click the blue button in the lower-right corner to send a message to our support team.


## License
[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Fjoey-fossa%2FAuklet-Agent-Java.svg?type=large)](https://app.fossa.com/projects/git%2Bgithub.com%2Fjoey-fossa%2FAuklet-Agent-Java?ref=badge_large)