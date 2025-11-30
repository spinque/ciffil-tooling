# Ciffil Service

The report describing this work can be found on [Zenodo](https://zenodo.org/records/17750643)

## Importing a CIFF file into DB index

```
./gradlew App:run --args="import <FILE> <connectionstring>"
```

optional: `--prefix <PREFIX>` (to point to a different table in the database)

## Exporting a DB index to CIFF file

```
./gradlew App:run --args="export <connectionstring> <FILE>"
```

optional: `--prefix <PREFIX>` (to point to a different table in the database)

## Querying a DB index 

```
./gradlew App:run --args="query <connectionstring> 'search query'"
```

## Acknowlegements
This software is part of an OpenWebSearch.eu project funded by the EC under the GA 101070014 within a Horizon Europe Framework programme.
