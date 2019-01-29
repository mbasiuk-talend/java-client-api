package com.marklogic.client.datamovement;

import java.io.InputStream;
import java.io.Reader;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.marklogic.client.io.JacksonHandle;

/**
 * The JacksonCSVSplitter class uses the Jackson CSV parser without attempting to abstract it capabilities. 
 * The application can override defaults by configuring the Jackson ObjectReader and CsvSchema including parsing TSV
 */
public class JacksonCSVSplitter implements Splitter<JacksonHandle> {
    private CsvSchema csvSchema = null;
    private CsvMapper csvMapper;
    private long count = 0;
    
    public CsvMapper getCsvMapper() {
        return csvMapper;
    }

    public JacksonCSVSplitter withCsvSchema(CsvSchema schema) {
        this.csvSchema = schema;
        return this;
    } 
    
    public JacksonCSVSplitter withCsvMapper(CsvMapper mapper) {
        this.csvMapper = mapper;
        return this;
    }
    
    public CsvSchema getCsvSchema() {
        return csvSchema;
    }
    
    private CsvMapper configureCsvMapper() {
        if(csvMapper == null) {
        csvMapper = new CsvMapper()
                .configure(CsvParser.Feature.ALLOW_TRAILING_COMMA, true)
                .configure(CsvParser.Feature.FAIL_ON_MISSING_COLUMNS, false)
                .configure(CsvParser.Feature.IGNORE_TRAILING_UNMAPPABLE, false)
                .configure(CsvParser.Feature.INSERT_NULLS_FOR_MISSING_COLUMNS, false)
                .configure(CsvParser.Feature.SKIP_EMPTY_LINES, true)
                .configure(CsvParser.Feature.TRIM_SPACES, true)
                .configure(CsvParser.Feature.WRAP_AS_ARRAY, false);
        }
        return csvMapper;
    }

    @Override
    public Stream<JacksonHandle> split(InputStream input) throws Exception { 

        if(input == null) {
            throw new IllegalArgumentException("InputSteam cannot be null.");
        }
        Iterator<JsonNode> nodeItr = configureObjReader().readValues(input);
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(nodeItr, Spliterator.ORDERED), false)
                .map(this::configureJacksonHandle);
    }
    public Stream<JacksonHandle> split(Reader input) throws Exception  { 

        if(input == null) {
            throw new IllegalArgumentException("Input cannot be null.");
        }
        Iterator<JsonNode> nodeItr = configureObjReader().readValues(input);
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(nodeItr, Spliterator.ORDERED), false)
                .map(this::configureJacksonHandle);
    }

    @Override
    public long getCount() { 
        return this.count;
    }
    
    private void incrementCount() {
        this.count++;
    }
    
    private ObjectReader configureObjReader() {
        this.count=0;
        CsvSchema firstLineSchema = getCsvSchema()!=null? getCsvSchema():CsvSchema.emptySchema().withHeader();
        
        ObjectReader objectReader = configureCsvMapper().readerFor(JsonNode.class);
        
        return objectReader.with(firstLineSchema);
    }
    
    private JacksonHandle configureJacksonHandle(JsonNode content) {
        incrementCount();
        return new JacksonHandle(content);
    }
}