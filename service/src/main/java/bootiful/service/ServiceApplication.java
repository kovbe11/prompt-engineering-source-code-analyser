package bootiful.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SpringBootApplication
public class ServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceApplication.class, args);
    }

    @Bean
    VectorStore vectorStore(EmbeddingClient ec,
                            JdbcTemplate t) {
        return new PgVectorStore(t, ec);
    }

    @Bean
    TokenTextSplitter tokenTextSplitter() {
        return new TokenTextSplitter();
    }

    static void init(VectorStore vectorStore, JdbcTemplate template, Resource pdfResource)
            throws Exception {

        template.update("delete from vector_store");

        var config = PdfDocumentReaderConfig.builder()
                .withPageExtractedTextFormatter(new ExtractedTextFormatter.Builder()
                                                        .withNumberOfBottomTextLinesToDelete(1)
                                                        .withNumberOfTopTextLinesToDelete(1)
                        .withNumberOfTopPagesToSkipBeforeDelete(14)
                        .build())
                .withPagesPerDocument(5)
                .build();

        var pdfReader = new PagePdfDocumentReader(pdfResource, config);
        var textSplitter = new TokenTextSplitter();
        vectorStore.accept(textSplitter.apply(pdfReader.get()));

    }

    @Bean
    ApplicationRunner applicationRunner(
            Chatbot chatbot,
            VectorStore vectorStore,
            JdbcTemplate jdbcTemplate,
            @Value("classpath:coding-interview-book.pdf") Resource resource,
            @Value("classpath:intersection-problem.txt") Resource problemResource,
            @Value("classpath:intersect-n2-solution.txt") Resource solutionResource,
            @Value("classpath:prompt-1.txt") Resource promptResource
            ) {
        return args -> {
            //init(vectorStore, jdbcTemplate, resource);
            var problem = new String(problemResource.getInputStream().readAllBytes());
            var solution = new String(solutionResource.getInputStream().readAllBytes());
            var prompt = new String(promptResource.getInputStream().readAllBytes());
            var response = chatbot.chat(solution, problem, prompt);
            System.out.println(Map.of("response", response));
        };
    }

}


@Component
@Slf4j
class Chatbot {


    private final String template = """
            {prompt}
            
            Use the information from the PROBLEM section to understand the specific problem the provided code is solving.
            Use the information from the TIPS section to help guide your response. Those tips are from a book on coding interviews.
            
            PROBLEM:
            {problem}
            ----------------
            TIPS:
            {tips}
                        
            """;
    private final ChatClient aiClient;
    private final VectorStore vectorStore;

    Chatbot(ChatClient aiClient, VectorStore vectorStore) {
        this.aiClient = aiClient;
        this.vectorStore = vectorStore;
    }

    public String chat(String providedCode, String problem, String prompt) {
        var listOfSimilarDocuments = this.vectorStore.similaritySearch(problem);
        var tips = listOfSimilarDocuments
                .stream()
                .map(Document::getContent)
                .collect(Collectors.joining(System.lineSeparator()));
//        log.info("Tips: {}", tips);
        var systemMessage = new SystemPromptTemplate(this.template)
                .createMessage(Map.of("prompt", prompt, "tips", tips, "problem", problem));
        var userMessage = new UserMessage(providedCode);
        var fullPrompt = new Prompt(List.of(systemMessage, userMessage));
        var aiResponse = aiClient.call(fullPrompt);
        return aiResponse.getResult().getOutput().getContent().replace(".", "\n");
    }
}
