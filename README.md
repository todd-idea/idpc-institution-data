# IDEA Data Portal CLI - Collect Institution Data

This project provides an example application that pulls data from the IDEA Data Portal. It is a
Groovy-based application that uses Gradle to build.

By providing it a valid application name and key (via command line arguments), you can create a CSV
of institution data and how they use IDEA instruments.

### Example Data

ID     | Name                    | FICE    | Chair | Admin | Teaching | Learning | Diagnostic
-------|-------------------------|---------|-------|-------|----------|----------|-----------
123456 | Kansas State University | 001122  | 0     | 0     | 0        | 3629     | 2508
234567 | Iowa State University   | 112233  | 153   | 15    | 0        | 0        | 0
345678 | University of Kansas    | 223344  | 0     | 0     | 0        | 51934    | 7395

## Building

To build this project, you will need Gradle installed and have access to the required dependencies (if connected to the
internet, they will be downloaded for you).

Once Gradle is setup, you can run the following to get the dependencies downloaded and the code compiled.
```
gradle build
```

## Installing

You can install the application using Gradle as well. You can run the following to install it (relative to the project root)
in build/install/idpc-collect-data.
```
gradle installDist
```

## Running

Once installed, you can run using the following.
```
cd build/install/idpc-collect-data/bin
./idpc-institution-data -a "TestClient" -k "ABCDEFG1234567890"
```
This will run against the IDEA Data Portal (rest.ideasystem.org) and spit out CSV (comma separated value) data that can
be imported into a spreadsheet (LibreCalc or Microsof Excel).

There are some command line options that you can use as well.
- h (host): This can configure the application to query a different server
- p (port): This can configure the application to query a different port on the server
- v (verbose): This can turn on more verbose output; this dumps a lot more information if you want to see what is being returned