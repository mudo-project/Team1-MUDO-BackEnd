package com.academy.mudogroupware.dataimport.application.port;

import java.util.List;

public interface ImportAnalysisPort {

    List<ParsedImportSheet> analyze(List<ParsedImportSheet> sheets);
}
