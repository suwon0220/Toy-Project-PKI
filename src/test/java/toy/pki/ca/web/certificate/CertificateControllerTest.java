package toy.pki.ca.web.certificate;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import toy.pki.ca.application.profile.service.CertificateProfileService;

@WebMvcTest(CertificateController.class)
class CertificateControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private CertificateProfileService certificateProfileService;

    @ParameterizedTest
    @EnumSource(KeyMode.class)
    void rendersIssueDialogWithoutProfiles(KeyMode keyMode) throws Exception {
        when(certificateProfileService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/pki/certificates")
                    .param("mode", "issue")
                    .param("keyMode", keyMode.name()))
               .andExpect(status().isOk())
               .andExpect(view().name("pki/certificates/list"))
               .andExpect(content().string(containsString("name=\"validityDays\"")))
               .andExpect(content().string(containsString("name=\"notBefore\"")))
               .andExpect(content().string(containsString("name=\"subjectDn.commonName\"")))
               .andExpect(content().string(containsString(keyMode == KeyMode.NEW
                   ? "name=\"keyAlgorithmPreset\"" : "name=\"kmsKeyId\"")));
    }
}
