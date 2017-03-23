[![Build Status](https://travis-ci.org/lhotari/akka-http-health.svg?branch=master)](https://travis-ci.org/lhotari/akka-http-health)

# akka-http-health

Library for adding `/health` endpoint checks for Akka Http applications.

Provides a default `/health` endpoint that checks:
* that the memory state is healthy
* that there is enough available disk space

A load balancer like AWS ELB can be configured to call the health endpoint and
it could decide to destroy any unhealthy instances.

### Getting Started

#### Adding the dependency

sbt 0.13.6+
```
resolvers += Resolver.bintrayRepo("lhotari","releases")
libraryDependencies += "io.github.lhotari" %% "akka-http-health" % "1.0.5"
```

#### Simple use

The route instance created by calling `io.github.lhotari.akka.http.health.HealthEndpoint.createDefaultHealthRoute()` will handle `/health` with default settings.
Append that route instance to the desired route binding.

Because of security concerns, it is generally adviced to serve the health endpoint on a separate port that isn't exposed to the public.

#### Advanced use

For customization, use the trait `io.github.lhotari.akka.http.health.HealthEndpoint` and override protected methods. Calling the `createHealthRoute()` method creates the route instance.

### Contributing

Pull requests are welcome.

Some basic guidelines:
* A few unit tests would help a lot as well - someone has to do that before the PR gets merged.
* Please rebase the pull request branch against the current master.
* When writing a commit message please follow [these conventions](http://chris.beams.io/posts/git-commit/#seven-rules).
* If you are fixing an existing issue please add `Fixes gh-XXXX` at the end of the commit message (where `XXXX` is the issue number).
* Add the license header to each source code file (see existing source code files for an example)

### Contact

[Lari Hotari](mailto:lari@hotari.net)

### License

The library is Open Source Software released under the MIT license. See the [LICENSE file](LICENSE) for details.
