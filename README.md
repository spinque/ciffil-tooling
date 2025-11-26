# Ciffil Service


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
