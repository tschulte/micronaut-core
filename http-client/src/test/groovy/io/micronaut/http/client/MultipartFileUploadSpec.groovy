package io.micronaut.http.client

import io.micronaut.context.ApplicationContext
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Header
import io.micronaut.http.annotation.Post
import io.micronaut.runtime.server.EmbeddedServer
import io.reactivex.Flowable
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

class MultipartFileUploadSpec extends Specification {

    @Shared @AutoCleanup ApplicationContext context = ApplicationContext.run()
    @Shared EmbeddedServer embeddedServer = context.getBean(EmbeddedServer).start()
    @Shared @AutoCleanup HttpClient client = context.createBean(HttpClient, embeddedServer.getURL())
    @Shared File uploadDir = File.createTempDir()


    void "test multipart file request"() {
        given:
        File file = new File(uploadDir, "data.txt")
        file.text = "test file"
        file.createNewFile()

        when:

        Flowable<HttpResponse<File>> flowable = Flowable.fromPublisher(client.exchange(
                HttpRequest.POST("/multipart/upload", file)
                        .contentType(MediaType.MULTIPART_FORM_DATA_TYPE)
                        .accept(MediaType.TEXT_PLAIN_TYPE),
                File
        ))
        HttpResponse<File> response = flowable.blockingFirst()
        def body = response.getBody()

        then:
        body.isPresent()
    }

    @Controller('/multipart')
    static class MultipartController {

        @Post(uri = '/upload', consumes = MediaType.MULTIPART_FORM_DATA)
        HttpResponse<String> upload(File data,
                                           @Header String contentType, @Header long contentLength, @Header accept) {
            assert contentType == MediaType.MULTIPART_FORM_DATA
            assert contentLength == 13
            assert accept == MediaType.TEXT_PLAIN
            return HttpResponse.ok("Uploaded")
        }

    }
}
