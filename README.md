# Elasticsearch kvdictionary Ingest Processor

It reads a key-value dictionary from the processor configuration and/or a file in `config/kv_dictionary_plugin` and it applies that key-value translation to a field

## Usage


```
PUT _ingest/pipeline/kvdictionary-pipeline
{
  "description": "When abc is the value of field input, it stores def in the field output",
  "processors": [
    {
      "kvdictionary": {
        "field": "input",
        "target_field": "output",
        "dictionary_json": {
          "abc":"def"
        },
        "ignore_case":false
      }
    }
  ]
}


PUT /testkv/doc/1?pipeline=kvdictionary-pipeline
{
  "input" : "abc"
}

GET /testkv/doc/1
{
  "_index": "testkv",
  "_type": "doc",
  "_id": "1",
  "_version": 1,
  "found": true,
  "_source": {
    "output": "def",
    "input": "abc"
  }
}
```

## Configuration

_At least one dictionary must be defined. If both are defined, and there is a key conflict, the JSON value prevails_  

| Parameter | Use |
| --- | --- |
| field   | Field containing the key in the dictionary |
| target_field   | Field to contain the value associated to the key  |
| dictionary_file   | File containing key-value pairs. Each line must contain a single pair in the format `key:value`. Values can contain `:` |
| dictionary_json   | JSON object containing key-value objects as in the previous example. Values can only be strings. |
| ignore_case   | _optional_ `true` if keys must be matched regardless of the case. `false` otherwise. It defaults to `false` |
| charset   | *not implemented* It defaults to `UTF-8` |

## Setup

In order to install this plugin, you need to create a zip distribution first by running

```bash
gradle clean check
```

This will produce a zip file in `build/distributions`.

After building the zip file, you can install it like this

```bash
bin/elasticsearch-plugin install file:///path/to/ingest-kvdictionary/build/distribution/ingest-kvdictionary-0.0.1-SNAPSHOT.zip
```

Dictionaries are expected in the folder `config/kv_dictionary_plugin`. If you want to use a dictionary called `dict1.txt` in that dictionary, you need to configure the parameter `dictionary_file` to `dict1.txt`

## Resiliency
The dictionary is intended to work regardless of most errors, and warn about them. Some examples:
- If no dictionary is defined, the processor will warn in the logs about doing nothing, but it will not fail when added to a pipeline, and documents going through the pipeline will be normally ingested
- If a `charset` (not implemented) is defined, it will set the default charset and warn in the logs
- If there is an io error reading the dictionary, it will fail and log the error. 
- If some line in the dictionary file cannot be parsed, it will report that there were errors after it is loaded. 
  - To see the exact lines that failed, enable debug logs and load a pipeline containing the same processor definition.


## Bugs & TODO

* There are always bugs
* and todos...

