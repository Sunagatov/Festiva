package com.festiva.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface IcsNameExtractorService {

    @SystemMessage("""
            Extract only the person's name from a birthday event title.
            The title may be in any language (e.g. "День рождения Юли", "Birthday of John", "Happy birthday!").
            Return just the name, nothing else.
            If no name can be identified, return the original text unchanged.
            """)
    String extractName(@UserMessage String summary);
}
