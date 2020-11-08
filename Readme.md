[![Build Status](https://travis-ci.org/barakb/mini-rest-client.svg?branch=master)](https://travis-ci.org/barakb/mini-rest-client)
[![Download](https://api.bintray.com/packages/barakb/maven/mini-rest-client/images/download.svg) ](https://bintray.com/barakb/maven/mini-rest-client/_latestVersion)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.barakb/mini-rest-client.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.github.barakb%22%20AND%20a:%22mini-rest-client%22)
[![GitHub License](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](https://www.apache.org/licenses/LICENSE-2.0)
### A Kotlin Minimal REST client

I had 3 goals in mind when starting this work.

1. Fully async code no blocking threads.
2. Easy to compose both sequentially and concurrently
3. Minimum dependencies.
4. Small

- Choosing the underlining http client to be Apache HttpAsyncClient **satisfy the first and third requirements**.
- Extending `CloseableHttpAsyncClient.execute` as a suspend function (in the file `CloseableHttpAsyncClientExt.kt`)
  enable easy composition of the result client sequentially and concurrently, hence **satisfy the second requirement**. 


To consume this project using maven add the following to your pom.xml


````Xml
<dependency>
     <groupId>com.github.barakb</groupId>
     <artifactId>mini-rest-client</artifactId>
     <version>1.0.2</version>
</dependency>
````

Or gradle

````kotlin

implementation("com.github.barakb:mini-rest-client:1.0.2")
````


##### Usage:
To create a Nomad client Kotlin DSL can be used.
```Kotlin
fun main(): Unit = runBlocking {
    HttpClient {
        defaultRequest {
            url = "http://httpbin.org/"
            header("name", "value")
            param("v", "f")
        }
    }.use { client ->
        val headers = client.get<JsonObject> {
            path = "headers"
        }
        println("headers: $headers")
    }
}
```   

Inside the client DLS you have full access to the underline HttpAsyncClientBuilder, so it is easy to configure

```kotlin
client {
}
```

