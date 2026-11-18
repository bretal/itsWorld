package com.itsworld.admin.service;

import com.itsworld.common.util.MinIOUtils;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MiniProgramAssetServiceTest {
    @Test
    void resizesAndEncodesImageBeforeVerifyingPublicLink() throws Exception {
        HttpServer http = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        http.start();
        try {
            MinIOUtils minio = mock(MinIOUtils.class);
            when(minio.uploadToExistingBucket(eq("itsworld-static"), startsWith("miniapp-v1/onboarding/"),
                    any(), eq("image/webp"))).thenAnswer(invocation -> {
                InputStream stream = invocation.getArgument(2);
                byte[] encoded = stream.readAllBytes();
                assertEquals("RIFF", new String(encoded, 0, 4, java.nio.charset.StandardCharsets.US_ASCII));
                assertEquals("WEBP", new String(encoded, 8, 4, java.nio.charset.StandardCharsets.US_ASCII));
                http.createContext("/asset.webp", exchange -> {
                    exchange.getResponseHeaders().set("Content-Type", "image/webp");
                    exchange.getResponseHeaders().set("Content-Length", String.valueOf(encoded.length));
                    exchange.sendResponseHeaders(200, -1);
                    exchange.close();
                });
                return "http://127.0.0.1:" + http.getAddress().getPort() + "/asset.webp";
            });
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            ImageIO.write(new BufferedImage(900, 1800, BufferedImage.TYPE_INT_ARGB), "png", png);
            var input = new MockMultipartFile("file", "asset.png", "image/png", png.toByteArray());

            var result = new MiniProgramAssetService(minio).upload(input, "onboarding");

            assertEquals(900, result.originalWidth());
            assertEquals(1800, result.originalHeight());
            assertEquals(540, result.width());
            assertEquals(1080, result.height());
            assertTrue(result.bytes() > 0);
        } finally {
            http.stop(0);
        }
    }
}
