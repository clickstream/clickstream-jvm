# clickstream-jvm

The JVM driver for *clickstream.io*: captures users browsing sessions for servlets.

## Disclaimer

This is an alpha release, it is tested only with Spring 3 MVC.

## Example using **Maven** and any JVM application built on `javax.servlet`

In order to use, include the following in your
**pom.xml** file in `<dependencies>`:

        <dependency>
            <groupId>io.clickstream</groupId>
            <artifactId>driver</artifactId>
            <version>0.3-SNAPSHOT</version>
        </dependency>

**web.xml** file:

    <filter>
        <filter-name>Capture</filter-name>
        <filter-class>
            io.clickstream.driver
        </filter-class>
        <init-param>
            <param-name>capture</param-name>
            <param-value>true</param-value>
        </init-param>
        <init-param>
            <param-name>api-key</param-name>
            <param-value>YOUR API KEY</param-value>
        </init-param>
        <init-param>
            <param-name>filter-params</param-name>
            <param-value>password|credit_card</param-value>
        </init-param>
        <init-param>
            <param-name>filter-uri</param-name>
            <param-value>forbidden|\/admin</param-value>
        </init-param>
    </filter>
    <filter-mapping>
        <filter-name>Capture</filter-name>
        <url-pattern>/*</url-pattern>
    </filter-mapping>

## Options

- `api-key`: the api key for authentication (mandatory)
- `api-uri`: overwrite api uri endpoint
- `capture`: set to true to collect data, default `false`
- `bench`: set to true to benchmark middleware overhead, default `false`
- `capture_crawlers`: set to true to capture hits from crawlers, default `false`
- `crawlers`: overwrite crawlers user agent regex
- `filter-params`: pipe separated strings of parameters to filter, default `null` e.g. `password|credit_card`
- `filter-uri`: pipe separated strings (regex) of uri for which **not** to capture data, default `null` e.g. `forbidden|\/admin`

## Author

Jerome Touffe-Blin, [@jtblin](https://twitter.com/jtlbin), [http://www.linkedin.com/in/jtblin](http://www.linkedin.com/in/jtblin)

## License

clickstream-jvm is copyright 2013 Jerome Touffe-Blin and contributors. It is licensed under the BSD license. See the include LICENSE file for details.

