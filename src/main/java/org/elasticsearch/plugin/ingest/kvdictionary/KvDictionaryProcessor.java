/*
 * Copyright [2018] [Ismael Hasan Romero]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */


package org.elasticsearch.plugin.ingest.kvdictionary;

import static org.elasticsearch.ingest.ConfigurationUtils.readBooleanProperty;
import static org.elasticsearch.ingest.ConfigurationUtils.readOptionalMap;
import static org.elasticsearch.ingest.ConfigurationUtils.readOptionalStringProperty;
import static org.elasticsearch.ingest.ConfigurationUtils.readStringProperty;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.io.PathUtils;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.ingest.AbstractProcessor;
import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.ingest.Processor;

public class KvDictionaryProcessor extends AbstractProcessor {

    private static final Logger LOGGER = Loggers.getLogger(KvDictionaryProcessor.class);

    public static final String TYPE = "kvdictionary";

    private final String field;
    private final String targetField;
    private final Map<String, String> dictionary;
    private final boolean ignoreCase;
    // TODO: locale management
    private static final Locale LOCALE = Locale.ENGLISH;

    public KvDictionaryProcessor(String tag, String field, String targetField, Map<String, String> dictionary, boolean ignoreCase) {
        super(tag);
        this.field = field;
        this.targetField = targetField;
        this.dictionary = dictionary;
        this.ignoreCase = ignoreCase;
    }

    @Override
    public void execute(IngestDocument ingestDocument) throws Exception {
        String content = ingestDocument.getFieldValue(field, String.class);
        LOGGER.info(ingestDocument.toString());
        if (content != null) {
            ingestDocument.setFieldValue(targetField, dictionary.get(ignoreCase ? content.toLowerCase(LOCALE) : content));

        } else {
            LOGGER.trace("Documents does not contain a value for {}", field);
        }
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public static final class Factory implements Processor.Factory {

        private final Path dictionaryPath;

        public Factory(Path dicionaryPath) {
            super();
            this.dictionaryPath = dicionaryPath;
        }

        @Override
        public KvDictionaryProcessor create(Map<String, Processor.Factory> factories, String tag, Map<String, Object> config)
                throws IOException {
            // Initialization of the processor properties
            // ignore_case is not required, it defaults to true.
            // dictionary and dictionaryFile, one of the two is required. They are used in
            // conjunction; the dictionary in the processor definition can overwrite terms from the
            // dictionary in the file.
            String field = readStringProperty(TYPE, tag, config, "field");
            String targetField = readStringProperty(TYPE, tag, config, "target_field");

            Map<String, String> dictionaryAsJson = readOptionalMap(TYPE, tag, config, "dictionary_json");
            String dictionaryName = readOptionalStringProperty(TYPE, tag, config, "dictionary_file");
            boolean ignoreCase = readBooleanProperty(TYPE, tag, config, "ignore_case", false);
            String charset = readStringProperty(TYPE, tag, config, "charset", StandardCharsets.UTF_8.name());
            // TODO: charset management
            if (!charset.equals(StandardCharsets.UTF_8.name())) {
                LOGGER.warn("Charset management not implemented. Using default \"{}\" instead of configured {}",
                        StandardCharsets.UTF_8.name(), charset);
                charset = StandardCharsets.UTF_8.name();
            }
            LOGGER.debug(
                    "Initializing dictionary: field \"{}\", targetField \"{}\", dictionaryName \"{}\", ignoreCase \"{}\", charset \"{}\"",
                    field, targetField, dictionaryName, ignoreCase, charset);

            // Creation of the dictionary
            Map<String, String> dictionary = new HashMap<String, String>();
            if (dictionaryName == null && dictionaryAsJson == null) {
                LOGGER.warn("No dictionaries defined. This processor does nothing.");
            } else {
                if (dictionaryName != null) {
                    String line;
                    BufferedReader reader = null;
                    try {
                        reader = Files.newBufferedReader(PathUtils.get(new Path[] { dictionaryPath }, dictionaryName),
                                Charset.forName(charset));
                    } catch (IOException e) {
                        LOGGER.error("Cannot instantiate dictionary <{}> with charset <{}>",
                                PathUtils.get(new Path[] { dictionaryPath }, dictionaryName), charset);
                        throw e;
                    }
                    boolean dictionaryHasErrors = false;
                    try {
                        while ((line = reader.readLine()) != null) {
                            int pos = line.indexOf(":");
                            if (pos >= 1 && pos < line.length() - 1) {
                                dictionary.put(ignoreCase ? line.substring(0, pos).toLowerCase(LOCALE) : line.substring(0, pos),
                                        line.substring(pos + 1));
                            } else {
                                LOGGER.debug("Error retrieving key:value from line \"{}\"", line);
                                dictionaryHasErrors = true;
                            }
                        }
                    } catch (IOException e) {
                        LOGGER.error("Error reading the dictionary file {}", dictionaryName);
                        throw e;
                    }
                    reader.close();
                    if (dictionary.isEmpty()) {
                        LOGGER.warn("Dictionary \"{}\" is empty. No translations available", dictionaryName);
                    }
                    if (dictionaryHasErrors) {
                        LOGGER.warn(
                                "There were errors parsing the lines of the dictionary in the file. Enable debug mode for"
                                        + " \"{}\" and load an instance of the dictionary to list all of the failing lines",
                                KvDictionaryProcessor.class.getName());
                    }
                }
                if (dictionaryAsJson != null) {
                    if (ignoreCase) {
                        for (Entry<String, String> entry : dictionaryAsJson.entrySet()) {
                            dictionary.put(entry.getKey().toLowerCase(LOCALE), entry.getValue());
                        }
                    } else {
                        dictionary.putAll(dictionaryAsJson);
                    }
                }
            }
            LOGGER.debug("Loaded {} entries in the dictionary", dictionary.size());

            return new KvDictionaryProcessor(tag, field, targetField, dictionary, ignoreCase);
        }
    }
}
