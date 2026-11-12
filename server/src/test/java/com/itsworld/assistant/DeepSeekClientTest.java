package com.itsworld.assistant;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsworld.assistant.service.DeepSeekClient;
import com.itsworld.common.exception.BusinessException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
class DeepSeekClientTest {
    HttpServer server; String base; ObjectMapper json=new ObjectMapper();
    @BeforeEach void start() throws Exception {
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        base="http://127.0.0.1:"+server.getAddress().getPort(); server.start();
    }
    @AfterEach void stop() { server.stop(0); }
    void response(int status,String body) {
        server.createContext("/chat/completions",exchange->{
            byte[] bytes=body.getBytes(StandardCharsets.UTF_8); exchange.sendResponseHeaders(status,bytes.length);
            exchange.getResponseBody().write(bytes); exchange.close();
        });
    }
    @Test void requestShapeAndToolResponse() throws Exception {
        var captured=new AtomicReference<String>();
        server.createContext("/chat/completions",exchange->{
            captured.set(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
            byte[] bytes="{\"choices\":[{\"message\":{\"role\":\"assistant\",\"tool_calls\":[{\"id\":\"c1\"}]}}]}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200,bytes.length); exchange.getResponseBody().write(bytes); exchange.close();
        });
        var client=new DeepSeekClient(json,true,"test-key",base,"deepseek-flash");
        var result=client.complete(List.of(json.createObjectNode().put("role","user").put("content","test")),json.createArrayNode(),System.nanoTime()+Duration.ofSeconds(5).toNanos());
        assertEquals("c1",result.path("choices").get(0).path("message").path("tool_calls").get(0).path("id").asText());
        var body=json.readTree(captured.get()); assertEquals("disabled",body.path("thinking").path("type").asText());
        assertEquals(900,body.path("max_tokens").asInt()); assertEquals("deepseek-flash",body.path("model").asText());
    }
    @Test void disabledAndExpiredDeadline() {
        var client=new DeepSeekClient(json,false,"",base,"test");
        assertEquals("ASSISTANT_NOT_CONFIGURED",assertThrows(BusinessException.class,()->client.complete(List.of(),null,Long.MAX_VALUE)).getMessage());
        var enabled=new DeepSeekClient(json,true,"test",base,"test");
        assertEquals("ASSISTANT_TIMEOUT",assertThrows(BusinessException.class,()->enabled.complete(List.of(),null,System.nanoTime()-1)).getMessage());
    }
    @Test void statusCodesDoNotExposeUpstreamBody() {
        for (int code:List.of(401,402,429,500)) {
            response(code,"private detail");
            var client=new DeepSeekClient(json,true,"test",base,"test");
            var error=assertThrows(BusinessException.class,()->client.complete(List.of(),null,Long.MAX_VALUE));
            assertFalse(error.getMessage().contains("private"));
            server.removeContext("/chat/completions");
        }
    }
    @Test void badJsonAndErrorsAreSanitized() {
        response(401,"secret-upstream-body"); var client=new DeepSeekClient(json,true,"test",base,"test");
        assertEquals("ASSISTANT_UPSTREAM_AUTH",assertThrows(BusinessException.class,()->client.complete(List.of(),null,Long.MAX_VALUE)).getMessage());
        server.removeContext("/chat/completions"); response(200,"bad-json");
        assertEquals("ASSISTANT_UPSTREAM_ERROR",assertThrows(BusinessException.class,()->client.complete(List.of(),null,Long.MAX_VALUE)).getMessage());
    }
    @Test void shortDeadlineTimesOutWithoutRetry() {
        server.createContext("/chat/completions",exchange->{try { Thread.sleep(250); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } exchange.close(); });
        var client=new DeepSeekClient(json,true,"test",base,"test");
        assertEquals("ASSISTANT_TIMEOUT",assertThrows(BusinessException.class,()->client.complete(List.of(),null,System.nanoTime()+Duration.ofMillis(50).toNanos())).getMessage());
    }
}
