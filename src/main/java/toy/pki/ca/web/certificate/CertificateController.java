package toy.pki.ca.web.certificate;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.extern.slf4j.Slf4j;
import toy.pki.ca.web.certificate.form.CertificateIssueForm;



@Slf4j 
@Controller
@RequestMapping("/pki/certificates")
public class CertificateController {

    @ModelAttribute("caId")
    public String caId() {
        return "ca";
    }

    @ModelAttribute("sortDir")
    public String sortDir() {
        return "test";
    }

    @ModelAttribute ("filter")
    public CertificateFilter filter() {
        return new CertificateFilter();
    }

    @ModelAttribute ("signModes")
    public List<String> signModes() {
        return List.of("CA_SIGNED", "SELF_SIGNED");
    }

    @GetMapping("")
    public String certificates(
        @ModelAttribute("filter") CertificateFilter filter,
        @RequestParam(required = false) String mode,
        @RequestParam(required = false, defaultValue = "0") int page,
        @RequestParam(required = false, defaultValue = "20") int size,
        Model model
    ) {
        log.info("GET /pki/certificates - mode: {}, page: {}, size: {}", mode, page, size);

        model.addAttribute("size", size);
        model.addAttribute("sortField", "notAfter");
        model.addAttribute("sortDir", "asc");
        model.addAttribute("caList", List.of());
        model.addAttribute("statuses", List.of());

        if( mode != null && mode.equals("issue") ) {
            log.info("Issuing new certificate - preparing issue form");
            model.addAttribute("issueForm", new CertificateIssueForm());
        }

        log.info("Rendering certificate list view");
        model.addAttribute("totalCertificateCount", 0);
        return "pki/certificates/list";
    }

    @PostMapping("")
    public String postMethodName(@RequestBody String entity) {
        //TODO: process POST request
        
        return entity;
    }
    
    
}
