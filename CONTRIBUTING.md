# Contributing

1. Build everything and run tests to learn how to do both:
  - `mvn -B -Dmaven.test.skip=true package`
  - `mvn -B test`

  The build compiles the engine into `target/classes`; the tests run against those classes, so a
  change to the engine is picked up by the next test run without any further step.

2. Make your changes and add a test for them
  - The tests live under `src/test/java`, one class per part of the engine
  - Run the tests as per the above to validate your changes

3. Create a pull request
  - Ensure the build runs: `mvn -B verify` builds, tests and packages
  - `commitlint` checks the commit message format

## Code of Conduct

Please note that this project is released with a Contributor Code of Conduct. By participating in
this project you agree to abide by its terms.

## Running the command line

The engine ships a command that renders a template:

    mvn -B -q -Dmaven.test.skip=true package
    java -cp target/classes com.liquidjs.cli.Main --template '{{ "hello" | capitalize }}'

`--help` lists every option it takes.
