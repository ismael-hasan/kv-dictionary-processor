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

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import org.elasticsearch.common.Randomness;
import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.ingest.RandomDocumentPicks;
import org.elasticsearch.test.ESTestCase;

public class KvDictionaryProcessorTests extends ESTestCase {
    private String field;
    private String targetField;
    private List<String> keysCapitalized;
    private Map<String, String> dictionary;
    private List<String> inexistentTerms;
    private Random generator;
    private static final Locale LOCALE = Locale.ENGLISH;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        field = "input";
        targetField = "output";
        keysCapitalized = Arrays.asList(new String[] { "Input1","Input2","Input3","Input4","Input5"});
        dictionary = new HashMap<String, String>();
        for (String key : keysCapitalized) {
            dictionary.put(key, "translated_" + key);
        }

        inexistentTerms = Arrays.asList(new String[] { "inexistent1", "inexistent2" });
        generator = Randomness.get();
    }

    public void testReplaceCaseSensitiveFound() throws Exception {
        String currentKey = keysCapitalized.get(generator.nextInt(dictionary.size()));
        String currentValue = dictionary.get(currentKey);

        Map<String, Object> document = new HashMap<>();
        document.put(field, currentKey);
        IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);

        KvDictionaryProcessor processor = new KvDictionaryProcessor(randomAlphaOfLength(10), field, targetField, dictionary, false);
        processor.execute(ingestDocument);
        Map<String, Object> data = ingestDocument.getSourceAndMetadata();

        assertThat(data, hasKey(targetField));
        assertThat(data.get(targetField), is(currentValue));
    }

    public void testReplaceCaseSensitiveNotFound() throws Exception {

        String currentKey = keysCapitalized.get(generator.nextInt(dictionary.size())).toUpperCase(LOCALE);

        Map<String, Object> document = new HashMap<>();
        document.put(field, currentKey);
        IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);

        KvDictionaryProcessor processor = new KvDictionaryProcessor(randomAlphaOfLength(10), field, targetField, dictionary, false);
        processor.execute(ingestDocument);
        Map<String, Object> data = ingestDocument.getSourceAndMetadata();

        assertNull(data.get(targetField));
    }

    public void testReplaceCaseInsensitive() throws Exception {

        String currentKey = keysCapitalized.get(generator.nextInt(dictionary.size())).toUpperCase(LOCALE);
        String currentValue = dictionary.get(currentKey);

        Map<String, Object> document = new HashMap<>();
        document.put(field, currentKey);
        IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);

        KvDictionaryProcessor processor = new KvDictionaryProcessor(randomAlphaOfLength(10), field, targetField, dictionary, true);
        processor.execute(ingestDocument);
        Map<String, Object> data = ingestDocument.getSourceAndMetadata();

        assertThat(data, hasKey(targetField));
        assertThat(data.get(targetField), is(currentValue));
    }
    
    public void testReplaceCaseInsensitiveNotFound() throws Exception {

        String currentKey = inexistentTerms.get(generator.nextInt(inexistentTerms.size()));

        Map<String, Object> document = new HashMap<>();
        document.put(field, currentKey);
        IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);

        KvDictionaryProcessor processor = new KvDictionaryProcessor(randomAlphaOfLength(10), field, targetField, dictionary, true);
        processor.execute(ingestDocument);
        Map<String, Object> data = ingestDocument.getSourceAndMetadata();

        assertNull(data.get(targetField));
    }


}
