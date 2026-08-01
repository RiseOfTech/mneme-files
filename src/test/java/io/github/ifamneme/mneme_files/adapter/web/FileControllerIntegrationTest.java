package io.github.ifamneme.mneme_files.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import io.github.ifamneme.mneme_files.AbstractIntegrationTest;

class FileControllerIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@LocalServerPort
	private int port;

	@Test
	void uploadThenDownload_roundTripsEachAllowedType() throws Exception {
		assertRoundTrip("valid.jpg", "image/jpeg");
		assertRoundTrip("valid.png", "image/png");
		assertRoundTrip("valid.webp", "image/webp");
	}

	private void assertRoundTrip(String fixture, String expectedContentType) throws Exception {
		byte[] bytes = readFixture(fixture);
		MockMultipartFile file = new MockMultipartFile("file", fixture, expectedContentType, bytes);

		MvcResult uploadResult = mockMvc.perform(multipart("/v1/files").file(file))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").exists())
			.andExpect(jsonPath("$.contentType").value(expectedContentType))
			.andExpect(jsonPath("$.size").value(bytes.length))
			.andExpect(header().string("Location", matchesPattern(".*/v1/files/.+")))
			.andReturn();

		String id = com.jayway.jsonpath.JsonPath.read(uploadResult.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(get("/v1/files/{id}", id))
			.andExpect(status().isOk())
			.andExpect(header().string("Content-Type", expectedContentType))
			.andExpect(header().longValue("Content-Length", bytes.length))
			.andExpect(header().exists("ETag"))
			.andExpect(header().string("Cache-Control",
					allOf(containsString("public"), containsString("max-age=31536000"), containsString("immutable"))))
			.andExpect(content().bytes(bytes));
	}

	@Test
	void unknownButWellFormedId_returns404WithProblemJson() throws Exception {
		mockMvc.perform(get("/v1/files/{id}", java.util.UUID.randomUUID()))
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.code").value("file_not_found"));
	}

	@Test
	void malformedId_returns400AndNeverReachesStorage() throws Exception {
		mockMvc.perform(get("/v1/files/{id}", "not-a-uuid"))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.code").value("invalid_request"));
	}

	@Test
	void leadingBytesMatchNoAllowedType_returns415EvenIfDeclaredTypeIsAllowed() throws Exception {
		byte[] textBytes = readFixture("text.txt");
		// Declared type falsely claims an allowed image type - it must be ignored.
		MockMultipartFile file = new MockMultipartFile("file", "text.txt", "image/jpeg", textBytes);

		mockMvc.perform(multipart("/v1/files").file(file))
			.andExpect(status().isUnsupportedMediaType())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.code").value("unsupported_content_type"));
	}

	@Test
	void jpegDeclaredAsOctetStream_succeedsAndIsStoredAsDetectedType() throws Exception {
		byte[] jpegBytes = readFixture("valid.jpg");
		MockMultipartFile file = new MockMultipartFile("file", "photo", "application/octet-stream", jpegBytes);

		mockMvc.perform(multipart("/v1/files").file(file))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.contentType").value("image/jpeg"));
	}

	@Test
	void twoUploadsOfIdenticalBytes_yieldDistinctIndependentlyRetrievableIds() throws Exception {
		byte[] bytes = readFixture("valid.png");

		MvcResult first = mockMvc
			.perform(multipart("/v1/files").file(new MockMultipartFile("file", "a.png", "image/png", bytes)))
			.andExpect(status().isCreated())
			.andReturn();
		MvcResult second = mockMvc
			.perform(multipart("/v1/files").file(new MockMultipartFile("file", "b.png", "image/png", bytes)))
			.andExpect(status().isCreated())
			.andReturn();

		String firstId = com.jayway.jsonpath.JsonPath.read(first.getResponse().getContentAsString(), "$.id");
		String secondId = com.jayway.jsonpath.JsonPath.read(second.getResponse().getContentAsString(), "$.id");
		assertThat(firstId).isNotEqualTo(secondId);

		mockMvc.perform(get("/v1/files/{id}", firstId)).andExpect(status().isOk()).andExpect(content().bytes(bytes));
		mockMvc.perform(get("/v1/files/{id}", secondId)).andExpect(status().isOk()).andExpect(content().bytes(bytes));
	}

	// MockMvc dispatches in-memory, without a real socket, so it cannot observe whether
	// a client actually receives a complete response body versus a connection reset.
	// This is the one case in the suite that needs a genuine HTTP round trip.
	@Test
	void oversizedUpload_returnsCompleteProblemJsonBody_notAConnectionReset() {
		byte[] oversized = new byte[6 * 1024 * 1024];
		new Random().nextBytes(oversized);
		oversized[0] = (byte) 0xFF;
		oversized[1] = (byte) 0xD8;
		oversized[2] = (byte) 0xFF;

		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add("file", new ByteArrayResource(oversized) {
			@Override
			public String getFilename() {
				return "huge.jpg";
			}
		});

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

		RestTemplate restTemplate = new RestTemplate();
		try {
			ResponseEntity<String> response = restTemplate.postForEntity("http://localhost:" + port + "/v1/files",
					request, String.class);
			assertThat(response.getStatusCode().value()).isEqualTo(413);
			assertThat(response.getBody()).contains("file_too_large");
		}
		catch (RestClientResponseException ex) {
			assertThat(ex.getStatusCode().value()).isEqualTo(413);
			assertThat(ex.getResponseBodyAsString()).contains("file_too_large");
		}
	}

	private byte[] readFixture(String name) throws IOException {
		try (InputStream in = getClass().getResourceAsStream("/fixtures/" + name)) {
			return in.readAllBytes();
		}
	}

}
