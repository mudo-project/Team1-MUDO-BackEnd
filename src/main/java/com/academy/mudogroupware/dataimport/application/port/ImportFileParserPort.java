package com.academy.mudogroupware.dataimport.application.port;

public interface ImportFileParserPort {

    ParsedImportSheet parse(ImportFile file);
}
