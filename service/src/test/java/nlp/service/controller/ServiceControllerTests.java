package nlp.service.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import nlp.common.model.annotation.GenericAnnotation;
import nlp.common.model.document.GenericDocument;
import nlp.common.model.protocol.ServiceResponseContent;
import nlp.service.ServiceApplication;
import nlp.service.config.ServiceConfiguration;
import nlp.service.utils.TestUtils;


/**
 * This class implements tests for testing the functionality of an example REST Service
 *  controller for extracting annotations from example documents.
 *  For the moment, only GATE controller is used and the example GATE application includes
 *  a simple Drug application identifying common drug names.
 */
@SpringBootTest(classes = ServiceApplication.class)
@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@ContextConfiguration(classes = {ServiceConfiguration.class})
@TestPropertySource(locations = "classpath:test-application.properties")
public class ServiceControllerTests {

    @Autowired
    private MockMvc mockMvc;

    final private String PROCESS_ENDPOINT_URL = "/api/process";
    final private String INFO_ENDPOINT_URL = "/api/info";


    @Test
    public void testGetApplicationInfo() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                    .get(INFO_ENDPOINT_URL)
                    .accept(MediaType.APPLICATION_JSON_UTF8))
                .andReturn();

        int status = result.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status, "Incorrect Response Status");
    }


    @Test
    public void testProcessEmptyPayload() throws Exception {
        GenericDocument inDoc = TestUtils.createEmptyDocument();

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                    .post(PROCESS_ENDPOINT_URL)
                    .contentType(MediaType.APPLICATION_JSON_UTF8)
                    .accept(MediaType.APPLICATION_JSON_UTF8)
                    .content(createPayload(inDoc.getText())))
                    .andReturn();

        int status = result.getResponse().getStatus();
        assertEquals(HttpStatus.BAD_REQUEST.value(), status, "Incorrect Response Status");
    }


    @Test
    public void testProcessBlankDocuments() throws Exception {

        List<GenericDocument> inDocs = TestUtils.createBlankDocuments();

        for (GenericDocument doc : inDocs) {
            MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .post(PROCESS_ENDPOINT_URL)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(MediaType.APPLICATION_JSON_UTF8)
                        .content(createPayload(doc.getText())))
                    .andReturn();

            int status = result.getResponse().getStatus();
            assertEquals(HttpStatus.OK.value(), status, "Incorrect Response Status");

            ObjectMapper mapper = new ObjectMapper();
            ServiceResponseContent response = mapper.readValue(result.getResponse().getContentAsString(),
                    ServiceResponseContent.class);

            // check the processing status
            assertEquals(true, response.getResult().getSuccess(), "Invalid processing status");

            // check whether there were any annotations returned
            assertEquals(0, response.getResult().getAnnotations().size(), "Returned annotations");
        }
    }


    @Test
    public void testProcessExampleDocumentDrugNames() throws Exception {

        GenericDocument inDoc = TestUtils.createShortDocument();

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                .post(PROCESS_ENDPOINT_URL)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .accept(MediaType.APPLICATION_JSON_UTF8)
                .content(createPayload(inDoc.getText())))
                .andReturn();

        int status = result.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status, "Incorrect Response Status");

        // parse the content
        ObjectMapper mapper = new ObjectMapper();
        ServiceResponseContent response = mapper.readValue(result.getResponse().getContentAsString(),
                                                            ServiceResponseContent.class);

        assertEquals(true, response.getResult().getSuccess(), "Invalid processing status");

        // should be only one annotation returned
        List<GenericAnnotation> annotations = response.getResult().getAnnotations();
        assertEquals(1, annotations.size(), "Returned annotations");

        // this annotation should be Prozac and listed as drug:medication
        GenericAnnotation ann = annotations.get(0);
        assertEquals("prozac", ann.getAttributes().get("name").toString().toLowerCase());
        assertEquals("drug", ann.getAttributes().get("majorType").toString().toLowerCase());
        assertEquals("medication", ann.getAttributes().get("minorType").toString().toLowerCase());
    }


    @Test
    public void testProcessACMDDocumentDrugNames() throws Exception {

        GenericDocument inDoc = TestUtils.createACMDocument();

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                .post(PROCESS_ENDPOINT_URL)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .accept(MediaType.APPLICATION_JSON_UTF8)
                .content(createPayload(inDoc.getText())))
                .andReturn();

        int status = result.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status, "Incorrect Response Status");

        // parse the content
        ObjectMapper mapper = new ObjectMapper();
        ServiceResponseContent response = mapper.readValue(result.getResponse().getContentAsString(),
                ServiceResponseContent.class);

        assertEquals(true, response.getResult().getSuccess(), "Invalid processing status");
        assertNotEquals(0, response.getResult().getAnnotations(), "Returned annotations");
    }


    /**
     * Helper functions
     */
    static private String createPayload(String text) throws Exception {
        JSONObject json = new JSONObject()
                .put("content", new JSONObject()
                        .put("text", text));
        return json.toString();
    }
}
