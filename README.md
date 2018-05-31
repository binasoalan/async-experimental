# binasoalan

Main repository

## Prerequisites

You will need [Leiningen][] 2.7.0 or above installed.

[leiningen]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run:

    lein ring server

## Integration testing

Run database migration using test profile:

```
lein with-profile test run migrate
```

Run server with test profile:

```
lein with-profile test ring server-headless
```

Run tests:

```
lein test
```

## License

Copyright © 2018 binasoalan
