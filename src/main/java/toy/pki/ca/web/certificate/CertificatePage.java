package toy.pki.ca.web.certificate;

import java.util.List;

public record CertificatePage(List<CertificateView> content, int number, int size, int totalElements) {
    public boolean isEmpty() { return content.isEmpty(); }
    public int getTotalPages() { return Math.max(1, Math.ceilDiv(totalElements, size)); }
    public boolean hasPrevious() { return number > 0; }
    public boolean hasNext() { return number + 1 < getTotalPages(); }
}
