//import org.springframework.ai.vectorstore.SearchRequest;
//import org.springframework.ai.vectorstore.VectorStore;
//import org.springframework.ai.document.Document;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.List;
//
//@RestController
//public class RagTestController {
//
//    @Autowired
//    private VectorStore vectorStore;
//
//    @GetMapping("/testSearch")
//    public String testSearch(String question) {
//        // 💡 核心：进行相似度搜索！
//        // withTopK(1) 表示只拿出最相似的 1 条结果
//        List<Document> results = vectorStore.similaritySearch(
//                SearchRequest.query(question).withTopK(1)
//        );
//
//        if (results.isEmpty()) {
//            return "知识库里没找到相关内容...";
//        }
//
//        // 打印出找到的最相似的内容
//        return "匹配到的知识：" + results.get(0).getContent();
//    }
//}